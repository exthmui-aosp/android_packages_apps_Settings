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

package com.android.settings.deviceinfo.batteryinfo;

/**
 * Feature Provider used for getting battery information.
 */
public interface BatteryInfoFeatureProvider {

    /** Returns true if Manufacture date should be shown */
    boolean isManufactureDateAvailable();

    /** Returns true if First use date should be shown */
    boolean isFirstUseDateAvailable();

    /** Returns the summary of battery manufacture date */
    CharSequence getManufactureDateSummary();

    /** Returns the summary of battery first use date */
    CharSequence getFirstUseDateSummary();
}
