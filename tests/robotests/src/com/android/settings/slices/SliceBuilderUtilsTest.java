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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.Pair;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeCopyableController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeSliderController;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.FakeUnavailablePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceBuilderUtilsTest {

    private final String KEY = "KEY";
    private final String TITLE = "title";
    private final String SUMMARY = "summary";
    private final String SCREEN_TITLE = "screen title";
    private final String KEYWORDS = "a, b, c";
    private final String FRAGMENT_NAME = "fragment name";
    private final int ICON = R.drawable.ic_settings;
    private final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private final Class TOGGLE_CONTROLLER = FakeToggleController.class;
    private final Class SLIDER_CONTROLLER = FakeSliderController.class;
    private final Class COPYABLE_CONTROLLER = FakeCopyableController.class;
    private final Class CONTEXT_CONTROLLER = FakeContextOnlyPreferenceController.class;
    private final boolean IS_DYNAMIC_SUMMARY_ALLOWED = false;

    private final String INTENT_PATH = SettingsSlicesContract.PATH_SETTING_INTENT + "/" + KEY;
    private final String ACTION_PATH = SettingsSlicesContract.PATH_SETTING_ACTION + "/" + KEY;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private ArgumentCaptor<Pair<Integer, Object>> mLoggingArgumentCatpor;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mLoggingArgumentCatpor = ArgumentCaptor.forClass(Pair.class);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void buildIntentSlice_returnsMatchingSlice() {
        final SliceData sliceData = getDummyData(CONTEXT_CONTROLLER, SliceData.SliceType.INTENT);
        final Slice slice = SliceBuilderUtils.buildSlice(mContext, sliceData);

        SliceTester.testSettingsIntentSlice(mContext, slice, sliceData);
    }

    @Test
    public void buildToggleSlice_returnsMatchingSlice() {
        final SliceData dummyData = getDummyData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, dummyData);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());
        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(dummyData.getKey());
        SliceTester.testSettingsToggleSlice(mContext, slice, dummyData);
    }

    @Test
    public void buildSliderSlice_returnsMatchingSlice() {
        final SliceData data = getDummyData(SLIDER_CONTROLLER, SliceData.SliceType.SLIDER);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());
        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(data.getKey());
        SliceTester.testSettingsSliderSlice(mContext, slice, data);
    }

    @Test
    public void buildCopyableSlice_returnsMatchingSlice() {
        final SliceData dummyData = getDummyData(COPYABLE_CONTROLLER, -1);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, dummyData);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());
        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(dummyData.getKey());
        SliceTester.testSettingsCopyableSlice(mContext, slice, dummyData);
    }

    @Test
    public void testUriBuilder_oemAuthority_intentPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(INTENT_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(INTENT_PATH, false);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testUriBuilder_oemAuthority_actionPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(ACTION_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(ACTION_PATH, false);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testUriBuilder_platformAuthority_intentPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(ACTION_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(ACTION_PATH, true);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testUriBuilder_platformAuthority_actionPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(ACTION_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(ACTION_PATH, true);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testGetPreferenceController_buildsMatchingController() {
        final BasePreferenceController controller =
                SliceBuilderUtils.getPreferenceController(mContext, getDummyData());

        assertThat(controller).isInstanceOf(FakeToggleController.class);
    }

    @Test
    public void testGetPreferenceController_contextOnly_buildsMatchingController() {
        final BasePreferenceController controller =
                SliceBuilderUtils.getPreferenceController(mContext,
                        getDummyData(CONTEXT_CONTROLLER, 0));

        assertThat(controller).isInstanceOf(FakeContextOnlyPreferenceController.class);
    }

    @Test
    public void getDynamicSummary_allowDynamicSummary_returnsControllerSummary() {
        final SliceData data = getDummyData(true /*isDynamicSummaryAllowed*/);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getDynamicSummary_returnsScreenTitle() {
        final SliceData data = getDummyData();
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(data.getScreenTitle());
    }

    @Test
    public void getDynamicSummary_noScreenTitle_returnsPrefControllerSummary() {
        final SliceData data = getDummyData("", "");
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getDynamicSummary_screenTitleMatchesTitle_returnsPrefControllerSummary() {
        final SliceData data = getDummyData("", TITLE);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getDynamicSummary_emptyScreenTitle_emptyControllerSummary_returnsEmptyString() {
        final SliceData data = getDummyData(null, null);
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);
        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo("");
    }

    @Test
    public void
    getDynamicSummary_emptyScreenTitle_placeHolderControllerSummary_returnsEmptyString() {
        final SliceData data = getDummyData(mContext.getString(R.string.summary_placeholder), null);
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);
        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo("");
    }

    @Test
    public void getDynamicSummary_screenTitleAndControllerPlaceholder_returnsSliceEmptyString() {
        final String summaryPlaceholder = mContext.getString(R.string.summary_placeholder);
        final SliceData data = getDummyData(summaryPlaceholder, summaryPlaceholder);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        doReturn(summaryPlaceholder).when(controller).getSummary();

        CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo("");
    }

    @Test
    public void getPathData_splitsIntentUri() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isTrue();
        assertThat(pathPair.second).isEqualTo(KEY);
    }

    @Test
    public void getPathData_splitsActionUri() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isFalse();
        assertThat(pathPair.second).isEqualTo(KEY);
    }

    @Test
    public void getPathData_noKey_returnsNull() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair).isNull();
    }

    @Test
    public void getPathData_extraArg_returnsNull() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isFalse();
        assertThat(pathPair.second).isEqualTo(KEY + "/" + KEY);
    }

    @Test
    public void testUnsupportedSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.SWITCH);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNull();
    }

    @Test
    public void testDisabledForUserSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.SWITCH);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_FOR_USER);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNull();
    }

    @Test
    public void testDisabledDependentSettingSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.INTENT);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());
        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(data.getKey());
        SliceTester.testSettingsUnavailableSlice(mContext, slice, data);
    }

    @Test
    public void testConditionallyUnavailableSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.SWITCH);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());

        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(data.getKey());
        assertThat(slice).isNull();
    }

    @Test
    public void testContentIntent_includesUniqueData() {
        final SliceData sliceData = getDummyData();
        final Uri expectedUri = new Uri.Builder().appendPath(sliceData.getKey()).build();

        final Intent intent = SliceBuilderUtils.getContentIntent(mContext, sliceData);
        final Uri intentData = intent.getData();

        assertThat(intentData).isEqualTo(expectedUri);
    }

    @Test
    public void buildIntentSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings).toIcon().getResId();
        final SliceData sliceData = getDummyData(CONTEXT_CONTROLLER, SliceData.SliceType.INTENT,
                0x0);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, sliceData);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void buildToggleSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings).toIcon().getResId();
        final SliceData dummyData = getDummyData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                0x0);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, dummyData);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void buildSliderSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings).toIcon().getResId();
        final SliceData data = getDummyData(SLIDER_CONTROLLER, SliceData.SliceType.SLIDER, 0x0);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void getSafeIcon_replacesEmptyIconWithSettingsIcon() {
        final int settingsIcon = R.drawable.ic_settings;
        final int zeroIcon = 0x0;
        final SliceData data = getDummyData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                zeroIcon);

        final IconCompat actualIcon = SliceBuilderUtils.getSafeIcon(mContext, data);

        final int actualIconResource = actualIcon.toIcon().getResId();
        assertThat(actualIconResource).isNotEqualTo(zeroIcon);
        assertThat(actualIconResource).isEqualTo(settingsIcon);
    }

    private SliceData getDummyData() {
        return getDummyData(TOGGLE_CONTROLLER, SUMMARY, SliceData.SliceType.SWITCH, SCREEN_TITLE,
                ICON, IS_DYNAMIC_SUMMARY_ALLOWED);
    }

    private SliceData getDummyData(boolean isDynamicSummaryAllowed) {
        return getDummyData(TOGGLE_CONTROLLER, SUMMARY, SliceData.SliceType.SWITCH, SCREEN_TITLE,
                ICON, isDynamicSummaryAllowed);
    }

    private SliceData getDummyData(Class prefController, int sliceType, int icon) {
        return getDummyData(TOGGLE_CONTROLLER, SUMMARY, SliceData.SliceType.SWITCH, SCREEN_TITLE,
                icon, IS_DYNAMIC_SUMMARY_ALLOWED);
    }

    private SliceData getDummyData(String summary, String screenTitle) {
        return getDummyData(TOGGLE_CONTROLLER, summary, SliceData.SliceType.SWITCH, screenTitle,
                ICON, IS_DYNAMIC_SUMMARY_ALLOWED);
    }

    private SliceData getDummyData(Class prefController, int sliceType) {
        return getDummyData(prefController, SUMMARY, sliceType, SCREEN_TITLE, ICON,
                IS_DYNAMIC_SUMMARY_ALLOWED);
    }

    private SliceData getDummyData(Class prefController, String summary, int sliceType,
            String screenTitle, int icon, boolean isDynamicSummaryAllowed) {
        return new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(summary)
                .setScreenTitle(screenTitle)
                .setKeywords(KEYWORDS)
                .setIcon(icon)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(prefController.getName())
                .setSliceType(sliceType)
                .setDynamicSummaryAllowed(isDynamicSummaryAllowed)
                .build();
    }
}
