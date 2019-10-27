package com.example.ble;

public interface BluetoothServiceStateChange {
    void serviceStopped();
    void dataSent(String data);
    void dataReceived(String data);
}
