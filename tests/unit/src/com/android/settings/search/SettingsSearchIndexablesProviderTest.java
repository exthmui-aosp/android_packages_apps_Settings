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

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.SearchIndexablesContract;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SettingsSearchIndexablesProviderTest {

    private Context mContext;


    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testSiteMapPairsFetched() {
        final Uri uri = Uri.parse("content://" + mContext.getPackageName() + "/" +
                SearchIndexablesContract.SITE_MAP_PAIRS_PATH);
        final Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);

        final int size = cursor.getCount();
        assertThat(size).isGreaterThan(0);
        while (cursor.moveToNext()) {
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow(
                    SearchIndexablesContract.SiteMapColumns.PARENT_CLASS)))
                    .isNotEmpty();
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow(
                    SearchIndexablesContract.SiteMapColumns.CHILD_CLASS)))
                    .isNotEmpty();
        }
    }
}
