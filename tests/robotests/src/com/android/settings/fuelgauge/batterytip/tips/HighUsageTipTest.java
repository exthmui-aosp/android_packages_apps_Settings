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
import android.text.format.DateUtils;

import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.batterytip.HighUsageApp;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class HighUsageTipTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final long SCREEN_TIME = 30 * DateUtils.MINUTE_IN_MILLIS;

    private Context mContext;
    private HighUsageTip mBatteryTip;
    private List<HighUsageApp> mUsageAppList;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mUsageAppList = new ArrayList<>();
        mUsageAppList.add(new HighUsageApp(PACKAGE_NAME, SCREEN_TIME));
        mBatteryTip = new HighUsageTip(SCREEN_TIME, mUsageAppList);
    }

    @Test
    public void testParcelable() {

        Parcel parcel = Parcel.obtain();
        mBatteryTip.writeToParcel(parcel, mBatteryTip.describeContents());
        parcel.setDataPosition(0);

        final HighUsageTip parcelTip = new HighUsageTip(parcel);

        assertThat(parcelTip.getTitle(mContext)).isEqualTo("Phone used heavily");
        assertThat(parcelTip.getType()).isEqualTo(BatteryTip.TipType.HIGH_DEVICE_USAGE);
        assertThat(parcelTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
        assertThat(parcelTip.getScreenTimeMs()).isEqualTo(SCREEN_TIME);
        assertThat(parcelTip.mHighUsageAppList.size()).isEqualTo(1);
        final HighUsageApp app = parcelTip.mHighUsageAppList.get(0);
        assertThat(app.packageName).isEqualTo(PACKAGE_NAME);
        assertThat(app.screenOnTimeMs).isEqualTo(SCREEN_TIME);
    }
}
