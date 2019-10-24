package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    BleService bleService;
    BClassicService bclassicService;
    DeviceBluetoothDetector detector;
    BluetoothAdapter btAdapter;
    MainActivity thisActivity;
    static int REQUEST_ENABLE_BT = 1;
    static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;

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

        bleService = new BleService(this);
        bclassicService = new BClassicService(this);
        detector = new DeviceBluetoothDetector(this, new DeviceBluetoothService[]{bleService, bclassicService});
    }
}
