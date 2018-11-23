/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiDppConfiguratorActivityTest {
    @Rule
    public final ActivityTestRule<WifiDppConfiguratorActivity> mActivityRule =
            new ActivityTestRule<>(WifiDppConfiguratorActivity.class);

    @Test
    public void launchActivity_modeQrCodeScanner_shouldNotAutoFinish() {
        Intent intent = new Intent();
        intent.putExtra(WifiDppConfiguratorActivity.EXTRA_LAUNCH_MODE,
                WifiDppConfiguratorActivity.LaunchMode.LAUNCH_MODE_QR_CODE_SCANNER.getMode());
        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isEqualTo(false);
    }

    @Test
    public void launchActivity_modeQrCodeGenerator_shouldNotAutoFinish() {
        Intent intent = new Intent();
        intent.putExtra(WifiDppConfiguratorActivity.EXTRA_LAUNCH_MODE,
                WifiDppConfiguratorActivity.LaunchMode.LAUNCH_MODE_QR_CODE_GENERATOR.getMode());
        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isEqualTo(false);
    }

    @Test
    public void launchActivity_modeChooseSavedWifiNetwork_shouldNotAutoFinish() {
        Intent intent = new Intent();
        intent.putExtra(WifiDppConfiguratorActivity.EXTRA_LAUNCH_MODE,
                WifiDppConfiguratorActivity.LaunchMode
                .LAUNCH_MODE_CHOOSE_SAVED_WIFI_NETWORK.getMode());
        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isEqualTo(false);
    }

    @Test
    public void launchActivity_noLaunchMode_shouldFinishActivityWithResultCodeCanceled() {
        // If we do not specify launch mode, the activity will finish itself right away
        Intent intent = new Intent();
        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivityResult().getResultCode()).
                isEqualTo(Activity.RESULT_CANCELED);
    }
}
