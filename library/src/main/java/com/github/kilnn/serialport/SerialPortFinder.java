package com.github.kilnn.serialport;

import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by Kilnn on 2017/10/22.
 */

public class SerialPortFinder {

    public class Driver {
        private Driver(String name, String root) {
            mDriverName = name;
            mDeviceRoot = root;
        }

        private String mDriverName;
        private String mDeviceRoot;
        private Vector<File> mDevices = null;

        private Vector<File> getDevices() throws SecurityException {
            if (mDevices == null) {
                mDevices = new Vector<>();
                File dev = new File("/dev");
                if (!dev.canRead()) {
                    throw new SecurityException();
                }
                File[] files = dev.listFiles();
                if (files != null) {
                    int i;
                    for (i = 0; i < files.length; i++) {
                        if (files[i].getAbsolutePath().startsWith(mDeviceRoot)) {
                            Log.d(TAG, "Found new device: " + files[i]);
                            mDevices.add(files[i]);
                        }
                    }
                }
            }
            return mDevices;
        }

        public String getName() {
            return mDriverName;
        }
    }

    private static final String TAG = "SerialPortFinder";

    private Vector<Driver> mDrivers = null;

    private Vector<Driver> getDrivers(boolean refresh) throws IOException, SecurityException {
        if (refresh || mDrivers == null) {
            mDrivers = new Vector<>();
            File file = new File("/proc/tty/drivers");
            if (!file.canRead()) {
                throw new SecurityException();
            }
            LineNumberReader r = new LineNumberReader(new FileReader(file));
            String l;
            while ((l = r.readLine()) != null) {
                // Issue 3:
                // Since driver name may contain spaces, we do not extract driver name with split()
                String drivername = l.substring(0, 0x15).trim();
                String[] w = l.split(" +");
                if ((w.length >= 5) && (w[w.length - 1].equals("serial"))) {
                    Log.d(TAG, "Found new driver " + drivername + " on " + w[w.length - 4]);
                    mDrivers.add(new Driver(drivername, w[w.length - 4]));
                }
            }
            r.close();
        }
        return mDrivers;
    }

    public String[] getAllDevices(boolean refresh) throws IOException, SecurityException {
        Vector<String> devices = new Vector<>();
        // Parse each driver
        Iterator<Driver> itdriv;
        itdriv = getDrivers(refresh).iterator();
        while (itdriv.hasNext()) {
            Driver driver = itdriv.next();
            Iterator<File> itdev = driver.getDevices().iterator();
            while (itdev.hasNext()) {
                String device = itdev.next().getName();
                String value = String.format("%s (%s)", device, driver.getName());
                devices.add(value);
            }
        }
        return devices.toArray(new String[devices.size()]);
    }

    public String[] getAllDevicesPath(boolean refresh) throws IOException, SecurityException {
        Vector<String> devices = new Vector<>();
        // Parse each driver
        Iterator<Driver> itdriv;
        itdriv = getDrivers(refresh).iterator();
        while (itdriv.hasNext()) {
            Driver driver = itdriv.next();
            Iterator<File> itdev = driver.getDevices().iterator();
            while (itdev.hasNext()) {
                String device = itdev.next().getAbsolutePath();
                devices.add(device);
            }
        }
        return devices.toArray(new String[devices.size()]);
    }
}