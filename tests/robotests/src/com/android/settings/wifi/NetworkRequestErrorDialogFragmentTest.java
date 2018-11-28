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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.wifi.NetworkRequestErrorDialogFragment.ERROR_DIALOG_TYPE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResourcesImpl.class, ShadowAlertDialogCompat.class})
public class NetworkRequestErrorDialogFragmentTest {

    private FragmentActivity mActivity;
    private NetworkRequestErrorDialogFragment mFragment;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = spy(NetworkRequestErrorDialogFragment.newInstance());
        mFragment.show(mActivity.getSupportFragmentManager(), null);
    }

    @Test
    public void display_shouldShowTimeoutDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();

        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(RuntimeEnvironment.application
                .getString(R.string.network_connection_timeout_dialog_message))
                .isEqualTo(shadowAlertDialog.getMessage());
    }

    @Test
    public void display_shouldShowAbortDialog() {
        mFragment = spy(NetworkRequestErrorDialogFragment.newInstance());
        Bundle bundle = new Bundle();
        bundle.putSerializable(NetworkRequestErrorDialogFragment.DIALOG_TYPE,
                ERROR_DIALOG_TYPE.ABORT);
        mFragment.setArguments(bundle);
        mFragment.show(mActivity.getSupportFragmentManager(), null);

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();

        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(RuntimeEnvironment.application
                .getString(R.string.network_connection_errorstate_dialog_message))
                .isEqualTo(shadowAlertDialog.getMessage());
    }

    @Test
    public void clickPositiveButton_shouldCallStartScanningDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertThat(positiveButton).isNotNull();

        positiveButton.performClick();
        verify(mFragment, times(1)).startScanningDialog();
    }

    @Test
    public void clickNegativeButton_shouldCloseTheDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton).isNotNull();

        negativeButton.performClick();
        assertThat(alertDialog.isShowing()).isFalse();
    }
}
