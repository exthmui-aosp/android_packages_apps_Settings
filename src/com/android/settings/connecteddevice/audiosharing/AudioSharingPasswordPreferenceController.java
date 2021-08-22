/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.isBroadcasting;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.nio.charset.StandardCharsets;

public class AudioSharingPasswordPreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
                AudioSharingPasswordPreference.OnDialogEventListener {

    private static final String TAG = "AudioSharingPasswordPreferenceController";
    private static final String PREF_KEY = "audio_sharing_stream_password";
    private static final String SHARED_PREF_NAME = "audio_sharing_settings";
    private static final String SHARED_PREF_KEY = "default_password";
    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private AudioSharingPasswordPreference mPreference;
    private final AudioSharingPasswordValidator mAudioSharingPasswordValidator;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AudioSharingPasswordPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBtManager = Utils.getLocalBluetoothManager(context);
        mBroadcast =
                mBtManager != null
                        ? mBtManager.getProfileManager().getLeAudioBroadcastProfile()
                        : null;
        mAudioSharingPasswordValidator = new AudioSharingPasswordValidator();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setValidator(this);
            mPreference.setIsPassword(true);
            mPreference.setDialogLayoutResource(R.layout.audio_sharing_password_dialog);
            mPreference.setOnDialogEventListener(this);
            updatePreference();
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean isTextValid(String value) {
        return mAudioSharingPasswordValidator.isTextValid(value);
    }

    @Override
    public void onBindDialogView() {
        if (mPreference == null || mBroadcast == null) {
            return;
        }
        mPreference.setEditable(!isBroadcasting(mBtManager));
        var password = mBroadcast.getBroadcastCode();
        mPreference.setChecked(isPublicBroadcast(password));
    }

    @Override
    public void onPreferenceDataChanged(@NonNull String password, boolean isPublicBroadcast) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mBroadcast == null || isBroadcasting(mBtManager)) {
                                Log.w(
                                        TAG,
                                        "onPreferenceDataChanged() changing password when"
                                                + " broadcasting or null!");
                                return;
                            }
                            boolean isCurrentPublicBroadcast =
                                    isPublicBroadcast(mBroadcast.getBroadcastCode());
                            String currentDefaultPassword = getDefaultPassword(mContext);
                            if (password.equals(currentDefaultPassword)
                                    && isCurrentPublicBroadcast == isPublicBroadcast) {
                                Log.d(TAG, "onPreferenceDataChanged() nothing changed");
                                return;
                            }
                            persistDefaultPassword(mContext, password);
                            mBroadcast.setBroadcastCode(
                                    isPublicBroadcast ? new byte[0] : password.getBytes());
                            updatePreference();
                            mMetricsFeatureProvider.action(
                                    mContext,
                                    SettingsEnums.ACTION_AUDIO_STREAM_PASSWORD_UPDATED,
                                    isPublicBroadcast ? 1 : 0);
                        });
    }

    private void updatePreference() {
        if (mBroadcast == null || mPreference == null) {
            return;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            byte[] password = mBroadcast.getBroadcastCode();
                            boolean noPassword = isPublicBroadcast(password);
                            String passwordToDisplay =
                                    noPassword
                                            ? getDefaultPassword(mContext)
                                            : new String(password, StandardCharsets.UTF_8);
                            String passwordSummary =
                                    noPassword
                                            ? mContext.getString(
                                                    R.string.audio_streams_no_password_summary)
                                            : "********";

                            AudioSharingUtils.postOnMainThread(
                                    mContext,
                                    () -> {
                                        // Check nullability to pass NullAway check
                                        if (mPreference != null) {
                                            mPreference.setText(passwordToDisplay);
                                            mPreference.setSummary(passwordSummary);
                                        }
                                    });
                        });
    }

    private static void persistDefaultPassword(Context context, String defaultPassword) {
        if (getDefaultPassword(context).equals(defaultPassword)) {
            return;
        }

        SharedPreferences sharedPref =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        if (sharedPref == null) {
            Log.w(TAG, "persistDefaultPassword(): sharedPref is empty!");
            return;
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREF_KEY, defaultPassword);
        editor.apply();
    }

    private static String getDefaultPassword(Context context) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        if (sharedPref == null) {
            Log.w(TAG, "getDefaultPassword(): sharedPref is empty!");
            return "";
        }

        String value = sharedPref.getString(SHARED_PREF_KEY, "");
        if (value != null && value.isEmpty()) {
            Log.w(TAG, "getDefaultPassword(): default password is empty!");
        }
        return value;
    }

    private static boolean isPublicBroadcast(@Nullable byte[] password) {
        return password == null || password.length == 0;
    }
}