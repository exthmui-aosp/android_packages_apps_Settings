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

package com.android.settings.development.featureflags;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;

import androidx.preference.SwitchPreference;

public class FeatureFlagPreference extends SwitchPreference {

    private final String mKey;
    private final boolean mIsPersistent;

    public FeatureFlagPreference(Context context, String key) {
        super(context);
        mKey = key;
        setKey(key);
        setTitle(key);
        mIsPersistent = FeatureFlagPersistent.isPersistent(key);
        boolean isFeatureEnabled;
        if (mIsPersistent) {
            isFeatureEnabled = FeatureFlagPersistent.isEnabled(context, key);
        } else {
            isFeatureEnabled = FeatureFlagUtils.isEnabled(context, key);
        }
        super.setChecked(isFeatureEnabled);
    }

    @Override
    public void setChecked(boolean isChecked) {
        super.setChecked(isChecked);
        if (mIsPersistent) {
            FeatureFlagPersistent.setEnabled(getContext(), mKey, isChecked);
        } else {
            FeatureFlagUtils.setEnabled(getContext(), mKey, isChecked);
        }

        // A temporary logic for settings_hide_second_layer_page_navigate_up_button_in_two_pane
        // Remove it before Android T release.
        if (TextUtils.equals(mKey,
                FeatureFlagUtils.SETTINGS_HIDE_SECOND_LAYER_PAGE_NAVIGATE_UP_BUTTON_IN_TWO_PANE)) {
            Settings.Global.putString(getContext().getContentResolver(),
                    mKey, String.valueOf(isChecked));
        }
    }
}
