package com.example.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BleService bleService;
    BClassicService bclassicService;
    DeviceBluetoothDetector detector;
    BluetoothAdapter btAdapter;
    MainActivity thisActivity;
    BluetoothManager bluetoothManager;
    final static int REQUEST_ENABLE_BT = 1;
    static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;

    public final static UUID characteristicUUID = UUID.fromString("af20fbac-2518-4998-9af7-af42540731b3");

    BluetoothGattServer server;

    ArrayList<String> bleItems=new ArrayList<String>();
    ArrayList<BluetoothDevice> bleDevices=new ArrayList<BluetoothDevice>();
    ArrayAdapter<String> bleadapter;

    ArrayList<String> bclassicItems=new ArrayList<String>();
    ArrayList<BluetoothDevice> bclassicDevices=new ArrayList<BluetoothDevice>();
    ArrayAdapter<String> bclassicadapter;

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

    public void addBle(BluetoothDevice device){
        bleItems.add(device.getName() + " - " + device.getAddress());
        bleDevices.add(device);
        bleadapter.notifyDataSetChanged();
    }

    public void addClassic(BluetoothDevice device){
        bclassicItems.add(device.getName() + " - " + device.getAddress());
        bclassicDevices.add(device);
        bclassicadapter.notifyDataSetChanged();
    }

    public void startClassicServer(View view){
        this.detector.scanForDevices(false);
        ConsoleActivity.passClassicServerConfig(this, btAdapter);
        Toast.makeText(this, "Starting classic server", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ConsoleActivity.class);
        startActivity(intent);
    }

    public void startBleServer(View view){
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
        this.startActivity(discoverableIntent);

        server = bluetoothManager.openGattServer(this, ConsoleActivity.bluetoothGattServerCallback);

        BluetoothGattService service = new BluetoothGattService(UUID.fromString("f6ec37db-bda1-46ec-a43a-6d86de88561d"), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString("af20fbac-2518-4998-9af7-af42540731b3"),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(characteristic);

        server.addService(service);

        ConsoleActivity.passBLEConfiguration(this, server);

        Intent intent = new Intent(this, ConsoleActivity.class);
        startActivity(intent);
    }

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


        thisActivity = this;

        bleadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                bleItems);
        ((ListView)findViewById(R.id.listViewBle)).setAdapter(bleadapter);
        ((ListView)findViewById(R.id.listViewBle)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                BluetoothDevice deviceBle = bleDevices.get(position);
                if (deviceBle.getUuids() == null){
                    Toast.makeText(thisActivity, "Device " + deviceBle.getName() + " - " + deviceBle.getAddress() + " Has no discovered services, can't connect", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(thisActivity, "Connecting to " + deviceBle.getName() + " - " + deviceBle.getAddress() + " " + deviceBle.getUuids(), Toast.LENGTH_SHORT).show();
                BluetoothGatt bluetoothGatt = deviceBle.connectGatt(thisActivity, false, BleService.bleCallback);
                bleService.setRxBehavior(bclassicService);
                bleService.addBluetoothCommunicationHandler(bluetoothGatt);
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
                BluetoothDevice device = bclassicDevices.get(position);
                if (device.getUuids() == null){
                    Toast.makeText(thisActivity, "Device " + device.getName() + " - " + device.getAddress() + " Has no discovered services, can't connect", Toast.LENGTH_SHORT).show();
                    return;
                }

                ConnectThread connectThread = new ConnectThread(device, device.getUuids()[0], bleService);
                bclassicService.addBluetoothCommunicationHandler(connectThread);
                connectThread.start();
                Toast.makeText(thisActivity, "Connecting to " + device.getName() + " - " + device.getAddress() + " " + device.getUuids(), Toast.LENGTH_SHORT).show();

            }
        });

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
        ConsoleActivity.passBridgeConfig(this, bleService, bclassicService);
        Intent intent = new Intent(this, ConsoleActivity.class);
        startActivity(intent);
    }
}

