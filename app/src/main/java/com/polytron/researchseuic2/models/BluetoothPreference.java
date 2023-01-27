package com.polytron.researchseuic2.models;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class BluetoothPreference {
    static String TAG = "BluetoothPreference";
    private static final String SHARED_PREF_NAME = "myBluetoothPreference";
    static final String KEY_BLUETOOTH_NAME = "bluetooth_name";
    static final String KEY_BLUETOOTH_ADDRESS = "bluetooth_address";

    android.content.SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Activity activity;

    public BluetoothPreference(Activity activity) {
        this.activity = activity;
        sharedPreferences = activity.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public boolean saveBluetoothData(String name, String address) {
        try {
            editor.putString(KEY_BLUETOOTH_NAME, name);
            editor.putString(KEY_BLUETOOTH_ADDRESS, address);
            editor.apply();
            Log.i(TAG, "saveBluetoothData: Success!");
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "saveBluetoothData: Error!");
            e.printStackTrace();
        }
        return false;
    }

    public void clearBluetoothData() {
        editor.remove(KEY_BLUETOOTH_NAME);
        editor.remove(KEY_BLUETOOTH_ADDRESS);
        editor.apply();
        Log.i(TAG, "clearBluetoothData: User Data Removed!");
    }

    public String getKeyBluetoothName() {
        try{
            return sharedPreferences.getString(KEY_BLUETOOTH_NAME, null);
        }
        catch (Exception e) {
            throw e;
        }
    }

    public String getKeyBluetoothAddress() {
        try{
            return sharedPreferences.getString(KEY_BLUETOOTH_ADDRESS, null);
        }
        catch (Exception e) {
            throw e;
        }
    }

}
