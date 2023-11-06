/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.db;

import android.content.ContentValues;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Locale;

/** A {@link Entity} class to save battery usage slot into database. */
@Entity
public class BatteryUsageSlotEntity {
    /** Keys for accessing {@link ContentValues}. */
    public static final String KEY_TIMESTAMP = "timestamp";

    public static final String KEY_BATTERY_USAGE_SLOT = "batteryUsageSlot";

    @PrimaryKey(autoGenerate = true)
    private long mId;

    public final long timestamp;
    public final String batteryUsageSlot;

    public BatteryUsageSlotEntity(final long timestamp, final String batteryUsageSlot) {
        this.timestamp = timestamp;
        this.batteryUsageSlot = batteryUsageSlot;
    }

    /** Sets the auto-generated content ID. */
    public void setId(long id) {
        this.mId = id;
    }

    /** Gets the auto-generated content ID. */
    public long getId() {
        return mId;
    }

    @Override
    public String toString() {
        final String recordAtDateTime = ConvertUtils.utcToLocalTimeForLogging(timestamp);
        final StringBuilder builder =
                new StringBuilder()
                        .append("\nBatteryUsageSlot{")
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\ttimestamp=%s|batteryUsageSlot=%s",
                                        recordAtDateTime,
                                        batteryUsageSlot))
                        .append("\n}");
        return builder.toString();
    }

    /** Creates new {@link BatteryUsageSlotEntity} from {@link ContentValues}. */
    public static BatteryUsageSlotEntity create(ContentValues contentValues) {
        Builder builder = BatteryUsageSlotEntity.newBuilder();
        if (contentValues.containsKey(KEY_TIMESTAMP)) {
            builder.setTimestamp(contentValues.getAsLong(KEY_TIMESTAMP));
        }
        if (contentValues.containsKey(KEY_BATTERY_USAGE_SLOT)) {
            builder.setBatteryUsageSlot(contentValues.getAsString(KEY_BATTERY_USAGE_SLOT));
        }
        return builder.build();
    }

    /** Creates a new {@link Builder} instance. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** A convenience builder class to improve readability. */
    public static class Builder {
        private long mTimestamp;
        private String mBatteryUsageSlot;

        /** Sets the timestamp. */
        @CanIgnoreReturnValue
        public Builder setTimestamp(final long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        /** Sets the battery usage slot. */
        @CanIgnoreReturnValue
        public Builder setBatteryUsageSlot(final String batteryUsageSlot) {
            mBatteryUsageSlot = batteryUsageSlot;
            return this;
        }

        /** Builds the {@link BatteryUsageSlotEntity}. */
        public BatteryUsageSlotEntity build() {
            return new BatteryUsageSlotEntity(mTimestamp, mBatteryUsageSlot);
        }

        private Builder() {}
    }
}
