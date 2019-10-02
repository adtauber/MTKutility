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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.lang.reflect.Method;

import com.google.gson.Gson;

public class Main extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    //change veriable value to force a rebuild of the app preferences
    public static final String initSTART = "initStrt";

    private boolean OK;
    private final int REQUEST_WRITE_STORAGE = 0;
    private FragmentManager fragmentManager;
    NavigationView navigationView;
    private long back_pressed;
    private static String msg;
    private static String NL = System.getProperty("line.separator");
    public static final int ABORT = 9;
    private final byte[] binPMTK253 = new byte[]{(byte) 0x04, 0x24, 0x0E, 0x00, (byte) 0xFD, 0x00, 0x00, 0x00, (byte) 0xC2, 0x01, 0x00, 0x30, 0x0D, 0x0A};

    private ActionBar actionbar;
    private int activeFragment;
    private Menu nav_Menu;
    private Fragment fragment = null;

    public static SharedPreferences publicPrefs;
    public static SharedPreferences.Editor publicPrefEditor;
    public static SharedPreferences appPrefs;
    public static SharedPreferences.Editor appPrefEditor;
    public static Activity mContext;
    public static String errMsg;
    public static boolean aborting = false;
    public static boolean firstRun = true;
    public static boolean stopBkGrnd = false;
    public static OutputStreamWriter logWriter;
    private static StringBuilder readBuf = new StringBuilder();
    private static int rx = 0;

    private int screenWidth;
    private int screenHeight;
    private int screenDPI;

    private int btnsFont;
    private int htmlFont;
    private int AGPSsize;
    public static int debugLVL;
    private static int cmdDelay;
    private static int epoDelay;
    private static int downDelay;
    public static int cmdRetries;
    private boolean stopNMEA;
    public static boolean stopLOG = false;
    private int downBlockSize;
    public static String GPstatsTxt;
    public static String AGPSTxt;
    public static String LogTxt;

    private boolean hasWrite = false;
    public static boolean logFileIsOpen = false;
    private File basePath;
    private File logPath;
    private File logFile;
    private File errFile;
    private File binPath;
    private File epoPath;
    private File gpxPath;
    private String basePathName = "mtkutility";
    private String binPathName = "mtkutility/bin";
    private String gpxPathName = "mtkutility/gpx";
    private String epoPathName = "mtkutility/epo";
    private String logFileName = "MTKutilityLog.txt";
    private String errFileName = "MTKutilityErr.txt";
    private FileOutputStream lOut;

    public static BluetoothAdapter bluetoothAdapter = null;
    public static String GPSmac = null;
    public static BluetoothSocket GPSsocket = null;
    public static InputStream GPSin = null;
    public static OutputStream GPSout = null;

    public boolean isOK() {
        return OK;
    }

    //    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        //turn off screen rotation - force portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //set public and private preference handlers
        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();
        firstRun = appPrefs.getBoolean(initSTART, true);

        //set custom exception handler as default handler
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        }

        //get screen size for initial Log entries
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        screenDPI = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        setContentView(R.layout.main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionbar = getSupportActionBar();

        // create the navigation drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        nav_Menu = navigationView.getMenu();

        // set the toolbar title
        String title = getResources().getString(R.string.app_name) + " - " + getString(R.string.nav_home);
        actionbar.setTitle(title);

        //make sure phone has Bluetooth
        hasBluetooth();
        if (!aborting) {
            //is this the first app execute? - execute startup routine
//            Toast.makeText(mContext, String.format("initial run set %", firstRun), Toast.LENGTH_LONG).show();
            if (firstRun) initialRun();
            getSharedPreference();

            //check for file write permission - twice
            hasWrite = checkFileWritePermisssion();
            if (hasWrite) {
                //open activity Log file
                makeSureFoldersExist();
                if (!aborting) {
                    if (!openLog()) {
                        aborting = true;
                        errMsg = String.format(getString(R.string.logFatal), logFile);
                        Log(ABORT, errMsg);
                    }
                }
            } else {
                requestFileWritePermisssion();
            }
        }

        //set the startup screen from the navigation drawer
        fragmentManager = getSupportFragmentManager();
        Fragment fragment;
        Log(0, String.format("+++ firstRun is set %1$b +++", firstRun));
        if (firstRun) {
            fragment = new AboutFragment();
            activeFragment = R.id.nav_About;
        } else {
            fragment = new HomeFragment();
            activeFragment = R.id.nav_Home;
            nav_Menu.findItem(R.id.nav_Home).setVisible(true);
            nav_Menu.findItem(R.id.nav_MakeGPX).setVisible(true);
            nav_Menu.findItem(R.id.nav_GetEPO).setVisible(true);
            nav_Menu.findItem(R.id.nav_eMail).setVisible(true);
//            nav_Menu.findItem(R.id.nav_About).setVisible(true);
        }
        appPrefEditor.putInt("screenDPI", screenDPI);
        appPrefEditor.putInt("screenWidth", screenWidth);
        final int commit = fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
    }//onCreate()

    //    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log(0, "Main.onCreateOptionsMenu()");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }//onCreateOptionsMenu(Menu menu)

    //    @Override
    public void onBackPressed() {
        Log(0, "Main.onBackPressed()");
        if (firstRun || activeFragment == R.id.nav_Home) {
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                closeActivities();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                finish();
                super.onBackPressed();
            } else {
                Toast.makeText(mContext, getString(R.string.back1), Toast.LENGTH_LONG).show();
            }
        }
        if (activeFragment != R.id.nav_Home) {
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                stopBkGrnd = true;
                fragment = new HomeFragment();
                activeFragment = R.id.nav_Home;
                getSupportActionBar().setTitle("MTKutility - Home");
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
                back_pressed = 0;
                return;
            } else {
                Toast.makeText(mContext, getString(R.string.homeNav), Toast.LENGTH_SHORT).show();
            }
        }
        back_pressed = System.currentTimeMillis();
    }//onBackPressed()

    //    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log(0, "Main.onOptionsItemSelected()");
        Intent prefIntent = new Intent(this, PreferencesActivity.class);
        startActivity(prefIntent);
        return true;
    }//onOptionsItemSelected(MenuItem item)

    //    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log(0, "Main.onNavigationItemSelected()");
        // Handle navigation view item clicks here.
        Log(0, String.format("Main.onNavigationItemSelected() %s selected", item.getTitle()));
        int selected = item.getItemId();
        if (activeFragment == R.id.nav_Home) {
            stopBkGrnd = true;
            goSleep(500);
        }

        switch (selected) {
            case R.id.nav_Home:
                fragment = new HomeFragment();
                activeFragment = R.id.nav_Home;
                break;
            case R.id.nav_GetLog:
                fragment = new GetLogFragment();
                activeFragment = R.id.nav_GetLog;
                break;
            case R.id.nav_clrLog:
                fragment = new ClrLogFragment();
                activeFragment = R.id.nav_clrLog;
                break;
            case R.id.nav_MakeGPX:
                fragment = new MakeGPXFragment();
                activeFragment = R.id.nav_MakeGPX;
                break;
            case R.id.nav_GetEPO:
                fragment = new GetEPOFragment();
                activeFragment = R.id.nav_GetEPO;
                break;
            case R.id.nav_CheckEPO:
                fragment = new CheckEPOFragment();
                activeFragment = R.id.nav_CheckEPO;
                break;
            case R.id.nav_UpdtAGPS:
                fragment = new UpdtAGPSFragment();
                activeFragment = R.id.nav_UpdtAGPS;
                break;
            case R.id.nav_Settings:
                fragment = new SettingsFragment();
                activeFragment = R.id.nav_Settings;
                break;
            case R.id.nav_eMail:
                fragment = new eMailFragment();
                activeFragment = R.id.nav_eMail;
                break;
            case R.id.nav_Help:
                fragment = new HelpFragment();
                activeFragment = R.id.nav_Help;
                break;
            case R.id.nav_About:
                fragment = new AboutFragment();
                activeFragment = R.id.nav_About;
                break;
            case R.id.nav_Exit:
                if (firstRun || activeFragment == R.id.nav_Home) {
                    closeActivities();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    finish();
                } else {
                    Toast.makeText(mContext, getString(R.string.pleaseNav), Toast.LENGTH_LONG).show();
                }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        drawer.closeDrawer(GravityCompat.START);
        if (fragment != null) {
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            // set the toolbar title
            if (getSupportActionBar() != null) {
                String title = getResources().getString(R.string.app_name) + " - " + item.toString();
                getSupportActionBar().setTitle(title);
            }
        }

        return true;
    }//onNavigationItemSelected(MenuItem item)

    //    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log(0, "Main.onRequestPermissionsResult()");
        // The result of the popup opened with the requestPermissions() method
        // is in that method, you need to check that your application comes here
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasWrite = true;
                boolean ok = openLog();
                if (!ok) {
                    errMsg = getString(R.string.logFatal);
                    aborting = true;
                }
            } else {
                errMsg = "write permission has not been granted";
                aborting = true;
            }
        }
    }//onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)

    private void askForEmail() {
        Log(0, "Main.askForEmail()");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        // set dialog message
        alertDialogBuilder.setMessage(mContext.getString(R.string.crashLog)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Todo
                sendEmail(1);
            }
        });
        //show alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    } //askForEmail()

    protected static void buildCrashReport(Throwable ex) {
        String str = Log.getStackTraceString(ex);
        Log(0, "Main.buildCrashReport()");
        appPrefEditor.putBoolean("appFailed", true);
        appPrefEditor.commit();
        closeActivities();
        Log(0, "Main.buildCrashReport()");
        Log(0, "********** Stack **********");
        Log(999, str);
        Log(0, "****** End of Stack ******");
        //Todo
//        logClose();

        //create restart intent
        Intent intent = new Intent(mContext, Main.class);
        mContext.startActivity(intent);

        // make sure we die, otherwise the app will hang ...
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(2);
    }//buildCrashReport()

    private boolean checkFileWritePermisssion() {
        boolean hasWrite = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        return hasWrite;
    }//checkFileWritePermisssion()

    private static void closeActivities() {
        Log(0, "Main.closeActivities()");
        //stop NMEA AsyncTask
        stopBkGrnd = true;
        goSleep(3000);
        // close navigation drawer
        DrawerLayout drawer = mContext.findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            Log(1, "Main.closeActivities() closing navigation drawer");
            drawer.closeDrawer(GravityCompat.START);
        }
        //close logger input/output and disconnectGPS
        if (!(Main.GPSsocket == null) && GPSsocket.isConnected()) {
            Log(1, "Main.closeActivities() disconnecting GPS");
            disconnect();
        }
    }//closeActivities()

    private void getSharedPreference() {
        String curFunc = "Main.getSharedPreference()";

        btnsFont = Integer.parseInt(publicPrefs.getString("btnsFont", "12"));
        htmlFont = Integer.parseInt(publicPrefs.getString("htmlFont", "15"));
        AGPSsize = Integer.parseInt(publicPrefs.getString("AGPSsize", "7"));
        debugLVL = Integer.parseInt(publicPrefs.getString("debugPref", "0"));

        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        epoDelay = Integer.parseInt(publicPrefs.getString("epoDelay", "150"));
        downDelay = Integer.parseInt(publicPrefs.getString("downDelay", "50"));
//        cmdTimeOut = Integer.parseInt(publicPrefs.getString("cmdTimeOut", "30"));
        cmdRetries = Integer.parseInt(publicPrefs.getString("cmdRetries", "5"));
        stopNMEA = publicPrefs.getBoolean("stopNMEA", true);
        stopLOG = publicPrefs.getBoolean("stopLOG", false);
        downBlockSize = Integer.parseInt(publicPrefs.getString("downBlockSize", "2048"));
        Log(0, String.format("%1$s --- debugLVL=%2$d cmdRetries=%3$d", curFunc, debugLVL, cmdRetries));
        Log(0, String.format("%1$s --- AGPSsize=%2$d stopNMEA=%3$b stopLOG=%4$b", curFunc, AGPSsize, stopNMEA, stopLOG));
    }//getSharedPreference()

    private static void goSleep(int mSec) {
        Log(3, String.format("Main.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            buildCrashReport(e);
        }
    }//goSleep()

    private void hasBluetooth() {
        final int REQUEST_ENABLE_BT = 88;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
//            Log(VB0, "Main.hasBluetooth() phone does not support Bluetooth");
            errMsg = getString(R.string.noBluetooth);
            Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
            aborting = true;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                goSleep(3000);
            }
        }
    }//hasBluetooth()

    private void initialRun() {
        appPrefs.edit().clear().commit();
        publicPrefs.edit().clear().commit();
        goSleep(500);

        //build FTP site array
        ArrayList<urlModel> sitesList = new ArrayList<>();
        String[] F60 = getResources().getStringArray(R.array.F60);
        sitesList.add(new urlModel(F60[0], F60[1], F60[2], F60[3]));
        String[] F72 = getResources().getStringArray(R.array.F72);
        sitesList.add(new urlModel(F72[0], F72[1], F72[2], F72[3]));

        //store FTP site array in private preferences
        Gson gson = new Gson();
        String json = gson.toJson(sitesList);
        appPrefEditor.putString("urlKey", json);
        //store file info for other fragments
        appPrefEditor.putString("basePathName", basePathName);
        appPrefEditor.putString("binPathName", binPathName);
        appPrefEditor.putString("gpxPathName", gpxPathName);
        appPrefEditor.putString("epoPathName", epoPathName);
        appPrefEditor.putString("logFileName", logFileName);
        appPrefEditor.putString("errFileName", errFileName);

        appPrefEditor.putBoolean("appFailed", false);
        appPrefEditor.putBoolean(initSTART, false);
        appPrefEditor.commit();

        //store defaults for public preferences
        publicPrefEditor.putString("homeFont", "13");
        publicPrefEditor.putString("setBtns", "12");
        publicPrefEditor.putString("setHtml", "15");
        publicPrefEditor.putString("AGPSsize", "7");
        publicPrefEditor.putString("debugPref", "0");
        publicPrefEditor.putString("cmdDelay", "25");
        publicPrefEditor.putString("cmdRetries", "5");
        publicPrefEditor.putString("epoDelay", "150");
        publicPrefEditor.putBoolean("stopNMEA", true);
        publicPrefEditor.putBoolean("stopLOG", false);
        publicPrefEditor.putString("downBlockSize", "2048");
        publicPrefEditor.putString("downDelay", "50");
        publicPrefEditor.commit();
        goSleep(500);
    }//initialRun()

    protected static void Log(int mode, String msg) {
        if (!logFileIsOpen) {
            return;
        }
        if (mode < 999) {
            if (msg.length() > 127) {
                msg = msg.substring(0, 60) + " ... " + msg.substring(msg.length() - 30);
            }
        }
        switch (mode) {
            case 0:
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
            buildCrashReport(e);
        }
    }//Log()

    private void logPhoneInfo() {
        Log(0, "************ DEVICE INFO ************");
        Log(0, String.format("Brand:  %s", Build.BRAND));
        Log(0, String.format("Device: %s", Build.DEVICE));
        Log(0, String.format("Model:  %s", Build.MODEL));
        Log(0, String.format("ID:     %s", Build.ID));
        Log(0, String.format("Product: %s", Build.PRODUCT));
        Log(0, String.format("Screen DPI: %d", screenDPI));
        Log(0, String.format("width: %d", screenWidth));
        Log(0, String.format("height: %d", screenHeight));
        Log(0, String.format("************ FIRMWARE ************"));
        Log(0, String.format("Android Version: %s", Build.VERSION.RELEASE));
        Log(0, String.format("Android Increment: %s", Build.VERSION.INCREMENTAL));
        Log(0, String.format("Board: %s", Build.BOARD));
        Log(0, "**********************************");
    }//logPhoneInfo()

    private void makeSureFoldersExist() {
        //create app fo;ders in Downlaod folder
        OK = true;
        basePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), basePathName);
        if (!basePath.exists()) OK = basePath.mkdir();
        if (!OK) {
            errMsg = String.format(getString(R.string.makeFolderErr), basePathName);
            Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
            aborting = true;
            return;
        }

        gpxPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), gpxPathName);
        if (!gpxPath.exists())
