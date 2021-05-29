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

package com.android.settings.widget;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SettingsMainSwitchPreferenceTest {

    @Mock
    private EnforcedAdmin mEnforcedAdmin;
    private SettingsMainSwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        final SettingsMainSwitchBar switchBar = new SettingsMainSwitchBar(context);
        mPreference = new SettingsMainSwitchPreference(context);
        ReflectionHelpers.setField(mPreference, "mEnforcedAdmin", mEnforcedAdmin);
        ReflectionHelpers.setField(mPreference, "mMainSwitchBar", switchBar);
    }

    @Test
    public void updateStatus_isRestricted_restrictIconShouldDisplay() {
        mPreference.updateStatus(true);

        final SettingsMainSwitchBar switchBar = mPreference.getSwitchBar();
        final ImageView restrictedIcon = switchBar.findViewById(
                com.android.settingslib.widget.R.id.restricted_icon);

        assertThat(restrictedIcon.getVisibility() == View.VISIBLE).isTrue();
    }
}
