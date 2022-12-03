/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.bugreport;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.settings.fuelgauge.batteryusage.DatabaseUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Provides debug information in the bugreports when device is dumpping. */
public final class BugReportContentProvider extends ContentProvider {
    private static final String TAG = "BugReportContentProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final Context context = getContext();
        if (context == null) {
            Log.w(TAG, "failed to dump BatteryUsage state: null context");
            return;
        }
        if (DatabaseUtils.isWorkProfile(context)) {
            Log.w(TAG, "ignore battery usage states dump in the work profile");
            return;
        }
        writer.println("dump BatteryUsage states:");
        LogUtils.dumpUsageDatabaseHist(context, writer);
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("unsupported!");
    }
}
