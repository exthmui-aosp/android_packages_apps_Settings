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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class AdvancedConnectedDeviceDashboardFragmentTest {

    private AdvancedConnectedDeviceDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = new AdvancedConnectedDeviceDashboardFragment();
    }

    @Test
    public void testCategory_isConnectedDevice() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_DEVICE);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
            AdvancedConnectedDeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void testGetCategoryKey_returnCategoryDevice() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_DEVICE);
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks = ConnectedDeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final int xmlId = new ConnectedDeviceDashboardFragment().getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }
}
