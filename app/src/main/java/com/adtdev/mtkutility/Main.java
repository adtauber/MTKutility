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
import android.os.Handler;
import android.os.Message;
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
import java.util.Arrays;
import java.util.Date;
import java.lang.reflect.Method;
import java.util.List;

import com.google.gson.Gson;

public class Main extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DrawerLocker {
    //change veriable value to force a rebuild of the app preferences
    public static final String initSTART = "version40";
    DrawerLayout drawer;
    ActionBarDrawerToggle toggle;

    private ProgressDialog dialog;
    private static boolean OK;
    private final int REQUEST_WRITE_STORAGE = 0;
    private FragmentManager fragmentManager;
    NavigationView navigationView;
    private long back_pressed;
    private static String msg;
    private static String NL = System.getProperty("line.separator");
    public static final int ABORT = 9;
    private final byte[] binPMTK253 = new byte[]{(byte) 0x04, 0x24, 0x0E, 0x00, (byte) 0xFD, 0x00, 0x00, 0x00, (byte) 0xC2, 0x01, 0x00, 0x30, 0x0D, 0x0A};
    private static final List<Integer> backOK = Arrays.asList(R.id.nav_About,R.id.nav_Help,R.id.nav_Home,R.id.nav_eMail);

    private ActionBar actionbar;
    private int activeFragment;
    private Menu nav_Menu;
    private Fragment fragment = null;
    private MenuItem menuItem;
    private int itemSelected;

    public static SharedPreferences publicPrefs;
    public static SharedPreferences.Editor publicPrefEditor;
    public static SharedPreferences appPrefs;
    public static SharedPreferences.Editor appPrefEditor;
    public static Activity mContext;
    public static String errMsg;
    public static boolean aborting = false;
    public static boolean firstRun = true;
    public static boolean showNMEA = true;
    public static boolean NMEArunning = false;
    public static OutputStreamWriter logWriter;
    private static StringBuilder readBuf = new StringBuilder();
    private static int rx = 0;

    private int screenWidth;
    private int screenHeight;
    private int screenDPI;

    private static int homeFont;
    private static int btnsFont;
    private static int htmlFont;
    private static int AGPSsize;
    public static int debugLVL;
    private static int cmdDelay;
    private static int epoDelay;
    private static int dwnDelay;
    private static int cmdRetry;
    private static int retryInc;
    private static boolean stopNMEA;
    public static boolean stopLOG = false;
    private static int downBlockSize;
    private int logRecCount;

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
        drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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
            if (firstRun) initialRun();

