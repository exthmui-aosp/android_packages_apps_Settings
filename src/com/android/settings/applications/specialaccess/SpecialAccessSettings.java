/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications.specialaccess;

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECTED_WORK_AND_PERSONAL_APPS_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGE_DEVICE_ADMIN_APPS;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class SpecialAccessSettings extends DashboardFragment {

    private static final String TAG = "SpecialAccessSettings";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterpriseStringTitle("interact_across_profiles",
                CONNECTED_WORK_AND_PERSONAL_APPS_TITLE, R.string.interact_across_profiles_title);
        replaceEnterpriseStringTitle("device_administrators",
                MANAGE_DEVICE_ADMIN_APPS, R.string.manage_device_admin);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        use(DataSaverController.class).init(getViewLifecycleOwner());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.special_access;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SPECIAL_ACCESS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.special_access);
}
