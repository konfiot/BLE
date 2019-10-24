package com.example.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private final MainActivity activity;

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream


    public AcceptThread(BluetoothAdapter btAdapter, MainActivity activity) {
        this.activity = activity;
        BluetoothServerSocket tmp = null;

        try {
            tmp = btAdapter.listenUsingRfcommWithServiceRecord("Bluetooth Server", UUID.randomUUID());
        } catch (IOException e) {
            Log.e("Server", "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;


    }

    public void run() {
        BluetoothSocket socket = null;
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
        activity.startActivity(discoverableIntent);

        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e("Server", "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e("Server", "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e("Server", "Error occurred when creating output stream", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;


                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e("Server", "Could not close the connect socket", e);
        }
    }

    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

        } catch (IOException e) {
            Log.e("Server", "Error occurred when sending data", e);
        }
    }

}