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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class SwipeUpPreferenceControllerTest {

    private Context mContext;
    private ShadowPackageManager mPackageManager;
    private SwipeUpPreferenceController mController;

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String KEY_SWIPE_UP = "gesture_swipe_up";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new SwipeUpPreferenceController(mContext, KEY_SWIPE_UP);
    }

    @Test
    public void testIsGestureAvailable_matchingServiceExists_shouldReturnTrue() {
        final ComponentName recentsComponentName = ComponentName.unflattenFromString(
                mContext.getString(com.android.internal.R.string.config_recentsComponentName));
        final Intent quickStepIntent = new Intent(ACTION_QUICKSTEP)
                .setPackage(recentsComponentName.getPackageName());
        mPackageManager.addResolveInfoForIntent(quickStepIntent, new ResolveInfo());

        assertThat(SwipeUpPreferenceController.isGestureAvailable(mContext)).isTrue();
    }

    @Test
    public void testIsGestureAvailable_noMatchingServiceExists_shouldReturnFalse() {
        assertThat(SwipeUpPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsChecked_configIsSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        mController.setChecked(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_configIsNotSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        mController.setChecked(false);
        assertThat(mController.isChecked()).isFalse();
    }
}
