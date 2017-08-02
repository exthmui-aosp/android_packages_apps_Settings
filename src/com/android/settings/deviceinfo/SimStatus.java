/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static android.content.Context.CARRIER_CONFIG_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstantConversions;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.DeviceInfoUtils;

import java.util.List;


/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Operator info (area update info cell broadcast)
 * # Signal Strength
 *
 */
public class SimStatus extends SettingsPreferenceFragment {
    private static final String TAG = "SimStatus";

    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_ICCID = "iccid";

    @VisibleForTesting
    static final String KEY_IMS_REGISTRATION_STATE = "ims_reg_state";

    static private final String CB_AREA_INFO_RECEIVED_ACTION =
            "com.android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static private final String GET_LATEST_CB_AREA_INFO_ACTION =
            "com.android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    static private final String CELL_BROADCAST_RECEIVER_APP = "com.android.cellbroadcastreceiver";

    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    private Phone mPhone = null;
    private Resources mRes;
    private SubscriptionInfo mSir;
    private boolean mShowLatestAreaInfo;
    private boolean mShowICCID;
    private boolean mShowImsRegState;

    // Default summary for items
    private String mDefaultText;

    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ListView mListView;
    private List<SubscriptionInfo> mSelectableSubInfos;

    private PhoneStateListener mPhoneStateListener;

