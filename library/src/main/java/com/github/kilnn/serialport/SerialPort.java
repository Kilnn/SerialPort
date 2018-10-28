package com.github.kilnn.serialport;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * NDK 和 CMake
 * http://blog.csdn.net/wl9739/article/details/52607010
 * <p>
 * 串口
 * https://blog.csdn.net/u010312949/article/details/80199018
 * http://www.jianshu.com/p/e5004d75bd9c
 * http://blog.csdn.net/tangcheng_ok/article/details/7021470
 * http://blog.csdn.net/q4878802/article/details/52996548
 * <p>
 * 串口权限
 * http://blog.csdn.net/vir56k/article/details/47662207
 * <p>
 * USB转串口
 * http://blog.csdn.net/u010661782/article/details/50749080
 */
public class SerialPort {
    private static final String TAG = "SerialPort";
    private static final int BUFFER_SIZE = 4096;

    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        void onReceiveData(byte[] data);

        /**
         * Called when ReceiveDataThread aborts due to an
         * error.
         */
        void onRunError(Exception e);
    }

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private boolean mRunning;
    private ByteBuffer mReadBuffer;
    private ReadThread mReadThread;

    private volatile Listener mListener;

    public SerialPort(String path, int baudrate) throws SecurityException, IOException {
        this(new File(path), baudrate, 0);
    }

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
                        + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead()
                        || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
        mRunning = true;
        mReadBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        mReadThread = new ReadThread();
        mReadThread.start();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * 发送指令到串口
     *
     * @return 发送指令是否成功
     */
    public boolean sendData(byte[] data) {
        final OutputStream out = mFileOutputStream;
        if (out == null) return false;
        try {
            out.write(data);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void release() {
        mRunning = false;
        if (mReadThread != null && mReadThread.isAlive()) {
            mReadThread.interrupt();
        }
        close();
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (mRunning && !isInterrupted()) {
                    // Handle incoming data.
                    int len = mFileInputStream.read(mReadBuffer.array());
                    if (len > 0) {
                        Log.d(TAG, "Read data len=" + len);
                        final Listener listener = mListener;
                        if (listener != null) {
                            final byte[] data = new byte[len];
                            mReadBuffer.get(data, 0, len);
                            listener.onReceiveData(data);
                        }
                        mReadBuffer.clear();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
                final Listener listener = mListener;
                if (listener != null) {
                    listener.onRunError(e);
                }
            } finally {
                release();
            }
        }
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    private native static FileDescriptor open(String path, int baudrate, int flags);

    private native void close();

    static {
        System.loadLibrary("serial-port");
    }
}
