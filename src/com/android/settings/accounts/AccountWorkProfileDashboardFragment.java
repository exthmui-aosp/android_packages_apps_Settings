/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accounts;

import static android.provider.Settings.EXTRA_AUTHORITIES;

import static com.android.settings.accounts.AccountDashboardFragment.buildAutofillPreferenceControllers;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncWorkDataPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Account Setting page for work profile.
 */
public class AccountWorkProfileDashboardFragment extends DashboardFragment {

    private static final String TAG = "AccountWorkProfileFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCOUNT_WORK;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accounts_work_dashboard_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        buildAutofillPreferenceControllers(context, controllers);
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        buildAccountPreferenceControllers(context, this /* parent */, authorities, controllers);
        return controllers;
    }

    private static void buildAccountPreferenceControllers(
            Context context, SettingsPreferenceFragment parent, String[] authorities,
            List<AbstractPreferenceController> controllers) {
        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(context, parent, authorities,
                        ProfileSelectFragment.ProfileType.WORK);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(accountPrefController);
        }
        controllers.add(accountPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, parent));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, parent));
    }

    // TODO: b/141601408. After featureFlag settings_work_profile is launched, unmark this
//    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
//            new BaseSearchIndexProvider(R.xml.accounts_work_dashboard_settings) {
//
//                @Override
//                public List<AbstractPreferenceController> createPreferenceControllers(
//                        Context context) {
//                    ..Add autofill here too..
//                    return buildPreferenceControllers(
//                            context, null /* parent */, null /* authorities*/);
//                }
//            };
}
