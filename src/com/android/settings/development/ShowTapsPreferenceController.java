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
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class ShowTapsPreferenceController extends DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String SHOW_TOUCHES_KEY = "show_touches";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    private SwitchPreference mPreference;

    public ShowTapsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return SHOW_TOUCHES_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int showTapsMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, SETTING_VALUE_OFF);
        mPreference.setChecked(showTapsMode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SHOW_TOUCHES,
                SETTING_VALUE_OFF);
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }
}
