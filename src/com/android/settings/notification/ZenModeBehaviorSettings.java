/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class ZenModeBehaviorSettings extends ZenModeSettingsBase implements Indexable {

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeAlarmsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeMediaSystemOtherPreferenceController(context, lifecycle));
        controllers.add(new ZenModeEventsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeRemindersPreferenceController(context, lifecycle));
        controllers.add(new ZenModeMessagesPreferenceController(context, lifecycle));
        controllers.add(new ZenModeCallsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeRepeatCallersPreferenceController(context, lifecycle));
        controllers.add(new ZenModeScreenOnPreferenceController(context, lifecycle));
        controllers.add(new ZenModeScreenOffPreferenceController(context, lifecycle));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_behavior_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_PRIORITY;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(ZenModeAlarmsPreferenceController.KEY);
                    keys.add(ZenModeMediaSystemOtherPreferenceController.KEY);
                    keys.add(ZenModeEventsPreferenceController.KEY);
                    keys.add(ZenModeRemindersPreferenceController.KEY);
                    keys.add(ZenModeMessagesPreferenceController.KEY);
                    keys.add(ZenModeCallsPreferenceController.KEY);
                    keys.add(ZenModeRepeatCallersPreferenceController.KEY);
                    keys.add(ZenModeScreenOnPreferenceController.KEY);
                    keys.add(ZenModeScreenOffPreferenceController.KEY);
                    return keys;
                }

            @Override
            public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                return buildPreferenceControllers(context, null);
            }
        };
}
