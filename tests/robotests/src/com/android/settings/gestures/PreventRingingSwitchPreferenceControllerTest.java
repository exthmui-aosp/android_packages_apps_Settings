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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.widget.SwitchBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreventRingingSwitchPreferenceControllerTest {
    private Context mContext;
    private Resources mResources;
    private PreventRingingSwitchPreferenceController mController;
    private Preference mPreference = mock(Preference.class);

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(com.android.internal.R.bool.config_volumeHushGestureEnabled))
                .thenReturn(true);
        mController = new PreventRingingSwitchPreferenceController(mContext);
        mController.mSwitch = mock(SwitchBar.class);
    }

    @Test
    public void testIsAvailable_configIsTrue_shouldReturnTrue() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_configIsFalse_shouldReturnFalse() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testOn_updateState_hushOff() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_OFF);
        mController.updateState(mPreference);
        verify(mController.mSwitch, times(1)).setChecked(false);
    }

    @Test
    public void testOn_updateState_hushVibrate() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_VIBRATE);
        mController.updateState(mPreference);
        verify(mController.mSwitch, times(1)).setChecked(true);
    }

    @Test
    public void testOn_updateState_hushMute() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_MUTE);
        mController.updateState(mPreference);
        verify(mController.mSwitch, times(1)).setChecked(true);
    }
}
