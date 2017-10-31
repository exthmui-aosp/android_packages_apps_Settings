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
 *
 */

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.ComponentName;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchFeatureProviderImplTest {
    private SearchFeatureProviderImpl mProvider;
    private Activity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(Activity.class).create().visible().get();
        mProvider = spy(new SearchFeatureProviderImpl());
    }

    @Test
    public void getSiteMapManager_shouldCacheInstance() {
        final SiteMapManager manager1 = mProvider.getSiteMapManager();
        final SiteMapManager manager2 = mProvider.getSiteMapManager();

        assertThat(manager1).isSameAs(manager2);
    }

    @Test
    public void getDatabaseSearchLoader_shouldCleanupQuery() {
        final String query = "  space ";

        mProvider.getStaticSearchResultTask(mActivity, query);

        verify(mProvider).cleanQuery(eq(query));
    }

    @Test
    public void getInstalledAppSearchLoader_shouldCleanupQuery() {
        final String query = "  space ";

        mProvider.getInstalledAppSearchTask(mActivity, query);

        verify(mProvider).cleanQuery(eq(query));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyLaunchSearchResultPageCaller_nullCaller_shouldCrash() {
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, null /* caller */);
    }

    @Test(expected = SecurityException.class)
    public void everifyLaunchSearchResultPageCaller_badCaller_shouldCrash() {
        final ComponentName cn = new ComponentName("pkg", "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }

    @Test
    public void verifyLaunchSearchResultPageCaller_goodCaller_shouldNotCrash() {
        final ComponentName cn = new ComponentName(mActivity.getPackageName(), "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }

    @Test
    public void cleanQuery_trimsWhitespace() {
        final String query = "  space ";
        final String cleanQuery = "space";

        assertThat(mProvider.cleanQuery(query)).isEqualTo(cleanQuery);
    }
}
