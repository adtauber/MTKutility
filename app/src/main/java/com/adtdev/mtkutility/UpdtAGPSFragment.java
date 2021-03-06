package com.adtdev.mtkutility;
/**
 * @author Alex Tauber
 * <p>
 * This file is part of the open source Android app MTKutility2. You can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 3 of the License. This extends to files included that were authored by
 * others and modified to make them suitable for this app. All files included were subject to
 * open source licensing.
 * <p>
 * MTKutility2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You can review a copy of the
 * GNU General Public License at http://www.gnu.org/licenses.
 * <p>
 * GetEPOFragment
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.adtdev.fileChooser.FileChooser;

import static android.app.Activity.RESULT_OK;

public class UpdtAGPSFragment extends Fragment {
    private static final int REQUEST_PATH = 1;
    private static final int BUFFER_SIZE = 0x1000;
    private static final int EPO60 = 60;
    private static final int EPO72 = 72;
    private static final int doLOCAL = 0;
    private SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    private TextView tv1;
    private ScrollView mSv;
    private TextView mTv;
    private ProgressBar mProgress;
    private Button btnEfile;
    private TextView epoFile;
    private Button btnUpdtEPO;
    private Button btnResetEPO;
    private final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private char[] hexChars;
    private String[] parms;
    private File txtFile;
    private boolean fileOpen = false;
    private FileWriter out;

    private boolean ok = true;
    private boolean abort = false;
    private boolean doExtract = false;
    private String epoName, epoPath, msg;
    private int epoType;
    private int epoBlk;
    private int epoSeq = 0;
    private int epoPackets;
    private int maxBytes;
    private int maxPackets;
    private int buflen;
    private byte[] epo60 = new byte[191];
    private byte[] epo72 = new byte[227];
    private byte[] epoCMD;
    private byte[] rbuf = new byte[4096];
    private byte[] extract;
    private String strAGPS;
    private FileInputStream is;

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private final int ABORT = 9;
    private boolean logFileIsOpen;
    private String NL = System.getProperty("line.separator");
    private Context mContext = Main.mContext;
    private int cmdDelay;
    private int epoDelay;
    private int cmdRetries;
    private int retryInc;
    private boolean stopNMEA;
    private boolean stopLOG;

    private byte[] epoBytes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logFileIsOpen = Main.logFileIsOpen;
        mLog(0, "UpdtAGPSFragment.onCreateView()");
        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        View rootView = inflater.inflate(R.layout.updtagps, container, false);
        tv1 = rootView.findViewById(R.id.tv1);
        mSv = rootView.findViewById(R.id.mSv);
        mTv = rootView.findViewById(R.id.mTv);
        mProgress = getActivity().findViewById(R.id.circularProgressbar);
        epoFile = rootView.findViewById(R.id.epoFile);

        btnEfile = rootView.findViewById(R.id.btnEfile);
        btnEfile.setTransformationMethod(null);
        btnEfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetLogFragment - button " + btnEfile.getText() + " pressed");
                epoName = "";
                epoPath = "";
                btnUpdtEPO.setEnabled(false);
                mTv.setText("");
                epoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/mtkutility/epo";
                Intent intent = new Intent(getActivity(), FileChooser.class);
                intent.putExtra("method", doLOCAL);
                intent.putExtra("root", "/storage");
                intent.putExtra("start", epoPath);
                intent.putExtra("nofolders", false);
                intent.putExtra("showhidden", false);
                startActivityForResult(intent, REQUEST_PATH);
            }
        });

        btnUpdtEPO = rootView.findViewById(R.id.btnUpdtEPO);
        btnUpdtEPO.setTransformationMethod(null);
        btnUpdtEPO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetLogFragment - button " + btnUpdtEPO.getText() + " pressed");
                new UpdtAGPSFragment.updateAGPS(getActivity()).execute();
            }
        });

        btnResetEPO = rootView.findViewById(R.id.btnResetEPO);
        btnResetEPO.setTransformationMethod(null);
        btnResetEPO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetLogFragment - button " + btnResetEPO.getText() + " pressed");
                new UpdtAGPSFragment.resetEPO(getActivity()).execute();
            }
        });


        return rootView;
    }//onCreateView()

    public void onViewCreated(View view, Bundle savedInstanceState) {
        btnUpdtEPO.setEnabled(false);
        buildPackets();
        String strAGPS = appPrefs.getString("strAGPS", "");
        mTv.append(strAGPS + NL);
        scrollDown();
    }//onViewCreated()

    @Override
    public void onResume() {
        super.onResume();
        cmdRetries = Integer.parseInt(publicPrefs.getString("cmdRetries", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "50"));
        epoDelay = Integer.parseInt(publicPrefs.getString("epoDelay", "200"));
        retryInc = Integer.parseInt(publicPrefs.getString("retryInc", "0"));
        stopNMEA = publicPrefs.getBoolean("stopNMEA", true);
        stopLOG = publicPrefs.getBoolean("stopLOG", false);

        String curFunc = "GetLogFragment.onResume";
        mLog(1, curFunc);
    }    //onResume()

    private void buildPackets() {
        mLog(0, "UpdtAGPSFragment.buildPackets()");
        int i;
        // build EPO binary packet type 722
        epo60 = new byte[191];
        //initialize SAT data and variable bytes
        for (i = 6; i < 189; i++) {
            epo60[i] = (byte) 0x00;
        }
        //fill static bytes
        epo60[0] = (byte) 0x04; //preamble - 2 bytes
        epo60[1] = (byte) 0x24;
        epo60[2] = (byte) 0xBF; //packet length - 2 bytes
        epo60[3] = (byte) 0x00;
        epo60[4] = (byte) 0xD2; //command ID - 2 bytes
        epo60[5] = (byte) 0x02;
        epo60[189] = (byte) 0x0D; // carriage return
        epo60[190] = (byte) 0x0A; // line feed

        // build EPO binary packet type 723
        epo72 = new byte[227];
        //initialize SAT data and variable bytes
        for (i = 6; i < 225; i++) {
            epo72[i] = (byte) 0x00;
        }
        //fill static bytes
        epo72[0] = (byte) 0x04; //preamble - 2 bytes
        epo72[1] = (byte) 0x24;
        epo72[2] = (byte) 0xE3; //packet length - 2 bytes
        epo72[3] = (byte) 0x00;
        epo72[4] = (byte) 0xD3; //command ID - 2 bytes
        epo72[5] = (byte) 0x02;
        epo72[225] = (byte) 0x0D; // carriage return
        epo72[226] = (byte) 0x0A; // line feed
        //initialize SAT data bytes
        for (i = 6; i < 225; i++) {
            epo72[i] = (byte) 0x00;
        }
    }//buildPackets()

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // determine which child activity is calling us back.
        if (requestCode == REQUEST_PATH) {
            if (resultCode == RESULT_OK) {
                epoName = data.getStringExtra("GetFileName");
                epoPath = data.getStringExtra("GetPath");
                if (checkEPOfile()) {
                    btnUpdtEPO.setEnabled(true);
                }
                epoFile.setText(epoName);
            }
        }
    }//onActivityResult()

    private boolean checkEPOfile() {
        try {
            File epoFILE = new File(epoPath);
            epoBytes = new byte[(int) epoFILE.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(epoFILE));
            dis.readFully(epoBytes);
            ok = determinetype();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ok;
    }//checkEPOfile()

    public Date dateCalc(int weeks, int secs) {
        mLog(0, "UpdtAGPSFragment.dateCalc()");
        Calendar wrkCal = new GregorianCalendar(1980, 0, 6, 0, 0, 0);
        wrkCal.add(Calendar.DATE, weeks * 7);
        wrkCal.add(Calendar.SECOND, secs);
        return wrkCal.getTime();
    }//dateCalc()

    private boolean determinetype() {
        ok = false;
        int AGPSsize = Integer.parseInt(publicPrefs.getString("AGPSsize", "7"));
        if (epoBytes[0] == epoBytes[EPO60] && epoBytes[1] == epoBytes[EPO60 + 1] && epoBytes[2] == epoBytes[EPO60 + 2]) {
            if ((epoBytes.length % 1920) == 0) {
                maxBytes = AGPSsize * 4 * 32 * EPO60;
                if (maxBytes > epoBytes.length) maxBytes = epoBytes.length;
                maxPackets = maxBytes / EPO60;
                epoType = EPO60;
                epoBlk = 187;
                epoCMD = new byte[191];
                epoCMD = epo60;
                epoPackets = epoBytes.length / EPO60;
                msg = epoBytes.length + " bytes " + getString(R.string.epo60);
                ok = true;
            } else {
                ok = false;
            }
        } else if (epoBytes[0] == epoBytes[EPO72] && epoBytes[1] == epoBytes[EPO72 + 1] && epoBytes[2] == epoBytes[EPO72 + 2]) {
            if ((epoBytes.length % 2304) == 0) {
                maxBytes = AGPSsize * 4 * 32 * EPO72;
                if (maxBytes > epoBytes.length) maxBytes = epoBytes.length;
                maxPackets = maxBytes / EPO72;
                epoType = EPO72;
                epoBlk = 223;
                epoCMD = new byte[227];
                epoCMD = epo72;
                epoPackets = epoBytes.length / EPO72;
                msg = epoBytes.length + " bytes " + getString(R.string.epo72);
                ok = true;
            } else {
                ok = false;
            }
        }
        if (ok) {
            mTv.append(msg + " = " + epoPackets + " SETs\n");
            mTv.append("processing " + maxPackets + " SETs\n");
            scrollDown();
        } else {
            mTv.setText(getString(R.string.badEPO) + "\n");
            scrollDown();
            Toast.makeText(getActivity(), getString(R.string.badEPO), Toast.LENGTH_LONG).show();
        }
        return ok;
    }//determinetype()

    private void getEPOsetting(int iff) {
        //update AGPS info
        ok = false;
        int rpt = 5;
        while (rpt > 0) {
            parms = Main.mtkCmd("PMTK607", "PMTK707", cmdDelay);
            rpt--;
            if (parms == null) {
                goSleep(250);
                continue;
            }
            switch (iff) {
                case 0:
                    if (Integer.valueOf(parms[1]) != 0) rpt = 0;
                    break;
                case 1:
                    if (Integer.valueOf(parms[1]) == 0) rpt = 0;
                    break;
            }
        }
        strAGPS = parms[1] + " EPO sets";
        if (Integer.valueOf(parms[1]) > 0) {
            Date dd = dateCalc(Integer.valueOf(parms[4]), Integer.valueOf(parms[5]));
            strAGPS = strAGPS + " expires " + SDF.format(dd);
        }
        appPrefEditor.putString("strAGPS", strAGPS).commit();
    }//getEPOsetting()

    public void goSleep(int mSec) {
        mLog(3, String.format("UpdtAGPSFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(Log.getStackTraceString(e));
        }
    }//goSleep()

    private void mLog(int mode, String msg) {
        if (logFileIsOpen) {
            Main.mLog(mode, msg);
        }
    }//Log()

    private void scrollDown() {
        final ScrollView scrollView = getActivity().findViewById(R.id.mSv);
        scrollView.post(new Runnable() {
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private class resetEPO extends AsyncTask<Void, String, Void> {
        private Context mContext;
        ProgressDialog dialog;

        public resetEPO(Context context) {
            mLog(0, "UpdtAGPSFragment.resetEPO.resetEPO()");
            mContext = context;
        }//updateAGPS()

        @Override
        protected void onPreExecute() {
            mLog(0, "UpdtAGPSFragment.resetEPO.onPreExecute()");
            dialog = new ProgressDialog(mContext);
            dialog.setMessage(getString(com.adtdev.fileChooser.R.string.working));
            dialog.show();
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(0, "UpdtAGPSFragment.resetEPO.doInBackground()");
            //delete the MTK logger EPO data
            int retry = cmdRetries;
            while (retry > 0) {
                Main.mtkCmd("PMTK127", "PMTK001,127", epoDelay * 2);
                retry--;
                if (parms == null) continue;
                if (parms[0].contains("PMTK001")) {
                    if (parms[1].contains("127") && !parms[2].contains("3")) continue;
                } else {
                    continue;
                }
            }
            getEPOsetting(1);
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(0, "UpdtAGPSFragment.resetEPO.onPostExecute()");
            btnEfile.setEnabled(true);
            btnUpdtEPO.setEnabled(false);
            btnResetEPO.setEnabled(true);
            mTv.append(strAGPS + NL);
            scrollDown();
            if (dialog.isShowing()) dialog.dismiss();
        }//onPostExecute()
    }//class resetEPO

    private class updateAGPS extends AsyncTask<Void, String, Void> {
        //NOTE: this module incldues the option of creating a seperate debug file for the binary traffic
        //Set the next variable true to turn this on
        private boolean doBINdebug = false;
        private final byte[] binPMTK253 = new byte[]{(byte) 0x04, 0x24, 0x0E, 0x00, (byte) 0xFD, 0x00, 0x00, 0x00, (byte) 0xC2, 0x01, 0x00, 0x30, 0x0D, 0x0A};

        private ProgressDialog dialog;
        ProgressBar mProgress = getActivity().findViewById(R.id.circularProgressbar);
        TextView tv1 = getActivity().findViewById(R.id.tv1);
        TextView tv2 = getActivity().findViewById(R.id.tv2);

        final int blkSize = 0x0800;
        final int SATstart = 8;
        private Context mContext;
        private String msg;
        private int epoRead = 0;
        private int CMDix = SATstart;
        private int BUFix = 0;
        private int pct;
        private int bMax = 100;
        private int rereads = 10;
        private String[] parms;
        private Date dstart;
        private Date dend;

        public updateAGPS(Context context) {
            mLog(0, "UpdtAGPSFragment.updateAGPS.updateAGPS()");
            mContext = context;
            dialog = new ProgressDialog(mContext);
        }//updateAGPS()

        @Override
        protected void onPreExecute() {
            mLog(0, "UpdtAGPSFragment.updateAGPS.onPreExecute()");
            //disable navigation drawer to prevent interrupts
            ((DrawerLocker) getActivity()).setDrawerEnabled(false);
            initProgress();
            btnEfile.setEnabled(false);
            btnUpdtEPO.setEnabled(false);
            btnResetEPO.setEnabled(false);
            dstart = new Date();
            mTv.append("AGPS update started " + SDF.format(dstart) + NL);
            scrollDown();
            this.dialog.setMessage(getString(R.string.intializing));
            this.dialog.show();
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            String curFunc = "UpdtAGPSFragment.updateAGPS.doInBackground()";
            mLog(0, curFunc);
            //set appFailed at start of doInBackground - clear at end so that process hancing will get reported
            appPrefEditor.putBoolean("appFailed",true).commit();
            //delete the MTK logger EPO data
//            int retry = cmdRetries;
//            while (retry > 0) {
//                Main.mtkCmd("PMTK127", "PMTK001,127", epoDelay * 2);
//                retry--;
//                if (parms == null) continue;
//                if (parms[0].contains("PMTK001")) {
//                    if (parms[1].contains("127") && !parms[2].contains("3")) continue;
//                } else {
//                    continue;
//                }
//            }

            //Switch the protocol to BINARY mode
            mLog(0, "***************** switching to binary mode *****************");
            Main.sendCommand("PMTK253,1,0");
            //wait for command to take effect
            goSleep(5000);

            if (doBINdebug) openEPOdebugFile();
            mLog(1, String.format("+++ EPO blocks start **** %1$d EPO%2$d packets", epoPackets, epoType));
            abort = false;
            while (BUFix < maxBytes) {
                mLog(1, String.format("+++ EPO blocks loop **** BUFix=%1$d of %2$d ***", BUFix, maxBytes));
                for (int lx = 0; lx < epoType; lx++) {
                    epoCMD[CMDix] = epoBytes[BUFix];
                    CMDix++;
                    BUFix++;
                }
                mLog(1, String.format("+++ EPO blocks loop **** epoRead=%1$d of %2$d ***", epoRead, maxPackets));
                epoRead++;
                if (CMDix > epoBlk) {
                    if (dialog.isShowing()) dialog.dismiss();
                    // 3 blocks of EPO have been transferred - time to send binary command
                    // set binary command sequence number
                    ok = sendEPOcmd();
                    if (!ok) {
                        Main.sendBytes(binPMTK253);
                        goSleep(5000);
                        abort = true;
                        msg = getString(R.string.binAbort);
                        mLog(0, "+++ EPO blocks loop **** " + msg);
                        break;
                    }
                    CMDix = SATstart;
                    epoSeq++;
                }
            }
            if (abort == false) {
                //check SETs trnasferred - send last record if less than 3
                if (CMDix < epoBlk) {
                    mLog(1, String.format("+++ EPO blocks loop **** last SETS-CMDix=%1$d epoBlk=%2$d ***", CMDix, epoBlk));
                    ok = sendEPOcmd();
                }
                //send end of records command
                for (int i = SATstart; i < epoBlk; i++) {
                    epoCMD[i] = 0x00;
                }
                epoCMD[6] = (byte) 0xFF;
                epoCMD[7] = (byte) 0xFF;
                epoCMD[epoBlk + 1] = (byte) 0x00;
                //set packet checksum - exclusive OR of bytes between the preamble and checksum
                for (int i = 2; i < epoBlk + 1; i++) {
                    epoCMD[epoBlk + 1] ^= epoCMD[i];
                }
                mLog(1, String.format("+++ EPO blocks loop **** sending end packet ****"));
                Main.sendBytes(epoCMD);
                goSleep(3000);
            }

            //end biuanary mode
            mLog(0, "***************** switching to normal mode *****************");
            //send the binary command to switch to NMEA mode
            Main.sendBytes(binPMTK253);
            goSleep(1000);
            //follow up with the MTK command to wait for switch to NMEA to complete
            int retry = cmdRetries;
            while (retry > 0) {
                mLog(1, String.format("***************** waiting for NMEA mode try %1$d", retry));
                Main.mtkCmd("PMTK253,0,0", "PMTK001,253", epoDelay * 2);
                retry--;
                if (parms == null) continue;
                if (parms[0].contains("PMTK001")) {
                    if (parms[1].contains("253") && !parms[2].contains("3")) continue;
                } else {
                    continue;
                }
            }
            //update AGPS info
            getEPOsetting(0);
            appPrefEditor.putBoolean("appFailed",false).commit();
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(0, "UpdtAGPSFragment.updateAGPS.onPostExecute()");
            ((DrawerLocker) getActivity()).setDrawerEnabled(true);
            if (dialog.isShowing()) dialog.dismiss();
            if (abort) {
                mTv.append(msg + "\n");
                scrollDown();
                Toast.makeText(mContext,msg,Toast.LENGTH_LONG).show();
            }

            btnEfile.setEnabled(true);
            btnUpdtEPO.setEnabled(false);
            btnResetEPO.setEnabled(true);
            dend = new Date();
            long diff = dend.getTime() - dstart.getTime();
            diff = diff / 1000;
            long minutes = diff / 60;
            long seconds = diff - (minutes * 60);
            long hours = minutes / 60;
            mTv.append(String.format(getString(R.string.epoDone), SDF.format(dend)) + NL);
            mTv.append(String.format("AGPS update time %1$d hours, %2$d minutes, %3$d seconds", hours, minutes, seconds) + NL);
            mTv.append(strAGPS + NL);
            scrollDown();
        }//onPostExecute()

        @Override
        protected void onProgressUpdate(String... values) {
            pct = (BUFix * 100) / maxBytes;
            if (pct >= 100) {
                pct = 100;
                this.dialog.setMessage(getString(R.string.getSetngs));
                this.dialog.show();
            }
            mProgress.setProgress(pct);
            tv1.setText(Integer.toString(epoRead));
        }//onProgressUpdate()

        private void initProgress() {
            mLog(0, "UpdtAGPSFragment.updateAGPS.initProgress()");
            mProgress.setProgress(0);   // Main Progress
            mProgress.setMax(100); // Maximum Progress
            mProgress.setSecondaryProgress(100); // Secondary Progress
            tv1.setText("0");
            tv2.setText("SETs");
        }//initProgress()


        private boolean sendEPOcmd() {
            mLog(1, "UpdtAGPSFragment.updateAGPS.sendEPOcmd()");
            boolean isok;
            epoCMD[6] = (byte) (epoSeq & 0xFF);
            epoCMD[7] = (byte) ((epoSeq >> 8) & 0xFF);
            epoCMD[epoBlk + 1] = (byte) 0x00;
            //set packet checksum - exclusive OR of bytes between the preamble and checksum
            for (int i = 2; i < epoBlk + 1; i++) {
                epoCMD[epoBlk + 1] ^= epoCMD[i];
            }
            isok = false;
            Main.sendBytes(epoCMD);
            if (doBINdebug) writeHEX(">", epoCMD);
            rereads = 50;
            int delay = epoDelay;
            while (rereads > 0) {
                rbuf = readBytes(delay);
                delay += retryInc;
                if (rbuf == null || rbuf.length == 0) {
                    abort = true;
                    msg = "ABORTING - no binary reply received ***";
                    mLog(0, "+++ EPO blocks loop **** rbuf is null");
                    return false;
                }
                mLog(1, String.format("+++ EPO blocks loop **** rereads=%1$d  rbuf.length=%2$d", rereads, rbuf.length));
                if (doBINdebug) writeHEX(Integer.toString(rbuf.length) + "<", rbuf);
                for (int j = 0; j < rbuf.length; j++) {
                    // Check if this is the start of a new message
                    if ((!doExtract) && (rbuf[j] == 0x04)) {
                        mLog(1, "+++ EPO blocks loop **** doExtract=true");
                        doExtract = true;
                        buflen = 0;
                        extract = new byte[12];
                    }
                    if (doExtract) {
                        extract[buflen] = rbuf[j];
                        if ((buflen == 1) && (extract[buflen] != 0x24)) {
                            mLog(1, "+++ EPO blocks loop **** doExtract=false");
                            doExtract = false;
                        }
                        buflen++;
                        if (buflen > 11) {
                            mLog(1, String.format("+++ EPO blocks loop **** buflen>11, doExtract=false, rereads=%1$d", rereads));
                            doExtract = false;
                            mLog(1, bytesToHex(extract));
                            if (doBINdebug) writeHEX("<<", extract);
                            if (extract[6] == epoCMD[6] && extract[7] == epoCMD[7]) { //received reply for EPO send
                                if (extract[8] == 0x01) {
                                    mLog(1, "+++ EPO blocks loop **** record accepted");
                                    isok = true;
                                    publishProgress(" ");
                                    rereads = 0;
                                    //clear EPO sets for next loop to have correctly filled record when less than 3 sets left
                                    for (int i = SATstart; i < epoBlk; i++) {
                                        epoCMD[i] = 0x00;
                                    }
                                }
                            }
                        } else {
                            continue;
                        }
                    }
                }
                rereads--;
            }
            return isok;
        }//sendEPOcmd()

        private void openEPOdebugFile() {
            mLog(0, "UpdtAGPSFragment.updateAGPS.openEPOdebugFile()");
            txtFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mtkutility/epo.txt");
            if (!txtFile.exists()) {
                try {
                    txtFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                out = new FileWriter(txtFile);
                fileOpen = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }//openEPOdebugFile()


        public byte[] readBytes(int timeout) {
            int bytes_available = 0;
            int retry = timeout / 10;
            byte[] buf = null;

            while (bytes_available == 0 && retry > 0) {
                mLog(1, String.format("+++ readBytes retry:%1$d  delay:%2$d", retry, timeout));
                try {
                    bytes_available = Main.GPSin.available();
                } catch (IOException e) {
                    Main.buildCrashReport(Log.getStackTraceString(e));
                }
                goSleep(timeout);
                retry--;
            }
            if (bytes_available > 0) {
                buf = new byte[bytes_available];
                try {
                    Main.GPSin.read(buf);
                } catch (IOException e) {
                    Main.buildCrashReport(Log.getStackTraceString(e));
                }
            }
            mLog(1, String.format("+++ readBytes retry:%1$d  available:%2$d", retry, bytes_available));
            return buf;
        }//readBytes(timeout)

        private void writeHEX(String io, byte[] bytes) {
            String txt = io + bytesToHex(bytes) + ">\n";
            if (fileOpen) {
                try {
                    out.append(txt);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }//writeHEX()

        private String bytesToHex(byte[] bytes) {
            hexChars = new char[(bytes.length * 2)];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }//bytesToHex()
    }//class updateAGPS
}//class UpdtAGPSFragment

