package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
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

    static ClassicServerMode classicServer;

    static ConsolType currentConsoleType = ConsolType.BRIDGE;

    static ConsoleActivity consoleActivity;

    EditText transferConsol;
    Button sendToClassic;
    Button sendToBle, returnToMain;

    private static BluetoothServiceStateChange callback = new BluetoothServiceStateChange() {

        @Override
        public void serviceStateChange(boolean isConnected) {
            if(isConnected) {
            } else {
                bleService.endService();
                classicService.endService();
            }
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

        consoleActivity = this;
        console = findViewById(R.id.textConsolSniffer);
        console.setText("Start of interactions\n");

        returnToMain = findViewById(R.id.stopButton);
        returnToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endConsoleView();
            }
        });

        switch(currentConsoleType) {
            case BRIDGE:
                bleService.setRxBehavior(classicService);
                bleService.setServiceCallback(callback);
                classicService.setRxBehavior(bleService);
                classicService.setServiceCallback(callback);
                bleService.changeContext(this);
                classicService.changeContext(this);
                bleService.start();
                classicService.start();
                break;
            case BRIDGE_BLE_ONLY:
                bleService.setRxBehavior(deviceCommCallbackWhenMissingOtherHalf);
                bleService.setServiceCallback(callback);
                bleService.changeContext(this);
                bleService.start();
                break;
            case BRIDEG_CLC_ONLY:
                classicService.setRxBehavior(deviceCommCallbackWhenMissingOtherHalf);
                classicService.changeContext(this);
                classicService.start();
                break;
        }

        sendToBle = findViewById(R.id.buttonSendBLE);
        sendToBle.setVisibility(currentConsoleType == ConsolType.CLASSIC_SERVER? View.INVISIBLE : View.VISIBLE);
        sendToBle.setEnabled((currentConsoleType.getFlagValue() & ConsolType.BLE_SERVER.getFlagValue()) != 0);
        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(currentConsoleType) {
                    case BRIDGE_BLE_ONLY:
                        writeToConsole("TX: " + transferConsol.getText().toString());
                    case BRIDGE:
                        bleService.sendDataToDevice(transferConsol.getText().toString());
                        break;
                    default:
                        // Do nothing
                }
                transferConsol.setText("");
            }
        });

        sendToClassic = findViewById(R.id.buttonSendClassic);
        sendToClassic.setVisibility(currentConsoleType == ConsolType.BLE_SERVER? View.INVISIBLE : View.VISIBLE);
        sendToClassic.setEnabled((currentConsoleType.getFlagValue() & ConsolType.CLASSIC_SERVER.getFlagValue()) != 0);
        sendToClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(currentConsoleType) {
                    case BRIDEG_CLC_ONLY:
                        writeToConsole("TX: " + transferConsol.getText().toString());
                    case BRIDGE:
                        classicService.sendDataToDevice(transferConsol.getText().toString());
                        break;
                    default:
                        // Do nothing
                }
                transferConsol.setText("");
            }
        });
        transferConsol = findViewById(R.id.editSendMessage);
    }

    static void writeToConsole(String outputString) {
        if(console != null) {
            console.append(outputString + "\n");
        } else {
            Toast.makeText(consoleActivity, "Could not write to console", Toast.LENGTH_LONG).show();
        }
    }

    static void newBridgePassConfig(MainActivity mainActivity, BleService leService, BClassicService clService, ConsolType consolType) {
        mainContext = mainActivity;
        bleService = leService;
        classicService = clService;
        currentConsoleType = consolType;
    }

    public void endConsoleView() {
        switch(currentConsoleType) {
            case BRIDGE:
                bleService.endService();
                classicService.endService();
                bleService.changeContext(mainContext);
                classicService.changeContext(mainContext);
                break;
            case BRIDEG_CLC_ONLY:
                classicService.endService();
                classicService.changeContext(mainContext);
                break;
            case BRIDGE_BLE_ONLY:
                bleService.endService();
                bleService.changeContext(mainContext);
                break;
        }
        finish();
    }

    public BluetoothDataReception deviceCommCallbackWhenMissingOtherHalf = new BluetoothDataReception() {
        @Override
        public void bluetoothDataReceptionCallback(byte[] data) {
            writeToConsole(DeviceBluetoothService.translateMessage("RX: ", data));
        }

        @Override
        public void bluetoothConnectionChanged(BluetoothDevice device, boolean connected) {
            StringBuilder message = new StringBuilder("Bluetooth device ");
            if(connected) {
                message.append("connection success");
            } else {
                message.append("was disconnected");
            }
            Toast.makeText(consoleActivity, message.toString(), Toast.LENGTH_LONG).show();
            endConsoleView();
        }
    };
}
