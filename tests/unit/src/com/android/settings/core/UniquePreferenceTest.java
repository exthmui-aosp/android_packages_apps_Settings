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

import static junit.framework.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.platform.test.annotations.Presubmit;
import android.provider.SearchIndexableResource;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.SearchIndexableResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class UniquePreferenceTest {

    private static final String TAG = "UniquePreferenceTest";
    private static final List<String> IGNORE_PREF_TYPES = Arrays.asList(
            "com.android.settingslib.widget.FooterPreference");
    private static final List<String> SUPPORTED_PREF_TYPES = Arrays.asList(
            "Preference", "PreferenceCategory", "PreferenceScreen");
    private static final List<String> WHITELISTED_DUPLICATE_KEYS = Arrays.asList(
            "owner_info_settings",          // Lock screen message in security - multiple xml files
                                            // contain this because security page is constructed by
                                            // combining small xml chunks. Eventually the page
                                            // should be formed as one single xml and this entry
                                            // should be removed.

            "dashboard_tile_placeholder",   // This is the placeholder pref for injecting dynamic
                                            // tiles.
            // Dup keys from connected device page experiment.
            "usb_mode",
            "connected_devices_screen",
            "toggle_bluetooth",
            "toggle_nfc",
            "android_beam_settings",
            // Dup keys from About Phone v2 experiment.
            "ims_reg_state",
            "bt_address",
            "device_model",
            "firmware_version",
            "regulatory_info",
            "manual",
            "legal_container",
            "device_feedback",
            "fcc_equipment_id",
            "sim_status",
            "build_number",
            "phone_number",
            "imei_info",
            "wifi_ip_address",
            "wifi_mac_address",
            "safety_info",
            // Dupe keys from data usage v2.
            "data_usage_screen",
            "cellular_data_usage",
            "data_usage_wifi_screen",
            "status_header",
            "billing_preference",
            "data_usage_cellular_screen",
            "wifi_data_usage",
            "data_usage_enable"
    );

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    /**
     * All preferences should have their unique key. It's especially important for many parts of
     * Settings to work properly: we assume pref keys are unique in displaying, search ranking,\
     * search result suppression, and many other areas.
     * <p/>
     * So in this test we are checking preferences participating in search.
     * <p/>
     * Note: Preference is not limited to just <Preference/> object. Everything in preference xml
     * should have a key.
     */
    @Test
    @Presubmit
    public void allPreferencesShouldHaveUniqueKey()
            throws IOException, XmlPullParserException, Resources.NotFoundException {
        final Set<String> uniqueKeys = new HashSet<>();
        final Set<String> nullKeyClasses = new HashSet<>();
        final Set<String> duplicatedKeys = new HashSet<>();
        final SearchIndexableResources resources =
                FeatureFactory.getFactory(mContext).getSearchFeatureProvider()
                        .getSearchIndexableResources();
        for (Class<?> clazz : resources.getProviderValues()) {
            verifyPreferenceKeys(uniqueKeys, duplicatedKeys, nullKeyClasses, clazz);
        }

        if (!nullKeyClasses.isEmpty()) {
            final StringBuilder nullKeyErrors = new StringBuilder()
                    .append("Each preference/SearchIndexableData must have a key, ")
                    .append("the following classes have null keys:\n");
            for (String c : nullKeyClasses) {
                nullKeyErrors.append(c).append("\n");
            }
            fail(nullKeyErrors.toString());
        }

        if (!duplicatedKeys.isEmpty()) {
            final StringBuilder dupeKeysError = new StringBuilder(
                    "The following keys are not unique\n");
            for (String c : duplicatedKeys) {
                dupeKeysError.append(c).append("\n");
            }
            fail(dupeKeysError.toString());
        }
    }

    private void verifyPreferenceKeys(Set<String> uniqueKeys, Set<String> duplicatedKeys,
            Set<String> nullKeyClasses, Class<?> clazz)
            throws IOException, XmlPullParserException, Resources.NotFoundException {
        if (clazz == null) {
            return;
        }
        final String className = clazz.getName();
        final Indexable.SearchIndexProvider provider =
                DatabaseIndexingUtils.getSearchIndexProvider(clazz);
        final List<SearchIndexableRaw> rawsToIndex = provider.getRawDataToIndex(mContext, true);
        final List<SearchIndexableResource> resourcesToIndex =
                provider.getXmlResourcesToIndex(mContext, true);
        verifyResources(className, resourcesToIndex, uniqueKeys, duplicatedKeys, nullKeyClasses);
        verifyRaws(className, rawsToIndex, uniqueKeys, duplicatedKeys, nullKeyClasses);
    }

    private void verifyResources(String className, List<SearchIndexableResource> resourcesToIndex,
            Set<String> uniqueKeys, Set<String> duplicatedKeys, Set<String> nullKeyClasses)
            throws IOException, XmlPullParserException, Resources.NotFoundException {
        if (resourcesToIndex == null) {
            Log.d(TAG, className + "is not providing SearchIndexableResource, skipping");
            return;
        }

        for (SearchIndexableResource sir : resourcesToIndex) {
            if (sir.xmlResId <= 0) {
                Log.d(TAG, className + " doesn't have a valid xml to index.");
                continue;
            }
            final XmlResourceParser parser = mContext.getResources().getXml(sir.xmlResId);

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
                if (IGNORE_PREF_TYPES.contains(nodeName)) {
                    continue;
                }
                if (!SUPPORTED_PREF_TYPES.contains(nodeName) && !nodeName.endsWith("Preference")) {
                    continue;
                }
                final AttributeSet attrs = Xml.asAttributeSet(parser);
                final String key = PreferenceXmlParserUtils.getDataKey(mContext, attrs);
                if (TextUtils.isEmpty(key)) {
                    Log.e(TAG, "Every preference must have an key; found null key"
                            + " in " + className
                            + " at " + parser.getPositionDescription());
                    nullKeyClasses.add(className);
                    continue;
                }
                if (uniqueKeys.contains(key) && !WHITELISTED_DUPLICATE_KEYS.contains(key)) {
                    Log.e(TAG, "Every preference key must unique; found " + nodeName
                            + " in " + className
                            + " at " + parser.getPositionDescription());
                    duplicatedKeys.add(key);
                }
                uniqueKeys.add(key);
            } while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth));
        }
    }

    private void verifyRaws(String className, List<SearchIndexableRaw> rawsToIndex,
            Set<String> uniqueKeys, Set<String> duplicatedKeys, Set<String> nullKeyClasses) {
        if (rawsToIndex == null) {
            Log.d(TAG, className + "is not providing SearchIndexableRaw, skipping");
            return;
        }
        for (SearchIndexableRaw raw : rawsToIndex) {
            if (TextUtils.isEmpty(raw.key)) {
                Log.e(TAG, "Every SearchIndexableRaw must have an key; found null key"
                        + " in " + className);
                nullKeyClasses.add(className);
                continue;
            }
            if (uniqueKeys.contains(raw.key) && !WHITELISTED_DUPLICATE_KEYS.contains(raw.key)) {
                Log.e(TAG, "Every SearchIndexableRaw key must unique; found " + raw.key
                        + " in " + className);
                duplicatedKeys.add(raw.key);
            }
        }
    }
}
