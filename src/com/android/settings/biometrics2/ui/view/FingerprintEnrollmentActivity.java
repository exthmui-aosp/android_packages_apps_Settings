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

package com.android.settings.biometrics2.ui.view;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.CHALLENGE_GENERATOR;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_DURING_GENERATE_CHALLENGE;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFindSensor;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollEnrolling;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.factory.BiometricsFragmentFactory;
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.FingerprintChallengeGenerator;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Fingerprint enrollment activity implementation
 */
public class FingerprintEnrollmentActivity extends FragmentActivity {

    private static final String TAG = "FingerprintEnrollmentActivity";

    protected static final int LAUNCH_CONFIRM_LOCK_ACTIVITY = 1;

    private FingerprintEnrollmentViewModel mViewModel;
    private AutoCredentialViewModel mAutoCredentialViewModel;
    private ActivityResultLauncher<Intent> mNextActivityLauncher;
    private ActivityResultLauncher<Intent> mChooseLockLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNextActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (it) -> mViewModel.onContinueEnrollActivityResult(
                        it,
                        mAutoCredentialViewModel.getUserId())
        );
        mChooseLockLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (it) -> onChooseOrConfirmLockResult(true, it)
        );

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        mViewModel = viewModelProvider.get(FingerprintEnrollmentViewModel.class);
        mViewModel.setRequest(new EnrollmentRequest(getIntent(), getApplicationContext()));
        mViewModel.setSavedInstanceState(savedInstanceState);
        getLifecycle().addObserver(mViewModel);

        mAutoCredentialViewModel = viewModelProvider.get(AutoCredentialViewModel.class);
        mAutoCredentialViewModel.setCredentialModel(new CredentialModel(getIntent(),
                SystemClock.elapsedRealtimeClock()));
        getLifecycle().addObserver(mAutoCredentialViewModel);

        mViewModel.getSetResultLiveData().observe(this, this::onSetActivityResult);
        mAutoCredentialViewModel.getActionLiveData().observe(this, this::onCredentialAction);

        // Theme
        setTheme(mViewModel.getRequest().getTheme());
        ThemeHelper.trySetDynamicColor(this);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // fragment
        setContentView(R.layout.biometric_enrollment_container);
        final FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.setFragmentFactory(
                new BiometricsFragmentFactory(getApplication(), viewModelProvider));

        final FingerprintEnrollIntroViewModel fingerprintEnrollIntroViewModel =
                viewModelProvider.get(FingerprintEnrollIntroViewModel.class);
        fingerprintEnrollIntroViewModel.setEnrollmentRequest(mViewModel.getRequest());
        fingerprintEnrollIntroViewModel.setUserId(mAutoCredentialViewModel.getUserId());
        fingerprintEnrollIntroViewModel.getActionLiveData().observe(
                this, this::observeIntroAction);
        final String tag = "FingerprintEnrollIntroFragment";
        fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, FingerprintEnrollIntroFragment.class, null, tag)
                .commit();
    }

    private void onSetActivityResult(@NonNull ActivityResult result) {
        setResult(mViewModel.getRequest().isAfterSuwOrSuwSuggestedAction()
                        ? RESULT_CANCELED
                        : result.getResultCode(),
                result.getData());
        finish();
    }

    private void onCredentialAction(@NonNull Integer action) {
        switch (action) {
            case CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK: {
                final Intent intent = mAutoCredentialViewModel.getChooseLockIntent(this,
                        mViewModel.getRequest().isSuw(), mViewModel.getRequest().getSuwExtras());
                if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "chooseLock, fail to set isWaiting flag to true");
                }
                mChooseLockLauncher.launch(intent);
                return;
            }
            case CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK: {
                final boolean launched = mAutoCredentialViewModel.getConfirmLockLauncher(
                        this,
                        LAUNCH_CONFIRM_LOCK_ACTIVITY,
                        getString(R.string.security_settings_fingerprint_preference_title)
                ).launch();
                if (!launched) {
                    // This shouldn't happen, as we should only end up at this step if a lock thingy
                    // is already set.
                    Log.e(TAG, "confirmLock, launched is true");
                    finish();
                } else if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "confirmLock, fail to set isWaiting flag to true");
                }
                return;
            }
            case CREDENTIAL_FAIL_DURING_GENERATE_CHALLENGE: {
                Log.w(TAG, "observeCredentialLiveData, finish with action:" + action);
                if (mViewModel.getRequest().isAfterSuwOrSuwSuggestedAction()) {
                    setResult(Activity.RESULT_CANCELED);
                }
                finish();
            }
        }
    }

    private void onChooseOrConfirmLockResult(boolean isChooseLock,
            @NonNull ActivityResult activityResult) {
        if (!mViewModel.isWaitingActivityResult().compareAndSet(true, false)) {
            Log.w(TAG, "isChooseLock:" + isChooseLock + ", fail to unset waiting flag");
        }
        if (mAutoCredentialViewModel.checkNewCredentialFromActivityResult(
                isChooseLock, activityResult)) {
            overridePendingTransition(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out);
        }
    }

    private void observeIntroAction(@NonNull Integer action) {
        switch (action) {
            case FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_FINISHED, null));
                return;
            }
            case FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_SKIP, null));
                return;
            }
            case FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL: {
                final boolean isSuw = mViewModel.getRequest().isSuw();
                if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "startNext, isSuw:" + isSuw + ", fail to set isWaiting flag");
                }
                final Intent intent = new Intent(this, isSuw
                        ? SetupFingerprintEnrollEnrolling.class
                        : FingerprintEnrollFindSensor.class);
                intent.putExtras(mAutoCredentialViewModel.getCredentialBundle());
                intent.putExtras(mViewModel.getNextActivityBaseIntentExtras());
                mNextActivityLauncher.launch(intent);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewModel.checkFinishActivityDuringOnPause(isFinishing(), isChangingConfigurations());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == LAUNCH_CONFIRM_LOCK_ACTIVITY) {
            onChooseOrConfirmLockResult(false, new ActivityResult(resultCode, data));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        final Application application =
                super.getDefaultViewModelCreationExtras().get(APPLICATION_KEY);
        final MutableCreationExtras ret = new MutableCreationExtras();
        ret.set(APPLICATION_KEY, application);
        final FingerprintRepository repository = FeatureFactory.getFactory(application)
                .getBiometricsRepositoryProvider().getFingerprintRepository(application);
        ret.set(CHALLENGE_GENERATOR, new FingerprintChallengeGenerator(repository));
        return ret;
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        return new BiometricsViewModelFactory();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setStatusBarColor(getBackgroundColor());
    }

    @ColorInt
    private int getBackgroundColor() {
        final ColorStateList stateList = Utils.getColorAttr(this, android.R.attr.windowBackground);
        return stateList != null ? stateList.getDefaultColor() : Color.TRANSPARENT;
    }

    @Override
    protected void onDestroy() {
        getLifecycle().removeObserver(mViewModel);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.onSaveInstanceState(outState);
    }
}
