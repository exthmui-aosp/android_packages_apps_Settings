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

import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Vibration intensity settings configuration to be shared between different preference
 * controllers that handle the same setting key.
 */
public abstract class VibrationPreferenceConfig {

    /**
     * SettingsProvider key for the main "Vibration & haptics" toggle preference, that can disable
     * all device vibrations.
     */
    public static final String MAIN_SWITCH_SETTING_KEY = Settings.System.VIBRATE_ON;

    protected final ContentResolver mContentResolver;
    private final Vibrator mVibrator;
    private final String mSettingKey;
    private final int mDefaultIntensity;
    private final VibrationAttributes mVibrationAttributes;

    /** Returns true if the user setting for enabling device vibrations is enabled. */
    public static boolean isMainVibrationSwitchEnabled(ContentResolver contentResolver) {
        return Settings.System.getInt(contentResolver, MAIN_SWITCH_SETTING_KEY, ON) == ON;
    }

    public VibrationPreferenceConfig(Context context, String settingKey, int vibrationUsage) {
        mContentResolver = context.getContentResolver();
        mVibrator = context.getSystemService(Vibrator.class);
        mSettingKey = settingKey;
        mDefaultIntensity = mVibrator.getDefaultVibrationIntensity(vibrationUsage);
        mVibrationAttributes = new VibrationAttributes.Builder()
                .setUsage(vibrationUsage)
                .build();
    }

    /** Returns the setting key for this setting preference. */
    public String getSettingKey() {
        return mSettingKey;
    }

    /** Returns true if this setting preference is enabled for user update. */
    public boolean isPreferenceEnabled() {
        return isMainVibrationSwitchEnabled(mContentResolver);
    }

    /** Returns the default intensity to be displayed when the setting value is not set. */
    public int getDefaultIntensity() {
        return mDefaultIntensity;
    }

    /** Reads setting value for corresponding {@link VibrationPreferenceConfig} */
    public int readIntensity() {
        return Settings.System.getInt(mContentResolver, mSettingKey, mDefaultIntensity);
    }

    /** Update setting value for corresponding {@link VibrationPreferenceConfig} */
    public boolean updateIntensity(int intensity) {
        return Settings.System.putInt(mContentResolver, mSettingKey, intensity);
    }

    /** Play a vibration effect with intensity just selected by the user. */
    public void playVibrationPreview() {
        mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
                mVibrationAttributes);
    }

    /** {@link ContentObserver} for a setting described by a {@link VibrationPreferenceConfig}. */
    public static final class SettingObserver extends ContentObserver {
        private static final Uri MAIN_SWITCH_SETTING_URI =
                Settings.System.getUriFor(MAIN_SWITCH_SETTING_KEY);

        private final Uri mUri;
        private AbstractPreferenceController mPreferenceController;
        private Preference mPreference;

        /** Creates observer for given preference. */
        public SettingObserver(VibrationPreferenceConfig preferenceConfig) {
            super(new Handler(/* async= */ true));
            mUri = Settings.System.getUriFor(preferenceConfig.getSettingKey());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mPreferenceController == null || mPreference == null) {
                // onDisplayPreference not triggered yet, nothing to update.
                return;
            }
            if (mUri.equals(uri) || MAIN_SWITCH_SETTING_URI.equals(uri)) {
                mPreferenceController.updateState(mPreference);
            }
        }

        /**
         * Register this observer to given {@link ContentResolver}, to be called from lifecycle
         * {@code onStart} method.
         */
        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(mUri, /* notifyForDescendants= */ false, this);
            contentResolver.registerContentObserver(MAIN_SWITCH_SETTING_URI,
                    /* notifyForDescendants= */ false, this);
        }

        /**
         * Unregister this observer from given {@link ContentResolver}, to be called from lifecycle
         * {@code onStop} method.
         */
        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        /**
         * Binds this observer to given controller and preference, once it has been displayed to the
         * user.
         */
        public void onDisplayPreference(AbstractPreferenceController controller,
                Preference preference) {
            mPreferenceController = controller;
            mPreference = preference;
        }
    }
}
