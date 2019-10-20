package com.example.ble;

import java.util.concurrent.LinkedBlockingQueue;

public abstract class DeviceBluetoothSevice {

    protected LinkedBlockingQueue<String> txQueue, rxQueue;

    protected BluetoothDataReception rxCallback;

    protected DeviceBluetoothSevice() {
        txQueue = new LinkedBlockingQueue<>();
        rxQueue = new LinkedBlockingQueue<>();
    }

    abstract protected void sendDataToDevice(String data);

    abstract protected String receiveDataFromDevice();

    public void dispatchDataToDevice(String data) {
        txQueue.add(data);
    }

    public void setDataReceptionCallback(BluetoothDataReception rxCallback) {
        this.rxCallback = rxCallback;
    }
}
