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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(SettingsRobolectricTestRunner.class)
public class DataUsagePreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    private DataUsagePreferenceController mController;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(Robolectric.setupActivity(Activity.class));
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mPreference = new SwitchPreference(mContext);
        mController = new DataUsagePreferenceController(mContext, "data_usage");
        mController.init(SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_validSubId_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnsearchable() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void handlePreferenceTreeClick_needDialog_showDialog() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        mController.handlePreferenceTreeClick(mPreference);

        verify(mContext).startActivity(captor.capture());
        final Intent intent = captor.getValue();

        assertThat(intent.getAction()).isEqualTo(Settings.ACTION_MOBILE_DATA_USAGE);
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID, 0)).isEqualTo(SUB_ID);
    }

    @Test
    public void updateState_invalidSubId_disabled() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }
}
