/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode >  Apps
 */
public class ZenModeAppsFragment extends ZenModeFragmentBase {

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeAppsPreferenceController(
                context, ZenModeAppsPreferenceController.KEY_PRIORITY, mBackend));
        controllers.add(new ZenModeAppsPreferenceController(
                context, ZenModeAppsPreferenceController.KEY_NONE, mBackend));
        // TODO: b/308819928 - The manual DND mode cannot have the ALL type;
        // unify the controllers into one and only create a preference if isManualDnd is false.
        controllers.add(new ZenModeAppsPreferenceController(
                context, ZenModeAppsPreferenceController.KEY_ALL, mBackend));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_apps_settings;
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - make this the correct metrics category
        return SettingsEnums.NOTIFICATION_ZEN_MODE_PRIORITY;
    }
}
