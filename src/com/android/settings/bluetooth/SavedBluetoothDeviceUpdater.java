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
package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

/**
 * Maintain and update saved bluetooth devices(bonded but not connected)
 */
public class SavedBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "SavedBluetoothDeviceUpdater";
    private static final boolean DBG = false;

    private static final String PREF_KEY = "saved_bt";

    public SavedBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
    }

    @Override
    public void forceUpdate() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final CachedBluetoothDeviceManager cachedManager =
                    mLocalManager.getCachedDeviceManager();
            for (BluetoothDevice device
                    : BluetoothAdapter.getDefaultAdapter().getMostRecentlyConnectedDevices()) {
                final CachedBluetoothDevice cachedDevice = cachedManager.findDevice(device);
                if (isFilterMatched(cachedDevice)) {
                    // Add the preference if it is new one
                    addPreference(cachedDevice, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
                } else {
                    removePreference(cachedDevice);
                }
            }
        } else {
            removeAllDevicesFromPreference();
        }
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (DBG) {
            Log.d(TAG, "isFilterMatched() device name : " + cachedDevice.getName() +
                    ", is connected : " + device.isConnected() + ", is profile connected : "
                    + cachedDevice.isConnected());
        }
        return device.getBondState() == BluetoothDevice.BOND_BONDED && !device.isConnected();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mFragment.getMetricsCategory());
        final CachedBluetoothDevice device = ((BluetoothDevicePreference) preference)
                .getBluetoothDevice();
        device.connect();
        return true;
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }
}
