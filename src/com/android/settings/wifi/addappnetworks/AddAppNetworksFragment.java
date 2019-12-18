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

package com.android.settings.wifi.addappnetworks;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.internal.PreferenceImageView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fragment list those networks, which is proposed by other app, to user, and handle user's
 * choose on either saving those networks or rejecting the request.
 */
public class AddAppNetworksFragment extends InstrumentedFragment {
    public static final String TAG = "AddAppNetworksFragment";

    // Security types of a requested or saved network.
    private static final String SECURITY_NO_PASSWORD = "nopass";
    private static final String SECURITY_WEP = "wep";
    private static final String SECURITY_WPA_PSK = "wpa";
    private static final String SECURITY_SAE = "sae";

    // Possible result values in each item of the returned result list, which is used
    // to inform the caller APP the processed result of each specified network.
    @VisibleForTesting
    static final int RESULT_NETWORK_INITIAL = -1;  //initial value
    private static final int RESULT_NETWORK_SUCCESS = 0;
    private static final int RESULT_NETWORK_ADD_ERROR = 1;
    @VisibleForTesting
    static final int RESULT_NETWORK_ALREADY_EXISTS = 2;

    // Handler messages for controlling different state and delay showing the status message.
    private static final int MESSAGE_START_SAVING_NETWORK = 1;
    private static final int MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK = 2;
    private static final int MESSAGE_SHOW_SAVE_FAILED = 3;
    private static final int MESSAGE_FINISH = 4;

    // Signal level for the constant signal icon.
    private static final int MAX_RSSI_SIGNAL_LEVEL = 4;
    // Max networks count within one request
    private static final int MAX_SPECIFIC_NETWORKS_COUNT = 5;

    // Duration for showing different status message.
    private static final long SHOW_SAVING_INTERVAL_MILLIS = 500L;
    private static final long SHOW_SAVED_INTERVAL_MILLIS = 1000L;

    @VisibleForTesting
    FragmentActivity mActivity;
    @VisibleForTesting
    View mLayoutView;
    @VisibleForTesting
    Button mCancelButton;
    @VisibleForTesting
    Button mSaveButton;
    @VisibleForTesting
    String mCallingPackageName;
    @VisibleForTesting
    List<WifiConfiguration> mAllSpecifiedNetworksList;
    @VisibleForTesting
    List<UiConfigurationItem> mUiToRequestedList;
    @VisibleForTesting
    List<Integer> mResultCodeArrayList;

    private boolean mIsSingleNetwork;
    private boolean mAnyNetworkSavedSuccess;
    private TextView mSummaryView;
    private TextView mSingleNetworkProcessingStatusView;
    private int mSavingIndex;
    private UiConfigurationItemAdapter mUiConfigurationItemAdapter;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager mWifiManager;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showSaveStatusByState(msg.what);

