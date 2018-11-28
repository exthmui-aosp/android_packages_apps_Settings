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
 * limitations under the License
 */

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
        SettingsShadowResourcesImpl.class
})
public class NetworkRequestDialogActivityTest {

    @Test
    public void LaunchActivity_shouldShowNetworkRequestDialog() {
        NetworkRequestDialogActivity activity = Robolectric
                .setupActivity(NetworkRequestDialogActivity.class);

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog.isShowing()).isTrue();
    }
}
