package com.example.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClassicServerMode extends Activity {

    public static final UUID SERIAL_SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID

    private boolean running;
    private int readBufferPosition;
    private byte[] readXfer;
    private StringBuilder rxBuilder;

    private BluetoothSocket mmSocket;
    BluetoothAdapter btAdapter;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private BluetoothDataReception rxCalback;


    private Thread rxListeningThread;
    Button sendToClassic, stopServer;
    TextView console;
    EditText outputMessage;
    AtomicBoolean connected;
    ClassicServerMode activity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bridge_view);

        activity = this;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connected = new AtomicBoolean(false);

        Button sendToBle = findViewById(R.id.buttonSendBLE);
        sendToBle.setText("START SERVER");
        sendToBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    openServerSocket();
                } catch(Exception e) {
                    e.printStackTrace();
                    endServer();
                }
            }
        });


        stopServer = findViewById(R.id.stopButton);
        sendToClassic = findViewById(R.id.buttonSendClassic);
        console = findViewById(R.id.textConsolSniffer);
        outputMessage = findViewById(R.id.editSendMessage);

        sendToClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = outputMessage.getText().toString();
                write(message);
                updateConsoleMessage("TX: "+ message);
            }
        });

        stopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endServer();
            }
        });

        startDiscovery();

    }
    protected void endServer() {
        try {
            if(connected.get()) {
                if(running) {
                    running = false;
                    rxListeningThread.join();
                }
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
                connected.set(false);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        finish();
    }

    void updateConsoleMessage(String message) {
        console.append(message+"\n");
    }

    void startDiscovery() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
        activity.startActivity(discoverableIntent);
    }

    void openServerSocket() throws Exception {
//        startDiscovery();
        final BluetoothServerSocket socket = btAdapter.listenUsingRfcommWithServiceRecord("bt_classic_server", SERIAL_SERVICE_UUID);
        mmSocket = socket.accept();
        socket.close();

        btAdapter.cancelDiscovery();
        if(mmSocket != null) {
            connected.set(true);
            mmOutStream = mmSocket.getOutputStream();
            mmInStream = mmSocket.getInputStream();
            startListening();
        } else {
            updateConsoleMessage("Could not open classic client socket");
        }
    }


    void startListening() {
        running = true;
        readXfer = new byte[1024];
        readBufferPosition = 0;
        rxListeningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer msgBuffer;
                while (running) {
                    try {
                        int bytesAvailable = mmInStream.available();
                        if (bytesAvailable > 0) {
                            if ((readBufferPosition + bytesAvailable) > 1024) {
                                bytesAvailable = 1024 - readBufferPosition;
                            }
                            mmInStream.read(readXfer, readBufferPosition, bytesAvailable);

                            msgBuffer = ByteBuffer.allocate(bytesAvailable);

                            for (int i = readBufferPosition; i < readBufferPosition + bytesAvailable; i++) {
                                msgBuffer.put(readXfer[i]);
                            }
                            updateConsoleMessage("RX: " + new String(msgBuffer.array(), "UTF-8"));
                        }
                    } catch (IOException e) {
                        updateConsoleMessage("Reader ended abruptly");
                        break;
                    }


                }
            }
        });
        rxListeningThread.start();
    }

    // Call this from the main activity to send data to the remote device.
    protected void write(String message) {
        try {
            write(message.getBytes("UTF-8"));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    protected void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e("Server", "Error occurred when sending data", e);
        }
    }

}