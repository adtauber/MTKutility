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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.adtdev.fileChooser.FileChooser;

import static android.app.Activity.RESULT_OK;

public class GetEPOFragment extends Fragment {

//    private myLibrary mL;

    private static final int BYPASS_SELECT = 0;
    private static final int LOCAL_FILE = 1;
    private static final int FTP_FILE = 2;
    public static final int LOCAL_FILES = 0;
    public static final int FTP_SELECT = 1;
    public static final int FTP_DOWNLOAD = 2;
    private static final String urlKey = "urlKey";
    private static final int doLOCAL = 0;
    private static final int doFTPselect = 1;
    private static final int doFTPdownld = 2;
    private String NL = System.getProperty("line.separator");

    private TextView FTPurl;
    private TextView FTPpath;
    private TextView FTPuser;
    private TextView FTPpswd;
    private Button btnFTPsave;
    private Button btnFTPsel;
    private Button btnFTPadd;
    private Button btnFTPdel;
    private Button btnFTPfile;
    private Button btnLCLfile;
    private TextView FTPfile;
    private TextView LCLfile;
    private Button btnFTPapnd;
    private Button btnFTPdnld;
    private TextView HTPurl;
    private Button btnHTPsave;
    private Button btnHTPdnld;
    private Button btnChkDl;
    private EditText aFTPdesc;
    private EditText aFTPip;
    private EditText aFTPpath;
    private EditText aFTPuser;
    private EditText aFTPpswd;

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private Context mContext = Main.mContext;
    private boolean logFileIsOpen = Main.logFileIsOpen;
    private int debugLVL = 0;
    private final int ABORT = 9;
    private OutputStreamWriter logWriter = Main.logWriter;

    private Intent intent;
    private boolean FTPappend;
    private String startPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/mtkutility/epo";
    private String localFile;
    private File epoPath;
    private String ftpDLfile;
    private String ftpDLpath;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLog(0, "GetEPOFragment.onCreateView()");

        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        View rootView = inflater.inflate(R.layout.getepo, container, false);
        FTPurl = rootView.findViewById(R.id.FTPip);
        FTPpath = rootView.findViewById(R.id.FTPpath);
        FTPuser = rootView.findViewById(R.id.FTPuser);
        FTPpswd = rootView.findViewById(R.id.FTPpswd);
        FTPfile = rootView.findViewById(R.id.FTPfile);
        LCLfile = rootView.findViewById(R.id.LCLfile);
        HTPurl = rootView.findViewById(R.id.HTPurl);

        btnFTPsel = rootView.findViewById(R.id.btnFTPsel);
        btnFTPsel.setTransformationMethod(null);
        btnFTPsel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPsel.getText() + " pressed");
                selectFTPurl();
            }
        });

        btnFTPsave = rootView.findViewById(R.id.btnFTPsave);
        btnFTPsave.setTransformationMethod(null);
        btnFTPsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPsave.getText() + " pressed");
                saveFTPpreferences();
            }
        });

        btnFTPadd = rootView.findViewById(R.id.btnFTPadd);
        btnFTPadd.setTransformationMethod(null);
        btnFTPadd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPadd.getText() + " pressed");
                addFTPurl();
            }
        });

        btnFTPdel = rootView.findViewById(R.id.btnFTPdel);
        btnFTPdel.setTransformationMethod(null);
        btnFTPdel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPdel.getText() + " pressed");
                delFTPurl();
            }
        });

//        private Button btnFTPfile;
        btnFTPfile = rootView.findViewById(R.id.btnFTPfile);
        btnFTPfile.setTransformationMethod(null);
        btnFTPfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPfile.getText() + " pressed");
                doFTPfileSelect();
            }
        });

//        private Button btnLCLfile;
        btnLCLfile = rootView.findViewById(R.id.btnLCLfile);
        btnLCLfile.setTransformationMethod(null);
        btnLCLfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnLCLfile.getText() + " pressed");
                doLCLfileSelect();
            }
        });

//        private Button btnFTPapnd;
        btnFTPapnd = rootView.findViewById(R.id.btnFTPapnd);
        btnFTPapnd.setTransformationMethod(null);
        btnFTPapnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPapnd.getText() + " pressed");
                doFTPappend();
            }
        });

