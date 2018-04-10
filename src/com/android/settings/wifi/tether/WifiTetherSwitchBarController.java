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

import static android.net.ConnectivityManager.TETHERING_WIFI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;

import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class WifiTetherSwitchBarController implements SwitchWidgetController.OnSwitchChangeListener,
        LifecycleObserver, OnStart, OnStop {

    private static final IntentFilter WIFI_INTENT_FILTER;

    private final Context mContext;
    private final SwitchWidgetController mSwitchBar;
    private final ConnectivityManager mConnectivityManager;
    private final DataSaverBackend mDataSaverBackend;
    private final WifiManager mWifiManager;
    @VisibleForTesting
    final ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback =
            new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringFailed() {
                    super.onTetheringFailed();
                    mSwitchBar.setChecked(false);
                    updateWifiSwitch();
                }
            };

    static {
        WIFI_INTENT_FILTER = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        WIFI_INTENT_FILTER.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    }

    WifiTetherSwitchBarController(Context context, SwitchWidgetController switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mDataSaverBackend = new DataSaverBackend(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mSwitchBar.setChecked(mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED);
        mSwitchBar.setListener(this);
        updateWifiSwitch();
    }

    @Override
    public void onStart() {
        mSwitchBar.startListening();
        mContext.registerReceiver(mReceiver, WIFI_INTENT_FILTER);
    }

    @Override
    public void onStop() {
        mSwitchBar.stopListening();
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (isChecked) {
            startTether();
        } else {
            stopTether();
        }
        return true;
    }

    void stopTether() {
        mSwitchBar.setEnabled(false);
        mConnectivityManager.stopTethering(TETHERING_WIFI);
    }

    void startTether() {
        mSwitchBar.setEnabled(false);
        mConnectivityManager.startTethering(TETHERING_WIFI, false /* showProvisioningUi */,
                mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                final int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
                handleWifiApStateChanged(state);
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                updateWifiSwitch();
            }
        }
    };

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                if (!mSwitchBar.isChecked()) {
                    mSwitchBar.setChecked(true);
                }
                updateWifiSwitch();
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                if (mSwitchBar.isChecked()) {
                    mSwitchBar.setChecked(false);
                }
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mSwitchBar.setChecked(false);
                updateWifiSwitch();
                break;
            default:
                mSwitchBar.setChecked(false);
                updateWifiSwitch();
                break;
        }
    }

    private void updateWifiSwitch() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (!isAirplaneMode) {
            mSwitchBar.setEnabled(!mDataSaverBackend.isDataSaverEnabled());
        } else {
            mSwitchBar.setEnabled(false);
        }
    }
}
