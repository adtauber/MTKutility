package com.adtdev.mtkutility;
/**
 * @author Alex Tauber
 *
 * This file is part of the open source Android app MTKutility. You can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 3 of the License. This extends to files included that were authored by
 * others and modified to make them suitable for this app. All files included were subject to
 * open source licensing.
 *
 * MTKutility is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You can review a copy of the
 * GNU General Public License at http://www.gnu.org/licenses.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class AboutFragment extends Fragment {
    private Context mContext;
    private static String NL = System.getProperty("line.separator");
    private View mV;
    private WebView wv;
    String aboutXML = "file:///android_asset/MTKabout.html";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = Main.mContext;

        // Inflate the layout for this fragment
        mV =  inflater.inflate(R.layout.webview, container, false);
        wv = mV.findViewById(R.id.webview);

        if (Main.firstRun) {
            Main.appPrefEditor.putBoolean(Main.initSTART, false).commit();
            aboutXML = "file:///android_asset/MTKstartup.html";
            wv.loadUrl(aboutXML);
        }else {
            selectSource();
        }
        return mV;
    }

    private void selectSource() {
        String[] cs = {"about MTKutility", "welcome message" };
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("Select topic")
                .setItems(cs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int idx) {
                        switch (idx){
                            case 0:
                                aboutXML = "file:///android_asset/MTKabout.html";
                                break;
                            case 1:
                                aboutXML = "file:///android_asset/MTKstartup.html";
                                break;
                        }
                        dialog.dismiss();
                        wv.loadUrl(aboutXML);
                    }
                })
                .show();
    }//selectSource()
}
