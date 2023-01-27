package com.polytron.researchseuic2.models;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SettingReaderManager {
    static final String TAG = "SETTING_READER_MANAGER";
    private static final String SHARED_PREF_NAME = "mySettingReaderSeuic";

    private static final String KEY_POWER = "power";
    private static final String KEY_FILTER_RSSI = "filter_rssi";
    private static final String KEY_RANGE_RSSI = "range_rssi";
//    private static final String KEY_SESSION = "session";
    private static final String KEY_REGION_FREQUENCY = "region_frequency";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Activity activity;

    public SettingReaderManager(Activity activity){
        this.activity = activity;
        sharedPreferences = this.activity.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveSettingReader(){
        try{
            editor.putInt(KEY_POWER, AppConstants.power);
            editor.putBoolean(KEY_FILTER_RSSI, AppConstants.filterRssi);
            editor.putInt(KEY_RANGE_RSSI, AppConstants.rangeRssi);
//            editor.putInt(KEY_SESSION, AppConstants.session);
            editor.putString(KEY_REGION_FREQUENCY, AppConstants.regFreq);
            editor.apply();
            System.out.println("Setting Reader Saved");
        }
        catch (Exception e){
            Log.e(TAG, "saveSettingReader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getPower(){
        try{
            if(!sharedPreferences.contains(KEY_POWER)){
                editor.putInt(KEY_POWER, AppConstants.power);
                editor.apply();
                return AppConstants.power;
            }
            else
                return sharedPreferences.getInt(KEY_POWER, AppConstants.power);
        }
        catch (Exception e){
            throw e;
        }
    }

    public boolean getFilterRssi(){
        try{
            if(!sharedPreferences.contains(KEY_FILTER_RSSI)){
                editor.putBoolean(KEY_FILTER_RSSI, AppConstants.filterRssi);
                editor.apply();
                return AppConstants.filterRssi;
            }
            else
                return sharedPreferences.getBoolean(KEY_FILTER_RSSI, false);
        }
        catch (Exception e){
            throw e;
        }
    }

    public int getRangeRssi(){
        try{
            if(!sharedPreferences.contains(KEY_RANGE_RSSI)){
                editor.putInt(KEY_RANGE_RSSI, AppConstants.rangeRssi);
                editor.apply();
                return AppConstants.rangeRssi;
            }
            else
                return sharedPreferences.getInt(KEY_RANGE_RSSI, AppConstants.rangeRssi);
        }
        catch (Exception e){
            throw e;
        }
    }

//    public int getSession(){
//        try{
//            if(!sharedPreferences.contains(KEY_SESSION)){
//                editor.putInt(KEY_SESSION, AppConstants.session);
//                editor.apply();
//                return AppConstants.session;
//            }
//            else
//                return sharedPreferences.getInt(KEY_SESSION, AppConstants.session);
//        }
//        catch (Exception e){
//            throw e;
//        }
//    }

    public String getRegFreq(){
        try{
            if(!sharedPreferences.contains(KEY_REGION_FREQUENCY)){
                editor.putString(KEY_REGION_FREQUENCY, AppConstants.regFreq);
                editor.apply();
                return AppConstants.regFreq;
            }
            else
                return sharedPreferences.getString(KEY_REGION_FREQUENCY, AppConstants.regFreq);
        }
        catch (Exception e){
            throw e;
        }
    }
}
