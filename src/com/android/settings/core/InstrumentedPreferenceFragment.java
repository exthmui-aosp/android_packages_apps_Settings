/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.core;

import android.annotation.Nullable;
import android.annotation.StringRes;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.XmlRes;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.core.instrumentation.VisibilityLoggerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.survey.SurveyMixin;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

/**
 * Instrumented fragment that logs visibility state.
 */
public abstract class InstrumentedPreferenceFragment extends ObservablePreferenceFragment
        implements Instrumentable {

    private static final String TAG = "InstrumentedPrefFrag";
    private static final String FEATURE_FLAG_USE_PREFERENCE_SCREEN_TITLE =
            "settings_use_preference_screen_title";
    protected MetricsFeatureProvider mMetricsFeatureProvider;

    // metrics placeholder value. Only use this for development.
    protected final int PLACEHOLDER_METRIC = 10000;

    private final VisibilityLoggerMixin mVisibilityLoggerMixin;

    public InstrumentedPreferenceFragment() {
        // Mixin that logs visibility change for activity.
        mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory());
        getLifecycle().addObserver(mVisibilityLoggerMixin);
        getLifecycle().addObserver(new SurveyMixin(this, getClass().getSimpleName()));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (usePreferenceScreenTitle()) {
            final int title = getTitle();
            if (title != -1) {
                getActivity().setTitle(title);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void onResume() {
        mVisibilityLoggerMixin.setSourceMetricsCategory(getActivity());
        super.onResume();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        updateActivityTitleWithScreenTitle(getPreferenceScreen());
    }

    public static boolean usePreferenceScreenTitle() {
        return FeatureFlagUtils.isEnabled(FEATURE_FLAG_USE_PREFERENCE_SCREEN_TITLE);
    }

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    protected final VisibilityLoggerMixin getVisibilityLogger() {
        return mVisibilityLoggerMixin;
    }

    /**
     * Return the resource id of the title to be used for the fragment. This is for preference
     * fragments that do not have an explicit preference screen xml, and hence the title need to be
     * specified separately. Do not use this method if the title is already specified in the
     * preference screen.
     */
    @StringRes
    protected int getTitle() {
        return -1;
    }

    private void updateActivityTitleWithScreenTitle(PreferenceScreen screen) {
        if (usePreferenceScreenTitle() && screen != null) {
            final CharSequence title = screen.getTitle();
            if (!TextUtils.isEmpty(title)) {
                getActivity().setTitle(title);
            } else {
                Log.w(TAG, "Screen title missing for fragment " + this.getClass().getName());
            }
        }
    }

}
