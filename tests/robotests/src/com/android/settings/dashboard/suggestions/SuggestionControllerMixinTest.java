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

import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.LoaderManager;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowSuggestionController.class
        })
public class SuggestionControllerMixinTest {

    @Mock
    private SuggestionControllerMixin.SuggestionControllerHost mHost;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private SuggestionControllerMixin mMixin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
    }

    @After
    public void tearDown() {
        ShadowSuggestionController.reset();
    }

    @Test
    public void goThroughLifecycle_onStartStop_shouldStartStopController() {
        mMixin = new SuggestionControllerMixin(mContext, mHost, mLifecycle);

        mLifecycle.handleLifecycleEvent(ON_START);
        assertThat(ShadowSuggestionController.sStartCalled).isTrue();

        mLifecycle.handleLifecycleEvent(ON_STOP);
        assertThat(ShadowSuggestionController.sStopCalled).isTrue();
    }

    @Test
    public void onServiceConnected_shouldGetSuggestion() {
        final LoaderManager loaderManager = mock(LoaderManager.class);
        when(mHost.getLoaderManager()).thenReturn(loaderManager);

        mMixin = new SuggestionControllerMixin(mContext, mHost, mLifecycle);
        mMixin.onServiceConnected();

        verify(loaderManager).restartLoader(SuggestionLoader.LOADER_ID_SUGGESTIONS,
                null /* args */, mMixin /* callback */);
    }

    @Test
    public void onServiceConnected_hostNotAttached_shouldDoNothing() {
        when(mHost.getLoaderManager()).thenReturn(null);

        mMixin = new SuggestionControllerMixin(mContext, mHost, mLifecycle);
        mMixin.onServiceConnected();

        verify(mHost).getLoaderManager();
    }

    @Test
    public void onServiceDisconnected_hostNotAttached_shouldDoNothing() {
        when(mHost.getLoaderManager()).thenReturn(null);

        mMixin = new SuggestionControllerMixin(mContext, mHost, mLifecycle);
        mMixin.onServiceDisconnected();

        verify(mHost).getLoaderManager();
    }

    @Test
    public void doneLoadingg_shouldSetSuggestionLoaded() {
        mMixin = new SuggestionControllerMixin(mContext, mHost, mLifecycle);

        mMixin.onLoadFinished(mock(SuggestionLoader.class), null);

        assertThat(mMixin.isSuggestionLoaded()).isTrue();

        mMixin.onLoaderReset(mock(SuggestionLoader.class));

        assertThat(mMixin.isSuggestionLoaded()).isFalse();
    }
}