//        private Button btnFTPdnld;
        btnFTPdnld = rootView.findViewById(R.id.btnFTPdnld);
        btnFTPdnld.setTransformationMethod(null);
        btnFTPdnld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnFTPdnld.getText() + " pressed");
                doFTPdownload();
            }
        });


//        private Button btnHTPsave;
        btnHTPsave = rootView.findViewById(R.id.btnHTPsave);
        btnHTPsave.setTransformationMethod(null);
        btnHTPsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnHTPsave.getText() + " pressed");
                saveHTPpreferences();
            }
        });

        btnHTPdnld = rootView.findViewById(R.id.btnHTPdnld);
        btnHTPdnld.setTransformationMethod(null);
        btnHTPdnld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnHTPdnld.getText() + " pressed");
                new HTPdownld(getActivity()).execute();
            }
        });

//        private Button btnChkDl;
        btnChkDl = rootView.findViewById(R.id.btnChkDl);
        btnChkDl.setTransformationMethod(null);
        btnChkDl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "GetEPOFragment - button " + btnChkDl.getText() + " pressed");
                doLOCALview();
            }
        });
        btnFTPapnd.setEnabled(false);
        btnFTPdnld.setEnabled(false);
//        btnHTPdnld.setEnabled(false);
        return rootView;
    }//onCreateView()

    public void onPause() {
        super.onPause();
        String curFunc = "GetEPOFragment.onPause";
        mLog(1, curFunc);
    }    //onPause()

    @Override
    public void onResume() {
        super.onResume();
        String curFunc = "GetEPOFragment.onResume";
        mLog(1, curFunc);
        while (Main.BkGrndActive) {
            Main.BkGrndActive = false;
            goSleep(50);
        }
    }    //onResume()

    public void onViewCreated(View view, Bundle savedInstanceState) {
        mLog(0, "GetEPOFragment.onViewCreated()");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugLVL", "0"));
//        String FTPipD = "60.248.237.25";
//        String FTPpathD = "/";
//        String FTPuserD = "tsi0001";
//        String FTPpswdD = "tweyet";
//        String FTPfileD = "MTK7d.EPO";
        String FTPipD = "81.223.20.116";
        String FTPpathD = "/AGPS";
        String FTPuserD = "krippl-gps-master";
        String FTPpswdD = "master";
        String HTPurlD = "http://epodownload.mediatek.com/EPO.DAT"; // http://epodownload.mediatek.com/EPO.MD5
        FTPurl.setText(appPrefs.getString("FTPurl", FTPipD), TextView.BufferType.NORMAL);
        FTPpath.setText(appPrefs.getString("FTPpath", FTPpathD), TextView.BufferType.NORMAL);
        FTPuser.setText(appPrefs.getString("FTPuser", FTPuserD), TextView.BufferType.NORMAL);
        FTPpswd.setText(appPrefs.getString("FTPpswd", FTPpswdD), TextView.BufferType.NORMAL);
        HTPurl.setText(appPrefs.getString("HTPurl", HTPurlD));
    }//onViewCreated()

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mLog(0, "GetEPOFragment.onActivityResult()");
        // determine which child activity is calling us back.
        //               String Name = data.getStringExtra("GetFileName");
