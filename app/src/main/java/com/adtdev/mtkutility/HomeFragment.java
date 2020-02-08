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
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class HomeFragment extends Fragment implements getGPSid.GPSdialogListener {

    private static String NL = System.getProperty("line.separator");
    SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    private final int REQUEST_ENABLE_BT = 88;
    private static StringBuilder mText = new StringBuilder();
    private static final int TEXT_MAX_SIZE = 5120;
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
    private static ScrollView mSvText;
    private static TextView mTvText;
    private TextView mEPOinfo;
    private int homeFont;
    private int cmdRetry;
    private static int cmdDelay;

    private CheckBox cbxInsecure;
    private String msg;
    private boolean NMEApaused = false;
    private int valx;
    private int resetCmd;
    private int recMode;
    private ProgressDialog pDialog;
    private BluetoothDevice GPSdevice = null;
    private String GPSname;
    private String[] parms;
    private boolean hasAGPS = true;
    private boolean isGPSlogger = true;
    private String GPSmac;

    private File logPath;
    private File logFile;
    private String basePath;
    private String logFileName;
    private static boolean logFileIsOpen;

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private Context mContext = Main.mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logFileIsOpen = Main.logFileIsOpen;
        final String currFunc = "HomeFragment.onCreateView";
        mLog(0, currFunc);

        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        basePath = appPrefs.getString("basePath", "");
        logFileName = appPrefs.getString("logFileName", "");
        logFile = new File(basePath, logFileName);

        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "50"));

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
        GPSmac = appPrefs.getString("GPSmac", "");
        if (!GPSmac.isEmpty()) {
            GPSdevice = Main.bluetoothAdapter.getRemoteDevice(GPSmac);
            GPSname = GPSdevice.getName();
            GPSid.setText(GPSname);
        }
        //show stored checkbox state
        cbxInsecure = mV.findViewById(R.id.cbxInsecure);
        cbxInsecure.setChecked(appPrefs.getBoolean("allowInsecure", false));

        mSvText = mV.findViewById(R.id.mSvText);
        mTvText = mV.findViewById(R.id.mTvText);

