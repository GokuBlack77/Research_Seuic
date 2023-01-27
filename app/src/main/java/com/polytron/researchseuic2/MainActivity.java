package com.polytron.researchseuic2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Image;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.polytron.researchseuic2.adapters.ScanResultAdapter;
import com.polytron.researchseuic2.models.AppConstants;
import com.polytron.researchseuic2.models.BluetoothPreference;
import com.polytron.researchseuic2.models.DialogReaderSetting;
import com.polytron.researchseuic2.models.LoadingDialog;
import com.polytron.researchseuic2.models.ScanResult;
import com.polytron.researchseuic2.models.SettingReaderManager;
import com.seuic.scankey.IKeyEventCallback;
import com.seuic.scankey.ScanKeyService;
import com.seuic.scanner.DecodeInfo;
import com.seuic.scanner.DecodeInfoCallBack;
import com.seuic.scanner.Scanner;
import com.seuic.scanner.ScannerFactory;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;


public class MainActivity extends AppCompatActivity implements DecodeInfoCallBack {

    private final Handler threadHandler = new Handler();
    static final String TAG = "MainActivity";
    private InventoryRunable mInventoryRunable;
    private Thread mInventoryThread;
    ArrayList<EditText> listEditText = new ArrayList<>();
    ImageView ivSetting;
    TextView tvTitle, tvResult;
    Button bConnectDevice;
    CheckBox cbNomorSeri, cbRFID;
    ListView lvResult;
    RecyclerView rvResult;
    ScanResultAdapter scanResultAdapter;

    ArrayAdapter<String> adapter;
    Spinner spDevices;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice current_device;
    ConnectedThread mConnectedThread;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID_INSECURE =
//            UUID.fromString("30444335-3534-5046-5847-842AFD171B6F"); // UUID Joe
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ArrayList<ScanResult> scanResultArrayList;
    ArrayList<ScanResult> listResult = new ArrayList<>();
    BluetoothPreference bluetoothPreference;
    ArrayList<String> strings = new ArrayList<String>();
    int bluCount = 0;
    String incomingMessage;
    byte[] buffer = new byte[1024];  // buffer store for the stream
    int bytes; // bytes returned from read()
    boolean countDownDone = true;

    Scanner scanner;
    private boolean startScan = false;
    private boolean nomorSeriBool = false;
    private boolean RFIDBool = false;
    private boolean connected = false;

    private UHFService mDevice;

    public boolean mInventoryStart = false;
    private List<EPC> mEPCList;
    static int m_count = 0;
    private static SoundPool mSoundPool;
    private static int soundID;
    private SettingReaderManager srm;
    private LoadingDialog loadingDialog;
    private boolean doubleBackToExitPressedOnce = false;

