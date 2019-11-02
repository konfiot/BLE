package com.example.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.widget.Toast;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BleService extends DeviceBluetoothService {

    private static BluetoothDataReception rxBehavior;
    private static BleService service;

    private static LinkedBlockingQueue<Boolean> serviceResponceCheck;

    BluetoothGatt comHandler;
    BluetoothGattCharacteristic bleCharact = new BluetoothGattCharacteristic(UUID.fromString("af20fbac-2518-4998-9af7-af42540731b3"),
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

    BleService(Context context) {
        super(context);
        comHandler = null;
        rxBehavior = null;
        service = this;
        serviceResponceCheck = new LinkedBlockingQueue<Boolean>(1) ;
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
        comHandler.close();
        super.endService();
    }

    @Override
    public void setRxBehavior(BluetoothDataReception behavior) {
        rxBehavior = behavior;
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

    @Override
    public void bluetoothConnectionChanged(boolean connected) {
        if(!connected) {

        }
    }


    public boolean correctDevice() {
        comHandler.discoverServices();
        try {
            Boolean good = serviceResponceCheck.poll(2, TimeUnit.SECONDS);
            if(good != null && good) {
                BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(BleServerMode.BLE_XFER_SERVICE,
                        BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                comHandler.writeDescriptor(descriptor);
            }
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static BluetoothGattCallback bleCallback =  new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                Toast.makeText(service.context, "Connected to GATT server.", Toast.LENGTH_LONG).show();
            } else if( newState == BluetoothProfile.STATE_DISCONNECTED) {
                Toast.makeText(service.context, "Disconnected from GATT server", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            for( BluetoothGattService service : gatt.getServices() ) {
                if(BleServerMode.BLE_XFER_SERVICE.equals(service.getUuid())) {
                    serviceResponceCheck.add(true);
                    return;
                }
            }
            serviceResponceCheck.add(false);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //  super.onCharacteristicWrite(gatt, characteristic, status);
            rxBehavior.bluetoothDataReceptionCallback(characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor
        descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if()
        }
    };
}
