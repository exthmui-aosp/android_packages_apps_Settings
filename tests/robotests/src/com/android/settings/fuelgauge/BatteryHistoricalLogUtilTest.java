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

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

@RunWith(RobolectricTestRunner.class)
public final class BatteryHistoricalLogUtilTest {

    private final StringWriter mTestStringWriter = new StringWriter();
    private final PrintWriter mTestPrintWriter = new PrintWriter(mTestStringWriter);

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        BatteryHistoricalLogUtil.getSharedPreferences(mContext).edit().clear().commit();
    }

    @Test
    public void printHistoricalLog_withDefaultLogs() {
        BatteryHistoricalLogUtil.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);
        assertThat(mTestStringWriter.toString()).contains("No past logs");
    }

    @Test
    public void writeLog_withExpectedLogs() {
        BatteryHistoricalLogUtil.writeLog(mContext, Action.APPLY, "pkg1", "logs");
        BatteryHistoricalLogUtil.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);

        assertThat(mTestStringWriter.toString()).contains("pkg1\tAction:APPLY\tEvent:logs");
    }

    @Test
    public void writeLog_multipleLogs_withCorrectCounts() {
        for (int i = 0; i < BatteryHistoricalLogUtil.MAX_ENTRIES; i++) {
            BatteryHistoricalLogUtil.writeLog(mContext, Action.MANUAL, "pkg" + i, "logs");
        }
        BatteryHistoricalLogUtil.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);

        assertThat(mTestStringWriter.toString().split("MANUAL").length).isEqualTo(41);
    }

    @Test
    public void writeLog_overMaxEntriesLogs_withCorrectCounts() {
        for (int i = 0; i < BatteryHistoricalLogUtil.MAX_ENTRIES + 10; i++) {
            BatteryHistoricalLogUtil.writeLog(mContext, Action.RESET, "pkg" + i, "logs");
        }
        BatteryHistoricalLogUtil.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);

        assertThat(mTestStringWriter.toString().split("RESET").length).isEqualTo(41);
    }
}
