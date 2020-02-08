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

package com.android.settings.notification.zen;

import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModePriorityConversationsPreferenceController
        extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_conversations";
    private final ZenModeBackend mBackend;
    private ListPreference mPreference;

    public ZenModePriorityConversationsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
        mBackend = ZenModeBackend.getInstance(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateValue(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object selectedContactsFrom) {
        mBackend.saveConversationSenders(Integer.parseInt(selectedContactsFrom.toString()));
        updateValue(preference);
        return true;
    }

    private void updateValue(Preference preference) {
        mPreference = (ListPreference) preference;
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                mPreference.setEnabled(false);
                mPreference.setValue(String.valueOf(CONVERSATION_SENDERS_NONE));
                mPreference.setSummary(mBackend.getAlarmsTotalSilencePeopleSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(mBackend.getConversationSummary());
                int senders = mBackend.getPriorityConversationSenders();

                switch (senders) {
                    case CONVERSATION_SENDERS_NONE:
                        mPreference.setValue(String.valueOf(CONVERSATION_SENDERS_NONE));
                        break;
                    case CONVERSATION_SENDERS_IMPORTANT:
                        mPreference.setValue(String.valueOf(CONVERSATION_SENDERS_IMPORTANT));
                        break;
                    default:
                        mPreference.setValue(String.valueOf(CONVERSATION_SENDERS_ANYONE));
                        break;
                }
        }
    }
}
