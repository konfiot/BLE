package com.example.ble;

import android.app.Activity;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BleServerMode extends Activity {

    public final static UUID BLE_XFER_CHARACTERISTIC = UUID.fromString("af20fbac-2518-4998-9af7-af42540731b3");
    public final static UUID BLE_XFER_SERVICE = UUID.fromString("f6ec37db-bda1-46ec-a43a-6d86de88561d");
    public final static UUID BLE_CLIENT_DESCRIPTOR = UUID.fromString("f6ec37db-f8e4-11e9-8f0b-362b9e155667");

    private BluetoothGattServer bleServer;

    private BluetoothManager bleManager;

    private BluetoothLeAdvertiser bleAdvertiser;

    private BluetoothDevice bleClient;

    private BluetoothAdapter blueAdapter;

    private BluetoothGattCharacteristic bleServerChar;

    private BluetoothGattService bleService;

    private AtomicBoolean connected;

    private BleServerMode context;

    Button sendToBle, stopServer;
    TextView console;
    EditText outputMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bridge_view);

        context = this;

        blueAdapter = BluetoothAdapter.getDefaultAdapter();
        bleManager = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        connected = new AtomicBoolean(false);

        Button sendToClassic = findViewById(R.id.buttonSendClassic);
        sendToClassic.setVisibility(View.INVISIBLE);


        stopServer = findViewById(R.id.stopButton);
        sendToBle = findViewById(R.id.buttonSendBLE);
        console = findViewById(R.id.textConsolSniffer);
        outputMessage = findViewById(R.id.editSendMessage);

        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = outputMessage.getText().toString();
                write(message);
                updateConsoleMessage("TX: "+ message);
            }
        });

        stopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endServer();
            }
        });

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadReceiver, filter);

        startAdvertising();
        startServer();
    }

    private BroadcastReceiver broadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopAdvertising();
                    stopServer();
                    break;
                default:
                    //nothing
            }
        }
    };

    protected void endServer() {
        stopServer();
        stopAdvertising();
        finish();
    }

    void updateConsoleMessage(String message) {
        console.append(message+"\n");
    }

    void rxMessageReceived(byte data[]) {
        updateConsoleMessage(DeviceBluetoothService.translateMessage("RX: ", data));
    }

    void write(String data) {
        try {
            write(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    void write(byte data[]) {
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
                .setTimeout(0)
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
        bleServer = bleManager.openGattServer(this, serverCb);
        if(bleServer == null) {
            Toast.makeText(this, "Could not start bleServer", Toast.LENGTH_LONG).show();
            return false;
        }
        // Recreates the service for the server
        bleService = new BluetoothGattService(BLE_XFER_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        bleServerChar = getNewCharacteristic();
        bleServerChar.addDescriptor(new BluetoothGattDescriptor(BLE_CLIENT_DESCRIPTOR,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        bleService.addCharacteristic(bleServerChar);

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
                updateConsoleMessage("Connected to device "+ device.getName());
                connected.set(true);
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected.set(false);
                updateConsoleMessage("Disconnected device "+ device.getName());
                endServer();
            }

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            updateConsoleMessage("Device " + device.getName() + " attempted to write to the server");
            if(bleClient.equals(device)) {
                if (BLE_XFER_CHARACTERISTIC.equals(characteristic.getUuid())) {
                    System.out.println("Data was received properly");
                    rxMessageReceived(value);
                }
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            updateConsoleMessage("Device "+ device.getName() + "requested to read at description");
            if(BLE_CLIENT_DESCRIPTOR.equals(descriptor.getUuid())) {
                byte answer[] = device.equals(bleClient) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

                bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, answer);
            } else {
                updateConsoleMessage("Device description does not match expected");
                bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            // Makes sure that the right service can request to subscribe to the server
            updateConsoleMessage("Device "+ device.getName() + "requested to write at description");
            if(BLE_CLIENT_DESCRIPTOR.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    updateConsoleMessage("Added device as client device");
                    bleClient = device;
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    updateConsoleMessage("Removed device as client device");
                    bleClient = null;
                }

                if (responseNeeded) {
                    bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            } else {
                updateConsoleMessage("Device description does not match expected");
                if (responseNeeded) {
                    bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }

        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            updateConsoleMessage("Notification was successfully sent");
        }
    };

}
