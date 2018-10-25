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

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.MOBILE_NETWORK_SELECT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.NetworkRegistrationState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.OperatorInfo;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Choose network" settings UI for the Phone app.
 */
public class NetworkSelectSettings extends DashboardFragment {

    private static final String TAG = "NetworkSelectSettings";

    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SCAN_ERROR = 3;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 4;

    private static final String PREF_KEY_CONNECTED_NETWORK_OPERATOR =
            "connected_network_operator_preference";
    private static final String PREF_KEY_NETWORK_OPERATORS = "network_operators_preference";

    @VisibleForTesting
    PreferenceCategory mPreferenceCategory;
    @VisibleForTesting
    PreferenceCategory mConnectedPreferenceCategory;
    @VisibleForTesting
    NetworkOperatorPreference mSelectedPreference;
    private View mProgressHeader;
    private Preference mStatusMessagePreference;
    @VisibleForTesting
    List<CellInfo> mCellInfoList;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    @VisibleForTesting
    TelephonyManager mTelephonyManager;
    private List<String> mForbiddenPlmns;
    private boolean mShow4GForLTE = true;
    private NetworkScanHelper mNetworkScanHelper;
    private final ExecutorService mNetworkScanExecutor = Executors.newFixedThreadPool(1);
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private boolean mUseNewApi;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUseNewApi = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI);
        mSubId = getArguments().getInt(MobileSettingsActivity.KEY_SUBSCRIPTION_ID);

        addPreferencesFromResource(R.xml.choose_network);
        mConnectedPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_CONNECTED_NETWORK_OPERATOR);
        mPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_NETWORK_OPERATORS);
        mStatusMessagePreference = new Preference(getContext());
        mSelectedPreference = null;
        mTelephonyManager = TelephonyManager.from(getContext()).createForSubscriptionId(mSubId);
        mNetworkScanHelper = new NetworkScanHelper(
                mTelephonyManager, mCallback, mNetworkScanExecutor);
        PersistableBundle bundle = ((CarrierConfigManager) getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE)).getConfigForSubId(mSubId);
        if (bundle != null) {
            mShow4GForLTE = bundle.getBoolean(
                    CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        }

        mMetricsFeatureProvider = FeatureFactory
                .getFactory(getContext()).getMetricsFeatureProvider();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null) {
            mProgressHeader = setPinnedHeaderView(R.layout.wifi_progress_header)
                    .findViewById(R.id.progress_bar_animation);
            setProgressBarVisible(false);
        }
        forceUpdateConnectedPreferenceCategory();
    }

    @Override
    public void onStart() {
        super.onStart();
        mForbiddenPlmns = Arrays.asList(mTelephonyManager.getForbiddenPlmns());
        setProgressBarVisible(true);

        if (mUseNewApi) {
            mNetworkScanHelper.startNetworkScan(
                    NetworkScanHelper.NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS);
        } else {
            mNetworkScanHelper.startNetworkScan(
                    NetworkScanHelper.NETWORK_SCAN_TYPE_WAIT_FOR_ALL_RESULTS);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopNetworkQuery();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != mSelectedPreference) {
            stopNetworkQuery();
            setProgressBarVisible(false);
            // Refresh the last selected item in case users reselect network.
            if (mSelectedPreference != null) {
                mSelectedPreference.setSummary(null);
            }

            mSelectedPreference = (NetworkOperatorPreference) preference;
            CellInfo cellInfo = mSelectedPreference.getCellInfo();

            mMetricsFeatureProvider.action(getContext(),
                    MetricsEvent.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK);

            // Set summary as "Disconnected" to the previously connected network
            if (mConnectedPreferenceCategory.getPreferenceCount() > 0) {
                NetworkOperatorPreference connectedNetworkOperator = (NetworkOperatorPreference)
                        (mConnectedPreferenceCategory.getPreference(0));
                if (!CellInfoUtil.getNetworkTitle(cellInfo).equals(
                        CellInfoUtil.getNetworkTitle(connectedNetworkOperator.getCellInfo()))) {
                    connectedNetworkOperator.setSummary(R.string.network_disconnected);
                }
            }

            final OperatorInfo operatorInfo = CellInfoUtil.getOperatorInfoFromCellInfo(cellInfo);
            final boolean isSuccess = mTelephonyManager.setNetworkSelectionModeManual(
                    operatorInfo, true /* persistSelection */);
            mSelectedPreference.setSummary(
                    isSuccess ? R.string.network_connected : R.string.network_could_not_connect);
        }

        return true;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.choose_network;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return MOBILE_NETWORK_SELECT;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_NETWORK_SCAN_RESULTS:
                    List<CellInfo> results = aggregateCellInfoList((List<CellInfo>) msg.obj);
                    mCellInfoList = new ArrayList<>(results);
                    if (mCellInfoList != null && mCellInfoList.size() != 0) {
                        updateAllPreferenceCategory();
                    } else {
                        addMessagePreference(R.string.empty_networks_list);
                    }

                    break;

                case EVENT_NETWORK_SCAN_ERROR:
                    stopNetworkQuery();
                    addMessagePreference(R.string.network_query_error);
                    break;

                case EVENT_NETWORK_SCAN_COMPLETED:
                    stopNetworkQuery();
                    if (mCellInfoList == null) {
                        // In case the scan timeout before getting any results
                        addMessagePreference(R.string.empty_networks_list);
                    }
                    break;
            }
            return;
        }
    };

    private final NetworkScanHelper.NetworkScanCallback mCallback =
            new NetworkScanHelper.NetworkScanCallback() {
                public void onResults(List<CellInfo> results) {
                    Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_RESULTS, results);
                    msg.sendToTarget();
                }

                public void onComplete() {
                    Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED);
                    msg.sendToTarget();
                }

                public void onError(int error) {
                    Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_ERROR, error,
                            0 /* arg2 */);
                    msg.sendToTarget();
                }
            };

    /**
     * Update the currently available network operators list, which only contains the unregistered
     * network operators. So if the device has no data and the network operator in the connected
     * network operator category shows "Disconnected", it will also exist in the available network
     * operator category for user to select. On the other hand, if the device has data and the
     * network operator in the connected network operator category shows "Connected", it will not
     * exist in the available network category.
     */
    @VisibleForTesting
    void updateAllPreferenceCategory() {
        updateConnectedPreferenceCategory();

        mPreferenceCategory.removeAll();
        for (int index = 0; index < mCellInfoList.size(); index++) {
            if (!mCellInfoList.get(index).isRegistered()) {
                NetworkOperatorPreference pref = new NetworkOperatorPreference(
                        mCellInfoList.get(index), getContext(), mForbiddenPlmns, mShow4GForLTE);
                pref.setKey(CellInfoUtil.getNetworkTitle(mCellInfoList.get(index)));
                pref.setOrder(index);
                mPreferenceCategory.addPreference(pref);
            }
        }
    }

    /**
     * Config the connected network operator preference when the page was created. When user get
     * into this page, the device might or might not have data connection.
     * - If the device has data:
     * 1. use {@code ServiceState#getNetworkRegistrationStates()} to get the currently
     * registered cellIdentity, wrap it into a CellInfo;
     * 2. set the signal strength level as strong;
     * 3. use {@link TelephonyManager#getNetworkOperatorName()} to get the title of the
     * previously connected network operator, since the CellIdentity got from step 1 only has
     * PLMN.
     * - If the device has no data, we will remove the connected network operators list from the
     * screen.
     */
    private void forceUpdateConnectedPreferenceCategory() {
        if (mTelephonyManager.getDataState() == mTelephonyManager.DATA_CONNECTED) {
            // Try to get the network registration states
            ServiceState ss = mTelephonyManager.getServiceState();
            List<NetworkRegistrationState> networkList =
                    ss.getNetworkRegistrationStates(AccessNetworkConstants.TransportType.WWAN);
            if (networkList == null || networkList.size() == 0) {
                // Remove the connected network operators category
                mConnectedPreferenceCategory.setVisible(false);
                return;
            }
            CellIdentity cellIdentity = networkList.get(0).getCellIdentity();
            CellInfo cellInfo = CellInfoUtil.wrapCellInfoWithCellIdentity(cellIdentity);
            if (cellInfo != null) {
                NetworkOperatorPreference pref = new NetworkOperatorPreference(
                        cellInfo, getContext(), mForbiddenPlmns, mShow4GForLTE);
                pref.setTitle(mTelephonyManager.getNetworkOperatorName());
                pref.setSummary(R.string.network_connected);
                // Update the signal strength icon, since the default signalStrength value would be
                // zero (it would be quite confusing why the connected network has no signal)
                pref.setIcon(NetworkOperatorPreference.NUMBER_OF_LEVELS - 1);
                mConnectedPreferenceCategory.addPreference(pref);
            } else {
                // Remove the connected network operators category
                mConnectedPreferenceCategory.setVisible(false);
            }
        } else {
            // Remove the connected network operators category
            mConnectedPreferenceCategory.setVisible(false);
        }
    }

    /**
     * Configure the ConnectedNetworkOperatorsPreferenceCategory. The category only need to be
     * configured if the category is currently empty or the operator network title of the previous
     * connected network is different from the new one.
     */
    private void updateConnectedPreferenceCategory() {
        CellInfo connectedNetworkOperator = null;
        for (CellInfo cellInfo : mCellInfoList) {
            if (cellInfo.isRegistered()) {
                connectedNetworkOperator = cellInfo;
                break;
            }
        }

        if (connectedNetworkOperator != null) {
            addConnectedNetworkOperatorPreference(connectedNetworkOperator);
        }
    }

    private void addConnectedNetworkOperatorPreference(CellInfo cellInfo) {
        mConnectedPreferenceCategory.removeAll();
        final NetworkOperatorPreference pref = new NetworkOperatorPreference(
                cellInfo, getContext(), mForbiddenPlmns, mShow4GForLTE);
        pref.setSummary(R.string.network_connected);
        mConnectedPreferenceCategory.addPreference(pref);
        mConnectedPreferenceCategory.setVisible(true);
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void addMessagePreference(int messageId) {
        setProgressBarVisible(false);
        mStatusMessagePreference.setTitle(messageId);
        mConnectedPreferenceCategory.setVisible(false);
        mPreferenceCategory.removeAll();
        mPreferenceCategory.addPreference(mStatusMessagePreference);
    }

    /**
     * The Scan results may contains several cell infos with different radio technologies and signal
     * strength for one network operator. Aggregate the CellInfoList by retaining only the cell info
     * with the strongest signal strength.
     */
    private List<CellInfo> aggregateCellInfoList(List<CellInfo> cellInfoList) {
        Map<String, CellInfo> map = new HashMap<>();
        for (CellInfo cellInfo : cellInfoList) {
            String plmn = CellInfoUtil.getOperatorInfoFromCellInfo(cellInfo).getOperatorNumeric();
            if (cellInfo.isRegistered() || !map.containsKey(plmn)) {
                map.put(plmn, cellInfo);
            } else {
                if (map.get(plmn).isRegistered()
                        || map.get(plmn).getCellSignalStrength().getLevel()
                        > cellInfo.getCellSignalStrength().getLevel()) {
                    // Skip if the stored cellInfo is registered or has higher signal strength level
                    continue;
                }
                // Otherwise replace it with the new CellInfo
                map.put(plmn, cellInfo);
            }
        }
        return new ArrayList<>(map.values());
    }

    private void stopNetworkQuery() {
        if (mNetworkScanHelper != null) {
            mNetworkScanHelper.stopNetworkQuery();
        }
    }

    @Override
    public void onDestroy() {
        mNetworkScanExecutor.shutdown();
        super.onDestroy();
    }
}
