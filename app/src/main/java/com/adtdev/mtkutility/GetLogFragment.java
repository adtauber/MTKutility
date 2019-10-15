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

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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
    private Button btnReset;
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
        mLog(0, "GetLogFragment.onCreateView");

        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();
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
                mLog(0, "GetLogFragment - button " + btnRun.getText() + " pressed +++++");
                new logDownload(getActivity()).execute();
            }
        });


        btnReset = rootView.findViewById(R.id.reset);
        btnReset.setTransformationMethod(null);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetLogFragment - button " + btnReset.getText() + " pressed +++++");
//                appPrefs.getInt("DLcmd", 0);
                appPrefEditor.putInt("DLcmd", 0);
                appPrefEditor.commit();
                appendMsg(String.format(getString(R.string.logRS), 0));
                btnReset.setEnabled(false);
            }
        });

        return rootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        mLog(0, "GetLogFragment.onViewCreated");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugLVL", "0"));
        btnReset.setEnabled(false);
    }//onViewCreated()

    public void onPause() {
        super.onPause();
        String curFunc = "GetLogFragment.onPause";
        mLog(1, curFunc);
    }    //onPause()

    @Override
    public void onResume() {
        super.onResume();
        String curFunc = "GetLogFragment.onResume";
        mLog(1, curFunc);
        while (Main.BkGrndActive) {
            Main.BkGrndActive = false;
            goSleep(50);
        }
        logRecCount = appPrefs.getInt("logRecCount", 0);
        appendMsg(String.format(getString(R.string.logrecs), logRecCount));
        int recs = appPrefs.getInt("DLcmd", 0);
        if (recs > 0) {
            btnReset.setEnabled(true);
            appendMsg(String.format(getString(R.string.logRS), recs));
        }
    }    //onResume()

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
        mLog(3, String.format("GetLogFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(e);
        }
    }//goSleep()

    private void mLog(int mode, String msg) {
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

        private boolean noDLrestart = false;
        private boolean stopNMEA;
        private boolean stopLOG;
        private int downBlockSize;
        private int cmdDelay;
        private int dwnDelay;
        private int retryInc;
        private FileOutputStream bOut;
        private boolean binFileIsOpen = false;
        private int cmdRetry = 0;
        private int bMax = 0;
        private String msg;

        public logDownload(Context context) {
            mLog(1, "GetLogFragment.logDownload.logDownload");
            mContext = context;
        }//logDownload()

        @Override
        protected void onPreExecute() {
            String curFunc = "GetLogFragment.logDownload.doInBackground()";
            mLog(1, curFunc);
            goSleep(500);
            initProgress();
            while (Main.BkGrndActive) {
                Main.BkGrndActive = false;
                goSleep(250);
            }
            Main.BkGrndActive = true;
            btnRun.setEnabled(false);
            dstart = new Date();
            appendMsg(String.format(getString(R.string.logBgn), SDF.format(dstart)));
            binPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), binPathName);
            stopNMEA = publicPrefs.getBoolean("stopNMEA", true);
            stopLOG = publicPrefs.getBoolean("stopLOG", false);
            cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
            cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
            dwnDelay = Integer.parseInt(publicPrefs.getString("dwnDelay", "50"));
            retryInc = Integer.parseInt(publicPrefs.getString("retryInc", "0"));
            downBlockSize = Integer.parseInt(publicPrefs.getString("downBlockSize", "2048"));
            offset = appPrefs.getInt("DLcmd", 0);
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            String curFunc = "GetLogFragment.logDownload.doInBackground()";
            mLog(1, curFunc);
            byte[] binBytes = null;

            //create temp output
            binFileIsOpen = BINopen();
            if (!binFileIsOpen) {
                aborting = true;
                msg = String.format(getString(R.string.dlbAbort), binFile);
                return null;
            }

            if (stopNMEA) Main.NMEAstop();
            if (stopLOG) Main.LOGstop();

            bMax = getBytesToRead();
            if (aborting) return null;


            bRead = offset;
            while (bRead < bMax) {
                mLog(1, String.format("%1$s offset=%2$d bRead=%3$d bMax=%4$d ", curFunc, offset, bRead, bMax));
//                for (int retry = 0; retry <= cmdRetry; retry++) {
                if (!Main.BkGrndActive) return null;
                binBytes = processBLK();
                if (aborting) {
                    return null;
                }
                mLog(1, String.format("%1$s received %2$d bytes", curFunc, binBytes.length));
                if (binBytes.length > 0) {
                    try {
                        bOut.write(binBytes);
                        mLog(1, String.format("%1$s wrote %2$d bytes to file", curFunc, binBytes.length));
                        offset += binBytes.length;
                        bRead += binBytes.length;
                        appPrefEditor.putInt("DLcmd", offset);
                        appPrefEditor.commit();
                    } catch (IOException e) {
                        Main.buildCrashReport(e);
                        return null;
                    }
                    publishProgress(" ");
                }
                goSleep(dwnDelay);
            }
            BINclose();
            if (stopNMEA) Main.NMEAstart();
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(0, "GetLogFragment.logDownload.onPostExecute");
            appPrefEditor.putInt("DLcmd", 0);
            appPrefEditor.commit();
            SimpleDateFormat FDF = new SimpleDateFormat("yyyy.MM.dd.HHmmss", Locale.CANADA);

            if (binFileIsOpen) {
                try {
                    bOut.flush();
                    bOut.close();
                } catch (IOException e) {
                    Main.BkGrndActive = false;
                    e.printStackTrace();
                }
            }

            if (aborting) {
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                Main.BkGrndActive = false;
                return;
            }

            Date dt = new Date();
            File nn = new File(binPath, FDF.format(dt) + ".bin");
            // rename binary file to today's date and time
            binFile.renameTo(nn);
            appendMsg(getText(R.string.created) + nn.toString());
