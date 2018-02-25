/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.webview.WebViewUpdateServiceWrapper;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WebViewAppPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PackageManagerWrapper mPackageManager;
    @Mock
    private WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;
    @Mock
    private Preference mPreference;
    @Mock
    private DefaultAppInfo mAppInfo;

    private Context mContext;
    private WebViewAppPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new WebViewAppPreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
        ReflectionHelpers.setField(mController, "mWebViewUpdateServiceWrapper",
                mWebViewUpdateServiceWrapper);
        doReturn(mAppInfo).when(mController).getDefaultAppInfo();
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_hasAppLabel_shouldSetAppLabelAndIcon() {
        final String appLabel = "SomeRandomAppLabel!!!";
        when(mAppInfo.loadLabel()).thenReturn(appLabel);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(appLabel);
    }

    @Test
    public void updateState_noAppLabel_shouldSetAppDefaultLabelAndNullIcon() {
        final String appLabel = null;
        when(mAppInfo.loadLabel()).thenReturn(appLabel);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.app_list_preference_none);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldBeDisabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_preferenceShouldBeEnabled() {
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
    }
}
