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
 *
 */

package com.android.settings.search;

import static com.android.settings.search.DatabaseResultLoader.BASE_RANKS;
import static com.android.settings.search.SearchResult.TOP_RANK;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.BadParcelableException;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Controller to Build search results from {@link Cursor} Objects.
 *
 * Each converted {@link Cursor} has the following fields:
 * - String Title
 * - String Summary
 * - int rank
 * - {@link Drawable} icon
 * - {@link ResultPayload} payload
 */
public class CursorToSearchResultConverter {

    private static final String TAG = "CursorConverter";

    /**
     * These indices are used to match the columns of the this loader's SELECT statement.
     * These are not necessarily the same order nor similar coverage as the schema defined in
     * IndexDatabaseHelper
     */
    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_SUMMARY_ON = 2;
    public static final int COLUMN_INDEX_SUMMARY_OFF = 3;
    public static final int COLUMN_INDEX_CLASS_NAME = 4;
    public static final int COLUMN_INDEX_SCREEN_TITLE = 5;
    public static final int COLUMN_INDEX_ICON = 6;
    public static final int COLUMN_INDEX_INTENT_ACTION = 7;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE = 8;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS = 9;
    public static final int COLUMN_INDEX_KEY = 10;
    public static final int COLUMN_INDEX_PAYLOAD_TYPE = 11;
    public static final int COLUMN_INDEX_PAYLOAD = 12;

    private final Context mContext;

    private final int LONG_TITLE_LENGTH = 20;

    private static final String[] whiteList = {
            "main_toggle_wifi",
            "main_toggle_bluetooth",
            "main_toggle_bluetooth_obsolete",
            "toggle_airplane",
            "tether_settings",
            "battery_saver",
            "toggle_nfc",
            "restrict_background",
            "data_usage_enable",
            "button_roaming_key",
    };
    private static final Set<String> prioritySettings = new HashSet(Arrays.asList(whiteList));


    public CursorToSearchResultConverter(Context context) {
        mContext = context;
    }

    public Set<SearchResult> convertCursor(Cursor cursorResults, int baseRank) {
        if (cursorResults == null) {
            return null;
        }
        final Map<String, Context> contextMap = new HashMap<>();
        final Set<SearchResult> results = new HashSet<>();

        while (cursorResults.moveToNext()) {
            SearchResult result = buildSingleSearchResultFromCursor(contextMap, cursorResults,
                    baseRank);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    public static ResultPayload getUnmarshalledPayload(byte[] marshalledPayload,
            int payloadType) {
        try {
            switch (payloadType) {
                case ResultPayload.PayloadType.INTENT:
                    return ResultPayloadUtils.unmarshall(marshalledPayload,
                            ResultPayload.CREATOR);
                case ResultPayload.PayloadType.INLINE_SWITCH:
                    return ResultPayloadUtils.unmarshall(marshalledPayload,
                            InlineSwitchPayload.CREATOR);
                case ResultPayload.PayloadType.INLINE_LIST:
                    return ResultPayloadUtils.unmarshall(marshalledPayload,
                            InlineListPayload.CREATOR);
            }
        } catch (BadParcelableException e) {
            Log.w(TAG, "Error creating parcelable: " + e);
        }
        return null;
    }

    private SearchResult buildSingleSearchResultFromCursor(Map<String, Context> contextMap,
            Cursor cursor, int baseRank) {
        final int docId = cursor.getInt(COLUMN_INDEX_ID);
        final String pkgName = cursor.getString(COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
        final String title = cursor.getString(COLUMN_INDEX_TITLE);
        final String summaryOn = cursor.getString(COLUMN_INDEX_SUMMARY_ON);
        final String className = cursor.getString(COLUMN_INDEX_CLASS_NAME);
        final String key = cursor.getString(COLUMN_INDEX_KEY);
        final String iconResStr = cursor.getString(COLUMN_INDEX_ICON);
        final int payloadType = cursor.getInt(COLUMN_INDEX_PAYLOAD_TYPE);
        final byte[] marshalledPayload = cursor.getBlob(COLUMN_INDEX_PAYLOAD);
        final ResultPayload payload = getUnmarshalledPayload(marshalledPayload, payloadType);

        final int rank = getRank(title, baseRank, key);

        final SearchResult.Builder builder = new SearchResult.Builder()
                .setStableId(docId)
                .setTitle(title)
                .setSummary(summaryOn)
                .setRank(rank)
                .setIcon(getIconForPackage(contextMap, pkgName, className, iconResStr))
                .setPayload(payload);
        return builder.build();
    }

    private Drawable getIconForPackage(Map<String, Context> contextMap, String pkgName,
            String className, String iconResStr) {
        final int iconId = TextUtils.isEmpty(iconResStr)
                ? 0 : Integer.parseInt(iconResStr);
        Drawable icon;
        Context packageContext;
        if (iconId == 0) {
            icon = null;
        } else {
            if (TextUtils.isEmpty(className) && !TextUtils.isEmpty(pkgName)) {
                packageContext = contextMap.get(pkgName);
                if (packageContext == null) {
                    try {
                        packageContext = mContext.createPackageContext(pkgName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Cannot create Context for package: " + pkgName);
                        return null;
                    }
                    contextMap.put(pkgName, packageContext);
                }
            } else {
                packageContext = mContext;
            }
            try {
                icon = packageContext.getDrawable(iconId);
            } catch (Resources.NotFoundException nfe) {
                icon = null;
            }
        }
        return icon;
    }


    /** Uses the breadcrumbs to determine the offset to the base rank.
     *  There are three checks
     *  A) If the result is prioritized and the highest base level
     *  B) If the query matches the highest level menu title
     *  C) If the query is longer than 20
     *
     *  If the query matches A, set it to TOP_RANK
     *  If the query matches B, the offset is 0.
     *  If the query matches C, the offset is 1

     * @param title of the result.
     * @param baseRank of the result. Lower if it's a better result.
     * @return
     */
    private int getRank(String title, int baseRank, String key) {
        // The result can only be prioritized if it is a top ranked result.
        if (prioritySettings.contains(key) && baseRank < BASE_RANKS[1]) {
            return TOP_RANK;
        }
        if (title.length() > LONG_TITLE_LENGTH) {
            return baseRank + 1;
        }
        return baseRank;
    }
}