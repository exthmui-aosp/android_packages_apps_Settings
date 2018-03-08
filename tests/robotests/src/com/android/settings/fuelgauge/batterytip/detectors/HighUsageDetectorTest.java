/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip.detectors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.BatteryStats;
import android.text.format.DateUtils;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.HighUsageDataParser;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class HighUsageDetectorTest {
    private static final int UID = 123;
    private static final long SCREEN_ON_TIME_MS = DateUtils.HOUR_IN_MILLIS;
    private Context mContext;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private HighUsageDataParser mDataParser;

    private AppInfo mAppInfo;
    private BatteryTipPolicy mPolicy;
    private HighUsageDetector mHighUsageDetector;
    private List<BatterySipper> mUsageList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPolicy = spy(new BatteryTipPolicy(mContext));
        mHighUsageDetector = spy(new HighUsageDetector(mContext, mPolicy, mBatteryStatsHelper));
        mHighUsageDetector.mBatteryUtils = mBatteryUtils;
        mHighUsageDetector.mDataParser = mDataParser;
        doNothing().when(mHighUsageDetector).parseBatteryData();
        doReturn(UID).when(mBatterySipper).getUid();
        mAppInfo = new AppInfo.Builder()
                .setUid(UID)
                .setScreenOnTimeMs(SCREEN_ON_TIME_MS)
                .build();

        mUsageList = new ArrayList<>();
        mUsageList.add(mBatterySipper);
    }

    @Test
    public void testDetect_disabledByPolicy_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "highUsageEnabled", false);

        assertThat(mHighUsageDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_containsHighUsageApp_tipVisible() {
        doReturn(true).when(mDataParser).isDeviceHeavilyUsed();
        when(mBatteryStatsHelper.getUsageList()).thenReturn(mUsageList);
        doReturn(SCREEN_ON_TIME_MS).when(mBatteryUtils).getProcessTimeMs(
                BatteryUtils.StatusType.FOREGROUND, mBatterySipper.uidObj,
                BatteryStats.STATS_SINCE_CHARGED);

        final HighUsageTip highUsageTip = (HighUsageTip) mHighUsageDetector.detect();
        assertThat(highUsageTip.isVisible()).isTrue();
        assertThat(highUsageTip.getHighUsageAppList()).containsExactly(mAppInfo);
    }
}
