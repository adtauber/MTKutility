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
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;

public class HelpFragment extends Fragment {
    private Context mContext;
    private OutputStreamWriter logWriter;
    private static String NL = System.getProperty("line.separator");
    private View mV;
    private WebView wv;
    String helpXML = "file:///android_asset/aboutscrn.html";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = Main.mContext;
        logWriter = Main.logWriter;
        mLog("HelpFragment.onCreateView()");

        // Inflate the layout for this fragment
        mV = inflater.inflate(R.layout.webview, container, false);
        wv = mV.findViewById(R.id.webview);
        selectSource();
        return mV;
    }

    private void selectSource() {
        mLog("HelpFragment.selectSource()");
        //@formatter:off
        String[] cs = {
                getString(R.string.hs00), //Things You Need to Know
                getString(R.string.hs01), //app preferences
                getString(R.string.hs02), //Home screen
                getString(R.string.hs03), //Download Log screen
                getString(R.string.hs04), //Erase GPS Records
                getString(R.string.hs05), //Make GPX file screen
                getString(R.string.hs06), //Get EPO file screen
                getString(R.string.hs07), //Udate AGPS screen
                getString(R.string.hs08), //Update Settings screen
                getString(R.string.hs09), //Send App Log screen
                getString(R.string.hs10), //Exit
        };
        //@formatter:on
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("Select help topic").setItems(cs, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int idx) {
                switch (idx) {
                    case 0:
                        helpXML = "file:///android_asset/overview.html";
                        mLog("HelpFragment.selectSource() - selected overview.html");
                        break;
                    case 1:
                        helpXML = "file:///android_asset/appPref.html";
                        mLog("HelpFragment.selectSource() - selected appPref.html");
                        break;
                    case 2:
                        helpXML = "file:///android_asset/home.html";
                        mLog("HelpFragment.selectSource() - selected home.html");
                        break;
                    case 3:
                        helpXML = "file:///android_asset/download.html";
                        mLog("HelpFragment.selectSource() - selected download.html");
                        break;
                    case 4:
                        helpXML = "file:///android_asset/eraseGPS.html";
                        mLog("HelpFragment.selectSource() - selected eraseGPS.html");
                        break;
                    case 5:
                        helpXML = "file:///android_asset/makeGPX.html";
                        mLog("HelpFragment.selectSource() - selected makeGPX.html");
                        break;
                    case 6:
                        helpXML = "file:///android_asset/getEPO.html";
                        mLog("HelpFragment.selectSource() - selected getEPO.html");
                        break;
                    case 7:
                        helpXML = "file:///android_asset/updateAGPS.html";
                        mLog("HelpFragment.selectSource() - selected updateAGPS.html");
                        break;
                    case 8:
                        helpXML = "file:///android_asset/settings.html";
                        mLog("HelpFragment.selectSource() - selected settings.html");
                        break;
                    case 9:
                        helpXML = "file:///android_asset/sendlog.html";
                        mLog("HelpFragment.selectSource() - selected sendlog.html");
                        break;
                    case 10:
                        helpXML = "file:///android_asset/exit.html";
                        mLog("HelpFragment.selectSource() - selected exit.html");
                        break;
                }
                dialog.dismiss();
                wv.loadUrl(helpXML);
            }
        }).show();
    }//selectSource()

    private void mLog(String msg) {
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
    }
}
