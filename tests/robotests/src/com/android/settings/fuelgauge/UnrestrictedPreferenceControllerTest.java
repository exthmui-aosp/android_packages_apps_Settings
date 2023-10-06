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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UnrestrictedPreferenceControllerTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    private UnrestrictedPreferenceController mController;
    private SelectorWithWidgetPreference mPreference;

    @Mock BatteryOptimizeUtils mockBatteryOptimizeUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new UnrestrictedPreferenceController(
                RuntimeEnvironment.application, UID, PACKAGE_NAME);
        mPreference = new SelectorWithWidgetPreference(RuntimeEnvironment.application);
        mController.mBatteryOptimizeUtils = mockBatteryOptimizeUtils;
    }

    @Test
    public void testUpdateState_isValidPackage_prefEnabled() {
        when(mockBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void testUpdateState_invalidPackage_prefDisabled() {
        when(mockBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_isSystemOrDefaultAppAndUnrestrictedStates_prefChecked() {
        when(mockBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mockBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);
        when(mockBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(
                BatteryOptimizeUtils.MODE_UNRESTRICTED);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_isSystemOrDefaultApp_prefUnchecked() {
        when(mockBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mockBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_isUnrestrictedStates_prefChecked() {
        when(mockBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mockBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(
                BatteryOptimizeUtils.MODE_UNRESTRICTED);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_prefUnchecked() {
        when(mockBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testHandlePreferenceTreeClick_samePrefKey_verifyAction() {
        mPreference.setKey(mController.KEY_UNRESTRICTED_PREF);
        mController.handlePreferenceTreeClick(mPreference);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick_incorrectPrefKey_noAction() {
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }
}
