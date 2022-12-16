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

import static org.junit.Assert.assertThrows;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Tests for {@link BatteryUsageContentProvider}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageContentProviderTest {
    private static final Uri VALID_BATTERY_STATE_CONTENT_URI = DatabaseUtils.BATTERY_CONTENT_URI;
    private static final String PACKAGE_NAME1 = "com.android.settings1";
    private static final String PACKAGE_NAME2 = "com.android.settings2";
    private static final String PACKAGE_NAME3 = "com.android.settings3";
    private static final long USER_ID1 = 1;
    private static final long USER_ID2 = 2;

    private Context mContext;
    private BatteryUsageContentProvider mProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mProvider = new BatteryUsageContentProvider();
        mProvider.attachInfo(mContext, /*info=*/ null);
        BatteryTestUtils.setUpBatteryStateDatabase(mContext);
    }

    @Test
    public void onCreate_withoutWorkProfileMode_returnsTrue() {
        assertThat(mProvider.onCreate()).isTrue();
    }

    @Test
    public void onCreate_withWorkProfileMode_returnsFalse() {
        BatteryTestUtils.setWorkProfile(mContext);
        assertThat(mProvider.onCreate()).isFalse();
    }

    @Test
    public void queryAndInsert_incorrectContentUri_throwsIllegalArgumentException() {
        final Uri.Builder builder =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE + "/0");
        final Uri uri = builder.build();
        mProvider.onCreate();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mProvider.query(
                                uri, /*strings=*/ null, /*s=*/ null, /*strings1=*/ null,
                                /*s1=*/ null));
        assertThrows(
                IllegalArgumentException.class,
                () -> mProvider.insert(uri, /*contentValues=*/ null));
    }

    @Test
    public void queryAndInsert_incorrectAuthority_throwsIllegalArgumentException() {
        final Uri.Builder builder =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY + ".debug")
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE);
        final Uri uri = builder.build();
        mProvider.onCreate();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mProvider.query(
                                uri, /*strings=*/ null, /*s=*/ null, /*strings1=*/ null,
                                /*s1=*/ null));
        assertThrows(
                IllegalArgumentException.class,
                () -> mProvider.insert(uri, /*contentValues=*/ null));
    }

    @Test
    public void query_batteryState_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff = currentTime.toMillis() - 3;

        final Cursor cursor = insertBatteryState(currentTime, Long.toString(expiredTimeCutoff));

        // Verifies the result not include expired data.
        assertThat(cursor.getCount()).isEqualTo(3);
        final int packageNameIndex = cursor.getColumnIndex("packageName");
        // Verifies the first data package name.
        cursor.moveToFirst();
        final String actualPackageName1 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName1).isEqualTo(PACKAGE_NAME1);
        // Verifies the second data package name.
        cursor.moveToNext();
        final String actualPackageName2 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName2).isEqualTo(PACKAGE_NAME2);
        // Verifies the third data package name.
        cursor.moveToNext();
        final String actualPackageName3 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName3).isEqualTo(PACKAGE_NAME3);
        cursor.close();
        // Verifies the broadcast intent.
        TimeUnit.SECONDS.sleep(1);
        final List<Intent> intents = Shadows.shadowOf((Application) mContext).getBroadcastIntents();
        assertThat(intents).hasSize(1);
        assertThat(intents.get(0).getAction()).isEqualTo(
                BootBroadcastReceiver.ACTION_PERIODIC_JOB_RECHECK);
    }

    @Test
    public void query_batteryStateTimestamp_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff = currentTime.toMillis() - 1;

        final Cursor cursor = insertBatteryState(currentTime, Long.toString(expiredTimeCutoff));

        // Verifies the result not include expired data.
        assertThat(cursor.getCount()).isEqualTo(2);
        final int packageNameIndex = cursor.getColumnIndex("packageName");
        // Verifies the first data package name.
        cursor.moveToFirst();
        final String actualPackageName1 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName1).isEqualTo(PACKAGE_NAME2);
        // Verifies the third data package name.
        cursor.moveToNext();
        final String actualPackageName2 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName2).isEqualTo(PACKAGE_NAME3);
        cursor.close();
        // Verifies the broadcast intent.
        TimeUnit.SECONDS.sleep(1);
        final List<Intent> intents = Shadows.shadowOf((Application) mContext).getBroadcastIntents();
        assertThat(intents).hasSize(1);
        assertThat(intents.get(0).getAction()).isEqualTo(
                BootBroadcastReceiver.ACTION_PERIODIC_JOB_RECHECK);
    }

    @Test
    public void query_appUsageTimestamp_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final long timestamp1 = System.currentTimeMillis();
        final long timestamp2 = timestamp1 + 2;
        final long timestamp3 = timestamp1 + 4;
        // Inserts some valid testing data.
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID1, timestamp1, PACKAGE_NAME1);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID2, timestamp2, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID1, timestamp3, PACKAGE_NAME3);

        final Cursor cursor1 = getCursorOfLatestTimestamp(USER_ID1);
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(0)).isEqualTo(timestamp3);

        final Cursor cursor2 = getCursorOfLatestTimestamp(USER_ID2);
        assertThat(cursor2.getCount()).isEqualTo(1);
        cursor2.moveToFirst();
        assertThat(cursor2.getLong(0)).isEqualTo(timestamp2);

        final long notExistingUserId = 3;
        final Cursor cursor3 = getCursorOfLatestTimestamp(notExistingUserId);
        assertThat(cursor3.getCount()).isEqualTo(1);
        cursor3.moveToFirst();
        assertThat(cursor3.getLong(0)).isEqualTo(0);
    }

    @Test
    public void insert_batteryState_returnsExpectedResult() {
        mProvider.onCreate();
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(51)
                        .setBatteryStatus(2)
                        .setBatteryHealth(3)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setAppLabel("Settings")
                        .setIsHidden(true)
                        .setBootTimestamp(101L)
                        .setTotalPower(99)
                        .setConsumePower(9)
                        .setForegroundUsageConsumePower(1)
                        .setForegroundServiceUsageConsumePower(2)
                        .setBackgroundUsageConsumePower(3)
                        .setCachedUsageConsumePower(3)
                        .setPercentOfTotal(0.9)
                        .setForegroundUsageTimeInMs(1000)
                        .setBackgroundUsageTimeInMs(2000)
                        .setDrainType(1)
                        .build();
        final String expectedBatteryInformationString =
                ConvertUtils.convertBatteryInformationToString(batteryInformation);
        ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_UID, Long.valueOf(101L));
        values.put(BatteryHistEntry.KEY_USER_ID, Long.valueOf(1001L));
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, new String("com.android.settings"));
        values.put(BatteryHistEntry.KEY_TIMESTAMP, Long.valueOf(2100021L));
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, Integer.valueOf(2));
        values.put(BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START, true);
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION, expectedBatteryInformationString);

        final Uri uri = mProvider.insert(VALID_BATTERY_STATE_CONTENT_URI, values);

        assertThat(uri).isEqualTo(VALID_BATTERY_STATE_CONTENT_URI);
        // Verifies the BatteryState content.
        final List<BatteryState> states =
                BatteryStateDatabase.getInstance(mContext).batteryStateDao().getAllAfter(0);
        assertThat(states).hasSize(1);
        assertThat(states.get(0).uid).isEqualTo(101L);
        assertThat(states.get(0).userId).isEqualTo(1001L);
        assertThat(states.get(0).packageName).isEqualTo("com.android.settings");
        assertThat(states.get(0).timestamp).isEqualTo(2100021L);
        assertThat(states.get(0).consumerType).isEqualTo(2);
        assertThat(states.get(0).isFullChargeCycleStart).isTrue();
        assertThat(states.get(0).batteryInformation).isEqualTo(expectedBatteryInformationString);
    }

    @Test
    public void insert_partialFieldsContentValues_returnsExpectedResult() {
        mProvider.onCreate();
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(52)
                        .setBatteryStatus(3)
                        .setBatteryHealth(2)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .build();
        final String expectedBatteryInformationString =
                ConvertUtils.convertBatteryInformationToString(batteryInformation);
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, new String("fake_data"));
        values.put(BatteryHistEntry.KEY_TIMESTAMP, Long.valueOf(2100022L));
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION, expectedBatteryInformationString);

        final Uri uri = mProvider.insert(VALID_BATTERY_STATE_CONTENT_URI, values);

        assertThat(uri).isEqualTo(VALID_BATTERY_STATE_CONTENT_URI);
        // Verifies the BatteryState content.
        final List<BatteryState> states =
                BatteryStateDatabase.getInstance(mContext).batteryStateDao().getAllAfter(0);
        assertThat(states).hasSize(1);
        assertThat(states.get(0).packageName).isEqualTo("fake_data");
        assertThat(states.get(0).timestamp).isEqualTo(2100022L);
        assertThat(states.get(0).batteryInformation).isEqualTo(expectedBatteryInformationString);
    }

    @Test
    public void insert_appUsageEvent_returnsExpectedResult() {
        mProvider.onCreate();
        ContentValues values = new ContentValues();
        values.put(AppUsageEventEntity.KEY_UID, 101L);
        values.put(AppUsageEventEntity.KEY_USER_ID, 1001L);
        values.put(AppUsageEventEntity.KEY_TIMESTAMP, 10001L);
        values.put(AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE, 1);
        values.put(AppUsageEventEntity.KEY_PACKAGE_NAME, "com.android.settings1");
        values.put(AppUsageEventEntity.KEY_INSTANCE_ID, 100001L);
        values.put(AppUsageEventEntity.KEY_TASK_ROOT_PACKAGE_NAME, "com.android.settings2");

        final Uri uri = mProvider.insert(DatabaseUtils.APP_USAGE_EVENT_URI, values);

        assertThat(uri).isEqualTo(DatabaseUtils.APP_USAGE_EVENT_URI);
        // Verifies the AppUsageEventEntity content.
        final List<AppUsageEventEntity> entities =
                BatteryStateDatabase.getInstance(mContext).appUsageEventDao().getAllAfter(0);
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).uid).isEqualTo(101L);
        assertThat(entities.get(0).userId).isEqualTo(1001L);
        assertThat(entities.get(0).timestamp).isEqualTo(10001L);
        assertThat(entities.get(0).appUsageEventType).isEqualTo(1);
        assertThat(entities.get(0).packageName).isEqualTo("com.android.settings1");
        assertThat(entities.get(0).instanceId).isEqualTo(100001L);
        assertThat(entities.get(0).taskRootPackageName).isEqualTo("com.android.settings2");
    }

    @Test
    public void delete_throwsUnsupportedOperationException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> mProvider.delete(/*uri=*/ null, /*s=*/ null, /*strings=*/ null));
    }

    @Test
    public void update_throwsUnsupportedOperationException() {
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        mProvider.update(
                                /*uri=*/ null, /*contentValues=*/ null, /*s=*/ null,
                                /*strings=*/ null));
    }

    private Cursor insertBatteryState(
            Duration currentTime,
            String queryTimestamp)
            throws Exception {
        mProvider.onCreate();
        final FakeClock fakeClock = new FakeClock();
        fakeClock.setCurrentTime(currentTime);
        mProvider.setClock(fakeClock);
        final long currentTimestamp = currentTime.toMillis();
        // Inserts some valid testing data.
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, currentTimestamp - 2, PACKAGE_NAME1,
                /*isFullChargeStart=*/ true);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, currentTimestamp - 1, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, currentTimestamp, PACKAGE_NAME3);

        final Uri batteryStateQueryContentUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE)
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_KEY_TIMESTAMP, queryTimestamp)
                        .build();

        final Cursor cursor =
                mProvider.query(
                        batteryStateQueryContentUri,
                        /*strings=*/ null,
                        /*s=*/ null,
                        /*strings1=*/ null,
                        /*s1=*/ null);

        return cursor;
    }

    private Cursor getCursorOfLatestTimestamp(final long userId) {
        final Uri appUsageLatestTimestampQueryContentUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.APP_USAGE_LATEST_TIMESTAMP_PATH)
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_KEY_USERID, Long.toString(userId))
                        .build();

        return mProvider.query(
                        appUsageLatestTimestampQueryContentUri,
                        /*strings=*/ null,
                        /*s=*/ null,
                        /*strings1=*/ null,
                        /*s1=*/ null);
    }
}
