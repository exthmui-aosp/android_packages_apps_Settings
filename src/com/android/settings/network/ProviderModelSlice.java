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


import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.slices.CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI;

import android.annotation.ColorInt;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.WindowManager.LayoutParams;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.NetworkProviderWorker;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.slice.WifiSlice;
import com.android.settings.wifi.slice.WifiSliceItem;
import com.android.wifitrackerlib.WifiEntry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link CustomSliceable} for Wi-Fi and mobile data connection, used by generic clients.
 */
// ToDo If the provider model become default design in the future, the code needs to refactor
// the whole structure and use new "data object", and then split provider model out of old design.
public class ProviderModelSlice extends WifiSlice {

    private static final String TAG = "ProviderModelSlice";
    protected static final String PREF_NAME = "ProviderModelSlice";
    protected static final String PREF_HAS_TURNED_OFF_MOBILE_DATA = "PrefHasTurnedOffMobileData";

    private final ProviderModelSliceHelper mHelper;
    private final SharedPreferences mSharedPref;

    public ProviderModelSlice(Context context) {
        super(context);
        mHelper = getHelper();
        mSharedPref = getSharedPreference();
    }

    @Override
    public Uri getUri() {
        return PROVIDER_MODEL_SLICE_URI;
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    protected boolean isApRowCollapsed() {
        return false;
    }

    @Override
    public Slice getSlice() {
        // The provider model slice step:
        // First section:  Add a Wi-Fi item which state is connected.
        // Second section:  Add a carrier item.
        // Third section:  Add the Wi-Fi items which are not connected.
        // Fourth section:  If device has connection problem, this row show the message for user.
        @InternetUpdater.InternetType int internetType = getInternetType();
        final ListBuilder listBuilder = mHelper.createListBuilder(getUri());
        if (mHelper.isAirplaneModeEnabled() && !mWifiManager.isWifiEnabled()
                && internetType != InternetUpdater.INTERNET_ETHERNET) {
            log("Airplane mode is enabled.");
            return listBuilder.build();
        }

        int maxListSize = 0;
        List<WifiSliceItem> wifiList = null;
        final NetworkProviderWorker worker = getWorker();
        if (worker != null) {
            // get Wi-Fi list.
            wifiList = worker.getResults();
            maxListSize = worker.getApRowCount();
        } else {
            log("network provider worker is null.");
        }

        final boolean hasCarrier = mHelper.hasCarrier();
        log("hasCarrier: " + hasCarrier);

        // First section:  Add a Ethernet or Wi-Fi item which state is connected.
        boolean isConnectedWifiAddedTop = false;
        final WifiSliceItem connectedWifiItem = mHelper.getConnectedWifiItem(wifiList);
        if (internetType == InternetUpdater.INTERNET_ETHERNET) {
            log("get Ethernet item which is connected");
            listBuilder.addRow(createEthernetRow());
            maxListSize--;
        } else {
            if (connectedWifiItem != null && internetType == InternetUpdater.INTERNET_WIFI) {
                log("get Wi-Fi item which is connected to internet");
                listBuilder.addRow(getWifiSliceItemRow(connectedWifiItem));
                isConnectedWifiAddedTop = true;
                maxListSize--;
            }
        }

        // Second section:  Add a carrier item.
        if (hasCarrier) {
            mHelper.updateTelephony();
            listBuilder.addRow(
                    mHelper.createCarrierRow(
                            worker != null ? worker.getNetworkTypeDescription() : ""));
            maxListSize--;
        }

        // Third section:  Add the connected Wi-Fi item to Wi-Fi list if the Ethernet is connected.
        if (connectedWifiItem != null && !isConnectedWifiAddedTop) {
            log("get Wi-Fi item which is connected");
            listBuilder.addRow(getWifiSliceItemRow(connectedWifiItem));
            maxListSize--;
        }

        // Fourth section:  Add the Wi-Fi items which are not connected.
        if (wifiList != null && wifiList.size() > 0) {
            log("get Wi-Fi items which are not connected. Wi-Fi items : " + wifiList.size());

            final List<WifiSliceItem> disconnectedWifiList = wifiList.stream()
                    .filter(wifiSliceItem -> wifiSliceItem.getConnectedState()
                            != WifiEntry.CONNECTED_STATE_CONNECTED)
                    .limit(maxListSize - 1)
                    .collect(Collectors.toList());
            for (WifiSliceItem item : disconnectedWifiList) {
                listBuilder.addRow(getWifiSliceItemRow(item));
            }
            listBuilder.addRow(getSeeAllRow());
        }
        return listBuilder.build();
    }

    @Override
    public PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(getUri().toString())
                // The FLAG_RECEIVER_FOREGROUND flag is necessary to avoid the intent delay of
                // the first sending after the device restarts
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .setData(getUri())
                .setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    /**
     * Update the current carrier's mobile data status.
     */
    @Override
    public void onNotifyChange(Intent intent) {
        final SubscriptionManager subscriptionManager = mHelper.getSubscriptionManager();
        if (subscriptionManager == null) {
            return;
        }
        final int defaultSubId = subscriptionManager.getDefaultDataSubscriptionId();
        log("defaultSubId:" + defaultSubId);

        if (!defaultSubscriptionIsUsable(defaultSubId)) {
            return;
        }

        boolean isToggleAction = intent.hasExtra(EXTRA_TOGGLE_STATE);
        boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                mHelper.isMobileDataEnabled());

        if (isToggleAction) {
            // The ToggleAction is used to set mobile data enabled.
            if (!newState && mSharedPref != null
                    && mSharedPref.getBoolean(PREF_HAS_TURNED_OFF_MOBILE_DATA, true)) {
                String carrierName = mHelper.getMobileTitle();
                if (carrierName.equals(mContext.getString(R.string.mobile_data_settings_title))) {
                    carrierName = mContext.getString(
                            R.string.mobile_data_disable_message_default_carrier);
                }
                showMobileDataDisableDialog(getMobileDataDisableDialog(defaultSubId, carrierName));
            } else {
                MobileNetworkUtils.setMobileDataEnabled(mContext, defaultSubId, newState,
                        false /* disableOtherSubscriptions */);
            }
        }

        final boolean isDataEnabled =
                isToggleAction ? newState : MobileNetworkUtils.isMobileDataEnabled(mContext);
        doCarrierNetworkAction(isToggleAction, isDataEnabled, defaultSubId);
    }

