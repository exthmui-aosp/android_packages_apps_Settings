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

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
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

    private Context mContext;

    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    private AdvancedBluetoothDetailsHeaderController mController;
    private LayoutPreference mLayoutPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new AdvancedBluetoothDetailsHeaderController(mContext, "pref_Key");
        mController.init(mCachedDevice);
        mLayoutPreference = new LayoutPreference(mContext,
                LayoutInflater.from(mContext).inflate(R.layout.advanced_bt_entity_header, null));
        mController.mLayoutPreference = mLayoutPreference;
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
    }

    @Test
    public void createBatteryIcon_hasCorrectInfo() {
        final Drawable drawable = mController.createBtBatteryIcon(mContext, BATTERY_LEVEL_MAIN);
        assertThat(drawable).isInstanceOf(BatteryMeterView.BatteryMeterDrawable.class);

        final BatteryMeterView.BatteryMeterDrawable iconDrawable =
                (BatteryMeterView.BatteryMeterDrawable) drawable;
        assertThat(iconDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL_MAIN);
    }

    @Test
    public void refresh_updateCorrectInfo() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_LEFT));
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_RIGHT_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_RIGHT));
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_CASE_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_MAIN));
        mController.refresh();

        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_left), BATTERY_LEVEL_LEFT);
        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_right), BATTERY_LEVEL_RIGHT);
        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_middle), BATTERY_LEVEL_MAIN);
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

    private void assertBatteryLevel(LinearLayout linearLayout, int batteryLevel) {
        final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
        assertThat(textView.getText().toString()).isEqualTo(
                com.android.settings.Utils.formatPercentage(batteryLevel));
    }

}