            //check for file write permission - twice
            hasWrite = checkFileWritePermisssion();
            if (hasWrite) {
                //open activity Log file
                makeSureFoldersExist();
                if (!aborting) {
                    if (!openLog()) {
                        aborting = true;
                        errMsg = String.format(getString(R.string.logFatal), logFile);
                        mLog(ABORT, errMsg);
                    }
                }
                getSharedPreference();
            } else {
                requestFileWritePermisssion();
            }
        }

        //set the startup screen from the navigation drawer
        fragmentManager = getSupportFragmentManager();
        Fragment fragment;
        mLog(0, String.format("+++ firstRun is set %1$b +++", firstRun));
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

    public boolean onCreateOptionsMenu(Menu menu) {
        mLog(0, "Main.onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }//onCreateOptionsMenu(Menu menu)

    public void onBackPressed() {
        mLog(0, "Main.onBackPressed");
//        if (firstRun || activeFragment == R.id.nav_Home) {
        if (firstRun || backOK.contains(activeFragment)) {
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                closeActivities();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//                finish();
                this.finishAffinity();
                super.onBackPressed();
            } else
                Toast.makeText(mContext, getString(R.string.back1), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, getString(R.string.pleaseNav), Toast.LENGTH_LONG).show();
        }
        back_pressed = System.currentTimeMillis();
    }//onBackPressed()

    public boolean onOptionsItemSelected(MenuItem item) {
        mLog(0, "Main.onOptionsItemSelected");
        Intent prefIntent = new Intent(this, PreferencesActivity.class);
        startActivity(prefIntent);
        return true;
    }//onOptionsItemSelected(MenuItem item)

    public boolean onNavigationItemSelected(MenuItem item) {
        mLog(0, "Main.onNavigationItemSelected");
        // Handle navigation view item clicks here.
        mLog(0, String.format("Main.onNavigationItemSelected %s selected *+*+*+*", item.getTitle()));
        menuItem = item;
        itemSelected = item.getItemId();

        switch (itemSelected) {
            case R.id.nav_Home:
                fragment = new HomeFragment();
                activeFragment = R.id.nav_Home;
                changeFragment(fragment);
                break;
            case R.id.nav_GetLog:
                fragment = new GetLogFragment();
                itemSelected = R.id.nav_GetLog;
                new getRecCount().execute();
                break;
            case R.id.nav_clrLog:
                fragment = new ClrLogFragment();
                itemSelected = R.id.nav_clrLog;
                new getRecCount().execute();
                break;
            case R.id.nav_MakeGPX:
                fragment = new MakeGPXFragment();
                activeFragment = R.id.nav_MakeGPX;
                changeFragment(fragment);
                break;
            case R.id.nav_GetEPO:
                fragment = new GetEPOFragment();
                activeFragment = R.id.nav_GetEPO;
                changeFragment(fragment);
                break;
            case R.id.nav_CheckEPO:
                fragment = new CheckEPOFragment();
                activeFragment = R.id.nav_CheckEPO;
                changeFragment(fragment);
                break;
            case R.id.nav_UpdtAGPS:
                fragment = new UpdtAGPSFragment();
                activeFragment = R.id.nav_UpdtAGPS;
                changeFragment(fragment);
                break;
            case R.id.nav_Settings:
                fragment = new SettingsFragment();
                activeFragment = R.id.nav_Settings;
                changeFragment(fragment);
                break;
            case R.id.nav_eMail:
                fragment = new eMailFragment();
                activeFragment = R.id.nav_eMail;
                changeFragment(fragment);
                break;
            case R.id.nav_Help:
                fragment = new HelpFragment();
                activeFragment = R.id.nav_Help;
                changeFragment(fragment);
                break;
            case R.id.nav_About:
                fragment = new AboutFragment();
                activeFragment = R.id.nav_About;
                changeFragment(fragment);
                break;
            case R.id.nav_Exit:
                if (firstRun || activeFragment == R.id.nav_Home) {
                    closeActivities();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    this.finishAffinity();
                } else {
                    Toast.makeText(mContext, getString(R.string.pleaseNav), Toast.LENGTH_LONG).show();
                }
        }
        return true;
    }//onNavigationItemSelected(MenuItem item)


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLog(0, "Main.onRequestPermissionsResult");
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
    }//onRequestPermissionsResult

    private void askForEmail() {
        mLog(0, "Main.askForEmail");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        // set dialog message
        alertDialogBuilder.setMessage(mContext.getString(R.string.crashLog)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sendEmail(1);
            }
        });
        //show alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    } //askForEmail()

    protected static void buildCrashReport(Throwable ex) {
        String str = Log.getStackTraceString(ex);
        mLog(0, "Main.buildCrashReport");
        appPrefEditor.putBoolean("appFailed", true);
        appPrefEditor.commit();
        closeActivities();
        mLog(0, "Main.buildCrashReport()");
        mLog(0, "********** Stack **********");
        mLog(999, str);
        mLog(0, "****** End of Stack ******");
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

    private void changeFragment(Fragment fragment) {
        //wait for background task to complete
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        while (NMEArunning) {
            showNMEA = false;
            goSleep(50);
        }
        mLog(2, String.format("Main.changeFragment NMEArunning=", NMEArunning));
        // set the toolbar title
        if (getSupportActionBar() != null) {
            String title = getResources().getString(R.string.app_name) + " - " + menuItem.toString();
            getSupportActionBar().setTitle(title);
        }
        //open new fragment
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }//changeFragment()

    private static void closeActivities() {
        mLog(0, "Main.closeActivities");
        while (NMEArunning) {
            showNMEA = false;
            goSleep(50);
        }
        // close navigation drawer
        DrawerLayout drawer = mContext.findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            mLog(1, "Main.closeActivities closing navigation drawer");
            drawer.closeDrawer(GravityCompat.START);
        }
        //close logger input/output and disconnectGPS
        if (!(GPSsocket == null) && GPSsocket.isConnected()) {
            mLog(1, "Main.closeActivities disconnecting GPS");
            disconnect();
        }
    }//closeActivities()

    public static void getSharedPreference() {
        String curFunc = "Main.getSharedPreference";
        mLog(0, curFunc);
        mLog(0, "********** app preferences **********");
        homeFont = Integer.parseInt(publicPrefs.getString("homeFont", "12"));
        mLog(0, String.format("homeFont = %1$s", homeFont));
        btnsFont = Integer.parseInt(publicPrefs.getString("btnsFont", "12"));
        mLog(0, String.format("btnsFont = %1$s", btnsFont));
        htmlFont = Integer.parseInt(publicPrefs.getString("htmlFont", "15"));
        mLog(0, String.format("htmlFont = %1$s", htmlFont));
        AGPSsize = Integer.parseInt(publicPrefs.getString("AGPSsize", "7"));
        mLog(0, String.format("AGPSsize = %1$s", AGPSsize));
        debugLVL = Integer.parseInt(publicPrefs.getString("debugLVL", "0"));
        mLog(0, String.format("debugLVL = %1$s", debugLVL));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        mLog(0, String.format("cmdDelay = %1$s", cmdDelay));
        epoDelay = Integer.parseInt(publicPrefs.getString("epoDelay", "150"));
        mLog(0, String.format("epoDelay = %1$s", epoDelay));
        dwnDelay = Integer.parseInt(publicPrefs.getString("dwnDelay", "50"));
        mLog(0, String.format("dwnDelay = %1$s", dwnDelay));
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        mLog(0, String.format("cmdRetry = %1$s", cmdRetry));
        retryInc = Integer.parseInt(publicPrefs.getString("retryInc", "1"));
        mLog(0, String.format("retryInc = %1$s", retryInc));
        stopNMEA = publicPrefs.getBoolean("stopNMEA", true);
        mLog(0, String.format("stopNMEA = %1$s", stopNMEA));
        stopLOG = publicPrefs.getBoolean("stopLOG", false);
        mLog(0, String.format("stopLOG = %1$s", stopLOG));
        downBlockSize = Integer.parseInt(publicPrefs.getString("downBlockSize", "2048"));
        mLog(0, String.format("downBlockSize = %1$s", downBlockSize));
        mLog(0, "*************************************");
    }//getSharedPreference()

    private static void goSleep(int mSec) {
        mLog(3, String.format("Main.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            buildCrashReport(e);
        }
    }//goSleep()

    private void hasBluetooth() {
        String curFunc = "Main.hasBluetooth";
        mLog(2, curFunc);
        final int REQUEST_ENABLE_BT = 88;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
//            mLog(VB0, "Main.hasBluetooth() phone does not support Bluetooth");
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
        String curFunc = "Main.initialRun";
        mLog(0, curFunc);
        //get the existing preferences - set defaults when they do not exist
        homeFont = Integer.parseInt(publicPrefs.getString("homeFont", "12"));
        btnsFont = Integer.parseInt(publicPrefs.getString("btnsFont", "12"));
        htmlFont = Integer.parseInt(publicPrefs.getString("htmlFont", "15"));
        AGPSsize = Integer.parseInt(publicPrefs.getString("AGPSsize", "7"));
        debugLVL = Integer.parseInt(publicPrefs.getString("debugLVL", "0"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        epoDelay = Integer.parseInt(publicPrefs.getString("epoDelay", "150"));
        dwnDelay = Integer.parseInt(publicPrefs.getString("dwnDelay", "50"));
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        retryInc = Integer.parseInt(publicPrefs.getString("retryInc", "0"));
        stopNMEA = publicPrefs.getBoolean("stopNMEA", true);
        stopLOG = publicPrefs.getBoolean("stopLOG", false);
        downBlockSize = Integer.parseInt(publicPrefs.getString("downBlockSize", "2048"));
        GPSmac = appPrefs.getString("GPSmac", "");

        //clear both the app and private preferences
        appPrefs.edit().clear().commit();
        publicPrefs.edit().clear().commit();
        goSleep(500);

        //set control preferences
        appPrefEditor.putBoolean("appFailed", false);
        appPrefEditor.putBoolean(initSTART, false);
        appPrefEditor.putInt("DLcmd", 0);

        //store file info for fragments
        appPrefEditor.putString("basePathName", basePathName);
        appPrefEditor.putString("binPathName", binPathName);
        appPrefEditor.putString("gpxPathName", gpxPathName);
        appPrefEditor.putString("epoPathName", epoPathName);
        appPrefEditor.putString("logFileName", logFileName);
        appPrefEditor.putString("errFileName", errFileName);
        appPrefEditor.putString("GPSmac", Main.GPSmac);

        //build FTP site array
        ArrayList<urlModel> sitesList = new ArrayList<>();
        String[] Q60 = getResources().getStringArray(R.array.Q60);
        sitesList.add(new urlModel(Q60[0], Q60[1], Q60[2], Q60[3], Q60[4]));
        String[] K60 = getResources().getStringArray(R.array.K60);
        sitesList.add(new urlModel(K60[0], K60[1], K60[2], K60[3], K60[4]));
        String[] F72 = getResources().getStringArray(R.array.F72);
        sitesList.add(new urlModel(F72[0], F72[1], F72[2], F72[3], F72[4]));
        //store FTP site array in private preferences
        Gson gson = new Gson();
        String json = gson.toJson(sitesList);
        appPrefEditor.putString("urlKey", json);

        appPrefEditor.commit();

        //store defaults for public preferences
        publicPrefEditor.putString("homeFont", String.valueOf(homeFont));
        publicPrefEditor.putString("btnsFont", String.valueOf(btnsFont));
        publicPrefEditor.putString("htmlFont", String.valueOf(htmlFont));
        publicPrefEditor.putString("AGPSsize", String.valueOf(AGPSsize));
        publicPrefEditor.putString("debugLVL", String.valueOf(debugLVL));
        publicPrefEditor.putString("cmdDelay", String.valueOf(cmdDelay));
        publicPrefEditor.putString("epoDelay", String.valueOf(epoDelay));
        publicPrefEditor.putString("dwnDelay", String.valueOf(dwnDelay));
        publicPrefEditor.putString("cmdRetry", String.valueOf(cmdRetry));
        publicPrefEditor.putString("retryInc", String.valueOf(retryInc));
        publicPrefEditor.putBoolean("stopNMEA", stopNMEA);
        publicPrefEditor.putBoolean("stopLOG", stopLOG);
        publicPrefEditor.putString("downBlockSize", String.valueOf(downBlockSize));
        publicPrefEditor.commit();
        goSleep(500);
    }//initialRun()

    protected static void mLog(int mode, String msg) {
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
    }//mLog()

    private void logPhoneInfo() {
        mLog(0, "************ DEVICE INFO ************");
        mLog(0, String.format("Brand:  %s", Build.BRAND));
        mLog(0, String.format("Device: %s", Build.DEVICE));
        mLog(0, String.format("Model:  %s", Build.MODEL));
        mLog(0, String.format("ID:     %s", Build.ID));
        mLog(0, String.format("Product: %s", Build.PRODUCT));
        mLog(0, String.format("Screen DPI: %d", screenDPI));
        mLog(0, String.format("width: %d", screenWidth));
        mLog(0, String.format("height: %d", screenHeight));
        mLog(0, String.format("************ FIRMWARE ************"));
        mLog(0, String.format("Android Version: %s", Build.VERSION.RELEASE));
        mLog(0, String.format("Android Increment: %s", Build.VERSION.INCREMENTAL));
        mLog(0, String.format("Board: %s", Build.BOARD));
        mLog(0, "**********************************");
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
        String curFunc = "Main.openLog";
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
            mLog(0, String.format("%1$s Log file opened %2$s", curFunc, currentDateTimeString));
            mLog(0, String.format("MTKutility version:%1$d  version name:%2$s", vNum, vNam));
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

    public void setDrawerEnabled(boolean enabled) {
        int lockMode = enabled ? DrawerLayout.LOCK_MODE_UNLOCKED :
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        drawer.setDrawerLockMode(lockMode);
        toggle.setDrawerIndicatorEnabled(enabled);
    }

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
                mLog(0, String.format("%s**** CustomExceptionHandler.uncaughtException()%s", NL, NL));
                buildCrashReport(ex);
            }
        }

    }//class CustomExceptionHandler

    private class getRecCount extends AsyncTask<Void, Void, Void> {
        private int dwnDelay;
        private int cmdRetry;
        private int retry;
        private String[] parms;

        protected void onPreExecute() {
            mLog(0, "GetLogFragment.getRecCount.onPreExecute");
            dwnDelay = Integer.parseInt(publicPrefs.getString("dwnDelay", "50"));
            dialog = new ProgressDialog(mContext);
            cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
            dialog.setMessage(getString(R.string.getSetngs));
            dialog.show();
            while (NMEArunning) {
                showNMEA = false;
                goSleep(50);
            }
        }//onPreExecute()

        protected Void doInBackground(Void... params) {
            String curFunc = "GetLogFragment.getRecCount.doInBackground";
            mLog(0, curFunc);
            retry = cmdRetry;
            while (retry > 0) {
                mLog(2, String.format("%1$s getting log record count (PMTK182,2,10) retry %2$d", curFunc, retry));
                parms = mtkCmd("PMTK182,2,10", "PMTK182,3,10", dwnDelay);
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
            if (dialog.isShowing()) dialog.dismiss();
            haveRecCount.sendEmptyMessage(0);
        }//onPostExecute()
    }//class getRecCount


    Handler haveRecCount = new Handler() {
        @Override
//        public void handleMessage(@org.jetbrains.annotations.NotNull Message msg) {
        public void handleMessage(@org.jetbrains.annotations.NotNull Message msg) {
            String curFunc = "GetLogFragment.myHandler.handleMessage()";
            mLog(0, curFunc);
            appPrefEditor.putInt("logRecCount", logRecCount);
            appPrefEditor.commit();
            if (logRecCount > 0) {
                activeFragment = itemSelected;
                changeFragment(fragment);
            } else {
                HomeFragment.showNMEA task = new HomeFragment.showNMEA();
                task.execute();
                switch (itemSelected) {
                    case R.id.nav_GetLog:
                        Toast.makeText(mContext, getString(R.string.noLogDL), Toast.LENGTH_LONG).show();
                        break;
                    case R.id.nav_clrLog:
                        Toast.makeText(mContext, getString(R.string.noLogClr), Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
            if (dialog.isShowing()) dialog.dismiss();
        }
    };//Handler myHandler

    //*************************************** bluetooth routines follow ********************************
    protected static boolean connect() {
        mLog(0, "Main.connect");
        GPSmac = appPrefs.getString("GPSmac", "");
        if (GPSmac.isEmpty()) {
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
        String currFunc = "Main.disconnect";
        //stop Async tasks
        if (!GPSsocket.isConnected()) {
            mLog(2, String.format("%1$s %2$s", currFunc, " GPS already disconnected"));
            return true;
        }

        try {
            mLog(2, String.format("%1$s %2$s", currFunc, " executing GPSout.flush"));
            GPSout.flush();
        } catch (IOException e) {
            buildCrashReport(e);
            return false;
        }
        try {
            mLog(2, String.format("%1$s %2$s", currFunc, " executing GPSout.close"));
            GPSout.close();
        } catch (IOException e) {
            buildCrashReport(e);
            return false;
        }
        try {
            mLog(2, String.format("%1$s %2$s", currFunc, " executing GPSsocket.close"));
            GPSsocket.close();
        } catch (IOException e) {
            buildCrashReport(e);
            return false;
        }
        mLog(2, String.format("%1$s %2$s", currFunc, " GPS is disconnected *****"));
        return true;
    }//disconnect()

    protected static String[] mtkCmd(String mtkcmd, String mtkreply, int delay) {
        String[] sArray = new String[99];
        int ic = 0;
        while (ic < cmdRetry) {
            mLog(1, String.format("Main.mtkCmd(\"%1$s\",\"%2$s\",%3$d)>>>>><<<<<", mtkcmd, mtkreply, delay));
            if (sendCommand(mtkcmd)) {
                goSleep(delay);
                int ix = 0;
                while (ix < cmdRetry) {
                    ix++;
                    sArray = waitForReply(mtkreply, delay);
                    if (sArray == null || sArray.length == 0) {
                        delay += retryInc;
                        mLog(2, String.format("Main.mtkCmd retry %1$d with %2$d delay *****", ix, delay));
                        continue;
                    } else {
                        return sArray;
                    }
                }
            }
            ic++;
        }
        return null;
    }//mtkCmd()

    protected static boolean sendCommand(String command) {
        String curFunc = "Main.sendCommand";
        mLog(1, String.format("%1$s (%2$s)>>>>>", curFunc, command));
        byte checksum = calculateChecksum(command);
        StringBuilder rec = new StringBuilder(256);
        rec.setLength(0);
        rec.append('$');
        rec.append(command);
        rec.append('*');
        rec.append(String.format("%02X", checksum));
        rec.append("\r\n");
        try {
            GPSout.write(rec.toString().getBytes());
        } catch (Exception e) {
            mLog(0, String.format("%1$s CRITICAL ERROR %2$s failed", curFunc, rec.toString().substring(0, rec.length() - 2)));
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
        String curFunc = "Main.sendEmail";
        mLog(0, String.format("%1$s index is %2$d", curFunc, idx));
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
            mLog(1, String.format("%1$s sending %2$s", curFunc, logFile));
            uri = FileProvider.getUriForFile(mContext,
                    mContext.getApplicationContext().getPackageName() + ".provider", logFile);
        } else {
            mLog(1, String.format("%1$s sending %2$s", curFunc, errFile));
            uri = FileProvider.getUriForFile(mContext,
                    mContext.getApplicationContext().getPackageName() + ".provider", errFile);
        }
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        mContext.startActivity(Intent.createChooser(emailIntent, mContext.getResources().getString(R.string.emailMsg)));
    }//sendEmail()

    protected static String[] waitForReply(String mtkReply, int delay) {
        String curFunc = "Main.waitForReply";
        mLog(1, String.format("%1$s (%2$s)<<<<<", curFunc, mtkReply));
        String[] sArray = new String[99];
        boolean doAppend = false;
        int retry = cmdRetry;
        char b = '\0';

        rx = 0;
        readBuf = new StringBuilder();
        readBuf.append(readString(delay));
        StringBuilder sndBuf = new StringBuilder();
        if (readBuf.length() > 0) {
            while (retry > 0) {
                b = readBuf.charAt(rx);
                if (b == '$') {
                    doAppend = true;
                    mLog(3, String.format("%1$s $ found at %2$d of %3$d", curFunc, rx, readBuf.length()));
                }
                if (b == '*' && doAppend) {
                    doAppend = false;
                    mLog(3, String.format("%1$s * found at index %2$d, sndBuf length=%3$d", curFunc, rx, sndBuf.length()));
                    //skip checksum and CR LF in replay
                    rx += 3;
                    //convert to a string array dropping beginng $ symbol
                    String reply = sndBuf.toString().substring(1);
                    sndBuf = new StringBuilder();
                    mLog(3, String.format("%1$s received: %2$s", curFunc, reply));
                    sArray = reply.split(",");
                    if (reply.contains(mtkReply)) {
                        mLog(2, String.format("%1$s received: %2$s", curFunc, reply));
                        return sArray;
                    }
                    //did we get the reply string or a PMTK001 reply - return all failure replies
                    // note: GPS normally returns the requested reply before the PMTK001
                    // when the command sent succeeds
                    if (reply.contains("PMTK001") && !sArray[sArray.length - 1].contains("3")) {
                        mLog(2, String.format("%1$s received: %2$s", curFunc, reply));
                        return sArray;
                    }
                    //skip NMEA sentinces
                    if (sArray[0].contains("GP")) {
                        sndBuf = new StringBuilder();
                        doAppend = false;
                        continue;
                    }

                }
                if (doAppend) {
                    sndBuf.append(b);
                }
                //do not return NMEA sebtebces
                if (sndBuf.length() == 3) {
                    if (sndBuf.toString().contains("GP")) {
                        sndBuf = new StringBuilder();
                        doAppend = false;
                    }
                }

                rx++;
                if (rx > readBuf.length() - 1) {
                    readBuf = new StringBuilder();
                    readBuf.append(readString(delay));
                    rx = 0;
                    if (readBuf.length() < 1) retry = 0;
                }
            }
        }
        mLog(0, String.format("%1$s failed - returning null sArray *****", curFunc));
        //return empty PMTK001 to signify timeout
        sArray = null;
        return sArray;
    }//waitForReply()

    protected static void sendBytes(byte[] byteArray) {
        String curFunc = "Main.sendBytes";
        mLog(1, String.format("%1$s (%2$s)>>>>>", curFunc, byteArray.toString()));
        try {
            GPSout.write(byteArray);
        } catch (Exception e) {
            mLog(0, String.format("%1$s (%2$s) failed *****", curFunc, byteArray.toString()));
            buildCrashReport(e);
        }
    }//sendBytes()

    protected static String readString(int delay) {
        String curFunc = "Main.readString";
        if (!showNMEA)
            mLog(2, String.format("%1$s(%2$s)<<<<<", curFunc, delay));
        char b = '\0';
        byte[] bytBuf = null;
        int retry = cmdRetry;
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
        if (!showNMEA)
            mLog(3, String.format("Main.readBytes(%1$d)<<<<<", delay));
        int bytes_available = 0;
        int retry = cmdRetry;
        byte[] buf = null;

        while (bytes_available == 0) {
            if (!showNMEA)
                mLog(2, String.format("Main.readBytes retry:%1$d  delay:%2$d", retry, delay));
            retry--;
            if (retry < 1) break;
            try {
                bytes_available = GPSin.available();
            } catch (IOException e) {
                buildCrashReport(e);
            }
            goSleep(delay * 2);

        }
        if (!showNMEA)
            mLog(3, String.format("Main.readBytes done: %1$d bytes available", bytes_available));
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

    public static void NMEAstart() {
        String curFunc = "Main.NMEAstart()";
        mLog(0, curFunc);
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        String[] parms;
        mLog(1, String.format("%1$s retreiving saved preference", curFunc));
        String cmd = appPrefs.getString("saveMNEA", "PMTK314,0,1,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0");
        String[] sArray = cmd.split(",");
        OK = false;
        for (int ix = 1; ix <= 19; ix++) {
            if (!sArray[ix].contains("0")) {
                OK = true;
                ix = 20;
            }
        }
        if (!OK) {
            mLog(1, String.format("%1$s stored preference is good", curFunc));
            sendCommand(cmd);
        } else {
            //reset NMEA outpu to default
            mtkCmd("PMTK314,-1", "PMTK001", cmdDelay);
            getNMEAsetting();
        }
    }//NMEAstart()

    public static void NMEAstop() {
        String curFunc = "Main.NMEAstop()";
        mLog(0, curFunc);
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        String[] parms;
        int retry = cmdRetry;
        tryagain:
        do {
            mLog(2, String.format("%1$s retry %2$d", curFunc, retry));
            parms = Main.mtkCmd("PMTK314,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "PMTK001,314", cmdDelay);
            goSleep(cmdDelay);
            parms = Main.mtkCmd("PMTK414", "PMTK514", cmdDelay);
            retry--;
            if (parms == null) continue;
            for (int ix = 1; ix <= 19; ix++) {
                if (parms[ix].contains("1")) continue tryagain;
            }
            retry = 0;
        } while (retry > 0);
    }//NMEAstop()

    public static void LOGstop() {
        String curFunc = "Main.LOGstop()";
        mLog(0, curFunc);
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        String[] parms;
        int retry = cmdRetry;
        do {
            mLog(2, String.format("%1$s retry %2$d", curFunc, retry));
            parms = mtkCmd("PMTK182,5", "PMTK001,182,5", cmdDelay);
            retry--;
            if (parms == null) {
                continue;
            }
            if (parms[0].contains("PMTK001"))
                if (parms[1].contains("182") && parms[3].contains("3")) retry = 0;
        } while (retry > 0);
    }//LOGstop()

    public void LOGstart() {
        String curFunc = "Main.LOGstart()";
        mLog(0, curFunc);
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        String[] parms;
        parms = Main.mtkCmd("PMTK182,4", "PMTK001,182,4", cmdDelay);

//            Main.sendCommand("PMTK182,4");
//            Main.waitForReply("PMTK001,182,4,3");
//        goSleep(2000);
    }//LOGstart()

    public static void getNMEAsetting() {
        String curFunc = "Main.getNMEAsetting()";
        mLog(0, curFunc);
        cmdRetry = Integer.parseInt(publicPrefs.getString("cmdRetry", "5"));
        cmdDelay = Integer.parseInt(publicPrefs.getString("cmdDelay", "25"));
        String[] parms;
        int retry = cmdRetry;
        boolean OK = false;
        while (retry > 0) {
            retry--;
            //get NMEA output setting from GPS
            parms = mtkCmd("PMTK414", "PMTK514", cmdDelay);
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
                    ix = 20;
                }
            }
            if (OK) {
                appPrefEditor.putString("saveMNEA", concatSarray(parms, 0));
                appPrefEditor.commit();
                retry = 0;
            } else {
                //reset GPS to default output if all 0 in settings
                mLog(1, String.format("%1$s invalid NMEA stored - resetting", curFunc));
                mtkCmd("PMTK314,-1", "PMTK001", cmdDelay * 4);
                continue;
            }
        }
    }//getNMEAsetting()

    private static String concatSarray(String[] Sa, int bgn) {
        mLog(0, "GetLogFragment.logDownload.concatSarray");
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
}
