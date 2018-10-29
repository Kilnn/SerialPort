package com.github.kilnn.serialport.sample;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kilnn.serialport.SerialPort;
import com.github.kilnn.serialport.SerialPortFinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private SerialPortFinder mSerialPortFinder;
    private Map<String, SerialPortConfig> mOpendSerialPortMap;
    private List<String> mSerialPortList;
    private SerialPortListAdapter mSerialPortListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        mSerialPortFinder = new SerialPortFinder();
        mOpendSerialPortMap = new HashMap<>();
        mSerialPortList = new ArrayList<>();
        mSerialPortListAdapter = new SerialPortListAdapter();
    }

    private TextView mEditWriteData;
    private ScrollView mScrollView;
    private TextView mTvReadData;

    private void initView() {
        mEditWriteData = findViewById(R.id.edit_write_data);
        Button btnWrite = findViewById(R.id.btn_write);
        Button btnClearWrite = findViewById(R.id.btn_clear_write);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = mEditWriteData.getText().toString().trim().replaceAll(" ", "");
                if (str.length() <= 0) return;
                selectWriteSerialPort(str);
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
                    showSerialPortList();
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    public void selectWriteSerialPort(final String data) {
        final String[] items = new String[mOpendSerialPortMap.size()];
        mOpendSerialPortMap.keySet().toArray(items);
        new AlertDialog.Builder(this)
                .setTitle("选择写入串口")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String path = items[which];
                        SerialPortConfig config = mOpendSerialPortMap.get(path);
                        if (config != null) {
                            byte[] values;
                            if (config.isWriteHex()) {
                                try {
                                    values = BytesUtil.hexStringToByteArray(data);
                                    if (values == null) {
                                        throw new IllegalArgumentException();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    toast("请输入合法的十六进制字符，如：12 34 56 78 9A BC DE FF");
                                    return;
                                }
                            } else {
                                values = data.getBytes();
                            }

                            SerialPort serialPort = config.getSerialPort();
                            if (serialPort == null) {
                                toast("串口未打开");
                            } else {
                                boolean success = serialPort.sendData(values);
                                if (!success) {
                                    toast("写入失败");
                                }
                            }
                        }
                    }
                })
                .show();
    }

    private AlertDialog mSerialPortListDialog;

    private void showSerialPortList() {
        //获取路径，先放入Set去重，然后放入List
        String[] paths = mSerialPortFinder.getAllDevicesPath(true);
        Set<String> pathSet = new HashSet<>(paths.length);
        pathSet.addAll(Arrays.asList(paths));
        mSerialPortList.clear();
        mSerialPortList.addAll(pathSet);

        if (mSerialPortListDialog == null) {
            mSerialPortListDialog = new AlertDialog.Builder(this)
                    .setAdapter(mSerialPortListAdapter, null).create();
        } else {
            mSerialPortListAdapter.notifyDataSetChanged();
        }
        mSerialPortListDialog.show();
    }

    private class SerialPortListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mSerialPortList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_serial_port, parent, false);
            }
            TextView tv_path = ViewHolderUtils.get(convertView, R.id.tv_path);
            TextView tv_info = ViewHolderUtils.get(convertView, R.id.tv_info);
            Button btn_action = ViewHolderUtils.get(convertView, R.id.btn_action);

            final String path = mSerialPortList.get(position);
            tv_path.setText(path);

            SerialPortConfig config = mOpendSerialPortMap.get(path);
            if (config == null) {//未打开的串口
                tv_info.setText(null);
                btn_action.setText("打开");
                btn_action.setBackgroundResource(R.drawable.selector_device_list_button_open_bg);
                btn_action.setTextColor(Color.BLACK);
                btn_action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mSerialPortListDialog != null && mSerialPortListDialog.isShowing()) {
                            mSerialPortListDialog.dismiss();
                        }
                        openSerialPort(path);

                    }
                });
            } else {
                tv_info.setText(String.format("波特率:%d  ReadStyle:%s WriteStyle:%s", config.getBaudrate(),
                        config.isReadHex() ? "Hex" : "String", config.isWriteHex() ? "Hex" : "String"));
                btn_action.setText("关闭");
                btn_action.setBackgroundResource(R.drawable.selector_device_list_button_close_bg);
                btn_action.setTextColor(Color.WHITE);
                btn_action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mSerialPortListDialog != null && mSerialPortListDialog.isShowing()) {
                            mSerialPortListDialog.dismiss();
                        }
                        closeSerialPort(path);
                    }
                });
            }
            return convertView;
        }
    }

    private void openSerialPort(final String path) {
        View view = getLayoutInflater().inflate(R.layout.layout_serial_port_config, null);

        /* Baud Rate */
        final Spinner baudRateSpinner = view.findViewById(R.id.spinner_baud_rate);
        ArrayAdapter<CharSequence> baudRateAdapter = ArrayAdapter.createFromResource(this, R.array.baud_rate, R.layout.my_spinner_textview);
        baudRateAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        baudRateSpinner.setAdapter(baudRateAdapter);
        baudRateSpinner.setGravity(0x10);
        baudRateSpinner.setSelection(9);//默认值115200

        /* Read Text Style */
        final Spinner readStyleSpinner = view.findViewById(R.id.spinner_read_style);
        ArrayAdapter<CharSequence> readStyleAdapter = ArrayAdapter.createFromResource(this, R.array.text_style, R.layout.my_spinner_textview);
        readStyleAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        readStyleSpinner.setAdapter(readStyleAdapter);
        readStyleSpinner.setGravity(0x10);
        readStyleSpinner.setSelection(0);

        /* Write Text Style */
        final Spinner writeStyleSpinner = view.findViewById(R.id.spinner_write_style);
        ArrayAdapter<CharSequence> writeStyleAdapter = ArrayAdapter.createFromResource(this, R.array.text_style, R.layout.my_spinner_textview);
        writeStyleAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        writeStyleSpinner.setAdapter(writeStyleAdapter);
        writeStyleSpinner.setGravity(0x10);
        writeStyleSpinner.setSelection(0);

        new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("串口设置")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int baudRate = Integer.parseInt(baudRateSpinner.getSelectedItem().toString());
                        boolean readHex = readStyleSpinner.getSelectedItem().toString().equals("Hex");
                        boolean writeHex = writeStyleSpinner.getSelectedItem().toString().equals("Hex");
                        setOpenSerialPort(path, baudRate, readHex, writeHex);
                    }
                })
                .show();
    }

    private void setOpenSerialPort(final String path, int baudRate, final boolean readHex, boolean writeHex) {
        try {
            SerialPort serialPort = new SerialPort(path, baudRate);
            SerialPortConfig config = new SerialPortConfig();
            config.setSerialPort(serialPort);
            config.setBaudrate(baudRate);
            config.setReadHex(readHex);
            config.setWriteHex(writeHex);
            mOpendSerialPortMap.put(path, config);

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
            toast("打开串口失败，无权限！！！");
        } catch (IOException e) {
            e.printStackTrace();
            toast("打开串口失败！！！");
        }
    }

    private void closeSerialPort(String path) {
        SerialPortConfig serialPortConfig = mOpendSerialPortMap.get(path);
        if (serialPortConfig != null) {
            SerialPort serialPort = serialPortConfig.getSerialPort();
            if (serialPort != null) {
                serialPort.setListener(null);
                serialPort.release();
            }
            mOpendSerialPortMap.remove(path);
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSerialPortListDialog != null && mSerialPortListDialog.isShowing()) {
            mSerialPortListDialog.dismiss();
        }
    }
}
