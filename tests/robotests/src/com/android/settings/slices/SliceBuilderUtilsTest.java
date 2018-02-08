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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.android.settings.TestConfig.SDK_VERSION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import androidx.app.slice.Slice;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = SDK_VERSION)
public class SliceBuilderUtilsTest {

    private final String KEY = "KEY";
    private final String TITLE = "title";
    private final String SUMMARY = "summary";
    private final String SCREEN_TITLE = "screen title";
    private final String FRAGMENT_NAME = "fragment name";
    private final int ICON = 1234; // I declare a thumb war
    private final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private final Class PREF_CONTROLLER = FakeToggleController.class;
    private final Class PREF_CONTROLLER2 = FakeContextOnlyPreferenceController.class;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testBuildSlice_returnsMatchingSlice() {
        Slice slice = SliceBuilderUtils.buildSlice(mContext, getDummyData());

        assertThat(slice).isNotNull(); // TODO improve test for Slice content
    }

    @Test
    public void testGetPreferenceController_buildsMatchingController() {
        BasePreferenceController controller = SliceBuilderUtils.getPreferenceController(
                mContext, getDummyData());

        assertThat(controller).isInstanceOf(FakeToggleController.class);
    }

    @Test
    public void testGetPreferenceController_contextOnly_buildsMatchingController() {
        BasePreferenceController controller = SliceBuilderUtils.getPreferenceController(mContext,
                getDummyData(PREF_CONTROLLER2));

        assertThat(controller).isInstanceOf(FakeContextOnlyPreferenceController.class);
    }

    @Test
    public void testDynamicSummary_returnsSliceSummary() {
        SliceData data = getDummyData();
        FakePreferenceController controller = new FakePreferenceController(mContext, KEY);

        String summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(data.getSummary());
    }

    @Test
    public void testDynamicSummary_returnsFragmentSummary() {
        SliceData data = getDummyData((String) null);
        FakePreferenceController controller = spy(new FakePreferenceController(mContext, KEY));
        String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        String summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void testDynamicSummary_returnsSliceScreenTitle() {
        SliceData data = getDummyData((String) null);
        FakePreferenceController controller = new FakePreferenceController(mContext, KEY);

        String summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(data.getScreenTitle());
    }

    @Test
    public void testDynamicSummary_placeHolderString_returnsScreenTitle() {
        SliceData data = getDummyData(mContext.getString(R.string.summary_placeholder));
        FakePreferenceController controller = new FakePreferenceController(mContext, KEY);
        String summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(data.getScreenTitle());
    }

    @Test
    public void testDynamicSummary_sliceDataAndFragmentPlaceholder_returnsSliceScreenTitle() {
        String summaryPlaceholder = mContext.getString(R.string.summary_placeholder);
        SliceData data = getDummyData(summaryPlaceholder);
        FakePreferenceController controller = spy(new FakePreferenceController(mContext, KEY));
        doReturn(summaryPlaceholder).when(controller).getSummary();

        String summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(data.getScreenTitle());
    }

    private SliceData getDummyData() {
        return getDummyData(PREF_CONTROLLER, SUMMARY);
    }

    private SliceData getDummyData(String summary) {
        return getDummyData(PREF_CONTROLLER, summary);
    }

    private SliceData getDummyData(Class prefController) {
        return getDummyData(prefController, SUMMARY);
    }

    private SliceData getDummyData(Class prefController, String summary) {
        return new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(summary)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(prefController.getName())
                .build();
    }
}
