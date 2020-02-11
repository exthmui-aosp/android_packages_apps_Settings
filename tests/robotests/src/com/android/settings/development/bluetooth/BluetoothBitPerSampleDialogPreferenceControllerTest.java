/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BluetoothBitPerSampleDialogPreferenceControllerTest {

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private PreferenceScreen mScreen;

    private BluetoothBitPerSampleDialogPreferenceController mController;
    private BluetoothBitPerSampleDialogPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecConfig mCodecConfigAAC;
    private BluetoothCodecConfig mCodecConfigSBC;
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mBluetoothA2dpConfigStore = spy(new BluetoothA2dpConfigStore());
        mController = spy(new BluetoothBitPerSampleDialogPreferenceController(mContext, mLifecycle,
                mBluetoothA2dpConfigStore));
        mPreference = new BluetoothBitPerSampleDialogPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mCodecConfigAAC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
                BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                BluetoothCodecConfig.SAMPLE_RATE_NONE,
                BluetoothCodecConfig.BITS_PER_SAMPLE_16 | BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                BluetoothCodecConfig.CHANNEL_MODE_NONE,
                0, 0, 0, 0);
        mCodecConfigSBC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC,
                BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                BluetoothCodecConfig.SAMPLE_RATE_NONE,
                BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                BluetoothCodecConfig.CHANNEL_MODE_NONE,
                0, 0, 0, 0);
    }

    @Test
    public void writeConfigurationValues_selectDefault_setHighest() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigSBC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(
            mBluetoothA2dp.getActiveDevice())).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.writeConfigurationValues(0);
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24);
    }

    @Test
    public void writeConfigurationValues_checkBitsPerSample() {
        mController.writeConfigurationValues(1);
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_16);

        mController.writeConfigurationValues(2);
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24);

        mController.writeConfigurationValues(3);
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_32);
    }

    @Test
    public void getCurrentIndexByConfig_verifyIndex() {
        assertThat(mController.getCurrentIndexByConfig(mCodecConfigSBC)).isEqualTo(
                mController.convertCfgToBtnIndex(BluetoothCodecConfig.BITS_PER_SAMPLE_24));
    }

    @Test
    public void getSelectableIndex_verifyList() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigSBC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(
            mBluetoothA2dp.getActiveDevice())).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        List<Integer> indexList = new ArrayList<>();
        indexList.add(mPreference.getDefaultIndex());
        indexList.add(mController.convertCfgToBtnIndex(BluetoothCodecConfig.BITS_PER_SAMPLE_16));
        indexList.add(mController.convertCfgToBtnIndex(BluetoothCodecConfig.BITS_PER_SAMPLE_24));

        assertThat(mController.getSelectableIndex().containsAll(indexList)).isTrue();
        assertThat(indexList.containsAll(mController.getSelectableIndex())).isTrue();
    }
}
