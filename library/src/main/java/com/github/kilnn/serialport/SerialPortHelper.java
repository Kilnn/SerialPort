package com.github.kilnn.serialport;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Kilnn on 2017/10/22.
 */

public class SerialPortHelper {
    private static final String PATH = "/dev/ttyS3";
    private static final int BAUDRATE = 115200;

    public interface OnDataReceiveListener {
        void onDataReceive(byte[] buffer, int size);
    }

    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private OnDataReceiveListener mListener = null;
    private boolean isStop = false;


    public SerialPortHelper(Context context) {
        try {
            String path = PreferenceManager.getDefaultSharedPreferences(context).getString("SerialPort", null);
            if (TextUtils.isEmpty(path)) {
                path = PATH;
            }
            mSerialPort = new SerialPort(new File(path), BAUDRATE, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            mReadThread = new ReadThread();
            isStop = false;
            mReadThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnDataReceiveListener(OnDataReceiveListener listener) {
        mListener = listener;
    }


    /**
     * 发送指令到串口
     *
     * @param cmd
     * @return
     */
    public boolean sendCmds(String cmd) {
        boolean result = true;
        byte[] mBuffer = cmd.getBytes();
        try {
            if (mOutputStream != null) {
                mOutputStream.write(mBuffer);
            } else {
                result = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public boolean sendBuffer(byte[] mBuffer) {
        boolean result = true;
        String tail = "";
        byte[] tailBuffer = tail.getBytes();
        byte[] mBufferTemp = new byte[mBuffer.length + tailBuffer.length];
        System.arraycopy(mBuffer, 0, mBufferTemp, 0, mBuffer.length);
        System.arraycopy(tailBuffer, 0, mBufferTemp, mBuffer.length, tailBuffer.length);
        try {
            if (mOutputStream != null) {
                mOutputStream.write(mBufferTemp);
            } else {
                result = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isStop && !isInterrupted()) {
                int size;
                try {
                    if (mInputStream == null)
                        return;
                    byte[] buffer = new byte[512];
                    size = mInputStream.read(buffer);
                    if (size > 0) {
//                          String str = new String(buffer, 0, size);
//                          Logger.d("length is:"+size+",data is:"+new String(buffer, 0, size));
                        if (mListener != null) {
                            mListener.onDataReceive(buffer, size);
                        }
                    }
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        isStop = true;
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        if (mSerialPort != null) {
            mSerialPort.close();
        }
    }

}
