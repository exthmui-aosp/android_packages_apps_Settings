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

package com.android.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.safetycenter.SafetyCenterStatus;
import com.android.settings.security.TopLevelSecurityEntryPreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(AndroidJUnit4.class)
public class TopLevelPrivacyEntryPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "top_level_privacy";

    private TopLevelPrivacyEntryPreferenceController mTopLevelPrivacyEntryPreferenceController;

    @Mock
    private Context mContext;

    @Before
    public void setUp() {
        DeviceConfig.resetToDefaults(Settings.RESET_MODE_PACKAGE_DEFAULTS,
                DeviceConfig.NAMESPACE_PRIVACY);

        mTopLevelPrivacyEntryPreferenceController =
                new TopLevelPrivacyEntryPreferenceController(mContext, PREFERENCE_KEY);
    }

    @After
    public void tearDown() {
        DeviceConfig.resetToDefaults(Settings.RESET_MODE_PACKAGE_DEFAULTS,
                DeviceConfig.NAMESPACE_PRIVACY);
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterEnabled_returnsUnavailable() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_PRIVACY,
                SafetyCenterStatus.SAFETY_CENTER_IS_ENABLED,
                /* value = */ Boolean.toString(true),
                /* makeDefault = */ false);

        assertThat(mTopLevelPrivacyEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSecurityEntryPreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterDisabled_returnsAvailable() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_PRIVACY,
                SafetyCenterStatus.SAFETY_CENTER_IS_ENABLED,
                /* value = */ Boolean.toString(false),
                /* makeDefault = */ false);

        assertThat(mTopLevelPrivacyEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSecurityEntryPreferenceController.AVAILABLE);
    }
}
