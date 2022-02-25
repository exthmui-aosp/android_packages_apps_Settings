/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.safetycenter;

import static android.safetycenter.SafetyCenterManager.ACTION_REFRESH_SAFETY_SOURCES;
import static android.safetycenter.SafetyCenterManager.EXTRA_REFRESH_SAFETY_SOURCE_IDS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.safetycenter.SafetySourceData;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class SafetySourceBroadcastReceiverTest {

    private Context mApplicationContext;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Mock
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = ApplicationProvider.getApplicationContext();
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mApplicationContext))
                .thenReturn(mLockPatternUtils);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsEnabled_withNoIntentAction_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent = new Intent().putExtra(EXTRA_REFRESH_SAFETY_SOURCE_IDS, new String[]{});

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsDisabled_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[]{ LockScreenSafetySource.SAFETY_SOURCE_ID });

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsEnabled_withNullSourceIds_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent = new Intent().setAction(ACTION_REFRESH_SAFETY_SOURCES);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsEnabled_withNoSourceIds_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCE_IDS, new String[]{});

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_withLockscreenSourceId_sendsLockscreenData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[]{ LockScreenSafetySource.SAFETY_SOURCE_ID });

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getId()).isEqualTo(LockScreenSafetySource.SAFETY_SOURCE_ID);
    }

    @Test
    public void sendSafetyData_withBiometricsSourceId_sendsBiometricData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[]{ BiometricsSafetySource.SAFETY_SOURCE_ID });

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        // TODO(b/215517420): Update this test when BiometricSafetySource is implemented.
        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_onBootCompleted_sendsBiometricAndLockscreenData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent = new Intent().setAction(Intent.ACTION_BOOT_COMPLETED);

        // TODO(b/215517420): Update this test when BiometricSafetySource is implemented to test
        // that biometrics data is also sent.
        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getId()).isEqualTo(LockScreenSafetySource.SAFETY_SOURCE_ID);
    }
}
