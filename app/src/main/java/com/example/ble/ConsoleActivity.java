package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ConsoleActivity extends Activity {

    static DeviceBluetoothService bleService, classicService;

    static TextView console;

    static MainActivity mainContext;

    static BluetoothAdapter btAdapter;

    static AcceptThread classicServer;

    static BleServerMode bleServer;

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

        switch(currentConsoleType) {
            case CLASSIC_SERVER:
                classicServer = new AcceptThread(btAdapter, mainContext, deviceCommCallbackWhenMissingOtherHalf);
                classicServer.start();
                break;
            case BRIDGE:
                classicService.start();
                bleService.start();
                break;
            case BLE_SERVER:
                bleServer.setRxCallback(deviceCommCallbackWhenMissingOtherHalf);
                bleServer.startServer();
                bleServer.startAdvertising();
//                Intent discoverableIntent =
//                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
//                this.startActivity(discoverableIntent);
                break;
        }

        sendToBle = findViewById(R.id.buttonSendBLE);
        sendToBle.setVisibility(currentConsoleType == ConsolType.CLASSIC_SERVER? View.INVISIBLE : View.VISIBLE);
        sendToBle.setEnabled(currentConsoleType == ConsolType.BRIDGE && bleService.serviceIsActive());
        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentConsoleType == ConsolType.BRIDGE) {
                    bleService.sendDataToDevice(transferConsol.getText().toString());
                } else {
                    writeToConsole("TX: " + transferConsol.getText().toString());
                    bleServer.writeDataToDevice(transferConsol.getText().toString());
                }
                transferConsol.setText(R.string.out_put_message);
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
                transferConsol.setText(R.string.out_put_message);
            }
        });
        transferConsol = findViewById(R.id.editSendMessage);

    }

    static void writeToConsole(String outputString) {
        console.append(outputString);
    }

    static void passBLEConfiguration(MainActivity mainActivity, BleServerMode server) {
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
        if(!bleService.serviceIsActive()) {
            classicService.setRxBehavior(deviceCommCallbackWhenMissingOtherHalf);
        }
        if(!classicService.serviceIsActive()) {
            bleService.setRxBehavior(deviceCommCallbackWhenMissingOtherHalf);
        }
    }

    static boolean newBridgePassConfig(MainActivity mainActivity, BluetoothDevice bleDevice, BluetoothDevice classicDevice, BleService leService, BClassicService clService) {
        if(bleDevice != null) {
            if (bleDevice.getUuids() == null) {
                Toast.makeText(mainActivity, "Device " + bleDevice.getName() + " - " + bleDevice.getAddress() + " Has no discovered services, can't connect", Toast.LENGTH_SHORT).show();
                return false;
            }
            Toast.makeText(mainActivity, "Connecting to " +  bleDevice.getName() + " - " + bleDevice.getAddress()  + " " + bleDevice.getUuids(), Toast.LENGTH_SHORT).show();
            BluetoothGatt bluetoothGatt = bleDevice.connectGatt(mainActivity, false, BleService.bleCallback);
            if(classicDevice != null) {
                leService.setRxBehavior(clService);
            } else {
                leService.setRxBehavior(deviceCommCallbackWhenMissingOtherHalf);
            }
            leService.addBluetoothCommunicationHandler(bluetoothGatt);
            if(!leService.correctDevice()) {
                Toast.makeText(mainActivity, R.string.incorrect_ble_connected, Toast.LENGTH_LONG).show();
                return false;
            }
            bleService = leService;
        }
        if(classicDevice != null) {
            if (classicDevice.getUuids() == null){
                Toast.makeText(mainActivity, "Device " + classicDevice.getName() + " - " + classicDevice.getAddress() + " Has no discovered services, can't connect", Toast.LENGTH_SHORT).show();
                return false;
            }

            ConnectThread connectThread;
            if(bleDevice != null) {
                connectThread = new ConnectThread(classicDevice, classicDevice.getUuids()[0], bleService);

            } else {
                connectThread = new ConnectThread(classicDevice, classicDevice.getUuids()[0], deviceCommCallbackWhenMissingOtherHalf);
            }
            clService.addBluetoothCommunicationHandler(connectThread);
            connectThread.start();
            Toast.makeText(mainActivity, "Connecting to " + classicDevice.getName() + " - " + classicDevice.getAddress() + " " + classicDevice.getUuids(), Toast.LENGTH_SHORT).show();
        }
        currentConsoleType = ConsolType.BRIDGE;
        return true;
    }

    static void passClassicServerConfig(MainActivity context, BluetoothAdapter adapter) {
        mainContext = context;
        btAdapter = adapter;
        currentConsoleType = ConsolType.CLASSIC_SERVER;
    }

    public void returnToMainActivity(View view) {
        switch(currentConsoleType) {
            case BLE_SERVER:
                bleServer.stopAdvertising();
                bleServer.stopServer();
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

    public static void endConsoleView() {

    }

    public static BluetoothDataReception deviceCommCallbackWhenMissingOtherHalf = new BluetoothDataReception() {
        @Override
        public void bluetoothDataReceptionCallback(byte[] data) {
            writeToConsole(DeviceBluetoothService.translateMessage("RX: ", data));
        }

        @Override
        public void bluetoothConnectionChanged(boolean connected) {
            Toast.makeText(mainContext, "Bluetooth device was disconnected", Toast.LENGTH_LONG).show();
            endConsoleView();
        }

    };
}
