package com.github.kilnn.serialport.sample;

import com.github.kilnn.serialport.SerialPort;

public class SerialPortConfig {
    private SerialPort serialPort;
    private int baudrate;
    private boolean readHex;
    private boolean writeHex;

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public void setSerialPort(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
    }

    public boolean isReadHex() {
        return readHex;
    }

    public void setReadHex(boolean readHex) {
        this.readHex = readHex;
    }

    public boolean isWriteHex() {
        return writeHex;
    }

    public void setWriteHex(boolean writeHex) {
        this.writeHex = writeHex;
    }
}
