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

package com.android.settings.applications.defaultapps;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.settings.R;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for choosing default browser.
 */
public class DefaultBrowserPicker extends DefaultAppPickerFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_browser_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_BROWSER_PICKER;
    }

    @Override
    protected String getDefaultKey() {
        return mPm.getDefaultBrowserPackageNameAsUser(mUserId);
    }

    @Override
    protected boolean setDefaultKey(String packageName) {
        return mPm.setDefaultBrowserPackageNameAsUser(packageName, mUserId);
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final Context context = getContext();
        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        final List<ResolveInfo> list =
            DefaultBrowserPreferenceController.getCandidates(mPm, mUserId);

        for (ResolveInfo info : list) {
            try {
                candidates.add(new DefaultAppInfo(context, mPm, mUserId,
                        mPm.getApplicationInfoAsUser(info.activityInfo.packageName, 0, mUserId)));
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
        }

        return candidates;
    }
}
