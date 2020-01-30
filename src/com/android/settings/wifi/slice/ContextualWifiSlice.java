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

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settingslib.wifi.AccessPoint;

/**
 * {@link CustomSliceable} for Wi-Fi, used by contextual homepage.
 */
public class ContextualWifiSlice extends WifiSlice {

    @VisibleForTesting
    static final int COLLAPSED_ROW_COUNT = 0;

    @VisibleForTesting
    static long sActiveUiSession = -1000;
    @VisibleForTesting
    static boolean sToggleNeeded = true;

    public ContextualWifiSlice(Context context) {
        super(context);
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final long currentUiSession = FeatureFactory.getFactory(mContext)
                .getSlicesFeatureProvider().getUiSessionToken();
        if (currentUiSession != sActiveUiSession) {
            sActiveUiSession = currentUiSession;
            sToggleNeeded = !hasWorkingNetwork();
        } else if (!mWifiManager.isWifiEnabled()) {
            sToggleNeeded = true;
        }
        return super.getSlice();
    }

    static int getApRowCount() {
        return sToggleNeeded ? DEFAULT_EXPANDED_ROW_COUNT : COLLAPSED_ROW_COUNT;
    }

    @Override
    protected boolean isToggleNeeded() {
        return sToggleNeeded;
    }

    @Override
    protected ListBuilder.RowBuilder getHeaderRow(AccessPoint accessPoint) {
        final ListBuilder.RowBuilder builder = super.getHeaderRow(accessPoint);
        if (!sToggleNeeded) {
            builder.setTitleItem(getLevelIcon(accessPoint), ListBuilder.ICON_IMAGE)
                    .setSubtitle(getSubtitle(accessPoint));
        }
        return builder;
    }

    private IconCompat getLevelIcon(AccessPoint accessPoint) {
        if (accessPoint != null) {
            return getAccessPointLevelIcon(accessPoint);
        }

        final Drawable drawable = mContext.getDrawable(
                com.android.settingslib.Utils.getWifiIconResource(0));
        final int color = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.colorControlNormal);
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        return Utils.createIconWithDrawable(drawable);
    }

    private CharSequence getSubtitle(AccessPoint accessPoint) {
        if (isCaptivePortal()) {
            final int id = mContext.getResources()
                    .getIdentifier("network_available_sign_in", "string", "android");
            return mContext.getText(id);
        }

        if (accessPoint == null) {
            return mContext.getText(R.string.disconnected);
        }

        final NetworkInfo networkInfo = accessPoint.getNetworkInfo();
        if (networkInfo == null) {
            return mContext.getText(R.string.disconnected);
        }

        final State state = networkInfo.getState();
        DetailedState detailedState;
        if (state == State.CONNECTING) {
            detailedState = DetailedState.CONNECTING;
        } else if (state == State.CONNECTED) {
            detailedState = DetailedState.CONNECTED;
        } else {
            detailedState = networkInfo.getDetailedState();
        }

        final String[] formats = mContext.getResources().getStringArray(
                R.array.wifi_status_with_ssid);
        final int index = detailedState.ordinal();
        return String.format(formats[index], accessPoint.getTitle());
    }

    private boolean hasWorkingNetwork() {
        return !TextUtils.equals(getActiveSSID(), WifiManager.UNKNOWN_SSID) && hasInternetAccess();
    }

    private String getActiveSSID() {
        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return WifiManager.UNKNOWN_SSID;
        }
        return WifiInfo.sanitizeSsid(mWifiManager.getConnectionInfo().getSSID());
    }

    private boolean hasInternetAccess() {
        final NetworkCapabilities nc = mConnectivityManager.getNetworkCapabilities(
                mWifiManager.getCurrentNetwork());
        return nc != null
                && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
                && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return ContextualWifiScanWorker.class;
    }
}
