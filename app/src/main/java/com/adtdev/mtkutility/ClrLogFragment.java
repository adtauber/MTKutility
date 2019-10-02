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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.Toast;

public class ClrLogFragment extends Fragment {

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
    private boolean logFileIsOpen = Main.logFileIsOpen;
    private int logRecCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log(0, "ClrLogFragment.onCreateView()");

        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        rootView = inflater.inflate(R.layout.clrlog, container, false);
//        LogTxt = rootView.findViewById(R.id.LogTxt);
        tv1 = rootView.findViewById(R.id.tv1);
        mSv = rootView.findViewById(R.id.mSv);
        mTv = rootView.findViewById(R.id.mTv);

        btnRun = rootView.findViewById(R.id.run);
        btnRun.setTransformationMethod(null);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(0, "ClrLogFragment - button " + btnRun.getText() + " pressed");
                new eraseLog(getActivity()).execute();
            }
        });

        return rootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log(0, "ClrLogFragment.onViewCreated()");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugPref", "0"));
        new getRecCount(getActivity()).execute();
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
        Log(3, String.format("ClrLogFragment.goSleep(%d)", mSec));
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
        time = time.replace("AM","");
        time = time.replace("PM","");
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
            Log(0, "ClrLogFragment.getRecCount.getRecCount()");
            mContext = context;
        }//getRecCount()

        protected void onPreExecute() {
            Log(0, "ClrLogFragment.getRecCount.onPreExecute()");
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
                }else {
                    ix--;
                    continue;
                }
            };
            return null;
        }//doInBackground()

        protected void onPostExecute(Void param) {
            Log(0, "ClrLogFragment.getRecCount.onPostExecute()");
            if (Main.stopBkGrnd) return;
            if (dialog.isShowing()) dialog.dismiss();
            myHandler.sendEmptyMessage(0);
            Main.stopBkGrnd = true;
        }//onPostExecute()
    }//class getRecCount

    private class eraseLog extends AsyncTask<Void, String, Void> {
        private Context mContext;
        private ProgressDialog dialog;
        private SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.CANADA);
        private String msg;
        private String[] parms;
        private String mode;
        private boolean OK;
        private int ix;
        private int downDelay;
        private boolean aborting = false;
        private Date dstart;

        public eraseLog(Context context) {
            Log(0, "ClrLogFragment.eraseLog.eraseLog()");
            mContext = context;
        }//eraseLog()

        @Override
        protected void onPreExecute() {
            Log(0, "ClrLogFragment.eraseLog.onPreExecute()");
            Main.stopBkGrnd = false;
            downDelay = Integer.parseInt(publicPrefs.getString("downDelay", "50"));
            dstart = new Date();
            appendMsg(String.format(getString(R.string.clrBgn), SDF.format(dstart)));
            dialog = new ProgressDialog(mContext);
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
            Main.stopBkGrnd = false;
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            String curFunc = "ClrLogFragment.eraseLog.doInBackground()";
            Log(0, curFunc);
            //format log using PMTK182,6,1
            parms = Main.mtkCmd("PMTK182,6,1", "PMTK001,182,6", downDelay * 100);
            ix = 10;
            do {
                Log(1, String.format("%1$s waiting for logger ready (PMTK182,3,1,1) retry %2$d", curFunc, ix));
                ix--;
                parms = Main.mtkCmd("PMTK182,2,1", "PMTK182,3,1", downDelay * 10);
                if (parms != null && parms[3].contains("1")) {
                    ix = 0;
                }
            } while (ix > 0);

            ix = 10;
            do {
                Log(1, String.format("%1$s getting log record count (PMTK182,2,10) retry %2$d", curFunc, ix));
                parms = Main.mtkCmd("PMTK182,2,10", "PMTK182,3,10", downDelay * 40);
                if (parms != null && parms[0].contains("PMTK182") && parms[1].contains("3")) {
                    ix = 0;
                    logRecCount = Integer.parseInt(parms[3], 16);
                    Main.LogTxt = logRecCount + " log records";
                    mode = "W";
                    msg = mContext.getString(R.string.erased);
                    publishProgress();
                    goSleep(250);
                    msg = String.format("GPS has %d records", logRecCount);
                    publishProgress();
                    Log(0, String.format("%1$s Log has %2$d records ******", curFunc, logRecCount));
                }
                ix--;
            } while (ix > 0);
            return null;
        }//doInBackground()

        @Override
        protected void onProgressUpdate(String... values) {
            Log(0, "ClrLogFragment.eraseLog.onProgressUpdate()");
            if (mode == "L") Log(0, msg);
            if (mode == "W") appendMsg(msg);
        }

        @Override
        protected void onPostExecute(Void param) {
            Log(0, "ClrLogFragment.eraseLog.onPostExecute()");
            if (Main.stopBkGrnd) return;
//            if (aborting) {
//                appendMsg(Main.errMsg);
//                Toast.makeText(mContext, Main.errMsg, Toast.LENGTH_LONG).show();
//            }
            Date dend = new Date();
            long diff = dend.getTime() - dstart.getTime();
            diff = diff / 1000;
            long minutes = diff / 60;
            long seconds = diff - (minutes * 60);
            long hours = minutes / 60;
            appendMsg(String.format(getString(R.string.clrEnd), SDF.format(dend)));
            appendMsg(String.format("Log downlaod time %1$d hours, %2$d minutes, %3$d seconds", hours, minutes, seconds));
            btnRun.setEnabled(false);
            if (dialog.isShowing()) dialog.dismiss();
            Main.stopBkGrnd = true;
        }//onPostExecute()
    }//class eraseLog

    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(@org.jetbrains.annotations.NotNull Message msg) {
            switch (msg.what) {
                case 0:
                    if (logRecCount < 1) {
                        appendMsg(getString(R.string.noLogClr));
                        Toast.makeText(mContext, getString(R.string.noLogClr), Toast.LENGTH_LONG).show();
                    }else {
                        appendMsg(String.format(getString(R.string.GPSrecs), logRecCount));
                        btnRun.setEnabled(true);
                    }
                    break;
                default:
                    break;
            }
        }
    };//Handler myHandler
}//class ClrLogFragment