    // SignalR
    HubConnection hubConnection;
    HubProxy hubProxy;
    Thread thread;
    private class CheckBluetooth implements Runnable{
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            while (true){
                if (bluCount == 0){
                    if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                        bluCount = 1;
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }

                if(bluetoothAdapter != null && bluetoothAdapter.isEnabled())
                    bluCount = 0;

                try{
                    Thread.sleep(100);
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    private void printLog(String msg, boolean isError) {
        if (isError)
            Log.e(TAG, msg);
        else
            Log.i(TAG, msg);
    }

    private Handler handlerRfid = new Handler() {

        public void handleMessage(Message msg) {
            // Refresh listview
            switch (msg.what) {
                case 1:
//                    mEPCList.clear();
                    synchronized (MainActivity.this) {
                        mEPCList = mDevice.getTagIDs();
                    }
                    refreshData();
                    break;
                case 2:
                    BtnOnce();
                    handlerRfid.sendEmptyMessageDelayed(2, 200);
                    break;
                case 3:
                    BtnOnce();
                    break;
                default:
                    break;
            }
        }

        ;
    };

    public void Start_Server(View view) {

        MainActivity.AcceptThread accept = new MainActivity.AcceptThread();
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

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new MainActivity.ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void SendMessage() {
        byte[] bytes = listEditText.get(0).getText().toString().getBytes(Charset.defaultCharset());
        mConnectedThread.write(bytes);
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
//            byte[] buffer = new byte[1024];  // buffer store for the stream
//
//            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            tvResult.setText(incomingMessage);
                            bConnectDevice.setClickable(false);
                            bConnectDevice.setText("Connecting");

//                            if(incomingMessage.trim().equals("Message has been sent")) {
                            if(incomingMessage.trim().equals("Device is Connected") || incomingMessage.trim().equals("Message has been sent")) {
                                Log.e(TAG, "Connected");
                                tvResult.setText("Device is connected");
                                connected = true;
//                                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                                bConnectDevice.setClickable(true);
                                bConnectDevice.setText("Connected");

                                spDevices.setEnabled(false);
                                spDevices.setClickable(false);
                            }
                            else {
//                                Log.e(TAG, "Connecting");
//                                Toast.makeText(MainActivity.t
//                                his, "Connected", Toast.LENGTH_LONG).show();
                            }
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

    @SuppressLint("MissingPermission")
    public void pairDevice(View v) {
        Log.e(TAG, "pairDevice: " + current_device.getName() );
        MainActivity.ConnectThread connect = new MainActivity.ConnectThread(current_device,MY_UUID_INSECURE);
        connect.start();
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
                if(mmDevice != null) {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                if(mmSocket != null) {
                    mmSocket.connect();
                }

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
            if(mmSocket != null) {
                connected(mmSocket);
            }
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

    @SuppressLint("MissingPermission")
    public Object[] getPairedDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//        Log.e("Get Paired Device", "" + pairedDevices.size() );
        if (pairedDevices.size() > 0) {
            Object[] devices = pairedDevices.toArray();
            return devices;
        }
        else {
            Log.e(TAG, "getPairedDevice: 0");
        }
        return null;
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
                    }
                }
            }

        }
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothPreference = new BluetoothPreference(this);

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        try {

            loadingDialog = new LoadingDialog(this);
            srm = new SettingReaderManager(this);
            findId();
            tvTitle.setText("Welcome...");
            loadSettingReader();

            mDevice = UHFService.getInstance(); //Menyalakan RFID
            boolean ret = mDevice.open();
            if (!ret) {
                Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
            }

            mInventoryRunable = new InventoryRunable();
            mEPCList = new ArrayList<EPC>();
            mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
            soundID = mSoundPool.load(this, R.raw.scan, 1);
            lvResult = (ListView) findViewById(R.id.lvResult);
//            rvResult = (RecyclerView) findViewById(R.id.rvResult);


            cbNomorSeri = (CheckBox) findViewById(R.id.cbNomorSeri);
            cbRFID = (CheckBox) findViewById(R.id.cbRFID);

            scanResultArrayList = new ArrayList<>();

//            scanResultArrayList.add(new ScanResult("rfid", "serial number", "date", 0, 0));
            scanResultAdapter = new ScanResultAdapter(scanResultArrayList, this);

            lvResult.setAdapter(scanResultAdapter);

            scanner = ScannerFactory.getScanner(this); //Menghubungkan Barcode Scanner
            scanner.open();
            scanner.setDecodeInfoCallBack(this);
            scanner.enable();
            if (scanner == null){
                printLog("scanner(NULL)", true);
            }
            else{
                if(!mDevice.setRegion(AppConstants.regFreq)){
                    Toast.makeText(this, "Gagal melakukan pengaturan \"Region Frequency\"....", Toast.LENGTH_SHORT).show();
                }
                if(!mDevice.setPower(AppConstants.power)){
                    Toast.makeText(this, "Gagal melakukan pengaturan \"Power\"....", Toast.LENGTH_SHORT).show();
                }
            }


            bConnectDevice = (Button) findViewById(R.id.bConnectDevice);

            bConnectDevice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(spDevices.getSelectedItemPosition() != -1) {
                        Log.e(TAG, "selected device position: " + spDevices.getSelectedItemPosition());
                        pairDevice(view);
                        connected = !connected;
                        if(connected) {
                            bConnectDevice.setText("Connecting");
                        }
                        else {
                            bConnectDevice.setText("Connect");
                            spDevices.setEnabled(true);
                            spDevices.setClickable(true);
                        }
                    }
                }
            });
//            strings.add("");
            for(int i=0;i<getPairedDevice().length;i++) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) getPairedDevice()[i];
                strings.add(bluetoothDevice.getName() + " ~ " + bluetoothDevice.getAddress());
//                strings.add("hello");
                Log.e("bluetooth device: ", bluetoothDevice.getName() + " | " + bluetoothDevice.getAddress());
            }

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, strings);
            spDevices.setPrompt("Select the Device");
            spDevices.setAdapter(adapter);
