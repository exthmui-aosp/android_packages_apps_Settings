/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.app;

import static android.provider.Settings.Secure.BUBBLE_IMPORTANT_CONVERSATIONS;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

public class ConversationImportantPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "ConvoImpPC";
    private static final String KEY = "important";
    private final NotificationSettings.DependentFieldListener mDependentFieldListener;

    public ConversationImportantPreferenceController(Context context,
            NotificationBackend backend, NotificationSettings.DependentFieldListener listener) {
        super(context, backend);
        mDependentFieldListener = listener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mAppRow == null || mChannel == null) {
            return false;
        }
        return true;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setDisabledByAdmin(mAdmin);
            pref.setChecked(mChannel.isImportantConversation());
            pref.setEnabled(!pref.isDisabledByAdmin());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel == null) {
            return false;
        }
        final boolean value = (Boolean) newValue;
        mChannel.setImportantConversation(value);
        if (value && bubbleImportantConversations()) {
            mChannel.setAllowBubbles(true);
            mDependentFieldListener.onFieldValueChanged();
        }
        saveChannel();

        return true;
    }

    private boolean bubbleImportantConversations() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                BUBBLE_IMPORTANT_CONVERSATIONS, 1) == 1;
    }
}
