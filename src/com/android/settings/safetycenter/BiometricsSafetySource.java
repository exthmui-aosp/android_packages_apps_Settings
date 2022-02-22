/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.safetycenter;

import android.content.Context;

/** Combined Biometrics Safety Source for Safety Center. */
public final class BiometricsSafetySource {

    public static final String SAFETY_SOURCE_ID = "Biometrics";

    private BiometricsSafetySource() {}

    /** Sends biometric safety data to Safety Center. */
    public static void sendSafetyData(Context context) {
        if (!SafetyCenterStatusHolder.get().isEnabled(context)) {
            return;
        }

        // TODO(b/215517420): Send biometric data to Safety Center if there are biometrics available
        // on this device.
    }
}
