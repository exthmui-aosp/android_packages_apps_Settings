/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.util.FeatureFlagUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppLocaleUtil;

/**
 * A controller to update current locale information of application.
 */
public class AppLocalePreferenceController extends AppInfoPreferenceControllerBase {
    private static final String TAG = AppLocalePreferenceController.class.getSimpleName();

    public AppLocalePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isFeatureOn = FeatureFlagUtils
                .isEnabled(mContext, FeatureFlagUtils.SETTINGS_APP_LANGUAGE_SELECTION);
        return isFeatureOn && canDisplayLocaleUi() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppLocaleDetails.class;
    }

    @Override
    public CharSequence getSummary() {
        return AppLocaleDetails.getSummary(mContext, mParent.getAppEntry().info.packageName);
    }

    boolean canDisplayLocaleUi() {
        return AppLocaleUtil.canDisplayLocaleUi(mContext, mParent.getAppEntry());
    }
}
