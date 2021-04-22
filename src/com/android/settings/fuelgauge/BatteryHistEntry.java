/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.time.Duration;
import java.util.TimeZone;

/** A container class to carry data from {@link ContentValues}. */
public class BatteryHistEntry {
    private static final String TAG = "BatteryHistEntry";

    /** Keys for accessing {@link ContentValues} or {@link Cursor}. */
    public static final String KEY_UID = "uid";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_APP_LABEL = "appLabel";
    public static final String KEY_PACKAGE_NAME = "packageName";
    public static final String KEY_IS_HIDDEN = "isHidden";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_ZONE_ID = "zoneId";
    public static final String KEY_TOTAL_POWER = "totalPower";
    public static final String KEY_CONSUME_POWER = "consumePower";
    public static final String KEY_PERCENT_OF_TOTAL = "percentOfTotal";
    public static final String KEY_FOREGROUND_USAGE_TIME = "foregroundUsageTimeInMs";
    public static final String KEY_BACKGROUND_USAGE_TIME = "backgroundUsageTimeInMs";
    public static final String KEY_DRAIN_TYPE = "drainType";
    public static final String KEY_CONSUMER_TYPE = "consumerType";
    public static final String KEY_BATTERY_LEVEL = "batteryLevel";
    public static final String KEY_BATTERY_STATUS = "batteryStatus";
    public static final String KEY_BATTERY_HEALTH = "batteryHealth";

    public final long mUid;
    public final long mUserId;
    public final String mAppLabel;
    public final String mPackageName;
    // Whether the data is represented as system component or not?
    public final boolean mIsHidden;
    // Records the timestamp relative information.
    public final long mTimestamp;
    public final String mZoneId;
    // Records the battery usage relative information.
    public final double mTotalPower;
    public final double mConsumePower;
    public final double mPercentOfTotal;
    public final long mForegroundUsageTimeInMs;
    public final long mBackgroundUsageTimeInMs;
    public final int mDrainType;
    public final int mConsumerType;
    // Records the battery intent relative information.
    public final int mBatteryLevel;
    public final int mBatteryStatus;
    public final int mBatteryHealth;

    private String mKey = null;
    private boolean mIsValidEntry = true;

    public BatteryHistEntry(ContentValues values) {
        mUid = getLong(values, KEY_UID);
        mUserId = getLong(values, KEY_USER_ID);
        mAppLabel = getString(values, KEY_APP_LABEL);
        mPackageName = getString(values, KEY_PACKAGE_NAME);
        mIsHidden = getBoolean(values, KEY_IS_HIDDEN);
        mTimestamp = getLong(values, KEY_TIMESTAMP);
        mZoneId = getString(values, KEY_ZONE_ID);
        mTotalPower = getDouble(values, KEY_TOTAL_POWER);
        mConsumePower = getDouble(values, KEY_CONSUME_POWER);
        mPercentOfTotal = getDouble(values, KEY_PERCENT_OF_TOTAL);
        mForegroundUsageTimeInMs = getLong(values, KEY_FOREGROUND_USAGE_TIME);
        mBackgroundUsageTimeInMs = getLong(values, KEY_BACKGROUND_USAGE_TIME);
        mDrainType = getInteger(values, KEY_DRAIN_TYPE);
        mConsumerType = getInteger(values, KEY_CONSUMER_TYPE);
        mBatteryLevel = getInteger(values, KEY_BATTERY_LEVEL);
        mBatteryStatus = getInteger(values, KEY_BATTERY_STATUS);
        mBatteryHealth = getInteger(values, KEY_BATTERY_HEALTH);
    }

