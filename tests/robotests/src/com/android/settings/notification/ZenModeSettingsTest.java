/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.assertEquals;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZenModeSettingsTest {

    private ZenModeSettings.SummaryBuilder mBuilder;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mBuilder = new ZenModeSettings.SummaryBuilder(mContext);
    }

    @Test
    public void testGetBehaviorSettingSummary_noSoundsCanBypass() {
        NotificationManager.Policy policy = new NotificationManager.Policy(0, 0, 0);
        final String result = mBuilder.getBehaviorSettingSummary(policy,
                Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        String totalSilence = mContext.getString(R.string.zen_mode_no_exceptions);
        assertEquals(totalSilence, result);
    }

    @Test
    public void testGetBehaviorSettingSummary_alarmsAndMedia() {
        NotificationManager.Policy policy = new NotificationManager.Policy(
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
                        | NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA,
                0, 0);
        final String result = mBuilder.getBehaviorSettingSummary(policy,
                Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        String alarmsAndMedia = mContext.getString(R.string.join_two_items,
                mContext.getString(R.string.zen_mode_alarms),
                mContext.getString(R.string.zen_mode_media).toLowerCase());
        assertEquals(alarmsAndMedia, result);
    }

    @Test
    public void testBlockedEffectsSummary_none() {
        NotificationManager.Policy policy = new NotificationManager.Policy(0, 0, 0, 0);
        assertEquals("Never", mBuilder.getBlockedEffectsSummary(policy));
    }

    @Test
    public void testBlockedEffectsSummary_screen_on() {
        NotificationManager.Policy policy = new NotificationManager.Policy(
                0, 0, 0, NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK);
        assertEquals("When screen is on", mBuilder.getBlockedEffectsSummary(policy));
    }

    @Test
    public void testBlockedEffectsSummary_screen_off() {
        NotificationManager.Policy policy = new NotificationManager.Policy(
                0, 0, 0, NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT);
        assertEquals("When screen is off", mBuilder.getBlockedEffectsSummary(policy));
    }

    @Test
    public void testBlockedEffectsSummary_both() {
        NotificationManager.Policy policy = new NotificationManager.Policy(0, 0, 0,
                NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST
                        | NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS);
        assertEquals("When screen is off, When screen is on",
                mBuilder.getBlockedEffectsSummary(policy));
    }

    @Test
    public void searchProvider_shouldIndexDefaultXml() {
        final List<SearchIndexableResource> sir = ZenModeSettings.SEARCH_INDEX_DATA_PROVIDER
                .getXmlResourcesToIndex(mContext, true /* enabled */);

        assertThat(sir).hasSize(1);
        assertThat(sir.get(0).xmlResId).isEqualTo(R.xml.zen_mode_settings);
    }
}
