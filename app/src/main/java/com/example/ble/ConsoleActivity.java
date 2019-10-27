package com.example.ble;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class ConsoleActivity extends Activity {

    static DeviceBluetoothService bleService, classicService;

    static TextView console;

    static Context mainContext;

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

        sendToBle = findViewById(R.id.buttonSendBLE);
        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleService.sendDataToDevice(transferConsol.getText().toString());
            }
        });
        sendToBle.setVisibility(classicService == null ? View.INVISIBLE : View.VISIBLE);

        sendToClassic = findViewById(R.id.buttonSendClassic);
        sendToClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                classicService.sendDataToDevice(transferConsol.getText().toString());
            }
        });
        sendToClassic.setVisibility(classicService == null ? View.INVISIBLE : View.VISIBLE);
        transferConsol = findViewById(R.id.editSendMessage);

    }

    static void writeToConsole(String outputString) {
        console.append(outputString);
    }

    static void passMainActivityContext(Context context) {
        mainContext = context;
    }

    static void passBleService(BleService service) {
        if(bleService == null || !bleService.equals(service)) {
            bleService = service;
            bleService.setServiceCallback(callback);
        }
    }

    static void passClassicService(BClassicService service) {
        if (classicService == null || !classicService.equals(service)){
            classicService = service;
            classicService.setServiceCallback(callback);
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
