package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    BleService bleService;
    DeviceBluetoothDetector detector;
    BluetoothAdapter btAdapter;
    MainActivity thisActivity;
    private static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;

    private ScanCallback scnCB;

    MainActivity() {

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, R.string.blc_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(thisActivity != null) {
            thisActivity = new MainActivity();
        }

        if(btAdapter == null) {
            Toast.makeText(this, R.string.bl_adapter_not_found, Toast.LENGTH_LONG).show();
        }
        if(!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        bleService = new BleService(this);
        detector = new DeviceBluetoothDetector(this, new DeviceBluetoothService[]{bleService}, btAdapter);
    }
}
