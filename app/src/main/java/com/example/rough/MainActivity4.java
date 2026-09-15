package com.example.rough;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.ToggleButton;

import com.example.rough.databases.DatabaseHelper;

public class MainActivity4 extends AppCompatActivity{// implements CompoundButton.OnCheckedChangeListener {

    private ToggleButton btnPlough, btnPloughDown;
    private ToggleButton btnSow;
    private ToggleButton btnSprinkle;

    private Button btn_ON_OFF;
    private ImageButton btn_top;
    private ImageButton btn_left;
    private ImageButton btn_right;
    private ImageButton btn_bottom;
    Button connectToDevice;

    Button btn_memstart;
    Button btn_memstop;
    Button btn_memplayl;

    TextView textview_Time;

    String deviceName;
    String deviceHardwareAddress;

    private int seconds = 0;
    private boolean running;
    private boolean wasRunning;
    private Handler TimerHandler;
    Runnable mStatusChecker;
    String LastDirection = "";
    DatabaseHelper myDatabaseHelper;
    boolean isMem_Rec_On = false;
    boolean isHandlerRun = false;

    // Global variables we will use in the
    private static final String TAG = "FrugalLogs";
    private static final int REQUEST_ENABLE_BT = 1;
    //We will use a Handler to get the BT Connection statys
    public static Handler handler;
    private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update

    BluetoothDevice arduinoBTModule = null;
    BluetoothSocket btSocket = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //We declare a default UUID to create the global variable

    OutputStream outputStream;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        TextView btDevices = findViewById(R.id.btDevices);

        btnPlough = findViewById(R.id.btnPlough);  //up operation will perform
        btnPloughDown= findViewById(R.id.btnPloughDown);  //down operation
        btnSow = findViewById(R.id.btnSow);
        btnSprinkle = findViewById(R.id.btnSprinkle);
        btn_ON_OFF= findViewById(R.id.btn_ON_OFF);
        btn_top=findViewById(R.id.top);
        btn_right=findViewById(R.id.right);
        btn_left=findViewById(R.id.left);
        btn_bottom=findViewById(R.id.bottom);

        btn_memstart = findViewById(R.id.btn_memStart);
        btn_memstop = findViewById(R.id.btn_memStop);
        btn_memplayl = findViewById(R.id.btn_memPlay);

        textview_Time = findViewById(R.id.textView2);

        myDatabaseHelper = new DatabaseHelper(MainActivity4.this);
        TimerHandler = new Handler();

        connectToDevice= (Button)findViewById(R.id.connectToDevice);
        Log.d(TAG, "Begin Execution");
        //====================================================================================================================
        //Intances of BT Manager and BT Adapter needed to work with BT in Android.
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        //Using a handler to update the interface in case of an error connecting to the BT device
        //My idea is to show handler vs RxAndroid
        /*
        handler = new Handler(Looper.getMainLooper()) {
        };
        */

