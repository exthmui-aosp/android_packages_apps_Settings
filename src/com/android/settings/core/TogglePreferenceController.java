/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.core;

import android.app.slice.Slice;
import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

/**
 * Abstract class that consolidates logic for updating toggle controllers.
 * It automatically handles the getting and setting of the switch UI element.
 * Children of this class implement methods to get and set the underlying value of the setting.
 */
public abstract class TogglePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "TogglePrefController";

    public TogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * @return {@code true} if the Setting is enabled.
     */
    public abstract boolean isChecked();

    /**
     * Set the Setting to {@param isChecked}
     *
     * @param isChecked Is {@true} when the setting should be enabled.
     */
    public abstract void setChecked(boolean isChecked);

    @Override
    public final void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(isChecked());
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean auto = (Boolean) newValue;
        setChecked(auto);
        return true;
    }

    @Override
    public Slice getSettingSlice() {
        // TODO
        return null;
    }
}