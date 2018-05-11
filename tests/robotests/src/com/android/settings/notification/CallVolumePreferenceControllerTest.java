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

package com.android.settings.notification;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;


import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowAudioManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowAudioManager.class})
public class CallVolumePreferenceControllerTest {
    private static final String TEST_KEY = "Test_Key";

    @Mock
    private AudioHelper mHelper;

    private Context mContext;
    private CallVolumePreferenceController mController;
    private ShadowAudioManager mShadowAudioManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new CallVolumePreferenceController(mContext, TEST_KEY);
        mController.setAudioHelper(mHelper);
        mShadowAudioManager = ShadowAudioManager.getShadow();
    }

    @Test
    public void getAvailabilityStatus_singleVolume_shouldReturnDisable() {
        when(mHelper.isSingleVolume()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_notSingleVolume_shouldReturnAvailable() {
        when(mHelper.isSingleVolume()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getMuteIcon_shouldEqualToOriginalIcon() {
        assertThat(mController.getMuteIcon()).isEqualTo(R.drawable.ic_local_phone_24_lib);
    }

    @Test
    public void getAudioStream_onBluetoothScoOff_shouldEqualToStreamVoiceCall() {
        mShadowAudioManager.setBluetoothScoOn(false);

        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_VOICE_CALL);
    }

    @Test
    public void getAudioStream_onBluetoothScoOn_shouldEqualToStreamBtSco() {
        mShadowAudioManager.setBluetoothScoOn(true);

        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_BLUETOOTH_SCO);
    }
}
