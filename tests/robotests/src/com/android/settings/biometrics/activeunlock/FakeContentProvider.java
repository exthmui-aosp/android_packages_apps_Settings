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

package com.android.settings.biometrics.activeunlock;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.android.settings.testutils.ActiveUnlockTestUtils;

/** ContentProvider to provider tile summary for ActiveUnlock in tests. */
public final class FakeContentProvider extends ContentProvider {
    public static final String AUTHORITY = ActiveUnlockTestUtils.PROVIDER;
    public static final Uri URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath("getSummary")
            .build();
    public static final String METHOD_SUMMARY = "getSummary";
    public static final String KEY_SUMMARY = "com.android.settings.summary";
    private static final String METHOD_DEVICE_NAME = "getDeviceName";
    private static final String KEY_DEVICE_NAME = "com.android.settings.active_unlock.device_name";
    @Nullable private static String sTileSummary;
    @Nullable private static String sDeviceName;

    public FakeContentProvider() {
        super();
    }

    public static void setTileSummary(String summary) {
        sTileSummary = summary;
    }

    public static void setDeviceName(String deviceName) {
        sDeviceName = deviceName;
    }

    public static void init(Context context) {
        Settings.Secure.putString(
                context.getContentResolver(), ActiveUnlockTestUtils.PROVIDER_SETTING, AUTHORITY);
        sTileSummary = null;
        sDeviceName = null;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle bundle = new Bundle();
        if (METHOD_SUMMARY.equals(method)) {
            bundle.putCharSequence(KEY_SUMMARY, sTileSummary);
        } else if (METHOD_DEVICE_NAME.equals(method)) {
            bundle.putCharSequence(KEY_DEVICE_NAME, sDeviceName);
        }
        return bundle;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
