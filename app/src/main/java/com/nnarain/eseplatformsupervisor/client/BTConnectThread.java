package com.nnarain.eseplatformsupervisor.client;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Natesh on 17/04/2015.
 */
public class BTConnectThread extends Thread {

    /**
     * Connection Status Callback
     * */
    public interface OnConnectListener
    {
        public void onConnect(BluetoothSocket socket, InputStream in, OutputStream out);
        public void onConnectFailed();
    }

    private BluetoothDevice device;
    private OnConnectListener listener;

    private final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BTConnectThread(BluetoothDevice device, OnConnectListener listener)
    {
        this.device = device;
        this.listener = listener;
    }

    @Override
    public void run() {

        super.run();

        final BluetoothSocket socket;

        // Connect to the device and retrieve streams
        try
        {
            socket = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
            socket.connect();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            listener.onConnect(socket, in, out);
        }
        catch(IOException e)
        {
            listener.onConnectFailed();
        }

    }
}
