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

import android.content.Context;
import android.os.AsyncTask;
import android.os.BatteryUsageStats;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.List;

/** Load battery usage data in the background. */
public final class BatteryUsageDataLoader {
    private static final String TAG = "BatteryUsageDataLoader";

    @VisibleForTesting
    static BatteryAppListPreferenceController sController;

    private BatteryUsageDataLoader() {
    }

    static void enqueueWork(Context context) {
        AsyncTask.execute(() -> {
            Log.d(TAG, "loadUsageDataSafely() in the AsyncTask");
            loadUsageDataSafely(context.getApplicationContext());
        });
    }

    @VisibleForTesting
    static void loadUsageData(Context context) {
        final long start = System.currentTimeMillis();
        final BatteryUsageStats batteryUsageStats = DataProcessor.getBatteryUsageStats(context);
        final List<BatteryEntry> batteryEntryList =
                DataProcessor.generateBatteryEntryListFromBatteryUsageStats(
                        context,
                        batteryUsageStats,
                        sController);
        if (batteryEntryList == null || batteryEntryList.isEmpty()) {
            Log.w(TAG, "getBatteryEntryList() returns null or empty content");
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(TAG, String.format("getBatteryUsageStats() in %d/ms", elapsedTime));

        // Uploads the BatteryEntry data into SettingsIntelligence.
        DatabaseUtils.sendBatteryEntryData(
                context, batteryEntryList, batteryUsageStats);
        DataProcessor.closeBatteryUsageStats(batteryUsageStats);
    }

    private static void loadUsageDataSafely(Context context) {
        try {
            loadUsageData(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "loadUsageData:" + e);
        }
    }
}
