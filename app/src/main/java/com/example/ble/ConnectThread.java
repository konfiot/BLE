package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectThread extends Thread {
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    final String TAG = "Client";
    final ParcelUuid UUID;
    private InputStream mmInStream;
    private DeviceBluetoothService callback;
    private OutputStream mmOutStream;

    public ConnectThread(BluetoothDevice device, ParcelUuid UUID, DeviceBluetoothService callback) {
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.UUID = UUID;
        this.callback = callback;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(UUID.getUuid());
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        Log.i(TAG, "Connecting");
        byte[] read = new byte[32];
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            Log.i(TAG, "Connected");

            mmSocket = mmSocket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e("Server", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e("Server", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            while (true){
                tmpIn.read(read, 0, 32);
                callback.sendDataToDevice(read.toString());
            }

        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
                Log.e(TAG, "Error Connecting", connectException);
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.

    }

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

        } catch (IOException e) {
            Log.e("Server", "Error occurred when sending data", e);
        }
    }


    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
