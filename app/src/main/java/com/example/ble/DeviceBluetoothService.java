package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DeviceBluetoothService extends Thread implements BluetoothDataReception {

    protected Context context;

    protected LinkedBlockingQueue<byte[]> txQueue;

    protected ArrayList<BluetoothDevice> btDevices;

    protected AtomicBoolean appIsRunning;

    protected BluetoothServiceStateChange serviceCB;

    protected DeviceBluetoothService(Context context) {
        txQueue = new LinkedBlockingQueue<>();
        btDevices = new ArrayList<>();
        appIsRunning = new AtomicBoolean(false);
    }

    public void changeContext(Context newContext) {
        context = newContext;
    }

    public void setServiceCallback(BluetoothServiceStateChange serviceCB) {
        this.serviceCB = serviceCB;
    }

    protected void sendDataToDevice(String data) {
        sendDataToDevice(data.getBytes());
    }

    abstract protected void sendDataToDevice(byte data[]);

    abstract protected boolean serviceIsActive();

    abstract public void addBluetoothCommunicationHandler(Object comHandler);

    public void endService(){
        if(isAlive()) {
            appIsRunning.set(false);
            try {
                join();
            } catch (InterruptedException e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void run() {
        appIsRunning.set(true);
        byte txMessage[];
        while(appIsRunning.get()) {
            try {
                txMessage = txQueue.poll(100, TimeUnit.MILLISECONDS);
                if(txMessage != null) {
                    sendDataToDevice(txMessage);
                }
            } catch (InterruptedException e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                appIsRunning.set(false);

            }
        }
        serviceCB.serviceStopped();
    }

    public void addDevice(BluetoothDevice device) {
        if(btDevices.contains(device)) {
            btDevices.add(device);
        }
    }

    protected String translateMessage(String initialPart, byte[] data) {
        StringBuilder builder = new StringBuilder(initialPart);
        try {
            builder.append(new String(data, Charset.defaultCharset()));
        } catch(Exception e) {
            builder.append(data.toString());
        }
        return builder.toString();
    }
}
