package com.shetisakha;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.shetisakha.bluetooth.BluetoothController;
import com.shetisakha.classes.GridViewAdapter;
import com.shetisakha.classes.PatternLockView;

import java.util.List;

public class activity_grid extends AppCompatActivity {

    private static final String TAG = "FrugalLogs";

    static PatternLockView patternview;
    TextView btDevices;
    Button runonpath;

    String LastAction = "";
    int DelayTime = 1000;
    int RightTurnDelay = 3000;
    int LeftTurnDelay = 3000;

    private Handler TimerHandler;
    Runnable mStatusChecker;

    Button connectToDevice;
    private BluetoothController mBluetooth;

    ToggleButton btn_Plough_up_down, btn_Sprinkling, btn_Seeding;

    int inc = 0;
    boolean isCommandSend = false;
    Point cur_location;
    Point next_location;
    boolean TurnDelatOnOff = false;
    Handler end_handler = new Handler();

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

        TimerHandler = new Handler();

        mBluetooth = BluetoothController.getInstance(this);
        mBluetooth.setConnectionListener(new BluetoothController.ConnectionListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                Log.d(TAG, "Connected to " + device.getName());
            }

            @Override
            public void onConnectionFailed(BluetoothDevice device) {
                Log.d(TAG, "Connection failed");
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

        runonpath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (patternview.selectedDots != null) {
                    run_Mem();
                }
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
        List<BluetoothDevice> pairedDevices = mBluetooth.getPairedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                btDevicesString.append(device.getName()).append(" || ")
                        .append(device.getAddress()).append("\n");
            }
            btDevices.setText(btDevicesString);
        }

        if (!mBluetooth.connect("HC-05")) {
            Toast.makeText(this, "HC-05 not found. Please pair it first.", Toast.LENGTH_LONG).show();
        }
    }

    private void run_Right() {
        mBluetooth.send("R");
        Log.e("Turn", "Turn Right");
        end_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetooth.send("S");
                Log.e("Stop", "Stop");
            }
        }, 1300);
    }

    private void run_Left() {
        mBluetooth.send("L");
        Log.e("Turn", "Turn Left");
        end_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetooth.send("S");
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
                            mBluetooth.send("S");
                            Log.e("Stop", "Stop-----" + "Delay Time 10,000");
                            TurnDelatOnOff = false;
                        }

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
                                mBluetooth.send("R");
                                Log.e("Right Turn", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x > next_location.x && cur_location.y == next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "L";
                                TurnDelatOnOff = true;
                                mBluetooth.send("L");
                                Log.e("Left Turn", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                mBluetooth.send("F");
                                Log.e("Go Forword F", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        else if (LastAction == "R") {
                            if (cur_location.x == next_location.x && cur_location.y < next_location.y) {
                                //Go Rigth
                                DelayTime = RightTurnDelay;
                                LastAction = "D";
                                TurnDelatOnOff = true;
                                mBluetooth.send("R");
                                Log.e("Go Downword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x == next_location.x && cur_location.y > next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "F";
                                TurnDelatOnOff = true;
                                mBluetooth.send("L");
                                Log.e("Go Upword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                mBluetooth.send("F");
                                Log.e("Go Forword R", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        else if (LastAction == "L") {
                            if (cur_location.x == next_location.x && cur_location.y > next_location.y) {
                                //Go Right
                                DelayTime = RightTurnDelay;
                                LastAction = "F";
                                TurnDelatOnOff = true;
                                mBluetooth.send("R");
                                Log.e("Go Upword", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x == next_location.x && cur_location.y < next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "D";
                                TurnDelatOnOff = true;
                                mBluetooth.send("L");
                                Log.e("Go Downword ", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                mBluetooth.send("F");
                                Log.e("Go Forword L", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }
                        else if (LastAction == "D") {
                            if (cur_location.x > next_location.x && cur_location.y == next_location.y) {
                                //Go Right
                                DelayTime = RightTurnDelay;
                                LastAction = "R";
                                TurnDelatOnOff = true;
                                mBluetooth.send("R");
                                Log.e("Go Right", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else if (cur_location.x < next_location.x && cur_location.y == next_location.y) {
                                //Go Left
                                DelayTime = LeftTurnDelay;
                                LastAction = "L";
                                TurnDelatOnOff = true;
                                mBluetooth.send("L");
                                Log.e("Go Left", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            } else {
                                mBluetooth.send("F");
                                Log.e("Go Forword D", String.valueOf(cur_location.x) + " " + String.valueOf(cur_location.y));
                            }
                        }

                        TimerHandler.postDelayed(this, DelayTime);
                        inc++;
                        DelayTime = 1000;
                    } else {
                        DelayTime = 1000;
                        LastAction = "";
                        inc = 0;
                        mBluetooth.send("S");
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
            mBluetooth.send(recorded_path);
        }
    }

    public void onCheckedChanged(View view) {
        //=================================================
        //SPRINKLING OPERATION
        if (btn_Sprinkling.isChecked() == true) {
            mBluetooth.send("7");
        } else {
            mBluetooth.send("8");
        }
        //=================================================
        //SOWING OPERATION
        if (btn_Seeding.isChecked() == true) {
            mBluetooth.send("1");     // SEeding ON
        } else {
            mBluetooth.send("2");   //seeding OFF
        }
        //=================================================
        //PLOUGHING UP-DOWN OPERATION
        if (btn_Plough_up_down.isChecked() == true) {
            mBluetooth.send("5");           //5 is for ON...UP
        }
        // Performing Down operation in else part
        else {
            mBluetooth.send("3");          //3 is for ON....Down
        }
    }
}