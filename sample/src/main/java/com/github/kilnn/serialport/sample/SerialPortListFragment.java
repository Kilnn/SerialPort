package com.github.kilnn.serialport.sample;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class SerialPortListFragment extends PreventRestoreDialogFragment {

    public interface SerialPortListFragmentHolder {
        List<String> getSerialPortList();

        Map<String, SerialPortConfig> getOpenedSerialPortMap();

        void openSerialPort(String path);

        void closeSerialPort(String path);
    }

    private SerialPortListFragmentHolder mHolder;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SerialPortListFragmentHolder) {
            mHolder = (SerialPortListFragmentHolder) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHolder = null;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState, Object NULL_FOR_OVERRIDE) {
        return new AlertDialog.Builder(getContext())
                .setAdapter(new SerialPortListAdapter(this), null).create();
    }

    private static class SerialPortListAdapter extends BaseAdapter {
        private SerialPortListFragment fragment;
        private SerialPortListFragmentHolder holder;
        private List<String> serialPortList;
        private Map<String, SerialPortConfig> openedSerialPortMap;

        private SerialPortListAdapter(SerialPortListFragment fragment) {
            this.fragment = fragment;
            holder = fragment.mHolder;
            if (holder != null) {
                serialPortList = holder.getSerialPortList();
                openedSerialPortMap = holder.getOpenedSerialPortMap();
            }
        }

        @Override
        public int getCount() {
            return serialPortList != null ? serialPortList.size() : 0;
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
                convertView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.item_serial_port, parent, false);
            }
            TextView tv_path = ViewHolderUtils.get(convertView, R.id.tv_path);
            TextView tv_info = ViewHolderUtils.get(convertView, R.id.tv_info);
            Button btn_action = ViewHolderUtils.get(convertView, R.id.btn_action);

            final String path = serialPortList.get(position);
            tv_path.setText(path);

            SerialPortConfig config = openedSerialPortMap.get(path);
            if (config == null) {//未打开的串口
                tv_info.setText(null);
                btn_action.setText(R.string.action_open);
                btn_action.setBackgroundResource(R.drawable.selector_device_list_button_open_bg);
                btn_action.setTextColor(Color.BLACK);
                btn_action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fragment.dismissAllowingStateLoss();
                        if (holder != null) {
                            holder.openSerialPort(path);
                        }
                    }
                });
            } else {
                tv_info.setText(String.format(
                        fragment.getString(R.string.serial_port_config),
                        config.getBaudrate(),
                        config.isReadHex() ? "Hex" : "String",
                        config.isWriteHex() ? "Hex" : "String"));
                btn_action.setText(R.string.action_close);
                btn_action.setBackgroundResource(R.drawable.selector_device_list_button_close_bg);
                btn_action.setTextColor(Color.WHITE);
                btn_action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fragment.dismissAllowingStateLoss();
                        if (holder != null) {
                            holder.closeSerialPort(path);
                        }
                    }
                });
            }
            return convertView;
        }
    }


}
