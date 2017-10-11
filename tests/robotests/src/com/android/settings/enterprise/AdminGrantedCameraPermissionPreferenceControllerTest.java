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

package com.android.settings.enterprise;

import android.Manifest;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link AdminGrantedCameraPermissionPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class AdminGrantedCameraPermissionPreferenceControllerTest extends
        AdminGrantedPermissionsPreferenceControllerTestBase {

    public AdminGrantedCameraPermissionPreferenceControllerTest() {
        super("enterprise_privacy_number_camera_access_packages",
                new String[] {Manifest.permission.CAMERA},
                Manifest.permission_group.CAMERA);
    }

    @Override
    protected AdminGrantedPermissionsPreferenceControllerBase createController(boolean async) {
        return new AdminGrantedCameraPermissionPreferenceController(mContext,null /* lifecycle */,
                async);
    }
}
