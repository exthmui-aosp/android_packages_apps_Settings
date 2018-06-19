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

package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPreference;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

/**
 * This class controls the radio buttons for choosing between different USB functions.
 */
public class UsbDetailsFunctionsController extends UsbDetailsController
        implements RadioButtonPreference.OnClickListener {

    static final Map<Long, Integer> FUNCTIONS_MAP = new LinkedHashMap<>();

    static {
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_MTP, R.string.usb_use_file_transfers);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_RNDIS, R.string.usb_use_tethering);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_MIDI, R.string.usb_use_MIDI);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_PTP, R.string.usb_use_photo_transfers);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_NONE, R.string.usb_use_charging_only);
    }

    private PreferenceCategory mProfilesContainer;

    public UsbDetailsFunctionsController(Context context, UsbDetailsFragment fragment,
            UsbBackend backend) {
        super(context, fragment, backend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mProfilesContainer = (PreferenceCategory) screen.findPreference(getPreferenceKey());
    }

    /**
     * Gets a switch preference for the particular option, creating it if needed.
     */
    private RadioButtonPreference getProfilePreference(String key, int titleId) {
        RadioButtonPreference pref = (RadioButtonPreference) mProfilesContainer.findPreference(key);
        if (pref == null) {
            pref = new RadioButtonPreference(mProfilesContainer.getContext());
            pref.setKey(key);
            pref.setTitle(titleId);
            pref.setOnClickListener(this);
            mProfilesContainer.addPreference(pref);
        }
        return pref;
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (!connected || dataRole != UsbPort.DATA_ROLE_DEVICE) {
            mProfilesContainer.setEnabled(false);
        } else {
            // Functions are only available in device mode
            mProfilesContainer.setEnabled(true);
        }
        RadioButtonPreference pref;
        for (long option : FUNCTIONS_MAP.keySet()) {
            int title = FUNCTIONS_MAP.get(option);
            pref = getProfilePreference(UsbBackend.usbFunctionsToString(option), title);
            // Only show supported options
            if (mUsbBackend.areFunctionsSupported(option)) {
                pref.setChecked(functions == option);
            } else {
                mProfilesContainer.removePreference(pref);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        long function = UsbBackend.usbFunctionsFromString(preference.getKey());
        if (function != mUsbBackend.getCurrentFunctions() && !Utils.isMonkeyRunning()) {
            mUsbBackend.setCurrentFunctions(function);
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_functions";
    }
}
