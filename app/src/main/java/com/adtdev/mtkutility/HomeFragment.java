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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class HomeFragment extends Fragment implements getGPSid.GPSdialogListener {

    private String NL = System.getProperty("line.separator");
    SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.CANADA);
    private final int REQUEST_ENABLE_BT = 88;
    private StringBuilder mText = new StringBuilder();
    private final int TEXT_MAX_SIZE = 5120;
    final byte[] binPMTK253 = new byte[]{(byte) 0x04, 0x24, 0x0E, 0x00, (byte) 0xFD, 0x00, 0x00, 0x00, (byte) 0xC2, 0x01, 0x00, 0x30, 0x0D, 0x0A};
    final String asterisks = "*******************************************";

    //layout inflater values
    private View mV;

    private TextView GPstats;
    private ScrollView mSvMsg;
    private TextView msgFrame;
    private TextView txtGGA;
    private TextView txtGLL;
    private TextView txtGSA;
    private TextView txtGSV;
    private TextView txtRMC;
    private TextView txtVTG;
    private TextView txtZDA;
    private Spinner GGA;
    private Spinner GLL;
    private Spinner GSA;
    private Spinner GSV;
    private Spinner RMC;
    private Spinner VTG;
    private Spinner ZDA;

    public String GGAs;
    public String GLLs;
    public String GSAs;
    public String GSVs;
    public String RMCs;
    public String VTGs;
    public String ZDAs;

    private TextView txNMEAinp;
    private TextView txtChkBox;
    private TextView txtRS;
    private Button btnGetGPS;
    private TextView GPSid;
    private Button btnConnect;
    private Button btnPause;
    private Button btnSvNMEA;
    private Button btnNMEAdflt;
    private Button btnCold;
    private Button btnWarm;
    private Button btnHot;
    private Button btnFactory;
    private ScrollView mSvText;
    private TextView mTvText;
    private TextView mEPOinfo;
    private int homeFont;
    private int cmdRetries;
    private int cmdDelay;

    private CheckBox cbxInsecure;
    private String msg;
    private boolean NMEApaused = false;
    private int valx;
    private int resetCmd;
    private ProgressDialog pDialog;
    private BluetoothDevice GPSdevice = null;
    private String GPSname;
    private int flashSize;
    private String[] parms;
    private boolean hasAGPS = true;
    private boolean isGPSlogger = true;

    private File logPath;
    private File logFile;
    private String basePathName;
    private String logFileName;

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private Context mContext = Main.mContext;
    private int debugLVL;
    private final int ABORT = 9;
    private OutputStreamWriter logWriter = Main.logWriter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log(0, "HomeFragment.onCreateView()");

        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        basePathName = appPrefs.getString("basePathName", "");
        logFileName = appPrefs.getString("logFileName", "");
        logPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), basePathName);
        logFile = new File(logPath, logFileName);

        cmdRetries = Integer.parseInt(publicPrefs.getString("cmdRetries", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));

        mV = inflater.inflate(R.layout.home, container, false);

        GPstats = mV.findViewById(R.id.GPstats);
        txNMEAinp = mV.findViewById(R.id.txNMEAinp);
        txtChkBox = mV.findViewById(R.id.txtChkBox);

        txtGGA = mV.findViewById(R.id.txtGGA);
        txtGLL = mV.findViewById(R.id.txtGLL);
        txtGSA = mV.findViewById(R.id.txtGSA);
        txtGSV = mV.findViewById(R.id.txtGSV);
        txtRMC = mV.findViewById(R.id.txtRMC);
        txtVTG = mV.findViewById(R.id.txtVTG);
        txtZDA = mV.findViewById(R.id.txtZDA);
        txtRS = mV.findViewById(R.id.txtRS);
        GGA = mV.findViewById(R.id.GGA);
        GLL = mV.findViewById(R.id.GLL);
        GSA = mV.findViewById(R.id.GSA);
        GSV = mV.findViewById(R.id.GSV);
        RMC = mV.findViewById(R.id.RMC);
        VTG = mV.findViewById(R.id.VTG);
        ZDA = mV.findViewById(R.id.ZDA);

        GPSid = mV.findViewById(R.id.txtGPSname);
        mSvMsg = mV.findViewById(R.id.mSvMsg);
        msgFrame = mV.findViewById(R.id.msgFrame);

        //show stored GPS device name
        GPSid = mV.findViewById(R.id.txtGPSname);
        Main.GPSmac = appPrefs.getString("GPSmac", "");
        if (Main.GPSmac.length() > 0 && !(Main.bluetoothAdapter == null)) {
            GPSdevice = Main.bluetoothAdapter.getRemoteDevice(Main.GPSmac);
            GPSname = GPSdevice.getName();
            GPSid.setText(GPSname);
        }
        //show stored checkbox state
        cbxInsecure = mV.findViewById(R.id.cbxInsecure);
