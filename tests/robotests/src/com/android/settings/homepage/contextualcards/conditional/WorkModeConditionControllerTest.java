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

package com.android.settings.homepage.contextualcards.conditional;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.app.Activity;
import android.content.ComponentName;

import com.android.settings.Settings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;

@RunWith(SettingsRobolectricTestRunner.class)
public class WorkModeConditionControllerTest {

    @Mock
    private ConditionManager mConditionManager;
    private Activity mActivity;
    private WorkModeConditionController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.setupActivity(Activity.class));
        mController = new WorkModeConditionController(mActivity, mConditionManager);
    }

    @Test
    public void onPrimaryClick_shouldLaunchAccountsSetting() {
        final ComponentName componentName =
                new ComponentName(mActivity, Settings.AccountDashboardActivity.class);

        mController.onPrimaryClick(mActivity);

        final ShadowActivity shadowActivity = Shadow.extract(mActivity);
        assertThat(shadowActivity.getNextStartedActivity().getComponent()).isEqualTo(componentName);
    }

}
