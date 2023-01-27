package com.polytron.researchseuic2.models;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.polytron.researchseuic2.R;
import com.seuic.uhf.UHFService;

import java.util.ArrayList;

public class DialogReaderSetting {
    public interface CallbackReaderSetting{
        void onResponse(boolean success, String msg);
        void closeDialog();
        void error(Exception ex);
    }

    static final String TAG = "ResponseReaderSetting";

    public static void configurationReader(Activity activity, UHFService mDevice, final CallbackReaderSetting callback){
        try{
            String[] arrPower = new String[33];
            for(int i = 1; i <= 33; i++){
                arrPower[i-1] = i + "";
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, R.layout.custom_spinner_item_1, arrPower);
            adapter.setDropDownViewResource(R.layout.custom_spinner_item_1);

            SettingReaderManager srm = new SettingReaderManager(activity);
            ArrayList<String> listRegFreq = new ArrayList<>();
//            listRegFreq.add(DataUtils.FREQ_POINT_NORTH_AMERICA);
//            listRegFreq.add(DataUtils.FREQ_POINT_EUROPE);
//            listRegFreq.add(DataUtils.FREQ_POINT_CHINA);

            final AlertDialog alertDialog;
            final AlertDialog.Builder dialog;
            LayoutInflater inflater;
            final View dialogView;

            inflater = activity.getLayoutInflater();
            dialogView = inflater.inflate(R.layout.form_reader_setting, null);

            dialog = new AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .setPositiveButton("Apply", null)
                    .setNegativeButton("Cancel", null);

            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(dialogView.getWindowToken(), 0);

            final LinearLayout llSession = (LinearLayout) dialogView.findViewById(R.id.llSession);
            final RadioButton rbS0 = (RadioButton) dialogView.findViewById(R.id.rbS0),
                    rbS1 = (RadioButton) dialogView.findViewById(R.id.rbS1),
                    rbS2 = (RadioButton) dialogView.findViewById(R.id.rbS2),
                    rbS3 = (RadioButton) dialogView.findViewById(R.id.rbS3),
                    rbReg0 = (RadioButton) dialogView.findViewById(R.id.rbReg0),
                    rbReg1 = (RadioButton) dialogView.findViewById(R.id.rbReg1),
                    rbReg2 = (RadioButton) dialogView.findViewById(R.id.rbReg2),
                    rbSignalR = (RadioButton) dialogView.findViewById(R.id.rbSignalR),
                    rbBluetooth = (RadioButton) dialogView.findViewById(R.id.rbBluetooth);
//            final EditText etTransmitPower = (EditText) dialogView.findViewById(R.id.etTransmitPower);
            final EditText etRssi = (EditText) dialogView.findViewById(R.id.etRssi);
            final Button btnMinus = (Button) dialogView.findViewById(R.id.btnMinus),
                    btnPlus = (Button) dialogView.findViewById(R.id.btnPlus);
            final CheckBox cbFilterRssi = (CheckBox) dialogView.findViewById(R.id.cbFilterRssi);
            final LinearLayout llFilterRssi = (LinearLayout) dialogView.findViewById(R.id.llFilterRssi);
            final LinearLayout llSignalR = (LinearLayout) dialogView.findViewById(R.id.llSignalR);
            final LinearLayout llBluetooth = (LinearLayout) dialogView.findViewById(R.id.llBluetooth);
            final RadioGroup rbGroupRegFreq = (RadioGroup) dialogView.findViewById(R.id.rbGroupRegFreq);
            final RadioGroup rbGroupMethod = (RadioGroup) dialogView.findViewById(R.id.rbGroupMethod);
            final TextView tvFreq = (TextView) dialogView.findViewById(R.id.tvFreq);
            final Spinner spinnerPower = (Spinner) dialogView.findViewById(R.id.spinnerPower);
            rbReg0.setText("FCC");
            rbReg1.setText("ETSI");
            rbReg2.setText("China1");

            llSession.setVisibility(View.GONE);
            tvFreq.setVisibility(View.GONE);
//            if(AppConstants.session == 0)
//                rbS0.setChecked(true);
//            else if(AppConstants.session == 1)
//                rbS1.setChecked(true);
//            else if(AppConstants.session == 2)
//                rbS2.setChecked(true);
//            else if(AppConstants.session == 3)
//                rbS3.setChecked(true);

            if(AppConstants.method.equals("signalr")){
                rbSignalR.setChecked(true);
            }
            else if(AppConstants.method.equals("bluetooth")){
                rbBluetooth.setChecked(true);
            }

            if(AppConstants.regFreq.equals("FCC")){
                rbReg0.setChecked(true);
            }
            else if(AppConstants.regFreq.equals("ETSI")){
                rbReg1.setChecked(true);
            }
            else if(AppConstants.regFreq.equals("China1")){
                rbReg2.setChecked(true);
            }

//            etTransmitPower.setText(AppConstants.power + "");
            spinnerPower.setAdapter(adapter); spinnerPower.setSelection(adapter.getPosition(AppConstants.power + ""));
            cbFilterRssi.setChecked(AppConstants.filterRssi);
            llFilterRssi.setVisibility((cbFilterRssi.isChecked()) ? View.VISIBLE : View.GONE);
            etRssi.setText(AppConstants.rangeRssi + "");

            rbGroupMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    int selectedId = rbGroupMethod.getCheckedRadioButtonId();
                    if(selectedId != -1){
                        RadioButton rbBtnSelected = (RadioButton) dialogView.findViewById(selectedId);
                        if(rbBtnSelected.getText().toString().toLowerCase().equals("signalr client")){
                            llSignalR.setVisibility(View.VISIBLE);
                            llBluetooth.setVisibility(View.GONE);
                        }
                        else if(rbBtnSelected.getText().toString().toLowerCase().equals("bluetooth")){
                            llSignalR.setVisibility(View.GONE);
                            llBluetooth.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });

            rbGroupRegFreq.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    int selectedId = rbGroupRegFreq.getCheckedRadioButtonId();
                    if(selectedId != -1){
                        RadioButton rbBtnSelected = (RadioButton) dialogView.findViewById(selectedId);
                        if(rbBtnSelected.getText().toString().toLowerCase().equals("north american"))
                            tvFreq.setText("Frequency : " + listRegFreq.get(0));
                        else if(rbBtnSelected.getText().toString().toLowerCase().equals("europe"))
                            tvFreq.setText("Frequency : " + listRegFreq.get(1));
                        else if(rbBtnSelected.getText().toString().toLowerCase().equals("china"))
                            tvFreq.setText("Frequency : " + listRegFreq.get(2));
                    }
                }
            });

            cbFilterRssi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b)
                        llFilterRssi.setVisibility(View.VISIBLE);
                    else
                        llFilterRssi.setVisibility(View.GONE);
                }
            });

            btnMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int rssi = Integer.parseInt(etRssi.getText().toString());
                    if(rssi < -30){
                        etRssi.setText(String.valueOf(Integer.parseInt(etRssi.getText().toString()) + 1));
                    }
                }
            });

            btnPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int rssi = Integer.parseInt(etRssi.getText().toString());
                    if(rssi > -60){
                        etRssi.setText(String.valueOf(Integer.parseInt(etRssi.getText().toString()) - 1));
                    }
                }
            });

            alertDialog = dialog.show();
            Button posBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            posBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try{
                        //                    if(rbS0.isChecked())
//                        AppConstants.session = 0;
//                    else if(rbS1.isChecked())
//                        AppConstants.session = 1;
//                    else if(rbS2.isChecked())
//                        AppConstants.session = 2;
//                    else if(rbS3.isChecked())
//                        AppConstants.session = 3;


                        String tempRegFreq = "";
                        if(rbReg0.isChecked())
                            AppConstants.regFreq = rbReg0.getText().toString();
                        else if(rbReg1.isChecked())
                            AppConstants.regFreq = rbReg1.getText().toString();
                        else if(rbReg2.isChecked())
                            AppConstants.regFreq = rbReg2.getText().toString();

                        if(!"".equals(tempRegFreq)){
                            if(!mDevice.setRegion(tempRegFreq)){
                                Toast.makeText(activity, "Gagal melakukan pengaturan \"Region Frequency\"....", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            AppConstants.regFreq = tempRegFreq;
                        }

                        int convertPowerToInt = Integer.parseInt(spinnerPower.getSelectedItem().toString());
                        int tempPower = convertPowerToInt <= 0 ? 1 : convertPowerToInt >= 34 ? 33 : convertPowerToInt;
                        if(!mDevice.setPower(tempPower)){
                            Toast.makeText(activity, "Gagal melakukan pengaturan \"Power\"....", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        AppConstants.power = tempPower;
                        AppConstants.filterRssi = cbFilterRssi.isChecked();
                        AppConstants.rangeRssi = Integer.parseInt(etRssi.getText().toString());
                        srm.saveSettingReader();
                        alertDialog.dismiss();
                        callback.onResponse(true, "Setting Reader sudah Tersimpan....");
                    }
                    catch (Exception ex){
                        callback.error(ex);
                    }
                }
            });

            Button negBtn = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });

            callback.closeDialog();
        }
        catch (Exception ex){
            Log.e(TAG, "settingReader: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
