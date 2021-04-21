/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.wifitrackerlib.WifiPickerTracker;

/**
 * Preference controller for "Carrier Wi-Fi network"
 */
public class CarrierWifiTogglePreferenceController extends TogglePreferenceController implements
        WifiPickerTracker.WifiPickerTrackerCallback {

    private static final String TAG = "CarrierWifiTogglePreferenceController";
    protected static final String CARRIER_WIFI_TOGGLE_PREF_KEY = "carrier_wifi_toggle";
    protected static final String CARRIER_WIFI_NETWORK_PREF_KEY = "carrier_wifi_network";

    protected final Context mContext;
    protected boolean mIsProviderModelEnabled;
    protected int mSubId;
    protected WifiPickerTrackerHelper mWifiPickerTrackerHelper;
    protected boolean mIsCarrierProvisionWifiEnabled;
    protected Preference mCarrierNetworkPreference;

    public CarrierWifiTogglePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mIsProviderModelEnabled = Utils.isProviderModelEnabled(context);
    }

    /** Initialize related properties */
    public void init(Lifecycle lifecycle, int subId) {
        mSubId = subId;
        mWifiPickerTrackerHelper = new WifiPickerTrackerHelper(lifecycle, mContext, this);
        mIsCarrierProvisionWifiEnabled =
                mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(mSubId);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mIsProviderModelEnabled) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return mIsCarrierProvisionWifiEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mWifiPickerTrackerHelper.isCarrierNetworkEnabled(mSubId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mWifiPickerTrackerHelper == null) {
            return false;
        }
        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(isChecked);
        updateCarrierNetworkPreference(isChecked);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCarrierNetworkPreference = screen.findPreference(CARRIER_WIFI_NETWORK_PREF_KEY);
        updateCarrierNetworkPreference(isChecked());
    }

    @Override
    public void onWifiStateChanged() {
        if (mCarrierNetworkPreference != null && mCarrierNetworkPreference.isVisible()) {
            mCarrierNetworkPreference.setSummary(getCarrierNetworkSsid());
        }
    }

    @Override
    public void onWifiEntriesChanged() {
        if (mCarrierNetworkPreference != null && mCarrierNetworkPreference.isVisible()) {
            mCarrierNetworkPreference.setSummary(getCarrierNetworkSsid());
        }
    }

    @Override
    public void onNumSavedNetworksChanged() {
        // Do nothing
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
        // Do nothing
    }

    protected void updateCarrierNetworkPreference(boolean isCarrierNetworkEnabled) {
        if (mCarrierNetworkPreference == null) {
            return;
        }
        if (!isCarrierNetworkEnabled || getAvailabilityStatus() != AVAILABLE) {
            mCarrierNetworkPreference.setVisible(false);
            return;
        }
        mCarrierNetworkPreference.setVisible(true);
        mCarrierNetworkPreference.setSummary(getCarrierNetworkSsid());
    }

    protected String getCarrierNetworkSsid() {
        if (mWifiPickerTrackerHelper == null) {
            return null;
        }
        return mWifiPickerTrackerHelper.getCarrierNetworkSsid();
    }
}
