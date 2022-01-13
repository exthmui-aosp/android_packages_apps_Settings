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

package com.android.settings.accessibility;

import android.content.Context;
import android.os.Vibrator;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Abstract preference controller for a vibration intensity setting, that displays multiple
 * intensity levels to the user as a slider.
 */
public abstract class VibrationIntensityPreferenceController extends SliderPreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected final VibrationPreferenceConfig mPreferenceConfig;
    private final VibrationPreferenceConfig.SettingObserver mSettingsContentObserver;

    protected VibrationIntensityPreferenceController(Context context, String prefkey,
            VibrationPreferenceConfig preferenceConfig) {
        super(context, prefkey);
        mPreferenceConfig = preferenceConfig;
        mSettingsContentObserver = new VibrationPreferenceConfig.SettingObserver(
                preferenceConfig);
    }

    @Override
    public void onStart() {
        mSettingsContentObserver.register(mContext.getContentResolver());
    }

    @Override
    public void onStop() {
        mSettingsContentObserver.unregister(mContext.getContentResolver());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        mSettingsContentObserver.onDisplayPreference(this, preference);
        // TODO: remove this and replace with a different way to play the haptic preview without
        // relying on the setting being propagated to the service.
        preference.setContinuousUpdates(true);
        preference.setMin(getMin());
        preference.setMax(getMax());
    }

    @Override
    public int getMin() {
        return Vibrator.VIBRATION_INTENSITY_OFF;
    }

    @Override
    public int getMax() {
        return Vibrator.VIBRATION_INTENSITY_HIGH;
    }

    @Override
    public int getSliderPosition() {
        final int position = mPreferenceConfig.readIntensity();
        return Math.min(position, getMax());
    }

    @Override
    public boolean setSliderPosition(int position) {
        final boolean success = mPreferenceConfig.updateIntensity(position);

        if (success && (position != Vibrator.VIBRATION_INTENSITY_OFF)) {
            mPreferenceConfig.playVibrationPreview();
        }

        return success;
    }
}
