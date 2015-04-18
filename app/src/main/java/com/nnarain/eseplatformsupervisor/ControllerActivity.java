package com.nnarain.eseplatformsupervisor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.util.Timer;
import java.util.TimerTask;


public class ControllerActivity extends Activity {

    private BTConnectThread tConnect;

    // streams
    private BluetoothSocket socket;
    private InputStream rx = null;
    private OutputStream tx = null;

    private String address;

    private boolean isConnected = false;

    //
    private static final String TAG = ControllerActivity.class.getSimpleName();

    private Timer update;
    private final TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {

            try
            {
                tx.write(("<P>").getBytes());
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

        }
    };

    private static final long INIT_DELAY     = 500;
    private static final long TIMER_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_controller);

        // retrieve the device address
        final Bundle extras = this.getIntent().getExtras();
        address = extras.getString(MainActivity.EXTRA_ADDRESS);

        // create the update timer task
        update = new Timer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        connect(address);
    }

    @Override
    protected void onPause() {
        super.onPause();

        update.cancel();

        try
        {
            if(socket != null) socket.close();
            if(rx != null) rx.close();
            if(tx != null) tx.close();

            if(tConnect != null)
            {
                tConnect.join();
                tConnect = null;
            }
        }
        catch(IOException e)
        {
        }
        catch (InterruptedException e)
        {
        }

    }

    private void connect(String addr)
    {
        // stop looking for devices
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.cancelDiscovery();

        // create a device from address
        BluetoothDevice device = adapter.getRemoteDevice(addr);

        // create the connection thread
        tConnect = new BTConnectThread(device, new BTConnectThread.OnConnectListener()
        {
            @Override
            public void onConnect(BluetoothSocket socket, InputStream in, OutputStream out) {
                ControllerActivity.this.socket = socket;
                ControllerActivity.this.rx = in;
                ControllerActivity.this.tx = out;
                ControllerActivity.this.isConnected = true;

                ControllerActivity.this.update.schedule(updateTask, TIMER_INTERVAL, TIMER_INTERVAL);

                Log.d(TAG, "Connected");
            }

            @Override
            public void onConnectFailed() {
                Log.d(TAG, "Device Connect Failed!!!");
                finish();
            }
        });

        tConnect.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