//            OK = false; //use for testing
            OK = gpxPath.mkdir();
        if (!OK) {
            //makeFolderErr
            errMsg = String.format(getString(R.string.makeFolderErr), gpxPathName);
            Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
            aborting = true;
            return;
        }

        binPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), binPathName);
        if (!binPath.exists()) OK = binPath.mkdir();
        if (!OK) {
            errMsg = String.format(getString(R.string.makeFolderErr), binPathName);
            Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
            aborting = true;
            return;
        }

        epoPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), epoPathName);
        // make sure mtkutility/bin directory exists - create if it is missing
        if (!epoPath.exists()) OK = epoPath.mkdir();
        if (!OK) {
            errMsg = String.format(getString(R.string.makeFolderErr), epoPathName);
            Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
            aborting = true;
            return;
        }
    }//makeSureFoldersExist

    private boolean openLog() {
        // Create Log file objects for the the email method
        OK = true;
        logFileIsOpen = false;
        logPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), basePathName);
        // make sure mtkutility directory exists - create if it is missing
        if (!logPath.exists()) {
            OK = logPath.mkdirs();
        }
        if (!OK) {
            return false;
        }

        logFile = new File(logPath, logFileName);
        errFile = new File(logPath, errFileName);
        //get rid of error log fromprevious run
        if (errFile.exists()) {
            errFile.delete();
        }
        // did previous app execute fail - ask user to send an email
        OK = appPrefs.getBoolean("appFailed", false);
        if (OK) {
            if (logFile.exists()) {
                // rename Log file to preserve error Log for email
                logFile.renameTo(errFile);
                askForEmail();
                appPrefEditor.putBoolean("appFailed", false);
                appPrefEditor.commit();
            }
        }

        if (logFile.exists()) {
            logFile.delete();
        }

        try {
            logFile.createNewFile();
            lOut = new FileOutputStream(logFile);
            logFileIsOpen = true;
        } catch (IOException e) {
            buildCrashReport(e);
        }

