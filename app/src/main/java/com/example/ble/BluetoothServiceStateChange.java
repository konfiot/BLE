package com.example.ble;

public interface BluetoothServiceStateChange {
    void serviceStateChange(boolean isConnected);
    void dataSent(String data);
    void dataReceived(String data);
}
