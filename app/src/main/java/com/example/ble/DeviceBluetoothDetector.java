package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class DeviceBluetoothDetector {

    private static final long SCAN_PERIOD = 6000; // 6 seconds

    Activity activity;

    DeviceBluetoothService btServices[];

    BluetoothAdapter btAdapter;

    BluetoothLeScanner bleScanner;

    Handler handler = new Handler();

    DeviceBluetoothDetector(Activity activity, DeviceBluetoothService btServices[]) {
        this.activity = activity;
        this.btServices = btServices;

        if(!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(activity, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            activity.finish();
        }

        if(!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(activity, R.string.blc_not_supported, Toast.LENGTH_LONG).show();
            activity.finish();
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        bleScanner = btAdapter.getBluetoothLeScanner();

//        if (Build.VERSION.SDK_INT < 21) {
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
//                    addDeviceToProperService(bluetoothDevice);
//                }
//            };
//        }
    }

    public void scanForDevices(final boolean enable) {
        if(enable) {
            handler.postDelayed( new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(activity, R.string.scan_stopped, Toast.LENGTH_SHORT).show();
                    bleScanner.stopScan(scanCallback);
                }

            }, SCAN_PERIOD);
            Toast.makeText(activity, R.string.scan_start, Toast.LENGTH_SHORT).show();
            bleScanner.startScan(scanCallback);
        } else {
//            if (Build.VERSION.SDK_INT < 21) {
//
//            } else {
                bleScanner.stopScan(scanCallback);
//            }
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            addDeviceToProperService(result.getDevice());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("Failed", Integer.toString(errorCode));
        }
    };

    private void addDeviceToProperService(BluetoothDevice device) {
        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                btServices[1].addDevice(device);
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                btServices[0].addDevice(device);
                break;
            default:
                Toast.makeText(activity, R.string.unknown_btdevice_detected, Toast.LENGTH_SHORT);
        }
    }

}
