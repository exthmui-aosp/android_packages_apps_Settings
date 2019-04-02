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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class AdvancedBluetoothDetailsHeaderControllerTest{
    private static final int BATTERY_LEVEL_MAIN = 30;
    private static final int BATTERY_LEVEL_LEFT = 25;
    private static final int BATTERY_LEVEL_RIGHT = 45;
    private static final String ICON_URI = "content://test.provider/icon.png";
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";

    private Context mContext;

    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private Bitmap mBitmap;
    @Mock
    private ImageView mImageView;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    private AdvancedBluetoothDetailsHeaderController mController;
    private LayoutPreference mLayoutPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new AdvancedBluetoothDetailsHeaderController(mContext, "pref_Key");
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        mController.init(mCachedDevice);
        mLayoutPreference = new LayoutPreference(mContext,
                LayoutInflater.from(mContext).inflate(R.layout.advanced_bt_entity_header, null));
        mController.mLayoutPreference = mLayoutPreference;
        mController.mBluetoothAdapter = mBluetoothAdapter;
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
    }

    @Test
    public void createBatteryIcon_hasCorrectInfo() {
        final Drawable drawable = mController.createBtBatteryIcon(mContext, BATTERY_LEVEL_MAIN,
                true /* charging */);
        assertThat(drawable).isInstanceOf(BatteryMeterView.BatteryMeterDrawable.class);

        final BatteryMeterView.BatteryMeterDrawable iconDrawable =
                (BatteryMeterView.BatteryMeterDrawable) drawable;
        assertThat(iconDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL_MAIN);
        assertThat(iconDrawable.getCharging()).isTrue();
    }

    @Test
    public void refresh_connected_updateCorrectInfo() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_LEFT));
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_RIGHT_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_RIGHT));
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_CASE_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_MAIN));
        when(mCachedDevice.isConnected()).thenReturn(true);
        mController.refresh();

        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_left), BATTERY_LEVEL_LEFT);
        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_right), BATTERY_LEVEL_RIGHT);
        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_middle), BATTERY_LEVEL_MAIN);
    }

    @Test
    public void refresh_disconnected_updateCorrectInfo() {
        when(mCachedDevice.isConnected()).thenReturn(false);

        mController.refresh();

        final LinearLayout layout = mLayoutPreference.findViewById(R.id.layout_middle);

        assertThat(mLayoutPreference.findViewById(R.id.layout_left).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_right).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(layout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(layout.findViewById(R.id.header_title).getVisibility()).isEqualTo(View.GONE);
        assertThat(layout.findViewById(R.id.bt_battery_summary).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(layout.findViewById(R.id.bt_battery_icon).getVisibility()).isEqualTo(View.GONE);
        assertThat(layout.findViewById(R.id.header_icon).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void getAvailabilityStatus_unthetheredHeadset_returnAvailable() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET))
                .thenReturn("true");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notUnthetheredHeadset_returnUnavailable() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET))
                .thenReturn("false");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateIcon_existInCache_setImageBitmap() {
        mController.mIconCache.put(ICON_URI, mBitmap);

        mController.updateIcon(mImageView, ICON_URI);

        verify(mImageView).setImageBitmap(mBitmap);
    }

    @Test
    public void onStart_isAvailable_registerCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET))
                .thenReturn("true");

        mController.onStart();

        verify(mBluetoothAdapter).registerMetadataListener(mBluetoothDevice,
                mController.mMetadataListener, mController.mHandler);
    }

    @Test
    public void onStop_isAvailable_unregisterCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET))
                .thenReturn("true");

        mController.onStop();

        verify(mBluetoothAdapter).unregisterMetadataListener(mBluetoothDevice);
    }

    @Test
    public void onStart_notAvailable_registerCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET))
                .thenReturn("false");

        mController.onStart();

        verify(mBluetoothAdapter, never()).registerMetadataListener(mBluetoothDevice,
                mController.mMetadataListener, mController.mHandler);
    }

    @Test
    public void onStop_notAvailable_unregisterCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET))
                .thenReturn("false");

        mController.onStop();

        verify(mBluetoothAdapter, never()).unregisterMetadataListener(mBluetoothDevice);
    }

    private void assertBatteryLevel(LinearLayout linearLayout, int batteryLevel) {
        final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
        assertThat(textView.getText().toString()).isEqualTo(
                com.android.settings.Utils.formatPercentage(batteryLevel));
    }

}
