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
 */

package com.android.settings.homepage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

/**
 * Provider stores and manages user interaction feedback for homepage contextual cards.
 */
public class CardContentProvider extends ContentProvider {

    private static final String TAG = "CardContentProvider";

    public static final String CARD_AUTHORITY = "com.android.settings.homepage.CardContentProvider";

    /** URI matcher for ContentProvider queries. */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    /** URI matcher type for cards table */
    private static final int MATCH_CARDS = 100;
    /** URI matcher type for card log table */
    private static final int MATCH_CARD_LOG = 200;

    static {
        sUriMatcher.addURI(CARD_AUTHORITY, CardDatabaseHelper.CARD_TABLE, MATCH_CARDS);
    }

    private CardDatabaseHelper mDBHelper;

    @Override
    public boolean onCreate() {
        mDBHelper = CardDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            if (Build.IS_DEBUGGABLE) {
                enableStrictMode(true);
            }

            final SQLiteDatabase database = mDBHelper.getWritableDatabase();
            final String table = getTableFromMatch(uri);
            final long ret = database.insert(table, null, values);
            if (ret != -1) {
                getContext().getContentResolver().notifyChange(uri, null);
            } else {
                Log.e(TAG, "The CardContentProvider insertion failed! Plase check SQLiteDatabase's "
                        + "message.");
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            if (Build.IS_DEBUGGABLE) {
                enableStrictMode(true);
            }

            final SQLiteDatabase database = mDBHelper.getWritableDatabase();
            final String table = getTableFromMatch(uri);
            final int rowsDeleted = database.delete(table, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return rowsDeleted;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("getType operation not supported currently.");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            if (Build.IS_DEBUGGABLE) {
                enableStrictMode(true);
            }

            final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            final String table = getTableFromMatch(uri);
            queryBuilder.setTables(table);
            final SQLiteDatabase database = mDBHelper.getReadableDatabase();
            final Cursor cursor = queryBuilder.query(database,
                    projection, selection, selectionArgs, null, null, sortOrder);

            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            if (Build.IS_DEBUGGABLE) {
                enableStrictMode(true);
            }

            final SQLiteDatabase database = mDBHelper.getWritableDatabase();
            final String table = getTableFromMatch(uri);
            final int rowsUpdated = database.update(table, values, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return rowsUpdated;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void enableStrictMode(boolean enabled) {
        StrictMode.setThreadPolicy(enabled
                ? new StrictMode.ThreadPolicy.Builder().detectAll().build()
                : StrictMode.ThreadPolicy.LAX);
    }

    @VisibleForTesting
    String getTableFromMatch(Uri uri) {
        final int match = sUriMatcher.match(uri);
        String table;
        switch (match) {
            case MATCH_CARDS:
                table = CardDatabaseHelper.CARD_TABLE;
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri format: " + uri);
        }
        return table;
    }
}
