/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.TetheringManager;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.settings.Utils;

/**
 * This controller helps to manage the switch state and visibility of USB tether switch
 * preference. It stores preference values when preference changed.
 *
 */
public final class UsbTetherPreferenceController extends TetherBasePreferenceController implements
        LifecycleObserver {

    private static final String TAG = "UsbTetherPrefController";

    private boolean mUsbConnected;
    private boolean mMassStorageActive;
    private boolean mUsbTethering;

    public UsbTetherPreferenceController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public boolean isChecked() {
        return mUsbTethering;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mTetherEnabler == null) {
            return false;
        }
        if (isChecked) {
            mTetherEnabler.startTethering(TetheringManager.TETHERING_USB);
        } else {
            mTetherEnabler.stopTethering(TetheringManager.TETHERING_USB);
        }
        return true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_STATE);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        mContext.registerReceiver(mUsbChangeReceiver, filter);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mUsbChangeReceiver);
    }

    @Override
    public int getAvailabilityStatus() {
        String[] usbRegexs = mCm.getTetherableUsbRegexs();
        if (usbRegexs == null || usbRegexs.length == 0 || Utils.isMonkeyRunning()) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(mUsbConnected && !mMassStorageActive);
        }
    }

    @Override
    public void onTetherStateUpdated(int state) {
        mUsbTethering = TetherEnabler.isUsbTethering(state);
        updateState(mPreference);
    }

    @VisibleForTesting
    final BroadcastReceiver mUsbChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(Intent.ACTION_MEDIA_SHARED, action)) {
                mMassStorageActive = true;
            } else if (TextUtils.equals(Intent.ACTION_MEDIA_UNSHARED, action)) {
                mMassStorageActive = false;
            } else if (TextUtils.equals(UsbManager.ACTION_USB_STATE, action)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
            }
            updateState(mPreference);
        }
    };
}
