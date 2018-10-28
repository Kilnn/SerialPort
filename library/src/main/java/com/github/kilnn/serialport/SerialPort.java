package com.github.kilnn.serialport;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * NDK 和 CMake
 * http://blog.csdn.net/wl9739/article/details/52607010
 * <p>
 * 串口
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

    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        if (!device.canRead() || !device.canWrite()) {
            try {
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudrate, flags);
        Log.e("Kilnn", "mFd:" + mFd);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);

        Log.e("Kilnn", "mFileInputStream:" + mFileInputStream);
        Log.e("Kilnn", "mFileOutputStream:" + mFileOutputStream);
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    private native static FileDescriptor open(String path, int baudrate, int flags);

    public native void close();

    static {
        System.loadLibrary("serial-port");
    }
}
