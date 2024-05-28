/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_UNSET;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_UNSET;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.icu.text.MessageFormat;
import android.provider.Contacts;
import android.service.notification.ConversationChannelWrapper;
import android.service.notification.ZenPolicy;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.app.ConversationListSettings;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Common preference controller functionality for zen mode priority senders preferences for both
 * messages and calls.
 *
 * These controllers handle the settings regarding which priority senders that are allowed to
 * bypass DND for calls or messages, which may be one of the following values: starred contacts, all
 * contacts, priority conversations (for messages only), anyone, or no one.
 */
class ZenModePrioritySendersPreferenceController
        extends AbstractZenModePreferenceController {
    private final boolean mIsMessages; // if this is false, then this preference is for calls

    static final String KEY_ANY = "senders_anyone";
    static final String KEY_CONTACTS = "senders_contacts";
    static final String KEY_STARRED = "senders_starred_contacts";
    static final String KEY_IMPORTANT = "conversations_important";
    static final String KEY_NONE = "senders_none";

    private int mNumImportantConversations = 0;

    private static final Intent ALL_CONTACTS_INTENT =
            new Intent(Contacts.Intents.UI.LIST_DEFAULT)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    private static final Intent STARRED_CONTACTS_INTENT =
            new Intent(Contacts.Intents.UI.LIST_STARRED_ACTION)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    private static final Intent FALLBACK_INTENT = new Intent(Intent.ACTION_MAIN)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

    private final PackageManager mPackageManager;
    private PreferenceCategory mPreferenceCategory;
    private List<SelectorWithWidgetPreference> mSelectorPreferences = new ArrayList<>();

    private final ZenModeSummaryHelper mZenModeSummaryHelper;

    public ZenModePrioritySendersPreferenceController(Context context, String key,
            boolean isMessages, ZenModesBackend backend) {
        super(context, key, backend);
        mIsMessages = isMessages;

        String contactsPackage = context.getString(R.string.config_contacts_package_name);
        ALL_CONTACTS_INTENT.setPackage(contactsPackage);
        STARRED_CONTACTS_INTENT.setPackage(contactsPackage);
        FALLBACK_INTENT.setPackage(contactsPackage);

        mPackageManager = mContext.getPackageManager();
        if (!FALLBACK_INTENT.hasCategory(Intent.CATEGORY_APP_CONTACTS)) {
            FALLBACK_INTENT.addCategory(Intent.CATEGORY_APP_CONTACTS);
        }
        mZenModeSummaryHelper = new ZenModeSummaryHelper(mContext, mBackend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        if (mPreferenceCategory.getPreferenceCount() == 0) {
            makeSelectorPreference(KEY_STARRED,
                    com.android.settings.R.string.zen_mode_from_starred, mIsMessages);
            makeSelectorPreference(KEY_CONTACTS,
                    com.android.settings.R.string.zen_mode_from_contacts, mIsMessages);
            if (mIsMessages) {
                makeSelectorPreference(KEY_IMPORTANT,
                        com.android.settings.R.string.zen_mode_from_important_conversations, true);
            }
            makeSelectorPreference(KEY_ANY,
                    com.android.settings.R.string.zen_mode_from_anyone, mIsMessages);
            makeSelectorPreference(KEY_NONE,
                    com.android.settings.R.string.zen_mode_none_messages, mIsMessages);
        }
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (mIsMessages) {
            updateChannelCounts();
        }
        final int currContactsSetting = getPrioritySenders(zenMode.getPolicy());
        final int currConversationsSetting = getPriorityConversationSenders(zenMode.getPolicy());
        for (SelectorWithWidgetPreference pref : mSelectorPreferences) {
            // for each preference, check whether the current state matches what this state
            // would look like if the button were checked.
            final int[] checkedState = keyToSettingEndState(pref.getKey(), true);
            final int checkedContactsSetting = checkedState[0];
            final int checkedConversationsSetting = checkedState[1];

            boolean match = checkedContactsSetting == currContactsSetting;
            if (mIsMessages && checkedConversationsSetting != CONVERSATION_SENDERS_UNSET) {
                // "CONVERSATION_SENDERS_UNSET" in checkedContactsSetting means this preference
                // doesn't govern the priority senders setting, so the full match happens when
                // either the priority senders setting matches or if it's CONVERSATION_SENDERS_UNSET
                // so only the conversation setting needs to match.
                match = (match || checkedContactsSetting == PEOPLE_TYPE_UNSET)
                        && (checkedConversationsSetting == currConversationsSetting);
            }

            pref.setChecked(match);
        }
        updateSummaries();
    }

    public void onResume() {
        if (mIsMessages) {
            updateChannelCounts();
        }
        updateSummaries();
    }

    private void updateChannelCounts() {
        ParceledListSlice<ConversationChannelWrapper> impConversations =
                mBackend.getConversations(true);
        int numImportantConversations = 0;
        if (impConversations != null) {
            for (ConversationChannelWrapper conversation : impConversations.getList()) {
                if (!conversation.getNotificationChannel().isDemoted()) {
                    numImportantConversations++;
                }
            }
        }
        mNumImportantConversations = numImportantConversations;
    }

    private int getPrioritySenders(ZenPolicy policy) {
        if (mIsMessages) {
            return policy.getPriorityMessageSenders();
        } else {
            return policy.getPriorityCallSenders();
        }
    }

    private int getPriorityConversationSenders(ZenPolicy policy) {
        if (mIsMessages) {
            return policy.getPriorityConversationSenders();
        }
        return CONVERSATION_SENDERS_UNSET;
    }

    private SelectorWithWidgetPreference makeSelectorPreference(String key, int titleId,
            boolean isCheckbox) {
        final SelectorWithWidgetPreference pref =
                new SelectorWithWidgetPreference(mPreferenceCategory.getContext(), isCheckbox);
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(mSelectorClickListener);

        View.OnClickListener widgetClickListener = getWidgetClickListener(key);
        if (widgetClickListener != null) {
            pref.setExtraWidgetOnClickListener(widgetClickListener);
        }

        mPreferenceCategory.addPreference(pref);
        mSelectorPreferences.add(pref);
        return pref;
    }

    private View.OnClickListener getWidgetClickListener(String key) {
        if (!KEY_CONTACTS.equals(key) && !KEY_STARRED.equals(key) && !KEY_IMPORTANT.equals(key)) {
            return null;
        }

        if (KEY_STARRED.equals(key) && !isStarredIntentValid()) {
            return null;
        }

        if (KEY_CONTACTS.equals(key) && !isContactsIntentValid()) {
            return null;
        }

        return v -> {
            if (KEY_STARRED.equals(key)
                    && STARRED_CONTACTS_INTENT.resolveActivity(mPackageManager) != null) {
                mContext.startActivity(STARRED_CONTACTS_INTENT);
            } else if (KEY_CONTACTS.equals(key)
                    && ALL_CONTACTS_INTENT.resolveActivity(mPackageManager) != null) {
                mContext.startActivity(ALL_CONTACTS_INTENT);
            } else if (KEY_IMPORTANT.equals(key)) {
                // TODO: b/332937635 - set correct metrics category
                new SubSettingLauncher(mContext)
                        .setDestination(ConversationListSettings.class.getName())
                        .setSourceMetricsCategory(SettingsEnums.DND_CONVERSATIONS)
                        .launch();
            } else {
                mContext.startActivity(FALLBACK_INTENT);
            }
        };
    }

    private boolean isStarredIntentValid() {
        return STARRED_CONTACTS_INTENT.resolveActivity(mPackageManager) != null
                || FALLBACK_INTENT.resolveActivity(mPackageManager) != null;
    }

    private boolean isContactsIntentValid() {
        return ALL_CONTACTS_INTENT.resolveActivity(mPackageManager) != null
                || FALLBACK_INTENT.resolveActivity(mPackageManager) != null;
    }

    void updateSummaries() {
        for (SelectorWithWidgetPreference pref : mSelectorPreferences) {
            pref.setSummary(getSummary(pref.getKey()));
        }
    }

    // Gets the desired end state of the priority senders and conversations for the given key
    // and whether it is being checked or unchecked. [type]_UNSET indicates no change in state.
    //
    // Returns an integer array with 2 entries. The first entry is the setting for priority senders
    // and the second entry is for priority conversation senders; if isMessages is false, then
    // no changes will ever be prescribed for conversation senders.
    int[] keyToSettingEndState(String key, boolean checked) {
        int[] endState = new int[]{ PEOPLE_TYPE_UNSET, CONVERSATION_SENDERS_UNSET };
        if (!checked) {
            // Unchecking any priority-senders-based state should reset the state to NONE.
            // "Unchecking" the NONE state doesn't do anything, in practice.
            switch (key) {
                case KEY_STARRED:
                case KEY_CONTACTS:
                case KEY_ANY:
                case KEY_NONE:
                    endState[0] = PEOPLE_TYPE_NONE;
            }

            // For messages, unchecking "priority conversations" and "any" should reset conversation
            // state to "NONE" as well.
            if (mIsMessages) {
                switch (key) {
                    case KEY_IMPORTANT:
                    case KEY_ANY:
                    case KEY_NONE:
                        endState[1] = CONVERSATION_SENDERS_NONE;
                }
            }
        } else {
            // All below is for the enabling (checked) state.
            switch (key) {
                case KEY_STARRED:
                    endState[0] = PEOPLE_TYPE_STARRED;
                    break;
                case KEY_CONTACTS:
                    endState[0] = PEOPLE_TYPE_CONTACTS;
                    break;
                case KEY_ANY:
                    endState[0] = PEOPLE_TYPE_ANYONE;
                    break;
                case KEY_NONE:
                    endState[0] = PEOPLE_TYPE_NONE;
            }

            // In the messages case *only*, also handle changing of conversation settings.
            if (mIsMessages) {
                switch (key) {
                    case KEY_IMPORTANT:
                        endState[1] = CONVERSATION_SENDERS_IMPORTANT;
                        break;
                    case KEY_ANY:
                        endState[1] = CONVERSATION_SENDERS_ANYONE;
                        break;
                    case KEY_NONE:
                        endState[1] = CONVERSATION_SENDERS_NONE;
                }
            }
        }
        // Error case check: if somehow, after all of that, endState is still
        // {PEOPLE_TYPE_UNSET, CONVERSATION_SENDERS_UNSET}, something has gone wrong.
        if (endState[0] == PEOPLE_TYPE_UNSET && endState[1] == CONVERSATION_SENDERS_UNSET) {
            throw new IllegalArgumentException("invalid key " + key);
        }

        return endState;
    }

    // Returns the preferences, if any, that should be newly saved for the specified setting and
    // checked state in an array where index 0 is the new senders setting and 1 the new
    // conversations setting. A return value of [type]_UNSET indicates that nothing should
    // change.
    //
    // The returned conversations setting will always be CONVERSATION_SENDERS_UNSET (not to change)
    // in the calls case.
    //
    // Checking and unchecking is mostly an operation of setting or unsetting the relevant
    // preference, except for some special handling where the conversation setting overlaps:
    //   - setting or unsetting "priority contacts" or "contacts" has no effect on the
    //     priority conversation setting, and vice versa
    //   - if "priority conversations" is selected, and the user checks "anyone", the conversation
    //     setting is also set to any conversations
    //   - if "anyone" is previously selected, and the user clicks "priority conversations", then
    //     the contacts setting is additionally reset to "none".
    //   - if "anyone" is previously selected, and the user clicks one of the contacts values,
    //     then the conversations setting is additionally reset to "none".
    int[] settingsToSaveOnClick(String key, boolean checked,
            int currSendersSetting, int currConvosSetting) {
        int[] savedSettings = new int[]{ PEOPLE_TYPE_UNSET, CONVERSATION_SENDERS_UNSET };

        // If the preference isn't a checkbox, always consider this to be "checking" the setting.
        // Otherwise, toggle.
        final int[] endState = keyToSettingEndState(key, checked);
        final int prioritySendersSetting = endState[0];
        final int priorityConvosSetting = endState[1];
        if (prioritySendersSetting != PEOPLE_TYPE_UNSET
                && prioritySendersSetting != currSendersSetting) {
            savedSettings[0] = prioritySendersSetting;
        }

        // Only handle conversation settings for the messages case. If not messages, there should
        // never be any change to the conversation senders setting.
        if (mIsMessages) {
            if (priorityConvosSetting != CONVERSATION_SENDERS_UNSET
                    && priorityConvosSetting != currConvosSetting) {
                savedSettings[1] = priorityConvosSetting;
            }

            // Special-case handling for the "priority conversations" checkbox:
            // If a specific selection exists for priority senders (starred, contacts), we leave
            // it untouched. Otherwise (when the senders is set to "any"), set it to NONE.
            if (key.equals(KEY_IMPORTANT)
                    && currSendersSetting == PEOPLE_TYPE_ANYONE) {
                savedSettings[0] = PEOPLE_TYPE_NONE;
            }

            // Flip-side special case for clicking either "contacts" option: if a specific selection
            // exists for priority conversations, leave it untouched; otherwise, set to none.
            if ((key.equals(KEY_STARRED) || key.equals(KEY_CONTACTS))
                    && currConvosSetting == CONVERSATION_SENDERS_ANYONE) {
                savedSettings[1] = CONVERSATION_SENDERS_NONE;
            }
        }

        return savedSettings;
    }

    private String getSummary(String key) {
        switch (key) {
            case KEY_STARRED:
                return mZenModeSummaryHelper.getStarredContactsSummary();
            case KEY_CONTACTS:
                return mZenModeSummaryHelper.getContactsNumberSummary();
            case KEY_IMPORTANT:
                return getConversationSummary();
            case KEY_ANY:
                return mContext.getResources().getString(mIsMessages
                        ? R.string.zen_mode_all_messages_summary
                        : R.string.zen_mode_all_calls_summary);
            case KEY_NONE:
            default:
                return null;
        }
    }

    private String getConversationSummary() {
        final int numConversations = mNumImportantConversations;

        if (numConversations == CONVERSATION_SENDERS_UNSET) {
            return null;
        } else {
            MessageFormat msgFormat = new MessageFormat(
                    mContext.getString(R.string.zen_mode_conversations_count),
                    Locale.getDefault());
            Map<String, Object> args = new HashMap<>();
            args.put("count", numConversations);
            return msgFormat.format(args);
        }
    }

    @VisibleForTesting
    SelectorWithWidgetPreference.OnClickListener mSelectorClickListener =
            new SelectorWithWidgetPreference.OnClickListener() {
                @Override
                public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
                    savePolicy(policy -> {
                        ZenPolicy previousPolicy = policy.build();
                        final int[] settingsToSave = settingsToSaveOnClick(
                                preference.getKey(),
                                preference.isCheckBox() ? !preference.isChecked() : true,
                                getPrioritySenders(previousPolicy),
                                getPriorityConversationSenders(previousPolicy));
                        final int prioritySendersSetting = settingsToSave[0];
                        final int priorityConvosSetting = settingsToSave[1];

                        if (prioritySendersSetting != PEOPLE_TYPE_UNSET) {
                            if (mIsMessages) {
                                policy.allowMessages(prioritySendersSetting);
                            } else {
                                policy.allowCalls(prioritySendersSetting);
                            }
                        }
                        if (mIsMessages && priorityConvosSetting != CONVERSATION_SENDERS_UNSET) {
                            policy.allowConversations(priorityConvosSetting);
                        }
                        return policy;
                    });
                }
            };
}