//        logFileIsOpen = false; //use to test Log file open failure
        if (logFileIsOpen) {
            PackageInfo pinfo = null;
            int vNum = 0;
            String vNam = "";
            try {
                pinfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                vNum = pinfo.versionCode;
                vNam = pinfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            logWriter = new OutputStreamWriter(lOut);
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            Log(0, "Log file opened " + currentDateTimeString + NL);
            Log(0, String.format("MTKutility version:%1$d  version name:%2$s debugLvl:%3$s", vNum, vNam, debugLVL));
            logPhoneInfo();
            return true;
        }
        return false;
    }//openLog()

    private void requestFileWritePermisssion() {
        if (!hasWrite) {
            // ask the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            goSleep(5000);
        }
    }//requestFileWritePermisssion()

    public void createException(int Int) {
        // call with Int = 0
        int res = 1 / Int;
    }//testException()

    public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
        private Activity mContext;

        public CustomExceptionHandler(Activity context) {
            mContext = context;
        }

        public void uncaughtException(Thread t, Throwable ex) {
            OK = true;
            if (!logFileIsOpen) {
                OK = openLog();
            }
            if (OK) {
                Log(0, String.format("%s**** CustomExceptionHandler.uncaughtException()%s", NL, NL));
                buildCrashReport(ex);
            }
        }

    }//class CustomExceptionHandler

    //*************************************** bluetooth routines follow ********************************
    protected static boolean connect() {
        Log(0, "Main.connect()");
        if (GPSmac == null) {
            Main.errMsg = mContext.getString(R.string.noGPSselected);
            Main.aborting = true;
            return false;
        }
        Method m = null;
        String methodName;
        BluetoothDevice GPSdevice = null;

        boolean allowInsecure = appPrefs.getBoolean("allowInsecure", false);
        GPSdevice = bluetoothAdapter.getRemoteDevice(GPSmac);

        if (allowInsecure) {
            methodName = "createInsecureRfcommSocket";
        } else {
            methodName = "createRfcommSocket";
        }
        try {
            m = GPSdevice.getClass().getMethod(methodName, int.class);
        } catch (SecurityException e) {
            buildCrashReport(e);
        } catch (NoSuchMethodException e) {
            buildCrashReport(e);
        }

        try {
            GPSsocket = (BluetoothSocket) m.invoke(GPSdevice, Integer.valueOf(1));
        } catch (IllegalArgumentException e) {
            buildCrashReport(e);
        } catch (IllegalAccessException e) {
            buildCrashReport(e);
        } catch (InvocationTargetException e) {
            buildCrashReport(e);
        }

        try {
            GPSsocket.connect();
        } catch (IOException e) {
            return false;
        }

        try {
            GPSin = GPSsocket.getInputStream();
        } catch (IOException e) {
            buildCrashReport(e);
        }

        try {
            GPSout = GPSsocket.getOutputStream();
        } catch (IOException e) {
            buildCrashReport(e);
        }
        return GPSsocket.isConnected();
    }//connect()

    protected static boolean disconnect() {
        Log(0, "Main.disconnect()");
        //stop Async tasks
        stopLOG = true;
        goSleep(3000);

        try {
            Log(2, "Main.disconnect() executing GPSout.flush()");
            GPSout.flush();
        } catch (IOException e) {
            buildCrashReport(e);
            return false;
        }
        try {
            Log(2, "Main.disconnect() executing GPSout.close()");
            GPSout.close();
        } catch (IOException e) {
            buildCrashReport(e);
            return false;
        }
        try {
            Log(2, "Main.disconnect() executing GPSsocket.close()");
            GPSsocket.close();
        } catch (IOException e) {
            buildCrashReport(e);
            return false;
        }
        Log(0, "Main.disconnect() GPS is disconnected *****");
        return true;
    }//disconnect()

    protected static String[] mtkCmd(String mtkcmd, String mtkreply, int delay) {
        for (int i = 0; i <= cmdRetries; i++) {
            Log(1, "Main.mtkCmd(" + mtkcmd + "|" + mtkreply + "|" + delay + ")");
            String[] sArray = new String[99];
            if (sendCommand(mtkcmd)) {
                goSleep(delay);
                sArray = waitForReply(mtkreply, delay);
                return sArray;
            }
        }
        return null;
    }//mtkCmd()

    protected static boolean sendCommand(String command) {
        Log(1, "Main.sendCommand()");
        byte checksum = calculateChecksum(command);
        StringBuilder rec = new StringBuilder(256);
        rec.setLength(0);
        rec.append('$');
        rec.append(command);
        rec.append('*');
        rec.append(String.format("%02X", checksum));
        rec.append("\r\n");
        Log(1, "Main.sendCommand() sending: " + rec.toString().substring(0, rec.length() - 2));
        //clear the GPS send buffer
//        readString(cmdDelay);
        try {
            GPSout.write(rec.toString().getBytes());
        } catch (Exception e) {
            Log(0, String.format("Main.sendCommand() %s failed-", rec.toString()));
            buildCrashReport(e);
            return false;
        }
        return true;
    }//sendCommand()

    private static byte calculateChecksum(String command) {
        int startPoint = command.indexOf("$") + 1;
        int endPoint = command.indexOf("*", startPoint);
        if (endPoint == -1) {
            endPoint = command.length();
        }
        byte checksum = 0;
        for (int i = startPoint; i < endPoint; i++) {
            checksum ^= (byte) command.charAt(i);
        }
        return (checksum);
    }//calculateChecksum{}

    public void sendEmail(int idx) {
        Log(0, "myLibrary.sendEmail()");
        switch (idx) {
            case 0:
                if (!logFile.exists() || !logFile.canRead()) return;
                break;
            case 1:
                if (!errFile.exists() || !errFile.canRead()) return;
                break;
        }
        //create lookup table for email body text
        final int[] LOOKUP_TABLE = new int[]{R.string.emailtext, R.string.errortext};
        Uri uri;

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mContext.getString(R.string.myEmail)});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "mtkutility log file");
        emailIntent.putExtra(Intent.EXTRA_TEXT, mContext.getString(LOOKUP_TABLE[idx]));
        if (idx == 0) {
            uri = FileProvider.getUriForFile(mContext,
                    mContext.getApplicationContext().getPackageName() + ".provider", logFile);
        } else {
            uri = FileProvider.getUriForFile(mContext,
                    mContext.getApplicationContext().getPackageName() + ".provider", errFile);
        }
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        mContext.startActivity(Intent.createChooser(emailIntent, mContext.getResources().getString(R.string.emailMsg)));
    }//sendEmail()

    protected static String[] waitForReply(String mtkReply, int delay) {
        String curFunc = "Main.waitForReply()";
        Log(0, String.format("%1$s waiting for:%2$s", curFunc, mtkReply));
        String[] sArray = new String[99];
        boolean doAppend = false;
        int retry = cmdRetries;
        char b = '\0';

        rx = 0;
        readBuf = new StringBuilder();
        readBuf.append(readString(delay));
        StringBuilder sndBuf = new StringBuilder();
        if (readBuf.length() > 0) {
            do {
                b = readBuf.charAt(rx);
                if (b == '$') {
                    doAppend = true;
                    Log(2, String.format("%1$s $ found at %2$d of %3$d", curFunc, rx, readBuf.length()));
                }
                if (b == '*' && doAppend) {
                    doAppend = false;
                    Log(2, String.format("%1$s * found at index %2$d, sndBuf length=%3$d", curFunc, rx, sndBuf.length()));
                    //skip checksum and CR LF in replay
                    rx += 3;
                    //convert to a string array dropping beginng $ symbol
                    String reply = sndBuf.toString().substring(1);
                    sndBuf = new StringBuilder();
//                    reply = reply.substring(1);
                    Log(2, String.format("%1$s received: %2$s", curFunc, reply));
                    sArray = reply.split(",");
                    if (reply.contains(mtkReply)) return sArray;
                    //did we get the reply string or a PMTK001 reply - return all failure replies
                    // note: GPS normally returns the requested reply before the PMTK001
                    // when the command sent succeeds
                    if (reply.contains("PMTK001") && !sArray[sArray.length - 1].contains("3"))
                        return sArray;
                }
                if (doAppend) {
                    sndBuf.append(b);
                }

                rx++;
                if (rx > readBuf.length() - 1) {
                    readBuf = new StringBuilder();
                    readBuf.append(readString(delay));
                    rx = 0;
                    if (readBuf.length() < 1) retry = 0;
                }
            } while (retry > 0);
        }
        Log(0, String.format("%1$s failed - returning null sArray *****", curFunc));
        //return empty PMTK001 to signify timeout
        sArray = null;
        return sArray;
    }//waitForReply()

    protected static void sendBytes(byte[] byteArray) {
        Log(2, "Main.sendBytes");
        try {
            GPSout.write(byteArray);
        } catch (Exception e) {
            Log(0, "myLibrary.sendBytes() failed ");
            buildCrashReport(e);
        }
    }//sendBytes()

    protected static String readString(int delay) {
        Log(2, String.format("Main.readString(%1$d)", delay));
        char b = '\0';
        byte[] bytBuf = null;
        int retry = cmdRetries;
        do {
            bytBuf = readBytes(delay);
            if (bytBuf == null) {
                retry--;
                continue;
            } else {
                retry = 0;
            }
        } while (retry > 0);
        StringBuilder newBuf = new StringBuilder();
        if (bytBuf != null) {
            for (int j = 0; j < bytBuf.length; j++) {
                b = (char) (bytBuf[j] & 0xff);
                newBuf.append(b);
            }
        }
        return newBuf.toString();
    }//readString()

    protected static byte[] readBytes(int delay) {
        Log(2, String.format("Main.readBytes(%1$d)", delay));
        int bytes_available = 0;
        int retry = cmdRetries;
        byte[] buf = null;

        while (bytes_available == 0 && retry > 0) {
            Log(2, String.format("Main.readBytes retry:%1$d  delay:%2$d", retry, delay));
            retry--;
            try {
                bytes_available = GPSin.available();
            } catch (IOException e) {
                buildCrashReport(e);
            }
            goSleep(delay);
        }

        Log(2, String.format("Main.readBytes done: %1$d bytes available", bytes_available));
        if (bytes_available > 0) {
            buf = new byte[bytes_available];
            try {
                GPSin.read(buf);
            } catch (IOException e) {
                buildCrashReport(e);
            }
        }
        return buf;
    }//readBytes()
}
