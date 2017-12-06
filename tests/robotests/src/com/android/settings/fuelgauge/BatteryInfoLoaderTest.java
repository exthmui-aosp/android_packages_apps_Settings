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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryStats;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.TestConfig;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryInfoLoaderTest {
    private static final long TEST_TIME_REMAINING = 1000L;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mHelper;
    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private PowerUsageFeatureProvider mProvider;
    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStats mStats;

    private Intent mDisChargingBatteryBroadcast;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mProvider = FakeFeatureFactory
                .setupForTest()
                .getPowerUsageFeatureProvider(mContext);

        mDisChargingBatteryBroadcast = BatteryTestUtils.getDischargingIntent();

        doReturn(mContext).when(mContext).getApplicationContext();
        when(mStats.computeBatteryTimeRemaining(anyLong())).thenReturn(TEST_TIME_REMAINING);
        doReturn(mStats).when(mHelper).getStats();
        doReturn(mDisChargingBatteryBroadcast).when(mContext).registerReceiver(any(), any());
    }

    @Test
    public void test_loadInBackground_dischargingOldEstimate_dischargingLabelNotNull() {
        BatteryInfoLoader loader = new BatteryInfoLoader(mContext, mHelper);

        BatteryInfo info = loader.loadInBackground();

        assertThat(info.remainingLabel).isNotNull();
        assertThat(info.remainingTimeUs).isEqualTo(TEST_TIME_REMAINING);
    }
}
