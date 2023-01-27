package com.polytron.researchseuic2.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class DeviceBluetoothManager {
    static final String TAG = "DEVICE_BLUETOOTH_MANAGER";
    private static final String SHARED_PREF_NAME = "DeviceBluetoothPolytron";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Activity activity;

    public DeviceBluetoothManager(Activity activity){
        this.activity = activity;
        sharedPreferences = activity.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
}