    public BatteryHistEntry(Cursor cursor) {
        mUid = getLong(cursor, KEY_UID);
        mUserId = getLong(cursor, KEY_USER_ID);
        mAppLabel = getString(cursor, KEY_APP_LABEL);
        mPackageName = getString(cursor, KEY_PACKAGE_NAME);
        mIsHidden = getBoolean(cursor, KEY_IS_HIDDEN);
        mTimestamp = getLong(cursor, KEY_TIMESTAMP);
        mZoneId = getString(cursor, KEY_ZONE_ID);
        mTotalPower = getDouble(cursor, KEY_TOTAL_POWER);
        mConsumePower = getDouble(cursor, KEY_CONSUME_POWER);
        mPercentOfTotal = getDouble(cursor, KEY_PERCENT_OF_TOTAL);
        mForegroundUsageTimeInMs = getLong(cursor, KEY_FOREGROUND_USAGE_TIME);
        mBackgroundUsageTimeInMs = getLong(cursor, KEY_BACKGROUND_USAGE_TIME);
        mDrainType = getInteger(cursor, KEY_DRAIN_TYPE);
        mConsumerType = getInteger(cursor, KEY_CONSUMER_TYPE);
        mBatteryLevel = getInteger(cursor, KEY_BATTERY_LEVEL);
        mBatteryStatus = getInteger(cursor, KEY_BATTERY_STATUS);
        mBatteryHealth = getInteger(cursor, KEY_BATTERY_HEALTH);
    }

    /** Whether this {@link BatteryHistEntry} is valid or not? */
    public boolean isValidEntry() {
        return mIsValidEntry;
    }

    /** Whether this {@link BatteryHistEntry} is user consumer or not. */
    public boolean isUserEntry() {
        return mConsumerType == ConvertUtils.CONSUMER_TYPE_USER_BATTERY;
    }

    /** Whether this {@link BatteryHistEntry} is app consumer or not. */
    public boolean isAppEntry() {
        return mConsumerType == ConvertUtils.CONSUMER_TYPE_UID_BATTERY;
    }

    /** Whether this {@link BatteryHistEntry} is system consumer or not. */
    public boolean isSystemEntry() {
        return mConsumerType == ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY;
    }

    /** Gets an identifier to represent this {@link BatteryHistEntry}. */
    public String getKey() {
        if (mKey == null) {
            switch (mConsumerType) {
                case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                    mKey = Long.toString(mUid);
                    break;
                case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                    mKey = "S|" + mDrainType;
                    break;
                case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
                    mKey = "U|" + mUserId;
                    break;
            }
        }
        return mKey;
    }

    @Override
    public String toString() {
        final String recordAtDateTime = ConvertUtils.utcToLocalTime(mTimestamp);
        final StringBuilder builder = new StringBuilder()
            .append("\nBatteryHistEntry{")
            .append(String.format("\n\tpackage=%s|label=%s|uid=%d|userId=%d|isHidden=%b",
                  mPackageName, mAppLabel, mUid, mUserId, mIsHidden))
            .append(String.format("\n\ttimestamp=%s|zoneId=%s", recordAtDateTime, mZoneId))
            .append(String.format("\n\tusage=%f|total=%f|consume=%f|elapsedTime=%d|%d",
                  mPercentOfTotal, mTotalPower, mConsumePower,
                  Duration.ofMillis(mForegroundUsageTimeInMs).getSeconds(),
                  Duration.ofMillis(mBackgroundUsageTimeInMs).getSeconds()))
            .append(String.format("\n\tdrain=%d|consumer=%d", mDrainType, mConsumerType))
            .append(String.format("\n\tbattery=%d|status=%d|health=%d\n}",
                  mBatteryLevel, mBatteryStatus, mBatteryHealth));
        return builder.toString();
    }

    private int getInteger(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsInteger(key);
        };
        mIsValidEntry = false;
        return 0;
    }

    private int getInteger(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getInt(columnIndex);
        }
        mIsValidEntry = false;
        return 0;
    }

    private long getLong(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsLong(key);
        }
        mIsValidEntry = false;
        return 0L;
    }

    private long getLong(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getLong(columnIndex);
        }
        mIsValidEntry = false;
        return 0L;
    }

    private double getDouble(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsDouble(key);
        }
        mIsValidEntry = false;
        return 0f;
    }

    private double getDouble(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getDouble(columnIndex);
        }
        mIsValidEntry = false;
        return 0f;
    }

    private String getString(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsString(key);
        }
        mIsValidEntry = false;
        return null;
    }

    private String getString(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getString(columnIndex);
        }
        mIsValidEntry = false;
        return null;
    }

    private boolean getBoolean(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsBoolean(key);
        }
        mIsValidEntry = false;
        return false;
    }

    private boolean getBoolean(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            // Use value == 1 to represent boolean value in the database.
            return cursor.getInt(columnIndex) == 1;
        }
        mIsValidEntry = false;
        return false;
    }

}
