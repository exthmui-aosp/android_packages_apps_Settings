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

package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.RadioButtonPreference;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that is used to control screen timeout.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ScreenTimeoutSettings extends RadioButtonPickerFragment implements
        HelpResourceProvider {
    private static final String TAG = "ScreenTimeout";
    /** If there is no setting in the provider, use this. */
    public static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final int DEFAULT_ORDER_OF_LOWEST_PREFERENCE = Integer.MAX_VALUE - 1;

    private CharSequence[] mInitialEntries;
    private CharSequence[] mInitialValues;
    private FooterPreference mPrivacyPreference;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @VisibleForTesting
    RestrictedLockUtils.EnforcedAdmin mAdmin;
    @VisibleForTesting
    Preference mDisableOptionsPreference;

    @VisibleForTesting
    AdaptiveSleepPermissionPreferenceController mAdaptiveSleepPermissionController;

    @VisibleForTesting
    AdaptiveSleepPreferenceController mAdaptiveSleepController;

    public ScreenTimeoutSettings() {
        super();
        mMetricsFeatureProvider = FeatureFactory.getFactory(getContext())
                .getMetricsFeatureProvider();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mInitialEntries = getResources().getStringArray(R.array.screen_timeout_entries);
        mInitialValues = getResources().getStringArray(R.array.screen_timeout_values);
        mAdaptiveSleepController = new AdaptiveSleepPreferenceController(context);
        mAdaptiveSleepPermissionController = new AdaptiveSleepPermissionPreferenceController(
                context);
        mPrivacyPreference = new FooterPreference(context);
        mPrivacyPreference.setIcon(R.drawable.ic_privacy_shield_24dp);
        mPrivacyPreference.setTitle(R.string.adaptive_sleep_privacy);
        mPrivacyPreference.setSelectable(false);
        mPrivacyPreference.setLayoutResource(R.layout.preference_footer);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<CandidateInfo> candidates = new ArrayList<>();
        final long maxTimeout = getMaxScreenTimeout(getContext());
        if (mInitialValues != null) {
            for (int i = 0; i < mInitialValues.length; ++i) {
                if (Long.parseLong(mInitialValues[i].toString()) <= maxTimeout) {
                    candidates.add(new TimeoutCandidateInfo(mInitialEntries[i],
                            mInitialValues[i].toString(), true));
                }
            }
        } else {
            Log.e(TAG, "Screen timeout options do not exist.");
        }
        return candidates;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdaptiveSleepPermissionController.updateVisibility();
        mAdaptiveSleepController.updatePreference();
    }

    @Override
    public void updateCandidates() {
        final String defaultKey = getDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList == null) {
            return;
        }

        for (CandidateInfo info : candidateList) {
            RadioButtonPreference pref =
                    new RadioButtonPreference(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            screen.addPreference(pref);
        }

        final long selectedTimeout = Long.parseLong(defaultKey);
        final long maxTimeout = getMaxScreenTimeout(getContext());
        if (!candidateList.isEmpty() && (selectedTimeout > maxTimeout)) {
            // The selected time out value is longer than the max timeout allowed by the admin.
            // Select the largest value from the list by default.
            final RadioButtonPreference preferenceWithLargestTimeout =
                    (RadioButtonPreference) screen.getPreference(candidateList.size() - 1);
            preferenceWithLargestTimeout.setChecked(true);
        }

        if (isScreenAttentionAvailable(getContext())) {
            mAdaptiveSleepPermissionController.addToScreen(screen);
            mAdaptiveSleepController.addToScreen(screen);
            screen.addPreference(mPrivacyPreference);
        }

        if (mAdmin != null) {
            setupDisabledFooterPreference();
            screen.addPreference(mDisableOptionsPreference);
        }
    }

    @VisibleForTesting
    void setupDisabledFooterPreference() {
        final String textDisabledByAdmin = getResources().getString(
                R.string.admin_disabled_other_options);
        final String textMoreDetails = getResources().getString(R.string.admin_more_details);

        final SpannableString spannableString = new SpannableString(
                textDisabledByAdmin + System.lineSeparator()
                + System.lineSeparator() + textMoreDetails);
        final ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), mAdmin);
            }
        };

        if (textDisabledByAdmin != null && textMoreDetails != null) {
            spannableString.setSpan(clickableSpan, textDisabledByAdmin.length() + 1,
                    textDisabledByAdmin.length() + textMoreDetails.length() + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        mDisableOptionsPreference = new FooterPreference(getContext());
        mDisableOptionsPreference.setLayoutResource(R.layout.preference_footer);
        mDisableOptionsPreference.setTitle(spannableString);
        mDisableOptionsPreference.setSelectable(false);
        mDisableOptionsPreference.setIcon(R.drawable.ic_info_outline_24dp);

        // The 'disabled by admin' preference should always be at the end of the setting page.
        mDisableOptionsPreference.setOrder(DEFAULT_ORDER_OF_LOWEST_PREFERENCE);
        mPrivacyPreference.setOrder(DEFAULT_ORDER_OF_LOWEST_PREFERENCE - 1);
    }

    @Override
    protected String getDefaultKey() {
        return getCurrentSystemScreenTimeout(getContext());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        setCurrentSystemScreenTimeout(getContext(), key);
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_TIMEOUT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_timeout_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_adaptive_sleep;
    }

    private Long getMaxScreenTimeout(Context context) {
        if (context == null) {
            return Long.MAX_VALUE;
        }
        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        if (dpm == null) {
            return Long.MAX_VALUE;
        }
        mAdmin = RestrictedLockUtilsInternal.checkIfMaximumTimeToLockIsSet(context);
        if (mAdmin != null) {
            return dpm.getMaximumTimeToLock(null /* admin */, UserHandle.myUserId());
        }
        return Long.MAX_VALUE;
    }

    private String getCurrentSystemScreenTimeout(Context context) {
        if (context == null) {
            return Long.toString(FALLBACK_SCREEN_TIMEOUT_VALUE);
        } else {
            return Long.toString(Settings.System.getLong(context.getContentResolver(),
                    SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));
        }
    }

    private void setCurrentSystemScreenTimeout(Context context, String key) {
        try {
            if (context != null) {
                final long value = Long.parseLong(key);
                mMetricsFeatureProvider.action(context, SettingsEnums.ACTION_SCREEN_TIMEOUT_CHANGED,
                        (int) value);
                Settings.System.putLong(context.getContentResolver(), SCREEN_OFF_TIMEOUT, value);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist screen timeout setting", e);
        }
    }

    private static boolean isScreenAttentionAvailable(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_adaptive_sleep_available);
    }

    private static class TimeoutCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        TimeoutCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_timeout_settings) {
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    if (!isScreenAttentionAvailable(context)) {
                        return null;
                    }
                    final Resources res = context.getResources();
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.adaptive_sleep_title);
                    data.key = AdaptiveSleepPreferenceController.PREFERENCE_KEY;
                    data.keywords = res.getString(R.string.adaptive_sleep_title);

                    final List<SearchIndexableRaw> result = new ArrayList<>(1);
                    result.add(data);
                    return result;
                }
            };
}
