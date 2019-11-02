package com.example.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This code is inspired by the android GATT server example at :
 * https://github.com/androidthings/sample-bluetooth-le-gattserver/blob/master/java/app/src/main/java/com/example/androidthings/gattserver/GattServerActivity.java
 */
public class BleServerMode {

    public final static UUID BLE_XFER_CHARACTERISTIC = UUID.fromString("af20fbac-2518-4998-9af7-af42540731b3");

    public final static UUID BLE_XFER_SERVICE = UUID.fromString("f6ec37db-bda1-46ec-a43a-6d86de88561d");

    private BluetoothGattServer bleServer;

    private BluetoothManager bleManager;

    private BluetoothLeAdvertiser bleAdvertiser;

    private BluetoothDevice bleClient;

    private BluetoothAdapter blueAdapter;

    private Context context;

    private BluetoothDataReception rxCallback;

    private BluetoothGattCharacteristic bleServerChar;

    private BluetoothGattService bleService;

    private AtomicBoolean connected;

    private byte rxByteData[];

    BleServerMode(Context context, BluetoothAdapter adapter) {
        this.context = context;
        blueAdapter = adapter;
        connected = new AtomicBoolean(false);

    }

    void setRxCallback(BluetoothDataReception rxCallback) {
        this.rxCallback = rxCallback;
    }

    void updateContext(Context newContext) {
        context = newContext;
    }

    void writeDataToDevice(String data) {
        writeDataToDevice(data.getBytes());
    }

    void writeDataToDevice(byte data[]) {
        if(connected.get()) {
            BluetoothGattCharacteristic txChar = bleServer
                    .getService(BLE_XFER_SERVICE)
                    .getCharacteristic(BLE_XFER_CHARACTERISTIC);
            txChar.setValue(data);
            bleServer.notifyCharacteristicChanged(bleClient, txChar, false);
        }
    }

    boolean startAdvertising() {
        bleAdvertiser = blueAdapter.getBluetoothLeAdvertiser();
        if(bleAdvertiser == null) {
            Toast.makeText(context, "Failed to create ble advertiser", Toast.LENGTH_SHORT).show();
            return false;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(MainActivity.DETECTION_TIMEOUT)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(BLE_XFER_SERVICE)).build();

        bleAdvertiser.startAdvertising(settings, data, advertiseCB);
        return true;
    }

    void stopAdvertising() {
        if(bleAdvertiser == null) {
            return;
        }
        bleAdvertiser.stopAdvertising(advertiseCB);
    }

    boolean startServer() {
        bleServer = bleManager.openGattServer(context, serverCb);
        if(bleServer == null) {
            Toast.makeText(context, "Could not start bleServer", Toast.LENGTH_LONG).show();
            return false;
        }
        // Recreates the service for the server
        bleService = new BluetoothGattService(BLE_XFER_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        bleService.addCharacteristic(getNewCharacteristic());
        // Adds the service to the server
        bleServer.addService(bleService);
        return true;
    }

    void stopServer() {
        if(bleServer == null) {
            return;
        }
        bleServer.close();
    }

    private BluetoothGattCharacteristic getNewCharacteristic(){
        return new BluetoothGattCharacteristic(BLE_XFER_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

    }

    private AdvertiseCallback advertiseCB = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Toast.makeText(context, R.string.start_advertising, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStartFailure(int errorCode) {
            Toast.makeText(context, R.string.fail_advertising, Toast.LENGTH_SHORT).show();
        }
    };

    private BluetoothGattServerCallback serverCb = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                Toast.makeText(context, "BLE client connected", Toast.LENGTH_SHORT).show();
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected.set(false);
                rxCallback.bluetoothConnectionChanged(false);
                Toast.makeText(context, "BLE client disconnected", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if(bleClient.equals(device)) {
                if (BLE_XFER_CHARACTERISTIC.equals(characteristic.getUuid())) {
                    rxCallback.bluetoothDataReceptionCallback(value);
                }
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            if(BLE_XFER_SERVICE.equals(descriptor.getUuid())) {
                byte[] answer = device.equals(bleClient) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            } else {
                bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            // Makes sure that the right service can request to subscribe to the server
            if(BLE_XFER_SERVICE.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    bleClient = device;
                    Toast.makeText(context, "Subscribed the device to the server", Toast.LENGTH_SHORT).show();
                    connected.set(true);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    bleClient = null;
                    connected.set(false);
                    Toast.makeText(context, "Subscribed the device to the server", Toast.LENGTH_SHORT).show();
                }

                rxCallback.bluetoothConnectionChanged(connected.get());

                if (responseNeeded) {
                    bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            } else {
                if (responseNeeded) {
                    bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }

        }
    };

}
