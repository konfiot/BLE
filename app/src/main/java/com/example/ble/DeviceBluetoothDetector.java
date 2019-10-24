package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class DeviceBluetoothDetector {
    MainActivity activity;

    DeviceBluetoothService btServices[];

    BluetoothAdapter btAdapter;

    Handler handler = new Handler();

    private SingBroadcastReceiver mReceiver;

    DeviceBluetoothDetector(MainActivity activity, DeviceBluetoothService btServices[]) {
        this.activity = activity;
        this.btServices = btServices;

        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(activity, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }

        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(activity, R.string.blc_not_supported, Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(activity, "No bluetooth adapter has been found", Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }
    }

    public void scanForDevices(final boolean enable) {
        if(enable) {
            if (btAdapter.isDiscovering()){
                btAdapter.cancelDiscovery();
            }

            if (!btAdapter.startDiscovery()) {
                Toast.makeText(activity, "Error starting discovery", Toast.LENGTH_SHORT).show();

            }

            mReceiver = new SingBroadcastReceiver();
            IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            activity.registerReceiver(mReceiver, ifilter);
            ifilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            activity.registerReceiver(mReceiver, ifilter);
            ifilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            activity.registerReceiver(mReceiver, ifilter);

        } else {
            btAdapter.cancelDiscovery();

        }
    }

    private void addDeviceToProperService(BluetoothDevice device) {
        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                btServices[1].addDevice(device);
                activity.addClassic(device);
                Toast.makeText(activity, "Classic device found : "+ device.getName() + " - " + device.getAddress() , Toast.LENGTH_SHORT).show();

                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                btServices[0].addDevice(device);
                activity.addBle(device);
                Toast.makeText(activity, "BLE device found : "+ device.getName() + " - " + device.getAddress(), Toast.LENGTH_SHORT).show();

                break;
            default:
                Toast.makeText(activity, R.string.unknown_btdevice_detected, Toast.LENGTH_SHORT).show();
        }
    }

    private class SingBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDeviceToProperService(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Toast.makeText(activity, R.string.scan_start, Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(activity, R.string.scan_stopped, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

