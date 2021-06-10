/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.RestrictedLockUtils;

/**
 * Utilities for things at the cross-section of biometrics and parental controls. For example,
 * determining if parental consent is required, determining which strings should be shown, etc.
 */
public class ParentalControlsUtils {

    private static final String TAG = "ParentalControlsUtils";
    private static final String TEST_ALWAYS_REQUIRE_CONSENT =
            "com.android.settings.biometrics.ParentalControlsUtils.always_require_consent";

    /**
     * Public version that enables test paths based on {@link #TEST_ALWAYS_REQUIRE_CONSENT}
     * @return non-null EnforcedAdmin if parental consent is required
     */
    public static RestrictedLockUtils.EnforcedAdmin parentConsentRequired(@NonNull Context context,
            @BiometricAuthenticator.Modality int modality) {

        final UserHandle userHandle = new UserHandle(UserHandle.myUserId());
        if (Build.IS_USERDEBUG || Build.IS_ENG) {
            final boolean testAlwaysRequireConsent = Settings.Secure.getInt(
                    context.getContentResolver(), TEST_ALWAYS_REQUIRE_CONSENT, 0) != 0;
            if (testAlwaysRequireConsent) {
                Log.d(TAG, "Requiring consent for test flow");
                return new RestrictedLockUtils.EnforcedAdmin(null /* ComponentName */, userHandle);
            }
        }

        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        return parentConsentRequiredInternal(dpm, modality, userHandle);
    }

    /**
     * Internal testable version.
     * @return non-null EnforcedAdmin if parental consent is required
     */
    @Nullable
    @VisibleForTesting
    static RestrictedLockUtils.EnforcedAdmin parentConsentRequiredInternal(
            @NonNull DevicePolicyManager dpm, @BiometricAuthenticator.Modality int modality,
            @NonNull UserHandle userHandle) {
        final ComponentName cn = dpm.getProfileOwnerOrDeviceOwnerSupervisionComponent(userHandle);
        if (cn == null) {
            return null;
        }

        final int keyguardDisabledFeatures = dpm.getKeyguardDisabledFeatures(cn);
        final boolean dpmFpDisabled = containsFlag(keyguardDisabledFeatures,
                DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);
        final boolean dpmFaceDisabled = containsFlag(keyguardDisabledFeatures,
                DevicePolicyManager.KEYGUARD_DISABLE_FACE);
        final boolean dpmIrisDisabled = containsFlag(keyguardDisabledFeatures,
                DevicePolicyManager.KEYGUARD_DISABLE_IRIS);

        final boolean consentRequired;
        if (containsFlag(modality, BiometricAuthenticator.TYPE_FINGERPRINT) && dpmFpDisabled) {
            consentRequired = true;
        } else if (containsFlag(modality, BiometricAuthenticator.TYPE_FACE) && dpmFaceDisabled) {
            consentRequired = true;
        } else if (containsFlag(modality, BiometricAuthenticator.TYPE_IRIS) && dpmIrisDisabled) {
            consentRequired = true;
        } else {
            consentRequired = false;
        }

        if (consentRequired) {
            return new RestrictedLockUtils.EnforcedAdmin(cn, userHandle);
        } else {
            return null;
        }
    }

    private static boolean containsFlag(int haystack, int needle) {
        return (haystack & needle) != 0;
    }
}
