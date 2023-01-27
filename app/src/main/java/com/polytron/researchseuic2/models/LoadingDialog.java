package com.polytron.researchseuic2.models;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.polytron.researchseuic2.R;

public class LoadingDialog {
    private Activity activity;
    private AlertDialog dialog;

    public LoadingDialog(Activity activity){
        this.activity = activity;
    }

    public void startLoadingDialog(String msg, boolean cancelable){
        try{
            if(dialog == null){
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                LayoutInflater inflater = activity.getLayoutInflater();
                View view = inflater.inflate(R.layout.custom_loading_1, null);

                TextView tvMessage = view.findViewById(R.id.tvMessage);
                tvMessage.setText(msg.equals("") ? "Mohon Menunggu..." : msg);
                builder.setView(view);
                builder.setCancelable(true);

                dialog = builder.create();
            }

            if(!dialog.isShowing()){
                dialog.show();
            }

        }
        catch (Exception ex){
            Log.e("LoadingDialog", "startLoadingDialog: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void dismissDialog(){
        try{
            if(dialog == null)
                return;

            if(dialog.isShowing())
                dialog.dismiss();
        }
        catch (Exception ex){
            Log.e("LoadingDialog", "startLoadingDialog: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
