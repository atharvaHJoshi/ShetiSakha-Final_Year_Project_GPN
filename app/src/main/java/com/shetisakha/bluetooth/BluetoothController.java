package com.shetisakha.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared, thread-safe Bluetooth SDK used to control the Arduino (HC-05) module.
 * All connection state is kept in a single singleton so it survives activity changes.
 */
public class BluetoothController {

    private static final String TAG = "BluetoothController";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ARDUINO_DEVICE_NAME = "HC-05";

    public static final int REQUEST_ENABLE_BT = 1;

    private static volatile BluetoothController sInstance;

    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private final Handler mMainHandler;

    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private BluetoothDevice mConnectedDevice;
    @Nullable
    private ConnectionListener mListener;

    public interface ConnectionListener {
        void onConnected(BluetoothDevice device);

        void onConnectionFailed(BluetoothDevice device);

        void onDisconnected();
    }

    private BluetoothController(Context context) {
        mContext = context.getApplicationContext();
        BluetoothManager manager = mContext.getSystemService(BluetoothManager.class);
        mAdapter = manager != null ? manager.getAdapter() : null;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized BluetoothController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BluetoothController(context);
        }
        return sInstance;
    }

    public void setConnectionListener(@Nullable ConnectionListener listener) {
        mListener = listener;
    }

    public boolean isSupported() {
        return mAdapter != null;
    }

    public boolean isEnabled() {
        return isSupported() && mAdapter.isEnabled();
    }

    public void requestEnable(Activity activity) {
        if (isSupported() && !isEnabled()) {
            activity.startActivityForResult(
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (!isSupported() || !isEnabled() || !hasConnectPermission()) {
            return devices;
        }
        Set<BluetoothDevice> bonded = mAdapter.getBondedDevices();
        if (bonded != null) {
            devices.addAll(bonded);
        }
        return devices;
    }

    @Nullable
    public BluetoothDevice findPairedDevice(String name) {
        for (BluetoothDevice device : getPairedDevices()) {
            String deviceName = device.getName();
            if (name.equals(deviceName)) {
                return device;
            }
        }
        return null;
    }

    @Nullable
    public BluetoothDevice findArduinoDevice() {
        return findPairedDevice(ARDUINO_DEVICE_NAME);
    }

    /** Finds the Arduino (HC-05) module and connects to it asynchronously. */
    public boolean connect() {
        return connect(findArduinoDevice());
    }

    /** Finds a paired device by name and connects to it asynchronously. */
    public boolean connect(@Nullable String deviceName) {
        return connect(findPairedDevice(deviceName));
    }

    public boolean connect(@Nullable final BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        if (isConnected()) {
            return true;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!hasConnectPermission()) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    postDisconnected();
                    return;
                }
                mAdapter.cancelDiscovery();
                BluetoothSocket socket = null;
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    socket.connect();
                    synchronized (BluetoothController.this) {
                        mSocket = socket;
                        mOutputStream = socket.getOutputStream();
                        mConnectedDevice = device;
                    }
                    Log.i(TAG, "Connected to " + device.getName() + " (" + device.getAddress() + ")");
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) {
                                mListener.onConnected(device);
                            }
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed", e);
                    closeQuietly();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) {
                                mListener.onConnectionFailed(device);
                            }
                        }
                    });
                }
            }
        }, "BtConnect").start();
        return true;
    }

    public synchronized boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    public synchronized void send(@Nullable String command) {
        if (command == null || mOutputStream == null) {
            return;
        }
        try {
            mOutputStream.write(command.getBytes());
            mOutputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to send command: " + command, e);
        }
    }

    public void disconnect() {
        closeQuietly();
        post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onDisconnected();
                }
            }
        });
    }

    private synchronized void closeQuietly() {
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException ignored) {
            }
            mOutputStream = null;
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException ignored) {
            }
            mSocket = null;
        }
        mConnectedDevice = null;
    }

    private void postDisconnected() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onDisconnected();
                }
            }
        });
    }

    private void post(Runnable runnable) {
        mMainHandler.post(runnable);
    }

    private boolean hasConnectPermission() {
        return ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }
}