package com.nnarain.eseplatformsupervisor.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.nnarain.eseplatformsupervisor.BTConnectThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Natesh on 18/04/2015.
 */
public class BluetoothPacketStream implements PacketStream {

    private String address;

    private BluetoothSocket socket;
    private InputStream rx = null;
    private OutputStream tx = null;

    private BTConnectThread connectThread;

    private boolean isConnected = false;

    private static final String TAG = BluetoothPacketStream.class.getSimpleName();

    public BluetoothPacketStream(String address)
    {
        this.address = address;
    }

    @Override
    public void connect() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.cancelDiscovery();

        BluetoothDevice device = adapter.getRemoteDevice(address);

        connectThread = new BTConnectThread(device, new BTConnectThread.OnConnectListener()
        {
            @Override
            public void onConnect(BluetoothSocket socket, InputStream in, OutputStream out) {
                BluetoothPacketStream.this.socket = socket;
                BluetoothPacketStream.this.rx = in;
                BluetoothPacketStream.this.tx = out;

                isConnected = true;
            }

            @Override
            public void onConnectFailed() {

            }
        });

        connectThread.start();

    }

    @Override
    public void close() {

        try
        {
            if(socket != null) socket.close();
            if(rx != null)     rx.close();
            if(tx != null)     tx.close();

            if(connectThread != null) connectThread.join();
            connectThread = null;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        isConnected = false;

    }

    @Override
    public void write(Packet packet) throws IOException{

        String s = packet.getContents();

        Log.d(TAG, "Writing: " + s);

        tx.write(packet.getContents().getBytes());

    }

    public boolean isConnected()
    {
        return this.isConnected;
    }
}
