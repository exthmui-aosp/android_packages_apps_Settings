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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import com.android.settings.R;
import com.android.settings.applications.SpacePreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.widget.ButtonPreference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BluetoothDetailsPairOtherController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsPairOtherControllerTest extends BluetoothDetailsControllerTestBase  {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private CachedBluetoothDevice mSubCachedDevice;
    private BluetoothDetailsPairOtherController mController;
    private ButtonPreference mPreference;
    private SpacePreference mSpacePreference;

    @Override
    public void setUp() {
        super.setUp();

        mController = new BluetoothDetailsPairOtherController(mContext, mFragment, mCachedDevice,
                mLifecycle);
        mPreference = new ButtonPreference(mContext);
        mSpacePreference = new SpacePreference(mContext, null);
        mPreference.setKey(mController.getPreferenceKey());
        mSpacePreference.setKey(BluetoothDetailsPairOtherController.KEY_SPACE);
        mScreen.addPreference(mPreference);
        mScreen.addPreference(mSpacePreference);
    }

    @Test
    public void init_leftSideDevice_rightSideButtonTitle() {
        when(mCachedDevice.getDeviceSide()).thenReturn(HearingAidInfo.DeviceSide.SIDE_LEFT);

        mController.init(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_pair_right_ear_button));
    }

    @Test
    public void init_rightSideDevice_leftSideButtonTitle() {
        when(mCachedDevice.getDeviceSide()).thenReturn(HearingAidInfo.DeviceSide.SIDE_RIGHT);

        mController.init(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_pair_left_ear_button));
    }

    @Test
    public void init_isNotConnectedAshaHearingAidDevice_notVisiblePreference() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(false);

        mController.init(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mSpacePreference.isVisible()).isFalse();
    }

    @Test
    public void isAvailable_isNotConnectedAshaHearingAidDevice_notAvailable() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_isConnectedAshaHearingAidDevice_isMonaural_notAvailable() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_MONAURAL);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_subDeviceIsConnectedAshaHearingAidDevice_notAvailable() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mSubCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getSubDevice()).thenReturn(mSubCachedDevice);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_subDeviceIsNotConnectedAshaHearingAidDevice_available() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mSubCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(false);
        when(mCachedDevice.getSubDevice()).thenReturn(mSubCachedDevice);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_subDeviceNotExist_available() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedDevice.getSubDevice()).thenReturn(null);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void refresh_leftSideDevice_leftSideButtonTitle() {
        when(mCachedDevice.getDeviceSide()).thenReturn(HearingAidInfo.DeviceSide.SIDE_RIGHT);
        mController.init(mScreen);

        mController.refresh();

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_pair_left_ear_button));
    }

    @Test
    public void refresh_isNotConnectedAshaHearingAidDevice_notVisiblePreference() {
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(false);
        mController.init(mScreen);

        mController.refresh();

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mSpacePreference.isVisible()).isFalse();
    }
}
