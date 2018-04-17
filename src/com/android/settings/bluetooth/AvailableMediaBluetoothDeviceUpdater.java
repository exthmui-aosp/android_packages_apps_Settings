/*
 * Copyright 2018 The Android Open Source Project
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

import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

/**
 * Controller to maintain available media Bluetooth devices
 */
public class AvailableMediaBluetoothDeviceUpdater extends BluetoothDeviceUpdater {

    private static final String TAG = "AvailableMediaBluetoothDeviceUpdater";
    private static final boolean DBG = false;

    private final AudioManager mAudioManager;

    public AvailableMediaBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @VisibleForTesting
    AvailableMediaBluetoothDeviceUpdater(DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback,
            LocalBluetoothManager localBluetoothManager) {
        super(fragment, devicePreferenceCallback, localBluetoothManager);
        mAudioManager = (AudioManager) fragment.getContext().
                getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onAudioModeChanged() {
        forceUpdate();
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        if (DBG) {
            Log.d(TAG,"onConnectionStateChanged() device : " +
                    cachedDevice.getName() + ", state : " + state);
        }
        if (state == BluetoothAdapter.STATE_CONNECTED) {
            if (isFilterMatched(cachedDevice)) {
                addPreference(cachedDevice);
            } else {
                removePreference(cachedDevice);
            }
        } else if (state == BluetoothAdapter.STATE_DISCONNECTED) {
            removePreference(cachedDevice);
        }
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final int audioMode = mAudioManager.getMode();
        final int currentAudioProfile;

        if (audioMode == AudioManager.MODE_RINGTONE
                || audioMode == AudioManager.MODE_IN_CALL
                || audioMode == AudioManager.MODE_IN_COMMUNICATION) {
            // in phone call
            currentAudioProfile = BluetoothProfile.HEADSET;
        } else {
            // without phone call
            currentAudioProfile = BluetoothProfile.A2DP;
        }

        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice)) {
            if (DBG) {
                Log.d(TAG, "isFilterMatched() current audio profile : " + currentAudioProfile);
            }
            // According to the current audio profile type,
            // this page will show the bluetooth device that have corresponding profile.
            // For example:
            // If current audio profile is a2dp, show the bluetooth device that have a2dp profile.
            // If current audio profile is headset,
            // show the bluetooth device that have headset profile.
            switch (currentAudioProfile) {
                case BluetoothProfile.A2DP:
                    isFilterMatched = cachedDevice.isA2dpDevice();
                    break;
                case BluetoothProfile.HEADSET:
                    isFilterMatched = cachedDevice.isHfpDevice();
                    break;
            }
            if (DBG) {
                Log.d(TAG, "isFilterMatched() device : " +
                        cachedDevice.getName() + ", isFilterMatched : " + isFilterMatched);
            }
        }
        return isFilterMatched;
    }
}

