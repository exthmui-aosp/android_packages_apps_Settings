/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Intent;
import android.os.Bundle;
import android.util.FeatureFlagUtils;

import com.android.settings.SettingsActivity;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.spa.SpaActivity;
import com.android.settings.spa.app.appsettings.AppSettingsProvider;

public class InstalledAppDetailsTop extends SettingsActivity {

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (!FeatureFlagUtils.isEnabled(this, FeatureFlagUtils.SETTINGS_ENABLE_SPA)) {
            return;
        }
        String packageName = super.getIntent().getData().getSchemeSpecificPart();
        SpaActivity.startSpaActivity(
                this, AppSettingsProvider.INSTANCE.getRoute(packageName, getUserId()));
        finish();
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, AppInfoDashboardFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AppInfoDashboardFragment.class.getName().equals(fragmentName);
    }
}
