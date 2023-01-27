package com.polytron.researchseuic2.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polytron.researchseuic2.R;
import com.polytron.researchseuic2.models.ScanResult;

import java.util.ArrayList;

public class ScanResultAdapter extends ArrayAdapter<ScanResult>{
//    private ArrayList<ScanResult> scanResultModels;
    Context context;
    ArrayList<ScanResult> listScanResult;
    static String TAG = "ScanResultAdapter";
//    private final String nomorResiAndRFID;
//    private final String date;
//    private final Activity context;

    private static class ViewHolder {
        TextView RFID;
        TextView serialNumber;
        TextView date;
        TextView counterRFID;
        TextView counterSerialNum;
    }

    public ScanResultAdapter(ArrayList<ScanResult> listScanResult, Context context) {
        super(context, R.layout.custom_listview, listScanResult);
        this.listScanResult = listScanResult;
        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ScanResult scanResult = getItem(position);
        ViewHolder viewHolder;

        final View result;

        viewHolder = new ViewHolder();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        view = inflater.inflate(R.layout.custom_listview, parent, false);
        viewHolder.RFID = (TextView) view.findViewById(R.id.tvScannedRFID);
        viewHolder.serialNumber = (TextView) view.findViewById(R.id.tvScannedSerialNum);
        viewHolder.counterRFID = (TextView) view.findViewById(R.id.tvCounterRFID);
        viewHolder.counterSerialNum = (TextView) view.findViewById(R.id.tvCounterSerialNum);
        viewHolder.date = (TextView) view.findViewById(R.id.tvDate);

        result = view;
        view.setTag(viewHolder);
//        if(view == null ) {
//        }
//        else {
//            viewHolder = (ViewHolder) view.getTag();
//            result=view;
//        }

//        Log.e(TAG, "ScanResultAdapter working!");

        if(scanResult.getRFID().isEmpty()) {
            viewHolder.RFID.setVisibility(View.GONE);
            viewHolder.counterRFID.setVisibility(View.GONE);
        }
        if(scanResult.getSerialNumber().isEmpty()) {
            viewHolder.serialNumber.setVisibility(View.GONE);
            viewHolder.counterSerialNum.setVisibility(View.GONE);
        }
        viewHolder.RFID.setText("RFID : " + scanResult.getRFID());
        viewHolder.serialNumber.setText("Nomor Seri : " + scanResult.getSerialNumber());
        viewHolder.date.setText(scanResult.getDate());
        viewHolder.counterRFID.setText("//" + scanResult.getCounterRFID() + "");
        viewHolder.counterSerialNum.setText("//" + scanResult.getCounterSerialNum() + "");

        return view;
    }
}
