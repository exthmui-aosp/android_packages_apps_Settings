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

package com.android.settings.network.telephony;

import android.content.Intent;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

/**
 * List of Phone-specific settings screens.
 */
public class CdmaOptions {
    private static final String LOG_TAG = "CdmaOptions";

    private CarrierConfigManager mCarrierConfigManager;
    private RestrictedPreference mButtonAPNExpand;
    private Preference mCategoryAPNExpand;
    private Preference mButtonCarrierSettings;

    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_APN_EXPAND_KEY = "button_cdma_apn_key";
    private static final String CATEGORY_APN_EXPAND_KEY = "category_cdma_apn_key";

    private PreferenceFragmentCompat mPrefFragment;
    private PreferenceScreen mPrefScreen;
    private int mSubId;

    public CdmaOptions(PreferenceFragmentCompat prefFragment, PreferenceScreen prefScreen, int subId) {
        mPrefFragment = prefFragment;
        mPrefScreen = prefScreen;
        mPrefFragment.addPreferencesFromResource(R.xml.cdma_options);
        mCarrierConfigManager = new CarrierConfigManager(prefFragment.getContext());

        // Initialize preferences.
        mButtonCarrierSettings = mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        mButtonAPNExpand = (RestrictedPreference) mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        mCategoryAPNExpand = mPrefScreen.findPreference(CATEGORY_APN_EXPAND_KEY);

        updateSubscriptionId(subId);
    }

    protected void updateSubscriptionId(int subId) {
        mSubId = subId;
        int phoneType = TelephonyManager.from(mPrefFragment.getContext())
                .createForSubscriptionId(mSubId).getPhoneType();

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        // Some CDMA carriers want the APN settings.
        boolean addAPNExpand = shouldAddApnExpandPreference(phoneType, carrierConfig);
        // Read platform settings for carrier settings
        boolean addCarrierSettings =
                carrierConfig.getBoolean(CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL);

        // Making no assumptions of whether they are added or removed at this point.
        // Calling add or remove explicitly to make sure they are updated.

        if (addAPNExpand) {
            log("update: addAPNExpand");
            mButtonAPNExpand.setDisabledByAdmin(
                    MobileNetworkUtils.isDpcApnEnforced(mButtonAPNExpand.getContext())
                            ? RestrictedLockUtilsInternal.getDeviceOwner(
                                    mButtonAPNExpand.getContext())
                            : null);
            mButtonAPNExpand.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            MetricsLogger.action(mButtonAPNExpand.getContext(),
                                    MetricsEvent.ACTION_MOBILE_NETWORK_APN_SETTINGS);
                            // We need to build the Intent by hand as the Preference Framework
                            // does not allow to add an Intent with some extras into a Preference
                            // XML file
                            final Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
                            // This will setup the Home and Search affordance
                            intent.putExtra(":settings:show_fragment_as_subsetting", true);
                            intent.putExtra("sub_id", mSubId);
                            mPrefFragment.startActivity(intent);
                            return true;
                        }
                    });
            mPrefScreen.addPreference(mCategoryAPNExpand);
        } else {
            mPrefScreen.removePreference(mCategoryAPNExpand);
        }

        if (addCarrierSettings) {
            mPrefScreen.addPreference(mButtonCarrierSettings);
        } else {
            mPrefScreen.removePreference(mButtonCarrierSettings);
        }
    }

    /**
     * Return whether we should add the APN expandable preference based on the phone type and
     * carrier config
     */
    @VisibleForTesting
    public static boolean shouldAddApnExpandPreference(int phoneType, PersistableBundle config) {
        return phoneType == PhoneConstants.PHONE_TYPE_CDMA
                && config.getBoolean(CarrierConfigManager.KEY_SHOW_APN_SETTING_CDMA_BOOL);
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            log("preferenceTreeClick: return BUTTON_CDMA_ROAMING_KEY true");
            return true;
        }
        if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            log("preferenceTreeClick: return CDMA_SUBSCRIPTION_KEY true");
            return true;
        }
        return false;
    }

    protected void log(String s) {
        android.util.Log.d(LOG_TAG, s);
    }
}