// --------------------------------------button handlers___________________________________________//
        btnSvNMEA = mV.findViewById(R.id.btnSvNMEA);
        btnSvNMEA.setTransformationMethod(null);
        btnSvNMEA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnSvNMEA.getText()));
                btnSvNMEA();
            }
        });

        btnNMEAdflt = mV.findViewById(R.id.btnNMEAdflt);
        btnNMEAdflt.setTransformationMethod(null);
        btnNMEAdflt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnNMEAdflt.getText()));
                new defaultNMEA(mContext).execute();
            }
        });


        btnPause = mV.findViewById(R.id.btnPause);
        btnPause.setTransformationMethod(null);
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnPause.getText()));
                if (NMEApaused) {
                    startNMEA();
                    NMEApaused = false;
                    btnPause.setText(getString(R.string.btnNMEApause));
                } else {
                    while (Main.NMEArunning) {
                        Main.showNMEA = false;
                        goSleep(50);
                    }
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
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnGetGPS.getText()));
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
                mLog(1, String.format("%1$s %2$s ", currFunc, "allow insecure checkbox changed"));
                appPrefEditor.putBoolean("allowInsecure", isChecked).commit();
            }
        });

        btnConnect = mV.findViewById(R.id.btnConnect);
        btnConnect.setTransformationMethod(null);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnConnect.getText()));
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
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnHot.getText()));
                doReset(101, getString(R.string.btnHot));
            }
        });

        btnWarm = mV.findViewById(R.id.btnWarm);
        btnWarm.setTransformationMethod(null);
        btnWarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnWarm.getText()));
                doReset(102, getString(R.string.btnWarm));
            }
        });

        btnCold = mV.findViewById(R.id.btnCold);
        btnCold.setTransformationMethod(null);
        btnCold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnCold.getText()));
                doReset(103, getString(R.string.btnCold));
            }
        });

        btnFactory = mV.findViewById(R.id.btnFactory);
        btnFactory.setTransformationMethod(null);
        btnFactory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(1, String.format("%1$s button %2$s pressed +++++", currFunc, btnFactory.getText()));
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
            msgFrame.append(getText(R.string.writing) + logFile.getPath() + NL);
        }
        int offset = appPrefs.getInt("DLcmd", 0);
        if (offset > 0) {
            msgFrame.append(String.format(getString(R.string.logRS), offset) + NL);
        }
        return mV;
    }//onCreateView(LayoutInflater, ViewGroup, Bundle)

    public void onViewCreated(View view, Bundle savedInstanceState) {
        mLog(0, "HomeFragment.onViewCreated");
        ((DrawerLocker) getActivity()).setDrawerEnabled(true);
        btnShowHide(false);
    }//onViewCreated()

    @Override
    public void onPause() {
        super.onPause();
        // do not Log here , causes abort on exit
        //stop the AsyncTask
    }//onPause()

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        ((DrawerLocker) getActivity()).setDrawerEnabled(true);
        mLog(0, "HomeFragment.onResume");
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "50"));
        recMode = appPrefs.getInt("recMode", 0);
        if (Main.GPSsocket == null) return;
        if (Main.GPSsocket.isConnected()) {
            Main.getNMEAsetting();
            setNMEAfields();
            setTextSizes();
            NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
            Menu nav_Menu = navigationView.getMenu();
            switch (recMode) {
                case 1:
                    nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
                    msgFrame.append(getText(R.string.wrongMode) + NL);
                    break;
                case 2:
                    nav_Menu.findItem(R.id.nav_GetLog).setVisible(true);
                    break;
                case 9:
                    nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
                    nav_Menu.findItem(R.id.nav_clrLog).setVisible(false);
                    nav_Menu.findItem(R.id.nav_MakeGPX).setVisible(false);
            }
            btnConnect.setText(getString(R.string.connected));
            btnShowHide(true);
            new getRecCount().execute();
        }
    }//onResume

    @Override
    public void onClick(android.support.v4.app.DialogFragment dialog) {
        mLog(0, "HomeFragment.onClick");
        if (GPSmac == null) return;
        GPSmac = appPrefs.getString("GPSmac", "");
        GPSdevice = Main.bluetoothAdapter.getRemoteDevice(GPSmac);
        GPSname = GPSdevice.getName();
        GPSid.setText(GPSname);
        msg = GPSname + " " + getString(R.string.selected);
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }//onClick(android.app.DialogFragment dialog)

    @Override
    public void onDialogNegativeClick(android.support.v4.app.DialogFragment dialog) {
        mLog(0, "HomeFragment.onDialogNegativeClick");
        msg = getText(android.R.string.cancel) + " pressed +++++";
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }//onDialogNegativeClick(android.app.DialogFragment dialog)

    private void btnSvNMEA() {
        mLog(0, "HomeFragment.btnSvNMEA");
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

        appPrefEditor.putString("NMEAsettings", cmd).commit();
        Toast.makeText(mContext, getString(R.string.saved), Toast.LENGTH_LONG).show();
    }//btnSvNMEA()

    private void btnGetGPS() {
        mLog(0, "HomeFragment.btnGetGPS");
        getGPSid dialog = new getGPSid();
        dialog.setTargetFragment(HomeFragment.this, 1);
        dialog.show(getFragmentManager(), "getGPSinfo");
    }//btnGetGPS()

    private void btnConnect() {
        mLog(0, "HomeFragment.btnConnect");
        String str;
        if (!(Main.GPSsocket == null) && Main.GPSsocket.isConnected()) {
            new disconnect(mContext).execute();
        } else {
            new connect(mContext).execute();
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
        mLog(0, "HomeFragment.concatSarray");
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
        mLog(1, "HomeFragment.dateCalc");
        Calendar wrkCal = new GregorianCalendar(1980, 0, 6, 0, 0, 0);
        wrkCal.add(Calendar.DATE, weeks * 7);
        wrkCal.add(Calendar.SECOND, secs);
        return wrkCal.getTime();
    }//dateCalc()

    public void doReset(int code, String name) {
        mLog(0, "HomeFragment.doReset");
        //stop NMEA coutput
        resetCmd = code;
        resetCMD doreset = new resetCMD(mContext);
        doreset.execute();
    }//btnReset()

    public int getFlashSize(int model) {
        mLog(0, "HomeFragment.getFlashSize");
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
//        int flashSize;
        mLog(0, "HomeFragment.getGPSinfo");
//        flashSize = 32 * 1024 * 1024 / 8; //a safe default flash size
        parms = Main.mtkCmd("PMTK605", "PMTK705", cmdDelay);
        if (parms != null && parms[0].contains("PMTK705")) {
//            flashSize = getFlashSize((int) Long.parseLong(parms[2], 16));
            String ss = concatSarray(parms, 1);
            mLog(0, asterisks);
            mLog(0, ss);
            mLog(0, asterisks);
        }
    }//getGPSinfo()

    public void goSleep(int mSec) {
        mLog(3, String.format("HomeFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(Log.getStackTraceString(e));
        }
    }//goSleep()

    private void isAGPS() {
        String curFunc = "HomeFragment.isAGPS";
        mLog(0, curFunc);
        boolean noGPS = publicPrefs.getBoolean("noGPS", false);
        if (noGPS) {
            hasAGPS = false;
            return;
        }
        StringBuilder txtOut = new StringBuilder();
        // check for AGPS
        int retry = cmdRetry;
        while (retry > 0) {
            mLog(2, String.format("%1$s PMTK607 retry %2$d", curFunc, retry));
            retry--;
            parms = Main.mtkCmd("PMTK607", "PMTK707", cmdDelay * 2);
            if (parms != null) retry = 0;
        }
        if (parms == null) {
            hasAGPS = false;
            mLog(0, String.format("%1$s %2$s returned null *****", curFunc, concatSarray(parms, 0)));
        } else {
            int EPOblks = 0;
            if ((parms[0].equals("PMTK001")) && !(parms[parms.length - 1].equals("3"))) {
                hasAGPS = false;
                mLog(0, String.format("%1$s %2$s returned *****", curFunc, concatSarray(parms, 0)));
            } else {
                EPOblks = Integer.valueOf(parms[1]);
                txtOut.append(parms[1] + " EPO sets");
                if (EPOblks > 0) {
                    Date dd = dateCalc(Integer.valueOf(parms[4]), Integer.valueOf(parms[5]));
                    txtOut.append(" expires " + SDF.format(dd));
                }
                appPrefEditor.putString("strAGPS", txtOut.toString()).commit();
                mLog(0, String.format("%1$s GPS has AGPS ******", curFunc));
            }
        }
    }//checkForAGPS()

    private String getRecCount() {
        String curFunc = "HomeFragment.getRecCount";
        mLog(0, curFunc);
        boolean OK;
        int logRecCount = 0;

        StringBuilder txtOut = new StringBuilder();
        int retry = cmdRetry;
        while (retry > 0) {
            retry--;
            //PMTK182,2,9 requests the GPS flash id
            parms = Main.mtkCmd("PMTK182,2,9", "PMTK182,3,9,", cmdDelay * 2);
            if (parms != null) retry = 0;
        }
        if (parms == null) {
            isGPSlogger = false;
            mLog(0, String.format("%1$s PMTK182,2,9 returned null", curFunc));
        } else {
            if ((parms[0].equals("PMTK001")) && !(parms[parms.length - 1].equals("3"))) {
                isGPSlogger = false;
                mLog(0, String.format("%1$s received: %2$ss", curFunc, concatSarray(parms, 0)));
            } else {
                retry = cmdRetry;
                while (retry > 0) {
                    retry--;
                    //PMTK182,2,10 requests the stored record count
                    parms = Main.mtkCmd("PMTK182,2,10", "PMTK182,3,10", cmdDelay * 2);
                    if (parms == null) continue;
                    if (parms[0].contains("PMTK182") && parms[1].contains("3")) {
                        logRecCount = Integer.parseInt(parms[3], 16);
                        appPrefEditor.putInt("logRecCount", logRecCount);
                        isGPSlogger = true;
                        retry = 0;
                    }
                }
                mLog(0, String.format("%1$s GPS is data logger ******", curFunc));
                mLog(0, String.format("%1$s log has %2$d records", curFunc, logRecCount));
                appPrefEditor.putString("strLOGR", String.format(getString(R.string.logrecs), logRecCount)).commit();
            }
        }
        return txtOut.toString();
    }//getRecCount()

    private static void mLog(int mode, String msg) {
        if (logFileIsOpen) {
            Main.mLog(mode, msg);
        }
    }//Log()

    public void sendPMTK253() {
        mLog(0, "HomeFragment.sendPMTK253");
        //send reset to normal text output - precaution to correct binary communication failure
        Main.sendBytes(binPMTK253);
        goSleep(500);
    }//sendPMTK253()

    private void setListeners() {
        //set item selected listeners for the NMEA spinners - not done in onCreateView as
        //setOnItemSelectedListener event is triggered when spiinner value is initialized
        GGA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners GGA.setOnItemSelectedListener");
                GGAs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        GLL.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners GLL.setOnItemSelectedListener");
                GLLs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        GSA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners GSA.setOnItemSelectedListener");
                GSAs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        GSV.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners GSV.setOnItemSelectedListener");
                GSVs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        RMC.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners RMC.setOnItemSelectedListener");
                RMCs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        VTG.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners VTG.setOnItemSelectedListener");
                VTGs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ZDA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mLog(1, "HomeFragment.setListeners ZDA.setOnItemSelectedListener");
                ZDAs = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }//setListeners()

    private void setNMEAfields() {
        mLog(1, "HomeFragment.setNMEAfields");
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
        mLog(1, "HomeFragment.setTextSizes");
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
        mLog(0, "HomeFragment.startNMEA");
        showNMEA task = new showNMEA();
        task.execute();
    }//startNMEA()

    static class showNMEA extends AsyncTask<Void, String, Void> {
        boolean loop = true;

        protected void onPreExecute() {
            mLog(1, "HomeFragment.showNMEA.onPreExecute");
            Main.showNMEA = true;
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(1, "HomeFragment.showNMEA.doInBackground");
            Main.NMEArunning = true;
            String reply = null;
            while (Main.showNMEA) {
                reply = Main.readString(cmdDelay);
                if (reply != null) publishProgress(reply);
            }
            Main.NMEArunning = false;
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
    }//class showNMEA

    public class connect extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = new ProgressDialog(mContext);
        NavigationView navigationView;
        Menu nav_Menu;

        public connect(Context context) {
            mLog(0, "HomeFragment.connect.connect");
            mContext = context;
        }//connect()

        @Override
        protected void onPreExecute() {
            mLog(1, "HomeFragment.connect.onPreExecute");
            this.dialog.setMessage(getString(R.string.connecting));
            this.dialog.show();
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... voids) {
            mLog(1, "HomeFragment.connect.doInBackground");
            boolean ok = Main.connect();
            if (!ok) return null;
            if (Main.GPSsocket.isConnected()) {
                //send text output reset - precautionary to correct fail during binary mode
                sendPMTK253();
                getGPSinfo();
                //get recording mode
                getRecordingMode();
                //check for AGPS and show expiry when GPS has AGPS
                isAGPS();
                //check for logger function and show trackpoint count
                if (recMode < 9) getRecCount();
                //make sure logger can display NMEA output
                Main.getNMEAsetting();
                mLog(0, "HomeFragment.connect.doInBackground() Connected *****");
            }
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(1, "HomeFragment.connect.onPostExecute");
            if (Main.aborting) {
                Toast.makeText(mContext, Main.errMsg, Toast.LENGTH_LONG).show();
                return;
            }
            if (Main.GPSsocket.isConnected()) {
                navigationView = getActivity().findViewById(R.id.nav_view);
                nav_Menu = navigationView.getMenu();
                nav_Menu.findItem(R.id.nav_Settings).setVisible(true);
                if (hasAGPS) {
                    mLog(1, "HomeFragment.connect.onPostExecute() enabling GetEPO, UpdtAGPS");
                    nav_Menu.findItem(R.id.nav_GetEPO).setVisible(true);
                    nav_Menu.findItem(R.id.nav_UpdtAGPS).setVisible(true);
                } else {
                    msgFrame.append(getText(R.string.noAGPS));
                    Toast.makeText(mContext, mContext.getString(R.string.noAGPS), Toast.LENGTH_LONG).show();
                    nav_Menu.findItem(R.id.nav_GetEPO).setVisible(false);
                    nav_Menu.findItem(R.id.nav_UpdtAGPS).setVisible(false);
                }
                switch (recMode) {
                    case 1:
                        nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
                        msgFrame.append(getText(R.string.wrongMode) + NL);
                        break;
                    case 2:
                        nav_Menu.findItem(R.id.nav_GetLog).setVisible(true);
                        nav_Menu.findItem(R.id.nav_clrLog).setVisible(true);
                        nav_Menu.findItem(R.id.nav_MakeGPX).setVisible(true);
                        break;
                    case 9:
                        nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
                        nav_Menu.findItem(R.id.nav_clrLog).setVisible(false);
                        nav_Menu.findItem(R.id.nav_MakeGPX).setVisible(false);
                        msgFrame.append(getText(R.string.noLog) + NL);
                }
                //get and show NMEA setting
                setListeners();
                setNMEAfields();
                btnConnect.setText(getString(R.string.connected));
                String agps = appPrefs.getString("strAGPS", "");
                String logr = appPrefs.getString("strLOGR", "");
                GPstats.setText(agps + NL + logr);
                msg = GPSname + " " + mContext.getString(R.string.GPSconnected);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                mLog(0, "HomeFragment.connect.onPostExecute " + msg);
                btnShowHide(true);
                btnGetGPS.setEnabled(false);
                startNMEA();
            } else {
                msg = mContext.getString(R.string.GPSconnectFail);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                mLog(0, "HomeFragment.connect.onPostExecute " + msg);
            }
            if (dialog.isShowing()) dialog.dismiss();
            mSvMsg.post(new Runnable() {
                @Override
                public void run() {
                    mSvMsg.fullScroll(View.FOCUS_DOWN);
                }
            });
        }//onPostExecute()

        private void getRecordingMode() {
            String curFunc = "HomeFragment.connect.getRecordingMode";
            mLog(0, curFunc);
            boolean noLOG = publicPrefs.getBoolean("noLOG", false);
            if (noLOG) {
                appPrefEditor.putInt("recMode", 9).commit();
                isGPSlogger = false;
                return;
            }
            int ix = cmdRetry;
            while (ix > 0) {//query logging method 1=overlap, 2=stop when full
                parms = Main.mtkCmd("PMTK182,2,6", "PMTK182,3,6", cmdDelay);
//                parms[0] = "PMTK001"; parms[1] = "182"; parms[2] = "1"; //test not a GPS logger
                if (parms == null) {
                    goSleep(150);
                    ix--;
                    continue;
                } else if (parms[0].contains("PMTK001")) {
                    if (parms[2].contains("1")) {
                        //command not recognized - not a GPS logger - set recording mode to 9
                        recMode = 9;
                        mLog(0, String.format("%1$s recording mode is %2$s ******", curFunc, recMode));
                        appPrefEditor.putInt("recMode", recMode).commit();
                        isGPSlogger = false;
                        return;
                    } else continue; //retry if PMTK001 result is 2 or 3
                } else {
                    mLog(0, String.format("%1$s recording mode is %2$s ******", curFunc, parms[3]));
                    recMode = Integer.parseInt(parms[3]);
                    appPrefEditor.putInt("recMode", recMode).commit();
                    return;
                }
            }
            mLog(0, String.format("%1$s PMTK182,2,6 failed ******", curFunc));
        }//recordingModeIsOK()
    }//class connect

    public class defaultNMEA extends AsyncTask<Void, Void, Void> {
        String str, strAGPS, strGPS;

        private ProgressDialog dialog = new ProgressDialog(mContext);

        public defaultNMEA(Context context) {
            mLog(0, "HomeFragment.defaultNMEA.defaultNMEA");
            mContext = context;
            while (Main.NMEArunning) {
                Main.showNMEA = false;
                goSleep(50);
            }
        }//defaultNMEA()

        @Override
        protected void onPreExecute() {
            mLog(1, "HomeFragment.defaultNMEA.onPreExecute");
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... voids) {
            mLog(1, "HomeFragment.defaultNMEA.doInBackground");
            Main.mtkCmd("PMTK314,-1", "PMTK001", cmdDelay);
            Main.getNMEAsetting();
            return null;
        }//doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(1, "HomeFragment.defaultNMEA.onPostExecute");
            setNMEAfields();
            startNMEA();
            if (dialog.isShowing()) dialog.dismiss();
        }//onPostExecute()
    }//class defaultNMEA

    public class disconnect extends AsyncTask<Void, Void, Void> {
        NavigationView navigationView;
        Menu nav_Menu;

        private ProgressDialog dialog = new ProgressDialog(mContext);

        public disconnect(Context context) {
            mLog(0, "HomeFragment.disconnect.disconnect");
            mContext = context;
            while (Main.NMEArunning) {
                Main.showNMEA = false;
                goSleep(50);
            }
        }//disconnect()

        @Override
        protected void onPreExecute() {
            mLog(1, "HomeFragment.disconnect.onPreExecute");
            this.dialog.setMessage(getString(R.string.disconnecting));
            this.dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mLog(1, "HomeFragment.disconnect.doInBackground");
            Main.disconnect();
            return null;
        }

        protected void onPostExecute(Void param) {
            mLog(1, "HomeFragment.disconnect.onPostExecute");
            btnConnect.setText(getString(R.string.disconnected));
            msg = String.format(getString(R.string.GPSdisconnected), GPSname);
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            mLog(0, "HomeFragment.disconnect.onPostExecute() " + msg);
            navigationView = getActivity().findViewById(R.id.nav_view);
            nav_Menu = navigationView.getMenu();
            nav_Menu.findItem(R.id.nav_GetLog).setVisible(false);
            nav_Menu.findItem(R.id.nav_clrLog).setVisible(false);
            nav_Menu.findItem(R.id.nav_UpdtAGPS).setVisible(false);
            nav_Menu.findItem(R.id.nav_Settings).setVisible(false);
            GPstats.setText("");
            btnShowHide(false);
            btnGetGPS.setEnabled(true);
            mTvText.setText("");
            if (dialog.isShowing()) dialog.dismiss();
        }
    }// class disconnect

    private class resetCMD extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog dialog = new ProgressDialog(mContext);
        String msg;

        public resetCMD(Context context) {
            mLog(0, "HomeFragment.resetCMD.resetCMD");
            mContext = context;
            while (Main.NMEArunning) {
                Main.showNMEA = false;
                goSleep(50);
            }
        }//resetCMD()

        @Override
        protected void onPreExecute() {
            mLog(1, "HomeFragment.resetCmd.onPreExecute");
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
        }//onPreExecute()

        @Override
        protected Integer doInBackground(Void... params) {
            mLog(1, "HomeFragment.resetCmd.doInBackground");
            switch (resetCmd) {
                case 101:
                    mLog(1, "HomeFragment.resetCMD.doInBackground sending mtkCmd(PMTK101, PMTK010,001)");
                    Main.mtkCmd("PMTK101", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnHot) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
                case 102:
                    mLog(1, "HomeFragment.resetCMD.doInBackground sending mtkCmd(PMTK102, PMTK010,001)");
                    Main.mtkCmd("PMTK102", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnWarm) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
                case 103:
                    mLog(1, "HomeFragment.resetCMD.doInBackground sending mtkCmd(PMTK103, PMTK010,001)");
                    Main.mtkCmd("PMTK103", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnCold) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
                case 104:
                    mLog(1, "HomeFragment.resetCMD.doInBackground sending mtkCmd(PMTK104, PMTK010,001)");
                    Main.mtkCmd("PMTK104", "PMTK010,001", cmdDelay);
                    msg = getString(R.string.btnFactory) + " " + getString(R.string.txtRSdone);
                    return resetCmd;
            }
            return resetCmd;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mLog(1, "HomeFragment.resetCmd.onPostExecute");
            startNMEA();
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            mLog(0, "" + msg);
            if (dialog.isShowing()) dialog.dismiss();
        }
    }//class resetCMD

    private class getRecCount extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private int dwnDelay;
        private int cmdRetry;
        private int retry;
        private String[] parms;
        private int logRecCount;

        protected void onPreExecute() {
            mLog(0, "GetLogFragment.getRecCount.onPreExecute");
            dwnDelay = Integer.parseInt(publicPrefs.getString("dwnDelay", "50"));
            dialog = new ProgressDialog(mContext);
            cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
            this.dialog.setMessage(getString(R.string.getSetngs));
            this.dialog.show();
            while (Main.NMEArunning) {
                Main.showNMEA = false;
                goSleep(50);
            }
        }//onPreExecute()

        protected Void doInBackground(Void... params) {
            String curFunc = "GetLogFragment.getRecCount.doInBackground";
            mLog(0, curFunc);
            retry = cmdRetry;
            while (retry > 0) {
                mLog(2, String.format("%1$s getting log record count (PMTK182,2,10) retry %2$d", curFunc, retry));
                parms = Main.mtkCmd("PMTK182,2,10", "PMTK182,3,10", dwnDelay);
                if (parms == null) {
                    retry--;
                    continue;
                }
                if (parms[0].contains("PMTK182") && parms[1].contains("3")) {
                    retry = 0;
                    logRecCount = Integer.parseInt(parms[3], 16);
                    mLog(0, String.format("%1$s Log has %2$d records", curFunc, logRecCount));
                } else {
                    retry--;
                    continue;
                }
            }
            ;
            return null;
        }//doInBackground()

        protected void onPostExecute(Void param) {
            mLog(0, "GetLogFragment.getRecCount.onPostExecute");

            String agps = appPrefs.getString("strAGPS", "");
            String logr = String.format(getString(R.string.logrecs), logRecCount);
            GPstats.setText(agps + NL + logr);
            appPrefEditor.putInt("logRecCount", logRecCount);
            appPrefEditor.putString("strLOGR", logr).commit();
            if (dialog.isShowing()) dialog.dismiss();
            startNMEA();
        }//onPostExecute()
    }//class getRecCount
}
