package com.example.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.widget.Toast;

public class BleService extends DeviceBluetoothService {

    BluetoothGatt comHandler;
    BluetoothGattCharacteristic bleCharact;

    BleService(Context context) {
        super(context);
        comHandler = null;
    }

    public void addBLECharactarestic(BluetoothGattCharacteristic bleCharact) {
        this.bleCharact = bleCharact;
    }

    @Override
    protected void sendDataToDevice(byte data[]) {
        bleCharact.setValue(data);
        comHandler.writeCharacteristic(bleCharact);
        serviceCB.dataSent(translateMessage("BLE TX: ", data));
    }

    @Override
    protected boolean serviceIsActive() {
        return comHandler != null;
    }

    @Override
    public void addBluetoothCommunicationHandler(Object comHandler) {
        if(comHandler instanceof BluetoothGatt) {
            this.comHandler = (BluetoothGatt) comHandler;
        } else {
            Toast.makeText(context, R.string.incorret_hanlder_passed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void endService() {
        //TODO: Add special end fo the service
        super.endService();
    }

    @Override
    public void bluetoothDataReceptionCallback(byte[] data) {
        serviceCB.dataReceived(translateMessage("Classic RX: ", data));
        int size = data.length;
        byte temp[];
        while(size > 32) {
            temp = new byte[32];
            for(int i = 0; i < 32; i++) {
                temp[i] = data[i];
            }
            txQueue.add(temp);
            temp = new byte[size - 32];
            for(int i = 32; i < size; i++) {
                temp[i-32] = data[i];
            }
            data = temp;
            size = data.length;
        }
        txQueue.add(data);
    }
}
