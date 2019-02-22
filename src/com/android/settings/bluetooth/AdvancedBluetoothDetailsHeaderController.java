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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class AdvancedBluetoothDetailsHeaderController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, CachedBluetoothDevice.Callback {

    @VisibleForTesting
    LayoutPreference mLayoutPreference;
    private CachedBluetoothDevice mCachedDevice;

    public AdvancedBluetoothDetailsHeaderController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean unthetheredHeadset = Utils.getBooleanMetaData(mCachedDevice.getDevice(),
                BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET);
        return unthetheredHeadset ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        mLayoutPreference.setVisible(isAvailable());
        refresh();
    }

    @Override
    public void onStart() {
        mCachedDevice.registerCallback(this::onDeviceAttributesChanged);
    }

    @Override
    public void onStop() {
        mCachedDevice.unregisterCallback(this::onDeviceAttributesChanged);
    }

    public void init(CachedBluetoothDevice cachedBluetoothDevice) {
        mCachedDevice = cachedBluetoothDevice;
    }

    @VisibleForTesting
    void refresh() {
        if (mLayoutPreference != null && mCachedDevice != null) {
            final TextView title = mLayoutPreference.findViewById(R.id.entity_header_title);
            title.setText(mCachedDevice.getName());
            final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
            summary.setText(mCachedDevice.getConnectionSummary());

            if (!mCachedDevice.isConnected()) {
                updateDisconnectLayout();
                return;
            }

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_left),
                    BluetoothDevice.METADATA_UNTHETHERED_LEFT_ICON,
                    BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY,
                    BluetoothDevice.METADATA_UNTHETHERED_LEFT_CHARGING,
                    R.string.bluetooth_left_name);

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_middle),
                    BluetoothDevice.METADATA_UNTHETHERED_CASE_ICON,
                    BluetoothDevice.METADATA_UNTHETHERED_CASE_BATTERY,
                    BluetoothDevice.METADATA_UNTHETHERED_CASE_CHARGING,
                    R.string.bluetooth_middle_name);

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_right),
                    BluetoothDevice.METADATA_UNTHETHERED_RIGHT_ICON,
                    BluetoothDevice.METADATA_UNTHETHERED_RIGHT_BATTERY,
                    BluetoothDevice.METADATA_UNTHETHERED_RIGHT_CHARGING,
                    R.string.bluetooth_right_name);
        }
    }

    @VisibleForTesting
    Drawable createBtBatteryIcon(Context context, int level, boolean charging) {
        final BatteryMeterView.BatteryMeterDrawable drawable =
                new BatteryMeterView.BatteryMeterDrawable(context,
                        context.getColor(R.color.meter_background_color));
        drawable.setBatteryLevel(level);
        drawable.setShowPercent(false);
        drawable.setBatteryColorFilter(new PorterDuffColorFilter(
                com.android.settings.Utils.getColorAttrDefaultColor(context,
                        android.R.attr.colorControlNormal),
                PorterDuff.Mode.SRC_IN));
        drawable.setCharging(charging);

        return drawable;
    }

    private void updateSubLayout(LinearLayout linearLayout, int iconMetaKey, int batteryMetaKey,
            int chargeMetaKey, int titleResId) {
        if (linearLayout == null) {
            return;
        }
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        final String iconUri = Utils.getStringMetaData(bluetoothDevice, iconMetaKey);
        if (iconUri != null) {
            final ImageView imageView = linearLayout.findViewById(R.id.header_icon);
            final IconCompat iconCompat = IconCompat.createWithContentUri(iconUri);
            imageView.setImageBitmap(iconCompat.getBitmap());
        }

        final int batteryLevel = Utils.getIntMetaData(bluetoothDevice, batteryMetaKey);
        final boolean charging = Utils.getBooleanMetaData(bluetoothDevice, chargeMetaKey);
        if (batteryLevel != Utils.META_INT_ERROR) {
            linearLayout.setVisibility(View.VISIBLE);
            final ImageView imageView = linearLayout.findViewById(R.id.bt_battery_icon);
            imageView.setImageDrawable(createBtBatteryIcon(mContext, batteryLevel, charging));
            imageView.setVisibility(View.VISIBLE);
            final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
            textView.setText(com.android.settings.Utils.formatPercentage(batteryLevel));
            textView.setVisibility(View.VISIBLE);
        } else {
            // Hide it if it doesn't have battery information
            linearLayout.setVisibility(View.GONE);
        }

        final TextView textView = linearLayout.findViewById(R.id.header_title);
        textView.setText(titleResId);
        textView.setVisibility(View.VISIBLE);
    }

    private void updateDisconnectLayout() {
        mLayoutPreference.findViewById(R.id.layout_left).setVisibility(View.GONE);
        mLayoutPreference.findViewById(R.id.layout_right).setVisibility(View.GONE);

        // Hide title, battery icon and battery summary
        final LinearLayout linearLayout = mLayoutPreference.findViewById(R.id.layout_middle);
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout.findViewById(R.id.header_title).setVisibility(View.GONE);
        linearLayout.findViewById(R.id.bt_battery_summary).setVisibility(View.GONE);
        linearLayout.findViewById(R.id.bt_battery_icon).setVisibility(View.GONE);

        // Only show bluetooth icon
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        final String iconUri = Utils.getStringMetaData(bluetoothDevice,
                BluetoothDevice.METADATA_MAIN_ICON);
        if (iconUri != null) {
            final ImageView imageView = linearLayout.findViewById(R.id.header_icon);
            final IconCompat iconCompat = IconCompat.createWithContentUri(iconUri);
            imageView.setImageBitmap(iconCompat.getBitmap());
        }
    }

    @Override
    public void onDeviceAttributesChanged() {
        if (mCachedDevice != null) {
            refresh();
        }
    }
}
