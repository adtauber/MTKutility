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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;

import com.adtdev.fileChooser.FileChooser;

import static android.app.Activity.RESULT_OK;


//import adtdev.com.mtkutility.R;

public class MakeGPXFragment extends Fragment {

//    private myLibrary mL;
    private File BINpath;
    private File GPXpath;
    private boolean cbxone;
    static ProgressDialog dialog;
    private static final int doLOCAL = 0;
    private String NL = System.getProperty("line.separator");

    private boolean logFileIsOpen = Main.logFileIsOpen;
    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private Context mContext = Main.mContext;
    private int debugLVL;
    private final int ABORT = 9;
    private OutputStreamWriter logWriter = Main.logWriter;

    private Button getfile;
    private Button makeGPX;
    private TextView fileName;
    private CheckBox cbxOne;
    private View rootView;

    private String startPath;
    private String binFile;

    // Keys
    public static final int REQUEST_PATH = 1;
    public static final String KEY_TOAST = "toast";
    public static final String MESSAGEFIELD = "textSwitcher";
    public static final String KEY_PROGRESS = "progressCompleted";
    public static final String CLOSE_PROGRESS = "closeProgressDialog";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log(0, "MakeGPXFragment.onCreateView()");
        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        rootView = inflater.inflate(R.layout.makegpx, container, false);
        fileName = rootView.findViewById(R.id.fileName);
        cbxOne = rootView.findViewById(R.id.cbxOne);

        cbxOne.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log(1, "+++ HomeFragment.onCreateView() +++ allow insecure checkbox changed");
                cbxone = isChecked;
            }
        });

        getfile = rootView.findViewById(R.id.getfile);
        getfile.setTransformationMethod(null);
        getfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(0, "+++ MakeGPXFragment.onCreateView() +++ button " + getfile.getText() + " pressed");
                getfile();
            }
        });

        makeGPX = rootView.findViewById(R.id.makeGPX);
        makeGPX.setTransformationMethod(null);
        makeGPX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log(0, "+++ MakeGPXFragment.onCreateView() +++ button " + makeGPX.getText() + " pressed");
                makeGPX();
            }
        });

        return rootView;
    }//onCreateView()

    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log(0, "MakeGPXFragment.onViewCreated()");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugPref", "0"));
        makeGPX.setEnabled(false);
    }//onViewCreated()

    private void getfile() {
        Log(0, "MakeGPXFragment.onCreateView()");
        startPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        startPath = startPath + "/mtkutility/bin";
        Intent intent = new Intent(mContext, FileChooser.class);
        intent.putExtra("method", doLOCAL);
        intent.putExtra("root","/storage");
        intent.putExtra("start", startPath);
        intent.putExtra("selfolders", false);
        intent.putExtra("selfiles", true);
        intent.putExtra("showhidden",false);
        startActivityForResult(intent,REQUEST_PATH);
    }//getfile()

    private void makeGPX(){
        dialog = new ProgressDialog(getActivity());
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage("Converting GPS Log to GPX file");
        dialog.setCancelable(true);
        dialog.setMax(100);
        dialog.show();

//        boolean onetrk = cbxOne.g
        String gpxFile = binFile;
        gpxFile = gpxFile.replace("bin","gpx");
        gpxFile = "mtkutility/gpx/" + gpxFile;
        BINpath = new File(startPath);
        GPXpath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), gpxFile);
        ParseBinFile parseBinFile = new ParseBinFile(BINpath, GPXpath, parseHandler, cbxone);
        Thread gpxThread = new Thread(parseBinFile);
        gpxThread.start();
    }//makeGPX()
    
    // Listen for results.
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        if (requestCode == REQUEST_PATH){
            if (resultCode == RESULT_OK) {
                binFile = data.getStringExtra("GetFileName");
                if (binFile.contains(".bin")) {
                    startPath = data.getStringExtra("GetPath");
                    fileName.setText(startPath);
                    makeGPX.setEnabled(true);
                }
            }
        }
    }//onActivityResult()

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

    // Define a Handler
    final Handler parseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.getData().containsKey(MESSAGEFIELD)) {
//                writeToMainTextArea(msg.getData().getString(MESSAGEFIELD));
            }
            if (msg.getData().containsKey(CLOSE_PROGRESS)) {
                if (msg.getData().getInt(CLOSE_PROGRESS) == 1){
                    dialog.dismiss();
                    makeGPX.setEnabled(false);
                }
            }
            if (msg.getData().containsKey(KEY_PROGRESS)) {
                dialog.show();
                dialog.setProgress(msg.getData().getInt(KEY_PROGRESS));}
//			if (msg.getData().containsKey(CREATEGPX)) {
//				createGPX(msg.getData().getString(CREATEGPX));}
            if (msg.getData().containsKey(KEY_TOAST)) {
                String message = msg.getData().getString(KEY_TOAST);
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();}
        }
    };


}
