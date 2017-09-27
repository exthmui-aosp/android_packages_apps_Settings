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

import static com.android.settings.development.WifiAggressiveHandoverPreferenceController
        .SETTING_VALUE_OFF;
import static com.android.settings.development.WifiAggressiveHandoverPreferenceController
        .SETTING_VALUE_ON;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiAggressiveHandoverPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private WifiAggressiveHandoverPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        mController = new WifiAggressiveHandoverPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldEnableAggressiveHandover() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mWifiManager).enableAggressiveHandover(SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldDisableAggressiveHandover() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mWifiManager).enableAggressiveHandover(SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingEnabled_shouldEnablePreference() {
        when(mWifiManager.getAggressiveHandover()).thenReturn(1);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_shouldDisablePreference() {
        when(mWifiManager.getAggressiveHandover()).thenReturn(0);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_shouldEnablePreference() {
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mWifiManager).enableAggressiveHandover(SETTING_VALUE_OFF);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
