/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricEnrollSidecar.Listener;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;

/**
 * Activity explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class FingerprintEnrollFindSensor extends BiometricEnrollBase {

    @Nullable
    private FingerprintFindSensorAnimation mAnimation;

    private FingerprintEnrollSidecar mSidecar;
    private boolean mNextClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.skip_label)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title);

        // This is an entry point for SetNewPasswordController, e.g.
        // adb shell am start -a android.app.action.SET_NEW_PASSWORD
        if (mToken == null && BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            final FingerprintManager fpm = getSystemService(FingerprintManager.class);
            fpm.generateChallenge(mUserId, (sensorId, challenge) -> {
                mToken = BiometricUtils.requestGatekeeperHat(this, getIntent(), mUserId, challenge);
                BiometricUtils.removeGatekeeperPasswordHandle(this, getIntent());

                // Put this into the intent. This is really just to work around the fact that the
                // enrollment sidecar gets the HAT from the activity's intent, rather than having
                // it passed in.
                getIntent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);

                startLookingForFingerprint();
            });
        } else if (mToken != null) {
            // HAT passed in from somewhere else, such as FingerprintEnrollIntroduction
            startLookingForFingerprint();
        } else {
            // There's something wrong with the enrollment flow, this should never happen.
            throw new IllegalStateException("HAT and GkPwHandle both missing...");
        }

        View animationView = findViewById(R.id.fingerprint_sensor_location_animation);
        if (animationView instanceof FingerprintFindSensorAnimation) {
            mAnimation = (FingerprintFindSensorAnimation) animationView;
        } else {
            mAnimation = null;
        }
    }

    protected int getContentView() {
        return R.layout.fingerprint_enroll_find_sensor;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAnimation != null) {
            mAnimation.startAnimation();
        }
    }

    private void startLookingForFingerprint() {
        mSidecar = (FingerprintEnrollSidecar) getSupportFragmentManager().findFragmentByTag(
                FingerprintEnrollEnrolling.TAG_SIDECAR);
        if (mSidecar == null) {
            mSidecar = new FingerprintEnrollSidecar();
            mSidecar.logMetrics(false);
            getSupportFragmentManager().beginTransaction()
                    .add(mSidecar, FingerprintEnrollEnrolling.TAG_SIDECAR)
                    .commitAllowingStateLoss();
        }
        mSidecar.setListener(new Listener() {
            @Override
            public void onEnrollmentProgressChange(int steps, int remaining) {
                mNextClicked = true;
                proceedToEnrolling(true /* cancelEnrollment */);
            }

            @Override
            public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            }

            @Override
            public void onEnrollmentError(int errMsgId, CharSequence errString) {
                if (mNextClicked && errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    mNextClicked = false;
                    proceedToEnrolling(false /* cancelEnrollment */);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAnimation != null) {
            mAnimation.pauseAnimation();
        }
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        return super.shouldFinishWhenBackgrounded() && !mNextClicked;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAnimation != null) {
            mAnimation.stopAnimation();
        }
    }

    protected void onSkipButtonClick(View view) {
        setResult(RESULT_SKIP);
        finish();
    }

    private void proceedToEnrolling(boolean cancelEnrollment) {
        if (mSidecar != null) {
            if (cancelEnrollment) {
                if (mSidecar.cancelEnrollment()) {
                    // Enrollment cancel requested. When the cancellation is successful,
                    // onEnrollmentError will be called with FINGERPRINT_ERROR_CANCELED, calling
                    // this again.
                    return;
                }
            }
            getSupportFragmentManager().beginTransaction().remove(mSidecar).
                    commitAllowingStateLoss();
            mSidecar = null;
            startActivityForResult(getFingerprintEnrollingIntent(), ENROLL_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                throw new IllegalStateException("Pretty sure this is dead code");
                /*
                mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                overridePendingTransition(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out);
                getIntent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startLookingForFingerprint();
                */
            } else {
                finish();
            }
        } else if (requestCode == ENROLL_REQUEST) {
            switch (resultCode) {
                case RESULT_FINISHED:
                case RESULT_SKIP:
                case RESULT_TIMEOUT:
                    setResult(resultCode);
                    finish();
                    break;
                default:
                    FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
                    int enrolled = fpm.getEnrolledFingerprints().size();
                    int max = getResources().getInteger(
                            com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
                    if (enrolled >= max) {
                        finish();
                    } else {
                        // We came back from enrolling but it wasn't completed, start again.
                        startLookingForFingerprint();
                    }
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_FIND_SENSOR;
    }
}
