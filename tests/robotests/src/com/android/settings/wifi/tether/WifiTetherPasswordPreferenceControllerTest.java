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

package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.ValidatedEditTextPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiTetherPasswordPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;
    @Mock
    private PreferenceScreen mScreen;

    private WifiTetherPasswordPreferenceController mController;
    private ValidatedEditTextPreference mPreference;
    private WifiConfiguration mConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPreference = new ValidatedEditTextPreference(RuntimeEnvironment.application);
        mConfig = new WifiConfiguration();
        mConfig.SSID = "test_1234";
        mConfig.preSharedKey = "test_password";

        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiApConfiguration()).thenReturn(mConfig);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = new WifiTetherPasswordPreferenceController(mContext, mListener);
    }

    @Test
    public void displayPreference_shouldStylePreference() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getText()).isEqualTo(mConfig.preSharedKey);
        assertThat(mPreference.getSummary()).isEqualTo(mConfig.preSharedKey);
    }

    @Test
    public void changePreference_shouldUpdateValue() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getPassword()).isEqualTo("1");

        mController.onPreferenceChange(mPreference, "0");
        assertThat(mController.getPassword()).isEqualTo("0");

        verify(mListener, times(2)).onTetherConfigUpdated();
    }

    @Test
    public void updateDisplay_shouldUpdateValue() {
        // Set controller password to anything and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getPassword()).isEqualTo("1");

        // Create a new config using different password
        final WifiConfiguration config = new WifiConfiguration();
        config.preSharedKey = "test_1234";
        when(mWifiManager.getWifiApConfiguration()).thenReturn(config);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mController.getPassword()).isEqualTo(config.preSharedKey);
        assertThat(mPreference.getSummary()).isEqualTo(config.preSharedKey);
    }

    @Test
    public void getSecuritySettingForPassword_returnCorrectType() {
        // valid wpa2 password
        mController.displayPreference(mScreen);
        assertThat(mController.getSecuritySettingForPassword())
                .isEqualTo(WifiConfiguration.KeyMgmt.WPA2_PSK);

        // password which is empty returns NONE
        mConfig = new WifiConfiguration();
        mConfig.SSID = "test_1234";
        mConfig.preSharedKey = "";
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiApConfiguration()).thenReturn(mConfig);
        mController = new WifiTetherPasswordPreferenceController(mContext, mListener);

        mController.displayPreference(mScreen);
        assertThat(mController.getSecuritySettingForPassword())
                .isEqualTo(WifiConfiguration.KeyMgmt.NONE);

        // default for unsupported types is wpa2
        mConfig = new WifiConfiguration();
        mConfig.SSID = "test_1234";
        mConfig.preSharedKey = "short";
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiApConfiguration()).thenReturn(mConfig);
        mController = new WifiTetherPasswordPreferenceController(mContext, mListener);

        mController.displayPreference(mScreen);
        assertThat(mController.getSecuritySettingForPassword())
                .isEqualTo(WifiConfiguration.KeyMgmt.WPA2_PSK);
    }

    @Test
    public void updateDisplay_shouldSetInputType() {
        // Set controller password to anything and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getPassword()).isEqualTo("1");

        // Create a new config using different password
        final WifiConfiguration config = new WifiConfiguration();
        config.preSharedKey = "test_1234";
        when(mWifiManager.getWifiApConfiguration()).thenReturn(config);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mPreference.isPassword()).isTrue();
    }
}
