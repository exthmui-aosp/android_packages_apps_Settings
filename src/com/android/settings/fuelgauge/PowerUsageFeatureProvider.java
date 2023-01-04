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

import android.content.Context;
import android.content.Intent;
import android.util.SparseIntArray;

import com.android.settingslib.fuelgauge.Estimate;

import java.util.List;
import java.util.Set;

/**
 * Feature Provider used in power usage
 */
public interface PowerUsageFeatureProvider {

    /**
     * Check whether the battery usage button is enabled in the battery page
     */
    boolean isBatteryUsageEnabled(Context context);

    /**
     * Returns an allowlist of app names combined into the system-apps item
     */
    List<String> getSystemAppsAllowlist(Context context);

    /**
     * Check whether location setting is enabled
     */
    boolean isLocationSettingEnabled(String[] packages);

    /**
     * Gets an {@link Intent} to show additional battery info.
     */
    Intent getAdditionalBatteryInfoIntent();

    /**
     * Check whether it is type service
     */
    boolean isTypeService(int uid);

    /**
     * Check whether it is type system
     */
    boolean isTypeSystem(int uid, String[] packages);

    /**
     * Returns an improved prediction for battery time remaining.
     */
    Estimate getEnhancedBatteryPrediction(Context context);

    /**
     * Returns an improved projection curve for future battery level.
     *
     * @param zeroTime timestamps (array keys) are shifted by this amount
     */
    SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long zeroTime);

    /**
     * Checks whether the toggle for enhanced battery predictions is enabled.
     */
    boolean isEnhancedBatteryPredictionEnabled(Context context);

    /**
     * Checks whether debugging should be enabled for battery estimates.
     */
    boolean isEstimateDebugEnabled();

    /**
     * Converts the provided string containing the remaining time into a debug string for enhanced
     * estimates.
     *
     * @return A string containing the estimate and a label indicating it is an enhanced estimate
     */
    String getEnhancedEstimateDebugString(String timeRemaining);

    /**
     * Converts the provided string containing the remaining time into a debug string.
     *
     * @return A string containing the estimate and a label indicating it is a normal estimate
     */
    String getOldEstimateDebugString(String timeRemaining);

    /**
     * Checks whether smart battery feature is supported in this device
     */
    boolean isSmartBatterySupported();

    /**
     * Checks whether we should show usage information by slots or not.
     */
    boolean isChartGraphSlotsEnabled(Context context);

    /**
     * Checks whether adaptive charging feature is supported in this device
     */
    boolean isAdaptiveChargingSupported();

    /**
     * Checks whether battery manager feature is supported in this device
     */
    boolean isBatteryManagerSupported();

    /**
     * Returns {@code true} if current defender mode is extra defend
     */
    boolean isExtraDefend();

    /**
     * Returns {@code true} if delay the hourly job when device is booting.
     */
    boolean delayHourlyJobWhenBooting();

    /**
     * Gets a intent for one time bypass charge limited to resume charging.
     */
    Intent getResumeChargeIntent(boolean isDockDefender);

    /**
     * Returns {@link Set} for hiding system component ids in the usage screen.
     */
    Set<Integer> getHideSystemComponentSet(Context context);

    /**
     * Returns {@link Set} for hiding application package names in the usage screen.
     */
    Set<CharSequence> getHideApplicationSet(Context context);

    /**
     * Returns {@link Set} for hiding applications background usage time.
     */
    Set<CharSequence> getHideBackgroundUsageTimeSet(Context context);

    /**
     * Returns {@link Set} for ignoring task root class names for screen on time.
     */
    Set<CharSequence> getIgnoreScreenOnTimeTaskRootSet(Context context);
}
