/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.deviceadmin;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class DeviceAdminSettings extends DashboardFragment {
    static final String TAG = "DeviceAdminSettings";

    public int getMetricsCategory() {
        return SettingsEnums.DEVICE_ADMIN_SETTINGS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DeviceAdminListPreferenceController.class).setFooterPreferenceMixin(
                mFooterPreferenceMixin);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_admin_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.device_admin_settings;
                    result.add(sir);
                    return result;
                }
            };
}
