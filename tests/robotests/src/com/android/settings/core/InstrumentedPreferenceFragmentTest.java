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
 * limitations under the License
 */

package com.android.settings.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.FeatureFlagUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settings.TestConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows = {
        SettingsShadowSystemProperties.class
})
public class InstrumentedPreferenceFragmentTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private Activity mActivity;

    private InstrumentedPreferenceFragmentTestable mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mPreferenceManager.getPreferenceScreen()).thenReturn(mScreen);

        mFragment = spy(new InstrumentedPreferenceFragmentTestable());
        ReflectionHelpers.setField(mFragment, "mPreferenceManager", mPreferenceManager);
    }

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void onCreatePreferences_preferenceScreenTitleFeatureOff_shouldNotAddPreference() {
        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + mFragment.FEATURE_FLAG_USE_PREFERENCE_SCREEN_TITLE,
                "false");

        mFragment.onCreatePreferences(Bundle.EMPTY, null /* rootKey */);

        verify(mFragment, never()).addPreferencesFromResource(anyInt());
    }

    @Test
    public void onCreatePreferences_noPreferenceScreenResId_shouldNotAddPreference() {
        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + mFragment.FEATURE_FLAG_USE_PREFERENCE_SCREEN_TITLE,
                "true");

        mFragment.onCreatePreferences(Bundle.EMPTY, null /* rootKey */);

        verify(mFragment, never()).addPreferencesFromResource(anyInt());
    }

    @Test
    public void onCreatePreferences_gotPreferenceScreenResId_shouldAddPreferences() {
        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + mFragment.FEATURE_FLAG_USE_PREFERENCE_SCREEN_TITLE,
                "true");
        mFragment.setPreferenceScreenResId(R.xml.screen_pinning_settings);
        when(mFragment.getActivity()).thenReturn(mActivity);

        mFragment.onCreatePreferences(Bundle.EMPTY, null /* rootKey */);

        verify(mFragment).addPreferencesFromResource(R.xml.screen_pinning_settings);
        verify(mActivity, never()).setTitle(any());
    }

    @Test
    public void onCreatePreferences_gotPrefScreenResIdAndTitle_shouldAddPreferencesAndSetTitle() {
        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + mFragment.FEATURE_FLAG_USE_PREFERENCE_SCREEN_TITLE,
                "true");
        mFragment.setPreferenceScreenResId(R.xml.screen_pinning_settings);
        when(mFragment.getActivity()).thenReturn(mActivity);
        final CharSequence title = "Test Title";
        when(mScreen.getTitle()).thenReturn(title);

        mFragment.onCreatePreferences(Bundle.EMPTY, null /* rootKey */);

        verify(mFragment).addPreferencesFromResource(R.xml.screen_pinning_settings);
        verify(mActivity).setTitle(title);
    }

    private static class InstrumentedPreferenceFragmentTestable
            extends InstrumentedPreferenceFragment {

        private int mScreenId = -1;

        public InstrumentedPreferenceFragmentTestable() {
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.VIEW_UNKNOWN;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return mScreenId;
        }

        public void setPreferenceScreenResId(int id) {
            mScreenId = id;
        }
    }
}
