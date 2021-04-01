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

package com.android.settings.gestures;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * The Fragment for one-handed mode settings.
 */
@SearchIndexable
public class OneHandedSettings extends DashboardFragment {

    private static final String TAG = "OneHandedSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ONE_HANDED;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void updatePreferenceStates() {
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
        super.updatePreferenceStates();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.one_handed_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.one_handed_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return OneHandedSettingsUtils.isSupportOneHandedMode();
                }
            };
}
