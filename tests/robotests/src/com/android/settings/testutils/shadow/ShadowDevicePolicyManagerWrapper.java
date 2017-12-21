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

package com.android.settings.testutils.shadow;

import android.annotation.UserIdInt;
import android.content.ComponentName;

import com.android.settings.wrapper.DevicePolicyManagerWrapper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

/**
 * Shadow for {@link DevicePolicyManagerWrapper} to allow stubbing hidden methods.
 */
@Implements(DevicePolicyManagerWrapper.class)
public class ShadowDevicePolicyManagerWrapper {
    private static ComponentName deviceOComponentName = null;
    private static int deviceOwnerUserId = -1;
    private static final Map<Integer, Long> profileTimeouts = new HashMap<>();

    @Implementation
    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return deviceOComponentName;
    }

    @Implementation
    public int getDeviceOwnerUserId() {
        return deviceOwnerUserId;
    }

    @Implementation
    public long getMaximumTimeToLock(ComponentName admin, @UserIdInt int userHandle) {
        return profileTimeouts.getOrDefault(userHandle, 0L);
    }

    public static void setDeviceOComponentName(ComponentName deviceOComponentName) {
        ShadowDevicePolicyManagerWrapper.deviceOComponentName = deviceOComponentName;
    }

    public static void setDeviceOwnerUserId(int deviceOwnerUserId) {
        ShadowDevicePolicyManagerWrapper.deviceOwnerUserId = deviceOwnerUserId;
    }

    public static void setMaximumTimeToLock(@UserIdInt int userHandle, Long timeout) {
        profileTimeouts.put(userHandle, timeout);
    }
}
