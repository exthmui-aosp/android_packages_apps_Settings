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

package com.android.settings.wifi.slice;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.provider.SettingsSlicesContract.KEY_WIFI;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.wifi.WifiDialogActivity;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link CustomSliceable} for Wi-Fi, used by generic clients.
 */
public class WifiSlice implements CustomSliceable {

    @VisibleForTesting
    static final int DEFAULT_EXPANDED_ROW_COUNT = 3;

    protected final WifiManager mWifiManager;
    private final Context mContext;

    public WifiSlice(Context context) {
        mContext = context;
        mWifiManager = mContext.getSystemService(WifiManager.class);
    }

    @Override
    public Uri getUri() {
        return WIFI_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final boolean isWifiEnabled = isWifiEnabled();

        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        final String title = mContext.getString(R.string.wifi_settings);
        final CharSequence summary = getSummary();
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        final PendingIntent primaryAction = getPrimaryAction();
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */, isWifiEnabled);

        final ListBuilder listBuilder = new ListBuilder(mContext, WIFI_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(title)
                        .setSubtitle(summary)
                        .addEndItem(toggleSliceAction)
                        .setPrimaryAction(primarySliceAction));

        if (!isWifiEnabled) {
            return listBuilder.build();
        }

        final List<AccessPoint> results =
                SliceBackgroundWorker.getInstance(mContext, this).getResults();

        // Need a loading text when results are not ready.
        boolean needLoadingRow = results == null;
        final int apCount = needLoadingRow ? 0 : results.size();

        // Add AP rows
        final CharSequence placeholder = mContext.getText(R.string.summary_placeholder);
        for (int i = 0; i < DEFAULT_EXPANDED_ROW_COUNT; i++) {
            if (i < apCount) {
                listBuilder.addRow(getAccessPointRow(results.get(i)));
            } else if (needLoadingRow) {
                listBuilder.addRow(new ListBuilder.RowBuilder()
                        .setTitle(mContext.getText(R.string.wifi_empty_list_wifi_on))
                        .setSubtitle(placeholder));
                needLoadingRow = false;
            } else {
                listBuilder.addRow(new ListBuilder.RowBuilder()
                        .setTitle(placeholder)
                        .setSubtitle(placeholder));
            }
        }
        return listBuilder.build();
    }

    private ListBuilder.RowBuilder getAccessPointRow(AccessPoint accessPoint) {
        final String title = accessPoint.getConfigName();
        final IconCompat levelIcon = IconCompat.createWithResource(mContext,
                com.android.settingslib.Utils.getWifiIconResource(accessPoint.getLevel()));
        final CharSequence apSummary = accessPoint.getSettingsSummary();
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitleItem(levelIcon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(!TextUtils.isEmpty(apSummary)
                        ? apSummary
                        : mContext.getText(R.string.summary_placeholder))
                .setPrimaryAction(SliceAction.create(
                        getAccessPointAction(accessPoint), levelIcon, ListBuilder.ICON_IMAGE,
                        title));

        final IconCompat endIcon = getEndIcon(accessPoint);
        if (endIcon != null) {
            rowBuilder.addEndItem(endIcon, ListBuilder.ICON_IMAGE);
        }
        return rowBuilder;
    }

    private IconCompat getEndIcon(AccessPoint accessPoint) {
        if (accessPoint.isActive()) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_settings);
        } else if (accessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_lock_closed);
        } else if (accessPoint.isMetered()) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_money);
        }
        return null;
    }

    private PendingIntent getAccessPointAction(AccessPoint accessPoint) {
        final Bundle extras = new Bundle();
        accessPoint.saveWifiState(extras);

        Intent intent;
        if (accessPoint.isActive()) {
            intent = new SubSettingLauncher(mContext)
                    .setTitleRes(R.string.pref_title_network_details)
                    .setDestination(WifiNetworkDetailsFragment.class.getName())
                    .setArguments(extras)
                    .setSourceMetricsCategory(MetricsEvent.WIFI)
                    .toIntent();
        } else {
            intent = new Intent(mContext, WifiDialogActivity.class);
            intent.putExtra(WifiDialogActivity.KEY_ACCESS_POINT_STATE, extras);
        }
        return PendingIntent.getActivity(mContext, accessPoint.hashCode() /* requestCode */,
                intent, 0 /* flags */);
    }

    /**
     * Update the current wifi status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    @Override
    public void onNotifyChange(Intent intent) {
        final boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                mWifiManager.isWifiEnabled());
        mWifiManager.setWifiEnabled(newState);
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link WifiScanWorker}
        // handle it.
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.wifi_settings).toString();
        final Uri contentUri = new Uri.Builder().appendPath(KEY_WIFI).build();
        final Intent intent = SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                WifiSettings.class.getName(), KEY_WIFI, screenTitle,
                MetricsEvent.DIALOG_WIFI_AP_EDIT)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);

        return intent;
    }

    protected String getActiveSSID() {
        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return WifiSsid.NONE;
        }
        return WifiInfo.removeDoubleQuotes(mWifiManager.getConnectionInfo().getSSID());
    }

    private boolean isWifiEnabled() {
        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                return true;
            default:
                return false;
        }
    }

    private CharSequence getSummary() {
        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
                final String ssid = getActiveSSID();
                if (TextUtils.equals(ssid, WifiSsid.NONE)) {
                    return mContext.getText(R.string.disconnected);
                }
                return ssid;
            case WifiManager.WIFI_STATE_ENABLING:
                return mContext.getText(R.string.disconnected);
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
                return mContext.getText(R.string.switch_off_text);
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return "";
        }
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, 0 /* flags */);
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return WifiScanWorker.class;
    }

    public static class WifiScanWorker extends SliceBackgroundWorker<AccessPoint>
            implements WifiTracker.WifiListener {

        private final Context mContext;

        private WifiTracker mWifiTracker;

        public WifiScanWorker(Context context, Uri uri) {
            super(context, uri);
            mContext = context;
        }

        @Override
        protected void onSlicePinned() {
            if (mWifiTracker == null) {
                mWifiTracker = new WifiTracker(mContext, this /* wifiListener */,
                        true /* includeSaved */, true /* includeScans */);
            }
            mWifiTracker.onStart();
            onAccessPointsChanged();
        }

        @Override
        protected void onSliceUnpinned() {
            mWifiTracker.onStop();
        }

        @Override
        public void close() {
            mWifiTracker.onDestroy();
        }

        @Override
        public void onWifiStateChanged(int state) {
            mContext.getContentResolver().notifyChange(getUri(), null);
        }

        @Override
        public void onConnectedChanged() {
        }

        @Override
        public void onAccessPointsChanged() {
            // in case state has changed
            if (!mWifiTracker.getManager().isWifiEnabled()) {
                updateResults(null);
                return;
            }
            // AccessPoints are sorted by the WifiTracker
            final List<AccessPoint> accessPoints = mWifiTracker.getAccessPoints();
            final List<AccessPoint> resultList = new ArrayList<>();
            for (AccessPoint ap : accessPoints) {
                if (ap.isReachable()) {
                    resultList.add(ap);
                }
            }
            updateResults(resultList);
        }
    }
}
