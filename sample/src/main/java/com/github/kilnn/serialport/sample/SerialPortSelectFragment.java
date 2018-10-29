package com.github.kilnn.serialport.sample;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.Map;

public class SerialPortSelectFragment extends PreventRestoreDialogFragment {

    private static final String EXTRA_DATA = "data";

    public static SerialPortSelectFragment newInstance(String data) {
        SerialPortSelectFragment fragment = new SerialPortSelectFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_DATA, data);
        fragment.setArguments(bundle);
        return fragment;
    }

    public interface SerialPortSelectFragmentHolder {
        void writeData(String path, String data);

        Map<String, SerialPortConfig> getOpenedSerialPortMap();
    }

    private SerialPortSelectFragmentHolder mHolder;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SerialPortSelectFragmentHolder) {
            mHolder = (SerialPortSelectFragmentHolder) context;
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
        final SerialPortSelectAdapter adapter = new SerialPortSelectAdapter(this);
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.select_write_serial_port)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mHolder != null) {
                            mHolder.writeData((String) adapter.getItem(which), getArguments().getString(EXTRA_DATA));
                        }
                    }
                }).create();
    }

    private static class SerialPortSelectAdapter extends BaseAdapter {
        private SerialPortSelectFragment fragment;
        private Map<String, SerialPortConfig> openedSerialPortMap;
        private String[] serialPorts;

        private SerialPortSelectAdapter(SerialPortSelectFragment fragment) {
            this.fragment = fragment;
            SerialPortSelectFragmentHolder holder = fragment.mHolder;
            if (holder != null) {
                openedSerialPortMap = holder.getOpenedSerialPortMap();
                serialPorts = new String[openedSerialPortMap.size()];
                openedSerialPortMap.keySet().toArray(serialPorts);
            }
        }

        @Override
        public int getCount() {
            return serialPorts != null ? serialPorts.length : 0;
        }

        @Override
        public Object getItem(int position) {
            return serialPorts[position];
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
            btn_action.setVisibility(View.GONE);

            final String path = serialPorts[position];
            tv_path.setText(path);
            SerialPortConfig config = openedSerialPortMap.get(path);
            if (config != null) {//未打开的串口
                tv_info.setText(String.format(
                        fragment.getString(R.string.serial_port_config),
                        config.getBaudrate(),
                        config.isReadHex() ? "Hex" : "String",
                        config.isWriteHex() ? "Hex" : "String"));
            }
            return convertView;
        }
    }


}
