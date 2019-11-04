package com.example.ble;

import android.bluetooth.BluetoothDevice;

interface DualBluetoothTypeCallback {
    void selectedDeviceType(BluetoothDevice device, int index);
}
