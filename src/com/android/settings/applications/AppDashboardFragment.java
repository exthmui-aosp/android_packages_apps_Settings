/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Settings page for apps. */
@SearchIndexable
public class AppDashboardFragment extends DashboardFragment {

    private static final String TAG = "AppDashboardFragment";
    private AppsPreferenceController mAppsPreferenceController;

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AppsPreferenceController(context));
        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_APP_NOTIF_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_apps_and_notifications;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.apps;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SpecialAppAccessPreferenceController.class).setSession(getSettingsLifecycle());
        mAppsPreferenceController = use(AppsPreferenceController.class);
        mAppsPreferenceController.setFragment(this /* fragment */);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    public String getCategoryKey() {
        // TODO(b/174964405): Remove this function when the silky flag was deprecated.
        // To include injection tiles, map this app fragment to the app category in the short term.
        // When we deprecate the silky flag, we have to:
        // 1. Remove this method.
        // 2. Update the mapping in DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP.
        return CategoryKey.CATEGORY_APPS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.apps;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    // TODO(b/174964405): This method should be removed when silky home launched.
                    // Only allow this page can be searchable when silky home enabled.
                    return FeatureFlagUtils.isEnabled(context, FeatureFlags.SILKY_HOME);
                }
            };
}
