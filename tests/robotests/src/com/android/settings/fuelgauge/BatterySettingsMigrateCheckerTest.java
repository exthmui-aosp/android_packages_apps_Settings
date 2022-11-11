/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.android.settings.fuelgauge.batterysaver.BatterySaverScheduleRadioButtonsController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class BatterySettingsMigrateCheckerTest {

    private Context mContext;
    private BatterySettingsMigrateChecker mBatterySettingsMigrateChecker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mBatterySettingsMigrateChecker = new BatterySettingsMigrateChecker();
    }

    @Test
    public void onReceive_invalidScheduledLevel_resetScheduledValue() {
        final int invalidScheduledLevel = 5;
        setScheduledLevel(invalidScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, new Intent());

        assertThat(getScheduledLevel())
                .isEqualTo(BatterySaverScheduleRadioButtonsController.TRIGGER_LEVEL_MIN);
    }

    @Test
    public void onReceive_validScheduledLevel_notResetScheduledValue() {
        final int validScheduledLevel = 12;
        setScheduledLevel(validScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, new Intent());

        assertThat(getScheduledLevel()).isEqualTo(validScheduledLevel);
    }

    @Test
    public void onReceive_validSpecialScheduledLevel_notResetScheduledValue() {
        final int validScheduledLevel = 0;
        setScheduledLevel(validScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, new Intent());

        assertThat(getScheduledLevel()).isEqualTo(validScheduledLevel);
    }

    private void setScheduledLevel(int scheduledLevel) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, scheduledLevel);
    }

    private int getScheduledLevel() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, /*defaultValue*/ 0);
    }
}
