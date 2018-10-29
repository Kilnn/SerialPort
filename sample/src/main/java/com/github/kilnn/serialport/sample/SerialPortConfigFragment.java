package com.github.kilnn.serialport.sample;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SerialPortConfigFragment extends PreventRestoreDialogFragment {

    private static final String EXTRA_PATH = "path";

    public static SerialPortConfigFragment newInstance(String path) {
        SerialPortConfigFragment fragment = new SerialPortConfigFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PATH, path);
        fragment.setArguments(bundle);
        return fragment;
    }

    public interface SerialPortConfigFragmentHolder {
        void setOpenSerialPort(String path, int baudRate, final boolean readHex, boolean writeHex);
    }

    private SerialPortConfigFragmentHolder mHolder;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SerialPortConfigFragmentHolder) {
            mHolder = (SerialPortConfigFragmentHolder) context;
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
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_serial_port_config, null);

        /* Baud Rate */
        final Spinner baudRateSpinner = view.findViewById(R.id.spinner_baud_rate);
        ArrayAdapter<CharSequence> baudRateAdapter = ArrayAdapter.createFromResource(getContext(), R.array.baud_rate, R.layout.my_spinner_textview);
        baudRateAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        baudRateSpinner.setAdapter(baudRateAdapter);
        baudRateSpinner.setSelection(9);//默认值115200

        /* Read Text Style */
        final Spinner readStyleSpinner = view.findViewById(R.id.spinner_read_style);
        ArrayAdapter<CharSequence> readStyleAdapter = ArrayAdapter.createFromResource(getContext(), R.array.text_style, R.layout.my_spinner_textview);
        readStyleAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        readStyleSpinner.setAdapter(readStyleAdapter);

        /* Write Text Style */
        final Spinner writeStyleSpinner = view.findViewById(R.id.spinner_write_style);
        ArrayAdapter<CharSequence> writeStyleAdapter = ArrayAdapter.createFromResource(getContext(), R.array.text_style, R.layout.my_spinner_textview);
        writeStyleAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        writeStyleSpinner.setAdapter(writeStyleAdapter);

        return new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.serial_port_setting)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int baudRate = Integer.parseInt(baudRateSpinner.getSelectedItem().toString());
                        boolean readHex = readStyleSpinner.getSelectedItem().toString().equals("Hex");
                        boolean writeHex = writeStyleSpinner.getSelectedItem().toString().equals("Hex");
                        String path = null;
                        if (getArguments() != null) {
                            path = getArguments().getString(EXTRA_PATH);
                        }
                        if (!TextUtils.isEmpty(path) && mHolder != null) {
                            mHolder.setOpenSerialPort(path, baudRate, readHex, writeHex);
                        }
                    }
                }).create();
    }

}
