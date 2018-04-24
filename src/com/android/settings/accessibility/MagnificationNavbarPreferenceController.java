/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.accessibility;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class MagnificationNavbarPreferenceController extends BasePreferenceController {

    private boolean mIsFromSUW = false;

    public MagnificationNavbarPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setIsFromSUW(boolean fromSUW) {
        mIsFromSUW = fromSUW;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            Bundle extras = preference.getExtras();
            extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED);
            extras.putInt(AccessibilitySettings.EXTRA_TITLE_RES,
                    R.string.accessibility_screen_magnification_navbar_title);
            extras.putInt(AccessibilitySettings.EXTRA_SUMMARY_RES,
                    R.string.accessibility_screen_magnification_navbar_summary);
            extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    Settings.Secure.getInt(mContext.getContentResolver(),
                            Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0)
                            == 1);
            extras.putBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW, mIsFromSUW);
        }
        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        return MagnificationPreferenceFragment.isApplicable(mContext.getResources())
                ? AVAILABLE
                : DISABLED_UNSUPPORTED;
    }

    @Override
    public CharSequence getSummary() {
        int resId = 0;
        if (mIsFromSUW) {
            resId = R.string.accessibility_screen_magnification_navbar_short_summary;
        } else {
            final boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) == 1;
            resId = (enabled ? R.string.accessibility_feature_state_on :
                    R.string.accessibility_feature_state_off);
        }
        return mContext.getText(resId);
    }
}
