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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Parcel;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EarlyWarningTipTest {
    private Context mContext;
    private EarlyWarningTip mEarlyWarningTip;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mEarlyWarningTip = new EarlyWarningTip(BatteryTip.StateType.NEW,
                false /* powerSaveModeOn */);
    }

    @Test
    public void testParcelable() {
        Parcel parcel = Parcel.obtain();
        mEarlyWarningTip.writeToParcel(parcel, mEarlyWarningTip.describeContents());
        parcel.setDataPosition(0);

        final EarlyWarningTip parcelTip = new EarlyWarningTip(parcel);

        assertThat(parcelTip.isPowerSaveModeOn()).isFalse();
    }

    @Test
    public void testInfo_stateNew_displayPowerModeInfo() {
        final EarlyWarningTip tip = new EarlyWarningTip(BatteryTip.StateType.NEW,
                false /* powerModeOn */);

        assertThat(tip.getTitle(mContext)).isEqualTo("Turn on Low Battery Mode");
        assertThat(tip.getSummary(mContext)).isEqualTo("Extend your battery life");
        assertThat(tip.getIconId()).isEqualTo(R.drawable.ic_battery_alert_24dp);
    }

    @Test
    public void testInfo_stateHandled_displayPowerModeHandledInfo() {
        final EarlyWarningTip tip = new EarlyWarningTip(BatteryTip.StateType.HANDLED,
                false /* powerModeOn */);

        assertThat(tip.getTitle(mContext)).isEqualTo("Low Battery Mode is on");
        assertThat(tip.getSummary(mContext)).isEqualTo("Some features are limited");
        assertThat(tip.getIconId()).isEqualTo(R.drawable.ic_perm_device_information_green_24dp);
    }

    @Test
    public void testUpdate_powerModeTurnedOn_typeBecomeHandled() {
        final EarlyWarningTip nextTip = new EarlyWarningTip(BatteryTip.StateType.INVISIBLE,
                true /* powerModeOn */);

        mEarlyWarningTip.updateState(nextTip);

        assertThat(mEarlyWarningTip.getState()).isEqualTo(BatteryTip.StateType.HANDLED);
    }
}
