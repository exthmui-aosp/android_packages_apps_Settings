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

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/**
 * A controller that manages the information about battery first use date.
 */
public class BatteryFirstUseDatePreferenceController extends BasePreferenceController {

    private BatteryInfoFeatureProvider mBatteryInfoFeatureProvider;

    public BatteryFirstUseDatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatteryInfoFeatureProvider = FeatureFactory.getFactory(
                context).getBatteryInfoFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatteryInfoFeatureProvider.isFirstUseDateAvailable()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mBatteryInfoFeatureProvider.getFirstUseDateSummary();
    }
}
