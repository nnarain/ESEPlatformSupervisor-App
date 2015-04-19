package com.nnarain.eseplatformsupervisor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener{

    // list of bluetooth devices
    private ListView lvDevices;

    // device name
    private ArrayAdapter<String> deviceNames;
    // device address
    private ArrayList<String> deviceAddresses;

    // extras
    public static final String EXTRA_NAME    = "MainActivity.EXTRA_NAME";
    public static final String EXTRA_ADDRESS = "MainActivity.EXTRA_ADDRESS";

    // request code
    private static final int REQUEST_BT_ENABLE = 0;

    private static final String TAG = "MainActivity";

    // device discover receiver
    private final BroadcastReceiver brDeviceDiscover = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            deviceNames.add(device.getName());
            deviceAddresses.add(device.getAddress());

            lvDevices.setAdapter(deviceNames);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the bluetooth adapter
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        //
        lvDevices = (ListView)findViewById(R.id.lvDevices);
        lvDevices.setOnItemClickListener(this);

        // set the scan button action
        ((Button)findViewById(R.id.bnScan)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start looking for bluetooth devices
                if (adapter.startDiscovery())
                    Toast.makeText(MainActivity.this, "Scanning...", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_LONG).show();
            }
        });

        // init lists

        deviceNames = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1
        );

        deviceAddresses = new ArrayList<String>();

        // enable bluetooth and set receiver
        if(adapter != null)
        {
            if(adapter.isEnabled())
            {
                registerDiscoverReceiver();
            }
            else
            {
                enableBluetooth();
            }
        }


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        String name = deviceNames.getItem(position);
        String addr = deviceAddresses.get(position);

        final Bundle extras = new Bundle();
        extras.putString(EXTRA_NAME, name);
        extras.putString(EXTRA_ADDRESS, addr);

        Intent i = new Intent(MainActivity.this, ControllerActivity.class);
        i.putExtras(extras);

        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_BT_ENABLE)
        {
            if(resultCode == RESULT_OK)
            {
                registerDiscoverReceiver();
            }
        }

    }

    private void registerDiscoverReceiver()
    {
        registerReceiver(brDeviceDiscover, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private void enableBluetooth()
    {
        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(i, REQUEST_BT_ENABLE);
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(brDeviceDiscover);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id)
        {
            case R.id.menu_main_enableBluetooth:
                enableBluetooth();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
