package com.adtdev.mtkutility;
/**
 * @author Alex Tauber
 * <p>
 * This file is part of the open source Android app mtkutility. You can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 3 of the License. This extends to files included that were authored by
 * others and modified to make them suitable for this app. All files included were subject to
 * open source licensing.
 * <p>
 * mtkutility is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You can review a copy of the
 * GNU General Public License at http://www.gnu.org/licenses.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;

import com.adtdev.fileChooser.FileChooser;

import static android.app.Activity.RESULT_OK;

public class eMailFragment extends Fragment {
    private static final int REQUEST_PATH = 1;
    private static final int doLOCAL = 0;
    private File logPath;
    private File eFile;
    private File logFile;
    private File errFile;
    private String basePathName;
    private String logFileName;
    private String errFileName;

    private TextView lfileName;
    private Button getefile;
    private Button sendefile;
    private String startPath;

    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private int debugLVL = 0;
    private final int ABORT = 9;
    private boolean logFileIsOpen = Main.logFileIsOpen;
    private OutputStreamWriter logWriter = Main.logWriter;
    private String NL = System.getProperty("line.separator");
    private Context mContext = Main.mContext;

    @Override
    public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState) {
        return super.onGetLayoutInflater(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLog(0, "eMailFragment.onCreateView()");
        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        basePathName = appPrefs.getString("basePathName", "");
        logFileName = appPrefs.getString("logFileName", "");
        errFileName = appPrefs.getString("errFileName", "");

        logPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), basePathName);
        logFile = new File(logPath, logFileName);
        errFile = new File(logPath, errFileName);

        View rootView = inflater.inflate(R.layout.email, container, false);
        lfileName = rootView.findViewById(R.id.lfileName);

        getefile = rootView.findViewById(R.id.getefile);
        getefile.setTransformationMethod(null);
        getefile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "eMailFragment.onCreateView() button " + getefile.getText() + " pressed");
                getfile();
            }
        });

        sendefile = rootView.findViewById(R.id.sendefile);
        sendefile.setEnabled(false);
        sendefile.setTransformationMethod(null);
        sendefile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "eMailFragment.onCreateView() button " + sendefile.getText() + " pressed");
                sendEmail(0);
            }
        });
        debugLVL = Integer.parseInt(publicPrefs.getString("debugPref", "0"));
        return rootView;
    }//onCreateView()

    private void getfile() {
        mLog(0, "eMailFragment.getfile()");
        startPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        startPath = startPath + "/mtkutility";
        Intent intent = new Intent(mContext, FileChooser.class);
        intent.putExtra("method", doLOCAL);
        intent.putExtra("root", startPath);
        intent.putExtra("start", startPath);
        intent.putExtra("nofolders", true);
        intent.putExtra("showhidden", false);
        startActivityForResult(intent, REQUEST_PATH);
    }//getfile()

    // Listen for results.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mLog(0, "eMailFragment.onActivityResult()");
        // See which child activity is calling us back.
        if (requestCode == REQUEST_PATH) {
            if (resultCode == RESULT_OK) {
                eFile = new File(data.getStringExtra("GetPath"));
                lfileName.setText(data.getStringExtra("GetFileName"));
                sendefile.setEnabled(true);
            }
        }
    }//onActivityResult()

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
        time = time.replace("AM","");
        time = time.replace("PM","");
        try {
            logWriter.append(time + " " + msg + NL);
            logWriter.flush();
        } catch (IOException e) {
            Main.buildCrashReport(e);
        }
    }//Log()

    public void sendEmail(int idx) {
        mLog(0, "eMailFragment.sendEmail()");
        switch (idx){
            case 0:
                if (!eFile.exists() || !eFile.canRead()) return;
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
}
