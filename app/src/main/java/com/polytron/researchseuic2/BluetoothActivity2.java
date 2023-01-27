package com.polytron.researchseuic2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity2 extends AppCompatActivity {

//    private static final UUID MY_UUID_INSECURE =
//            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private static final UUID MY_UUID_INSECURE =
//            UUID.fromString("30444335-3534-5046-5847-842AFD171B6F"); // UUID Joe
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ConnectedThread mConnectedThread;
    private Handler handler;


    String TAG = "MainActivity";
    EditText send_data;
    TextView view_data;
    Spinner paired_device_spinner;
    StringBuilder messages;
    BluetoothDevice current_device = null;

//    @SuppressLint("MissingPermission")
//    public void pairDevice(View v) {
//        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//        Log.e("MainActivity", "" + pairedDevices.size() );
//        if (pairedDevices.size() > 0) {
//            Object[] devices = pairedDevices.toArray();
//            BluetoothDevice device = (BluetoothDevice) devices[1];
//            //ParcelUuid[] uuid = device.getUuids();
//            Log.e("MAinActivity", "" + device.getName() + " (" + device.getAddress() + ")" );
//            //Log.e("MAinActivity", "" + uuid)
//
//            ConnectThread connect = new ConnectThread(device,MY_UUID_INSECURE);
//            connect.start();
//        }
//    }

    public void pairDevice(View v) {
        ConnectThread connect = new ConnectThread(current_device,MY_UUID_INSECURE);
        connect.start();
    }

    @SuppressLint("MissingPermission")
    public void pairSelectedDevice(String selected_device) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//        Log.e("MainActivity", "" + pairedDevices.size() );
        if (pairedDevices.size() > 0) {
            Object[] devices = pairedDevices.toArray();
            String[] pairedDeviceList = new String[0];
            pairedDeviceList = selected_device.split(" ~ ");
            if(pairedDeviceList[0] != "") {
                Log.e(TAG, "Paired device list: " + pairedDeviceList[1].toString());
                for(int i = 0; i < devices.length;i++) {
                    BluetoothDevice device = (BluetoothDevice) devices[i];
                    if(Objects.equals(device.getAddress(), pairedDeviceList[1])) {
                        Log.e(TAG, "Device Selected!");
                        current_device = device;
//                    ConnectThread connect = new ConnectThread(device,MY_UUID_INSECURE);
//                    connect.start();
                        break;
                    }
                    else {
                        Log.e(TAG, "Device is not Selected!");
                        Log.e(TAG, device.getAddress());
                        Log.e(TAG, pairedDeviceList[1]);
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    public Object[] getPairedDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.e("MainActivity", "" + pairedDevices.size() );
        if (pairedDevices.size() > 0) {
            Object[] devices = pairedDevices.toArray();
            return devices;
        }
        return null;
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        @SuppressLint("MissingPermission")
        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        +MY_UUID_INSECURE );
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

            } catch (IOException e) {
                // Close the socket
                try {
                    e.printStackTrace();
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket. " + e.getMessage());
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
            }

            //will talk about this in the 3rd video
            connected(mmSocket);
        }
        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    final String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            view_data.setText(incomingMessage);
                        }
                    });


                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }


        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    public void SendMessage(View v) {
        byte[] bytes = send_data.getText().toString().getBytes(Charset.defaultCharset());
        mConnectedThread.write(bytes);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth2);

        send_data =(EditText) findViewById(R.id.editText);
        view_data = (TextView) findViewById(R.id.textView);
        paired_device_spinner = (Spinner) findViewById(R.id.spinner);

        ArrayList<String> strings = new ArrayList<String>();

        strings.add("");
        for(int i=0;i<getPairedDevice().length;i++) {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) getPairedDevice()[i];
            strings.add(bluetoothDevice.getName() + " ~ " + bluetoothDevice.getAddress());
            Log.e(TAG, bluetoothDevice.getName() + " | " + bluetoothDevice.getAddress());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, strings);
        paired_device_spinner.setPrompt("Select the Device");
        paired_device_spinner.setAdapter(adapter);

//        paired_device_spinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Log.e(TAG, "onItemClick: " + paired_device_spinner.getSelectedItem().toString());
//            }
//        });

        paired_device_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.e(TAG, "onItemClick: " + paired_device_spinner.getSelectedItem().toString());
                pairSelectedDevice(paired_device_spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void Start_Server(View view) {

        AcceptThread accept = new AcceptThread();
        accept.start();

    }

    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("appname", MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");

                socket = mmServerSocket.accept();

                Log.d(TAG, "run: RFCOM server socket accepted connection.");

            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            //talk about this is in the 3rd
            if (socket != null) {
                connected(socket);
            }

            Log.i(TAG, "END mAcceptThread ");
        }


        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }
}