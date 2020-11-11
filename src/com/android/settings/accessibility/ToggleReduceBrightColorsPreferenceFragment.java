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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Settings for reducing brightness. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleReduceBrightColorsPreferenceFragment extends ToggleFeaturePreferenceFragment {
    private static final String REDUCE_BRIGHT_COLORS_ACTIVATED_KEY =
            Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED;
    private static final String KEY_INTENSITY = "rbc_intensity";
    private static final String KEY_PERSIST = "rbc_persist";

    private final Handler mHandler = new Handler();
    private SettingsContentObserver mSettingsContentObserver;
    private ReduceBrightColorsIntensityPreferenceController mRbcIntensityPreferenceController;
    private ReduceBrightColorsPersistencePreferenceController mRbcPersistencePreferenceController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // TODO(b/170973645): Add banner
        mComponentName = AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME;
        mPackageName = getText(R.string.reduce_bright_colors_preference_title);
        mHtmlDescription = getText(R.string.reduce_bright_colors_preference_subtitle);
        final List<String> enableServiceFeatureKeys = new ArrayList<>(/* initialCapacity= */ 1);
        enableServiceFeatureKeys.add(REDUCE_BRIGHT_COLORS_ACTIVATED_KEY);
        mRbcIntensityPreferenceController =
                new ReduceBrightColorsIntensityPreferenceController(getContext(), KEY_INTENSITY);
        mRbcPersistencePreferenceController =
                new ReduceBrightColorsPersistencePreferenceController(getContext(), KEY_PERSIST);
        mRbcIntensityPreferenceController.displayPreference(getPreferenceScreen());
        mRbcPersistencePreferenceController.displayPreference(getPreferenceScreen());
        mSettingsContentObserver = new SettingsContentObserver(mHandler, enableServiceFeatureKeys) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                    updateSwitchBarToggleSwitch();
            }
        };

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferenceOrder();
    }

    /** Customizes the order by preference key. */
    private List<String> getPreferenceOrderList() {
        final List<String> lists = new ArrayList<>();
        lists.add(KEY_USE_SERVICE_PREFERENCE);
        lists.add(KEY_INTENSITY);
        lists.add(KEY_GENERAL_CATEGORY);
        lists.add(KEY_PERSIST);
        lists.add(KEY_INTRODUCTION_CATEGORY);
        return lists;
    }

    private void updatePreferenceOrder() {
        final List<String> lists = getPreferenceOrderList();
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);

        final int size = lists.size();
        for (int i = 0; i < size; i++) {
            final Preference preference = preferenceScreen.findPreference(lists.get(i));
            if (preference != null) {
                preference.setOrder(i);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBarToggleSwitch();
        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REDUCE_BRIGHT_COLORS_SETTINGS;
    }

    @Override
    public int getHelpResource() {
        // TODO(170973645): Link to help support page
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.reduce_bright_colors_settings;
    }


    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(mComponentName, enabled);
        Settings.Secure.putInt(getContentResolver(),
                REDUCE_BRIGHT_COLORS_ACTIVATED_KEY, enabled ? ON : OFF);
    }

    @Override
    protected void onRemoveSwitchPreferenceToggleSwitch() {
        super.onRemoveSwitchPreferenceToggleSwitch();
        mToggleServiceDividerSwitchPreference.setOnPreferenceClickListener(
                /* onPreferenceClickListener= */ null);
    }

    @Override
    protected void updateToggleServiceTitle(SwitchPreference switchPreference) {
        switchPreference.setTitle(R.string.reduce_bright_colors_switch_title);
    }

    @Override
    protected void onInstallSwitchPreferenceToggleSwitch() {
        super.onInstallSwitchPreferenceToggleSwitch();
        updateSwitchBarToggleSwitch();
        mToggleServiceDividerSwitchPreference.setOnPreferenceClickListener((preference) -> {
            boolean checked = ((SwitchPreference) preference).isChecked();
            onPreferenceToggled(mPreferenceKey, checked);
            return false;
        });
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
    }

    private void updateSwitchBarToggleSwitch() {
        final boolean checked = Settings.Secure.getInt(getContentResolver(),
                REDUCE_BRIGHT_COLORS_ACTIVATED_KEY, OFF) == ON;
        mRbcIntensityPreferenceController.updateState(getPreferenceScreen()
                .findPreference(KEY_INTENSITY));
        mRbcPersistencePreferenceController.updateState(getPreferenceScreen()
                .findPreference(KEY_PERSIST));
        if (mToggleServiceDividerSwitchPreference.isChecked() != checked) {
            mToggleServiceDividerSwitchPreference.setChecked(checked);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.reduce_bright_colors_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    // TODO(b/170970675): call into CDS to get availability/config status
                    return true;
                }
            };
}
