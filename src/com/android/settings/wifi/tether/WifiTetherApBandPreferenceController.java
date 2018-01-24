/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.wifi.tether;

import static android.net.wifi.WifiConfiguration.AP_BAND_2GHZ;
import static android.net.wifi.WifiConfiguration.AP_BAND_5GHZ;
import static android.net.wifi.WifiConfiguration.AP_BAND_ANY;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";
    private static final String[] BAND_VALUES =
            {String.valueOf(AP_BAND_ANY), String.valueOf(AP_BAND_2GHZ),
                    String.valueOf(AP_BAND_5GHZ)};

    private final String[] mBandEntries;
    private int mBandIndex;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mBandEntries = mContext.getResources().getStringArray(R.array.wifi_ap_band_config_full);
    }

    @Override
    public void updateDisplay() {
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        if (config == null) {
            mBandIndex = 0;
            Log.d(TAG, "Updating band index to 0 because no config");
        } else if (is5GhzBandSupported()) {
            mBandIndex = config.apBand;
            Log.d(TAG, "Updating band index to " + mBandIndex);
        } else {
            config.apBand = 0;
            mWifiManager.setWifiApConfiguration(config);
            mBandIndex = config.apBand;
            Log.d(TAG, "5Ghz not supported, updating band index to " + mBandIndex);
        }
        ListPreference preference = (ListPreference) mPreference;
        if (!is5GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setEntries(mBandEntries);
            preference.setEntryValues(BAND_VALUES);
            preference.setSummary(mBandEntries[mBandIndex + 1]);
            preference.setValue(String.valueOf(mBandIndex));
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mBandIndex = Integer.parseInt((String) newValue);
        Log.d(TAG, "Band preference changed, updating band index to " + mBandIndex);
        preference.setSummary(mBandEntries[mBandIndex + 1]);
        mListener.onTetherConfigUpdated();
        return true;
    }

    private boolean is5GhzBandSupported() {
        final String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            return false;
        }
        return true;
    }

    public int getBandIndex() {
        return mBandIndex;
    }
}
