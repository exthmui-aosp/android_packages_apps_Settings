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

package com.android.settings.biometrics.face;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.span.LinkSpan;
import com.google.android.setupdesign.template.RequireScrollMixin;

import java.util.List;

public class FaceEnrollIntroduction extends BiometricEnrollIntroduction {

    private static final String TAG = "FaceEnrollIntroduction";

    private FaceManager mFaceManager;
    private FaceFeatureProvider mFaceFeatureProvider;

    @Override
    protected void onCancelButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST)) {
            super.onCancelButtonClick(view);
        }
    }

    @Override
    protected void onSkipButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST)) {
            super.onSkipButtonClick(view);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFaceManager = Utils.getFaceManagerOrNull(this);
        mFaceFeatureProvider = FeatureFactory.getFactory(getApplicationContext())
                .getFaceFeatureProvider();

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_face_enroll_introduction_no_thanks)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        FooterButton.Builder nextButtonBuilder = new FooterButton.Builder(this)
                .setText(R.string.security_settings_face_enroll_introduction_agree)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(R.style.SudGlifButton_Primary);
        if (maxFacesEnrolled()) {
            nextButtonBuilder.setListener(this::onNextButtonClick);
            mFooterBarMixin.setPrimaryButton(nextButtonBuilder.build());
        } else {
            final FooterButton agreeButton = nextButtonBuilder.build();
            mFooterBarMixin.setPrimaryButton(agreeButton);
            final RequireScrollMixin requireScrollMixin = getLayout().getMixin(
                    RequireScrollMixin.class);
            requireScrollMixin.requireScrollWithButton(this, agreeButton,
                    R.string.security_settings_face_enroll_introduction_more,
                    this::onNextButtonClick);
        }

        final TextView footer2 = findViewById(R.id.face_enroll_introduction_footer_part_2);
        final int footer2TextResource =
                mFaceFeatureProvider.isAttentionSupported(getApplicationContext())
                        ? R.string.security_settings_face_enroll_introduction_footer_part_2
                        : R.string.security_settings_face_settings_footer_attention_not_supported;
        footer2.setText(footer2TextResource);

        // This path is an entry point for SetNewPasswordController, e.g.
        // adb shell am start -a android.app.action.SET_NEW_PASSWORD
        if (mToken == null && BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            mFooterBarMixin.getPrimaryButton().setEnabled(false);
            // We either block on generateChallenge, or need to gray out the "next" button until
            // the challenge is ready. Let's just do this for now.
            mFaceManager.generateChallenge((sensorId, challenge) -> {
                mToken = BiometricUtils.requestGatekeeperHat(this, getIntent(), mUserId, challenge);
                mSensorId = sensorId;
                mChallenge = challenge;
                mFooterBarMixin.getPrimaryButton().setEnabled(true);
            });
        }
    }

    @Override
    protected boolean isDisabledByAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                this, DevicePolicyManager.KEYGUARD_DISABLE_FACE, mUserId) != null;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.face_enroll_introduction;
    }

    @Override
    protected int getHeaderResDisabledByAdmin() {
        return R.string.security_settings_face_enroll_introduction_title_unlock_disabled;
    }

    @Override
    protected int getHeaderResDefault() {
        return R.string.security_settings_face_enroll_introduction_title;
    }

    @Override
    protected int getDescriptionResDisabledByAdmin() {
        return R.string.security_settings_face_enroll_introduction_message_unlock_disabled;
    }

    @Override
    protected FooterButton getCancelButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getSecondaryButton();
        }
        return null;
    }

    @Override
    protected FooterButton getNextButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getPrimaryButton();
        }
        return null;
    }

    @Override
    protected TextView getErrorTextView() {
        return findViewById(R.id.error_text);
    }

    private boolean maxFacesEnrolled() {
        if (mFaceManager != null) {
            final List<FaceSensorPropertiesInternal> props =
                    mFaceManager.getSensorPropertiesInternal();
            // This will need to be updated for devices with multiple face sensors.
            final int max = props.get(0).maxEnrollmentsPerUser;
            final int numEnrolledFaces = mFaceManager.getEnrolledFaces(mUserId).size();
            return numEnrolledFaces >= max;
        } else {
            return false;
        }
    }

    //TODO: Refactor this to something that conveys it is used for getting a string ID.
    @Override
    protected int checkMaxEnrolled() {
        if (mFaceManager != null) {
            if (maxFacesEnrolled()) {
                return R.string.face_intro_error_max;
            }
        } else {
            return R.string.face_intro_error_unknown;
        }
        return 0;
    }

    @Override
    protected void getChallenge(GenerateChallengeCallback callback) {
        mFaceManager = Utils.getFaceManagerOrNull(this);
        if (mFaceManager == null) {
            callback.onChallengeGenerated(0, 0L);
            return;
        }
        mFaceManager.generateChallenge(callback::onChallengeGenerated);
    }

    @Override
    protected String getExtraKeyForBiometric() {
        return ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
    }

    @Override
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, FaceEnrollEducation.class);
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected int getConfirmLockTitleResId() {
        return R.string.security_settings_face_preference_title;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_INTRO;
    }

    @Override
    public void onClick(LinkSpan span) {
        // TODO(b/110906762)
    }
}
