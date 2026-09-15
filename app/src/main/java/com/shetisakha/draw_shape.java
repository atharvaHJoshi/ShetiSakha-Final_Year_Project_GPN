package com.shetisakha;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.shetisakha.bluetooth.BluetoothController;
import com.shetisakha.classes.PathDrawView;

import java.util.List;

public class draw_shape extends AppCompatActivity {

    private static final String TAG = "FrugalLogs";

    public static int pathViewHeight;
    Button send;
    Button settings;
    static PathDrawView pathView;
    private List<String> stringList;

    private int seconds = 0;
    private boolean running;
    private Handler TimerHandler;
    Runnable mStatusChecker;

    TextView btDevices;
    Button connectToDevice;

    private BluetoothController mBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_shape);

        pathView = (PathDrawView) findViewById(R.id.canvas);
        send = (Button) findViewById(R.id.send);
        settings = (Button) findViewById(R.id.settingsButton);
        connectToDevice = (Button) findViewById(R.id.connectToDevice_draw);
        btDevices = findViewById(R.id.btDevices_draw);

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        pathView.addSharedPreferences(shPref);
        PreferenceManager.setDefaultValues(this, R.xml.prefrences, true);

        TimerHandler = new Handler();

        mBluetooth = BluetoothController.getInstance(this);
        mBluetooth.setConnectionListener(new BluetoothController.ConnectionListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                Log.d(TAG, "Connected to " + device.getName());
                Toast.makeText(draw_shape.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectionFailed(BluetoothDevice device) {
                Toast.makeText(draw_shape.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "Disconnected");
            }
        });

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectBluetooth();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pathView.stringList == null) {
                    Toast.makeText(getApplicationContext(), "No Path Drawn",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (pathView.stringList.size() > 0) {
                        stringList = pathView.stringList;
                        running = true;
                        run_Auto();

                        Toast.makeText(getApplicationContext(), "Message Sent",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Please draw a line.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                pathView.resetObstacleDetected();
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });
    }

    private void connectBluetooth() {
        if (!mBluetooth.isSupported()) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        mBluetooth.requestEnable(this);

        StringBuilder btDevicesString = new StringBuilder();
        for (BluetoothDevice device : mBluetooth.getPairedDevices()) {
            btDevicesString.append(device.getName()).append(" || ")
                    .append(device.getAddress()).append("\n");
        }
        btDevices.setText(btDevicesString);

        if (!mBluetooth.connect("HC-05")) {
            Toast.makeText(this, "HC-05 not found. Please pair it first.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onRestart() {
        pathView.validateLine();
        pathView.invalidate();
        super.onRestart();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        pathViewHeight = pathView.getHeight();
    }

    int inc = 0;
    boolean isCommandSend = false;

    private void run_Auto() {
        if (stringList.size() > 0) {
            TimerHandler.post(mStatusChecker = new Runnable() {
                @Override
                public void run() {
                    if (inc < stringList.size()) {
                        String[] direction = stringList.get(inc).split(":");

                        if (isCommandSend == false) {
                            mBluetooth.send(direction[0]);
                            isCommandSend = true;
                        }

                        Log.e("ListCount Time.", direction[1] + "-----" + seconds);

                        if (direction[1].equals(String.valueOf(seconds)) || direction[1].equals("0")) {
                            seconds = 0;
                            inc++;
                            isCommandSend = false;
                            Log.e("Stop", direction[1]);
                        }

                        if (running) {
                            seconds++;
                        }

                        TimerHandler.postDelayed(this, 1000);
                    } else {
                        mBluetooth.send("S");
                        inc = 0;
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                    }
                }
            });
        } else {
            Toast.makeText(draw_shape.this, "No Path Found!", Toast.LENGTH_LONG).show();
        }
    }
}