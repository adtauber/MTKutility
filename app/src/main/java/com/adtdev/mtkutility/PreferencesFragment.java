/**
 * @author Alex Tauber
 *
 * This file is part of the Android app mtkutility.
 *
 * mtkutility is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License. This extends to files
 * included that were authored by others and modified to make them suitable for
 * mtkutility. All files included were subject to open source licensing.
 *
 * mtkutility is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You can review a copy of the GNU General Public License
 * at http://www.gnu.org/licenses.
 *
 */
package com.adtdev.mtkutility;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import java.util.Set;

public class PreferencesFragment extends PreferenceFragment
        implements OnSharedPreferenceChangeListener {

    //	private myLibrary mL;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String PathName = "";
    private final int REQUEST_CODE_BIN_DIR = 1;
    private final int REQUEST_CODE_GPX_DIR = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		mL.mLog(mL.VB3, "PreferencesFragment.onCreate()");

        addPreferencesFromResource(R.xml.privateprefs);
    }	//onCreate()

    @Override
    public void onResume() {
        super.onResume();
//		mL.mLog(mL.VB3, "PreferencesFragment.onResume()");
//		mL.readBytesDelay = mL.cmdDelay;
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        initSummary();
    }	//onResume()

    @Override
    public void onPause() {
        super.onPause();
//		mL.mLog(mL.VB3, "PreferencesFragment.onPause()");
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        //update the shared preferences in app
        Main.getSharedPreference();
    }	//onPause()

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//		mL.mLog(mL.VB3, "PreferencesFragment.onSharedPreferenceChanged()");
        // Update summary
        updatePrefsSummary(sharedPreferences, findPreference(key));
    }	//onSharedPreferenceChanged()

    protected void updatePrefsSummary(SharedPreferences sharedPreferences, Preference pref) {
//		mL.mLog(mL.VB3, "PreferencesFragment.updatePrefsSummary()");
        if (pref == null) return;

        if (pref instanceof ListPreference) {
            // List Preference
            ListPreference listPref = (ListPreference) pref;
            listPref.setSummary(listPref.getEntry());

        } else if (pref instanceof EditTextPreference) {
            // EditPreference
            EditTextPreference editTextPref = (EditTextPreference) pref;
            editTextPref.setSummary(editTextPref.getText());

        } else if (pref instanceof MultiSelectListPreference) {
            // MultiSelectList Preference
            MultiSelectListPreference mlistPref = (MultiSelectListPreference) pref;
            String summaryMListPref = "";
            String and = "";
            // Retrieve values
            Set<String> values = mlistPref.getValues();
            for (String value : values) {
                // For each value retrieve index
                int index = mlistPref.findIndexOfValue(value);
                // Retrieve entry from index
                CharSequence mEntry = index >= 0
                        && mlistPref.getEntries() != null ? mlistPref
                        .getEntries()[index] : null;
                if (mEntry != null) {
                    // add summary
                    summaryMListPref = summaryMListPref + and + mEntry;
                    and = ";";}
            }
            // set summary
            mlistPref.setSummary(summaryMListPref);
        }
    }	//updatePrefsSummary()

    protected void initSummary() {
//		mL.mLog(mL.VB3, "PreferencesFragment.initSummary()");
        int pcsCount=getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < pcsCount; i++) {
            initPrefsSummary(getPreferenceManager().getSharedPreferences(),
                    getPreferenceScreen().getPreference(i));}
    }	//initSummary()

    protected void initPrefsSummary(SharedPreferences sharedPreferences, Preference p) {
//		mL.mLog(mL.VB3, "PreferencesFragment.initPrefsSummary()");
        if (p instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory) p;
            int pcCatCount= pCat.getPreferenceCount();
            for (int i = 0; i < pcCatCount; i++) {
                initPrefsSummary(sharedPreferences, pCat.getPreference(i));}
        } else {
            updatePrefsSummary(sharedPreferences, p);}
    }	//initPrefsSummary()

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
//		mL.mLog(mL.VB3, "PreferencesFragment.onPreferenceTreeClick()");
        return false;
    }	//onPreferenceTreeClick()

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//		mL.mLog(mL.VB3, "PreferencesFragment.onActivityResult()");
    }	//onActivityResult()
}
