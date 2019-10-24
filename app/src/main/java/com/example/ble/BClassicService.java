package com.example.ble;

import android.content.Context;

public class BClassicService extends DeviceBluetoothService {

    BClassicService(Context context) {
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
