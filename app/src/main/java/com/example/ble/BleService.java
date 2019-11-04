package com.example.ble;

import android.bluetooth.BluetoothDevice;
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
        BluetoothGattCharacteristic txChar = comHandler.getService(BleServerMode.BLE_XFER_SERVICE).getCharacteristic(BleServerMode.BLE_XFER_CHARACTERISTIC);

        txChar.setValue(data);

        if( comHandler.writeCharacteristic(bleCharact) ) {
            if(serviceCB != null) {
                serviceCB.dataSent(translateMessage("BLE TX: ", data));
            } else {
                Toast.makeText(context, "Wrote data to Gatt", Toast.LENGTH_LONG).show();
            }
        } else {
            if(serviceCB != null) {
                serviceCB.dataSent(translateMessage("Unable to send data to BLE: ", data));
            } else {
                Toast.makeText(context, "Could not write data to Gatt", Toast.LENGTH_LONG).show();
            }
        }
    }

//    private List<BluetoothGattService> gattServices;
//    public void setupGattServices() {
//        BluetoothGattService serrvice;
//        serrvice.
//    }

//    @Override
//    public void start() {
//
//        BluetoothGattDescriptor desc = new BluetoothGattDescriptor(BleServerMode.BLE_CLIENT_DESCRIPTOR, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
//        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//        comHandler.writeDescriptor(desc);
//
//        super.start();
//    }

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
        if(comHandler == null) {
            return;
        }
        comHandler.disconnect();
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
    public void bluetoothConnectionChanged(BluetoothDevice device, boolean connected) {
       // Do nothing
    }

    private static boolean deviceConenction;

    public boolean isDeviceConnected() {
        return deviceConenction;
    }

    public static BluetoothGattCallback bleCallback =  new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                deviceConenction = true;
                gatt.discoverServices();
                System.out.println("Connected to device " + gatt.getDevice().getName());
            } else if( newState == BluetoothProfile.STATE_DISCONNECTED) {
                deviceConenction = false;
                rxBehavior.bluetoothConnectionChanged( gatt.getDevice(),false);
                System.out.println("Disconnected from device " + gatt.getDevice().getName());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (BleServerMode.BLE_XFER_SERVICE.equals(service.getUuid())) {
                        BluetoothGattCharacteristic ser_char = service.getCharacteristic(BleServerMode.BLE_XFER_CHARACTERISTIC);
                        if(ser_char != null) {
                            BluetoothGattDescriptor char_desc = ser_char.getDescriptor(BleServerMode.BLE_CLIENT_DESCRIPTOR);
                            if(char_desc != null) {
                                gatt.setCharacteristicNotification(ser_char, true);
                                char_desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(char_desc);
                                return;
                            } else {
                                System.out.println("Could not get description " + gatt.getDevice().getName());
                            }
                        } else {
                            System.out.println("Could not get characteristic " + gatt.getDevice().getName());
                        }

                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            rxBehavior.bluetoothDataReceptionCallback(characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.equals(descriptor.getValue())) {
                    rxBehavior.bluetoothConnectionChanged( gatt.getDevice(),true);
                } else if(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE.equals(descriptor.getValue())){
                    rxBehavior.bluetoothConnectionChanged( gatt.getDevice(), false);
                }
            }
        }
    };
}