//               String Path = data.getStringExtra("GetPath");

        switch (requestCode) {
            case LOCAL_FILE:
                if (resultCode == RESULT_OK) {
                    localFile = data.getStringExtra("GetPath");
                    LCLfile.setText(localFile);
//                    LCLfile.setText(data.getStringExtra("GetFileName"));
                    if (!FTPfile.getText().toString().matches("EPO file name"))
                        btnFTPapnd.setEnabled(true);
                }
                break;
            case FTP_FILE:
                if (resultCode == RESULT_OK) {
//                    FTPfile.setText(data.getStringExtra("GetFileName"));
                    ftpDLfile = data.getStringExtra("GetFileName");
                    FTPfile.setText(data.getStringExtra("GetPath"));
                    ftpDLpath = data.getStringExtra("GetPath");
                    btnFTPdnld.setEnabled(true);
                    if (!LCLfile.getText().toString().matches("local file name"))
                        btnFTPapnd.setEnabled(true);
                }
                break;
            default:
                break;
        }
        if (requestCode == LOCAL_FILE) {
        }
    }//onActivityResult()

    private void addFTPurl() {
        mLog(0, "GetEPOFragment.addFTPurl()");
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.addurl, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setView(dialogView)
                .setTitle("Enter FTP site data")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Gson gson = new Gson();
                        String response = appPrefs.getString(urlKey, "");
                        ArrayList<urlModel> sitesList = gson.fromJson(response, new TypeToken<ArrayList<urlModel>>() {
                        }.getType());
                        aFTPdesc = dialogView.findViewById(R.id.aFTPdesc);
                        aFTPip = dialogView.findViewById(R.id.aFTPip);
                        aFTPpath = dialogView.findViewById(R.id.aFTPpath);
                        aFTPuser = dialogView.findViewById(R.id.aFTPuser);
                        aFTPpswd = dialogView.findViewById(R.id.aFTPpswd);
                        String ipn = aFTPdesc.getText().toString();
                        String url = aFTPip.getText().toString();
                        String pth = aFTPpath.getText().toString();
                        String usr = aFTPuser.getText().toString();
                        String psw = aFTPpswd.getText().toString();
                        sitesList.add(new urlModel(ipn, url, pth, usr, psw));
                        gson = new Gson();
                        String json = gson.toJson(sitesList);
                        appPrefEditor.putString(urlKey, json);
                        appPrefEditor.commit();
                    }
                }).show();

    }//addFTPurl()

    private void delFTPurl(){
        final ArrayList<String> selList = new ArrayList<String>();
        Gson gson = new Gson();
        String response = appPrefs.getString(urlKey, "");
        final ArrayList<urlModel> sitesList = gson.fromJson(response,
                new TypeToken<ArrayList<urlModel>>(){}.getType());

        for (urlModel site : sitesList) {
            selList.add(site.getDesc());
        }
        CharSequence[] cs = selList.toArray(new CharSequence[selList.size()]);
        int setChk = -1;
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("Select Site")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setItems(cs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int idx) {
                        urlModel obj = sitesList.get(idx);
                        sitesList.remove(idx);
                        Gson gson = new Gson();
                        String json = gson.toJson(sitesList);
                        appPrefEditor.putString(urlKey, json);
                        appPrefEditor.commit();
                        dialog.dismiss();
                    }
                })
                .show();
    }//delFTPurl()

    private void doFTPappend(){
        mLog(0, "GetEPOFragment.doFTPappend()");
        FTPappend = true;
        intent = new Intent(getActivity(), FileChooser.class);
        intent.putExtra("method", doFTPdownld);
        intent.putExtra("ftpURL", FTPurl.getText().toString());
        intent.putExtra("ftpPath", FTPpath.getText().toString());
        intent.putExtra("ftpName", FTPuser.getText().toString());
        intent.putExtra("ftpPswd", FTPpswd.getText().toString());
        intent.putExtra("ftpPort", "21");
//        intent.putExtra("srceFN", FTPfile.getText().toString());
        intent.putExtra("srceFN", ftpDLpath);
        intent.putExtra("destFN", localFile);
        intent.putExtra("append", FTPappend);
//        new FTPdownld(getActivity()).execute();
        startActivityForResult(intent, FTP_DOWNLOAD);

    }//doFTPappend()

    private void doFTPdownload(){
        String curFunc = "GetEPOFragment.doFTPdownload()";
        mLog(0, curFunc);
        String epoPathName;
        boolean OK = true;

        epoPathName = appPrefs.getString("epoPathName", "");
        epoPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), epoPathName);
        // make sure mtkutility/bin directory exists - create if it is missing
        if (!epoPath.exists()) {
            OK = epoPath.mkdirs();
        }
        if (!OK) {
            mLog(ABORT, String.format("%1$s aborting - create %2$s failed +++", curFunc, epoPathName));
            return;
        }
        epoPath = new File(epoPath.toString(), ftpDLfile);
        if (!FTPappend && epoPath.exists()) {
            epoPath.delete();
        }

        FTPappend = false;
        intent = new Intent(getActivity(), FileChooser.class);
        intent.putExtra("method", doFTPdownld);
        intent.putExtra("ftpURL", FTPurl.getText().toString());
        intent.putExtra("ftpPath", FTPpath.getText().toString());
        intent.putExtra("ftpName", FTPuser.getText().toString());
        intent.putExtra("ftpPswd", FTPpswd.getText().toString());
        intent.putExtra("ftpPort", "21");
