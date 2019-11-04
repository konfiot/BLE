package com.example.ble;

import android.bluetooth.BluetoothDevice;

public interface BluetoothDataReception {

    void bluetoothDataReceptionCallback(byte data[]);

    void bluetoothConnectionChanged(BluetoothDevice device, boolean connected);

}
