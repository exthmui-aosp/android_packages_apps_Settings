/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.os.BatteryConsumer;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.SystemBatteryConsumer;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class ConvertUtilsTest {

    @Mock
    private BatteryUsageStats mBatteryUsageStats;
    @Mock
    private BatteryEntry mockBatteryEntry;
    @Mock
    private BatteryConsumer mockBatteryConsumer;
    @Mock
    private UidBatteryConsumer mockUidBatteryConsumer;
    @Mock
    private UserBatteryConsumer mockUserBatteryConsumer;
    @Mock
    private SystemBatteryConsumer mockSystemBatteryConsumer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert_returnsExpectedContentValues() {
        final int expectedType = 3;
        when(mockBatteryEntry.getUid()).thenReturn(1001);
        when(mockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mockBatteryEntry.getDefaultPackageName())
            .thenReturn("com.google.android.settings.battery");
        when(mockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        mockBatteryEntry.percent = 0.3;
        when(mockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mockBatteryEntry.getBatteryConsumer())
            .thenReturn(mockSystemBatteryConsumer);
        when(mockSystemBatteryConsumer.getDrainType()).thenReturn(expectedType);

        final ContentValues values =
            ConvertUtils.convert(
                mockBatteryEntry,
                mBatteryUsageStats,
                /*batteryLevel=*/ 12,
                /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                /*timestamp=*/ 10001L);

        assertThat(values.getAsLong("uid")).isEqualTo(1001L);
        assertThat(values.getAsLong("userId"))
            .isEqualTo(UserHandle.getUserId(1001));
        assertThat(values.getAsString("appLabel")).isEqualTo("Settings");
        assertThat(values.getAsString("packageName"))
            .isEqualTo("com.google.android.settings.battery");
        assertThat(values.getAsBoolean("isHidden")).isTrue();
        assertThat(values.getAsLong("timestamp")).isEqualTo(10001L);
        assertThat(values.getAsString("zoneId"))
            .isEqualTo(TimeZone.getDefault().getID());
        assertThat(values.getAsDouble("totalPower")).isEqualTo(5.1);
        assertThat(values.getAsDouble("consumePower")).isEqualTo(1.1);
        assertThat(values.getAsDouble("percentOfTotal")).isEqualTo(0.3);
        assertThat(values.getAsLong("foregroundUsageTimeInMs")).isEqualTo(1234L);
        assertThat(values.getAsLong("backgroundUsageTimeInMs")).isEqualTo(5689L);
        assertThat(values.getAsInteger("drainType")).isEqualTo(expectedType);
        assertThat(values.getAsInteger("consumerType"))
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(values.getAsInteger("batteryLevel")).isEqualTo(12);
        assertThat(values.getAsInteger("batteryStatus"))
            .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(values.getAsInteger("batteryHealth"))
            .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    @Test
    public void testConvert_nullBatteryEntry_returnsExpectedContentValues() {
        final ContentValues values =
            ConvertUtils.convert(
                /*entry=*/ null,
                /*batteryUsageStats=*/ null,
                /*batteryLevel=*/ 12,
                /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                /*timestamp=*/ 10001L);

        assertThat(values.getAsLong("timestamp")).isEqualTo(10001L);
        assertThat(values.getAsString("zoneId"))
            .isEqualTo(TimeZone.getDefault().getID());
        assertThat(values.getAsInteger("batteryLevel")).isEqualTo(12);
        assertThat(values.getAsInteger("batteryStatus"))
            .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(values.getAsInteger("batteryHealth"))
            .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
        assertThat(values.getAsString("packageName"))
            .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void testGetDrainType_returnsExpetcedResult() {
        final int expectedType = 3;
        when(mockSystemBatteryConsumer.getDrainType())
            .thenReturn(expectedType);

        assertThat(ConvertUtils.getDrainType(mockSystemBatteryConsumer))
            .isEqualTo(expectedType);
    }

    @Test
    public void testGetDrainType_notValidConsumer_returnsInvalidTypeValue() {
        assertThat(ConvertUtils.getDrainType(mockUserBatteryConsumer))
            .isEqualTo(ConvertUtils.INVALID_DRAIN_TYPE);
    }

    @Test
    public void testGetConsumerType_returnsExpetcedResult() {
        assertThat(ConvertUtils.getConsumerType(mockUidBatteryConsumer))
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        assertThat(ConvertUtils.getConsumerType(mockUserBatteryConsumer))
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_USER_BATTERY);
        assertThat(ConvertUtils.getConsumerType(mockSystemBatteryConsumer))
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
    }

    @Test
    public void testGetConsumeType_invalidConsumer_returnsInvalidType() {
          assertThat(ConvertUtils.getConsumerType(mockBatteryConsumer))
              .isEqualTo(ConvertUtils.CONSUMER_TYPE_UNKNOWN);
    }

    @Test
    public void testGetIndexedUsageMap_returnsExpectedResult() {
        // Creates the fake testing data.
        final int timeSlotSize = 2;
        final long[] batteryHistoryKeys = new long[] {101L, 102L, 103L, 104L, 105L};
        final Map<Long, List<BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        batteryHistoryMap.put(
            Long.valueOf(batteryHistoryKeys[0]),
            Arrays.asList(
                createBatteryHistEntry(
                    "package1", "label1", 5.0, 1L, 10L, 20L)));
        batteryHistoryMap.put(
            Long.valueOf(batteryHistoryKeys[1]), new ArrayList<BatteryHistEntry>());
        batteryHistoryMap.put(
            Long.valueOf(batteryHistoryKeys[2]),
            Arrays.asList(
                createBatteryHistEntry(
                    "package2", "label2", 10.0, 2L, 15L, 25L)));
        batteryHistoryMap.put(
            Long.valueOf(batteryHistoryKeys[3]),
            Arrays.asList(
                createBatteryHistEntry(
                    "package2", "label2", 15.0, 2L, 25L, 35L),
                createBatteryHistEntry(
                    "package3", "label3", 5.0, 2L, 5L, 5L)));
        batteryHistoryMap.put(
            Long.valueOf(batteryHistoryKeys[4]),
            Arrays.asList(
                createBatteryHistEntry(
                    "package2", "label2", 30.0, 2L, 30L, 40L),
                createBatteryHistEntry(
                    "package2", "label2", 75.0, 3L, 40L, 50L),
                createBatteryHistEntry(
                    "package3", "label3", 5.0, 2L, 5L, 5L)));

        final Map<Integer, List<BatteryDiffEntry>> resultMap =
            ConvertUtils.getIndexedUsageMap(
                timeSlotSize, batteryHistoryKeys, batteryHistoryMap);

        assertThat(resultMap).hasSize(2);
        // Verifies the first timestamp result.
        List<BatteryDiffEntry> entryList = resultMap.get(Integer.valueOf(0));
        assertThat(entryList).hasSize(1);
        assertBatteryDiffEntry(entryList.get(0), 100.0, 15L, 25L);
        // Verifies the second timestamp result.
        entryList = resultMap.get(Integer.valueOf(1));
        assertThat(entryList).hasSize(3);
        assertBatteryDiffEntry(entryList.get(0), 5.0, 5L, 5L);
        assertBatteryDiffEntry(entryList.get(1), 75.0, 40L, 50L);
        assertBatteryDiffEntry(entryList.get(2), 20.0, 15L, 15L);
    }

    private static BatteryHistEntry createBatteryHistEntry(
            String packageName, String appLabel, double consumePower,
            long userId, long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        // Only insert required fields.
        final ContentValues values = new ContentValues();
        values.put("packageName", packageName);
        values.put("appLabel", appLabel);
        values.put("userId", userId);
        values.put("consumePower", consumePower);
        values.put("foregroundUsageTimeInMs", foregroundUsageTimeInMs);
        values.put("backgroundUsageTimeInMs", backgroundUsageTimeInMs);
        return new BatteryHistEntry(values);
    }

    private static void assertBatteryDiffEntry(
            BatteryDiffEntry entry, double percentOfTotal,
            long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        assertThat(entry.getPercentOfTotal()).isEqualTo(percentOfTotal);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
    }
}
