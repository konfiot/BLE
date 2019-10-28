package com.example.ble;

import android.content.Context;
import android.widget.Toast;

public class BClassicService extends DeviceBluetoothService {

    ConnectThread commHandler;

    BClassicService(Context context) {
        super(context);
        commHandler = null;
    }

    @Override
    protected void sendDataToDevice(byte data[]) {
        commHandler.write(data);
        serviceCB.dataSent(translateMessage("Classic TX: ", data));
    }

    @Override
    protected boolean serviceIsActive() {
        return commHandler != null;
    }

    @Override
    public void addBluetoothCommunicationHandler(Object comHandler) {
        if(comHandler instanceof ConnectThread) {
            commHandler = (ConnectThread) comHandler;
        } else {
            Toast.makeText(context, R.string.incorret_hanlder_passed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void endService()  {
        if(commHandler != null) {
            commHandler.cancel();
        }
        super.endService();
    }

    @Override
    public void bluetoothDataReceptionCallback(byte[] data) {
        serviceCB.dataReceived(translateMessage("Classic RX: ",data));
        txQueue.add(data);
    }
}
