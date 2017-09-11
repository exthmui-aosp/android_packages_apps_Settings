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

package android.util;

import android.os.SystemProperties;
import android.text.TextUtils;

/**
 * This class is only needed to get around Robolectric issue.
 */
public class FeatureFlagUtils {
    public static final String FFLAG_PREFIX = "sys.fflag.";
    public static final String FFLAG_OVERRIDE_PREFIX = FFLAG_PREFIX + "override.";

    /**
     * Whether or not a flag is enabled.
     *
     * @param feature the flag name
     * @return true if the flag is enabled (either by default in system, or override by user)
     */
    public static boolean isEnabled(String feature) {
        // Tries to get feature flag from system property.
        // Step 1: check if feature flag has any override. Flag name: sys.fflag.override.<feature>
        String value = SystemProperties.get(FFLAG_OVERRIDE_PREFIX + feature);
        if (!TextUtils.isEmpty(value)) {
            return Boolean.parseBoolean(value);
        }
        // Step 2: check if feature flag has any default value. Flag name: sys.fflag.<feature>
        value = SystemProperties.get(FFLAG_PREFIX + feature);
        return Boolean.parseBoolean(value);
    }
}
