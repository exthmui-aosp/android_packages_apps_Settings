/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import static android.content.ContentResolver.SCHEME_CONTENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import androidx.slice.Slice;

import java.util.Collection;
import java.util.HashMap;

/**
 * TODO Investigate using ShadowContentResolver.registerProviderInternal(String, ContentProvider)
 */
@RunWith(SettingsRobolectricTestRunner.class)
public class SettingsSliceProviderTest {

    private final String KEY = "KEY";
    private final String INTENT_PATH = SettingsSlicesContract.PATH_SETTING_INTENT + "/" + KEY;
    private final String ACTION_PATH = SettingsSlicesContract.PATH_SETTING_ACTION + "/" + KEY;
    private final String TITLE = "title";
    private final String SUMMARY = "summary";
    private final String SCREEN_TITLE = "screen title";
    private final String FRAGMENT_NAME = "fragment name";
    private final int ICON = 1234; // I declare a thumb war
    private final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private final String PREF_CONTROLLER = FakeToggleController.class.getName();
    private Context mContext;
    private SettingsSliceProvider mProvider;
    private SQLiteDatabase mDb;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mProvider = spy(new SettingsSliceProvider());
        mProvider.mSliceDataCache = new HashMap<>();
        mProvider.mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(mContext);
        when(mProvider.getContext()).thenReturn(mContext);

        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testInitialSliceReturned_emptySlice() {
        insertSpecialCase(KEY);
        Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);
        Slice slice = mProvider.onBindSlice(uri);

        assertThat(slice.getUri()).isEqualTo(uri);
        assertThat(slice.getItems()).isEmpty();
    }

    @Test
    public void testLoadSlice_returnsSliceFromAccessor() {
        insertSpecialCase(KEY);
        Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);

        mProvider.loadSlice(uri);
        SliceData data = mProvider.mSliceDataCache.get(uri);

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
    }

    @Test
    public void testLoadSlice_cachedEntryRemovedOnBuild() {
        SliceData data = getDummyData();
        mProvider.mSliceDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());
        insertSpecialCase(data.getKey());

        SliceData cachedData = mProvider.mSliceDataCache.get(data.getUri());

        assertThat(cachedData).isNull();
    }

    @Test
    public void getDescendantUris_fullActionUri_returnsSelf() {
        final Uri uri = SliceBuilderUtils.getUri(
                SettingsSlicesContract.PATH_SETTING_ACTION + "/key", true);

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(uri);
    }

    @Test
    public void getDescendantUris_fullIntentUri_returnsSelf() {
        final Uri uri = SliceBuilderUtils.getUri(
                SettingsSlicesContract.PATH_SETTING_ACTION + "/key", true);

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(uri);
    }

    @Test
    public void getDescendantUris_wrongPath_returnsEmpty() {
        final Uri uri = SliceBuilderUtils.getUri("invalid_path", true);

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_invalidPath_returnsEmpty() {
        final String key = "platform_key";
        insertSpecialCase(key, true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath("invalid")
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_platformSlice_doesNotReturnOEMSlice() {
        insertSpecialCase("oem_key", false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_doesNotReturnPlatformSlice() {
        insertSpecialCase("platform_key", true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_returnsOEMUriDescendant() {
        final String key = "oem_key";
        insertSpecialCase(key, false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(expectedUri);
    }

    @Test
    public void getDescendantUris_oemSliceNoPath_returnsOEMUriDescendant() {
        final String key = "oem_key";
        insertSpecialCase(key, false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(expectedUri);
    }

    @Test
    public void getDescendantUris_platformSlice_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        insertSpecialCase(key, true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(expectedUri);
    }

    @Test
    public void getDescendantUris_platformSliceNoPath_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        insertSpecialCase(key, true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .build();
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(expectedUri);
    }

    @Test
    public void getDescendantUris_noAuthorityNorPath_returnsAllUris() {
        final String platformKey = "platform_key";
        final String oemKey = "oemKey";
        insertSpecialCase(platformKey, true /* isPlatformSlice */);
        insertSpecialCase(oemKey, false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .build();
        final Uri expectedPlatformUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(platformKey)
                .build();
        final Uri expectedOemUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(oemKey)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(expectedPlatformUri, expectedOemUri);
    }

    private void insertSpecialCase(String key) {
        insertSpecialCase(key, true);
    }

    private void insertSpecialCase(String key, boolean isPlatformSlice) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, "s");
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, "s");
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, 1234);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, "test");
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, "test");
        values.put(SlicesDatabaseHelper.IndexColumns.PLATFORM_SLICE, isPlatformSlice);
        values.put(SlicesDatabaseHelper.IndexColumns.SLICE_TYPE, SliceData.SliceType.INTENT);

        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }

    private SliceData getDummyData() {
        return new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .build();
    }
}