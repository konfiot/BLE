package com.example.ble;

import java.util.List;

public interface BluetoothDeviceListListener {
    void updateDeviceList(List<String> deviceKey);
}
