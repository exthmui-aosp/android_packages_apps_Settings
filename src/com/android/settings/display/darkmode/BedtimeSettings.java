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

package com.android.settings.display.darkmode;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/** Manages Digital Wellbeing bedtime settings' intents. */
public final class BedtimeSettings {
    @VisibleForTesting
    public static final String ACTION_BEDTIME_SETTINGS = "android.settings.BEDTIME_SETTINGS";

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final String mWellbeingPackage;

    public BedtimeSettings(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mWellbeingPackage = mContext.getResources().getString(
                com.android.internal.R.string.config_defaultWellbeingPackage);
    }

    /**
     * Returns the bedtime settings intent. If the bedtime settings isn't available, returns
     * {@code null}.
     */
    @Nullable
    public Intent getBedtimeSettingsIntent() {
        Intent bedtimeSettingsIntent = new Intent(ACTION_BEDTIME_SETTINGS).setPackage(
                mWellbeingPackage);
        ResolveInfo bedtimeSettingInfo = mPackageManager.resolveActivity(bedtimeSettingsIntent,
                PackageManager.MATCH_DEFAULT_ONLY);

        if (bedtimeSettingInfo != null && bedtimeSettingInfo.activityInfo.isEnabled()) {
            return bedtimeSettingsIntent;
        } else {
            return null;
        }
    }
}
