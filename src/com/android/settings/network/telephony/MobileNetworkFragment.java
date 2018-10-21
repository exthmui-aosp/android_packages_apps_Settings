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

import static android.provider.Telephony.Carriers.ENFORCE_MANAGED_URI;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.telephony.cdma.CdmaApnPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSubscriptionPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSystemSelectPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkFragment extends DashboardFragment implements
        Preference.OnPreferenceChangeListener {

    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    private static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
    private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CARRIER_SETTINGS_EUICC_KEY =
            "carrier_settings_euicc_key";
    private static final String BUTTON_WIFI_CALLING_KEY = "wifi_calling_key";
    private static final String BUTTON_VIDEO_CALLING_KEY = "video_calling_key";
    private static final String BUTTON_MOBILE_DATA_ENABLE_KEY = "mobile_data_enable";
    private static final String BUTTON_DATA_USAGE_KEY = "data_usage_summary";
    private static final String BUTTON_ADVANCED_OPTIONS_KEY = "advanced_options";
    private static final String CATEGORY_CALLING_KEY = "calling";
    private static final String CATEGORY_GSM_APN_EXPAND_KEY = "category_gsm_apn_key";
    private static final String CATEGORY_CDMA_APN_EXPAND_KEY = "category_cdma_apn_key";
    private static final String BUTTON_GSM_APN_EXPAND_KEY = "button_gsm_apn_key";
    private static final String BUTTON_CDMA_APN_EXPAND_KEY = "button_cdma_apn_key";

    private static final String EXTRA_EXIT_ECM_RESULT = "exit_ecm_result";
    private static final String LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT =
            "android.telecom.action.CONNECTION_SERVICE_CONFIGURE";

    private final BroadcastReceiver
            mPhoneChangeReceiver = new PhoneChangeReceiver();
    private final ContentObserver
            mDpcEnforcedContentObserver = new DpcApnEnforcedObserver();

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    private int mSubId;

    //UI objects
    private SwitchPreference mButton4glte;
    private Preference mEuiccSettingsPref;
    private PreferenceCategory mCallingCategory;
    private Preference mWiFiCallingPref;
    private SwitchPreference mVideoCallingPref;
    private NetworkSelectListPreference mButtonNetworkSelect;

    private CdmaSystemSelectPreferenceController mCdmaSystemSelectPreferenceController;
    private CdmaSubscriptionPreferenceController mCdmaSubscriptionPreferenceController;

    private static final String iface = "rmnet0"; //TODO: this will go away
    private List<SubscriptionInfo> mActiveSubInfos;

    private UserManager mUm;
    private ImsManager mImsMgr;
    private boolean mOkClicked;

    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    private boolean mShow4GForLTE;
    private boolean mIsGlobalCdma;
    private boolean mOnlyAutoSelectInHomeNW;
    private boolean mUnavailable;

    private class PhoneCallStateListener extends PhoneStateListener {
        /*
         * Enable/disable the 'Enhanced 4G LTE Mode' when in/out of a call
         * and depending on TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);

            updateWiFiCallState();
            updateVideoCallState();
            updatePreferredNetworkType();
        }

        /**
         * Listen to different subId if it's changed.
         */
        protected void updateSubscriptionId(Integer subId) {
            if (subId.equals(PhoneCallStateListener.this.mSubId)) {
                return;
            }

            PhoneCallStateListener.this.mSubId = subId;

            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);

            // Now, listen to new subId if it's valid.
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    private final PhoneCallStateListener
            mPhoneStateListener = new PhoneCallStateListener();

    @Override
    public int getMetricsCategory() {
        //TODO(b/114749736): add metrics id for it
        return 0;
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (super.onPreferenceTreeClick(preference)) {
            return true;
        }
        sendMetricsEventPreferenceClicked(getPreferenceScreen(), preference);

        /** TODO: Refactor and get rid of the if's using subclasses */
        if (preference.getKey().equals(BUTTON_4G_LTE_KEY)) {
            return true;
        } else if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mCdmaOptions != null &&
                mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (mTelephonyManager.getEmergencyCallbackMode()) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                        new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mEuiccSettingsPref) {
            Intent intent = new Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS);
            startActivity(intent);
            return true;
        } else if (preference == mWiFiCallingPref || preference == mVideoCallingPref) {
            return false;
        }

        return true;
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener
            mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");
            initializeSubscriptions();
        }
    };

    private void initializeSubscriptions() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            // Process preferences in activity only if its not destroyed
            return;
        }
        updatePhone();
        updateBody();
        if (DBG) log("initializeSubscriptions:-");
    }

    private void updatePhone() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mImsMgr = ImsManager.getInstance(getContext(),
                    SubscriptionManager.getPhoneId(mSubId));
            mTelephonyManager = new TelephonyManager(getContext(), mSubId);
        }

        mPhoneStateListener.updateSubscriptionId(mSubId);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSubId = getArguments().getInt(MobileSettingsActivity.KEY_SUBSCRIPTION_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        use(MobileDataPreferenceController.class).init(getFragmentManager(), mSubId);
        use(RoamingPreferenceController.class).init(getFragmentManager(), mSubId);
        use(CdmaApnPreferenceController.class).init(mSubId);
        use(CarrierPreferenceController.class).init(mSubId);
        use(DataUsagePreferenceController.class).init(mSubId);
        use(PreferredNetworkModePreferenceController.class).init(mSubId);
        use(EnabledNetworkModePreferenceController.class).init(mSubId);
        use(Enhanced4gLtePreferenceController.class).init(mSubId);
        use(DataServiceSetupPreferenceController.class).init(mSubId);

        mCdmaSystemSelectPreferenceController = use(CdmaSystemSelectPreferenceController.class);
        mCdmaSystemSelectPreferenceController.init(getPreferenceManager(), mSubId);
        mCdmaSubscriptionPreferenceController = use(CdmaSubscriptionPreferenceController.class);
        mCdmaSubscriptionPreferenceController.init(getPreferenceManager(), mSubId);
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate:+");
        super.onCreate(icicle);
        final Context context = getContext();

        if (context == null) {
            Log.e(LOG_TAG, "onCreate:- with no valid activity.");
            return;
        }

        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(context);
        mTelephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        mCarrierConfigManager = new CarrierConfigManager(getContext());

        mButton4glte = (SwitchPreference)findPreference(BUTTON_4G_LTE_KEY);

        mCallingCategory = (PreferenceCategory) findPreference(CATEGORY_CALLING_KEY);
        mWiFiCallingPref = findPreference(BUTTON_WIFI_CALLING_KEY);
        mVideoCallingPref = (SwitchPreference) findPreference(BUTTON_VIDEO_CALLING_KEY);

        try {
            Context con = context.createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "NameNotFoundException for show4GFotLTE");
            mShow4GForLTE = false;
        }

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mEuiccSettingsPref = prefSet.findPreference(BUTTON_CARRIER_SETTINGS_EUICC_KEY);
        mEuiccSettingsPref.setOnPreferenceChangeListener(this);

        // Initialize mActiveSubInfo
        int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        mActiveSubInfos = mSubscriptionManager.getActiveSubscriptionInfoList();

        updatePhone();
        Log.i(LOG_TAG, "onCreate:-");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
                || !mUm.isSystemUser()) {
            mUnavailable = true;
            //TODO(b/114749736): migrate telephony_disallowed_preference_screen.xml
        } else {
            initializeSubscriptions();
        }
    }

    private class PhoneChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "onReceive:");
            if (getActivity() == null || getContext() == null) {
                // Received broadcast and activity is in the process of being torn down.
                return;
            }
            // When the radio changes (ex: CDMA->GSM), refresh all options.
            updateBody();
        }
    }

    private class DpcApnEnforcedObserver extends ContentObserver {
        DpcApnEnforcedObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.i(LOG_TAG, "DPC enforced onChange:");
            if (getActivity() == null || getContext() == null) {
                // Received content change and activity is in the process of being torn down.
                return;
            }
            updateBody();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume:+");

        if (mUnavailable) {
            Log.i(LOG_TAG, "onResume:- ignore mUnavailable == false");
            return;
        }

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        getPreferenceScreen().setEnabled(true);

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        // Video calling and WiFi calling state might have changed.
        updateCallingCategory();

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        final Context context = getContext();
        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        context.registerReceiver(mPhoneChangeReceiver, intentFilter);
        context.getContentResolver().registerContentObserver(ENFORCE_MANAGED_URI, false,
                mDpcEnforcedContentObserver);

        Log.i(LOG_TAG, "onResume:-");

    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_setting_fragment;
    }

    @Override
    protected String getLogTag() {
        return null;
    }

    private boolean hasActiveSubscriptions() {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    private void updateBodyBasicFields(FragmentActivity activity, PreferenceScreen prefSet,
            int phoneSubId, boolean hasActiveSubscriptions) {
        Context context = getContext();

        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void updateBody() {
        final FragmentActivity activity = getActivity();
        final PreferenceScreen prefSet = getPreferenceScreen();
        final boolean hasActiveSubscriptions = hasActiveSubscriptions();

        if (activity == null) {
            Log.e(LOG_TAG, "updateBody with no valid activity.");
            return;
        }

        if (prefSet == null) {
            Log.e(LOG_TAG, "updateBody with no null prefSet.");
            return;
        }

        updateBodyBasicFields(activity, prefSet, mSubId, hasActiveSubscriptions);

        if (hasActiveSubscriptions) {
            updateBodyAdvancedFields(activity, prefSet, mSubId, hasActiveSubscriptions);
        } else {
            // Shows the "Carrier" preference that allows user to add a e-sim profile.
            if (MobileNetworkUtils.showEuiccSettings(getContext())) {
                mEuiccSettingsPref.setSummary(null /* summary */);
                prefSet.addPreference(mEuiccSettingsPref);
            }
        }
    }

    private void updateBodyAdvancedFields(FragmentActivity activity, PreferenceScreen prefSet,
            int phoneSubId, boolean hasActiveSubscriptions) {
        boolean isLteOnCdma = mTelephonyManager.getLteOnCdmaMode()
                == PhoneConstants.LTE_ON_CDMA_TRUE;

        if (DBG) {
            log("updateBody: isLteOnCdma=" + isLteOnCdma + " phoneSubId=" + phoneSubId);
        }

        if (MobileNetworkUtils.showEuiccSettings(getContext())) {
            prefSet.addPreference(mEuiccSettingsPref);
            String spn = mTelephonyManager.getSimOperatorName();
            if (TextUtils.isEmpty(spn)) {
                mEuiccSettingsPref.setSummary(null);
            } else {
                mEuiccSettingsPref.setSummary(spn);
            }
        }

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                preferredNetworkMode);

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        mIsGlobalCdma = isLteOnCdma
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
        if (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
        } else if (carrierConfig.getBoolean(CarrierConfigManager
                .KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)
                && !mTelephonyManager.getServiceState().getRoaming()
                && mTelephonyManager.getServiceState().getDataRegState()
                == ServiceState.STATE_IN_SERVICE) {

            final int phoneType = mTelephonyManager.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                updateCdmaOptions(this, prefSet, mSubId);
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                updateGsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            // Since pref is being hidden from user, set network mode to default
            // in case it is currently something else. That is possible if user
            // changed the setting while roaming and is now back to home network.
            settingsNetworkMode = preferredNetworkMode;
        } else if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.

            updateCdmaOptions(this, prefSet, mSubId);
            updateGsmUmtsOptions(this, prefSet, phoneSubId);
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Global.getString(activity.getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));

        updatePreferredNetworkType();
        updateCallingCategory();

        // Enable link to CMAS app settings depending on the value in config.xml.
        final boolean isCellBroadcastAppLinkEnabled = activity.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        if (!mUm.isAdminUser() || !isCellBroadcastAppLinkEnabled
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
            if (ps != null) {
                root.removePreference(ps);
            }
        }

        mOnlyAutoSelectInHomeNW = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL);
        Preference ps;
        ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(CATEGORY_GSM_APN_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(CATEGORY_CDMA_APN_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(CATEGORY_CALLING_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(NetworkOperators.BUTTON_AUTO_SELECT_KEY);
        if (ps != null) {
            ps.setSummary(null);
            if (mTelephonyManager.getServiceState().getRoaming()) {
                ps.setEnabled(true);
            } else {
                ps.setEnabled(!mOnlyAutoSelectInHomeNW);
                if (mOnlyAutoSelectInHomeNW) {
                    ps.setSummary(getResources().getString(
                            R.string.manual_mode_disallowed_summary,
                            mTelephonyManager.getSimOperatorName()));
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DBG) log("onPause:+");

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        mSubscriptionManager
                .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        final Context context = getActivity();
        context.unregisterReceiver(mPhoneChangeReceiver);
        context.getContentResolver().unregisterContentObserver(mDpcEnforcedContentObserver);
        if (DBG) log("onPause:-");
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        sendMetricsEventPreferenceChanged(getPreferenceScreen(), preference, objValue);
        if (preference == mVideoCallingPref) {
            // If mButton4glte is not checked, mVideoCallingPref should be disabled.
            // So it only makes sense to call phoneMgr.enableVideoCalling if it's checked.
            if (mButton4glte.isChecked()) {
                mImsMgr.setVtSetting((boolean) objValue);
                return true;
            } else {
                loge("mVideoCallingPref should be disabled if mButton4glte is not checked.");
                mVideoCallingPref.setEnabled(false);
                return false;
            }
        }

        updateBody();
        // always let the preference setting proceed.
        return true;
    }

    private boolean is4gLtePrefEnabled(PersistableBundle carrierConfig) {
        return (mTelephonyManager.getCallState(mSubId)
                == TelephonyManager.CALL_STATE_IDLE)
                && mImsMgr != null
                && mImsMgr.isNonTtyOrTtyOnVolteEnabled()
                && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_EXIT_ECM:
                Boolean isChoiceYes = data.getBooleanExtra(
                        EXTRA_EXIT_ECM_RESULT, false);
                if (isChoiceYes) {
                    // If the phone exits from ECM mode, show the CDMA Options
                    final String key = mClickedPreference.getKey();
                    if (TextUtils.equals(key,
                            mCdmaSystemSelectPreferenceController.getPreferenceKey())) {
                        mCdmaSystemSelectPreferenceController.showDialog();
                    } else if (TextUtils.equals(key,
                            mCdmaSubscriptionPreferenceController.getPreferenceKey())) {
                        mCdmaSubscriptionPreferenceController.showDialog();
                    }
                } else {
                    // do nothing
                }
                break;

            default:
                break;
        }
    }

    private void updateWiFiCallState() {
        if (mWiFiCallingPref == null || mCallingCategory == null) {
            return;
        }

        // Removes the preference if the wifi calling is disabled.
        if (!MobileNetworkUtils.isWifiCallingEnabled(getContext(),
                SubscriptionManager.getPhoneId(mSubId))) {
            mCallingCategory.removePreference(mWiFiCallingPref);
            return;
        }

        final PhoneAccountHandle simCallManager =
                TelecomManager.from(getContext()).getSimCallManager();

        if (simCallManager != null) {
            Intent intent = buildPhoneAccountConfigureIntent(getContext(), simCallManager);
            PackageManager pm = getContext().getPackageManager();
            List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
            mWiFiCallingPref.setTitle(resolutions.get(0).loadLabel(pm));
            mWiFiCallingPref.setSummary(null);
            mWiFiCallingPref.setIntent(intent);
        } else {
            int resId = com.android.internal.R.string.wifi_calling_off_summary;
            if (mImsMgr.isWfcEnabledByUser()) {
                boolean isRoaming = mTelephonyManager.isNetworkRoaming();
                int wfcMode = mImsMgr.getWfcMode(isRoaming);

                switch (wfcMode) {
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                        resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                        resId = com.android.internal.R.string
                                .wfc_mode_cellular_preferred_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                        break;
                    default:
                        if (DBG) log("Unexpected WFC mode value: " + wfcMode);
                }
            }
            mWiFiCallingPref.setSummary(resId);
        }

        mCallingCategory.addPreference(mWiFiCallingPref);
        mWiFiCallingPref.setEnabled(mTelephonyManager.getCallState(mSubId)
                == TelephonyManager.CALL_STATE_IDLE && hasActiveSubscriptions());
    }

    private void updateVideoCallState() {
        if (mVideoCallingPref == null || mCallingCategory == null) {
            return;
        }

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

        if (mImsMgr != null
                && mImsMgr.isVtEnabledByPlatform()
                && mImsMgr.isVtProvisionedOnDevice()
                && MobileNetworkUtils.isImsServiceStateReady(mImsMgr)
                && (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS)
                || mTelephonyManager.isDataEnabled())) {
            mCallingCategory.addPreference(mVideoCallingPref);
            if (!mButton4glte.isChecked()) {
                mVideoCallingPref.setEnabled(false);
                mVideoCallingPref.setChecked(false);
            } else {
                mVideoCallingPref.setEnabled(mTelephonyManager.getCallState(mSubId)
                        == TelephonyManager.CALL_STATE_IDLE && hasActiveSubscriptions());
                mVideoCallingPref.setChecked(mImsMgr.isVtEnabledByUser());
                mVideoCallingPref.setOnPreferenceChangeListener(this);
            }
        } else {
            mCallingCategory.removePreference(mVideoCallingPref);
        }
    }

    private void updatePreferredNetworkType() {
        boolean enabled = mTelephonyManager.getCallState(
                mSubId) == TelephonyManager.CALL_STATE_IDLE
                && hasActiveSubscriptions();
        Log.i(LOG_TAG, "updatePreferredNetworkType: " + enabled);
    }

    private void updateCallingCategory() {
        if (mCallingCategory == null) {
            return;
        }

        updateWiFiCallState();
        updateVideoCallState();

        // If all items in calling category is removed, we remove it from
        // the screen. Otherwise we'll see title of the category but nothing
        // is in there.
        if (mCallingCategory.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(mCallingCategory);
        } else {
            getPreferenceScreen().addPreference(mCallingCategory);
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isWorldMode() {
        boolean worldModeOn = false;
        final String configString = getResources().getString(R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null &&
                    ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                            || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                            && mTelephonyManager != null
                            && configArray[1].equalsIgnoreCase(
                            mTelephonyManager.getGroupIdLevel1())))) {
                worldModeOn = true;
            }
        }

        Log.d(LOG_TAG, "isWorldMode=" + worldModeOn);

        return worldModeOn;
    }

    private void controlGsmOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }

        updateGsmUmtsOptions(this, prefSet, mSubId);

        PreferenceCategory networkOperatorCategory =
                (PreferenceCategory) prefSet.findPreference(
                        NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
        Preference carrierSettings = prefSet.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (networkOperatorCategory != null) {
            if (enable) {
                networkOperatorCategory.setEnabled(true);
            } else {
                prefSet.removePreference(networkOperatorCategory);
            }
        }
        if (carrierSettings != null) {
            prefSet.removePreference(carrierSettings);
        }
    }

    private void controlCdmaOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }
        updateCdmaOptions(this, prefSet, mSubId);
    }

    private boolean isSupportTdscdma() {
        if (getResources().getBoolean(R.bool.config_support_tdscdma)) {
            return true;
        }

        String operatorNumeric = mTelephonyManager.getServiceState().getOperatorNumeric();
        String[] numericArray = getResources().getStringArray(
                R.array.config_support_tdscdma_roaming_on_networks);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.equals(numeric)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Metrics events related methods. it takes care of all preferences possible in this
     * fragment(except a few that log on their own). It doesn't only include preferences in
     * network_setting_fragment.xml, but also those defined in GsmUmtsOptions and CdmaOptions.
     */
    private void sendMetricsEventPreferenceClicked(
            PreferenceScreen preferenceScreen, Preference preference) {
        final int category = getMetricsEventCategory(preferenceScreen, preference);
        if (category == MetricsProto.MetricsEvent.VIEW_UNKNOWN) {
            return;
        }

        // Send MetricsEvent on click. It includes preferences other than SwitchPreferences,
        // which send MetricsEvent in onPreferenceChange.
        // For ListPreferences, we log it here without a value, only indicating it's clicked to
        // open the list dialog. When a value is chosen, another MetricsEvent is logged with
        // new value in onPreferenceChange.
        if (preference == mEuiccSettingsPref
                || preference == mWiFiCallingPref
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_GSM_APN_EXPAND_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_APN_EXPAND_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY)) {
            MetricsLogger.action(getContext(), category);
        }
    }

    private void sendMetricsEventPreferenceChanged(
            PreferenceScreen preferenceScreen, Preference preference, Object newValue) {
        final int category = getMetricsEventCategory(preferenceScreen, preference);
        if (category == MetricsProto.MetricsEvent.VIEW_UNKNOWN) {
            return;
        }

        // MetricsEvent logging with new value, for SwitchPreferences and ListPreferences.
        if (preference == mVideoCallingPref) {
            MetricsLogger.action(getContext(), category, (Boolean) newValue);
        } else if (preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)
                || preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            // Network select preference sends metrics event in its own listener.
            MetricsLogger.action(getContext(), category, Integer.valueOf((String) newValue));
        }
    }

    private int getMetricsEventCategory(
            PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == null) {
            return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
        } else if (preference == mEuiccSettingsPref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_EUICC_SETTING;
        } else if (preference == mWiFiCallingPref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_WIFI_CALLING;
        } else if (preference == mVideoCallingPref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_VIDEO_CALLING_TOGGLE;
        } else if (preference == preferenceScreen
                .findPreference(NetworkOperators.BUTTON_AUTO_SELECT_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_AUTO_SELECT_NETWORK_TOGGLE;
        } else if (preference == preferenceScreen
                .findPreference(NetworkOperators.BUTTON_NETWORK_SELECT_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK;
        } else if (preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_CDMA_SYSTEM_SELECT;
        } else if (preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_CDMA_SUBSCRIPTION_SELECT;
        } else if (preference == preferenceScreen.findPreference(BUTTON_GSM_APN_EXPAND_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_APN_EXPAND_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_APN_SETTINGS;
        } else if (preference == preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_CARRIER_SETTINGS;
        } else {
            return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
        }
    }

    private void updateGsmUmtsOptions(PreferenceFragmentCompat prefFragment,
            PreferenceScreen prefScreen, final int subId) {
        // We don't want to re-create GsmUmtsOptions if already exists. Otherwise, the
        // preferences inside it will also be re-created which causes unexpected behavior.
        // For example, the open dialog gets dismissed or detached after pause / resume.
        if (mGsmUmtsOptions == null) {
            mGsmUmtsOptions = new GsmUmtsOptions(prefFragment, prefScreen, subId);
        } else {
            mGsmUmtsOptions.update(subId);
        }
    }

    private void updateCdmaOptions(PreferenceFragmentCompat prefFragment, PreferenceScreen prefScreen,
            int subId) {
        // We don't want to re-create CdmaOptions if already exists. Otherwise, the preferences
        // inside it will also be re-created which causes unexpected behavior. For example,
        // the open dialog gets dismissed or detached after pause / resume.
        if (mCdmaOptions == null) {
            mCdmaOptions = new CdmaOptions(prefFragment, prefScreen, subId);
        }
    }

    private static Intent buildPhoneAccountConfigureIntent(
            Context context, PhoneAccountHandle accountHandle) {
        Intent intent = buildConfigureIntent(
                context, accountHandle, TelecomManager.ACTION_CONFIGURE_PHONE_ACCOUNT);

        if (intent == null) {
            // If the new configuration didn't work, try the old configuration intent.
            intent = buildConfigureIntent(
                    context, accountHandle, LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT);
            if (intent != null) {
                Log.w(MobileNetworkFragment.LOG_TAG,
                        "Phone account using old configuration intent: " + accountHandle);
            }
        }
        return intent;
    }

    private static Intent buildConfigureIntent(
            Context context, PhoneAccountHandle accountHandle, String actionStr) {
        if (accountHandle == null || accountHandle.getComponentName() == null
                || TextUtils.isEmpty(accountHandle.getComponentName().getPackageName())) {
            return null;
        }

        // Build the settings intent.
        Intent intent = new Intent(actionStr);
        intent.setPackage(accountHandle.getComponentName().getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);

        // Check to see that the phone account package can handle the setting intent.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
        if (resolutions.size() == 0) {
            intent = null;  // set no intent if the package cannot handle it.
        }

        return intent;
    }

    //TODO(b/114749736): update search provider
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_setting_fragment;
                    result.add(sir);
                    return result;
                }
            };

    private static final class SetPreferredNetworkAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final TelephonyManager mTelephonyManager;
        private final int mSubId;
        private final int mNetworkType;
        private final Message mCallback;

        SetPreferredNetworkAsyncTask(
                TelephonyManager tm, int subId, int networkType, Message callback) {
            mTelephonyManager = tm;
            mSubId = subId;
            mNetworkType = networkType;
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return mTelephonyManager.setPreferredNetworkType(mSubId, mNetworkType);
        }

        @Override
        protected void onPostExecute(Boolean isSuccessed) {
            mCallback.obj = isSuccessed;
            mCallback.sendToTarget();
        }
    }
}
