package com.polytron.researchseuic2.models;

public class ScanResult {
    String RFID, serialNumber, date;
    int counterRFID, counterSerialNum;

    public ScanResult() {
        this.RFID = "";
        this.serialNumber = "";
        this.counterRFID = 0;
        this.date = "";
    }

    public ScanResult(String RFID, String serialNumber, String date, int counterRFID, int counterSerialNum) {
        this.RFID = RFID;
        this.serialNumber = serialNumber;
        this.date = date;
        this.counterRFID = counterRFID;
        this.counterSerialNum = counterSerialNum;
    }

    public String getRFID() {
        return RFID;
    }

    public void setRFID(String RFID) {
        this.RFID = RFID;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCounterRFID() {
        return counterRFID;
    }

    public void setCounterRFID(int counterRFID) {
        this.counterRFID = counterRFID;
    }

    public int getCounterSerialNum() {
        return counterSerialNum;
    }

    public void setCounterSerialNum(int counterSerialNum) {
        this.counterSerialNum = counterSerialNum;
    }
}