//            strings.add("hello");
//            strings.add("hello");
//            strings.add("hello");

            if(!Objects.equals(bluetoothPreference.getKeyBluetoothName(), "") && !Objects.equals(bluetoothPreference.getKeyBluetoothAddress(), "")) {
                if(adapter.getPosition(bluetoothPreference.getKeyBluetoothName() + " ~ " + bluetoothPreference.getKeyBluetoothAddress()) != -1) {
                    int spinnerPosition = adapter.getPosition(bluetoothPreference.getKeyBluetoothName() + " ~ " + bluetoothPreference.getKeyBluetoothAddress());
                    spDevices.setSelection(spinnerPosition);
                }
            }


            spDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.e(TAG, "onItemClick: " + spDevices.getSelectedItem().toString());
                    Log.e(TAG, "onItemSelected: item selected!");
                    pairSelectedDevice(spDevices.getSelectedItem().toString());
                    String[] pairedDevice = new String[0];
                    pairedDevice = spDevices.getSelectedItem().toString().split(" ~ ");
                    if(pairedDevice.length > 1) {
                        bluetoothPreference.saveBluetoothData(pairedDevice[0].trim(), pairedDevice[1].trim());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Log.e(TAG, "onTouch: touched");
                }
            });

//            spDevices.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Log.e(TAG, "onTouch: touched");
//                }
//            });


//            spDevices.setOnTouchListener(new View.OnTouchListener() {