    // Once the cell broadcast configuration is moved into telephony framework,
    private final BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null && mSir.getSubscriptionId() == cbMessage.getSubId()) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mCarrierConfigManager = (CarrierConfigManager) getSystemService(CARRIER_CONFIG_SERVICE);

        mSelectableSubInfos = SubscriptionManager.from(getContext())
                .getActiveSubscriptionInfoList();

        addPreferencesFromResource(R.xml.device_info_sim_status);

        mRes = getResources();
        mDefaultText = mRes.getString(R.string.device_info_default);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (mSelectableSubInfos == null) {
            mSir = null;
        } else {
            mSir = mSelectableSubInfos.size() > 0 ? mSelectableSubInfos.get(0) : null;

            if (mSelectableSubInfos.size() > 1) {
                View view = inflater.inflate(R.layout.icc_lock_tabs, container, false);
                final ViewGroup prefs_container = (ViewGroup) view.findViewById(
                        R.id.prefs_container);
                Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
                View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
                prefs_container.addView(prefs);

                mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
                mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
                mListView = (ListView) view.findViewById(android.R.id.list);

                mTabHost.setup();
                mTabHost.setOnTabChangedListener(mTabListener);
                mTabHost.clearAllTabs();

                for (int i = 0; i < mSelectableSubInfos.size(); i++) {
                    mTabHost.addTab(buildTabSpec(String.valueOf(i),
                            String.valueOf(mSelectableSubInfos.get(i).getDisplayName())));
                }
                return view;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updatePhoneInfos();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_SIM_STATUS;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPhone != null) {
            updatePreference(false /* isTabChanged */);

            updateSignalStrength(mPhone.getSignalStrength());
            updateServiceState(mPhone.getServiceState());
            updateImsRegistrationState();
            updateDataState();
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
            if (mShowLatestAreaInfo) {
                getContext().registerReceiver(mAreaInfoReceiver,
                        new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                        Manifest.permission.RECEIVE_EMERGENCY_BROADCAST, null);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                getLatestIntent.setPackage(CELL_BROADCAST_RECEIVER_APP);
                getContext().sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        Manifest.permission.RECEIVE_EMERGENCY_BROADCAST);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPhone != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
        if (mShowLatestAreaInfo) {
            getContext().unregisterReceiver(mAreaInfoReceiver);
        }
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = mDefaultText;
        }
        // some preferences may be missing
        final Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        String networktype = null;
        final int subId = mSir.getSubscriptionId();
        final int actualDataNetworkType = mTelephonyManager.getDataNetworkType(
                mSir.getSubscriptionId());
        final int actualVoiceNetworkType = mTelephonyManager.getVoiceNetworkType(
                mSir.getSubscriptionId());
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualDataNetworkType) {
            networktype = mTelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualVoiceNetworkType) {
            networktype = mTelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }

        boolean show4GForLTE = false;
        try {
            Context con = getActivity().createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            show4GForLTE = con.getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException for show4GFotLTE");
        }

        if (networktype != null && networktype.equals("LTE") && show4GForLTE) {
            networktype = "4G";
        }
        setSummaryText(KEY_NETWORK_TYPE, networktype);
    }

    private void updateDataState() {
        final int state =
                PhoneConstantConversions.convertDataState(mPhone.getDataConnectionState());

        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        setSummaryText(KEY_DATA_STATE, display);
    }

    private void updateServiceState(ServiceState serviceState) {
        final int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                // Set signal strength to 0 when service state is STATE_OUT_OF_SERVICE
                setSummaryText(KEY_SIGNAL_STRENGTH, "0");
            case ServiceState.STATE_EMERGENCY_ONLY:
                // Set summary string of service state to radioInfo_service_out when
                // service state is both STATE_OUT_OF_SERVICE & STATE_EMERGENCY_ONLY
                display = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = mRes.getString(R.string.radioInfo_service_off);
                // Also set signal strength to 0
                setSummaryText(KEY_SIGNAL_STRENGTH, "0");
                break;
        }

        setSummaryText(KEY_SERVICE_STATE, display);

        if (serviceState.getRoaming()) {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        final int state = mPhone.getServiceState().getState();

        if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                (ServiceState.STATE_POWER_OFF == state)) {
            setSummaryText(KEY_SIGNAL_STRENGTH, "0");
            return;
        }

        int signalDbm = signalStrength.getDbm();
        int signalAsu = signalStrength.getAsuLevel();

        if (-1 == signalDbm) {
            signalDbm = 0;
        }

        if (-1 == signalAsu) {
            signalAsu = 0;
        }

        setSummaryText(KEY_SIGNAL_STRENGTH, mRes.getString(R.string.sim_signal_strength, signalDbm,
                signalAsu));
    }

    @VisibleForTesting
    void updateImsRegistrationState() {
        boolean isImsRegistered = mTelephonyManager.isImsRegistered(mSir.getSubscriptionId());
        setSummaryText(KEY_IMS_REGISTRATION_STATE, mRes.getString(isImsRegistered ?
                R.string.ims_reg_status_registered : R.string.ims_reg_status_not_registered));
    }

    @VisibleForTesting
    void updatePreference(boolean isTabChanged) {
        if (isTabChanged) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen != null) {
                preferenceScreen.removeAll();
                addPreferencesFromResource(R.xml.device_info_sim_status);
            }
        }

        if (mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            mShowLatestAreaInfo = Resources.getSystem().getBoolean(
                    com.android.internal.R.bool.config_showAreaUpdateInfoSettings);
        }
        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                mSir.getSubscriptionId());
        mShowICCID = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL);
        mShowImsRegState = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL);

        // If formattedNumber is null or empty, it'll display as "Unknown".
        setSummaryText(KEY_PHONE_NUMBER,
                DeviceInfoUtils.getFormattedPhoneNumber(getContext(), mSir));
        setSummaryText(KEY_IMEI, mPhone.getImei());
        setSummaryText(KEY_IMEI_SV, mPhone.getDeviceSvn());

        if (!mShowICCID) {
            removePreferenceFromScreen(KEY_ICCID);
        } else {
            // Get ICCID, which is SIM serial number
            String iccid = mTelephonyManager.getSimSerialNumber(mSir.getSubscriptionId());
            setSummaryText(KEY_ICCID, iccid);
        }

        if (!mShowLatestAreaInfo) {
            removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
        }

        boolean hideSignalStrength = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_SIGNAL_STRENGTH_IN_SIM_STATUS_BOOL);
        if (hideSignalStrength) {
            removePreferenceFromScreen(KEY_SIGNAL_STRENGTH);
        }

        if (!mShowImsRegState) {
            removePreferenceFromScreen(KEY_IMS_REGISTRATION_STATE);
        }
    }

    private void updatePhoneInfos() {
        if (mSir != null) {
            // TODO: http://b/23763013
            final Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(
                        mSir.getSubscriptionId()));
            if (UserManager.get(getContext()).isAdminUser()
                    && SubscriptionManager.isValidSubscriptionId(mSir.getSubscriptionId())) {
                if (phone == null) {
                    Log.e(TAG, "Unable to locate a phone object for the given Subscription ID.");
                    return;
                }

                mPhone = phone;
                // To avoid register multiple listeners when user changes the tab.
                if (mPhoneStateListener != null && mTelephonyManager != null) {
                    mTelephonyManager.listen(mPhoneStateListener,
                            PhoneStateListener.LISTEN_NONE);
                    mPhoneStateListener = null;
                }
                mPhoneStateListener = new PhoneStateListener(mSir.getSubscriptionId()) {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        updateDataState();
                        updateNetworkType();
                    }

                    @Override
                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        updateSignalStrength(signalStrength);
                    }

                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        updateServiceState(serviceState);
                        updateImsRegistrationState();
                    }
                };
            }
        }
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            doTabChanged(tabId);
        }
    };

    @VisibleForTesting
    void doTabChanged(String tabId) {
        final int slotId = Integer.parseInt(tabId);
        mSir = mSelectableSubInfos.get(slotId);

        // The User has changed tab; update the SIM information.
        updatePhoneInfos();
        updatePreference(true /* isTabChanged */);
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                | PhoneStateListener.LISTEN_SERVICE_STATE);
        updateDataState();
        updateNetworkType();
    }

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }
}
