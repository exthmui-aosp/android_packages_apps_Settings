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

package com.android.settings.core;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.XmlRes;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to parse elements of XML preferences
 */
public class PreferenceXmlParserUtils {

    private static final String TAG = "PreferenceXmlParserUtil";
    @VisibleForTesting
    static final String PREF_SCREEN_TAG = "PreferenceScreen";
    private static final List<String> SUPPORTED_PREF_TYPES = Arrays.asList(
            "Preference", "PreferenceCategory", "PreferenceScreen");

    /**
     * Flag definition to indicate which metadata should be extracted when
     * {@link #extractMetadata(Context, int, int)} is called. The flags can be combined by using |
     * (binary or).
     */
    @IntDef(flag = true, value = {
            MetadataFlag.FLAG_INCLUDE_PREF_SCREEN,
            MetadataFlag.FLAG_NEED_KEY,
            MetadataFlag.FLAG_NEED_PREF_TYPE,
            MetadataFlag.FLAG_NEED_PREF_CONTROLLER,
            MetadataFlag.FLAG_NEED_PREF_TITLE,
            MetadataFlag.FLAG_NEED_PREF_SUMMARY,
            MetadataFlag.FLAG_NEED_PREF_ICON})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetadataFlag {
        int FLAG_INCLUDE_PREF_SCREEN = 1;
        int FLAG_NEED_KEY = 1 << 1;
        int FLAG_NEED_PREF_TYPE = 1 << 2;
        int FLAG_NEED_PREF_CONTROLLER = 1 << 3;
        int FLAG_NEED_PREF_TITLE = 1 << 4;
        int FLAG_NEED_PREF_SUMMARY = 1 << 5;
        int FLAG_NEED_PREF_ICON = 1 << 6;
    }

    public static final String METADATA_PREF_TYPE = "type";
    public static final String METADATA_KEY = "key";
    public static final String METADATA_CONTROLLER = "controller";
    public static final String METADATA_TITLE = "title";
    public static final String METADATA_SUMMARY = "summary";
    public static final String METADATA_ICON = "icon";

    private static final String ENTRIES_SEPARATOR = "|";

    public static String getDataKey(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_key);
    }

    public static String getDataTitle(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_title);
    }

    public static String getDataSummary(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_summary);
    }

    public static String getDataSummaryOn(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.CheckBoxPreference,
                com.android.internal.R.styleable.CheckBoxPreference_summaryOn);
    }

    public static String getDataSummaryOff(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.CheckBoxPreference,
                com.android.internal.R.styleable.CheckBoxPreference_summaryOff);
    }

    public static String getDataEntries(Context context, AttributeSet attrs) {
        return getDataEntries(context, attrs,
                com.android.internal.R.styleable.ListPreference,
                com.android.internal.R.styleable.ListPreference_entries);
    }

    public static String getDataKeywords(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, R.styleable.Preference_keywords);
    }

    public static String getController(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, R.styleable.Preference_controller);
    }

    public static int getDataIcon(Context context, AttributeSet attrs) {
        final TypedArray ta = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.Preference);
        final int dataIcon = ta.getResourceId(com.android.internal.R.styleable.Icon_icon, 0);
        ta.recycle();
        return dataIcon;
    }

    /**
     * Extracts metadata from preference xml and put them into a {@link Bundle}.
     *
     * @param xmlResId xml res id of a preference screen
     * @param flags    Should be one or more of {@link MetadataFlag}.
     */
    @NonNull
    public static List<Bundle> extractMetadata(Context context, @XmlRes int xmlResId, int flags)
            throws IOException, XmlPullParserException {
        final List<Bundle> metadata = new ArrayList<>();
        if (xmlResId <= 0) {
            Log.d(TAG, xmlResId + " is invalid.");
            return metadata;
        }
        final XmlResourceParser parser = context.getResources().getXml(xmlResId);

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && type != XmlPullParser.START_TAG) {
            // Parse next until start tag is found
        }
        final int outerDepth = parser.getDepth();

        do {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            final String nodeName = parser.getName();
            if (!hasFlag(flags, MetadataFlag.FLAG_INCLUDE_PREF_SCREEN)
                    && TextUtils.equals(PREF_SCREEN_TAG, nodeName)) {
                continue;
            }
            if (!SUPPORTED_PREF_TYPES.contains(nodeName) && !nodeName.endsWith("Preference")) {
                continue;
            }
            final Bundle preferenceMetadata = new Bundle();
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            if (hasFlag(flags, MetadataFlag.FLAG_NEED_PREF_TYPE)) {
                preferenceMetadata.putString(METADATA_PREF_TYPE, nodeName);
            }
            if (hasFlag(flags, MetadataFlag.FLAG_NEED_KEY)) {
                preferenceMetadata.putString(METADATA_KEY, getDataKey(context, attrs));
            }
            if (hasFlag(flags, MetadataFlag.FLAG_NEED_PREF_CONTROLLER)) {
                preferenceMetadata.putString(METADATA_CONTROLLER, getController(context, attrs));
            }
            if (hasFlag(flags, MetadataFlag.FLAG_NEED_PREF_TITLE)) {
                preferenceMetadata.putString(METADATA_TITLE, getDataTitle(context, attrs));
            }
            if (hasFlag(flags, MetadataFlag.FLAG_NEED_PREF_SUMMARY)) {
                preferenceMetadata.putString(METADATA_SUMMARY, getDataSummary(context, attrs));
            }
            if (hasFlag(flags, MetadataFlag.FLAG_NEED_PREF_ICON)) {
                preferenceMetadata.putInt(METADATA_ICON, getDataIcon(context, attrs));
            }
            metadata.add(preferenceMetadata);
        } while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth));
        parser.close();
        return metadata;
    }

    /**
     * Returns the fragment name if this preference launches a child fragment.
     */
    public static String getDataChildFragment(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference,
                R.styleable.Preference_android_fragment);
    }

    @Nullable
    private static String getData(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray ta = context.obtainStyledAttributes(set, attrs);
        String data = ta.getString(resId);
        ta.recycle();
        return data;
    }

    private static boolean hasFlag(int flags, @MetadataFlag int flag) {
        return (flags & flag) != 0;
    }

    private static String getDataEntries(Context context, AttributeSet set, int[] attrs,
            int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);
        sa.recycle();
        String[] data = null;
        if (tv != null && tv.type == TypedValue.TYPE_REFERENCE) {
            if (tv.resourceId != 0) {
                data = context.getResources().getStringArray(tv.resourceId);
            }
        }
        final int count = (data == null) ? 0 : data.length;
        if (count == 0) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        for (int n = 0; n < count; n++) {
            result.append(data[n]);
            result.append(ENTRIES_SEPARATOR);
        }
        return result.toString();
    }
}
