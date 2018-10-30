package com.github.kilnn.serialport.sample;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kilnn.serialport.SerialPort;
import com.github.kilnn.serialport.SerialPortFinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements SerialPortListFragment.SerialPortListFragmentHolder
        , SerialPortConfigFragment.SerialPortConfigFragmentHolder
        , SerialPortSelectFragment.SerialPortSelectFragmentHolder {

    private SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private Map<String, SerialPortConfig> mOpenedSerialPortMap = new HashMap<>();

    private TextView mEditWriteData;
    private ScrollView mScrollView;
    private TextView mTvReadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mEditWriteData = findViewById(R.id.edit_write_data);
        Button btnWrite = findViewById(R.id.btn_write);
        Button btnClearWrite = findViewById(R.id.btn_clear_write);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = mEditWriteData.getText().toString().trim().replaceAll(" ", "");
                if (str.length() <= 0) return;
                SerialPortSelectFragment.newInstance(str)
                        .showAllowingStateLoss(getSupportFragmentManager(), null);
            }
        });
        btnClearWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditWriteData.setText(null);
            }
        });

        mScrollView = findViewById(R.id.scroll_view);
        mTvReadData = findViewById(R.id.tv_read_data);
        Button btnClearRead = findViewById(R.id.btn_clear_read);
        btnClearRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTvReadData.setText(null);
            }
        });

        //显示设备信息
        mTvReadData.setText(getDeviceInfo());
    }

    private String getDeviceInfo() {
        StringBuilder sb = new StringBuilder();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        sb.append(dm.toString()).append("\n");
        sb.append("densityDpi:").append(dm.densityDpi).append("\n");

        sb.append("BRAND:").append(Build.BRAND).append("\n");
        sb.append("MODEL:").append(Build.MODEL).append("\n");
        sb.append("SDK:").append(Build.VERSION.SDK).append("\n");
        sb.append("RELEASE:").append(Build.VERSION.RELEASE).append("\n");
        sb.append("SDK_INT:").append(Build.VERSION.SDK_INT).append("\n");

        sb.append("TotalMemory:").append(Utils.getTotalMemory(this)).append("\n");
        sb.append("AvailMemory:").append(Utils.getAvailMemory(this)).append("\n");

        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_serial_port) {
            AndPermissionHelper.mainActivityRequest(this, new AndPermissionHelper.AndPermissionHelperListener1() {
                @Override
                public void onSuccess() {
                    new SerialPortListFragment()
                            .showAllowingStateLoss(getSupportFragmentManager(), null);
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public List<String> getSerialPortList() {
        String[] paths = null;
        try {
            paths = mSerialPortFinder.getAllDevicesPath(true);
        } catch (SecurityException e) {
            e.printStackTrace();
            toast(R.string.error_get_serial_port_permission);
        } catch (IOException e) {
            e.printStackTrace();
            toast(R.string.error_get_serial_port);
        }
        if (paths == null) {
            return new ArrayList<>(0);
        } else {
            List<String> list = new ArrayList<>(paths.length);
            for (String s : paths) {
                if (list.indexOf(s) == -1) {
                    list.add(s);
                }
            }
            return list;
        }
    }

    @Override
    public void writeData(String path, String data) {
        SerialPortConfig config = mOpenedSerialPortMap.get(path);
        if (config == null || config.getSerialPort() == null) return;
        byte[] values;
        if (config.isWriteHex()) {
            try {
                values = BytesUtil.hexStringToByteArray(data);
                if (values == null) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast(R.string.write_data_format_hex_error);
                return;
            }
        } else {
            values = data.getBytes();
        }
        if (!config.getSerialPort().sendData(values)) {
            toast(R.string.write_data_failed);
        }
    }

    @Override
    public Map<String, SerialPortConfig> getOpenedSerialPortMap() {
        return mOpenedSerialPortMap;
    }

    @Override
    public void openSerialPort(final String path) {
        SerialPortConfigFragment.newInstance(path).showAllowingStateLoss(getSupportFragmentManager(), null);
    }

    @Override
    public void closeSerialPort(String path) {
        SerialPortConfig config = mOpenedSerialPortMap.get(path);
        if (config != null) {
            SerialPort serialPort = config.getSerialPort();
            if (serialPort != null) {
                serialPort.release();
            }
            mOpenedSerialPortMap.remove(path);
        }
    }

    @Override
    public void setOpenSerialPort(final String path, int baudRate, final boolean readHex, boolean writeHex) {
        try {
            SerialPort serialPort = new SerialPort(path, baudRate);
            SerialPortConfig config = new SerialPortConfig();
            config.setSerialPort(serialPort);
            config.setBaudrate(baudRate);
            config.setReadHex(readHex);
            config.setWriteHex(writeHex);
            mOpenedSerialPortMap.put(path, config);

            serialPort.setListener(new SerialPort.Listener() {
                @Override
                public void onReceiveData(final byte[] data) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String msg;
                            if (readHex) {
                                msg = BytesUtil.byte2HexStr(data);
                            } else {
                                msg = new String(data);
                            }
                            msg += "\n";
                            mTvReadData.append(msg);
                            mScrollView.smoothScrollTo(0, mTvReadData.getBottom());
                        }
                    });
                }

                @Override
                public void onRunError(Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeSerialPort(path);
                        }
                    });
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
            toast(R.string.error_open_serial_port_permission);
        } catch (IOException e) {
            e.printStackTrace();
            toast(R.string.error_open_serial_port);
        }
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (String key : mOpenedSerialPortMap.keySet()) {
            SerialPortConfig config = mOpenedSerialPortMap.get(key);
            if (config != null && config.getSerialPort() != null) {
                config.getSerialPort().release();
            }
        }
        mOpenedSerialPortMap.clear();
    }
}
