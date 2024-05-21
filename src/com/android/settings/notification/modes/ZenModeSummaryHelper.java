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
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_ALARMS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_CALLS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_CONVERSATIONS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_EVENTS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_MEDIA;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_MESSAGES;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_REMINDERS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_REPEAT_CALLERS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_SYSTEM;

import android.content.Context;
import android.icu.text.MessageFormat;
import android.service.notification.ZenPolicy;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public class ZenModeSummaryHelper {

    private Context mContext;
    private ZenModesBackend mBackend;

    public ZenModeSummaryHelper(Context context, ZenModesBackend backend) {
        mContext = context;
        mBackend = backend;
    }

    private static final int[] ALL_PRIORITY_CATEGORIES = {
            PRIORITY_CATEGORY_ALARMS,
            PRIORITY_CATEGORY_MEDIA,
            PRIORITY_CATEGORY_SYSTEM,
            PRIORITY_CATEGORY_MESSAGES,
            PRIORITY_CATEGORY_CONVERSATIONS,
            PRIORITY_CATEGORY_EVENTS,
            PRIORITY_CATEGORY_REMINDERS,
            PRIORITY_CATEGORY_CALLS,
            PRIORITY_CATEGORY_REPEAT_CALLERS,
    };

    String getOtherSoundCategoriesSummary(ZenMode zenMode) {
        List<String> enabledCategories = getEnabledCategories(
                zenMode.getPolicy(),
                category -> PRIORITY_CATEGORY_ALARMS == category
                        || PRIORITY_CATEGORY_MEDIA == category
                        || PRIORITY_CATEGORY_SYSTEM == category
                        || PRIORITY_CATEGORY_REMINDERS == category
                        || PRIORITY_CATEGORY_EVENTS == category,
                true);
        int numCategories = enabledCategories.size();
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_other_sounds_summary),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", numCategories);
        if (numCategories >= 1) {
            args.put("sound_category_1", enabledCategories.get(0));
            if (numCategories >= 2) {
                args.put("sound_category_2", enabledCategories.get(1));
                if (numCategories == 3) {
                    args.put("sound_category_3", enabledCategories.get(2));
                }
            }
        }
        return msgFormat.format(args);
    }

    String getCallsSettingSummary(ZenMode zenMode) {
        List<String> enabledCategories = getEnabledCategories(zenMode.getPolicy(),
                category -> PRIORITY_CATEGORY_CALLS == category
                        || PRIORITY_CATEGORY_REPEAT_CALLERS == category, true);
        int numCategories = enabledCategories.size();
        if (numCategories == 0) {
            return mContext.getString(R.string.zen_mode_none_calls);
        } else if (numCategories == 1) {
            return mContext.getString(R.string.zen_mode_calls_summary_one,
                    enabledCategories.get(0));
        } else {
            return mContext.getString(R.string.zen_mode_calls_summary_two,
                    enabledCategories.get(0),
                    enabledCategories.get(1));
        }
    }

    String getMessagesSettingSummary(ZenPolicy policy) {
        List<String> enabledCategories = getEnabledCategories(policy,
                category -> PRIORITY_CATEGORY_MESSAGES == category
                        || PRIORITY_CATEGORY_CONVERSATIONS == category, true);
        int numCategories = enabledCategories.size();
        if (numCategories == 0) {
            return mContext.getString(R.string.zen_mode_none_messages);
        } else if (numCategories == 1) {
            return enabledCategories.get(0);
        } else {
            // While this string name seems like a slight misnomer: it's borrowing the analogous
            // calls-summary functionality to combine two permissions.
            return mContext.getString(R.string.zen_mode_calls_summary_two,
                    enabledCategories.get(0),
                    enabledCategories.get(1));
        }
    }

    String getBlockedEffectsSummary(ZenMode zenMode) {
        if (zenMode.getPolicy().shouldShowAllVisualEffects()) {
            return mContext.getResources().getString(
                    R.string.zen_mode_restrict_notifications_summary_muted);
        } else if (zenMode.getPolicy().shouldHideAllVisualEffects()) {
            return mContext.getResources().getString(
                    R.string.zen_mode_restrict_notifications_summary_hidden);
        } else {
            return mContext.getResources().getString(
                    R.string.zen_mode_restrict_notifications_summary_custom);
        }
    }

    private List<String> getEnabledCategories(ZenPolicy policy,
            Predicate<Integer> filteredCategories, boolean capitalizeFirstInList) {
        List<String> enabledCategories = new ArrayList<>();
        for (int category : ALL_PRIORITY_CATEGORIES) {
            boolean isFirst = capitalizeFirstInList && enabledCategories.isEmpty();
            if (filteredCategories.test(category) && policy.isCategoryAllowed(category, false)) {
                if (category == PRIORITY_CATEGORY_REPEAT_CALLERS
                        && policy.isCategoryAllowed(PRIORITY_CATEGORY_CALLS, false)
                        && policy.getPriorityCallSenders() == PEOPLE_TYPE_ANYONE) {
                    continue;
                }

                // For conversations, only the "priority conversations" setting is relevant; any
                // other setting is subsumed by the messages-specific messaging.
                if (category == PRIORITY_CATEGORY_CONVERSATIONS
                        && policy.isCategoryAllowed(PRIORITY_CATEGORY_CONVERSATIONS, false)
                        && policy.getPriorityConversationSenders()
                        != CONVERSATION_SENDERS_IMPORTANT) {
                    continue;
                }

                enabledCategories.add(getCategory(category, policy, isFirst));
            }
        }
        return enabledCategories;
    }

    private String getCategory(int category, ZenPolicy policy, boolean isFirst) {
        if (category == PRIORITY_CATEGORY_ALARMS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_alarms_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_alarms_list);
            }
        } else if (category == PRIORITY_CATEGORY_MEDIA) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_media_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_media_list);
            }
        } else if (category == PRIORITY_CATEGORY_SYSTEM) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_system_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_system_list);
            }
        } else if (category == PRIORITY_CATEGORY_MESSAGES) {
            if (policy.getPriorityMessageSenders() == PEOPLE_TYPE_ANYONE) {
                return mContext.getString(R.string.zen_mode_from_anyone);
            } else if (policy.getPriorityMessageSenders() == PEOPLE_TYPE_CONTACTS) {
                return mContext.getString(R.string.zen_mode_from_contacts);
            } else {
                return mContext.getString(R.string.zen_mode_from_starred);
            }
        } else if (category == PRIORITY_CATEGORY_CONVERSATIONS
                && policy.getPriorityConversationSenders() == CONVERSATION_SENDERS_IMPORTANT) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_from_important_conversations);
            } else {
                return mContext.getString(
                        R.string.zen_mode_from_important_conversations_second);
            }
        } else if (category == PRIORITY_CATEGORY_EVENTS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_events_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_events_list);
            }
        } else if (category == PRIORITY_CATEGORY_REMINDERS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_reminders_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_reminders_list);
            }
        } else if (category == PRIORITY_CATEGORY_CALLS) {
            if (policy.getPriorityCallSenders() == PEOPLE_TYPE_ANYONE) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_anyone);
                }
                return mContext.getString(R.string.zen_mode_all_callers);
            } else if (policy.getPriorityCallSenders() == PEOPLE_TYPE_CONTACTS) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_contacts);
                }
                return mContext.getString(R.string.zen_mode_contacts_callers);
            } else {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_starred);
                }
                return mContext.getString(R.string.zen_mode_starred_callers);
            }
        } else if (category == PRIORITY_CATEGORY_REPEAT_CALLERS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_repeat_callers);
            } else {
                return mContext.getString(R.string.zen_mode_repeat_callers_list);
            }
        }

        return "";
    }

    public String getStarredContactsSummary() {
        List<String> starredContacts = mBackend.getStarredContacts();
        int numStarredContacts = starredContacts.size();
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_starred_contacts_summary_contacts),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", numStarredContacts);
        if (numStarredContacts >= 1) {
            args.put("contact_1", starredContacts.get(0));
            if (numStarredContacts >= 2) {
                args.put("contact_2", starredContacts.get(1));
                if (numStarredContacts == 3) {
                    args.put("contact_3", starredContacts.get(2));
                }
            }
        }
        return msgFormat.format(args);
    }

    public String getContactsNumberSummary() {
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_contacts_count),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", mBackend.queryAllContactsData().getCount());
        return msgFormat.format(args);
    }

    public String getPeopleSummary(ZenMode zenMode) {
        final int callersAllowed = zenMode.getPolicy().getPriorityCallSenders();
        final int messagesAllowed = zenMode.getPolicy().getPriorityMessageSenders();
        final int conversationsAllowed = zenMode.getPolicy().getPriorityConversationSenders();
        final boolean areRepeatCallersAllowed =
                zenMode.getPolicy().isCategoryAllowed(PRIORITY_CATEGORY_REPEAT_CALLERS, false);

        if (callersAllowed == PEOPLE_TYPE_ANYONE
                && messagesAllowed == PEOPLE_TYPE_ANYONE
                && conversationsAllowed == CONVERSATION_SENDERS_ANYONE) {
            return mContext.getResources().getString(R.string.zen_mode_people_all);
        } else if (callersAllowed == PEOPLE_TYPE_NONE
                && messagesAllowed == PEOPLE_TYPE_NONE
                && conversationsAllowed == CONVERSATION_SENDERS_NONE
                && !areRepeatCallersAllowed) {
            return mContext.getResources().getString(R.string.zen_mode_people_none);
        } else {
            return mContext.getResources().getString(R.string.zen_mode_people_some);
        }
    }
}
