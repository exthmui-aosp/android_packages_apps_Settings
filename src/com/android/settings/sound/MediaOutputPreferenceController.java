/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.sound;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.media.MediaOutputSliceConstants;

import java.util.List;

/**
 * This class allows launching MediaOutputSlice to switch output device.
 * Preference would hide only when
 * - Bluetooth = OFF
 * - Bluetooth = ON and Connected Devices = 0 and Previously Connected = 0
 * - Media stream captured by remote device
 * - During a call.
 */
public class MediaOutputPreferenceController extends AudioSwitchPreferenceController {

    public MediaOutputPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            // In case UI is not ready.
            return;
        }

        if (Utils.isAudioModeOngoingCall(mContext)) {
            // Ongoing call status, switch entry for media will be disabled.
            mPreference.setVisible(false);
            preference.setSummary(
                    mContext.getText(R.string.media_out_summary_ongoing_call_state));
            return;
        }

        boolean deviceConnected = false;
        BluetoothDevice activeDevice = null;
        // Show preference if there is connected or previously connected device
        // Find active device and set its name as the preference's summary
        List<BluetoothDevice> connectedA2dpDevices = getConnectedA2dpDevices();
        List<BluetoothDevice> connectedHADevices = getConnectedHearingAidDevices();
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL
                && ((connectedA2dpDevices != null && !connectedA2dpDevices.isEmpty())
                || (connectedHADevices != null && !connectedHADevices.isEmpty()))) {
            deviceConnected = true;
            activeDevice = findActiveDevice();
        }
        mPreference.setVisible(deviceConnected);
        mPreference.setSummary((activeDevice == null) ?
                mContext.getText(R.string.media_output_default_summary) :
                activeDevice.getAliasName());
    }

    @Override
    public BluetoothDevice findActiveDevice() {
        BluetoothDevice activeDevice = findActiveHearingAidDevice();
        final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();

        if (activeDevice == null && a2dpProfile != null) {
            activeDevice = a2dpProfile.getActiveDevice();
        }
        return activeDevice;
    }

    /**
     * Find active hearing aid device
     */
    @Override
    protected BluetoothDevice findActiveHearingAidDevice() {
        final HearingAidProfile hearingAidProfile = mProfileManager.getHearingAidProfile();

        if (hearingAidProfile != null) {
            List<BluetoothDevice> activeDevices = hearingAidProfile.getActiveDevices();
            for (BluetoothDevice btDevice : activeDevices) {
                if (btDevice != null) {
                    return btDevice;
                }
            }
        }
        return null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            final Intent intent = new Intent()
                    .setAction(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            return true;
        }
        return false;
    }
}
