package com.example.ble;

public interface BluetoothDataReception {

    void bluetoothDataReceptionCallback(byte data[]);

    void bluetoothConnectionChanged(boolean connected);

}
