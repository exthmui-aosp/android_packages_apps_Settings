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
 * limitations under the License.
 */

package com.android.settings.notification;

import android.app.NotificationManager.Policy;
import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeVisEffectsCustomPreferenceController
        extends AbstractZenModePreferenceController {

    protected boolean mShowMenuSelected;
    protected static final int INTERRUPTIVE_EFFECTS = Policy.SUPPRESSED_EFFECT_AMBIENT
            | Policy.SUPPRESSED_EFFECT_PEEK
            | Policy.SUPPRESSED_EFFECT_LIGHTS
            | Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;

    public ZenModeVisEffectsCustomPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        if (mShowMenuSelected) {
            return true;
        }

        return areCustomOptionsSelected();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        ZenCustomRadioButtonPreference pref = (ZenCustomRadioButtonPreference) preference;
        pref.setChecked(areCustomOptionsSelected());

        pref.setOnGearClickListener(p -> {
            new SubSettingLauncher(mContext)
                    .setDestination(ZenModeBlockedEffectsSettings.class.getName())
                    .setTitle(R.string.zen_mode_what_to_block_title)
                    .setSourceMetricsCategory(MetricsProto.MetricsEvent.SETTINGS_ZEN_NOTIFICATIONS)
                    .launch();
        });

        pref.setOnRadioButtonClickListener(p -> {
            select();
        });
    }

    protected void setShownByMenu(boolean shown) {
        mShowMenuSelected = shown;
    }

    protected boolean areCustomOptionsSelected() {
        boolean allEffectsSuppressed =
                Policy.areAllVisualEffectsSuppressed(mBackend.mPolicy.suppressedVisualEffects);
        boolean noEffectsSuppressed = mBackend.mPolicy.suppressedVisualEffects == 0;

        return !(allEffectsSuppressed || noEffectsSuppressed);
    }

    protected void select() {
        mMetricsFeatureProvider.action(mContext,
                MetricsProto.MetricsEvent.ACTION_ZEN_CUSTOM, true);
        mBackend.savePolicy(mBackend.mPolicy.priorityCategories,
                mBackend.mPolicy.priorityCallSenders,
                mBackend.mPolicy.priorityMessageSenders,
                INTERRUPTIVE_EFFECTS);
    }
}