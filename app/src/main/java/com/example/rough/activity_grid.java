package com.example.rough;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Half;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.rough.classes.GridViewAdapter;
import com.example.rough.classes.PatternLockView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class activity_grid extends AppCompatActivity {

    static PatternLockView patternview;
    TextView btDevices;
    Button runonpath;

    String LastAction = "";
    int DelayTime = 1000;
    int RightTurnDelay = 3000;
    int LeftTurnDelay = 3000;

    private boolean wasRunning;
    private Handler TimerHandler;
    Runnable mStatusChecker;

    public static OutputStream outputStream;
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

    ToggleButton btn_Plough_up_down, btn_Sprinkling, btn_Seeding;
    Button btn_plough_on_off;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);

        patternview = (PatternLockView) findViewById(R.id.patternLockView);
        runonpath = (Button) findViewById(R.id.cmdRunOnPath);
        btDevices = findViewById(R.id.btDevices_Pt_Lock);
        connectToDevice = (Button) findViewById(R.id.connectToDevice_grid);
        btn_Sprinkling = (ToggleButton) findViewById(R.id.btn_memStart);
        btn_Seeding = (ToggleButton) findViewById(R.id.btn_memPlay);
        btn_Plough_up_down = (ToggleButton) findViewById(R.id.btn_memStop);

       // btn_plough_on_off = findViewById(R.id.btn_plough_on_off);

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
                        if (ActivityCompat.checkSelfPermission(activity_grid.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // arduinoBTModule= bluetoothAdapter.getRemoteDevice(deviceHardwareAddress);
                            btSocket = arduinoBTModule.createRfcommSocketToServiceRecord(arduinoUUID);
                            System.out.println(btSocket);
                            btSocket.connect();
                            outputStream= btSocket.getOutputStream();
                        }
                        System.out.println(btSocket.isConnected());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter++;
                } while (btSocket.isConnected() && counter >= 1);
            }
        });

        runonpath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (patternview.selectedDots != null) {
                    run_Mem();
//                    run_Right();
//                    run_Left();
//                    recorder_Path();
                }
            }
        });

        //=============================================================================================================================
        /* Plough Off Operation
        btn_plough_on_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BT_transmission(arduinoBTModule, "8");
            }
        });
        //=============================================================================================================================
     */
    }


    int inc = 0;
    boolean isCommandSend = false;
    Point cur_location;
    Point next_location;
    boolean TurnDelatOnOff = false;
    Handler end_handler = new Handler();

    private void run_Right() {
        BT_transmission(arduinoBTModule, "R");
        Log.e("Turn", "Turn Right");
        end_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BT_transmission(arduinoBTModule, "S");
                Log.e("Stop", "Stop");
            }
        }, 1300);
    }

    private void run_Left() {
        BT_transmission(arduinoBTModule, "L");
        Log.e("Turn", "Turn Left");
        end_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BT_transmission(arduinoBTModule, "S");
                Log.e("Stop", "Stop");
            }
        }, 1200);
    }

    private void run_Mem() {
        if (patternview.selectedDots.size() > 0) {
            TimerHandler.post(mStatusChecker = new Runnable() {
                @Override
                public void run() {
                    if (inc < patternview.selectedDots.size() - 1) {
                        if (TurnDelatOnOff == true) {
                            BT_transmission(arduinoBTModule, "S");
                            Log.e("Stop", "Stop-----" + "Delay Time 10,000");
                            TurnDelatOnOff = false;
                        }
                        //--------------------------------------------------------------------------

                        cur_location = patternview.selectedDots.get(inc);
                        if (inc < patternview.selectedDots.size()) {
                            next_location = patternview.selectedDots.get(inc + 1);
                        } else if (inc == patternview.selectedDots.size()) {
                            next_location = patternview.selectedDots.get(inc);
                        }

                        if (LastAction == "" || LastAction == "F") {
                            if (cur_location.x < next_location.x && cur_location.y == next_location.y) {
                                //Go Right
                                DelayTime = RightTurnDelay;
                                LastAction = "R";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "R");
                                Log.e("Right Turn", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x > next_location.x && cur_location.y == next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "L";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "L");
                                Log.e("Left Turn", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                BT_transmission(arduinoBTModule, "F");
                                Log.e("Go Forword F", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        //---------------------------------------------------------------------------------------------------
                        else if (LastAction == "R") {
                            if (cur_location.x == next_location.x && cur_location.y < next_location.y) {
                                //Go Rigth
                                DelayTime = RightTurnDelay;
                                LastAction = "D";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "R");
                                Log.e("Go Downword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x == next_location.x && cur_location.y > next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "F";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "L");
                                Log.e("Go Upword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                BT_transmission(arduinoBTModule, "F");
                                Log.e("Go Forword R", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        //---------------------------------------------------------------------------------------------------
                        else if (LastAction == "L") {
                            if (cur_location.x == next_location.x && cur_location.y > next_location.y) {
                                //Go Right
                                DelayTime = RightTurnDelay;
                                LastAction = "F";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "R");
                                Log.e("Go Upword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x == next_location.x && cur_location.y < next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "D";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "L");
                                Log.e("Go Downword ", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                BT_transmission(arduinoBTModule, "F");
                                Log.e("Go Forword L", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        //---------------------------------------------------------------------------------------------------
                        else if (LastAction == "D") {
                            if (cur_location.x > next_location.x && cur_location.y == next_location.y) {
                                //Go Right
                                DelayTime = RightTurnDelay;
                                LastAction = "R";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "R");
                                Log.e("Go Right", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x < next_location.x && cur_location.y == next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "L";
                                TurnDelatOnOff = true;
                                BT_transmission(arduinoBTModule, "L");
                                Log.e("Go Left", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                BT_transmission(arduinoBTModule, "F");
                                Log.e("Go Forword D", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        //---------------------------------------------------------------------------------------------------

                        TimerHandler.postDelayed(this, DelayTime);
                        inc++;
                        DelayTime = 1000;
                    } else {
                        DelayTime = 1000;
                        LastAction = "";
                        inc = 0;
                        BT_transmission(arduinoBTModule, "S");
                        Log.e("End", "---------------------------------------------------------------------------");
                        patternview.selectedDots.clear();
                        TimerHandler.removeCallbacks(mStatusChecker);
                    }
                }
            });
        } else {
            Toast.makeText(activity_grid.this, "No Path Found!", Toast.LENGTH_LONG).show();
        }
    }

    private void recorder_Path() {
        if (patternview.selectedDots.size() > 0) {
            String recorded_path = "";
            for (int i = 0; i < patternview.selectedDots.size() - 1; i++) {
                cur_location = patternview.selectedDots.get(i);
                if (i < patternview.selectedDots.size()) {
                    next_location = patternview.selectedDots.get(i + 1);
                } else if (i == patternview.selectedDots.size()) {
                    next_location = patternview.selectedDots.get(i);
                }

                if (LastAction == "" || LastAction == "F") {
                    if (cur_location.x < next_location.x && cur_location.y == next_location.y) {
                        //Go Right
                        LastAction = "R";
                        recorded_path = recorded_path + "R";
                        Log.e("Right Turn", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else if (cur_location.x > next_location.x && cur_location.y == next_location.y) {
                        //Go Left
                        LastAction = "L";
                        recorded_path = recorded_path + "L";
                        Log.e("Left Turn", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else {
                        recorded_path = recorded_path + "F";
                        Log.e("Go Forword F", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    }
                }
                //---------------------------------------------------------------------------------------------------
                else if (LastAction == "R") {
                    if (cur_location.x == next_location.x && cur_location.y < next_location.y) {
                        //Go Rigth
                        LastAction = "D";
                        recorded_path = recorded_path + "R";
                        Log.e("Go Downword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else if (cur_location.x == next_location.x && cur_location.y > next_location.y) {
                        //Go Left
                        LastAction = "F";
                        recorded_path = recorded_path + "L";
                        Log.e("Go Upword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else {
                        recorded_path = recorded_path + "F";
                        Log.e("Go Forword R", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    }
                }
                //---------------------------------------------------------------------------------------------------
                else if (LastAction == "L") {
                    if (cur_location.x == next_location.x && cur_location.y > next_location.y) {
                        //Go Right
                        LastAction = "F";
                        recorded_path = recorded_path + "R";
                        Log.e("Go Upword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else if (cur_location.x == next_location.x && cur_location.y < next_location.y) {
                        //Go Left
                        LastAction = "D";
                        recorded_path = recorded_path + "L";
                        Log.e("Go Downword ", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else {
                        recorded_path = recorded_path + "F";
                        Log.e("Go Forword L", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    }
                }
                //---------------------------------------------------------------------------------------------------
                else if (LastAction == "D") {
                    if (cur_location.x > next_location.x && cur_location.y == next_location.y) {
                        //Go Right
                        LastAction = "R";
                        recorded_path = recorded_path + "R";
                        Log.e("Go Right", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else if (cur_location.x < next_location.x && cur_location.y == next_location.y) {
                        //Go Left
                        LastAction = "L";
                        recorded_path = recorded_path + "L";
                        Log.e("Go Left", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    } else {
                        recorded_path = recorded_path + "F";
                        Log.e("Go Forword D", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                    }
                }
            }
            String ss = recorded_path;
            BT_transmission(arduinoBTModule, recorded_path);
        }
    }

    public void onCheckedChanged(View view) {
        //=================================================
        //SPRINKLING OPERATION
        if (btn_Sprinkling.isChecked() == true) {
            BT_transmission(arduinoBTModule, "7");
        } else {
            BT_transmission(arduinoBTModule, "8");
        }
        //=================================================
        //SOWING OPERATION
        if (btn_Seeding.isChecked() == true) {
            BT_transmission(arduinoBTModule, "1");     // SEeding ON
        } else {
            BT_transmission(arduinoBTModule, "2");   //seeding OFF
        }
        //=================================================
        //PLOUGHING UP-DOWN OPERATION
        if (btn_Plough_up_down.isChecked() == true) {
            BT_transmission(arduinoBTModule, "5");           //5 is for ON...UP
           // BT_transmission(arduinoBTModule, "5");           //5 is for ON...UP
           // BT_transmission(arduinoBTModule, "6");           //6 is for OFF...UP
        }
        // Performing Down operation in else part
        else {
            BT_transmission(arduinoBTModule, "3");          //3 is for ON....Down

           // BT_transmission(arduinoBTModule, "4");           //4 is for OFF...when it is going down
        }




        /*
        //=================================================
        //PLOUGHING UP OPERATION
        if (btn_plough_on_off.isChecked() == true) {
            BT_transmission(arduinoBTModule, "7");           //7 is for ON...UP
        } else {
            BT_transmission(arduinoBTModule, "8");          //8 :to stop the ploughing when it is goind up!
        }
        //PLOUGHING DOWN OPERATION
        if (btn_Plough_up_down.isChecked() == true) {
            BT_transmission(arduinoBTModule, "3");           //3 is for ON...DOWN
        } else {
            BT_transmission(arduinoBTModule, "4");          //4 : to stop the ploughing when it is going down!
        }
         */
    }


    void BT_transmission(BluetoothDevice device, String cmd) {
        // command = "1";
        if (btSocket != null) {
            try {
                if(outputStream!=null) {
                    // byte data[]= cmd.getBytes();
                    outputStream.write(cmd.getBytes());
                }

            } catch (Exception e) {
                Log.d(TAG,"data is not transmitting properly");
                e.printStackTrace();
            }
        }
    }
}