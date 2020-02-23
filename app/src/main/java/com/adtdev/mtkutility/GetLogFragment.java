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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.widget.Toast;

public class GetLogFragment extends Fragment {
    private static final int SIZEOF_SECTOR = 0x10000;
    private String[] parms;

    private View rootView;
    public TextView tv1;
    private ScrollView mSv;
    private TextView mTv;
    public ProgressBar mProgress;
    private Button btnRun;
    private Button btnReset;
    private boolean ok = true;
    private long DLstart;
    private boolean stopBKGRND = false;
    private final SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.ROOT);
    private final SimpleDateFormat fnFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.ROOT);
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private Context mContext = Main.mContext;
    private int downBlockSize;
    private int cmdDelay;
    private int dwnDelay;
    private int cmdRetry = 0;
    private final int ABORT = 9;
    private String NL = System.getProperty("line.separator");
    private String binPathName;
    private String fileNamePrefix;
    private boolean logFileIsOpen;
    private int logRecCount;
    private int DLcount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logFileIsOpen = Main.logFileIsOpen;
        mLog(0, "GetLogFragment.onCreateView");

        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        rootView = inflater.inflate(R.layout.getlog, container, false);
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
                mTv.setTextColor(Color.BLACK);
                mTv.setText("");
                DLcount = appPrefs.getInt("DLcmd", 0);
                if (DLcount > 0) {
                    btnReset.setEnabled(true);
                    appendMsg(String.format(getString(R.string.logRS), DLcount));
                    new connect(getActivity()).execute();
                } else {
                    DLstart = new Date().getTime();
                    appPrefEditor.putLong("DLstart", DLstart).commit();
                    new logDownload(getActivity()).execute();
                }
            }
        });


        btnReset = rootView.findViewById(R.id.reset);
        btnReset.setTransformationMethod(null);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetLogFragment - button " + btnReset.getText() + " pressed +++++");
                stopBKGRND = true;
                appPrefEditor.putInt("DLcmd", 0).commit();
                appendMsg(String.format(getString(R.string.logRS), 0));
                btnReset.setEnabled(false);
                btnRun.setEnabled(false);
            }
        });

        return rootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        mLog(0, "GetLogFragment.onViewCreated");
        btnReset.setEnabled(false);
        logRecCount = appPrefs.getInt("logRecCount", 0);
        appendMsg(String.format(getString(R.string.logrecs), logRecCount));
        DLcount = appPrefs.getInt("DLcmd", 0);
        if (DLcount > 0) {
            btnReset.setEnabled(true);
            appendMsg(String.format(getString(R.string.logRS), DLcount));
            new connect(getActivity()).execute();
        }
    } //onViewCreated()

    public void onPause() {
        super.onPause();
        String curFunc = "GetLogFragment.onPause";
        mLog(1, curFunc);
    } //onPause()


    @Override
    public void onResume() {
        super.onResume();
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "50"));
        dwnDelay = Integer.parseInt(publicPrefs.getString("dwnDelay", "150"));
        downBlockSize = Integer.parseInt(publicPrefs.getString("downBlockSize", "2048"));
        String curFunc = "GetLogFragment.onResume";
        mLog(1, curFunc);
    } //onResume()

    private void appendMsg(String msg) {
        mTv.append(msg + NL);
        scrollDown();
    } //appendMsg()

    private void scrollDown() {
        final ScrollView scrollView = getActivity().findViewById(R.id.mSv);
        scrollView.post(new Runnable() {
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    } //scrollDown()

    private void goSleep(int mSec) {
        mLog(3, String.format("GetLogFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(Log.getStackTraceString(e));
        }
    } //goSleep()

    private void mLog(int mode, String msg) {
        if (logFileIsOpen) {
            Main.mLog(mode, msg);
        }
    } //Log()

    private String bytesToHex(byte[] bytes) {
        mLog(3, "GetLogFragment.bytesToHex()");
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    } //bytesToHex()

    public class connect extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = new ProgressDialog(mContext);
        NavigationView navigationView;
        Menu nav_Menu;

        public connect(Context context) {
            mLog(0, "GetLogFragment.connect.connect");
            mContext = context;
        } //connect()

        @Override
        protected void onPreExecute() {
            mLog(1, "GetLogFragment.connect.onPreExecute");
            this.dialog.setMessage(getString(R.string.connecting));
            this.dialog.show();
        } //onPreExecute()

        @Override
        protected Void doInBackground(Void... voids) {
            mLog(1, "GetLogFragment.connect.doInBackground");
            goSleep(5000);
            boolean ok = Main.connect();
            if (!ok) return null;
            if (Main.GPSsocket.isConnected()) {
                mLog(0, "GetLogFragment.connect.doInBackground() Connected *****");
            }
            return null;
        } //doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(1, "GetLogFragment.connect.onPostExecute");
            if (dialog.isShowing()) dialog.dismiss();
            if (Main.aborting) {
                Toast.makeText(mContext, Main.errMsg, Toast.LENGTH_LONG).show();
                return;
            }
            if (Main.GPSsocket.isConnected()) {
                new logDownload(getActivity()).execute();
            } else {
                Toast.makeText(mContext, getText(R.string.DLconnectFail), Toast.LENGTH_LONG).show();
                mTv.setTextColor(Color.RED);
//                appendMsg(NL + getString(R.string.DLconnectFail));
                mTv.setText(NL + getString(R.string.DLconnectFail));
//                scrollDown();
            }
        } //onPostExecute()
    } //class connect

    public class disconnect extends AsyncTask<Void, Void, Void> {
        NavigationView navigationView;
        Menu nav_Menu;

        @Override
        protected Void doInBackground(Void... voids) {
            mLog(1, "GetLogFragment.disconnect.doInBackground");
            Main.disconnect();
            return null;
        }

        protected void onPostExecute(Void param) {
            mLog(1, "GetLogFragment.disconnect.onPostExecute");
            navigationView = getActivity().findViewById(R.id.nav_view);
            nav_Menu = navigationView.getMenu();
            nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
            nav_Menu.findItem(R.id.nav_clrLog).setVisible(false);
            nav_Menu.findItem(R.id.nav_UpdtAGPS).setVisible(false);
            nav_Menu.findItem(R.id.nav_Settings).setVisible(false);
        }
    } // class disconnect

    private class logDownload extends AsyncTask<Void, String, Void> {

        ProgressBar mProgress = getActivity().findViewById(R.id.circularProgressbar);
        TextView tv1 = getActivity().findViewById(R.id.tv1);
        TextView tv2 = getActivity().findViewById(R.id.tv2);
        private String file_time_stamp;
        private boolean aborting = false;
        private boolean OK = true;
        private boolean firstProg = true;
        private int offset = 0;
        private boolean reSumed = false;
        private String cmd;
        private int s3len;
        private int pct;
        private int bRead = 0;
        private long dend;
        private File binPath;
        private File binFile;

        private boolean noDLrestart = false;
        private FileOutputStream bOut;
        private boolean binFileIsOpen = false;
        private int bMax = 0;
        private String msg;

        public logDownload(Context context) {
            mLog(1, "GetLogFragment.logDownload.logDownload");
            mContext = context;
        } //logDownload()

        @Override
        protected void onPreExecute() {
            String curFunc = "GetLogFragment.logDownload.doInBackground";
            mLog(1, curFunc);
            //disable navigation drawer to prevent interrupts
            ((DrawerLocker) getActivity()).setDrawerEnabled(false);
            initProgress();
            btnRun.setEnabled(false);
            DLstart = appPrefs.getLong("DLstart", 0);
            appendMsg(String.format(getString(R.string.logBgn), SDF.format(DLstart)));
            binPath = new File(appPrefs.getString("binPath", ""));
//            binPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), binPathName);
            offset = appPrefs.getInt("DLcmd", 0);
            if (offset > 0) reSumed = true;
            //turn off command retry
            Main.doRetry = false;
        } //onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            String curFunc = "GetLogFragment.logDownload.doInBackground";
            mLog(1, curFunc);
            //set appFailed at start of doInBackground - clear at end so that process hanging will get reported
            appPrefEditor.putBoolean("appFailed", true).commit();
            byte[] binBytes = null;

            //create temp output
            binFileIsOpen = BINopen();
            if (!binFileIsOpen) {
                aborting = true;
                msg = String.format(getString(R.string.dlbAbort), binFile);
                return null;
            }

            bMax = getBytesToRead();
            if (aborting) return null;

            bRead = offset;
            while (bRead < bMax) {
                mLog(1, String.format("%1$s offset=%2$d bRead=%3$d bMax=%4$d ", curFunc, offset, bRead, bMax));
                binBytes = processBLK();
                mLog(3, String.format("%1$s received %2$d bytes", curFunc, binBytes.length));
                if (binBytes.length > 0) {
                    try {
                        bOut.write(binBytes);
                        mLog(1, String.format("%1$s wrote %2$d bytes to file", curFunc, binBytes.length));
                        offset += binBytes.length;
                        bRead += binBytes.length;
                        appPrefEditor.putInt("DLcmd", offset).commit();
                    } catch (IOException e) {
                        Main.buildCrashReport(Log.getStackTraceString(e));
                        return null;
                    }
                    publishProgress(" ");
                }
//                goSleep(dwnDelay);
                if (stopBKGRND) return null;
            }
            BINclose();
            appPrefEditor.putBoolean("appFailed", false).commit();
            fileNamePrefix = getFileName();
            return null;
        } //doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(0, "GetLogFragment.logDownload.onPostExecute");
            ((DrawerLocker) getActivity()).setDrawerEnabled(true);
            //turn on command retry again
            Main.doRetry = true;
            appPrefEditor.putInt("DLcmd", 0).commit();
            SimpleDateFormat FDF = new SimpleDateFormat("yyyy.MM.dd.HHmmss", Locale.ROOT);

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
//            File nn = new File(binPath, FDF.format(dt) + ".bin");
            File nn = new File(binPath, fileNamePrefix + ".bin");
            // rename binary file to today's date and time
            binFile.renameTo(nn);
            appendMsg(getText(R.string.created) + nn.toString());
            DLstart = appPrefs.getLong("DLstart", 0);
            dend = new Date().getTime();
            long diff = dend - DLstart;
            diff = diff / 1000;
            long minutes = diff / 60;
            long seconds = diff - (minutes * 60);
            long hours = minutes / 60;
            appendMsg(String.format(getString(R.string.logEnd), SDF.format(dend)));
            appendMsg(String.format("Log downlaod time %1$d hours, %2$d minutes, %3$d seconds", hours, minutes, seconds));
            if (reSumed) {
                new disconnect().execute();
            }
        } //onPostExecute()

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
        } //onProgressUpdate()

        private java.util.Date add1024toDate(java.util.Date oldDate) {   //dyj
            mLog(3, "MakeGPXFragment.add1024toDate()");
            Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar newDay = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            newDay.setTime(oldDate);
            newDay.add(Calendar.DATE, 7168); // add 7168 days
            if (newDay.after(today))
                return oldDate;
            else
                return newDay.getTime();
        } //add1024toDate()

        private int getBytesToRead() {
            String curFunc = "GetLogFragment.logDownload.getBytesToRead";
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
//                    continue;
                }
            }
            aborting = true;
            msg = String.format(getString(R.string.dlAbort), "PMTK182,2,8");
            return 0;
        } //getBytesToRead()

        private String getFileName() {
            byte[] buffer = new byte[SIZEOF_SECTOR];
            FileInputStream input;
            int read = 0;
            try {
                input = new FileInputStream(binFile);
                BufferedInputStream mReader = new BufferedInputStream(input, SIZEOF_SECTOR);
                read = mReader.read(buffer, 0, SIZEOF_SECTOR);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (read < 1) return null;

            ByteBuffer buf = ByteBuffer.wrap(buffer);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            boolean looping = true;
            String s;
            buf.position(0x200);
            int separator_length = 0x10;
            byte[] tmp = new byte[0x10];
            long utcTime = 0;

            while (looping) {
                // Test for record separators
                buf.get(tmp);
                s = bytesToHex(tmp);
                mLog(3, s);
                //check for a record separator - format:
                // AA AA AA AA AA AA AA 00 00 00 00 00 BB BB BB BB
                // START/STOP LOG = 0x07;
                if (tmp[0] == (byte) 0xAA && tmp[1] == (byte) 0xAA &&
                        tmp[2] == (byte) 0xAA && tmp[3] == (byte) 0xAA &&
                        tmp[12] == (byte) 0xBB && tmp[13] == (byte) 0xBB &&
                        tmp[14] == (byte) 0xBB && tmp[15] == (byte) 0xBB) {
                    // So we found a record separator..
                    continue;
                } else {
                    buf.position(buf.position() - separator_length);
                    utcTime = buf.getInt();
                    looping = false;
                }
            }
            Date mDate = new java.util.Date(utcTime * 1000);
            fnFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return fnFormatter.format(add1024toDate(mDate));
        } //getFileName()

        private void initProgress() {
            String curFunc = "GetLogFragment.logDownload.initProgress";
            mLog(1, curFunc);
            mProgress.setProgress(0);   // Main Progress
            mProgress.setMax(100); // Maximum Progress
            mProgress.setSecondaryProgress(100); // Secondary Progress
            tv1.setText("0");
            tv2.setText("bytes");
        } //initProgress()

        private byte[] processBLK() {
            String curFunc = "GetLogFragment.logDownload.processBLK";
            mLog(1, curFunc);
            int retry = cmdRetry;
            int delay = dwnDelay;
            cmd = String.format("PMTK182,7,%08X,%08X", offset, downBlockSize);
            parms = Main.mtkCmd(cmd, "PMTK182,8", delay);
            if (parms == null) {
                mLog(ABORT, String.format(getString(R.string.dlAbort), cmd));
            } else {
                if (parms[0].contains("PMTK182")) {
                    int retAddrs = Integer.parseInt(parms[2], 16);
                    mLog(3, String.format("%1$s offset:%2$d retAddrs:%3$d", curFunc, offset, retAddrs));
                    if (retAddrs == offset) {
                        s3len = parms[3].length();
                        // string returned length needs to be twice the blkSize
                        if (s3len % 2 != 0) {
                            mLog(1, String.format("%1$s needed even byte count-received %2$d", curFunc, s3len));
                            mLog(ABORT, String.format(getString(R.string.dlAbort), cmd));
                        }
                        if (s3len / 2 != downBlockSize) {
                            mLog(1, String.format("%1$s needed %2$d byte count-received %3$d", curFunc, downBlockSize, s3len));
                            mLog(ABORT, String.format(getString(R.string.dlAbort), cmd));
                        }
                        mLog(3, String.format("%1$s bin length is %2$d bytes", curFunc, s3len));
                        mLog(3, String.format("%1$s <%2$s>", curFunc, parms[3]));
                    }
                } else if (parms[0].contains("PMTK001")) {
                    //PMTK182,7 failed - need to abort
                    mLog(1, String.format("%1$s <%2$s>", curFunc, TextUtils.join(",", parms)));
                    mLog(ABORT, String.format(getString(R.string.dlAbort), cmd));
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
            mLog(2, String.format("%1$s return size is %2$d characters", curFunc, binArray.length));
            return binArray;
        } //processBLK()

        private boolean BINopen() {
            String curFunc = "GetLogFragment.logDownload.BINopen";
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
                Main.buildCrashReport(Log.getStackTraceString(e));
            }
            return false;
        } //BINopen()

        private void BINclose() {
            String curFunc = "GetLogFragment.logDownload.BINclose";
            mLog(1, curFunc);
            if (binFileIsOpen) {
                try {
                    bOut.flush();
                    bOut.close();
                } catch (IOException e) {
                    Main.buildCrashReport(Log.getStackTraceString(e));
                }
            }
            binFileIsOpen = false;
        } //BINclose()
    } //class logDownload
} //class GetLogFragment

