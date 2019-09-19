/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.location;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Dashboard Fragment to display all recent location requests, sorted by recency. */
@SearchIndexable
public class RecentLocationRequestSeeAllFragment extends DashboardFragment {
    private static final String TAG = "RecentLocationReqAll";
    public static final String PATH =
            "com.android.settings.location.RecentLocationRequestSeeAllFragment";

    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 1;
    private static final int MENU_HIDE_SYSTEM = Menu.FIRST + 2;

    private boolean mShowSystem = false;
    private MenuItem mShowSystemMenu;
    private MenuItem mHideSystemMenu;
    private RecentLocationRequestSeeAllPreferenceController mController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RECENT_LOCATION_REQUESTS_ALL;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return SEARCH_INDEX_DATA_PROVIDER.getXmlResourceId();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case MENU_SHOW_SYSTEM:
            case MENU_HIDE_SYSTEM:
                mShowSystem = menuItem.getItemId() == MENU_SHOW_SYSTEM;
                updateMenu();
                if (mController != null) {
                    mController.setShowSystem(mShowSystem);
                }
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void updateMenu() {
        mShowSystemMenu.setVisible(!mShowSystem);
        mHideSystemMenu.setVisible(mShowSystem);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle, RecentLocationRequestSeeAllFragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final RecentLocationRequestSeeAllPreferenceController controller =
                new RecentLocationRequestSeeAllPreferenceController(context, lifecycle, fragment);
        controllers.add(controller);
        if (fragment != null) {
            fragment.mController = controller;
        }
        return controllers;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.location_recent_requests_see_all) {

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(
                            context, /* lifecycle = */ null, /* fragment = */ null);
                }
            };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mShowSystemMenu = menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE,
                R.string.menu_show_system);
        mHideSystemMenu = menu.add(Menu.NONE, MENU_HIDE_SYSTEM, Menu.NONE,
                R.string.menu_hide_system);
        updateMenu();
    }
}
