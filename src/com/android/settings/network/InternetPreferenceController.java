/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.network.InternetUpdater.INTERNET_APM;
import static com.android.settings.network.InternetUpdater.INTERNET_APM_NETWORKS;
import static com.android.settings.network.InternetUpdater.INTERNET_CELLULAR;
import static com.android.settings.network.InternetUpdater.INTERNET_ETHERNET;
import static com.android.settings.network.InternetUpdater.INTERNET_WIFI;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.IdRes;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;
import com.android.settings.wifi.WifiSummaryUpdater;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * PreferenceController to update the internet state.
 */
public class InternetPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver, SummaryUpdater.OnSummaryChangeListener,
        InternetUpdater.OnInternetTypeChangedListener {

    public static final String KEY = "internet_settings";

    private Preference mPreference;
    private final WifiSummaryUpdater mSummaryHelper;
    private InternetUpdater mInternetUpdater;
    private @InternetUpdater.InternetType int mInternetType;

    @VisibleForTesting
    static Map<Integer, Integer> sIconMap = new HashMap<>();
    static {
        sIconMap.put(INTERNET_APM, R.drawable.ic_airplanemode_active);
        sIconMap.put(INTERNET_APM_NETWORKS, R.drawable.ic_airplane_safe_networks_24dp);
        sIconMap.put(INTERNET_WIFI, R.drawable.ic_wifi_signal_4);
        sIconMap.put(INTERNET_CELLULAR, R.drawable.ic_network_cell);
        sIconMap.put(INTERNET_ETHERNET, R.drawable.ic_settings_ethernet);
    }

    private static Map<Integer, Integer> sSummaryMap = new HashMap<>();
    static {
        sSummaryMap.put(INTERNET_APM, R.string.condition_airplane_title);
        sSummaryMap.put(INTERNET_APM_NETWORKS, R.string.airplane_mode_network_available);
        sSummaryMap.put(INTERNET_WIFI, 0);
        sSummaryMap.put(INTERNET_CELLULAR, 0);
        sSummaryMap.put(INTERNET_ETHERNET, R.string.to_switch_networks_disconnect_ethernet);
    }

    public InternetPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }
        mSummaryHelper = new WifiSummaryUpdater(mContext, this);
        mInternetUpdater = new InternetUpdater(context, lifecycle, this);
        mInternetType = mInternetUpdater.getInternetType();
        lifecycle.addObserver(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
    }

    @Override
    public void updateState(Preference preference) {
        if (mPreference == null) {
            return;
        }

        final @IdRes int icon = sIconMap.get(mInternetType);
        if (icon != 0) {
            final Drawable drawable = mContext.getDrawable(icon);
            if (drawable != null) {
                drawable.setTintList(
                        Utils.getColorAttr(mContext, android.R.attr.colorControlNormal));
                mPreference.setIcon(drawable);
            }
        }

        if (mustUseWiFiHelperSummary(mSummaryHelper.isWifiConnected(),
                mSummaryHelper.getSummary())) {
            return;
        }

        if (mInternetType == INTERNET_CELLULAR) {
            updateCellularSummary();
            return;
        }

        final @IdRes int summary = sSummaryMap.get(mInternetType);
        if (summary != 0) {
            mPreference.setSummary(summary);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mSummaryHelper.register(true);
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mSummaryHelper.register(false);
    }

    /**
     * Called when internet type is changed.
     *
     * @param internetType the internet type
     */
    public void onInternetTypeChanged(@InternetUpdater.InternetType int internetType) {
        final boolean needUpdate = (internetType != mInternetType);
        mInternetType = internetType;
        if (needUpdate) {
            ThreadUtils.postOnMainThread(() -> {
                updateState(mPreference);
            });
        }
    }

    @Override
    public void onSummaryChanged(String summary) {
        mustUseWiFiHelperSummary(mSummaryHelper.isWifiConnected(), summary);
    }

    @VisibleForTesting
    boolean mustUseWiFiHelperSummary(boolean isWifiConnected, String summary) {
        final boolean needUpdate = (mInternetType == INTERNET_WIFI)
                || (mInternetType == INTERNET_APM_NETWORKS && isWifiConnected);
        if (needUpdate && mPreference != null) {
            mPreference.setSummary(summary);
        }
        return needUpdate;
    }

    @VisibleForTesting
    void updateCellularSummary() {
        final SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        if (subscriptionManager == null) {
            return;
        }
        SubscriptionInfo subInfo = subscriptionManager.getDefaultDataSubscriptionInfo();
        if (subInfo == null) {
            return;
        }
        mPreference.setSummary(SubscriptionUtil.getUniqueSubscriptionDisplayName(
                subInfo, mContext));
    }
}
