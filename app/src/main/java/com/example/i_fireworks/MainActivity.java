package com.example.i_fireworks;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.i_fireworks.fragments.BoardFragment;
import com.example.i_fireworks.fragments.DrawFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //consts
    private static final int REQUEST_ENABLE_BT = 395;
    private static final String TAG = "MainActivity";
    private static final int BUFFER_SIZE = 20;
    private static final java.util.UUID UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter bluetoothAdapter;
    ConnectedThread connectedThread;
    SocketConnect socketConnect;
    BluetoothDevice hc05;

    //views
    TextView statusTv;
    TextView responseTV;

    BottomNavigationView bottomNav;

    //fragments
    Fragment selectedFragment;
    BoardFragment boardFragment;
    DrawFragment drawFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initViews();
        initListeners();
        initBt();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @SuppressLint("NonConstantResourceId")
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.board:
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .hide(selectedFragment)
                                    .show(boardFragment)
                                    .commit();
                            selectedFragment = boardFragment;
                            break;
                        case R.id.draw:
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .hide(selectedFragment)
                                    .show(drawFragment)
                                    .commit();
                            selectedFragment = drawFragment;
                            break;
                    }
                    return true;
                }
            };


    private final Handler handler = new Handler(msg -> {
        if (msg.what == MessageConstants.MESSAGE_READ) {
            byte[] readBuff = (byte[]) msg.obj;
            String tempMsg = new String(readBuff, 0, msg.arg1);
            Log.d(TAG, tempMsg);
            responseTV.setText(tempMsg);
            return true;
        }
        return false;
    });


    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // statusTv bt gotowy do dzialania
                statusTv.setText(R.string.DEVICE_READY);
            } else {
                statusTv.setText(R.string.ERR_NO_BT_PERMISSION);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() {
        boardFragment = new BoardFragment(this);
        drawFragment = new DrawFragment(this);
        selectedFragment = boardFragment;

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, drawFragment)
                .hide(drawFragment)
                .add(R.id.fragmentContainer, boardFragment)
//                .hide(boardFragment)
                .commitAllowingStateLoss();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNavigationView);
        responseTV = findViewById(R.id.responseTv);
        statusTv = findViewById(R.id.statusTv);

    }

    private void initListeners() {
        bottomNav.setOnNavigationItemSelectedListener(navListener);


    }

    private void initBt() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            statusTv.setText(R.string.ERR_BT_MODULE_NOT_FOUND);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        hc05 = getHC05();
        if (hc05 == null) {
            statusTv.setText(R.string.ERR_HC05_NOT_FOUND);
            return;
        } else {
            statusTv.setText(R.string.HC_PAIRED);
        }

        socketConnect = new SocketConnect();

        if (!socketConnect.isRunning())
            socketConnect.start();
    }


    public void startSocket(){
            if (!socketConnect.isRunning()) {
                socketConnect = new MainActivity.SocketConnect();
                socketConnect.start();
            }
    }

    public void sendMsg(byte[] bytes){
        if (connectedThread != null && connectedThread.isRunning()) {
            connectedThread.write(bytes);
        }
    }


    private BluetoothDevice getHC05() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                String deviceName = bluetoothDevice.getName();
                int bondState = bluetoothDevice.getBondState();
                if (deviceName.equals("HC-05") && bondState == BluetoothDevice.BOND_BONDED) {
//                    Log.d(TAG, "getHC05: " + bluetoothDevice.getAddress() + " STATUS: " + bondState);
                    return bluetoothDevice;
                }
            }
        }
        return null;
    }

    private class SocketConnect extends Thread {
        BluetoothSocket tmpSocket;
        BluetoothSocket mSocket;
        private boolean running = false;

        @Override
        public void run() {
            running = true;
            runOnUiThread(() -> statusTv.setText(R.string.CONNECTING));
            try {
                tmpSocket = hc05.createInsecureRfcommSocketToServiceRecord(UUID);
            } catch (IOException e) {
                // nie mozna nawiazac polaczenia
                runOnUiThread(() -> statusTv.setText(R.string.ERR_CANT_CONNECT_TO_HC));
                return;
            }
            mSocket = tmpSocket;
            if (connectedThread != null && connectedThread.isRunning())
                connectedThread.cancel();
            connectedThread = new ConnectedThread(mSocket);
            connectedThread.start();
            running = false;
        }


        public boolean isRunning() {
            return running;
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean running;

        public ConnectedThread(BluetoothSocket socket) {
            try {
                socket.connect();
                runOnUiThread(() -> statusTv.setText(R.string.HC_CONNECTED));
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when connecting to socket", e);
                runOnUiThread(() -> {
                    statusTv.setText(R.string.ERR_CANT_CONNECT_TO_HC);
//                        oledGrid.animate().scaleY(1).scaleX(1).start();
                });
            }
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
                runOnUiThread(() -> statusTv.setText(R.string.ERR_CANT_CONNECT_TO_HC));
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
                runOnUiThread(() -> statusTv.setText(R.string.ERR_CANT_CONNECT_TO_HC));
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            running = true;
            // mmBuffer store for the stream
            byte[] mmBuffer = new byte[BUFFER_SIZE];
            int numBytes = 0; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (running) {
                try {
                    numBytes += mmInStream.read(mmBuffer, numBytes, BUFFER_SIZE - numBytes);
                    if (numBytes > 1) {
                        String lastTwoChars = new String(mmBuffer, numBytes - 2, 2);
                        if (lastTwoChars.equals("-&")) {
                            mmBuffer[numBytes - 2] = 0;
                            mmBuffer[numBytes - 1] = 0;
                            Message readMsg = handler.obtainMessage(
                                    MessageConstants.MESSAGE_READ, numBytes, -1,
                                    mmBuffer);
                            readMsg.sendToTarget();
                            numBytes = 0;
                            mmBuffer = new byte[BUFFER_SIZE];
                        }
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, bytes);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }

        public boolean isRunning() {
            return running;
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            running = false;
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}