//        intent.putExtra("srceFN", FTPfile.getText().toString());
        intent.putExtra("srceFN", ftpDLpath);
        intent.putExtra("destFN", epoPath.toString());
        intent.putExtra("append", FTPappend);
        new FTPdownld(getActivity()).execute();
    }//doFTPdownload()

    private void doFTPfileSelect(){
        mLog(0, "GetEPOFragment.doFTPfileSelect()");
        intent = new Intent(getActivity(), FileChooser.class);
        intent.putExtra("method", doFTPselect);
        intent.putExtra("ftpURL", FTPurl.getText().toString());
        intent.putExtra("ftpPath", FTPpath.getText().toString());
        intent.putExtra("ftpName", FTPuser.getText().toString());
        intent.putExtra("ftpPswd", FTPpswd.getText().toString());
        intent.putExtra("ftpPort", "21");
        startActivityForResult(intent, FTP_FILE);
    }//doFTPfileSelect()

    private void doLCLfileSelect(){
        mLog(0, "GetEPOFragment.doLCLfileSelect()");
        intent = new Intent(getActivity(), FileChooser.class);
        intent.putExtra("method", doLOCAL);
        intent.putExtra("root", "/storage");
        intent.putExtra("start", startPath);
        intent.putExtra("selfolders", false);
        intent.putExtra("selfiles", true);
        intent.putExtra("showhidden", false);
        startActivityForResult(intent, LOCAL_FILE);
    }//doLCLfileSelect()

    private void doLOCALview() {
        mLog(0, "GetEPOFragment.doLOCALview()");
        intent = new Intent(getActivity(), FileChooser.class);
        intent.putExtra("method", doLOCAL);
//        intent.putExtra("root", "/storage");
        intent.putExtra("root", startPath);
        intent.putExtra("start", startPath);
        intent.putExtra("selfolders", false);
        intent.putExtra("selfiles", false);
        intent.putExtra("showhidden", false);
        startActivityForResult(intent, BYPASS_SELECT);
    }//doLOCALview()

    private void goSleep(int mSec) {
        mLog(3, String.format("ClrLogFragment.goSleep(%d)", mSec));
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
        time = time.replace("AM","");
        time = time.replace("PM","");
        try {
            logWriter.append(time + " " + msg + NL);
            logWriter.flush();
        } catch (IOException e) {
            Main.buildCrashReport(e);
        }
    }//Log()

    private void saveFTPpreferences() {
        mLog(0, "GetEPOFragment.saveFTPpreferences()");
        String tmp;
        tmp = FTPurl.getText().toString();
        appPrefEditor.putString("FTPurl", tmp);
        tmp = FTPpath.getText().toString();
        appPrefEditor.putString("FTPpath", tmp);
        tmp = FTPuser.getText().toString();
        appPrefEditor.putString("FTPuser", tmp);
        tmp = FTPpswd.getText().toString();
        appPrefEditor.putString("FTPpswd", tmp);
        appPrefEditor.commit();
        Toast.makeText(mContext, getString(R.string.saved), Toast.LENGTH_LONG).show();
    }//saveFTPpreferences()

    private void saveHTPpreferences() {
        mLog(0, "GetEPOFragment.saveHTPpreferences()");
        appPrefEditor.putString("HTPurl", HTPurl.getText().toString());
        appPrefEditor.commit();
        Toast.makeText(mContext, getString(R.string.saved), Toast.LENGTH_LONG).show();
    }//saveHTPpreferences()

    private void selectFTPurl() {
//        ArrayList<urlModel> sitesList = new ArrayList();

        final ArrayList<String> selList = new ArrayList<String>();
        Gson gson = new Gson();
        String response = appPrefs.getString(urlKey, "");
        final ArrayList<urlModel> sitesList = gson.fromJson(response,
                new TypeToken<ArrayList<urlModel>>(){}.getType());

        for (urlModel site : sitesList) {
            selList.add(site.getDesc());
        }
        CharSequence[] cs = selList.toArray(new CharSequence[selList.size()]);
        int setChk = -1;
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("Select Site")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setItems(cs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int idx) {
                        urlModel obj = sitesList.get(idx);
                        FTPurl.setText(obj.getURL());
                        FTPpath.setText(obj.getPATH());
                        FTPuser.setText(obj.getUSER());
                        FTPpswd.setText(obj.getPSWD());
                        dialog.dismiss();
                    }
                })
                .show();
    }//selectFTPurl()

    private void showToast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    } //showToast()

    private class FTPdownld extends AsyncTask<Void, String, Void> {
        private ProgressDialog dialog;
        private Context mContext;
        private String Surl, epoFN, tmp;
        private String[] parms;
        private boolean OK;
        private int ix;
        private File epoPath;


        public FTPdownld(Context context) {
            mLog(0, "GetEPOFragment.FTPdownld.HTPdownld()");
            mContext = context;
            dialog = new ProgressDialog(mContext);
        }//HTPdownld()

        @Override
        protected void onPreExecute() {
            mLog(0, "GetEPOFragment.FTPdownld.onPreExecute()");
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
            while (Main.BkGrndActive){
                Main.BkGrndActive = false;
                goSleep(250);
            }
            Main.BkGrndActive = true;
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(0, "GetEPOFragment.FTPdownld.doInBackground()");
            startActivityForResult(intent, BYPASS_SELECT);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            mLog(0, "GetEPOFragment.FTPdownld.onPostExecute()");
            if (dialog.isShowing()) dialog.dismiss();
            Main.BkGrndActive = false;
        }//onPostExecute()

    }//class FTPdownld

    private class HTPdownld extends AsyncTask<Void, String, Void> {
        private ProgressDialog dialog;
        private Context mContext;
        private String Surl, epoFN, tmp;
        private String[] parms;
        private boolean OK;
        private int ix;
        private File epoPath;
        public String epoPathName = "mtkutility/epo";

        public HTPdownld(Context context) {
            mLog(0, "GetEPOFragment.HTPdownld.HTPdownld()");
            mContext = context;
            dialog = new ProgressDialog(mContext);
        }//HTPdownld()

        @Override
        protected void onPreExecute() {
            mLog(0, "GetEPOFragment.HTPdownld.onPreExecute()");
            this.dialog.setMessage(getString(R.string.working));
            this.dialog.show();
            while (Main.BkGrndActive){
                Main.BkGrndActive = false;
                goSleep(250);
            }
            Main.BkGrndActive = true;
        }//onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(0, "GetEPOFragment.HTPdownld.doInBackground()");
            Surl = HTPurl.getText().toString();
            ix = Surl.lastIndexOf("/");
            epoFN = Surl.substring(ix + 1);
            OK = true;
            epoPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), epoPathName);
            // make sure mtkutility/bin directory exists - create if it is missing
            if (!epoPath.exists()) OK = epoPath.mkdirs();
            if (!OK) return null;

            epoPath = new File(epoPath.toString(), epoFN);
            if (epoPath.exists()) epoPath.delete();
            try {
                URL url = new URL(Surl);
                HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
                urlconn.setRequestMethod("GET");
                urlconn.setInstanceFollowRedirects(true);
                urlconn.connect();
                InputStream in = urlconn.getInputStream();
                FileOutputStream out = new FileOutputStream(epoPath.toString());
                int read;
                byte[] buffer = new byte[4096];
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                out.close();
                in.close();
                urlconn.disconnect();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        //        @Override
        //String values expected:  Action, Style, message, percent
        protected void onProgressUpdate(String... values) {
            mLog(0, "GetEPOFragment.HTPdownld.onProgressUpdate()");
//            mTv.append(msg + NL);
//            mSv.fullScroll(View.FOCUS_DOWN);
        }

        @Override
        protected void onPostExecute(Void param) {
            mLog(0, "GetEPOFragment.HTPdownld.onPostExecute()");
            if (dialog.isShowing()) dialog.dismiss();
            showToast("HTTP " + getString(R.string.dlDone));
            Main.BkGrndActive = false;
        }//onPostExecute()
    }//class HTPdownld
}//class GetEPOFragment
