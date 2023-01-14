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

import static android.net.TetheringManager.TETHERING_WIFI;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;

import static com.android.settings.wifi.WifiUtils.setCanShowWifiHotspotCached;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.network.tether.TetheringManagerModel;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    WifiTetherPreferenceControllerTest.ShadowWifiTetherSettings.class,
    WifiTetherPreferenceControllerTest.ShadowWifiTetherSoftApManager.class
})
public class WifiTetherPreferenceControllerTest {

    private static final String SSID = "Pixel";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    TetheringManagerModel mTetheringManagerModel;
    @Mock
    private SwitchWidgetController mSwitch;
    private SoftApConfiguration mSoftApConfiguration;

    private WifiTetherPreferenceController mController;
    private PrimarySwitchPreference mPreference;

    @Before
    public void setUp() {
        setCanShowWifiHotspotCached(true);
        FakeFeatureFactory.setupForTest();
        mPreference = new PrimarySwitchPreference(mContext);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mSoftApConfiguration = new SoftApConfiguration.Builder().setSsid(SSID).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mSoftApConfiguration);
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_ENABLED);

        mController = new WifiTetherPreferenceController(mContext, mLifecycle, mWifiManager,
                false /* initSoftApManager */, true /* isWifiTetheringAllow */,
                mTetheringManagerModel);
        mController.displayPreference(mScreen);
        mController.mSwitch = mSwitch;
    }

    @Test
    public void isAvailable_canNotShowWifiHotspot_shouldReturnFalse() {
        setCanShowWifiHotspotCached(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_canShowWifiHostspot_shouldReturnTrue() {
        setCanShowWifiHotspotCached(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPreference_wifiTetheringNotAllowed_shouldDisable() {
        mController = new WifiTetherPreferenceController(mContext, mLifecycle, mWifiManager,
                false /* initSoftApManager */, false /* isWifiTetheringAllow */,
                mTetheringManagerModel);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo("Not allowed by your organization");
    }

    @Test
    public void displayPreference_wifiTetheringAllowed_shouldEnable() {
        mController = new WifiTetherPreferenceController(mContext, mLifecycle, mWifiManager,
                false /* initSoftApManager */, true /* isWifiTetheringAllow */,
                mTetheringManagerModel);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void testHandleWifiApStateChanged_stateEnabling_showEnablingSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_ENABLING, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo("Turning hotspot on\u2026");
    }

    @Test
    public void testHandleWifiApStateChanged_stateEnabled_showEnabledSummary() {
        mController.handleWifiApStateChanged(WIFI_AP_STATE_ENABLED, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo("Pixel is active");
    }

    @Test
    public void testHandleWifiApStateChanged_stateDisabling_showDisablingSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_DISABLING, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo("Turning off hotspot\u2026");
    }

    @Test
    public void testHandleWifiApStateChanged_stateDisabled_showDisabledSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_DISABLED, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo(
                "Not sharing internet or content with other devices");
    }

    @Test
    public void handleWifiApStateChanged_stateDisabled_setSwitchUnchecked() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_DISABLED, 0 /* reason */);

        verify(mSwitch).setChecked(false);
    }

    @Test
    public void handleWifiApStateChanged_stateEnabled_setSwitchChecked() {
        mController.handleWifiApStateChanged(WIFI_AP_STATE_ENABLED, 0 /* reason */);

        verify(mSwitch).setChecked(true);
    }

    @Test
    public void setDataSaverEnabled_setEnabled_setPrefDisabled() {
        mController.setDataSaverEnabled(true);

        assertThat(mPreference.isEnabled()).isFalse();
        verify(mSwitch).setEnabled(false);
    }

    @Test
    public void setDataSaverEnabled_setDisabled_setPrefEnabled() {
        mController.setDataSaverEnabled(false);

        assertThat(mPreference.isEnabled()).isTrue();
        verify(mSwitch).setEnabled(true);
    }

    @Test
    public void onSwitchToggled_isChecked_startTethering() {
        boolean ret = mController.onSwitchToggled(true);

        verify(mTetheringManagerModel).startTethering(TETHERING_WIFI);
        assertThat(ret).isTrue();
    }

    @Test
    public void onSwitchToggled_isUnchecked_stopTethering() {
        boolean ret = mController.onSwitchToggled(false);

        verify(mTetheringManagerModel).stopTethering(TETHERING_WIFI);
        assertThat(ret).isTrue();
    }


    @Implements(WifiTetherSettings.class)
    public static final class ShadowWifiTetherSettings {

        @Implementation
        protected static boolean isTetherSettingPageEnabled() {
            return true;
        }
    }

    @Implements(WifiTetherSoftApManager.class)
    public static final class ShadowWifiTetherSoftApManager {
        @Implementation
        protected void registerSoftApCallback() {
            // do nothing
        }

        @Implementation
        protected void unRegisterSoftApCallback() {
            // do nothing
        }
    }
}