//            btnRun.setEnabled(true);
            dend = new Date();
            long diff = dend.getTime() - dstart.getTime();
            diff = diff / 1000;
            long minutes = diff / 60;
            long seconds = diff - (minutes * 60);
            long hours = minutes / 60;
            appendMsg(String.format(getString(R.string.logEnd), SDF.format(dend)));
            appendMsg(String.format("Log downlaod time %1$d hours, %2$d minutes, %3$d seconds", hours, minutes, seconds));
            Main.BkGrndActive = false;
        }//onPostExecute()

        //        @Override
        //String values expected:  Action, Style, message, percent
        protected void onProgressUpdate(String... values) {
            pct = (bRead * 100) / bMax;
            if (pct > 100) {
                pct = 100;
            }
            mLog(1, String.format("+++ onProgressUpdate +++ bRead=%1$d  bMax=%2$d  %3$d percent", bRead, bMax, pct));
            mProgress.setProgress(pct);
            tv1.setText(Integer.toString(bRead));
            if (firstProg) {
                appendMsg(String.format("%1$d log records - %2$d bytes", logRecCount, bMax));
                firstProg = false;
            }
        }//onProgressUpdate()

        private int getBytesToRead() {
            String curFunc = "GetLogFragment.logDownload.getBytesToRead()";
            mLog(1, curFunc);
            // Query the RCD_ADDR (data Log Next Write Address).
            int retry = cmdRetry;
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

        private void initProgress() {
            String curFunc = "GetLogFragment.logDownload.initProgress()";
            mLog(1, curFunc);
            mProgress.setProgress(0);   // Main Progress
            mProgress.setMax(100); // Maximum Progress
            mProgress.setSecondaryProgress(100); // Secondary Progress
            tv1.setText("0");
            tv2.setText("bytes");
        }//initProgress()

        private byte[] processBLK() {
            String curFunc = "GetLogFragment.logDownload.processBLK()";
            mLog(1, curFunc);
            int retry = cmdRetry;
//            int retry = 1;
            int delay = dwnDelay;
            while (retry > 0) {
                if (!Main.BkGrndActive) return null;
                mLog(2, String.format("%1$s command retry %2$d", curFunc, retry));
                retry--;
                cmd = String.format("PMTK182,7,%08X,%08X", offset, downBlockSize);
                parms = Main.mtkCmd(cmd, "PMTK182,8", delay);
                delay += retryInc;
                if (parms == null || parms.length < 1) continue;
                if (parms[0].contains("PMTK182")) {
                    int retAddrs = Integer.parseInt(parms[2], 16);
                    mLog(2, String.format("%1$s offset:%2$d retAddrs:%3$d", curFunc, offset, retAddrs));
                    if (retAddrs == offset) {
                        s3len = parms[3].length();
                        // string returned length needs to be twice the blkSize
                        if (s3len % 2 != 0) {
                            mLog(1, String.format("%1$s needed even byte count-received %2$d", curFunc, s3len));
                            continue;
                        }
                        if (s3len / 2 != downBlockSize) {
                            mLog(1, String.format("%1$s needed %2$d byte count-received %3$d", curFunc, downBlockSize, s3len));
                            continue;
                        }
                        //valid NMEA string has been found - break out of loop
                        retry = 0;
                        mLog(2, String.format("%1$s bin length is %2$d bytes", curFunc, s3len));
                        mLog(3, String.format("%1$s <%2$s>", curFunc, parms[3]));
                    }
                }
                if (parms[0].contains("PMTK001") && !parms[parms.length - 1].contains("3")) {
                    //PMTK182,7 failed - need to abort
                    aborting = true;
                    msg = String.format(getString(R.string.dlAbort), cmd);
                    return null;
                }
            }
            //convert returned value into binary string
            String string_byte;
            int jx = 0;
            mLog(3, String.format("%1$s s3len=%2$d blkSize=%3$d", curFunc, s3len, downBlockSize));
            byte[] binArray = new byte[downBlockSize];
            for (int ix = 0; ix < (s3len); ix += 2) {
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
            mLog(1, String.format("%1$s return size is %2$d characters", curFunc, binArray.length));
            return binArray;
        }//processBLK()

        private boolean BINopen() {
            String curFunc = "GetLogFragment.logDownload.BINopen()";
            mLog(1, curFunc);
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
            if (binFile.exists() && noDLrestart) {
                binFile.delete();
            }

            try {
                if (noDLrestart) {
                    binFile.createNewFile();
                    bOut = new FileOutputStream(binFile);
                } else {
                    bOut = new FileOutputStream(binFile, true);
                }

                return true;
            } catch (IOException e) {
                Main.buildCrashReport(e);
            }
            return false;
        }//BINopen()

        private void BINclose() {
            String curFunc = "GetLogFragment.logDownload.BINclose()";
            mLog(1, curFunc);
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
}//class GetLogFragment

