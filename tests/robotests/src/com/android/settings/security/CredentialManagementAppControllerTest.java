/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CredentialManagementAppControllerTest {

    private Context mContext;
    private CredentialManagementAppPreferenceController mController;
    private Preference mPreference;

    private static final String PREF_KEY_CREDENTIAL_MANAGEMENT_APP = "certificate_management_app";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new CredentialManagementAppPreferenceController(
                mContext, PREF_KEY_CREDENTIAL_MANAGEMENT_APP);
        mPreference = new Preference(mContext);
    }

    @Test
    public void getAvailabilityStatus_shouldAlwaysReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @Ignore
    public void updateState_noCredentialManagementApp_shouldDisablePreference() {
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isEqualTo(false);
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.no_certificate_management_app));
    }
}