            switch (msg.what) {
                case MESSAGE_START_SAVING_NETWORK:
                    mSaveButton.setEnabled(false);
                    // Save the proposed networks, start from first one.
                    saveNetwork(0);
                    break;

                case MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK:
                    // For the single network case, we need to call connection after saved.
                    if (mIsSingleNetwork) {
                        connectNetwork(0);
                    }
                    sendEmptyMessageDelayed(MESSAGE_FINISH,
                            SHOW_SAVED_INTERVAL_MILLIS);
                    break;

                case MESSAGE_SHOW_SAVE_FAILED:
                    mSaveButton.setEnabled(true);
                    break;

                case MESSAGE_FINISH:
                    finishWithResult(RESULT_OK, mResultCodeArrayList);
                    break;

                default:
                    // Do nothing.
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        mWifiManager = mActivity.getSystemService(WifiManager.class);

        return inflater.inflate(R.layout.wifi_add_app_networks, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initial UI variables.
        mLayoutView = view;
        mCancelButton = view.findViewById(R.id.cancel);
        mSaveButton = view.findViewById(R.id.save);
        mSummaryView = view.findViewById(R.id.app_summary);
        mSingleNetworkProcessingStatusView = view.findViewById(R.id.single_status);
        // Assigns button listeners and network save listener.
        mCancelButton.setOnClickListener(getCancelClickListener());
        mSaveButton.setOnClickListener(getSaveClickListener());
        prepareSaveResultListener();

        // Prepare the non-UI variables.
        final Bundle bundle = getArguments();
        createContent(bundle);
    }

    private void createContent(Bundle bundle) {
        mAllSpecifiedNetworksList =
                bundle.getParcelableArrayList(Settings.EXTRA_WIFI_CONFIGURATION_LIST);

        // If there is no network in the request intent or the requested networks exceed the
        // maximum limit, then just finish activity.
        if (mAllSpecifiedNetworksList == null || mAllSpecifiedNetworksList.isEmpty()
                || mAllSpecifiedNetworksList.size() > MAX_SPECIFIC_NETWORKS_COUNT) {
            finishWithResult(RESULT_CANCELED, null /* resultArrayList */);
            return;
        }

        // Initial the result arry.
        initializeResultCodeArray();
        // Filter the saved networks, and prepare a not saved networks list for UI to present.
        mUiToRequestedList = filterSavedNetworks(mWifiManager.getPrivilegedConfiguredNetworks());

        // If all the specific networks are all exist, we just need to finish with result.
        if (mUiToRequestedList.size() == 0) {
            finishWithResult(RESULT_OK, mResultCodeArrayList);
            return;
        }

        if (mAllSpecifiedNetworksList.size() == 1) {
            mIsSingleNetwork = true;
            // Set the multiple networks related layout as GONE.
            mLayoutView.findViewById(R.id.multiple_networks).setVisibility(View.GONE);
            // Show signal icon for single network case.
            setSingleNetworkSignalIcon();
            // Show the SSID of the proposed network.
            ((TextView) mLayoutView.findViewById(R.id.single_ssid)).setText(
                    mAllSpecifiedNetworksList.get(0).SSID);
            // Set the status view as gone when UI is initialized.
            mSingleNetworkProcessingStatusView.setVisibility(View.GONE);
        } else {
            // Multiple networks request case.
            mIsSingleNetwork = false;
            // Set the single network related layout as GONE.
            mLayoutView.findViewById(R.id.single_network).setVisibility(View.GONE);
            // Prepare a UI adapter and set to UI listview.
            final ListView uiNetworkListView = mLayoutView.findViewById(R.id.config_list);
            mUiConfigurationItemAdapter = new UiConfigurationItemAdapter(mActivity,
                    com.android.settingslib.R.layout.preference_access_point, mUiToRequestedList);
            uiNetworkListView.setAdapter(mUiConfigurationItemAdapter);
        }

        // Assigns caller app icon, title, and summary.
        mCallingPackageName =
                bundle.getString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME);
        assignAppIcon(mActivity, mCallingPackageName);
        assignTitleAndSummary(mActivity, mCallingPackageName);
    }

    private void initializeResultCodeArray() {
        final int networksSize = mAllSpecifiedNetworksList.size();
        mResultCodeArrayList = new ArrayList<>();

        for (int i = 0; i < networksSize; i++) {
            mResultCodeArrayList.add(RESULT_NETWORK_INITIAL);
        }
    }

