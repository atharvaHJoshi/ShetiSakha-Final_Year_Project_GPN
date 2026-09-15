package com.example.rough;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rough.classes.PathDrawView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class draw_shape extends AppCompatActivity {

    public static int pathViewHeight;
    public static OutputStream out;
    android.widget.Button send;
    android.widget.Button settings;
    static PathDrawView pathView;
    Thread BlueToothThread;
    boolean stop = false;
    int position;
    byte read[];
    private List<String> stringList;

    private int seconds = 0;
    private boolean running;
    private Handler TimerHandler;
    Runnable mStatusChecker;

    TextView btDevices;

    String deviceName;
    String deviceHardwareAddress;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private static final String TAG = "FrugalLogs";
    private static final int REQUEST_ENABLE_BT = 1;
    //We will use a Handler to get the BT Connection statys
    public static Handler handler;
    private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update
    Button connectToDevice;
    BluetoothDevice arduinoBTModule = null;
    BluetoothSocket btSocket = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //We declare a default UUID to create the global variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_shape);

        pathView = (PathDrawView) findViewById(R.id.canvas);
        send = (android.widget.Button) findViewById(R.id.send);
        settings = (android.widget.Button) findViewById(R.id.settingsButton);
        connectToDevice = (Button)findViewById(R.id.connectToDevice_draw);
        btDevices = findViewById(R.id.btDevices_draw);

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        pathView.addSharedPreferences(shPref);
        PreferenceManager.setDefaultValues(this, R.xml.prefrences, true);
        String option = shPref.getString("PREF_LIST", "Medium");

        TimerHandler = new Handler();

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check if the phone supports BT
                if (bluetoothAdapter == null) {
                    // Device doesn't support Bluetooth
                    Log.d(TAG, "Device doesn't support Bluetooth");
                }

                //Check BT enabled. If disabled, we ask the user to enable BT
                if (!bluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "Bluetooth is disabled");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        Log.d(TAG, "We don't BT Permissions");
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        Log.d(TAG, "Bluetooth is enabled now");
                    } else {
                        Log.d(TAG, "We have BT Permissions");
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        Log.d(TAG, "Bluetooth is enabled now");
                    }
                } else {
                    Log.d(TAG, "Bluetooth is enabled");
                }
                String btDevicesString = "";
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        deviceName = device.getName();
                        deviceHardwareAddress = device.getAddress(); // MAC address
                        Log.d(TAG, "deviceName:" + deviceName);
                        Log.d(TAG, "deviceHardwareAddress:" + deviceHardwareAddress);
                        //We append all devices to a String that we will display in the UI
                        btDevicesString = btDevicesString + deviceName + " || " + deviceHardwareAddress + "\n";
                        //If we find the HC 05 device (the Arduino BT module)
                        //We assign the device value to the Global variable BluetoothDevice
                        //We enable the button "Connect to HC 05 device"
                        if (deviceName.equals("HC-05")) {
                            Log.d(TAG, "HC-05 found");
                            arduinoUUID = device.getUuids()[0].getUuid();
                            arduinoBTModule = device;
                            //HC -05 Found, enabling the button to read results
                            // connectToDevice.setEnabled(true);
                        }
                        btDevices.setText(btDevicesString);
                    }
                }
                Log.d(TAG, "Button Pressed");
                int counter = 0;
                do {
                    try {
                        if (ActivityCompat.checkSelfPermission(draw_shape.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // arduinoBTModule= bluetoothAdapter.getRemoteDevice(deviceHardwareAddress);
                            btSocket = arduinoBTModule.createRfcommSocketToServiceRecord(arduinoUUID);
                            System.out.println(btSocket);
                            btSocket.connect();
                            out= btSocket.getOutputStream();
                        }
                        System.out.println(btSocket.isConnected());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter++;
                } while (btSocket.isConnected() && counter >= 1);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
                    if (pathView.stringList == null) {
                        Toast.makeText(getApplicationContext(), "No Path Drawn",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (pathView.stringList.size() > 0) {
                            stringList = pathView.stringList;
                            running = true;
                            run_Auto();

//                            for (String s : stringList) {
//                                out.write(s.getBytes());
//                            }

                            Toast.makeText(getApplicationContext(), "Message Sent",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Please draw a line.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                    pathView.resetObstacleDetected();

//                }catch(IOException e){
//                    e.printStackTrace();
//                }
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
            }
        });
    }

    void BT_transmission(BluetoothDevice device, String cmd) {
        if (btSocket != null) {
            try {
                if(out!=null) {
                    // byte data[]= cmd.getBytes();
                    out.write(cmd.getBytes());
                }

            } catch (Exception e) {
                Log.d(TAG,"data is not transmitting properly");
                e.printStackTrace();
            }
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

    private void run_Auto()
    {
        if(stringList.size() > 0)
        {
            TimerHandler.post(mStatusChecker = new Runnable() {
                @Override
                public void run()
                {
                    if(inc < stringList.size()){
                        String[] direction = stringList.get(inc).split(":");

                        if (isCommandSend == false) {
                            BT_transmission(arduinoBTModule, direction[0]);
                            isCommandSend = true;
                        }
                        //--------------------------------------------------------------------------

                        Log.e("ListCount Time.", direction[1] + "-----" + seconds );

                        if(direction[1].equals(String.valueOf(seconds)) || direction[1].equals("0")){
                            seconds = 0;
                            inc++;
                            isCommandSend = false;
                            Log.e("Stop", direction[1]);
                        }

                        if (running) {
                            seconds++;
                        }

                        TimerHandler.postDelayed(this, 1000);
                    }
                    else{
                        BT_transmission(arduinoBTModule, "S");
                        inc = 0;
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                    }
                }
            });
        }
        else{
            Toast.makeText(draw_shape.this, "No Path Found!",Toast.LENGTH_LONG).show();
        }
    }
}