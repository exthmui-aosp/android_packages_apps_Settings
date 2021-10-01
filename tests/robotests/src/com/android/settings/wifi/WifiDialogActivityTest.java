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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settings.wifi.dpp.WifiDppEnrolleeActivity;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowConnectivityManager.class,
        ShadowWifiManager.class,
        ShadowAlertDialogCompat.class
})
public class WifiDialogActivityTest {

    private static final String AP1_SSID = "\"ap1\"";
    @Mock
    private WifiConfigController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = AP1_SSID;
        doReturn(wifiConfig).when(mController).getConfig();
    }

    @Test
    public void onSubmit_shouldConnectToNetwork() {
        WifiDialogActivity activity = Robolectric.setupActivity(WifiDialogActivity.class);
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController);

        activity.onSubmit(dialog);

        assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP1_SSID);
    }

    @Test
    public void onSubmit_whenConnectForCallerIsFalse_shouldNotConnectToNetwork() {
        WifiDialogActivity activity =
                Robolectric.buildActivity(
                        WifiDialogActivity.class,
                        new Intent().putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false))
                        .setup().get();
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController);

        activity.onSubmit(dialog);

        assertThat(ShadowWifiManager.get().savedWifiConfig).isNull();
    }

    @Test
    public void onSubmit_whenLaunchInSetupFlow_shouldBeLightThemeForWifiDialog() {
        WifiDialogActivity activity =
                Robolectric.buildActivity(
                        WifiDialogActivity.class,
                        new Intent()
                                .putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false)
                                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                                .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true))
                        .setup().get();
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        activity.onSubmit(dialog);

        assertThat(dialog.getContext().getThemeResId())
                .isEqualTo(R.style.SuwAlertDialogThemeCompat_Light);
    }

    @Test
    public void onScan_whenLaunchFromDeferredSetup_shouldApplyLightTheme() {
        ActivityController<WifiDppEnrolleeActivity> controller = Robolectric.buildActivity(
                WifiDppEnrolleeActivity.class,
                new Intent()
                        .setAction(WifiDppEnrolleeActivity.ACTION_ENROLLEE_QR_CODE_SCANNER)
                        .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                        .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
        );
        controller.create();

        Intent intent = controller.getIntent();
        assertThat(intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, false)).isTrue();
        assertThat(intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, false)).isTrue();

        assertThat(controller.get().getThemeResId()).
                isEqualTo(R.style.LightTheme_SettingsBase_SetupWizard);
    }
}
