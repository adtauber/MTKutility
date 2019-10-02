package com.adtdev.mtkutility;
/**
 * @author Alex Tauber
 * <p>
 * This file is part of the open source Android app MTKutility. You can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 3 of the License. This extends to files included that were authored by
 * others and modified to make them suitable for this app. All files included were subject to
 * open source licensing.
 * <p>
 * MTKutility is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You can review a copy of the
 * GNU General Public License at http://www.gnu.org/licenses.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.Toast;

public class GetLogFragment extends Fragment {

    private String[] parms;

    private View rootView;
    public TextView tv1;
    private ScrollView mSv;
    private TextView mTv;
    public ProgressBar mProgress;
    private Button btnRun;
    private boolean ok = true;

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private Context mContext = Main.mContext;
    private int debugLVL = 0;
    private final int ABORT = 9;
    private OutputStreamWriter logWriter = Main.logWriter;
    private String NL = System.getProperty("line.separator");
    private String binPathName;
    private boolean logFileIsOpen = Main.logFileIsOpen;
    private int logRecCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log(0, "GetLogFragment.onCreateView()");

        publicPrefs = Main.publicPrefs;
        publicPrefEditor = Main.publicPrefEditor;
        appPrefs = Main.appPrefs;
        appPrefEditor = Main.appPrefEditor;
        binPathName = appPrefs.getString("binPathName", "");

        rootView = inflater.inflate(R.layout.getlog, container, false);
//        LogTxt = rootView.findViewById(R.id.LogTxt);
        tv1 = rootView.findViewById(R.id.tv1);
        mSv = rootView.findViewById(R.id.mSv);
        mTv = rootView.findViewById(R.id.mTv);
        mProgress = getActivity().findViewById(R.id.circularProgressbar);

        btnRun = rootView.findViewById(R.id.run);
        btnRun.setTransformationMethod(null);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(0, "GetLogFragment - button " + btnRun.getText() + " pressed");
                new logDownload(getActivity()).execute();
            }
        });

        return rootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log(0, "GetLogFragment.onViewCreated()");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugPref", "0"));
        new GetLogFragment.getRecCount(getActivity()).execute();
        btnRun.setEnabled(false);
    }//onViewCreated()

    private void appendMsg(String msg) {
        mTv.append(msg + NL);
        scrollDown();
    }//appendMsg()

    private void scrollDown() {
        final ScrollView scrollView = getActivity().findViewById(R.id.mSv);
        scrollView.post(new Runnable() {
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }//scrollDown()

    private void goSleep(int mSec) {
        Log(3, String.format("GetLogFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(e);
        }
    }//goSleep()

    private void Log(int mode, String msg) {
        if (!logFileIsOpen) {
            return;
        }
        switch (mode) {
            case 0:
                if (msg.length() > 127) {
                    msg = msg.substring(0, 60) + " ... " + msg.substring(msg.length() - 30);
                }
                break;
            case 1:
                if (mode > debugLVL) {
                    return;
                }
                break;
            case 2:
                if (mode > debugLVL) {
                    return;
                }
                break;
            case 3:
                if (mode > debugLVL) {
                    return;
                }
                break;
            case ABORT:
                throw new RuntimeException(msg);
        }
        String time = DateFormat.getDateTimeInstance().format(new Date());
        time = time.substring(12);
        time = time.replace("AM", "");
        time = time.replace("PM", "");
        try {
            logWriter.append(time + " " + msg + NL);
            logWriter.flush();
        } catch (IOException e) {
            Main.buildCrashReport(e);
        }
    }//Log()

    private class getRecCount extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private int downDelay;
        private int ix;

        public getRecCount(Context context) {
            Log(0, "GetLogFragment.getRecCount.getRecCount()");
            mContext = context;
        }//getRecCount()

        protected void onPreExecute() {
            Log(0, "GetLogFragment.getRecCount.onPreExecute()");
            downDelay = Integer.parseInt(publicPrefs.getString("downDelay", "50"));
            dialog = new ProgressDialog(mContext);
            this.dialog.setMessage(getString(R.string.getSetngs));
            this.dialog.show();
            Main.stopBkGrnd = false;
        }//onPreExecute()

        protected Void doInBackground(Void... params) {
            String curFunc = "GetLogFragment.getRecCount.doInBackground()";
            Log(0, curFunc);
            ix = 10;
            while (ix > 0) {
                Log(1, String.format("%1$s getting log record count (PMTK182,2,10) retry %2$d", curFunc, ix));
                parms = Main.mtkCmd("PMTK182,2,10", "PMTK182,3,10", downDelay);
                if (parms == null) {
                    ix--;
                    continue;
                }
                if (parms[0].contains("PMTK182") && parms[1].contains("3")) {
                    ix = 0;
                    logRecCount = Integer.parseInt(parms[3], 16);
                    Log(0, String.format("%1$s Log has %2$d records", curFunc, logRecCount));
                } else {
                    ix--;
                    continue;
                }
            }
            ;
            return null;
        }//doInBackground()

        protected void onPostExecute(Void param) {
            Log(0, "GetLogFragment.getRecCount.onPostExecute()");
            if (Main.stopBkGrnd) return;
            if (dialog.isShowing()) dialog.dismiss();
            myHandler.sendEmptyMessage(0);
            Main.stopBkGrnd = true;
        }//onPostExecute()
    }//class getRecCount

    private class logDownload extends AsyncTask<Void, String, Void> {

        ProgressBar mProgress = getActivity().findViewById(R.id.circularProgressbar);
        TextView tv1 = getActivity().findViewById(R.id.tv1);
        TextView tv2 = getActivity().findViewById(R.id.tv2);
        private String file_time_stamp;
        //        private String[] reply = null;
        private boolean aborting = false;
        private boolean OK = true;
        private boolean firstProg = true;
        private int offset = 0;
        private String cmd;
        private BufferedOutputStream binOout = null;
        private String[] sArray;
        private int s3len;
        private int pct;
        private int bRead = 0;
        private Date dstart;
        private Date dend;
        SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.CANADA);
        private File binPath;
        private File binFile;

        private boolean stopNMEA;
        private boolean stopLOG;
        private int downBlockSize;
        private int cmdDelay;
        private int downDelay;
        private FileOutputStream bOut;
        private boolean binFileIsOpen = false;
        private int cmdRetries = 0;
        private int bMax = 0;
        private String msg;

        public logDownload(Context context) {
            Log(1, "GetLogFragment.logDownload.logDownload()");
            mContext = context;
        }//logDownload()

        @Override
        protected void onPreExecute() {
            Log(0, "GetLogFragment.logDownload.onPreExecute()");
            Main.stopBkGrnd = false;
            initProgress();
            btnRun.setEnabled(false);
            dstart = new Date();
            appendMsg(String.format(getString(R.string.logBgn), SDF.format(dstart)));
            binPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), binPathName);
            stopNMEA = publicPrefs.getBoolean("stopNMEA", true);
            stopLOG = publicPrefs.getBoolean("stopLOG", false);
            cmdRetries = Integer.parseInt(publicPrefs.getString("cmdRetries", "5"));
            cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
            downDelay = Integer.parseInt(publicPrefs.getString("downDelay", "50"));
            downBlockSize = Integer.parseInt(publicPrefs.getString("downBlockSize", "2048"));
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            String curFunc = "GetLogFragment.logDownload.doInBackground()";
            Log(1, curFunc);
            byte[] binBytes = null;

            //create temp output
            binFileIsOpen = BINopen();
            if (!binFileIsOpen) {
                aborting = true;
                msg = String.format(getString(R.string.dlbAbort), binFile);
                return null;
            }

            if (stopNMEA) NMEAstop();
            if (stopLOG) LOGstop();

            bMax = getBytesToRead();
            if (aborting) {
                return null;
            }
            bRead = 0;
//            Log(1, String.format("%1$s bRead=%2$d bMax=%3$d ", curFunc, bRead, bMax));
            while ((bRead < bMax) && !Main.stopBkGrnd) {
                Log(1, String.format("%1$s bRead=%2$d bMax=%3$d ", curFunc, bRead, bMax));
//                for (int ix = 0; ix <= cmdRetries; ix++) {
                binBytes = processBLK();
                if (aborting) {
                    return null;
                }
//                }
                Log(1, String.format("%1$s received %2$d bytes", curFunc, binBytes.length));
                if (binBytes.length > 0) {
                    try {
                        bOut.write(binBytes);
                        Log(1, String.format("%1$s wrote %2$d bytes to file", curFunc, binBytes.length));
                        offset += binBytes.length;
                        bRead += binBytes.length;
                    } catch (IOException e) {
                        Main.buildCrashReport(e);
                        return null;
                    }
                    publishProgress(" ");
                }
                goSleep(downDelay);
            }
            BINclose();
            if (stopNMEA && !Main.stopBkGrnd) NMEAstart();
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            Log(0, "GetLogFragment.logDownload.onPostExecute()");
            if (Main.stopBkGrnd) return;
            SimpleDateFormat FDF = new SimpleDateFormat("yyyy.MM.dd.HHmmss", Locale.CANADA);

            if (binFileIsOpen) {
                try {
                    bOut.flush();
                    bOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (aborting) {
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                return;
            }

            Date dt = new Date();
            File nn = new File(binPath, FDF.format(dt) + ".bin");
            // rename binary file to today's date and time
            binFile.renameTo(nn);
            appendMsg(getText(R.string.created) + nn.toString());
//            btnErase.setVisibility(View.VISIBLE);
            btnRun.setEnabled(true);
            dend = new Date();
            long diff = dend.getTime() - dstart.getTime();
            diff = diff / 1000;
            long minutes = diff / 60;
            long seconds = diff - (minutes * 60);
            long hours = minutes / 60;
            appendMsg(String.format(getString(R.string.logEnd), SDF.format(dend)));
            appendMsg(String.format("Log downlaod time %1$d hours, %2$d minutes, %3$d seconds", hours, minutes, seconds));
            Main.stopBkGrnd = true;
        }//onPostExecute()

        //        @Override
        //String values expected:  Action, Style, message, percent
        protected void onProgressUpdate(String... values) {
            pct = (bRead * 100) / bMax;
            if (pct > 100) {
                pct = 100;
            }
            Log(1, String.format("+++ onProgressUpdate +++ bRead=%1$d  bMax=%2$d  %3$d percent", bRead, bMax, pct));
            mProgress.setProgress(pct);
            tv1.setText(Integer.toString(bRead));
            if (firstProg) {
                appendMsg(String.format("%,d", logRecCount) + " log records  " + String.format("%,d", bMax) + " bytes");
                firstProg = false;
            }
        }//onProgressUpdate()

        private String concatSarray(String[] Sa, int bgn) {
            Log(0, "GetLogFragment.logDownload.concatSarray()");
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (String s : Sa) {
                if (i >= bgn) {
                    builder.append(s);
                    if (i < Sa.length - 1) {
                        builder.append(",");
                    }
                }
                i++;
            }
            return builder.toString();
        }//concatSarray()

        private int getBytesToRead() {
            String curFunc = "GetLogFragment.logDownload.getBytesToRead()";
            Log(1, curFunc);
            // Query the RCD_ADDR (data Log Next Write Address).
            int retry = cmdRetries;
            while (retry > 0) {
                parms = Main.mtkCmd("PMTK182,2,8", "PMTK182,3,8", cmdDelay);
                retry--;
                if (parms == null) {
                    goSleep(cmdDelay * 2);
                    continue;
                }
                if (parms[0].contains("PMTK182")) {
                    if (parms[1].contains("3")) return Integer.parseInt(parms[3], 16);
                } else {
                    goSleep(cmdDelay * 2);
                    continue;
                }
            }
            aborting = true;
            msg = String.format(getString(R.string.dlAbort), "PMTK182,2,8");
            return 0;
        }//getBytesToRead()

        private void getNMEAsetting() {
            String curFunc = "GetLogFragment.logDownload.getNMEAsetting()";
            Log(0, curFunc);

            int retry = 10;
            boolean OK = false;
            do {
                retry--;
                //get NMEA output setting from GPS
                parms = Main.mtkCmd("PMTK414", "PMTK514", cmdDelay);
                if (parms == null) {
                    continue;
                }
                if (!parms[0].contains("PMTK514")) {
                    continue;
                }
                //check settings string to make sure we have at least 1 output
                OK = false;
                for (int ix = 1; ix <= 19; ix++) {
                    if (!parms[ix].contains("0")) {
                        OK = true;
                    }
                }
                if (OK) {
                    appPrefEditor.putString("saveMNEA", concatSarray(parms, 0));
                    appPrefEditor.commit();
                } else {
                    //reset GPS to default output if all 0 in settings
                    Log(1, String.format("%1$s invalid NMEA stored - resetting", curFunc));
                    Main.mtkCmd("PMTK314,-1", "PMTK001", cmdDelay);
                    goSleep(200);
                    retry = 10;
                    continue;
                }
            } while (!OK && retry > 0);
        }//getNMEAsetting()

        private void initProgress() {
            String curFunc = "GetLogFragment.logDownload.initProgress()";
            Log(1, curFunc);
            mProgress.setProgress(0);   // Main Progress
            mProgress.setMax(100); // Maximum Progress
            mProgress.setSecondaryProgress(100); // Secondary Progress
            tv1.setText("0");
            tv2.setText("bytes");
        }//initProgress()

        private void NMEAstart() {
            String curFunc = "GetLogFragment.logDownload..NMEAstart()";
            Log(0, curFunc);
            Log(1, String.format("%1$s retreiving saved preference", curFunc));
            String cmd = appPrefs.getString("saveMNEA", "PMTK314,0,1,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0");
            String[] sArray = cmd.split(",");
            OK = false;
            for (int ix = 1; ix <= 19; ix++) {
                if (!sArray[ix].contains("0")) {
                    OK = true;
                }
            }
            if (!OK) {
                //reset NMEA outpu to default
                Main.mtkCmd("PMTK314,-1", "PMTK001", cmdDelay);
                getNMEAsetting();
            }
        }//NMEAstart()

        private void NMEAstop() {
            String curFunc = "GetLogFragment.logDownload..NMEAstop()";
            Log(0, curFunc);
            int retry = cmdRetries;
            tryagain:
            while (retry > 0) {
                parms = Main.mtkCmd("PMTK314,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "PMTK001,314", cmdDelay);
                goSleep(cmdDelay);
                parms = Main.mtkCmd("PMTK414", "PMTK514", cmdDelay);
                retry--;
                if (parms == null) continue;
                for (int ix = 1; ix <= 19; ix++) {
                    if (parms[ix].contains("1")) continue tryagain;
                }
            }
        }//NMEAstop()

        private void LOGstop() {
            String curFunc = "GetLogFragment.logDownload..LOGstop()";
            Log(0, curFunc);
            int retry = cmdRetries;
            while (retry > 0) {
                parms = Main.mtkCmd("PMTK182,5", "PMTK001,182,5", cmdDelay);
                retry--;
                if (parms == null) {
                    continue;
                }
                if (parms[0].contains("PMTK001")) {
                    if (parms[1].contains("182") && !parms[3].contains("3")) continue;
                } else {
                    continue;
                }
            }
        }//NMEAstop()

        private void LOGstart() {
            String curFunc = "GetLogFragment.logDownload..LOGstart()";
            Log(0, curFunc);
            curFunc = String.format("+++ %1$s +++", curFunc);
            parms = Main.mtkCmd("PMTK182,4", "PMTK001,182,4", cmdDelay);

//            Main.sendCommand("PMTK182,4");
//            Main.waitForReply("PMTK001,182,4,3");
//        goSleep(2000);
        }//NMEAstop()

        private byte[] processBLK() {
            String curFunc = "GetLogFragment.logDownload.processBLK()";
            Log(1, curFunc);
            int ix = 0;
//            for (int ix = 0; ix <= cmdRetries; ix++) {
            while (ix < cmdRetries) {
                ix++;
                Log(2, String.format("%1$s command retry %2$d", curFunc, ix));
                cmd = String.format("PMTK182,7,%08X,%08X", offset, downBlockSize);
                parms = Main.mtkCmd(cmd, "PMTK182,8", downDelay);
                if (parms == null || parms.length < 1) continue;
                if (parms[0].contains("PMTK182")) {
                    int retAddrs = Integer.parseInt(parms[2], 16);
                    if (retAddrs == offset) {
                        s3len = parms[3].length();
                        // string returned length needs to be twice the blkSize
                        if (s3len % 2 != 0) {
                            Log(1, String.format("%1$s needed even byte count-received %2$d", curFunc, s3len));
                            continue;
                        }
                        if (s3len / 2 != downBlockSize) {
                            Log(1, String.format("%1$s needed %2$d byte count-received %3$d", curFunc, downBlockSize, s3len));
                            continue;
                        }
                        //valid NMEA string has been found - break out of loop
                        ix = 99;
                        Log(1, String.format("%1$s bin length is %2$d bytes", curFunc, s3len));
                        Log(3, String.format("%1$s <%2$s>", curFunc, parms[3]));
                    }
                }
                if (parms[0].contains("PMTK001") && !parms[parms.length - 1].contains("3")) {
                    //PMTK182,7 failed - need to abort
                    aborting = true;
                    msg = String.format(getString(R.string.dlAbort), cmd);
                    return null;
                }
            }
            //convert returned value a to binary string
            String string_byte;
            int jx = 0;
            Log(1, String.format("%1$s s3len=%2$d blkSize=%3$d", curFunc, s3len, downBlockSize));
            byte[] binArray = new byte[downBlockSize];
            for (ix = 0; ix < (s3len); ix += 2) {
                string_byte = parms[3].substring(ix, ix + 2);
                try {
                    binArray[jx] = (byte) (Integer.parseInt(string_byte, 16) & 0xFF);
                    jx++;
                } catch (NumberFormatException e) {
                    aborting = true;
                    msg = getString(R.string.dlcAbort);
                    return null;
                }
            }
            Log(1, String.format("%1$s return size is %2$d characters", curFunc, binArray.length));
            return binArray;
        }//processBLK()

        private boolean BINopen() {
            String curFunc = "GetLogFragment.logDownload..BINopen()";
            Log(1, curFunc);
            String binFileName = "temp.bin";
            // make sure mtkutility/bin directory exists - create if it is missing
            if (!binPath.exists()) {
                OK = binPath.mkdirs();
            }
            if (!OK) {
                return false;
            }
            // Create bin file for log download
            binFile = new File(binPath, binFileName);
            if (binFile.exists()) {
                binFile.delete();
            }

            try {
                binFile.createNewFile();
                bOut = new FileOutputStream(binFile);
                return true;
            } catch (IOException e) {
                Main.buildCrashReport(e);
            }
            return false;
        }//BINopen()

        private void BINclose() {
            String curFunc = "GetLogFragment.logDownload..BINclose()";
            Log(1, curFunc);
            if (binFileIsOpen) {
                try {
                    bOut.flush();
                    bOut.close();
                } catch (IOException e) {
                    Main.buildCrashReport(e);
                }
            }
            binFileIsOpen = false;
        }//BINclose()

    }//class logDownload

    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(@org.jetbrains.annotations.NotNull Message msg) {
            String curFunc = "GetLogFragment.myHandler.handleMessage()";
            Log(0, curFunc);
            switch (msg.what) {
                case 0:
                    if (logRecCount < 1) {
                        appendMsg(getString(R.string.noLogDL));
                        Toast.makeText(mContext, getString(R.string.noLogDL), Toast.LENGTH_LONG).show();
                    } else {
                        appendMsg(String.format(getString(R.string.GPSrecs), logRecCount));
                        btnRun.setEnabled(true);
                    }
                    break;
                default:
                    break;
            }
        }
    };//Handler myHandler
}//class GetLogFragment