    /**
     * Classify security type into following types:
     * 1. {@Code SECURITY_NO_PASSWORD}: No password network or OWE network.
     * 2. {@Code SECURITY_WEP}: Traditional WEP encryption network.
     * 3. {@Code SECURITY_WPA_PSK}: WPA/WPA2 preshare key type.
     * 4. {@Code SECURITY_SAE}: SAE type network.
     */
    private String getSecurityType(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.SAE)) {
            return SECURITY_SAE;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.OWE)) {
            return SECURITY_NO_PASSWORD;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK) || config.allowedKeyManagement.get(
                KeyMgmt.WPA2_PSK)) {
            return SECURITY_WPA_PSK;
        }
        return (config.wepKeys[0] == null) ? SECURITY_NO_PASSWORD : SECURITY_WEP;
    }

    /**
     * For the APP specified networks, filter saved ones and mark those saved as existed. And
     * finally return a new UiConfigurationItem list, which contains those new or need to be
     * updated networks, back to caller for creating UI to user.
     */
    @VisibleForTesting
    ArrayList<UiConfigurationItem> filterSavedNetworks(
            List<WifiConfiguration> savedWifiConfigurations) {
        ArrayList<UiConfigurationItem> uiToRequestedList = new ArrayList<>();

        boolean foundInSavedList;
        int networkPositionInBundle = 0;
        for (WifiConfiguration specifiecConfig : mAllSpecifiedNetworksList) {
            foundInSavedList = false;
            final String displayedSsid = removeDoubleQuotes(specifiecConfig.SSID);
            final String ssidWithQuotation = addQuotationIfNeeded(specifiecConfig.SSID);
            final String securityType = getSecurityType(specifiecConfig);

            for (WifiConfiguration privilegedWifiConfiguration : savedWifiConfigurations) {
                final String savedSecurityType = getSecurityType(privilegedWifiConfiguration);

                // If SSID or security type is different, should be new network or need to be
                // updated network.
                if (!ssidWithQuotation.equals(privilegedWifiConfiguration.SSID)
                        || !securityType.equals(savedSecurityType)) {
                    continue;
                }

                //  If specified network and saved network have same security types, we'll check
                //  more information according to their security type to judge if they are same.
                switch (securityType) {
                    case SECURITY_NO_PASSWORD:
                        foundInSavedList = true;
                        break;
                    case SECURITY_WEP:
                        if (specifiecConfig.wepKeys[0].equals(
                                privilegedWifiConfiguration.wepKeys[0])) {
                            foundInSavedList = true;
                        }
                        break;
                    case SECURITY_WPA_PSK:
                    case SECURITY_SAE:
                        if (specifiecConfig.preSharedKey.equals(
                                privilegedWifiConfiguration.preSharedKey)) {
                            foundInSavedList = true;
                        }
                        break;
                    default:
                        break;
                }
            }

            if (foundInSavedList) {
                // If this requested network already in the saved networks, mark this item in the
                // result code list as existed.
                mResultCodeArrayList.set(networkPositionInBundle, RESULT_NETWORK_ALREADY_EXISTS);
            } else {
                // Prepare to add to UI list to show to user
                UiConfigurationItem uiConfigurationIcon = new UiConfigurationItem(displayedSsid,
                        specifiecConfig, networkPositionInBundle);
                uiToRequestedList.add(uiConfigurationIcon);
            }
            networkPositionInBundle++;
        }

        return uiToRequestedList;
    }

    private void setSingleNetworkSignalIcon() {
        // TODO: Check level of the network to show signal icon.
        final Drawable wifiIcon = mActivity.getDrawable(
                Utils.getWifiIconResource(MAX_RSSI_SIGNAL_LEVEL)).mutate();
        final Drawable wifiIconDark = wifiIcon.getConstantState().newDrawable().mutate();
        wifiIconDark.setTintList(
                Utils.getColorAttr(mActivity, android.R.attr.colorControlNormal));
        ((ImageView) mLayoutView.findViewById(R.id.signal_strength)).setImageDrawable(wifiIconDark);
    }

    private String addQuotationIfNeeded(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(input).append("\"");

        return sb.toString();
    }

    static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private void assignAppIcon(Context context, String callingPackageName) {
        final Drawable drawable = loadPackageIconDrawable(context, callingPackageName);
        ((ImageView) mLayoutView.findViewById(R.id.app_icon)).setImageDrawable(drawable);
    }

    private Drawable loadPackageIconDrawable(Context context, String callingPackageName) {
        Drawable icon = null;
        try {
            icon = context.getPackageManager().getApplicationIcon(callingPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Cannot get application icon", e);
        }

        return icon;
    }

    private void assignTitleAndSummary(Context context, String callingPackageName) {
        // Assigns caller app name to title
        ((TextView) mLayoutView.findViewById(R.id.app_title)).setText(getTitle());

        // Set summary
        mSummaryView.setText(getAddNetworkRequesterSummary(
                Utils.getApplicationLabel(context, callingPackageName)));
    }

    private CharSequence getAddNetworkRequesterSummary(CharSequence appName) {
        return getString(mIsSingleNetwork ? R.string.wifi_add_app_single_network_summary
                : R.string.wifi_add_app_networks_summary, appName);
    }

    private CharSequence getTitle() {
        return getString(mIsSingleNetwork ? R.string.wifi_add_app_single_network_title
                : R.string.wifi_add_app_networks_title);
    }

    View.OnClickListener getCancelClickListener() {
        return (v) -> {
            Log.d(TAG, "User rejected to add network");
            finishWithResult(RESULT_CANCELED, null /* resultArrayList */);
        };
    }

    View.OnClickListener getSaveClickListener() {
        return (v) -> {
            Log.d(TAG, "User agree to add networks");
            // Start to process saving networks.
            final Message message = mHandler.obtainMessage(MESSAGE_START_SAVING_NETWORK);
            message.sendToTarget();
        };
    }

    /**
     * This class used to show network items to user, each item contains one specific (@Code
     * WifiConfiguration} and one index to mapping this UI item to the item in the APP request
     * network list.
     */
    @VisibleForTesting
    static class UiConfigurationItem {
        public final String mDisplayedSsid;
        public final WifiConfiguration mWifiConfiguration;
        public final int mIndex;

        UiConfigurationItem(String displayedSsid, WifiConfiguration wifiConfiguration, int index) {
            mDisplayedSsid = displayedSsid;
            mWifiConfiguration = wifiConfiguration;
            mIndex = index;
        }
    }

    private class UiConfigurationItemAdapter extends ArrayAdapter<UiConfigurationItem> {
        private final int mResourceId;
        private final LayoutInflater mInflater;

        UiConfigurationItemAdapter(Context context, int resourceId,
                List<UiConfigurationItem> objects) {
            super(context, resourceId, objects);
            mResourceId = resourceId;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(mResourceId, parent, false /* attachToRoot */);
            }

            final View divider = view.findViewById(
                    com.android.settingslib.R.id.two_target_divider);
            if (divider != null) {
                divider.setVisibility(View.GONE);
            }

            final UiConfigurationItem uiConfigurationItem = getItem(position);
            final TextView titleView = view.findViewById(android.R.id.title);
            if (titleView != null) {
                // Shows whole SSID for better UX.
                titleView.setSingleLine(false);
                titleView.setText(uiConfigurationItem.mDisplayedSsid);
            }

            final PreferenceImageView imageView = view.findViewById(android.R.id.icon);
            if (imageView != null) {
                final Drawable drawable = getContext().getDrawable(
                        com.android.settingslib.Utils.getWifiIconResource(MAX_RSSI_SIGNAL_LEVEL));
                drawable.setTintList(
                        com.android.settingslib.Utils.getColorAttr(getContext(),
                                android.R.attr.colorControlNormal));
                imageView.setImageDrawable(drawable);
            }

            final TextView summaryView = view.findViewById(android.R.id.summary);
            if (summaryView != null) {
                summaryView.setVisibility(View.GONE);
            }

            return view;
        }
    }

    private void prepareSaveResultListener() {
        mSaveListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Set success into result list.
                mResultCodeArrayList.set(mUiToRequestedList.get(mSavingIndex).mIndex,
                        RESULT_NETWORK_SUCCESS);
                mAnyNetworkSavedSuccess = true;

                if (saveNextNetwork()) {
                    return;
                }

                // Show saved or failed according to all results
                showSavedOrFail();
            }

            @Override
            public void onFailure(int reason) {
                // Set result code of this network to be failed in the return list.
                mResultCodeArrayList.set(mUiToRequestedList.get(mSavingIndex).mIndex,
                        RESULT_NETWORK_ADD_ERROR);

                if (saveNextNetwork()) {
                    return;
                }

                // Show saved or failed according to all results
                showSavedOrFail();
            }
        };
    }

    /**
     * For multiple networks case, we need to check if there is other network need to save.
     */
    private boolean saveNextNetwork() {
        // Save the next network if have.
        if (!mIsSingleNetwork && mSavingIndex < (mUiToRequestedList.size() - 1)) {
            mSavingIndex++;
            saveNetwork(mSavingIndex);
            return true;
        }

        return false;
    }

    /**
     * If any one of the specified networks is success, then we show saved and return all results
     * list back to caller APP, otherwise we show failed to indicate all networks saved failed.
     */
    private void showSavedOrFail() {
        Message nextStateMessage;
        if (mAnyNetworkSavedSuccess) {
            // Enter next state after all networks are saved.
            nextStateMessage = mHandler.obtainMessage(
                    MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK);
        } else {
            nextStateMessage = mHandler.obtainMessage(MESSAGE_SHOW_SAVE_FAILED);
        }
        // Delay to change to next state for showing saving mesage for a period.
        mHandler.sendMessageDelayed(nextStateMessage, SHOW_SAVING_INTERVAL_MILLIS);
    }

    /**
     * Call framework API to save single network.
     */
    private void saveNetwork(int index) {
        final WifiConfiguration wifiConfiguration = mUiToRequestedList.get(
                index).mWifiConfiguration;
        wifiConfiguration.SSID = addQuotationIfNeeded(wifiConfiguration.SSID);
        mSavingIndex = index;
        mWifiManager.save(wifiConfiguration, mSaveListener);
    }

    private void connectNetwork(int index) {
        final WifiConfiguration wifiConfiguration = mUiToRequestedList.get(
                index).mWifiConfiguration;
        mWifiManager.connect(wifiConfiguration, null /* ActionListener */);
    }

    private void finishWithResult(int resultCode, List<Integer> resultArrayList) {
        if (resultArrayList != null) {
            Intent intent = new Intent();
            intent.putIntegerArrayListExtra(Settings.EXTRA_WIFI_CONFIGURATION_RESULT_LIST,
                    (ArrayList<Integer>) resultArrayList);
            mActivity.setResult(resultCode, intent);
        }
        mActivity.finish();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_ADD_WIFI_NETWORKS;
    }

    private void showSaveStatusByState(int status) {
        switch (status) {
            case MESSAGE_START_SAVING_NETWORK:
                if (mIsSingleNetwork) {
                    // Set the initial text color for status message.
                    mSingleNetworkProcessingStatusView.setTextColor(
                            com.android.settingslib.Utils.getColorAttr(mActivity,
                                    android.R.attr.textColorSecondary));
                    mSingleNetworkProcessingStatusView.setText(
                            getString(R.string.wifi_add_app_single_network_saving_summary));
                    mSingleNetworkProcessingStatusView.setVisibility(View.VISIBLE);
                } else {
                    mSummaryView.setTextColor(
                            com.android.settingslib.Utils.getColorAttr(mActivity,
                                    android.R.attr.textColorSecondary));
                    mSummaryView.setText(
                            getString(R.string.wifi_add_app_networks_saving_summary,
                                    mUiToRequestedList.size()));
                }
                break;

            case MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK:
                if (mIsSingleNetwork) {
                    mSingleNetworkProcessingStatusView.setText(
                            getString(R.string.wifi_add_app_single_network_saved_summary));
                } else {
                    mSummaryView.setText(
                            getString(R.string.wifi_add_app_networks_saved_summary));
                }
                break;

            case MESSAGE_SHOW_SAVE_FAILED:
                if (mIsSingleNetwork) {
                    // Error message need to use colorError attribute to show.
                    mSingleNetworkProcessingStatusView.setTextColor(
                            com.android.settingslib.Utils.getColorAttr(mActivity,
                                    android.R.attr.colorError));
                    mSingleNetworkProcessingStatusView.setText(
                            getString(R.string.wifi_add_app_network_save_failed_summary));
                } else {
                    // Error message need to use colorError attribute to show.
                    mSummaryView.setTextColor(
                            com.android.settingslib.Utils.getColorAttr(mActivity,
                                    android.R.attr.colorError));
                    mSummaryView.setText(
                            getString(R.string.wifi_add_app_network_save_failed_summary));
                }
                break;

            default:
                // Do nothing.
                break;
        }
    }
}
