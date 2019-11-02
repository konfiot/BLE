package com.example.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 1;
    static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    static final int DETECTION_TIMEOUT = 600; // ms

    // Bridge service providers
    BleService bleService;
    BClassicService bclassicService;
    // Bridge bluetooth detector
    DeviceBluetoothDetector detector;

    // Ble server mode param
    BleServerMode serverMode;

    // General  local parameters
    BluetoothAdapter btAdapter;
    MainActivity thisActivity;
    BluetoothManager bluetoothManager;

    // This is to display the list of devices available to the bridge
    ArrayList<String> bleItems=new ArrayList<String>();
    ArrayAdapter<String> bleadapter;
    ArrayList<String> bclassicItems=new ArrayList<String>();
    ArrayAdapter<String> bclassicadapter;

    private TextView bleSelected, classicSelected;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(thisActivity, R.string.bt_disabled, Toast.LENGTH_SHORT).show();
                thisActivity.finish();
            }
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        detector.scanForDevices(false);

    }

    @Override
    public void onPause(){
        super.onPause();
        detector.scanForDevices(false);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        detector.scanForDevices(false);
    }

    protected boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ENABLE_BT);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    detector.scanForDevices(true);
                } else {
                    //TODO: re-request
                }
                break;
            }
        }
    }

    public BluetoothDeviceListListener addBle = new BluetoothDeviceListListener() {

        @Override
        public void updateDeviceList(List<String> deviceKey) {
            bleItems.clear();
            bleItems.addAll(deviceKey);
            bleadapter.notifyDataSetChanged();
        }
    };
    public BluetoothDeviceListListener addClassic = new BluetoothDeviceListListener() {

        @Override
        public void updateDeviceList(List<String> deviceKey) {
            bclassicItems.clear();
            bclassicItems.addAll(deviceKey);
            bclassicadapter.notifyDataSetChanged();
        }
    };

    public void startClassicServer(View view){
        this.detector.scanForDevices(false);
        ConsoleActivity.passClassicServerConfig(this, btAdapter);
        Toast.makeText(this, "Starting classic server", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ConsoleActivity.class);
        startActivity(intent);
    }

    public void startBleServer(View view){

        if(serverMode == null) {
            serverMode = new BleServerMode(this, btAdapter);
        }

        ConsoleActivity.passBLEConfiguration(this, serverMode);

        Intent intent = new Intent(this, ConsoleActivity.class);
        startActivity(intent);
    }

    private BluetoothDevice bleDevice, bclassicDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, R.string.blc_not_supported, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(this, "No bluetooth adapter has been found", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        bleService = new BleService(this);

        bclassicService = new BClassicService(this);
        bleDevice = null;
        bclassicDevice = null;

        thisActivity = this;

        bleSelected = findViewById(R.id.textBleSelected);
        classicSelected = findViewById(R.id.testClassicSelected);

        bleadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                bleItems);
        ((ListView)findViewById(R.id.listViewBle)).setAdapter(bleadapter);
        ((ListView)findViewById(R.id.listViewBle)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                String key = bleItems.get(position);
                bleDevice = bleService.getChosenDevice(key);
                bleSelected.setText("BLE device selected: " + key);
            }
        });

        bclassicadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                bclassicItems);
        ((ListView)findViewById(R.id.listViewBClassic)).setAdapter(bclassicadapter);
        ((ListView)findViewById(R.id.listViewBClassic)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                String key = bclassicItems.get(position);
                bclassicDevice = bclassicService.getChosenDevice(key);
                classicSelected.setText("Classic device selected: " + key);
            }
        });

        bleService.setBluetoothAdapterList(addBle);
        bclassicService.setBluetoothAdapterList(addClassic);

        bluetoothManager = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);

        detector = new DeviceBluetoothDetector(this, btAdapter, new DeviceBluetoothService[]{bleService, bclassicService});
        if (checkLocationPermission()) {
            detector.scanForDevices(true);
        }
    }

    public void restartDetection(View view) {
        if (checkLocationPermission()) {
            detector.scanForDevices(false);
            detector.scanForDevices(true);
        }
    }

    public void startBridgeMode(View view) {
//        ConsoleActivity.passBridgeConfig(this, bleService, bclassicService);
        if(ConsoleActivity.newBridgePassConfig(this, bleDevice, bclassicDevice, bleService, bclassicService)) {
            Intent intent = new Intent(this, ConsoleActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.unable_to_start_bridge, Toast.LENGTH_LONG).show();
        }
    }
}

