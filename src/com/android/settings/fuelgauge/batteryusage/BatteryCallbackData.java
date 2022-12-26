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

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;

/** Wraps the battery usage data and device screen-on time data used for battery usage page. */
public class BatteryCallbackData {

    // The usage app data used for rendering app list.
    private final Map<Integer, Map<Integer, BatteryDiffData>> mBatteryUsageMap;
    // The device screen-on time data.
    private final Map<Integer, Map<Integer, Long>> mDeviceScreenOnTime;

    public BatteryCallbackData(
            @Nullable Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageMap,
            @Nullable Map<Integer, Map<Integer, Long>> deviceScreenOnTime) {
        mBatteryUsageMap = batteryUsageMap;
        mDeviceScreenOnTime = deviceScreenOnTime;
    }

    public Map<Integer, Map<Integer, BatteryDiffData>> getBatteryUsageMap() {
        return mBatteryUsageMap;
    }

    public Map<Integer, Map<Integer, Long>> getDeviceScreenOnTime() {
        return mDeviceScreenOnTime;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "batteryUsageMap: %s; deviceScreenOnTime: %s",
                mBatteryUsageMap,
                mDeviceScreenOnTime);
    }
}
