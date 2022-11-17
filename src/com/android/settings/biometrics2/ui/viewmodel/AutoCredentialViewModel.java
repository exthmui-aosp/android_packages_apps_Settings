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

package com.android.settings.biometrics2.ui.viewmodel;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_SENSOR_ID;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_GK_PW_HANDLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.annotation.IntDef;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockSettingsHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * AutoCredentialViewModel which uses CredentialModel to determine next actions for activity, like
 * start ChooseLockActivity, start ConfirmLockActivity, GenerateCredential, or do nothing.
 */
public class AutoCredentialViewModel extends AndroidViewModel implements DefaultLifecycleObserver {

    private static final String TAG = "AutoCredentialViewModel";
    private static final boolean DEBUG = true;

    /**
     * Need activity to run choose lock
     */
    public static final int CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK = 1;

    /**
     * Need activity to run confirm lock
     */
    public static final int CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK = 2;

    /**
     * Fail to use challenge from hardware generateChallenge(), shall finish activity with proper
     * error code
     */
    public static final int CREDENTIAL_FAIL_DURING_GENERATE_CHALLENGE = 3;

    @IntDef(prefix = { "CREDENTIAL_" }, value = {
            CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK,
            CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK,
            CREDENTIAL_FAIL_DURING_GENERATE_CHALLENGE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CredentialAction {}

    /**
     * Generic callback for FingerprintManager#generateChallenge or FaceManager#generateChallenge
     */
    public interface GenerateChallengeCallback {
        /**
         * Generic generateChallenge method for FingerprintManager or FaceManager
         */
        void onChallengeGenerated(int sensorId, int userId, long challenge);
    }

    /**
     * A generic interface class for calling different generateChallenge from FingerprintManager or
     * FaceManager
     */
    public interface ChallengeGenerator {
        /**
         * Get callback that will be called later after challenge generated
         */
        @Nullable
        GenerateChallengeCallback getCallback();

        /**
         * Set callback that will be called later after challenge generated
         */
        void setCallback(@Nullable GenerateChallengeCallback callback);

        /**
         * Method for generating challenge from FingerprintManager or FaceManager
         */
        void generateChallenge(int userId);
    }

    /**
     * Used to generate challenge through FingerprintRepository
     */
    public static class FingerprintChallengeGenerator implements ChallengeGenerator {

        private static final String TAG = "FingerprintChallengeGenerator";

        @NonNull
        private final FingerprintRepository mFingerprintRepository;

        @Nullable
        private GenerateChallengeCallback mCallback = null;

        public FingerprintChallengeGenerator(@NonNull FingerprintRepository fingerprintRepository) {
            mFingerprintRepository = fingerprintRepository;
        }

        @Nullable
        @Override
        public GenerateChallengeCallback getCallback() {
            return mCallback;
        }

        @Override
        public void setCallback(@Nullable GenerateChallengeCallback callback) {
            mCallback = callback;
        }

        @Override
        public void generateChallenge(int userId) {
            final GenerateChallengeCallback callback = mCallback;
            if (callback == null) {
                Log.e(TAG, "generateChallenge, null callback");
                return;
            }
            mFingerprintRepository.generateChallenge(userId, callback::onChallengeGenerated);
        }
    }


    @NonNull private final LockPatternUtils mLockPatternUtils;
    @NonNull private final ChallengeGenerator mChallengeGenerator;
    private CredentialModel mCredentialModel = null;
    @NonNull private final MutableLiveData<Integer> mActionLiveData =
            new MutableLiveData<>();

    public AutoCredentialViewModel(
            @NonNull Application application,
            @NonNull LockPatternUtils lockPatternUtils,
            @NonNull ChallengeGenerator challengeGenerator) {
        super(application);
        mLockPatternUtils = lockPatternUtils;
        mChallengeGenerator = challengeGenerator;
    }

    public void setCredentialModel(@NonNull CredentialModel credentialModel) {
        mCredentialModel = credentialModel;
    }

    /**
     * Observe ActionLiveData for actions about choosing lock, confirming lock, or finishing
     * activity
     */
    @NonNull
    public LiveData<Integer> getActionLiveData() {
        return mActionLiveData;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        checkCredential();
    }

    /**
     * Check credential status for biometric enrollment.
     */
    private void checkCredential() {
        if (isValidCredential()) {
            return;
        }
        final long gkPwHandle = mCredentialModel.getGkPwHandle();
        if (isUnspecifiedPassword()) {
            mActionLiveData.postValue(CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK);
        } else if (CredentialModel.isValidGkPwHandle(gkPwHandle)) {
            generateChallenge(gkPwHandle);
        } else {
            mActionLiveData.postValue(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
        }
    }

    private void generateChallenge(long gkPwHandle) {
        mChallengeGenerator.setCallback((sensorId, userId, challenge) -> {
            mCredentialModel.setSensorId(sensorId);
            mCredentialModel.setChallenge(challenge);
            try {
                final byte[] newToken = requestGatekeeperHat(gkPwHandle, challenge, userId);
                mCredentialModel.setToken(newToken);
            } catch (IllegalStateException e) {
                Log.e(TAG, "generateChallenge, IllegalStateException", e);
                mActionLiveData.postValue(CREDENTIAL_FAIL_DURING_GENERATE_CHALLENGE);
                return;
            }

            mLockPatternUtils.removeGatekeeperPasswordHandle(gkPwHandle);
            mCredentialModel.clearGkPwHandle();

            if (DEBUG) {
                Log.d(TAG, "generateChallenge " + mCredentialModel);
            }

            // Check credential again
            if (!isValidCredential()) {
                Log.w(TAG, "generateChallenge, invalid Credential");
                mActionLiveData.postValue(CREDENTIAL_FAIL_DURING_GENERATE_CHALLENGE);
            }
        });
        mChallengeGenerator.generateChallenge(getUserId());
    }

    private boolean isValidCredential() {
        return !isUnspecifiedPassword()
                && CredentialModel.isValidToken(mCredentialModel.getToken());
    }

    private boolean isUnspecifiedPassword() {
        return mLockPatternUtils.getActivePasswordQuality(getUserId())
                == PASSWORD_QUALITY_UNSPECIFIED;
    }

    /**
     * Handle activity result from ChooseLockGeneric, ConfirmLockPassword, or ConfirmLockPattern
     * @param isChooseLock true if result is coming from ChooseLockGeneric. False if result is
     *                     coming from ConfirmLockPassword or ConfirmLockPattern
     * @param result activity result
     * @return if it is a valid result
     */
    public boolean checkNewCredentialFromActivityResult(boolean isChooseLock,
            @NonNull ActivityResult result) {
        if ((isChooseLock && result.getResultCode() == ChooseLockPattern.RESULT_FINISHED)
                || (!isChooseLock && result.getResultCode() == Activity.RESULT_OK)) {
            final Intent data = result.getData();
            if (data != null) {
                final long gkPwHandle = result.getData().getLongExtra(
                        EXTRA_KEY_GK_PW_HANDLE, INVALID_GK_PW_HANDLE);
                generateChallenge(gkPwHandle);
                return true;
            }
        }
        return false;
    }

    /**
     * Get userId for this credential
     */
    public int getUserId() {
        return mCredentialModel.getUserId();
    }

    @Nullable
    private byte[] requestGatekeeperHat(long gkPwHandle, long challenge, int userId)
            throws IllegalStateException {
        final VerifyCredentialResponse response = mLockPatternUtils
                .verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId);
        if (!response.isMatched()) {
            throw new IllegalStateException("Unable to request Gatekeeper HAT");
        }
        return response.getGatekeeperHAT();
    }

    /**
     * Get Credential bundle which will be used to launch next activity.
     */
    @NonNull
    public Bundle getCredentialBundle() {
        final Bundle retBundle = new Bundle();
        final long gkPwHandle = mCredentialModel.getGkPwHandle();
        if (CredentialModel.isValidGkPwHandle(gkPwHandle)) {
            retBundle.putLong(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        }
        final byte[] token = mCredentialModel.getToken();
        if (CredentialModel.isValidToken(token)) {
            retBundle.putByteArray(EXTRA_KEY_CHALLENGE_TOKEN, token);
        }
        final int userId = getUserId();
        if (CredentialModel.isValidUserId(userId)) {
            retBundle.putInt(Intent.EXTRA_USER_ID, userId);
        }
        retBundle.putLong(EXTRA_KEY_CHALLENGE, mCredentialModel.getChallenge());
        retBundle.putInt(EXTRA_KEY_SENSOR_ID, mCredentialModel.getSensorId());
        return retBundle;
    }

    /**
     * Get Intent for choosing lock
     */
    @NonNull
    public Intent getChooseLockIntent(@NonNull Context context, boolean isSuw,
            @NonNull Bundle suwExtras) {
        final Intent intent = BiometricUtils.getChooseLockIntent(context, isSuw,
                suwExtras);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
                true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);

        final int userId = getUserId();
        if (CredentialModel.isValidUserId(userId)) {
            intent.putExtra(Intent.EXTRA_USER_ID, userId);
        }
        return intent;
    }

    /**
     * Get ConfirmLockLauncher
     */
    @NonNull
    public ChooseLockSettingsHelper getConfirmLockLauncher(@NonNull Activity activity,
            int requestCode, @NonNull String title) {
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(requestCode)
                .setTitle(title)
                .setRequestGatekeeperPasswordHandle(true)
                .setForegroundOnly(true)
                .setReturnCredentials(true);

        final int userId = mCredentialModel.getUserId();
        if (CredentialModel.isValidUserId(userId)) {
            builder.setUserId(userId);
        }
        return builder.build();
    }

}
