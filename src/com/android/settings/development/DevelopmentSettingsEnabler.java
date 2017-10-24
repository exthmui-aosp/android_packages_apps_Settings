/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class DevelopmentSettingsEnabler implements LifecycleObserver, OnResume {

    private final Context mContext;
    private final SharedPreferences mDevelopmentPreferences;
    private boolean mLastEnabledState;

    public DevelopmentSettingsEnabler(Context context, Lifecycle lifecycle) {
        mContext = context;
        mDevelopmentPreferences = context.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onResume() {
        mLastEnabledState = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    public static boolean enableDevelopmentSettings(Context context, SharedPreferences prefs) {
        prefs.edit()
                .putBoolean(DevelopmentSettings.PREF_SHOW, true)
                .commit();
        return Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
    }

    public boolean getLastEnabledState() {
        return mLastEnabledState;
    }

    public void enableDevelopmentSettings() {
        mLastEnabledState = enableDevelopmentSettings(mContext, mDevelopmentPreferences);
    }

    public void disableDevelopmentSettings() {
        mDevelopmentPreferences.edit()
                .putBoolean(DevelopmentSettings.PREF_SHOW, false)
                .commit();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mLastEnabledState = false;
    }
}
