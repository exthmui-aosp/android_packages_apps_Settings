/**
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
import static org.mockito.Mockito.when;

import android.content.Context;

import android.app.AppOpsManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppStateChangeWifiStateBridgeTest {

    @Mock
    private AppEntry mEntry;
    @Mock
    private AppStateChangeWifiStateBridge.WifiSettingsState mState;
    private Context mContext;
    private AppFilter mFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mFilter = AppStateChangeWifiStateBridge.FILTER_CHANGE_WIFI_STATE;
    }

    @Test
    public void testFilterApp_inputNull_returnFalse() {
        assertThat(mFilter.filterApp(null)).isFalse();
    }

    @Test
    public void testFilterApp_extraInfoNull_returnFalse() {
        mEntry.extraInfo = null;
        assertThat(mFilter.filterApp(mEntry)).isFalse();
    }

    @Test
    public void testFilterApp_permissionedDeclaredTrue_returnTrue() {
        mState.permissionDeclared = true;
        mEntry.extraInfo = mState;
        assertThat(mFilter.filterApp(mEntry)).isTrue();
    }

    @Test
    public void testFilterApp_permissionedDeclaredFalse_returnFalse() {
        mState.permissionDeclared = false;
        mEntry.extraInfo = mState;
        assertThat(mFilter.filterApp(mEntry)).isFalse();
    }
}