//        BT.allowInsecure = appPrefs.getBoolean("allowInsecure", false);
        cbxInsecure.setChecked(appPrefs.getBoolean("allowInsecure", false));

        mSvText = mV.findViewById(R.id.mSvText);
        mTvText = mV.findViewById(R.id.mTvText);

// --------------------------------------button handlers___________________________________________//
        btnSvNMEA = mV.findViewById(R.id.btnSvNMEA);
        btnSvNMEA.setTransformationMethod(null);
        btnSvNMEA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnSvNMEA.getText() + " pressed");
                btnSvNMEA();
            }
        });

        btnNMEAdflt = mV.findViewById(R.id.btnNMEAdflt);
        btnNMEAdflt.setTransformationMethod(null);
        btnNMEAdflt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnNMEAdflt.getText() + " pressed");
                Main.stopBkGrnd = true;
                new defaultNMEA().execute();
            }
        });


        btnPause = mV.findViewById(R.id.btnPause);
        btnPause.setTransformationMethod(null);
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnPause.getText() + " pressed");
                if (NMEApaused) {
                    startNMEA();
                    NMEApaused = false;
                    btnPause.setText(getString(R.string.btnNMEApause));
                } else {
                    Main.stopBkGrnd = true;
                    NMEApaused = true;
                    btnPause.setText(getString(R.string.btnNMEAresume));
                }
            }
        });

        btnGetGPS = mV.findViewById(R.id.btnGetGPS);
        btnGetGPS.setTransformationMethod(null);
        btnGetGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnGetGPS.getText() + " pressed");
                if (Main.bluetoothAdapter.isEnabled()) {
                    btnGetGPS();
                } else {
                    if (!Main.bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }
            }
        });

        cbxInsecure.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log(1, "HomeFragment.onCreateView() allow insecure checkbox changed");
                appPrefEditor.putBoolean("allowInsecure", isChecked);
                appPrefEditor.commit();
            }
        });

        btnConnect = mV.findViewById(R.id.btnConnect);
        btnConnect.setTransformationMethod(null);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnConnect.getText() + " pressed");
                if (GPSname == null || GPSname.isEmpty()) {
                    msg = getString(R.string.noGPSselected);
                    Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                } else {
                    if (Main.bluetoothAdapter.isEnabled()) {
                        btnConnect();
                    } else {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }
            }
        });


        btnHot = mV.findViewById(R.id.btnHot);
        btnHot.setTransformationMethod(null);
        btnHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnHot.getText() + " pressed");
                doReset(101, getString(R.string.btnHot));
            }
        });

        btnWarm = mV.findViewById(R.id.btnWarm);
        btnWarm.setTransformationMethod(null);
        btnWarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnWarm.getText() + " pressed");
                doReset(102, getString(R.string.btnWarm));
            }
        });

        btnCold = mV.findViewById(R.id.btnCold);
        btnCold.setTransformationMethod(null);
        btnCold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnCold.getText() + " pressed");
                doReset(103, getString(R.string.btnCold));
            }
        });

        btnFactory = mV.findViewById(R.id.btnFactory);
        btnFactory.setTransformationMethod(null);
        btnFactory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(1, "HomeFragment.onCreateView() button " + btnFactory.getText() + " pressed");
                doReset(104, getString(R.string.btnFactory));
            }
        });
        setTextSizes();
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity().getBaseContext(), R.array.listNMEAshow, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.spinnerdropdown);
        GGA.setAdapter(adapter);
        GLL.setAdapter(adapter);
        GSA.setAdapter(adapter);
        GSV.setAdapter(adapter);
        RMC.setAdapter(adapter);
        VTG.setAdapter(adapter);
        ZDA.setAdapter(adapter);

        if (Main.aborting) {
            // Get all touchable views
            ArrayList<View> layoutButtons = mV.getTouchables();
            // loop through them, if they are instances of Button, disable them.
            for (View v : layoutButtons) {
                if (v instanceof Button) {
                    v.setEnabled(false);
                }
            }
            Toast.makeText(mContext, Main.errMsg, Toast.LENGTH_LONG).show();
            msgFrame.append(Main.errMsg);
            return mV;
        }
        //show Log file info in messages frame
        if (Main.logFileIsOpen) {
            msgFrame.append(getText(R.string.writing) + logFile.getPath());
        }
        return mV;
    }//onCreateView(LayoutInflater, ViewGroup, Bundle)

    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log(0, "HomeFragment.onViewCreated()");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugPref", "0"));
        btnShowHide(false);
    }//onViewCreated()

    @Override
    public void onPause() {
        super.onPause();
        // do not Log here , causes abort on exit
        //stop the AsyncTask
        Main.stopBkGrnd = true;
    }//onPause()

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        Log(0, "HomeFragment.onResume()");
        if (!(Main.GPSsocket == null) && Main.GPSsocket.isConnected()) {
            getNMEAsetting();
            setNMEAfields();
            setTextSizes();
            startNMEA();
            GPstats.setText(Main.AGPSTxt + NL + Main.LogTxt);
            btnConnect.setText(getString(R.string.connected));
            btnShowHide(true);
        }
    }//onResume

    @Override
    public void onClick(android.support.v4.app.DialogFragment dialog) {
        Log(0, "HomeFragment.onClick()");
        if (Main.GPSmac == null) {
            return;
        }
        GPSdevice = Main.bluetoothAdapter.getRemoteDevice(Main.GPSmac);
        GPSname = GPSdevice.getName();
        GPSid.setText(GPSname);
        msg = GPSname + " " + getString(R.string.selected);
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }//onClick(android.app.DialogFragment dialog)

    @Override
    public void onDialogNegativeClick(android.support.v4.app.DialogFragment dialog) {
        Log(0, "HomeFragment.onDialogNegativeClick()");
        msg = getText(android.R.string.cancel) + " pressed";
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }//onDialogNegativeClick(android.app.DialogFragment dialog)

    private void btnSvNMEA() {
        Log(0, "HomeFragment - " + btnSvNMEA.getText() + " pressed");
        //send output settings to logger
        //@formatter:off
        String cmd = "PMTK314," +
                GLL.getSelectedItem().toString() + "," +
                RMC.getSelectedItem().toString() + "," +
                VTG.getSelectedItem().toString() + "," +
                GGA.getSelectedItem().toString() + "," +
                GSA.getSelectedItem().toString() + "," +
                GSV.getSelectedItem().toString() + "," + "0,0,0,0,0,0,0,0,0,0,0," +
                ZDA.getSelectedItem().toString() + "," + "0";
        //@formatter:on
        Main.sendCommand(cmd);

        appPrefEditor.putString("NMEAsettings", cmd);
        appPrefEditor.commit();
        Toast.makeText(mContext, getString(R.string.saved), Toast.LENGTH_LONG).show();
    }//btnSvNMEA()

    private void btnGetGPS() {
        Log(0, "HomeFragment - " + btnGetGPS.getText() + " pressed");
        getGPSid dialog = new getGPSid();
        dialog.setTargetFragment(HomeFragment.this, 1);
        dialog.show(getFragmentManager(), "getGPSinfo");
    }//btnGetGPS()

    private void btnConnect() {
        Log(0, "HomeFragment.btnConnect()");
        String str;
        if (Main.GPSmac == null) {
            msg = getString(R.string.noGPSselected);
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            Log(0, "HomeFragment.btnConnect() GPS logger has not been selected");
            return;
        }
        if (!(Main.GPSsocket == null) && Main.GPSsocket.isConnected()) {
            Main.stopBkGrnd = true;
            new disconnect().execute();
        } else {
            Main.stopBkGrnd = false;
            new connect().execute();
        }
    }//btnConnect()

    private void btnShowHide(boolean sv) {
        btnSvNMEA.setEnabled(sv);
        btnNMEAdflt.setEnabled(sv);
        btnPause.setEnabled(sv);
        btnCold.setEnabled(sv);
        btnWarm.setEnabled(sv);
        btnHot.setEnabled(sv);
        btnFactory.setEnabled(sv);
    }//btnShowHide()

    public String concatSarray(String[] Sa, int bgn) {
        Log(0, "HomeFragment.concatSarray()");
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

    public Date dateCalc(int weeks, int secs) {
        Log(1, "HomeFragment.dateCalc()");
        Calendar wrkCal = new GregorianCalendar(1980, 0, 6, 0, 0, 0);
        wrkCal.add(Calendar.DATE, weeks * 7);
        wrkCal.add(Calendar.SECOND, secs);
        return wrkCal.getTime();
    }//dateCalc()

    public void doReset(int code, String name) {
        Log(0, "HomeFragment.doReset()");
        //stop NMEA coutput
        Main.stopBkGrnd = true;
        resetCmd = code;
        resetCMD doreset = new resetCMD();
        doreset.execute();
    }//btnReset()

    public int getFlashSize(int model) {
        Log(0, "HomeFragment.getFlashSize()");
        // 8 Mbit = 1 Mb
        if (model == 0x1388) return (8 * 1024 * 1024 / 8); // 757/ZI v1
        if (model == 0x5202) return (8 * 1024 * 1024 / 8); // 757/ZI v2
        // 32 Mbit = 4 Mb
        if (model == 0x0000) return (32 * 1024 * 1024 / 8); // Holux M-1200E
        if (model == 0x0001) return (32 * 1024 * 1024 / 8); // Qstarz BT-Q1000X
        if (model == 0x0004) return (32 * 1024 * 1024 / 8); // 747 A+ GPS Trip Recorder
        if (model == 0x0005) return (32 * 1024 * 1024 / 8); // Qstarz BT-Q1000P
        if (model == 0x0006) return (32 * 1024 * 1024 / 8); // 747 A+ GPS Trip Recorder
        if (model == 0x0008) return (32 * 1024 * 1024 / 8); // Pentagram PathFinder P 3106
        if (model == 0x000F) return (32 * 1024 * 1024 / 8); // 747 A+ GPS Trip Recorder
        if (model == 0x005C) return (32 * 1024 * 1024 / 8); // Holux M-1000C
        if (model == 0x8300) return (32 * 1024 * 1024 / 8); // Qstarz BT-1200
        // 16Mbit -> 2Mb
        // 0x0051    i-Blue 737, Qstarz 810, Polaris iBT-GPS, Holux M1000
        // 0x0002    Qstarz 815
        // 0x001B    i-Blue 747
        // 0x001d    BT-Q1000 / BGL-32
        // 0x0131    EB-85A
        return (16 * 1024 * 1024 / 8);
    }//getFlashSize()

    public void getGPSinfo() {
        Log(0, "HomeFragment.getGPSinfo()");
        flashSize = 32 * 1024 * 1024 / 8; //a safe default flash size
        parms = Main.mtkCmd("PMTK605", "PMTK705", cmdDelay);
        if (parms != null && parms[0].contains("PMTK705")) {
            flashSize = getFlashSize((int) Long.parseLong(parms[2], 16));
            String ss = concatSarray(parms, 1);
            Log(0, asterisks);
            Log(0, ss);
            Log(0, asterisks);
        }
    }//getGPSinfo()

    public void goSleep(int mSec) {
        Log(3, String.format("HomeFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(e);
        }
    }//goSleep()

    private void getNMEAsetting() {
        String curFunc = "HomeFragment.getNMEAsetting()";
        Log(0, curFunc);

        int retry = 10;
        boolean OK = false;
        do {
            retry--;
            //get NMEA output setting from GPS
            parms = Main.mtkCmd("PMTK414", "PMTK514", cmdDelay);
            if (parms == null) {
                Log(1, String.format("%1$s parms == null", curFunc));
                continue;
            }
            if (!parms[0].contains("PMTK514")) {
                Log(1, String.format("%1$s received:%2$s", curFunc, parms[0]));
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
                retry = 0;
            } else {
                //reset GPS to default output if all 0 in settings
                Log(1, String.format("%1$s invalid NMEA stored - resetting", curFunc));
                Main.mtkCmd("PMTK314,-1", "PMTK001", cmdDelay);
                goSleep(200);
//                retry = 10;
                continue;
            }
        } while (retry > 0);
    }//getNMEAsetting()

    private String hasAGPS() {
        String curFunc = "HomeFragment.hasAGPS()";
//            Log(0, curFunc);
        StringBuilder txtOut = new StringBuilder();
        // check for AGPS
        parms = Main.mtkCmd("PMTK607", "PMTK707", cmdDelay);
        if (parms == null) {
            hasAGPS = false;
            Log(0, String.format("%1$s PMTK607 returned null", curFunc));
            //showToast(mContext.getString(R.string.noAGPS));
        } else {
            int EPOblks = 0;
            if ((parms[0].equals("PMTK001")) && !(parms[parms.length - 1].equals("3"))) {
                hasAGPS = false;
                Log(0, String.format("%1$s %2$ss returned", curFunc, concatSarray(parms, 0)));
            } else {
                EPOblks = Integer.valueOf(parms[1]);
                txtOut.append(parms[1] + " EPO sets");
                if (EPOblks > 0) {
                    Date dd = dateCalc(Integer.valueOf(parms[4]), Integer.valueOf(parms[5]));
                    txtOut.append(" expires " + SDF.format(dd));
                }
                appPrefEditor.putString("strAGPS", txtOut.toString());
                appPrefEditor.commit();
                Main.AGPSTxt = txtOut.toString();
                Log(0, String.format("%1$s GPS has AGPS ******", curFunc));
            }
        }
        return txtOut.toString();
    }//hasAGPS()

    private String isGPSlogger() {
        String curFunc = "HomeFragment.isGPSlogger()";
//            Log(0, curFunc);
        boolean OK;
        int logRecCount = 0;

        StringBuilder txtOut = new StringBuilder();
        //PMTK182,2,9 requests the GPS flash id
        parms = Main.mtkCmd("PMTK182,2,9", "PMTK182,3,9,", cmdDelay * 2);
        if (parms == null) {
            isGPSlogger = false;
            Log(0, String.format("%1$s PMTK182,2,9 returned null", curFunc));
        } else {
            if ((parms[0].equals("PMTK001")) && !(parms[parms.length - 1].equals("3"))) {
                isGPSlogger = false;
                Log(0, String.format("%1$s received: %2$ss", curFunc, concatSarray(parms, 0)));
            } else {
                int ix = 10;
                do {
                    ix--;
                    //PMTK182,2,10 requests the stored record count
                    parms = Main.mtkCmd("PMTK182,2,10", "PMTK182,3,10", cmdDelay * 2);
                    if (parms != null && parms[0].contains("PMTK182") && parms[1].contains("3")) {
                        logRecCount = Integer.parseInt(parms[3], 16);
                        appPrefEditor.putInt("logRecCount", logRecCount);
                        appPrefEditor.commit();
                        txtOut.append(Integer.toString(logRecCount) + " log records");
                        Main.LogTxt = txtOut.toString();
                        isGPSlogger = true;
                        ix = 0;
                        Log(0, String.format("%1$s GPS is data logger ******", curFunc));
                        Log(0, String.format("%1$s log has %2$d records ******", curFunc, logRecCount));
                    }
                } while (ix > 0);
            }
        }
        return txtOut.toString();
    }//isGPSlogger()

    private void Log(int mode, String msg) {
        if (!Main.logFileIsOpen) {
            return;
        }
        switch (mode) {
            case 0:
                if (msg.length() > 127) {
                    msg = msg.substring(0, 127) + " ...";
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

    public void sendPMTK253() {
        Log(0, "HomeFragment.sendPMTK253()");
        //send reset to normal text output - precaution to correct binary communication failure
        Main.sendBytes(binPMTK253);
    }//sendPMTK253()

    private void setListeners() {
//        mLog(VB1, "HomeFragment.setListeners()");
        //set item selected listeners for the NMEA spinners - not done in onCreateView as
        //setOnItemSelectedListener event is triggered when spiinner value is initialized
        GGA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() GGA.setOnItemSelectedListener");
                GGAs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        GLL.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() GLL.setOnItemSelectedListener");
                GLLs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        GSA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() GSA.setOnItemSelectedListener");
                GSAs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        GSV.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() GSV.setOnItemSelectedListener");
                GSVs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        RMC.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() RMC.setOnItemSelectedListener");
                RMCs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        VTG.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() VTG.setOnItemSelectedListener");
                VTGs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ZDA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log(1, "HomeFragment.setListeners() ZDA.setOnItemSelectedListener");
                ZDAs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }//setListeners()

    private void setNMEAfields() {
        Log(1, "HomeFragment.setNMEAfields()");
        String cmd = appPrefs.getString("saveMNEA", "PMTK314,0,1,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0");
        parms = cmd.split(",");
        GLLs = parms[1];
        RMCs = parms[2];
        VTGs = parms[3];
        GGAs = parms[4];
        GSAs = parms[5];
        GSVs = parms[6];
        ZDAs = parms[18];
        GLL.setSelection(Integer.parseInt(GLLs));
        RMC.setSelection(Integer.parseInt(RMCs));
        VTG.setSelection(Integer.parseInt(VTGs));
        GGA.setSelection(Integer.parseInt(GGAs));
        GSA.setSelection(Integer.parseInt(GSAs));
        GSV.setSelection(Integer.parseInt(GSVs));
        ZDA.setSelection(Integer.parseInt(ZDAs));
    }//setNMEAfields()

    private void setTextSizes() {
        Log(1, "HomeFragment.setTextSizes()");
        homeFont = Integer.parseInt(publicPrefs.getString("homeFont", "13"));

        int NMEAfont = homeFont - 3;
        int epoMSGfont = homeFont - 4;
        int btnfont = homeFont - 2;

        txtGGA.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);
        txtGLL.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);
        txtGSA.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);
        txtGSV.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);
        txtRMC.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);
        txtVTG.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);
        txtZDA.setTextSize(TypedValue.COMPLEX_UNIT_SP, NMEAfont);

        txtChkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, epoMSGfont + 1);
        txNMEAinp.setTextSize(TypedValue.COMPLEX_UNIT_SP, epoMSGfont);
        btnSvNMEA.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnNMEAdflt.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnPause.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnGetGPS.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnConnect.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnCold.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnWarm.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnHot.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        btnFactory.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont);
        txtRS.setTextSize(TypedValue.COMPLEX_UNIT_SP, btnfont - 2);
    }//setTextSizes()

    private void startNMEA() {
        Log(0, "HomeFragment.startNMEA()");
        Main.stopBkGrnd = false;
        showNMEA task = new showNMEA();
        task.execute();
    }//startNMEA()

    class showNMEA extends AsyncTask<Void, String, Void> {
        boolean loop = true;

        protected void onPreExecute() {
            Log(1, "HomeFragment.showNMEA.onPreExecute()");
            Main.stopBkGrnd = false;
            //nothing to do here
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            Log(1, "HomeFragment.showNMEA.doInBackground()");
            String reply = null;

            while (!Main.stopBkGrnd) {
                reply = Main.readString(10);
                if (reply != null) publishProgress(reply);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (mTvText.length() > TEXT_MAX_SIZE) {
                StringBuilder sb = new StringBuilder();
                sb.append(mTvText.getText());
                sb.delete(0, TEXT_MAX_SIZE / 2);
                mTvText.setText(sb);
            }
            mTvText.append(values[0]);
            mText.setLength(0);
            mSvText.fullScroll(View.FOCUS_DOWN);
        }//onProgressUpdate()

        protected void onPostExecute() {
            Log(1, "HomeFragment.bkGroundOK.onPostExecute()");
            Main.stopBkGrnd = true;
            //nothing to do here
        }//onPostExecute()
    }//class showNMEA

    public class connect extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = new ProgressDialog(mContext);
        NavigationView navigationView;
        Menu nav_Menu;

        @Override
        protected void onPreExecute() {
            Log(1, "HomeFragment.connect.onPreExecute()");
            this.dialog.setMessage(getString(R.string.connecting));
            this.dialog.show();
            Main.GPstatsTxt = null;
            Main.stopBkGrnd = false;
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... voids) {
            Log(1, "HomeFragment.connect.doInBackground()");
            boolean ok = Main.connect();
            if (!ok) return null;
            if (Main.GPSsocket.isConnected()) {
                //send text output reset - precautionary to correct fail during binary mode
                sendPMTK253();
                goSleep(250);
                getGPSinfo();
                if (recordingModeIsOK()) {
                    //check for AGPS and show expiry when GPS has AGPS
                    Main.GPstatsTxt = hasAGPS() + NL;
                    //check for logger function and show trackpoint count
                    Main.GPstatsTxt = Main.GPstatsTxt + isGPSlogger();
                    //make sure logger can display NMEA output
                    getNMEAsetting();
                    Log(0, "HomeFragment.connect.doInBackground() Connected *****");
                } else {
                    Main.aborting = true;
                    Main.errMsg = getText(R.string.wrongMode).toString();
                }
            }
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            Log(1, "HomeFragment.connect.onPostExecute()");
            if (Main.aborting) {
                Toast.makeText(mContext, Main.errMsg, Toast.LENGTH_LONG).show();
                return;
            }
            if (Main.GPSsocket.isConnected()) {
                navigationView = getActivity().findViewById(R.id.nav_view);
                nav_Menu = navigationView.getMenu();
                nav_Menu.findItem(R.id.nav_Settings).setVisible(true);
                if (hasAGPS) {
                    Log(1, "HomeFragment.connect.onPostExecute() enabling GetEPO, UpdtAGPS");
                    nav_Menu.findItem(R.id.nav_GetEPO).setVisible(true);
//                    nav_Menu.findItem(R.id.nav_CheckEPO).setVisible(true);
                    nav_Menu.findItem(R.id.nav_UpdtAGPS).setVisible(true);
                } else {
                    msgFrame.append(NL + getText(R.string.noAGPS));
                    Toast.makeText(mContext, mContext.getString(R.string.noAGPS), Toast.LENGTH_LONG).show();
                }
                if (isGPSlogger) {
                    //logging method 1=overlap, 2=stop when full - app can only handle stop when full
                    Log(1, "HomeFragment.connect.onPostExecute() enabling GetLog, MakeGPX");
                    nav_Menu.findItem(R.id.nav_GetLog).setVisible(true);
                    nav_Menu.findItem(R.id.nav_clrLog).setVisible(true);
                    nav_Menu.findItem(R.id.nav_MakeGPX).setVisible(true);
                } else {
                    msgFrame.append(NL + getText(R.string.noLog));
                    Toast.makeText(mContext, mContext.getString(R.string.noLog), Toast.LENGTH_LONG).show();
                }

                //get and show NMEA setting
                setListeners();
                setNMEAfields();
                btnConnect.setText(getString(R.string.connected));
                GPstats.setText(Main.GPstatsTxt);
                msg = GPSname + " " + mContext.getString(R.string.GPSconnected);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                Log(0, "HomeFragment.connect.onPostExecute() " + msg);
                btnShowHide(true);
                btnGetGPS.setEnabled(false);
                startNMEA();
            } else {
                msg = mContext.getString(R.string.GPSconnectFail);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                Log(0, "HomeFragment.connect.onPostExecute() " + msg);
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            mSvMsg.post(new Runnable() {
                @Override
                public void run() {
                    mSvMsg.fullScroll(View.FOCUS_DOWN);
                }
            });
        }//onPostExecute()

        private boolean recordingModeIsOK() {
            String curFunc = "HomeFragment.connect.recordingModeIsOK()";
            Log(0, curFunc);
            int ix = cmdRetries;
            while (ix > 0) {//query logging method 1=overlap, 2=stop when full
                parms = Main.mtkCmd("PMTK182,2,6", "PMTK182,3,6", cmdDelay);
                if (parms == null) {
                    goSleep(150);
                    ix--;
                    continue;
                }
                Log(0, String.format("%1$s recording mode is %2$s ******", curFunc, parms[3]));
                if (parms[3].equals("2")) {
                    return true;
                } else {
                    return false;
                }
            };
            Log(0, String.format("%1$s PMTK182,2,6 failed ******", curFunc));
            return false;
        }//recordingModeIsOK()

        public void NMEAgetSetting() {
        }//NMEAgetSetting()
    }//class connect

    public class defaultNMEA extends AsyncTask<Void, Void, Void> {
        String str, strAGPS, strGPS;

        private ProgressDialog dialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {
            Log(1, "HomeFragment.defaultNMEA.onPreExecute()");
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... voids) {
            Log(1, "HomeFragment.defaultNMEA.doInBackground()");
            Main.mtkCmd("PMTK314,-1", "PMTK001", cmdDelay);
            getNMEAsetting();
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            Log(1, "HomeFragment.defaultNMEA.onPostExecute()");
            setNMEAfields();
            startNMEA();

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }//onPostExecute()
    }//class defaultNMEA


    public class disconnect extends AsyncTask<Void, Void, Void> {
        NavigationView navigationView;
        Menu nav_Menu;

        private ProgressDialog dialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {
            Log(1, "HomeFragment.disconnect.onPreExecute()");
//            readBytesDelay = cmdDelay;
            this.dialog.setMessage(getString(R.string.disconnecting));
            this.dialog.show();
            navigationView = getActivity().findViewById(R.id.nav_view);
            nav_Menu = navigationView.getMenu();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log(1, "HomeFragment.disconnect.doInBackground()");
            Main.disconnect();
            return null;
        }

        //        @Override
        protected void onPostExecute(Void param) {
            Log(1, "HomeFragment.disconnect.onPostExecute()");
            btnConnect.setText(getString(R.string.disconnected));
            msg = String.format(getString(R.string.GPSdisconnected), GPSname);
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            Log(0, "HomeFragment.disconnect.onPostExecute() " + msg);
            nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
            nav_Menu.findItem(R.id.nav_clrLog).setVisible(false);
            nav_Menu.findItem(R.id.nav_UpdtAGPS).setVisible(false);
            nav_Menu.findItem(R.id.nav_Settings).setVisible(false);
//            nav_Menu.findItem(R.id.nav_eMail).setVisible(false);
            GPstats.setText("");
            btnShowHide(false);
            btnGetGPS.setEnabled(true);
            mTvText.setText("");

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }// class disconnect

    private class resetCMD extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog dialog = new ProgressDialog(mContext);
        String msg;

        @Override
        protected void onPreExecute() {
            Log(1, "HomeFragment.resetCmd.onPreExecute()");
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
        }//onPreExecute()

        @Override
        protected Integer doInBackground(Void... params) {
            Log(1, "HomeFragment.resetCmd.doInBackground()");
            switch (resetCmd) {
                case 101:
                    Log(1, "HomeFragment.resetCMD.doInBackground() sending mtkCmd(PMTK101, PMTK010,001)");
                    Main.mtkCmd("PMTK101", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnHot) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
                case 102:
                    Log(1, "HomeFragment.resetCMD.doInBackground() sending mtkCmd(PMTK102, PMTK010,001)");
                    Main.mtkCmd("PMTK102", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnWarm) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
                case 103:
                    Log(1, "HomeFragment.resetCMD.doInBackground() sending mtkCmd(PMTK103, PMTK010,001)");
                    Main.mtkCmd("PMTK103", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnCold) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
                case 104:
                    Log(1, "HomeFragment.resetCMD.doInBackground() sending mtkCmd(PMTK104, PMTK010,001)");
                    Main.mtkCmd("PMTK104", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnFactory) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
            }
            return resetCmd;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log(1, "HomeFragment.resetCmd.onPostExecute()");
            startNMEA();
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            Log(0, "" + msg);

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }//class resetCMD
}
