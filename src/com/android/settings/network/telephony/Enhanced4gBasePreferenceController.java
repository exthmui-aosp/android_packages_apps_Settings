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

package com.android.settings.network.telephony;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.ims.VolteQueryImsState;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Enhanced 4G LTE"
 */
public class Enhanced4gBasePreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "Enhanced4g";

    @VisibleForTesting
    Preference mPreference;
    private PhoneCallStateListener mPhoneStateListener;
    private boolean mShow5gLimitedDialog;
    private boolean mHas5gCapability;
    @VisibleForTesting
    Integer mCallState;
    private final List<On4gLteUpdateListener> m4gLteListeners;

    protected static final int MODE_NONE = -1;
    protected static final int MODE_VOLTE = 0;
    protected static final int MODE_ADVANCED_CALL = 1;
    protected static final int MODE_4G_CALLING = 2;
    private int m4gCurrentMode = MODE_NONE;

    public Enhanced4gBasePreferenceController(Context context, String key) {
        super(context, key);
        m4gLteListeners = new ArrayList<>();
        mPhoneStateListener = new PhoneCallStateListener();
    }

    public Enhanced4gBasePreferenceController init(int subId) {
        if (mSubId == subId) {
            return this;
        }
        mSubId = subId;
        final PersistableBundle carrierConfig = getCarrierConfigForSubId(subId);
        if (carrierConfig == null) {
            return this;
        }

        final boolean show4GForLTE = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        m4gCurrentMode = carrierConfig.getInt(
                CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT);
        if (m4gCurrentMode != MODE_ADVANCED_CALL) {
            m4gCurrentMode = show4GForLTE ? MODE_4G_CALLING : MODE_VOLTE;
        }

        mShow5gLimitedDialog = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_VOLTE_5G_LIMITED_ALERT_DIALOG_BOOL);
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        init(subId);
        if (!isModeMatched()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        final PersistableBundle carrierConfig = getCarrierConfigForSubId(subId);
        if ((carrierConfig == null)
                || carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL)) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        final VolteQueryImsState queryState = queryImsState(subId);
        if (!queryState.isReadyToVoLte()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return (isUserControlAllowed(carrierConfig) && queryState.isAllowUserControl())
                ? AVAILABLE : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mPhoneStateListener.register(mContext, mSubId);
    }

    @Override
    public void onStop() {
        mPhoneStateListener.unregister();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        final SwitchPreference switchPreference = (SwitchPreference) preference;

        final VolteQueryImsState queryState = queryImsState(mSubId);
        switchPreference.setEnabled(isUserControlAllowed(getCarrierConfigForSubId(mSubId))
                && queryState.isAllowUserControl());
        switchPreference.setChecked(queryState.isEnabledByUser()
                && queryState.isAllowUserControl());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(mSubId);
        if (imsMmTelManager == null) {
            return false;
        }

        if (isDialogNeeded() && !isChecked) {
            show5gLimitedDialog(imsMmTelManager);
        } else {
            return setAdvancedCallingSettingEnabled(imsMmTelManager, isChecked);
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        final VolteQueryImsState queryState = queryImsState(mSubId);
        return queryState.isEnabledByUser();
    }

    public Enhanced4gBasePreferenceController addListener(On4gLteUpdateListener lsn) {
        m4gLteListeners.add(lsn);
        return this;
    }

    protected int getMode() {
        return MODE_NONE;
    }

    private boolean isModeMatched() {
        return m4gCurrentMode == getMode();
    }

    @VisibleForTesting
    VolteQueryImsState queryImsState(int subId) {
        return new VolteQueryImsState(mContext, subId);
    }

    private boolean isUserControlAllowed(final PersistableBundle carrierConfig) {
        return (mCallState != null) && (mCallState == TelephonyManager.CALL_STATE_IDLE)
                && (carrierConfig != null)
                && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
    }

    private class PhoneCallStateListener extends PhoneStateListener {

        PhoneCallStateListener() {
            super(Looper.getMainLooper());
        }

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);

            final long supportedRadioBitmask = mTelephonyManager.getSupportedRadioAccessFamily();
            mHas5gCapability =
                    (supportedRadioBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_NR) > 0;
        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }

    /**
     * Update other preferences when 4gLte state is changed
     */
    public interface On4gLteUpdateListener {
        void on4gLteUpdated();
    }

    private boolean isDialogNeeded() {
        Log.d(TAG, "Has5gCapability:" + mHas5gCapability);
        return mShow5gLimitedDialog && mHas5gCapability;
    }

    private void show5gLimitedDialog(ImsMmTelManager imsMmTelManager) {
        Log.d(TAG, "show5gLimitedDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        DialogInterface.OnClickListener networkSettingsClickListener =
                new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick,isChecked:false");
                        setAdvancedCallingSettingEnabled(imsMmTelManager, false);
                        updateState(mPreference);
                    }
                };
        builder.setTitle(R.string.volte_5G_limited_title)
                .setMessage(R.string.volte_5G_limited_text)
                .setNeutralButton(mContext.getResources().getString(
                        R.string.cancel), null)
                .setPositiveButton(mContext.getResources().getString(
                        R.string.condition_turn_off),
                        networkSettingsClickListener)
                .create()
                .show();
    }

    private boolean setAdvancedCallingSettingEnabled(ImsMmTelManager imsMmTelManager,
            boolean isChecked) {
        try {
            imsMmTelManager.setAdvancedCallingSettingEnabled(isChecked);
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "fail to set VoLTE=" + isChecked + ". subId=" + mSubId, exception);
            return false;
        }
        for (final On4gLteUpdateListener lsn : m4gLteListeners) {
            lsn.on4gLteUpdated();
        }
        return true;
    }
}
