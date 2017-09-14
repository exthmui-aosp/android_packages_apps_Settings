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

package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.service.settings.suggestions.ISuggestionService;
import android.util.FeatureFlagUtils;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowSystemProperties.class
        })
public class SuggestionControllerMixinTest {

    @Mock
    private Context mContext;
    @Mock
    private ISuggestionService mRemoteService;
    private Lifecycle mLifecycle;
    private SuggestionControllerMixin mMixin;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle();
        when(mContext.getApplicationContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void systemPropertySet_verifyIsEnabled() {
        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + SuggestionControllerMixin.FEATURE_FLAG, "true");
        assertThat(SuggestionControllerMixin.isEnabled()).isTrue();
    }

    @Test
    public void systemPropertyNotSet_verifyIsDisabled() {
        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + SuggestionControllerMixin.FEATURE_FLAG, "false");
        assertThat(SuggestionControllerMixin.isEnabled()).isFalse();
    }

    @Test
    public void onServiceConnected_shouldGetSuggestion() {
        mMixin = new SuggestionControllerMixin(mContext, mLifecycle);
        ReflectionHelpers.setField(mMixin, "mRemoteService", mRemoteService);
        mMixin.onServiceConnected();

        verify(mRemoteService).getSuggestions();
    }

}
