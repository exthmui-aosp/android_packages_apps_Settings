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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserManager;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class DataProcessManagerTest {
    private Context mContext;
    private DataProcessManager mDataProcessManager;

    @Mock
    private IUsageStatsManager mUsageStatsManager;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        DataProcessor.sUsageStatsManager = mUsageStatsManager;
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mUserManager)
                .when(mContext)
                .getSystemService(UserManager.class);

        mDataProcessManager = new DataProcessManager(
                mContext, /*handler=*/ null,  /*callbackFunction=*/ null,
                /*hourlyBatteryLevelsPerDay=*/ new ArrayList<>(), /*batteryHistoryMap=*/ null);
    }

    @Test
    public void start_loadEmptyDatabaseAppUsageData() {
        final MatrixCursor cursor = new MatrixCursor(
                new String[]{
                        AppUsageEventEntity.KEY_UID,
                        AppUsageEventEntity.KEY_PACKAGE_NAME,
                        AppUsageEventEntity.KEY_TIMESTAMP});
        DatabaseUtils.sFakeAppUsageEventSupplier = () -> cursor;
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getIsCurrentAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsDatabaseAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsCurrentBatteryHistoryLoaded()).isTrue();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isTrue();
        assertThat(mDataProcessManager.getAppUsageEventList()).isEmpty();
    }

    @Test
    public void start_loadExpectedAppUsageData() throws RemoteException {
        // Fake current usage data.
        final UsageEvents.Event event1 =
                getUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, /*timestamp=*/ 1);
        final UsageEvents.Event event2 =
                getUsageEvent(UsageEvents.Event.ACTIVITY_STOPPED, /*timestamp=*/ 2);
        final List<UsageEvents.Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);
        doReturn(getUsageEvents(events))
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), any());
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());

        // Fake database usage data.
        final MatrixCursor cursor = new MatrixCursor(
                new String[]{
                        AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE,
                        AppUsageEventEntity.KEY_TIMESTAMP});
        // Adds fake data into the cursor.
        cursor.addRow(new Object[] {
                AppUsageEventType.ACTIVITY_RESUMED.getNumber(), /*timestamp=*/ 3});
        cursor.addRow(new Object[] {
                AppUsageEventType.ACTIVITY_RESUMED.getNumber(), /*timestamp=*/ 4});
        cursor.addRow(new Object[] {
                AppUsageEventType.ACTIVITY_STOPPED.getNumber(), /*timestamp=*/ 5});
        cursor.addRow(new Object[] {
                AppUsageEventType.ACTIVITY_STOPPED.getNumber(), /*timestamp=*/ 6});
        DatabaseUtils.sFakeAppUsageEventSupplier = () -> cursor;

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getIsCurrentAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsDatabaseAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsCurrentBatteryHistoryLoaded()).isTrue();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isTrue();
        final List<AppUsageEvent> appUsageEventList = mDataProcessManager.getAppUsageEventList();
        assertThat(appUsageEventList.size()).isEqualTo(6);
        assertAppUsageEvent(
                appUsageEventList.get(0), AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1);
        assertAppUsageEvent(
                appUsageEventList.get(1), AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 2);
        assertAppUsageEvent(
                appUsageEventList.get(2), AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 3);
        assertAppUsageEvent(
                appUsageEventList.get(3), AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 4);
        assertAppUsageEvent(
                appUsageEventList.get(4), AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 5);
        assertAppUsageEvent(
                appUsageEventList.get(5), AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 6);
    }

    @Test
    public void start_currentUserLocked_emptyAppUsageList() throws RemoteException {
        final UsageEvents.Event event =
                getUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, /*timestamp=*/ 1);
        final List<UsageEvents.Event> events = new ArrayList<>();
        events.add(event);
        doReturn(getUsageEvents(events))
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), any());
        doReturn(false).when(mUserManager).isUserUnlocked(anyInt());
        final MatrixCursor cursor = new MatrixCursor(
                new String[]{
                        AppUsageEventEntity.KEY_UID,
                        AppUsageEventEntity.KEY_PACKAGE_NAME,
                        AppUsageEventEntity.KEY_TIMESTAMP});
        // Adds fake data into the cursor.
        cursor.addRow(new Object[] {101L, "app name1", 1001L});
        DatabaseUtils.sFakeAppUsageEventSupplier = () -> cursor;

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getAppUsageEventList()).isEmpty();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isFalse();
    }

    @Test
    public void getStartTimestampOfBatteryLevelData_returnExpectedResult() {
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        final List<Long> timestamps = new ArrayList<>();
        timestamps.add(101L);
        timestamps.add(1001L);
        final List<Integer> levels = new ArrayList<>();
        levels.add(1);
        levels.add(2);
        hourlyBatteryLevelsPerDay.add(null);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));

        final DataProcessManager dataProcessManager = new DataProcessManager(
                mContext, /*handler=*/ null,  /*callbackFunction=*/ null,
                hourlyBatteryLevelsPerDay, /*batteryHistoryMap=*/ null);

        assertThat(dataProcessManager.getStartTimestampOfBatteryLevelData()).isEqualTo(101);
    }

    @Test
    public void getStartTimestampOfBatteryLevelData_emptyLevels_returnZero() {
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        hourlyBatteryLevelsPerDay.add(null);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(new ArrayList<>(), new ArrayList<>()));

        final DataProcessManager dataProcessManager = new DataProcessManager(
                mContext, /*handler=*/ null,  /*callbackFunction=*/ null,
                hourlyBatteryLevelsPerDay, /*batteryHistoryMap=*/ null);

        assertThat(dataProcessManager.getStartTimestampOfBatteryLevelData()).isEqualTo(0);
    }

    private UsageEvents getUsageEvents(final List<UsageEvents.Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {"package"});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }

    private UsageEvents.Event getUsageEvent(
            final int eventType, final long timestamp) {
        final UsageEvents.Event event = new UsageEvents.Event();
        event.mEventType = eventType;
        event.mPackage = "package";
        event.mTimeStamp = timestamp;
        return event;
    }

    private void assertAppUsageEvent(
            final AppUsageEvent event, final AppUsageEventType eventType, final long timestamp) {
        assertThat(event.getType()).isEqualTo(eventType);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }
}
