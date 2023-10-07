/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.settings.SettingsEnums.CONTENT_PROTECTION_PREFERENCE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public class ContentProtectionPreferenceFragmentTest {

    @Mock
    private ContentProtectionPreferenceFragment mMockFragment;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mMockFragment = spy(new ContentProtectionPreferenceFragment());

        doReturn(mContext).when(mMockFragment).getContext();
    }

    @Test
    public void getMetricsCategory() {
        assertThat(mMockFragment.getMetricsCategory()).isEqualTo(CONTENT_PROTECTION_PREFERENCE);
    }

    @Test
    public void getPreferenceScreenResId(){
       assertThat(mMockFragment.getPreferenceScreenResId())
               .isEqualTo(R.layout.content_protection_preference_fragment);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> nonIndexableKeys =
                             ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> allKeys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.layout.content_protection_preference_fragment);

        assertThat(allKeys).containsAtLeastElementsIn(nonIndexableKeys);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, /* enabled = */ true);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes).isNotEmpty();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mMockFragment.getPreferenceScreenResId());
    }
}
