package com.example.ble;

import android.content.Context;

public class BleService extends DeviceBluetoothService {

    BleService(Context context) {
        super();
    }

    @Override
    protected void sendDataToDevice(String data) {

    }

    @Override
    protected String receiveDataFromDevice() {
        return null;
    }
}
