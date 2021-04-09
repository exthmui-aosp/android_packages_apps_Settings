/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;

import android.content.Context;
import android.hardware.SensorPrivacyManager;

import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.widget.BannerMessagePreference;

/**
 * The controller of Screen attention's camera disabled warning preference.
 * The preference appears when the camera access is disabled for Screen Attention feature.
 */
public class AdaptiveSleepCameraStatePreferenceController {
    @VisibleForTesting
    final BannerMessagePreference mPreference;
    private final SensorPrivacyManager mPrivacyManager;

    public AdaptiveSleepCameraStatePreferenceController(Context context) {
        mPreference = new BannerMessagePreference(context);
        mPreference.setTitle(R.string.auto_rotate_camera_lock_title);
        mPreference.setSummary(R.string.adaptive_sleep_camera_lock_summary);
        mPreference.setPositiveButtonText(R.string.allow);
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPrivacyManager.addSensorPrivacyListener(CAMERA,
                enabled -> updateVisibility());
        mPreference.setPositiveButtonOnClickListener(p -> {
            mPrivacyManager.setSensorPrivacy(CAMERA, false);
        });
    }

    /**
     * Adds the controlled preference to the provided preference screen.
     */
    public void addToScreen(PreferenceScreen screen) {
        screen.addPreference(mPreference);
        updateVisibility();
    }

    /**
     * Need this because all controller tests use RoboElectric. No easy way to mock this service,
     * so we mock the call we need
     */
    @VisibleForTesting
    boolean isCameraLocked() {
        return mPrivacyManager.isSensorPrivacyEnabled(CAMERA);
    }

    /**
     * Refreshes the visibility of the preference.
     */
    public void updateVisibility() {
        mPreference.setVisible(isCameraLocked());
    }
}
