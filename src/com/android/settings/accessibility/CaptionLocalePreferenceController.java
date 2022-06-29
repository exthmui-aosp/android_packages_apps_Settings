/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/** Controller that shows the caption locale summary. */
public class CaptionLocalePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final CaptioningManager mCaptioningManager;
    private LocalePreference mPreference;

    public CaptionLocalePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCaptioningManager = context.getSystemService(CaptioningManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        final String rawLocale = mCaptioningManager.getRawLocale();
        mPreference.setValue(rawLocale == null ? "" : rawLocale);
    }

    @Override
    public CharSequence getSummary() {
        return mPreference.getEntry();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE, (String) newValue);
        mPreference.setValue((String) newValue);
        mPreference.setSummary(mPreference.getEntry());
        return true;
    }
}
