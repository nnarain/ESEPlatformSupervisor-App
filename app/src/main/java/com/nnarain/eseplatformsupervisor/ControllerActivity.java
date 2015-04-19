package com.nnarain.eseplatformsupervisor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.nnarain.eseplatformsupervisor.client.BTConnectThread;
import com.nnarain.eseplatformsupervisor.client.BluetoothPacketStream;
import com.nnarain.eseplatformsupervisor.client.Packet;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class ControllerActivity extends Activity implements SeekBar.OnSeekBarChangeListener{

    // streams
    BluetoothPacketStream stream;

    private boolean isConnected = false;

    // UI Components
    private Button bnServoNeg, bnServoPos;
    private int servoAngle = 0;
    private int servoSpeed = 5;

    private Button bnStepNeg, bnStepPos;
    private int stepAngle = 0;
    private int stepSpeed = 5;

    private SeekBar sbMotorLeft, sbMotorRight;
    private int motorLVal, motorRVal;
    private boolean flagUpdateMotors;

    //
    private static final String TAG = ControllerActivity.class.getSimpleName();

    private Timer update;
    private TimerTask updateTask;

    private static final long INIT_DELAY     = 500;
    private static final long TIMER_INTERVAL = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        // get ui components
        bnServoNeg   = (Button)findViewById(R.id.bnServoNeg);
        bnServoPos   = (Button)findViewById(R.id.bnServoPos);

        bnStepNeg    = (Button)findViewById(R.id.bnStepNeg);
        bnStepPos    = (Button)findViewById(R.id.bnStepPos);

        ((SeekBar)findViewById(R.id.sbMotorLeft)).setOnSeekBarChangeListener(this);
        ((SeekBar)findViewById(R.id.sbMotorRight)).setOnSeekBarChangeListener(this);

        // retrieve the device address
        final Bundle extras = this.getIntent().getExtras();
        String address = extras.getString(MainActivity.EXTRA_ADDRESS);

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

                try
                {
                    if(stream.isConnected())
                    {
                        if(flagUpdateMotors)
                        {
                            updateMotors();
                        }

                        // servo control
                        if(bnServoPos.isPressed())
                        {
                            servoAngle = inc(servoAngle, servoSpeed, 180);
                            updateServo(servoAngle);
                        }
                        else if(bnServoNeg.isPressed())
                        {
                            servoAngle = inc(servoAngle, -servoSpeed, 180);
                            updateServo(servoAngle);
                        }

                        // stepper control
                        if(bnStepPos.isPressed())
                        {
                            stepAngle = inc(stepAngle, stepSpeed, 180);
                            updateStepper(stepAngle);
                        }
                        else if(bnStepNeg.isPressed())
                        {
                            stepAngle = inc(stepAngle, -stepSpeed, 180);
                            updateStepper(stepAngle);
                        }

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

        // check which seek bar it is and update the progress
        switch(seekBar.getId())
        {
            case R.id.sbMotorLeft:
                motorLVal = progress;
                break;
            case R.id.sbMotorRight:
                motorRVal = progress;
                break;
        }

        flagUpdateMotors = true;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateServo(int angle) throws IOException
    {
        Packet.Builder builder = new Packet.Builder();

        builder
                .setCommand(Packet.Command.SERVO)
                .addArgument(angle);

        Packet servo = builder.build();

        stream.write(servo);
    }

    private void updateStepper(int angle) throws IOException
    {
        Packet.Builder builder = new Packet.Builder();

        builder
                .setCommand(Packet.Command.STEPPER)
                .addArgument(angle);

        Packet step = builder.build();

        stream.write(step);
    }

    private void updateMotors() throws IOException
    {
        Packet.Builder builder = new Packet.Builder();

        int speedR = (motorRVal > 50) ? motorRVal : 255 - motorRVal;
        int speedL = (motorLVal > 50) ? motorLVal : 255 - motorLVal;

        int speed = Math.max(speedL, speedR);

        // build motor speed packet
        builder
                .setCommand(Packet.Command.MTR_SPEED)
                .addArgument(speed);

        Packet speedPacket = builder.build();

        builder.reset();

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

        // send the motor packets
        stream.write(speedPacket);
        stream.write(leftMotorPacket);
        stream.write(rightMotorPacket);

        flagUpdateMotors = false;
    }

    private int inc(int v, int inc, int limit)
    {
        v += inc;

        if(v < 0)
            v = 0;

        return v % limit;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controller, menu);
        return true;
    }

    private void sync()
    {
        if(stream.isConnected())
        {
            Packet sync = new Packet.Builder().setCommand(Packet.Command.SYNC).build();

            try
            {
                stream.write(sync);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

        }
        else
        {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id)
        {
            case R.id.menu_controller_sync:
                sync();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
