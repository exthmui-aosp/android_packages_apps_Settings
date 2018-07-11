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
 * limitations under the License
 */

package com.android.settings.biometrics.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.setupwizardlib.span.LinkSpan;

public class FingerprintEnrollIntroduction extends BiometricEnrollIntroduction {

    private static final String TAG = "FingerprintIntro";

    private FingerprintManager mFingerprintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(this);
    }

    @Override
    protected boolean isDisabledByAdmin() {
        return RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                this, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId) != null;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fingerprint_enroll_introduction;
    }

    @Override
    protected int getHeaderResDisabledByAdmin() {
        return R.string.security_settings_fingerprint_enroll_introduction_title_unlock_disabled;
    }

    @Override
    protected int getHeaderResDefault() {
        return R.string.security_settings_fingerprint_enroll_introduction_title;
    }

    @Override
    protected int getDescriptionResDisabledByAdmin() {
        return R.string.security_settings_fingerprint_enroll_introduction_message_unlock_disabled;
    }

    @Override
    protected Button getCancelButton() {
        return findViewById(R.id.fingerprint_cancel_button);
    }

    @Override
    protected Button getNextButton() {
        return findViewById(R.id.fingerprint_next_button);
    }

    @Override
    protected TextView getErrorTextView() {
        return findViewById(R.id.error_text);
    }

    @Override
    protected int checkMaxEnrolled() {
        if (mFingerprintManager != null) {
            final int max = getResources().getInteger(
                    com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
            final int numEnrolledFingerprints =
                    mFingerprintManager.getEnrolledFingerprints(mUserId).size();
            if (numEnrolledFingerprints >= max) {
                return R.string.fingerprint_intro_error_max;
            }
        } else {
            return R.string.fingerprint_intro_error_unknown;
        }
        return 0;
    }

    @Override
    protected long getChallenge() {
        if (mFingerprintManager == null) {
            return 0;
        }
        return mFingerprintManager.preEnroll();
    }

    @Override
    protected String getExtraKeyForBiometric() {
        return ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
    }

    @Override
    protected Intent getEnrollingIntent() {
        return new Intent(this, FingerprintEnrollFindSensor.class);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FINGERPRINT_ENROLL_INTRO;
    }

    @Override
    public void onClick(LinkSpan span) {
        if ("url".equals(span.getId())) {
            String url = getString(R.string.help_url_fingerprint);
            Intent intent = HelpUtils.getHelpIntent(this, url, getClass().getName());
            if (intent == null) {
                Log.w(TAG, "Null help intent.");
                return;
            }
            try {
                // This needs to be startActivityForResult even though we do not care about the
                // actual result because the help app needs to know about who invoked it.
                startActivityForResult(intent, LEARN_MORE_REQUEST);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + e);
            }
        }
    }
}
