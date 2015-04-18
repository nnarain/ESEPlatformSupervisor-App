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
import android.widget.SeekBar;

import com.nnarain.eseplatformsupervisor.client.BluetoothPacketStream;
import com.nnarain.eseplatformsupervisor.client.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.util.Timer;
import java.util.TimerTask;


public class ControllerActivity extends Activity implements SeekBar.OnSeekBarChangeListener{

    private BTConnectThread tConnect;

    // streams
   // private BluetoothSocket socket;
   // private InputStream rx = null;
   // private OutputStream tx = null;
    BluetoothPacketStream stream;

    private String address;

    private boolean isConnected = false;

    // UI Components
    private Button bnServoNeg, bnServoPos;

    private Button bnStepNeg, bnStepPos;

    private SeekBar sbMotorLeft, sbMotorRight;
    private int motorLVal, motorRVal;
    private boolean flagUpdateMotors;

    //
    private static final String TAG = ControllerActivity.class.getSimpleName();

    private Timer update;
    private TimerTask updateTask;

    private static final long INIT_DELAY     = 500;
    private static final long TIMER_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        // get ui components
        bnServoNeg   = (Button)findViewById(R.id.bnServoNeg);
        bnServoPos   = (Button)findViewById(R.id.bnServoPos);

        bnStepNeg    = (Button)findViewById(R.id.bnStepNeg);
        bnStepPos    = (Button)findViewById(R.id.bnStepPos);

        sbMotorLeft  = (SeekBar)findViewById(R.id.sbMotorLeft);
        sbMotorRight = (SeekBar)findViewById(R.id.sbMotorRight);

        // retrieve the device address
        final Bundle extras = this.getIntent().getExtras();
        address = extras.getString(MainActivity.EXTRA_ADDRESS);

        stream = new BluetoothPacketStream(address);

        update = new Timer();
        updateTask = makeUpdateTask();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "scheduling task...");
        update.schedule(updateTask, TIMER_INTERVAL, TIMER_INTERVAL);

        stream.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "cancelling task...");

        updateTask.cancel();
        updateTask = makeUpdateTask();

        update.purge();

        stream.close();
    }

    private TimerTask makeUpdateTask()
    {
        return new TimerTask() {
            @Override
            public void run() {

                Packet.Builder builder = new Packet.Builder();
                Packet ping = builder.setCommand(Packet.Command.PING).build();

                try
                {
                    if(stream.isConnected())
                    {
                        stream.write(ping);
                    }
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }

            }
        };
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if(seekBar.getId() == sbMotorLeft.getId())
        {
            motorLVal = progress;
        }
        else if(seekBar.getId() == sbMotorRight.getId())
        {
            motorRVal = progress;
        }

        flagUpdateMotors = true;

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateMotors()
    {
        Packet.Builder builder = new Packet.Builder();

        int speedR = (motorRVal > 50) ? motorRVal : 255 - motorRVal;
        int speedL = (motorLVal > 50) ? motorLVal : 255 - motorLVal;

        // build motor direction packets
        int d;
        d = (motorLVal > 50) ? 1 : 2;

        builder
                .setCommand(Packet.Command.MTR_DIR)
                .addArgument(0)
                .addArgument(d);

        Packet leftMotorPacket = builder.build();

        builder.reset();

        d = (motorRVal > 50) ? 1 : 2;

        builder
                .setCommand(Packet.Command.MTR_DIR)
                .addArgument(1)
                .addArgument(d);

        Packet rightMotorPacket = builder.build();

        flagUpdateMotors = false;
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
