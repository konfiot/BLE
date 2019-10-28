package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.UUID;

public class ConsoleActivity extends Activity {

    static DeviceBluetoothService bleService, classicService;

    static TextView console;

    static MainActivity mainContext;

    static BluetoothAdapter btAdapter;

    static AcceptThread classicServer;

    static BluetoothGattServer bleServer;

    static ConsolType currentConsoleType = ConsolType.BRIDGE;

    EditText transferConsol;
    Button sendToClassic;
    Button sendToBle;

    private static BluetoothServiceStateChange callback = new BluetoothServiceStateChange() {

        @Override
        public void serviceStopped() {
            bleService.endService();
            classicService.endService();
        }

        @Override
        public void dataSent(String data) {
            writeToConsole(data);
        }

        @Override
        public void dataReceived(String data) {
            writeToConsole(data);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bridge_view);

        if(currentConsoleType == ConsolType.CLASSIC_SERVER) {
            classicServer = new AcceptThread(btAdapter, mainContext, new BluetoothDataReception() {
                @Override
                public void bluetoothDataReceptionCallback(byte[] data) {
                    writeToConsole(DeviceBluetoothService.translateMessage("RX: ", data));
                }
            });
        }

        sendToBle = findViewById(R.id.buttonSendBLE);
        sendToBle.setVisibility(currentConsoleType == ConsolType.CLASSIC_SERVER? View.INVISIBLE : View.VISIBLE);
        sendToBle.setEnabled(currentConsoleType == ConsolType.BRIDGE);
        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentConsoleType == ConsolType.BRIDGE) {
                    bleService.sendDataToDevice(transferConsol.getText().toString());
                } else {
                    writeToConsole("TX: " + transferConsol.getText().toString());
                }
                transferConsol.setText("");
            }
        });

        sendToClassic = findViewById(R.id.buttonSendClassic);
        sendToClassic.setVisibility(currentConsoleType == ConsolType.BLE_SERVER? View.INVISIBLE : View.VISIBLE);
        sendToClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentConsoleType == ConsolType.BRIDGE) {
                    classicService.sendDataToDevice(transferConsol.getText().toString());
                } else {
                    writeToConsole("TX: " + transferConsol.getText().toString());
                    classicServer.write(transferConsol.getText().toString().getBytes());
                }
                transferConsol.setText("");
            }
        });
        transferConsol = findViewById(R.id.editSendMessage);

    }

    static void writeToConsole(String outputString) {
        console.append(outputString);
    }

    static void passBLEConfiguration(MainActivity mainActivity, BluetoothGattServer server) {
        mainContext = mainActivity;
        bleServer = server;
        currentConsoleType = ConsolType.BLE_SERVER;
    }

    static void passBridgeConfig(MainActivity context, BleService leService, BClassicService clService) {
        mainContext = context;
        bleService = leService;
        bleService.setServiceCallback(callback);
        classicService = clService;
        classicService.setServiceCallback(callback);
        currentConsoleType = ConsolType.BRIDGE;
    }

    static void passClassicServerConfig(MainActivity context, BluetoothAdapter adapter) {
        mainContext = context;
        btAdapter = adapter;
        currentConsoleType = ConsolType.CLASSIC_SERVER;
    }

    public void returnToMainActivity(View view) {
        switch(currentConsoleType) {
            case BLE_SERVER:

                break;
            case CLASSIC_SERVER:
                classicServer.cancel();
                btAdapter = null;
                break;
            case BRIDGE:
                bleService.endService();
                classicService.endService();
                bleService.changeContext(mainContext);
                classicService.changeContext(mainContext);
                break;
        }
        finish();
    }

    public static BluetoothGattServerCallback bluetoothGattServerCallback= new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//            Toast.makeText(thisActivity, "BLE Received data : " + new String(value), Toast.LENGTH_SHORT).show();
            writeToConsole(DeviceBluetoothService.translateMessage("RX: ", value));
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, UUID.randomUUID().toString().getBytes());
        }
    };
}
