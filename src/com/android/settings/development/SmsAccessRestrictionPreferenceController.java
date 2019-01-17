/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

// STOPSHIP b/118694572: remove the kill switch once the feature is tested and stable
public class SmsAccessRestrictionPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String SMS_ACCESS_RESTRICTION_ENABLED_KEY
            = "sms_access_restriction_enabled";

    public SmsAccessRestrictionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return SMS_ACCESS_RESTRICTION_ENABLED_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSetting((boolean) newValue);
        return true;
    }

    private void writeSetting(boolean isEnabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SMS_ACCESS_RESTRICTION_ENABLED,
                isEnabled ? 1 : 0);
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SMS_ACCESS_RESTRICTION_ENABLED, 0);
        ((SwitchPreference) mPreference).setChecked(mode != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeSetting(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
