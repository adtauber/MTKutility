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
 
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();
  }
}