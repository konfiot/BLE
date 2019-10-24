package com.example.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
                detector.scanForDevices(false); // Stop discovery
                Toast.makeText(thisActivity, "Connecting to " + device.getName() + " - " + device.getAddress() + " " + device.getUuids(), Toast.LENGTH_SHORT).show();
                ConnectThread connect = new ConnectThread(device, device.getUuids()[0]);
                connect.start();
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
                detector.scanForDevices(false); // Stop discovery
                Toast.makeText(thisActivity, "Connecting to " + device.getName() + " - " + device.getAddress() + " " + device.getUuids(), Toast.LENGTH_SHORT).show();
                ConnectThread connect = new ConnectThread(device, device.getUuids()[0]);
                connect.start();
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

