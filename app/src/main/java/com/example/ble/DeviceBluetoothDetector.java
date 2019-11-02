package com.example.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

public class DeviceBluetoothDetector {
    MainActivity activity;

    DeviceBluetoothService btServices[];

    BluetoothAdapter btAdapter;

    private SingBroadcastReceiver mReceiver;

    DeviceBluetoothDetector(MainActivity activity, BluetoothAdapter btAdapter, DeviceBluetoothService btServices[]) {
        this.activity = activity;
        this.btServices = btServices;
        this.btAdapter = btAdapter;

    }

    public void scanForDevices(final boolean enable) {
        if(enable) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, 3);
            }

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
        if(!(device.fetchUuidsWithSdp())){
            Toast.makeText(activity, "Failed to fetch UUIDs fore device "+ device.getName() + " - " + device.getAddress() , Toast.LENGTH_SHORT).show();
        }

        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                btServices[1].addDevice(device);
//                activity.addClassic(device);
                Toast.makeText(activity, "Classic device found : "+ device.getName() + " - " + device.getAddress() , Toast.LENGTH_SHORT).show();

                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                btServices[0].addDevice(device);
//                activity.addBle(device);
                Toast.makeText(activity, "BLE device found : "+ device.getName() + " - " + device.getAddress(), Toast.LENGTH_SHORT).show();

                break;
            default:
                Toast.makeText(activity, R.string.unknown_btdevice_detected, Toast.LENGTH_SHORT).show();
                System.out.println(device.toString() + " " + device.getType());
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