        try{
            myDatabaseHelper.createDatabase();
        } catch (IOException ioe){
            throw new Error("Unable to create database");
        }
        try{
            myDatabaseHelper.openDatabase();
            myDatabaseHelper.CreateTable();
        }catch (SQLException sqle){
            throw sqle;
        }

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
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
                        if (ActivityCompat.checkSelfPermission(MainActivity4.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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

        btn_ON_OFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMem_Rec_On == true) {
                    if(isHandlerRun == true){
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "s";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "S");
                    }
                    else{
                        LastDirection = "S";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "S");
                    }
                }
                else{
                    BT_transmission(arduinoBTModule, "S");
                }
            }
        });


        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMem_Rec_On == true) {
                    if(isHandlerRun == true){
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "L";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "L");
                    }
                    else{
                        LastDirection = "L";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "L");
                    }
                }
                else{
                    BT_transmission(arduinoBTModule, "L");
                }
            }
        });

        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMem_Rec_On == true) {
                    if(isHandlerRun == true){
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "R";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "R");
                    } else{
                        LastDirection = "R";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "R");
                    }
                }
                else{
                    BT_transmission(arduinoBTModule, "R");
                }
            }
        });
        btn_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMem_Rec_On == true) {
                    if(isHandlerRun == true){
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "F";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "F");
                    }
                    else{
                        LastDirection = "F";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "F");
                    }
                }
                else{
                    BT_transmission(arduinoBTModule, "F");
                }
            }
        });

        btn_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMem_Rec_On == true) {
                    if(isHandlerRun == true){
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "B";
                        running = true;
                        runTimer();
                        BT_transmission(arduinoBTModule, "B");
                    }
                }
                else{
                    BT_transmission(arduinoBTModule, "B");
                }
            }
        });

        btn_memstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isDelete = myDatabaseHelper.DeleteScheduler();
                isMem_Rec_On = true;
            }
        });

        btn_memstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMem_Rec_On == true) {
                    if(isHandlerRun == true){
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        textview_Time.setText("00:00:00");
                    }
                }
                isMem_Rec_On = false;
            }
        });

        btn_memplayl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                running = true;
                run_Mem();
            }
        });
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

    public void openhome(View view)
    {
        startActivity(new Intent(this,MainActivity3.class));
    }

    // You can define additional methods for handling button clicks or other functionality as needed


    //=============================================================================================================================

    public void onCheckedChanged(View view) {
        //=================================================
        //SPRINKLING OPERATION
        if (btnSprinkle.isChecked()==true) {
            BT_transmission(arduinoBTModule,"7");              //Sprinkling ON
        }else {
            BT_transmission(arduinoBTModule,"8");               //Sprinkling OFF
        }

        //=================================================
        //SOWING OPERATION
        if (btnSow.isChecked() == true) {
            BT_transmission(arduinoBTModule, "1");          //ON
        } else {
            BT_transmission(arduinoBTModule, "2");   //OFF
        }
        //=================================================
        //PLOUGHING UP OPERATION
        if (btnPlough.isChecked() == true) {
            BT_transmission(arduinoBTModule, "5");           //7 is for ON...UP
        } else {
            BT_transmission(arduinoBTModule, "6");          //8 :to stop the ploughing when it is going up!
        }


        //PLOUGHING DOWN OPERATION
        if (btnPloughDown.isChecked() == true) {
            BT_transmission(arduinoBTModule, "3");           //3 is for ON...DOWN
        } else {
            BT_transmission(arduinoBTModule, "4");          //4 : to stop the ploughing when it is going down!
        }

    }



    //==============================================================================================================================/
    @Override
    protected void onStart()
    {
        super.onStart();
    }

    private void runTimer()
    {
        TimerHandler.post(mStatusChecker = new Runnable() {
            @Override
            public void run()
            {
                isHandlerRun = true;
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                String time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
                textview_Time.setText(time);

                if (running) {
                    seconds++;
                }

                TimerHandler.postDelayed(this, 1000);
            }
        });
    }
    //======================================================================================================================
    int inc = 0;
    boolean isCommandSend = false;

    private void run_Mem()
    {
        Cursor resSchPath = myDatabaseHelper.GetPath("order by ID");
        resSchPath.moveToFirst();

        if(resSchPath.getCount() > 0)
        {
            TimerHandler.post(mStatusChecker = new Runnable() {
                @Override
                public void run()
                {
                    if(inc < resSchPath.getCount()){
                        String direction = resSchPath.getString(1);
                        String durations = resSchPath.getString(2);
                        if (isCommandSend == false) {
                            BT_transmission(arduinoBTModule, direction);
                            isCommandSend = true;
                            Log.e("Die.", direction);
                        }
                        //-------------------------------------------

                        int hours = seconds / 3600;
                        int minutes = (seconds % 3600) / 60;
                        int secs = seconds % 60;

                        String time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
                        textview_Time.setText(time);
                        Log.e("Time.", time);

                        if(durations.equals(time)){
                            resSchPath.moveToNext();
                            seconds = 0;
                            inc++;
                            isCommandSend = false;
                            Log.e("Stop", direction);
                        }

                        if (running) {
                            seconds++;
                        }
                        TimerHandler.postDelayed(this, 1000);
                    }
                    else{
                        inc = 0;
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                    }
                }
            });
        }
        else{
            Toast.makeText(MainActivity4.this, "No Path Found!",Toast.LENGTH_LONG).show();
        }
    }

}