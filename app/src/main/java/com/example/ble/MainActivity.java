package com.example.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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

public class MainActivity extends AppCompatActivity {

    BleService bleService;
    BClassicService bclassicService;
    DeviceBluetoothDetector detector;
    BluetoothAdapter btAdapter;
    MainActivity thisActivity;
    final static int REQUEST_ENABLE_BT = 1;
    static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;

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
        new AcceptThread(btAdapter, this).start();
        Toast.makeText(this, "Starting classic server", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thisActivity = this;

        bleadapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                bleItems);
        ((ListView)findViewById(R.id.listViewBle)).setAdapter(bleadapter);
        ((ListView)findViewById(R.id.listViewBle)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                BluetoothDevice device = bleDevices.get(position);
                if (device.getUuids() == null){
                    Toast.makeText(thisActivity, "Device " + device.getName() + " - " + device.getAddress() + " Has no discovered services, can't connect", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(thisActivity, "Connecting to " + device.getName() + " - " + device.getAddress() + " " + device.getUuids(), Toast.LENGTH_SHORT).show();
                BluetoothGatt bluetoothGatt = device.connectGatt(thisActivity, false, new BluetoothGattCallback() {
                    @Override
                    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                    }

                    @Override
                    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                        super.onPhyRead(gatt, txPhy, rxPhy, status);
                    }

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(gatt, characteristic, status);
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);
                    }

                    @Override
                    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorRead(gatt, descriptor, status);
                    }

                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorWrite(gatt, descriptor, status);
                    }

                    @Override
                    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                        super.onReliableWriteCompleted(gatt, status);
                    }

                    @Override
                    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                        super.onReadRemoteRssi(gatt, rssi, status);
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        super.onMtuChanged(gatt, mtu, status);
                    }
                });

            }
        });


        bclassicadapter=new ArrayAdapter<String>(this,
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
                ConnectThread connect = new ConnectThread(device, device.getUuids()[0]);
                connect.start();
                Toast.makeText(thisActivity, "Connecting to " + device.getName() + " - " + device.getAddress() + " " + device.getUuids(), Toast.LENGTH_SHORT).show();

            }
        });


        bleService = new BleService(this);
        bclassicService = new BClassicService(this);
        detector = new DeviceBluetoothDetector(this, new DeviceBluetoothService[]{bleService, bclassicService});
        if (checkLocationPermission()) {
            detector.scanForDevices(true);
        }
    }
}

