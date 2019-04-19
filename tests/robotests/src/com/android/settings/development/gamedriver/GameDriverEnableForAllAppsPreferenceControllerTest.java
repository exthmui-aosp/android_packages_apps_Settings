/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.gamedriver;

import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_ALL_APPS;
import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GameDriverEnableForAllAppsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private GameDriverContentObserver mGameDriverContentObserver;

    private Context mContext;
    private ContentResolver mResolver;
    private GameDriverEnableForAllAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();

        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        mController = new GameDriverEnableForAllAppsPreferenceController(mContext, "testKey");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void displayPreference_shouldAddSwitchPreference() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onStart_shouldRegister() {
        mController.mGameDriverContentObserver = mGameDriverContentObserver;
        mController.onStart();

        verify(mGameDriverContentObserver).register(mResolver);
    }

    @Test
    public void onStop_shouldUnregister() {
        mController.mGameDriverContentObserver = mGameDriverContentObserver;
        mController.onStop();

        verify(mGameDriverContentObserver).unregister(mResolver);
    }

    @Test
    public void onPreferenceChange_check_shouldUpdateSettingsGlobal() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.Global.getInt(
                           mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_ALL_APPS);
    }

    @Test
    public void onPreferenceChange_uncheck_shouldUpdateSettingsGlobal() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_ALL_APPS);
        mController.onPreferenceChange(mPreference, false);

        assertThat(Settings.Global.getInt(
                           mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_DEFAULT);
    }
}
