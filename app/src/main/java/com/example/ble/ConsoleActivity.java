package com.example.ble;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class ConsoleActivity extends Activity {

    static DeviceBluetoothService bleService, classicService;

    static TextView console;

    private static BluetoothDataReception bleReceptor = new BluetoothDataReception() {
        @Override
        public void bluetoothDataReceptionCallback(String messageRX) {
            if(classicService != null) {
                classicService.txQueue.add(messageRX);
            }
            writeToConsole("BLE-RX: "+messageRX+"\n");
        }
    };


    private static BluetoothDataReception classicReceptor = new BluetoothDataReception() {
        @Override
        public void bluetoothDataReceptionCallback(String messageRX) {
            if(bleService != null) {
                bleService.txQueue.add(messageRX);
            }
            writeToConsole("CLASSIC-RX: "+messageRX+"\n");
        }
    };

    EditText transferConsol;
    Button sendToClassic;
    Button sendToBle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bridge_view);

        sendToBle = findViewById(R.id.buttonSendBLE);
        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleService.sendDataToDevice(inputConsol.getText().toString());
            }
        });
        sendToBle.setVisibility(classicService == null ? View.INVISIBLE : View.VISIBLE);

        sendToClassic = findViewById(R.id.buttonSendClassic);
        sendToClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                classicService.sendDataToDevice(inputConsol.getText().toString());
            }
        });
        sendToClassic.setVisibility(classicService == null ? View.INVISIBLE : View.VISIBLE);
        transferConsol = findViewById(R.id.editSendMessage);

    }

    static void writeToConsole(String outputString) {
        console.append(outputString);
    }

    static void passBleService(BleService service) {
        if(bleService == null || !bleService.equals(service)) {
            bleService = service;
            bleService.setDataReceptionCallback(bleReceptor);
        }
    }

    static void passClassicService(BClassicService service) {
        if (classicService == null || !classicService.equals(service)){
            classicService = service;
            classicService.setDataReceptionCallback(classicReceptor);
        }
    }

    static void removeBluetoothService(boolean removeBLE) {
        if(removeBLE) {
            bleService = null;
        } else {
            classicService = null;
        }
    }

    public void returnToMainActivity(View view) {
        finish();
    }

}
