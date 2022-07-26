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
import android.content.res.Resources;
import android.graphics.Color;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener;
import com.android.settings.core.BasePreferenceController;

/** Preference controller for caption background color. */
public class CaptionBackgroundColorController extends BasePreferenceController
        implements OnValueChangedListener {

    private final CaptionHelper mCaptionHelper;

    public CaptionBackgroundColorController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCaptionHelper = new CaptionHelper(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final ColorPreference preference = screen.findPreference(getPreferenceKey());
        final Resources res = mContext.getResources();
        final int[] colorValues = res.getIntArray(R.array.captioning_color_selector_values);
        final String[] colorTitles = res.getStringArray(
                R.array.captioning_color_selector_titles);
        // Add "none" as an additional option for backgrounds.
        final int[] backgroundColorValues = new int[colorValues.length + 1];
        final String[] backgroundColorTitles = new String[colorTitles.length + 1];
        System.arraycopy(colorValues, 0, backgroundColorValues, 1, colorValues.length);
        System.arraycopy(colorTitles, 0, backgroundColorTitles, 1, colorTitles.length);
        backgroundColorValues[0] = Color.TRANSPARENT;
        backgroundColorTitles[0] = mContext.getString(R.string.color_none);
        preference.setTitles(backgroundColorTitles);
        preference.setValues(backgroundColorValues);
        final int backBackgroundColor = mCaptionHelper.getBackgroundColor();
        final int color = CaptionUtils.parseColor(backBackgroundColor);
        preference.setValue(color);
        preference.setOnValueChangedListener(this);
    }

    @Override
    public void onValueChanged(ListDialogPreference preference, int value) {
        final int backBackgroundColor = mCaptionHelper.getBackgroundColor();
        final int opacity = CaptionUtils.parseOpacity(backBackgroundColor);
        final int merged = CaptionUtils.mergeColorOpacity(value, opacity);
        mCaptionHelper.setBackgroundColor(merged);
        mCaptionHelper.setEnabled(true);
    }
}