//                @Override
//                public boolean onTouch(View view, MotionEvent motionEvent) {
//                    Log.e(TAG, "onTouch: touched");
//                    if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                        getPairedDevice();
//                        Toast.makeText(MainActivity.this, "Hello", Toast.LENGTH_LONG).show();
//                        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                        strings.clear();
//                        for(int i=0;i<getPairedDevice().length;i++) {
//                            BluetoothDevice bluetoothDevice = (BluetoothDevice) getPairedDevice()[i];
//                            strings.add(bluetoothDevice.getName() + " ~ " + bluetoothDevice.getAddress());
//                            Log.e("bluetooth device: ", bluetoothDevice.getName() + " | " + bluetoothDevice.getAddress());
//                        }
////                        spDevices.setAdapter(adapter);
//                    }
//                    return false;
//                }
//            });

            ivSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadingDialog.startLoadingDialog("Membuka pengaturan...", true);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            DialogReaderSetting.configurationReader(MainActivity.this, mDevice, new DialogReaderSetting.CallbackReaderSetting() {
                                @Override
                                public void onResponse(boolean success, String msg) {
                                    if (success) {
                                        Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void closeDialog() {
                                    loadingDialog.dismissDialog();
                                }

                                @Override
                                public void error(Exception ex) {
                                    Log.e(TAG, "error: " + ex.getMessage());
                                    ex.printStackTrace();
                                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }, 500);
                }
            });

            cbNomorSeri.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    nomorSeriBool = !nomorSeriBool;
                }
            });

            cbRFID.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    RFIDBool = !RFIDBool;
                }
            });

            listEditText.get(0).addTextChangedListener(new TextWatcher() {
                boolean _ignore = false;

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (_ignore)
                        return;

                    _ignore = true;
                    try {
                        if (listEditText.get(0).getText().toString().endsWith("\n")) {
                            listEditText.get(0).setText(editable.toString().substring(0, editable.toString().length() - 1));
//                            if (!listEditText.get(0).getText().toString().isEmpty() && !listEditText.get(1).getText().toString().isEmpty()) {
                            if (!listEditText.get(0).getText().toString().isEmpty()) {
//                                Log.e(TAG, "RFID: " + listEditText.get(1).getText().toString() + "\nBarcode: " + listEditText.get(0).getText().toString());
                                byte[] bytes = listEditText.get(0).getText().toString().getBytes(Charset.defaultCharset());
                                if(nomorSeriBool) {
                                    mConnectedThread.write(bytes);
                                    String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
//                                                            listResult.add(0, new ScanResult(listEditText.get(1).getText().toString(), listEditText.get(0).getText().toString(), currentDate, 0));
                                    boolean sameResult = false;
                                    for(int i = 0; i < scanResultArrayList.size();i++) {
//                                        Log.e(TAG, scanResultArrayList.size() + "" );
                                        if(Objects.equals(scanResultArrayList.get(i).getSerialNumber(), listEditText.get(0).getText().toString())) {
//                                            Log.e(TAG, "Scan Result ada yang sama");
                                            scanResultArrayList.get(i).setCounterSerialNum(scanResultArrayList.get(i).getCounterSerialNum() + 1);
                                            scanResultAdapter.notifyDataSetChanged();
                                            sameResult = true;
                                        }
                                    }

                                    if(!sameResult) {
//                                        Log.e(TAG, "Scan Result baru");
                                        scanResultArrayList.add(0, new ScanResult("", listEditText.get(0).getText().toString(), currentDate, 0, 1));
                                        scanResultAdapter.notifyDataSetChanged();
                                        lvResult.smoothScrollToPosition(0);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onTextChangedEtRfid: " + e.getMessage());
                        e.printStackTrace();
                    }
                    _ignore = false;
                }
            });

            listEditText.get(1).addTextChangedListener(new TextWatcher() {
                boolean _ignore = false;

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (_ignore)
                        return;

                    _ignore = true;
                    try {
                        if (listEditText.get(1).getText().toString().endsWith("\n")) {
                            listEditText.get(1).setText(editable.toString().substring(0, editable.toString().length() - 1));
                            if (!listEditText.get(1).getText().toString().isEmpty() ) {
                                byte[] bytes = listEditText.get(1).getText().toString().getBytes(Charset.defaultCharset());
                                if(RFIDBool) {
                                    if(countDownDone) {
                                        countDownDone = false;
                                        new CountDownTimer(100, 1000) {
                                            public void onFinish() {
                                                Log.i(TAG, "hello");
                                                Log.e(TAG, "text: " + listEditText.get(1).getText().toString());
                                                mConnectedThread.write(bytes);
                                                countDownDone = true;
                                                // When timer is finished
                                                // Execute your code here
                                            }

                                            public void onTick(long millisUntilFinished) {
                                                // millisUntilFinished    The amount of time until finished.
                                            }
                                        }.start();
                                    }
//                                    mConnectedThread.write(bytes);

                                    String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                                    boolean sameResult = false;
                                    for(int i = 0; i < scanResultArrayList.size();i++) {
//                                        Log.e(TAG, scanResultArrayList.size() + "" );
                                        if(Objects.equals(scanResultArrayList.get(i).getRFID(), listEditText.get(1).getText().toString())) {
//                                            Log.e(TAG, "Scan Result ada yang sama");
                                            scanResultArrayList.get(i).setCounterRFID(scanResultArrayList.get(i).getCounterRFID() + 1);
                                            scanResultAdapter.notifyDataSetChanged();
                                            sameResult = true;
                                        }
                                    }

                                    if(!sameResult) {
//                                        Log.e(TAG, "Scan Result baru");
                                        scanResultArrayList.add(0, new ScanResult(listEditText.get(1).getText().toString(), "", currentDate, 1, 0));
                                        scanResultAdapter.notifyDataSetChanged();
                                        lvResult.smoothScrollToPosition(0);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onTextChangedEtRfid: " + e.getMessage());
                        e.printStackTrace();
                    }
                    _ignore = false;
                }
            });

            if(nomorSeriBool && !RFIDBool) {

                if(!listEditText.get(0).getText().toString().isEmpty()) {
                    String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
//                                                            listResult.add(0, new ScanResult(listEditText.get(1).getText().toString(), listEditText.get(0).getText().toString(), currentDate, 0));
                    boolean sameResult = false;
                    for(int i = 0; i < scanResultArrayList.size();i++) {
                        Log.e(TAG, scanResultArrayList.size() + "" );
                        if(Objects.equals(scanResultArrayList.get(i).getSerialNumber(), listEditText.get(0).getText().toString())) {
                            Log.e(TAG, "Scan Result ada yang sama");
                            scanResultArrayList.get(i).setCounterSerialNum(scanResultArrayList.get(i).getCounterSerialNum() + 1);
                            scanResultAdapter.notifyDataSetChanged();
                            sameResult = true;
                        }
                    }

                    if(!sameResult) {
                        Log.e(TAG, "Scan Result baru");
                        scanResultArrayList.add(0, new ScanResult("", listEditText.get(0).getText().toString(), currentDate, 0, 1));
                        scanResultAdapter.notifyDataSetChanged();
                        lvResult.smoothScrollToPosition(0);
                    }
                }
            }

            if(!nomorSeriBool && RFIDBool) {
                if(!listEditText.get(1).getText().toString().isEmpty()) {
                    String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                    boolean sameResult = false;
                    for(int i = 0; i < scanResultArrayList.size();i++) {
                        Log.e(TAG, scanResultArrayList.size() + "" );
                        if(Objects.equals(scanResultArrayList.get(i).getRFID(), listEditText.get(1).getText().toString())) {
                            Log.e(TAG, "Scan Result ada yang sama");
                            scanResultArrayList.get(i).setCounterSerialNum(scanResultArrayList.get(i).getCounterRFID() + 1);
                            scanResultAdapter.notifyDataSetChanged();
                            sameResult = true;
                        }
                    }

                    if(!sameResult) {
                        Log.e(TAG, "Scan Result baru");
                        scanResultArrayList.add(0, new ScanResult(listEditText.get(1).getText().toString(), "", currentDate, 1, 0));
                        scanResultAdapter.notifyDataSetChanged();
                        lvResult.smoothScrollToPosition(0);
                    }
                }
            }

            if(nomorSeriBool && RFIDBool) {
                if(!listEditText.get(0).getText().toString().isEmpty() && !listEditText.get(1).getText().toString().isEmpty()) {
                    String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
//                                                            listResult.add(0, new ScanResult(listEditText.get(1).getText().toString(), listEditText.get(0).getText().toString(), currentDate, 0));
                    boolean sameResult = false;
                    for(int i = 0; i < scanResultArrayList.size();i++) {
                        Log.e(TAG, scanResultArrayList.size() + "" );
                        if(Objects.equals(scanResultArrayList.get(i).getSerialNumber(), listEditText.get(0).getText().toString()) && Objects.equals(scanResultArrayList.get(i).getRFID(), listEditText.get(1).getText().toString())) {
                            Log.e(TAG, "Scan Result ada yang sama");
                            scanResultArrayList.get(i).setCounterRFID(scanResultArrayList.get(i).getCounterRFID() + 1);
                            scanResultAdapter.notifyDataSetChanged();
                            sameResult = true;
                        }
                    }

                    if(!sameResult) {
                        Log.e(TAG, "Scan Result baru");
                        scanResultArrayList.add(0, new ScanResult(listEditText.get(1).getText().toString(), listEditText.get(0).getText().toString(), currentDate, 1, 1));
                        scanResultAdapter.notifyDataSetChanged();
                        lvResult.smoothScrollToPosition(0);
                    }
                }
            }

            thread = new Thread(new CheckBluetooth());
            thread.start();
        } catch (Exception ex) {
            printLog(ex.getMessage(), true);
            ex.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Timer t = new Timer();
//        TimerTask tt = new TimerTask() {
//            @Override
//            public void run() {
//                System.out.println("Task Timer on Fixed Rate");
//                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
//                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//                }
//            }
//        };
//        t.scheduleAtFixedRate(tt,500,100);

        if(requestCode == REQUEST_ENABLE_BT) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            strings.clear();
            for(int i=0;i<getPairedDevice().length;i++) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) getPairedDevice()[i];
                strings.add(bluetoothDevice.getName() + " ~ " + bluetoothDevice.getAddress());
                Log.e("bluetooth device: ", bluetoothDevice.getName() + " | " + bluetoothDevice.getAddress());
            }
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, strings);
            spDevices.setPrompt("Select the Device");
            spDevices.setAdapter(adapter);

            spDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.e(TAG, "onItemClick: " + spDevices.getSelectedItem().toString());
                    Log.e(TAG, "onItemSelected: item selected!");
                    pairSelectedDevice(spDevices.getSelectedItem().toString());
                    String[] pairedDevice = new String[0];
                    pairedDevice = spDevices.getSelectedItem().toString().split(" ~ ");
                    if(pairedDevice.length > 1) {
                        bluetoothPreference.saveBluetoothData(pairedDevice[0].trim(), pairedDevice[1].trim());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Log.e(TAG, "onTouch: touched");
                }
            });
//            adapter.notifyDataSetChanged();
//            spDevices.setAdapter(adapter);
//            bluetoothAdapter.notifyAll();
        }
        else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void findId(){
        listEditText.add((EditText) findViewById(R.id.etSerialNumber));
        listEditText.add((EditText) findViewById(R.id.etRfid));
        ivSetting = (ImageView) findViewById(R.id.ivSetting);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvResult = (TextView) findViewById(R.id.tvResult);
        spDevices = (Spinner) findViewById(R.id.spDevices);
    }

    private void sendBluetoothText(String message) {
        if(message != null) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            mConnectedThread.write(bytes);
        }
    }

    private void playSound() {
        if (mSoundPool == null) {
            mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
            soundID = mSoundPool.load(this, R.raw.scan, 1);// "/system/media/audio/notifications/Antimony.ogg"
        }
        mSoundPool.play(soundID, 1, 1, 0, 0, 1);
    }
    int counter = 0;
    private void refreshData() {
        Log.e(TAG, "refreshData: ");
        listEditText.get(0).setText(counter + "");
        counter++;
//        scanResultAdapter.notifyDataSetChanged();
        if (mEPCList != null) {
//            mDevice.getTagIDs().clear();
            if(RFIDBool) {
                for (EPC item : mEPCList) {
//                    SystemClock.sleep(100);
                    sendBluetoothText(item.getId());
//                    if(countDownDone) {
//                        countDownDone = false;
//                        new CountDownTimer(100, 1000) {
//                            public void onFinish() {
//                                Log.i(TAG, "text: " + item.getId());
//                                sendBluetoothText(item.getId());
//                                countDownDone = true;
//                                // When timer is finished
//                                // Execute your code here
//                            }
//
//                            public void onTick(long millisUntilFinished) {
//                                // millisUntilFinished    The amount of time until finished.
//                            }
//                        }.start();
//                    }
                    Log.e(TAG, "TagRFID: " + item.getId() + " || " + item.rssi);
                    String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                    boolean sameResult = false;
                    for(int i = 0; i < scanResultArrayList.size();i++) {
//                        Log.e(TAG, scanResultArrayList.size() + "" );
                        if(Objects.equals(scanResultArrayList.get(i).getRFID(), item.getId())) {
                            Log.e(TAG, "Scan Result ada yang sama");
//                            Log.e(TAG, scanResultArrayList.get(i).getCounterRFID() + 1 + "" );
                            scanResultArrayList.get(i).setCounterRFID(scanResultArrayList.get(i).getCounterRFID() + 1);
//                            Log.e(TAG, "Array list RFID Counter :" + scanResultArrayList.get(i).getCounterRFID());
                            scanResultAdapter.notifyDataSetChanged();
                            sameResult = true;
                        }

                    }

                    if(!sameResult) {
                        Log.e(TAG, "Scan Result baru");

                        scanResultArrayList.add(0, new ScanResult(item.getId(), "", currentDate, 1, 0));
                        scanResultAdapter.notifyDataSetChanged();
                        lvResult.smoothScrollToPosition(0);
                    }

//                    listEditText.get(1).setText(item.getId() + "\n");
//                    sendBluetoothText(item.getId());
//                    break;
                }
            }
        }
//        mEPCList = null;
    }

    private void BtnContinue() {
        if (mInventoryThread != null && mInventoryThread.isAlive()) {
            System.out.println("Thread not null");
            return;
        }

        if (mDevice.inventoryStart()) {
            System.out.println("RfidInventoryStart sucess.");

            mInventoryStart = true;
            mInventoryThread = new Thread(mInventoryRunable);
            mInventoryThread.start();

        } else {
            System.out.println("RfidInventoryStart faild.");
        }
        return;
    }

    private void BtnOnce() {
        EPC epc = new EPC();
        if (mDevice.inventoryOnce(epc, 100)) {
            String id = epc.getId();
            System.out.println("" + id);
            if (id != null && !"".equals(id)) {
                if(listEditText.get(1).getText().toString().equals("")){
                    playSound();
                    listEditText.get(1).setText(id);
                }
            }
            Log.d(TAG, "BtnOnce: OK!!!");
        }
    }

    private void BtnStop() {
        mInventoryStart = false;

        if (mInventoryThread != null) {
            mInventoryThread.interrupt();
            mInventoryThread = null;
        }
        System.out.println("begin stop!!");
        if (mDevice.inventoryStop()) {
            System.out.println("end stop!!");

        } else {
            System.out.println("RfidInventoryStop faild.");
        }
        return;
    }


    @Override
    protected void onDestroy() {
        Log.i("TAG", "onDestroy: YES");
        super.onDestroy();
        scanner.setDecodeInfoCallBack(null);
        scanner.close();
        scanner = null;
        mScanKeyService.unregisterCallback(mCallback);
        if(bConnectDevice.getText() == "Connected") {
            bConnectDevice.performClick();
        }
        try {
            if(thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

//        hubConnection.stop();
//        if(hubConnection != null) {
//        }
//        hubConnection = null;
    }

    @Override
    protected void onPause() {
        try{
            if(thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        scanner.stopScan();
        mDevice.close();
        super.onPause();
        Log.e("TAG", "onPause: YES");
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if(thread == null){
                thread = new Thread(new CheckBluetooth());
            }
            thread.start();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        mDevice = UHFService.getInstance(); //Menyalakan RFID
        // open UHF
        boolean ret = mDevice.open();
        if (!ret) {
            Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
        }
        mScanKeyService.registerCallback(mCallback, "100,101,102,249,249,250");
    }

    @Override
    public void onDecodeComplete(DecodeInfo info) {
        listEditText.get(0).setText(info.barcode + "\n");
    }

//    try{
//        mScanKeyService = ScanKeyService.getInstance();
//    }
//    catch(Exception ex) {
//        ex.printStackTrace();
//    }

    private ScanKeyService mScanKeyService = ScanKeyService.getInstance();
    private IKeyEventCallback mCallback = new IKeyEventCallback.Stub() {
        @Override
        public void onKeyDown(int keyCode) throws RemoteException {
            Log.d(TAG, "onKeyDown: keyCode=" + keyCode);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listEditText.get(0).setText("");
                    listEditText.get(1).setText("");
                    listEditText.get(0).requestFocus();
                }
            });
            if(RFIDBool) {
                BtnContinue(); //RFID
            }
            if(nomorSeriBool) {
                scanner.startScan(); //Barcode Scanner
            }
        }
        @Override
        public void onKeyUp(int keyCode) throws RemoteException {
            Log.d(TAG, "onKeyUp: keyCode=" + keyCode);
            mEPCList.clear();
            scanner.stopScan();
            BtnStop();
        }
    };
//    try{
//
//    }
//    catch(Exception e) {
//        e.printStackTrace();
//    }


    @Override
    public void onBackPressed() {
        try{
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce=false;
                }
            }, 2000);
        }
        catch (Exception e){
            Log.e(TAG, "onBackPressed: " + e.getMessage());
            e.printStackTrace();
            super.onBackPressed();
        }
    }

    private boolean loadSettingReader(){
        try{
            AppConstants.filterRssi = srm.getFilterRssi();
            AppConstants.rangeRssi = srm.getRangeRssi();
            AppConstants.power = srm.getPower();
//            AppConstants.session = srm.getSession();
            AppConstants.regFreq = srm.getRegFreq();
            return true;
        }
        catch (Exception ex){
            throw ex;
        }
    }

    private class InventoryRunable implements Runnable {

        @Override
        public void run() {

            while (mInventoryStart) {

                mDevice.getTagIDs().clear();
                Message message = Message.obtain();// Avoid repeated application of memory, reuse of information
                message.what = 1;
                handlerRfid.sendMessage(message);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void connectSignalRServer(){
        final ProgressDialog loading = new ProgressDialog(MainActivity.this);
        loading.setMessage("Please Wait...");
        loading.setCanceledOnTouchOutside(false);
        loading.show();
        try {
            hubConnection = new HubConnection("http://10.8.15.70:8080");
            hubConnection.connected(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"Berhasil terhubung dengan SignalR Server");
//                    btnStatusSignalRServer.setBackgroundResource(R.drawable.shape_green_circle);
//                    llReconnected.setVisibility(View.GONE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Berhasil terhubung dengan SignalR Server", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            hubProxy = hubConnection.createHubProxy("SimpleHub");

            ClientTransport clientTransport = new ServerSentEventsTransport((hubConnection.getLogger()));
            SignalRFuture<Void> signalRFuture = hubConnection.start(clientTransport);
            try {
                signalRFuture.get();
                loading.dismiss();
            } catch (InterruptedException | ExecutionException e) {
                loading.dismiss();

                Log.e(TAG, e.toString());
                Log.e(TAG, "connectSignalRServer: " + e.getMessage());
                e.printStackTrace();

                return;
            }
        }catch (Exception ex){
            loading.dismiss();
            Log.e(TAG, "connectSignalRServer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void sendSignalR(){
        try{
            String barcode = listEditText.get(0).getText().toString();
            String rfid = listEditText.get(1).getText().toString();
            if(barcode.isEmpty() || rfid.isEmpty())
                return;

            tvResult.append("Serial Number = " + barcode + "\n" +
                    "RFID = " + rfid + "\n\n");
            barcode = barcode.trim();
            rfid = rfid.trim();
            hubProxy.invoke("Send","SendKey", "{\"barcode\":\""+barcode+"\",\"rfid\":\""+rfid+"\"}");

            listEditText.get(0).setText("");
            listEditText.get(1).setText("");
        }
        catch (Exception ex){
            Log.e(TAG, "sendSignalR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}