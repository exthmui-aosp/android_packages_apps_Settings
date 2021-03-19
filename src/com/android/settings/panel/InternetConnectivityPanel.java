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

package com.android.settings.panel;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.network.NetworkProviderSettings.ACTION_NETWORK_PROVIDER_SETTINGS;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.network.InternetUpdater;
import com.android.settings.network.ProviderModelSliceHelper;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.telephony.DataConnectivityListener;
import com.android.settings.slices.CustomSliceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Internet Connectivity Panel.
 */
public class InternetConnectivityPanel implements PanelContent, LifecycleObserver,
        InternetUpdater.InternetChangeListener, DataConnectivityListener.Client,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "InternetConnectivityPanel";
    private static final int SUBTITLE_TEXT_NONE = -1;
    private static final int SUBTITLE_TEXT_WIFI_IS_TURNED_ON = R.string.wifi_is_turned_on_subtitle;
    private static final int SUBTITLE_TEXT_NON_CARRIER_NETWORK_UNAVAILABLE =
            R.string.non_carrier_network_unavailable;
    private static final int SUBTITLE_TEXT_ALL_CARRIER_NETWORK_UNAVAILABLE =
            R.string.all_network_unavailable;

    private final Context mContext;
    private final WifiManager mWifiManager;
    private final IntentFilter mWifiStateFilter;
    private final NetworkProviderTelephonyCallback mTelephonyCallback;
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (TextUtils.equals(intent.getAction(), WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    || TextUtils.equals(intent.getAction(),
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                updatePanelTitle();
            }
        }
    };

    @VisibleForTesting
    boolean mIsProviderModelEnabled;
    @VisibleForTesting
    InternetUpdater mInternetUpdater;
    @VisibleForTesting
    ProviderModelSliceHelper mProviderModelSliceHelper;

    private int mSubtitle = SUBTITLE_TEXT_NONE;
    private PanelContentCallback mCallback;
    private TelephonyManager mTelephonyManager;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private DataConnectivityListener mConnectivityListener;
    private int mDefaultDataSubid = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private InternetConnectivityPanel(Context context) {
        mContext = context.getApplicationContext();
        mIsProviderModelEnabled = Utils.isProviderModelEnabled(mContext);
        mInternetUpdater = new InternetUpdater(context, null /* Lifecycle */, this);

        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mConnectivityListener = new DataConnectivityListener(context, this);
        mTelephonyCallback = new NetworkProviderTelephonyCallback();
        mDefaultDataSubid = getDefaultDataSubscriptionId();
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);

        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiStateFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        mProviderModelSliceHelper = new ProviderModelSliceHelper(mContext, null);
    }

    /** create the panel */
    public static InternetConnectivityPanel create(Context context) {
        return new InternetConnectivityPanel(context);
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        if (!mIsProviderModelEnabled) {
            return;
        }
        mInternetUpdater.onResume();
        mSubscriptionsListener.start();
        mConnectivityListener.start();
        mTelephonyManager.registerTelephonyCallback(
                new HandlerExecutor(new Handler(Looper.getMainLooper())), mTelephonyCallback);
        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter);
        updatePanelTitle();
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (!mIsProviderModelEnabled) {
            return;
        }
        mInternetUpdater.onPause();
        mSubscriptionsListener.stop();
        mConnectivityListener.stop();
        mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
        mContext.unregisterReceiver(mWifiStateReceiver);
    }

    /**
     * @return a string for the title of the Panel.
     */
    @Override
    public CharSequence getTitle() {
        if (mIsProviderModelEnabled) {
            return mContext.getText(mInternetUpdater.isAirplaneModeOn()
                    ? R.string.airplane_mode : R.string.provider_internet_settings);
        }
        return mContext.getText(R.string.internet_connectivity_panel_title);
    }

    /**
     * @return a string for the subtitle of the Panel.
     */
    @Override
    public CharSequence getSubTitle() {
        if (mIsProviderModelEnabled && mSubtitle != SUBTITLE_TEXT_NONE) {
            return mContext.getText(mSubtitle);
        }
        return null;
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        if (mIsProviderModelEnabled) {
            uris.add(CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI);
            uris.add(CustomSliceRegistry.TURN_ON_WIFI_SLICE_URI);
        } else {
            uris.add(CustomSliceRegistry.WIFI_SLICE_URI);
            uris.add(CustomSliceRegistry.MOBILE_DATA_SLICE_URI);
            uris.add(AirplaneModePreferenceController.SLICE_URI);
        }
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return new Intent(mIsProviderModelEnabled
                ? ACTION_NETWORK_PROVIDER_SETTINGS : Settings.ACTION_WIRELESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    public boolean isCustomizedButtonUsed() {
        return mIsProviderModelEnabled;
    }

    @Override
    public CharSequence getCustomizedButtonTitle() {
        if (mInternetUpdater.isAirplaneModeOn() && !mInternetUpdater.isWifiEnabled()) {
            return null;
        }
        return mContext.getText(R.string.settings_button);
    }

    @Override
    public void onClickCustomizedButton() {
        mContext.startActivity(getSeeMoreIntent());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_INTERNET_CONNECTIVITY;
    }

    @Override
    public void registerCallback(PanelContentCallback callback) {
        mCallback = callback;
    }

    /**
     * Called when airplane mode state is changed.
     */
    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        updatePanelTitle();
    }

    /**
     * Called when Wi-Fi enabled is changed.
     */
    @Override
    public void onWifiEnabledChanged(boolean enabled) {
        updatePanelTitle();
    }

    @Override
    public void onSubscriptionsChanged() {
        final int defaultDataSubId = getDefaultDataSubscriptionId();
        log("onSubscriptionsChanged: defaultDataSubId:" + defaultDataSubId);
        if (mDefaultDataSubid == defaultDataSubId) {
            return;
        }
        if (SubscriptionManager.isUsableSubscriptionId(defaultDataSubId)) {
            mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
            mTelephonyManager.registerTelephonyCallback(
                    new HandlerExecutor(new Handler(Looper.getMainLooper())), mTelephonyCallback);
        }
        updatePanelTitle();
    }

    @Override
    public void onDataConnectivityChange() {
        log("onDataConnectivityChange");
        updatePanelTitle();
    }

    @VisibleForTesting
    void updatePanelTitle() {
        if (mCallback == null) {
            return;
        }
        updateSubtitleText();

        log("Subtitle:" + mSubtitle);
        if (mSubtitle != SUBTITLE_TEXT_NONE) {
            mCallback.onHeaderChanged();
        } else {
            // Other situations.
            //   Title: Airplane mode / Internet
            mCallback.onTitleChanged();
        }
        mCallback.onCustomizedButtonStateChanged();
    }

    @VisibleForTesting
    int getDefaultDataSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    private void updateSubtitleText() {
        mSubtitle = SUBTITLE_TEXT_NONE;
        if (!mInternetUpdater.isWifiEnabled()) {
            return;
        }

        if (mInternetUpdater.isAirplaneModeOn()) {
            // When the airplane mode is on and Wi-Fi is enabled.
            //   Title: Airplane mode
            //   Sub-Title: Wi-Fi is turned on
            log("Airplane mode is on + Wi-Fi on.");
            mSubtitle = SUBTITLE_TEXT_WIFI_IS_TURNED_ON;
            return;
        }

        final List<ScanResult> wifiList = mWifiManager.getScanResults();
        if (wifiList != null && wifiList.size() == 0) {
            // Sub-Title:
            // show non_carrier_network_unavailable
            //   - while Wi-Fi on + no Wi-Fi item
            // show all_network_unavailable:
            //   - while Wi-Fi on + no Wi-Fi item + no carrier
            //   - while Wi-Fi on + no Wi-Fi item + no data capability
            log("No Wi-Fi item.");
            mSubtitle = SUBTITLE_TEXT_NON_CARRIER_NETWORK_UNAVAILABLE;
            if (!mProviderModelSliceHelper.hasCarrier()
                    || !mProviderModelSliceHelper.isDataSimActive()) {
                log("No carrier item or no carrier data.");
                mSubtitle = SUBTITLE_TEXT_ALL_CARRIER_NETWORK_UNAVAILABLE;
            }
        }
    }

    private class NetworkProviderTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.DataConnectionStateListener,
            TelephonyCallback.ServiceStateListener {
        @Override
        public void onServiceStateChanged(ServiceState state) {
            log("onServiceStateChanged voiceState=" + state.getState()
                    + " dataState=" + state.getDataRegistrationState());
            updatePanelTitle();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            log("onDataConnectionStateChanged: networkType=" + networkType + " state=" + state);
            updatePanelTitle();
        }
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }
}
