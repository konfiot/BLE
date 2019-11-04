package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class PopUpDeviceQuestion extends Activity {

    static DualBluetoothTypeCallback callback;
    static BluetoothDevice device;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bridge_view);

        DisplayMetrics dm  = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height =dm.heightPixels;

        getWindow().setLayout((int)(width*0.8), (int) (height*0.5));

        Button bleDeviceButton = findViewById(R.id.buttonDeviceBLE);
        Button classicDeviceButton = findViewById(R.id.buttonDeviceClassic);
        TextView question = findViewById(R.id.textQuestion);

        question.setText(R.string.question_bt_device);

        bleDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.selectedDeviceType(device, 1);
                finish();
            }
        });
        classicDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.selectedDeviceType(device, 0);
                finish();
            }
        });
    }

    public static void setCallBack(DualBluetoothTypeCallback cb) {
        callback = cb;
    }

    public static void passDevice(BluetoothDevice desiredDevice) {
        device = desiredDevice;
    }

}
