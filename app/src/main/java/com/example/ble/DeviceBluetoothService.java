package com.example.ble;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class DeviceBluetoothService {

    protected LinkedBlockingQueue<String> txQueue, rxQueue;

    protected BluetoothDataReception rxCallback;

    protected ArrayList<BluetoothDevice> btDevices;

    protected DeviceBluetoothService() {
        txQueue = new LinkedBlockingQueue<>();
        rxQueue = new LinkedBlockingQueue<>();
        btDevices = new ArrayList<>();
    }

    abstract protected void sendDataToDevice(String data);

    abstract protected String receiveDataFromDevice();

    public void dispatchDataToDevice(String data) {
        txQueue.add(data);
    }

    public void setDataReceptionCallback(BluetoothDataReception rxCallback) {
        this.rxCallback = rxCallback;
    }

    public void addDevice(BluetoothDevice device) {
        if(btDevices.contains(device)) {
            btDevices.add(device);
        }
    }
}
