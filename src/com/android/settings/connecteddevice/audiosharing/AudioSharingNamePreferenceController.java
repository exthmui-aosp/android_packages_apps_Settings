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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.widget.ValidatedEditTextPreference;

public class AudioSharingNamePreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
                Preference.OnPreferenceChangeListener,
                DefaultLifecycleObserver {

    private static final String TAG = "AudioSharingNamePreferenceController";

    private static final String PREF_KEY = "audio_sharing_stream_name";

    protected Preference mPreference;

    private AudioSharingNameTextValidator mAudioSharingNameTextValidator;

    public AudioSharingNamePreferenceController(Context context) {
        super(context, PREF_KEY);
        mAudioSharingNameTextValidator = new AudioSharingNameTextValidator();
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableLeAudioSharing() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO: update broadcast when name is changed.
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isTextValid(String value) {
        return mAudioSharingNameTextValidator.isTextValid(value);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // TODO
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // TODO
    }
}
