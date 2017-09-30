/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import android.view.ThreadedRenderer;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {SettingsShadowSystemProperties.class})
public class HardwareLayersUpdatesPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private HardwareLayersUpdatesPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new HardwareLayersUpdatesPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void onPreferenceChanged_settingEnabled_turnOnHardwareLayersUpdates() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY, false /* default */);

        assertThat(mode).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_turnOffHardwareLayersUpdates() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY, false /* default */);

        assertThat(mode).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        SettingsShadowSystemProperties.set(ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY,
                Boolean.toString(true));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        SettingsShadowSystemProperties.set(ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY,
                Boolean.toString(false));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsEnabled_shouldEnablePreference() {
        mController.onDeveloperOptionsEnabled();

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY, false /* default */);

        assertThat(mode).isFalse();
        verify(mPreference).setEnabled(true);
    }
}
