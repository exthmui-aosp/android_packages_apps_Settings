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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;

import com.google.common.annotations.VisibleForTesting;

/**
 * This controller helps to manage the switch state and visibility of bluetooth tether switch
 * preference. It stores preference value when preference changed.
 */
public final class BluetoothTetherPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "BluetoothTetherPreferenceController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private final ConnectivityManager mCm;
    private int mBluetoothState;
    private Preference mPreference;
    private final SharedPreferences mSharedPreferences;

    public BluetoothTetherPreferenceController(Context context, String prefKey) {
        super(context, prefKey);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mSharedPreferences =
                context.getSharedPreferences(TetherEnabler.SHARED_PREF, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isChecked() {
        return mSharedPreferences.getBoolean(mPreferenceKey, false);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (DEBUG) {
            Log.d(TAG, "preference changing to " + isChecked);
        }
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(mPreferenceKey, isChecked);
        editor.apply();
        return true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mBluetoothState = BluetoothAdapter.getDefaultAdapter().getState();
        mContext.registerReceiver(mBluetoothChangeReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mBluetoothChangeReceiver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }

        switch (mBluetoothState) {
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_OFF:
                // fall through.
            case BluetoothAdapter.ERROR:
                preference.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
            case BluetoothAdapter.STATE_TURNING_ON:
                // fall through.
            default:
                preference.setEnabled(false);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final String[] bluetoothRegexs = mCm.getTetherableBluetoothRegexs();
        if (bluetoothRegexs == null || bluetoothRegexs.length == 0) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @VisibleForTesting
    final BroadcastReceiver mBluetoothChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(BluetoothAdapter.ACTION_STATE_CHANGED, intent.getAction())) {
                mBluetoothState =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateState(mPreference);
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(mPreferenceKey, key)) {
            updateState(mPreference);
        }
    }
}
