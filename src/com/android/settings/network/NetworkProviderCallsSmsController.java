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

import static androidx.lifecycle.Lifecycle.Event;

import android.content.Context;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.List;

public class NetworkProviderCallsSmsController extends AbstractPreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver {

    private static final String TAG = "NetworkProviderCallsSmsController";
    private static final String KEY = "calls_and_sms";
    private static final String PREFERRED_CALL_SMS = "preferred";
    private static final String PREFERRED_CALL = "calls preferred";
    private static final String PREFERRED_SMS = "SMS preferred";
    private static final String UNAVAILABLE = "unavailable";

    private UserManager mUserManager;
    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mSubscriptionsChangeListener;

    private RestrictedPreference mPreference;

    /**
     * The summary text and click behavior of the "Calls & SMS" item on the
     * Network & internet page.
     */
    public NetworkProviderCallsSmsController(Context context, Lifecycle lifecycle) {
        super(context);

        mUserManager = context.getSystemService(UserManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        if (lifecycle != null) {
            mSubscriptionsChangeListener = new SubscriptionsChangeListener(context, this);
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        mSubscriptionsChangeListener.start();
        update();
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        mSubscriptionsChangeListener.stop();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(
                mSubscriptionManager);

        if (subs.isEmpty()) {
            return null;
        } else {
            final StringBuilder summary = new StringBuilder();
            for (SubscriptionInfo subInfo : subs) {
                int subsSize = subs.size();

                // Set displayName as summary if there is only one valid SIM.
                if (subsSize == 1
                        && SubscriptionManager.isValidSubscriptionId(subInfo.getSubscriptionId())) {
                    return subInfo.getDisplayName();
                }

                CharSequence status = getPreferredStatus(subInfo);
                if (status.toString().isEmpty()) {
                    // If there are 2 or more SIMs and one of these has no preferred status,
                    // set only its displayName as summary.
                    summary.append(subInfo.getDisplayName());
                } else {
                    summary.append(subInfo.getDisplayName())
                            .append(" (")
                            .append(status)
                            .append(")");
                }
                // Do not add ", " for the last subscription.
                if (subInfo != subs.get(subs.size() - 1)) {
                    summary.append(", ");
                }
            }
            return summary;
        }
    }

    @VisibleForTesting
    protected CharSequence getPreferredStatus(SubscriptionInfo subInfo) {
        final int subId = subInfo.getSubscriptionId();
        String status = "";
        boolean isDataPreferred = subId == getDefaultVoiceSubscriptionId();
        boolean isSmsPreferred = subId == getDefaultSmsSubscriptionId();

        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            status = UNAVAILABLE;
        } else {
            if (isDataPreferred && isSmsPreferred) {
                status = PREFERRED_CALL_SMS;
            } else if (isDataPreferred) {
                status = PREFERRED_CALL;
            } else if (isSmsPreferred) {
                status = PREFERRED_SMS;
            }
        }
        return status;
    }

    @VisibleForTesting
    protected int getDefaultVoiceSubscriptionId(){
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    @VisibleForTesting
    protected int getDefaultSmsSubscriptionId(){
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    private void update() {
        if (mPreference == null || mPreference.isDisabledByAdmin()) {
            return;
        }
        refreshSummary(mPreference);
        mPreference.setOnPreferenceClickListener(null);
        mPreference.setFragment(null);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(
                mSubscriptionManager);
        if (subs.isEmpty()) {
            mPreference.setEnabled(false);
        } else {
            mPreference.setFragment(NetworkProviderCallsSmsFragment.class.getCanonicalName());
        }
    }

    @Override
    public boolean isAvailable() {
        return mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        update();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        refreshSummary(mPreference);
        update();
    }

    @Override
    public void onSubscriptionsChanged() {
        refreshSummary(mPreference);
        update();
    }
}
