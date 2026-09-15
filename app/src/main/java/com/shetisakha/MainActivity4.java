package com.shetisakha;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.shetisakha.bluetooth.BluetoothController;
import com.shetisakha.databases.DatabaseHelper;

import java.io.IOException;
import java.util.Locale;

public class MainActivity4 extends AppCompatActivity { // implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "FrugalLogs";

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

    private int seconds = 0;
    private boolean running;
    private Handler TimerHandler;
    Runnable mStatusChecker;
    String LastDirection = "";
    DatabaseHelper myDatabaseHelper;
    boolean isMem_Rec_On = false;
    boolean isHandlerRun = false;

    private BluetoothController mBluetooth;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        TextView btDevices = findViewById(R.id.btDevices);

        btnPlough = findViewById(R.id.btnPlough);  //up operation will perform
        btnPloughDown = findViewById(R.id.btnPloughDown);  //down operation
        btnSow = findViewById(R.id.btnSow);
        btnSprinkle = findViewById(R.id.btnSprinkle);
        btn_ON_OFF = findViewById(R.id.btn_ON_OFF);
        btn_top = findViewById(R.id.top);
        btn_right = findViewById(R.id.right);
        btn_left = findViewById(R.id.left);
        btn_bottom = findViewById(R.id.bottom);

        btn_memstart = findViewById(R.id.btn_memStart);
        btn_memstop = findViewById(R.id.btn_memStop);
        btn_memplayl = findViewById(R.id.btn_memPlay);

        textview_Time = findViewById(R.id.textView2);

        myDatabaseHelper = new DatabaseHelper(MainActivity4.this);
        TimerHandler = new Handler();

        connectToDevice = (Button) findViewById(R.id.connectToDevice);

        mBluetooth = BluetoothController.getInstance(this);
        mBluetooth.setConnectionListener(new BluetoothController.ConnectionListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                Log.d(TAG, "Connected to " + device.getName());
                Toast.makeText(MainActivity4.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectionFailed(BluetoothDevice device) {
                Toast.makeText(MainActivity4.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "Disconnected");
            }
        });

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectBluetooth(btDevices);
            }
        });

        try {
            myDatabaseHelper.createDatabase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }
        try {
            myDatabaseHelper.openDatabase();
            myDatabaseHelper.CreateTable();
        } catch (SQLException sqle) {
            throw sqle;
        }

        btn_ON_OFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMem_Rec_On == true) {
                    if (isHandlerRun == true) {
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "s";
                        running = true;
                        runTimer();
                        mBluetooth.send("S");
                    } else {
                        LastDirection = "S";
                        running = true;
                        runTimer();
                        mBluetooth.send("S");
                    }
                } else {
                    mBluetooth.send("S");
                }
            }
        });

        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMem_Rec_On == true) {
                    if (isHandlerRun == true) {
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "L";
                        running = true;
                        runTimer();
                        mBluetooth.send("L");
                    } else {
                        LastDirection = "L";
                        running = true;
                        runTimer();
                        mBluetooth.send("L");
                    }
                } else {
                    mBluetooth.send("L");
                }
            }
        });

        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMem_Rec_On == true) {
                    if (isHandlerRun == true) {
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "R";
                        running = true;
                        runTimer();
                        mBluetooth.send("R");
                    } else {
                        LastDirection = "R";
                        running = true;
                        runTimer();
                        mBluetooth.send("R");
                    }
                } else {
                    mBluetooth.send("R");
                }
            }
        });

        btn_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMem_Rec_On == true) {
                    if (isHandlerRun == true) {
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "F";
                        running = true;
                        runTimer();
                        mBluetooth.send("F");
                    } else {
                        LastDirection = "F";
                        running = true;
                        runTimer();
                        mBluetooth.send("F");
                    }
                } else {
                    mBluetooth.send("F");
                }
            }
        });

        btn_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMem_Rec_On == true) {
                    if (isHandlerRun == true) {
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                        Boolean isInsert1 = myDatabaseHelper.InsertPath(LastDirection, textview_Time.getText().toString());
                        LastDirection = "B";
                        running = true;
                        runTimer();
                        mBluetooth.send("B");
                    } else {
                        mBluetooth.send("B");
                    }
                } else {
                    mBluetooth.send("B");
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
                if (isMem_Rec_On == true) {
                    if (isHandlerRun == true) {
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

    private void connectBluetooth(TextView btDevices) {
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

    public void openhome(View view) {
        startActivity(new Intent(this, MainActivity3.class));
    }

    public void onCheckedChanged(View view) {
        //=================================================
        //SPRINKLING OPERATION
        if (btnSprinkle.isChecked() == true) {
            mBluetooth.send("7");              //Sprinkling ON
        } else {
            mBluetooth.send("8");               //Sprinkling OFF
        }

        //=================================================
        //SOWING OPERATION
        if (btnSow.isChecked() == true) {
            mBluetooth.send("1");          //ON
        } else {
            mBluetooth.send("2");   //OFF
        }
        //=================================================
        //PLOUGHING UP OPERATION
        if (btnPlough.isChecked() == true) {
            mBluetooth.send("5");           //5 is for ON...UP
        } else {
            mBluetooth.send("6");          //6 is for OFF...UP
        }

        //PLOUGHING DOWN OPERATION
        if (btnPloughDown.isChecked() == true) {
            mBluetooth.send("3");           //3 is for ON...DOWN
        } else {
            mBluetooth.send("4");          //4 is for OFF...DOWN
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void runTimer() {
        TimerHandler.post(mStatusChecker = new Runnable() {
            @Override
            public void run() {
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

    int inc = 0;
    boolean isCommandSend = false;

    private void run_Mem() {
        Cursor resSchPath = myDatabaseHelper.GetPath("order by ID");
        resSchPath.moveToFirst();

        if (resSchPath.getCount() > 0) {
            TimerHandler.post(mStatusChecker = new Runnable() {
                @Override
                public void run() {
                    if (inc < resSchPath.getCount()) {
                        String direction = resSchPath.getString(1);
                        String durations = resSchPath.getString(2);
                        if (isCommandSend == false) {
                            mBluetooth.send(direction);
                            isCommandSend = true;
                            Log.e("Die.", direction);
                        }

                        int hours = seconds / 3600;
                        int minutes = (seconds % 3600) / 60;
                        int secs = seconds % 60;

                        String time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
                        textview_Time.setText(time);
                        Log.e("Time.", time);

                        if (durations.equals(time)) {
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
                    } else {
                        inc = 0;
                        running = false;
                        seconds = 0;
                        TimerHandler.removeCallbacks(mStatusChecker);
                    }
                }
            });
        } else {
            Toast.makeText(MainActivity4.this, "No Path Found!", Toast.LENGTH_LONG).show();
        }
    }

}