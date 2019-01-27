/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import static android.Manifest.permission.GET_AND_REQUEST_SCREEN_LOCK_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.testutils.shadow.ShadowPasswordUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class SetNewPasswordActivityTest {

    private static final String APP_LABEL = "label";

    private int mProvisioned;

    @Before
    public void setUp() {
        mProvisioned = Settings.Global.getInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, mProvisioned);
        ShadowPasswordUtils.reset();
    }

    @Test
    public void testChooseLockGeneric() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        activity.launchChooseLock(new Bundle());
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;

        assertThat(intent.getComponent())
                .isEqualTo(new ComponentName(activity, ChooseLockGeneric.class));
    }

    @Test
    public void testSetupChooseLockGeneric() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        activity.launchChooseLock(new Bundle());
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;

        assertThat(intent.getComponent())
                .isEqualTo(new ComponentName(activity, SetupChooseLockGeneric.class));
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraWithoutPermission() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getNextStartedActivityForResult()).isNull();
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraWithPermission() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.addGrantedPermission(GET_AND_REQUEST_SCREEN_LOCK_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isTrue();
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE))
                .isEqualTo(PASSWORD_COMPLEXITY_HIGH);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraInvalidValue() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.addGrantedPermission(GET_AND_REQUEST_SCREEN_LOCK_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, -1);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraNoneComplexity() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.addGrantedPermission(GET_AND_REQUEST_SCREEN_LOCK_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_NONE);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewParentProfilePasswordExtraWithPermission() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.addGrantedPermission(GET_AND_REQUEST_SCREEN_LOCK_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
    }
}