    @VisibleForTesting
    AlertDialog getMobileDataDisableDialog(int defaultSubId, String carrierName) {
        return new Builder(mContext)
                .setTitle(R.string.mobile_data_disable_title)
                .setMessage(mContext.getString(R.string.mobile_data_disable_message,
                        carrierName))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        com.android.internal.R.string.alert_windows_notification_turn_off_action,
                        (dialog, which) -> {
                            MobileNetworkUtils.setMobileDataEnabled(mContext, defaultSubId,
                                    false /* enabled */,
                                    false /* disableOtherSubscriptions */);
                            if (mSharedPref != null) {
                                SharedPreferences.Editor editor = mSharedPref.edit();
                                editor.putBoolean(PREF_HAS_TURNED_OFF_MOBILE_DATA, false);
                                editor.apply();
                            }
                        })
                .create();
    }

    private void showMobileDataDisableDialog(AlertDialog dialog) {
        if (dialog == null) {
            log("AlertDialog is null");
            return;
        }

        dialog.getWindow().setType(LayoutParams.TYPE_KEYGUARD_DIALOG);
        dialog.show();
    }

    @VisibleForTesting
    void doCarrierNetworkAction(boolean isToggleAction, boolean isDataEnabled, int subId) {
        final NetworkProviderWorker worker = getWorker();
        if (worker == null) {
            return;
        }

        if (isToggleAction) {
            worker.setCarrierNetworkEnabledIfNeeded(isDataEnabled, subId);
            return;
        }

        if (isDataEnabled) {
            worker.connectCarrierNetwork();
        }
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.provider_internet_settings).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                NetworkProviderSettings.class.getName(), "" /* key */, screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(getUri());
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return NetworkProviderWorker.class;
    }

    @VisibleForTesting
    ProviderModelSliceHelper getHelper() {
        return new ProviderModelSliceHelper(mContext, this);
    }

    @VisibleForTesting
    NetworkProviderWorker getWorker() {
        return SliceBackgroundWorker.getInstance(getUri());
    }

    @VisibleForTesting
    SharedPreferences getSharedPreference() {
        return mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private @InternetUpdater.InternetType int getInternetType() {
        final NetworkProviderWorker worker = getWorker();
        if (worker == null) {
            return InternetUpdater.INTERNET_NETWORKS_AVAILABLE;
        }
        return worker.getInternetType();
    }

    @VisibleForTesting
    ListBuilder.RowBuilder createEthernetRow() {
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder();
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_settings_ethernet);
        if (drawable != null) {
            drawable.setTintList(Utils.getColorAttr(mContext, android.R.attr.colorAccent));
            rowBuilder.setTitleItem(Utils.createIconWithDrawable(drawable), ListBuilder.ICON_IMAGE);
        }
        return rowBuilder
                .setTitle(mContext.getText(R.string.ethernet))
                .setSubtitle(mContext.getText(R.string.to_switch_networks_disconnect_ethernet));
    }

    protected ListBuilder.RowBuilder getSeeAllRow() {
        final CharSequence title = mContext.getText(R.string.previous_connected_see_all);
        final IconCompat icon = getSeeAllIcon();
        return new ListBuilder.RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(getPrimaryAction(icon, title));
    }

    protected IconCompat getSeeAllIcon() {
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_arrow_forward);
        if (drawable != null) {
            drawable.setTint(
                    Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorControlNormal));
            return Utils.createIconWithDrawable(drawable);
        }
        return Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    protected SliceAction getPrimaryAction(IconCompat icon, CharSequence title) {
        final PendingIntent intent = PendingIntent.getActivity(mContext, 0 /* requestCode */,
                getIntent(), PendingIntent.FLAG_IMMUTABLE /* flags */);
        return SliceAction.createDeeplink(intent, icon, ListBuilder.ICON_IMAGE, title);
    }

    @Override
    protected IconCompat getWifiSliceItemLevelIcon(WifiSliceItem wifiSliceItem) {
        if (wifiSliceItem.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED
                && getInternetType() != InternetUpdater.INTERNET_WIFI) {
            final @ColorInt int tint = Utils.getColorAttrDefaultColor(mContext,
                    android.R.attr.colorControlNormal);
            final Drawable drawable = mContext.getDrawable(
                    WifiUtils.getInternetIconResource(
                            wifiSliceItem.getLevel(), wifiSliceItem.shouldShowXLevelIcon()));
            drawable.setTint(tint);
            return Utils.createIconWithDrawable(drawable);
        }
        return super.getWifiSliceItemLevelIcon(wifiSliceItem);
    }

    /**
     * Wrap the subscriptionManager call for test mocking.
     */
    @VisibleForTesting
    protected boolean defaultSubscriptionIsUsable(int defaultSubId) {
        return SubscriptionManager.isUsableSubscriptionId(defaultSubId);
    }
}
