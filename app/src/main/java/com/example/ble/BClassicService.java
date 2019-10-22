package com.example.ble;

public class BClassicService extends DeviceBluetoothService {

    BClassicService() {
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
