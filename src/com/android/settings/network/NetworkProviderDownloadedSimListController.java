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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.mobile.dataservice.DataServiceUtils;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkProviderDownloadedSimListController extends
        AbstractPreferenceController implements
        LifecycleObserver, MobileNetworkRepository.MobileNetworkCallback {
    private static final String TAG = "NetworkProviderDownloadedSimListCtrl";
    private static final String KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_category";
    private static final String KEY_PREFERENCE_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_list";
    private static final String KEY_ADD_MORE = "add_more";

    private SubscriptionManager mSubscriptionManager;
    private PreferenceCategory mPreferenceCategory;
    private Map<Integer, Preference> mPreferences;
    private LifecycleOwner mLifecycleOwner;
    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();

    public NetworkProviderDownloadedSimListController(Context context, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner) {
        super(context);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mPreferences = new ArrayMap<>();
        mLifecycleOwner = lifecycleOwner;
        mMobileNetworkRepository = MobileNetworkRepository.create(context, this);
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner);
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mMobileNetworkRepository.removeRegister();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM);
        screen.findPreference(KEY_ADD_MORE).setVisible(
                MobileNetworkUtils.showEuiccSettings(mContext));
        update();
    }

    private void update() {
        if (mPreferenceCategory == null) {
            return;
        }

        final Map<Integer, Preference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();

        final List<SubscriptionInfoEntity> subscriptions = getAvailableDownloadedSubscriptions();
        for (SubscriptionInfoEntity info : subscriptions) {
            final int subId = Integer.parseInt(info.subId);
            Preference pref = existingPreferences.remove(subId);
            if (pref == null) {
                pref = new Preference(mPreferenceCategory.getContext());
                mPreferenceCategory.addPreference(pref);
            }
            final CharSequence displayName = info.uniqueName;
            pref.setTitle(displayName);
            pref.setSummary(getSummary(info));

            pref.setOnPreferenceClickListener(clickedPref -> {
                MobileNetworkUtils.launchMobileNetworkSettings(mContext, info);
                return true;
            });
            mPreferences.put(subId, pref);
        }
        for (Preference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    public CharSequence getSummary(SubscriptionInfoEntity subInfo) {
        if (subInfo.isActiveSubscriptionId) {
            CharSequence config = subInfo.defaultSimConfig;
            CharSequence summary = mContext.getResources().getString(
                    R.string.sim_category_active_sim);
            if (config == "") {
                return summary;
            } else {
                final StringBuilder activeSim = new StringBuilder();
                activeSim.append(summary).append(config);
                return activeSim;
            }
        } else {
            return mContext.getString(R.string.sim_category_inactive_sim);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!getAvailableDownloadedSubscriptions().isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREFERENCE_DOWNLOADED_SIM;
    }

    @VisibleForTesting
    protected List<SubscriptionInfoEntity> getAvailableDownloadedSubscriptions() {
        List<SubscriptionInfoEntity> subList = new ArrayList<>();
        for (SubscriptionInfoEntity info : mSubInfoEntityList) {
            if (info.isEmbedded) {
                subList.add(info);
            }
        }
        return subList;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(mPreferenceCategory);
        update();
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        if (DataServiceUtils.shouldUpdateEntityList(mSubInfoEntityList, subInfoEntityList)) {
            mSubInfoEntityList = subInfoEntityList;
            mPreferenceCategory.setVisible(isAvailable());
            update();
        }
    }

    @Override
    public void onActiveSubInfoChanged(List<SubscriptionInfoEntity> activeSubInfoList) {
    }

    @Override
    public void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
    }

    @Override
    public void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
    }
}
