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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.format.DateUtils;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartPreferenceControllerV2Test {
    private static final String PREF_KEY = "pref_key";
    private static final String PREF_SUMMARY = "fake preference summary";

    @Mock
    private InstrumentedPreferenceFragment mFragment;
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private PreferenceGroup mAppListGroup;
    @Mock
    private Drawable mDrawable;
    @Mock
    private BatteryHistEntry mBatteryHistEntry;
    @Mock
    private BatteryChartViewV2 mDailyChartView;
    @Mock
    private BatteryChartViewV2 mHourlyChartView;
    @Mock
    private PowerGaugePreference mPowerGaugePreference;
    @Mock
    private BatteryUtils mBatteryUtils;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDiffEntry mBatteryDiffEntry;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BatteryChartPreferenceControllerV2 mBatteryChartPreferenceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        org.robolectric.shadows.ShadowSettings.set24HourTimeFormat(false);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        doReturn(new String[]{"com.android.googlequicksearchbox"})
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .getHideApplicationSummary(mContext);
        doReturn(new String[]{"com.android.gms.persistent"})
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .getHideApplicationEntries(mContext);
        mBatteryChartPreferenceController = createController();
        mBatteryChartPreferenceController.mPrefContext = mContext;
        mBatteryChartPreferenceController.mAppListPrefGroup = mAppListGroup;
        mBatteryChartPreferenceController.mDailyChartView = mDailyChartView;
        mBatteryChartPreferenceController.mHourlyChartView = mHourlyChartView;
        mBatteryDiffEntry = new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 1,
                /*backgroundUsageTimeInMs=*/ 2,
                /*consumePower=*/ 3,
                mBatteryHistEntry);
        mBatteryDiffEntry = spy(mBatteryDiffEntry);
        // Adds fake testing data.
        BatteryDiffEntry.sResourceCache.put(
                "fakeBatteryDiffEntryKey",
                new BatteryEntry.NameAndIcon("fakeName", /*icon=*/ null, /*iconId=*/ 1));
    }

    @Test
    public void onDestroy_activityIsChanging_clearBatteryEntryCache() {
        doReturn(true).when(mSettingsActivity).isChangingConfigurations();
        // Ensures the testing environment is correct.
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void onDestroy_activityIsNotChanging_notClearBatteryEntryCache() {
        doReturn(false).when(mSettingsActivity).isChangingConfigurations();
        // Ensures the testing environment is correct.
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        assertThat(BatteryDiffEntry.sResourceCache).isNotEmpty();
    }

    @Test
    public void onDestroy_clearPreferenceCache() {
        // Ensures the testing environment is correct.
        mBatteryChartPreferenceController.mPreferenceCache.put(
                PREF_KEY, mPowerGaugePreference);
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        // Verifies the result after onDestroy.
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).isEmpty();
    }

    @Test
    public void onDestroy_removeAllPreferenceFromPreferenceGroup() {
        mBatteryChartPreferenceController.onDestroy();
        verify(mAppListGroup).removeAll();
    }

    @Test
    public void setBatteryChartViewModel_6Hours() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));

        verify(mDailyChartView, atLeastOnce()).setVisibility(View.GONE);
        verify(mHourlyChartView, atLeastOnce()).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 97, 95),
                List.of("8 am", "10 am", "12 pm"),
                BatteryChartViewModel.SELECTED_INDEX_ALL,
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS));
    }

    @Test
    public void setBatteryChartViewModel_60Hours() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));

        verify(mDailyChartView, atLeastOnce()).setVisibility(View.VISIBLE);
        verify(mHourlyChartView, atLeastOnce()).setVisibility(View.GONE);
        verify(mDailyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 83, 59, 41),
                List.of("Sat", "Sun", "Mon", "Mon"),
                BatteryChartViewModel.SELECTED_INDEX_ALL,
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS));

        reset(mDailyChartView);
        reset(mHourlyChartView);
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 83, 59, 41),
                List.of("Sat", "Sun", "Mon", "Mon"),
                0,
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS));
        verify(mHourlyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 97, 95, 93, 91, 89, 87, 85, 83),
                List.of("8 am", "10 am", "12 pm", "2 pm", "4 pm", "6 pm", "8 pm", "10 pm",
                        "12 am"),
                BatteryChartViewModel.SELECTED_INDEX_ALL,
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS));

        reset(mDailyChartView);
        reset(mHourlyChartView);
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex = 6;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 83, 59, 41),
                List.of("Sat", "Sun", "Mon", "Mon"),
                1,
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS));
        verify(mHourlyChartView).setViewModel(new BatteryChartViewModel(
                List.of(83, 81, 79, 77, 75, 73, 71, 69, 67, 65, 63, 61, 59),
                List.of("12 am", "2 am", "4 am", "6 am", "8 am", "10 am", "12 pm", "2 pm",
                        "4 pm", "6 pm", "8 pm", "10 pm", "12 am"),
                6,
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS));

        reset(mDailyChartView);
        reset(mHourlyChartView);
        mBatteryChartPreferenceController.mDailyChartIndex = 2;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 83, 59, 41),
                List.of("Sat", "Sun", "Mon", "Mon"),
                2,
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS));
        verify(mHourlyChartView).setViewModel(new BatteryChartViewModel(
                List.of(59, 57, 55, 53, 51, 49, 47, 45, 43, 41),
                List.of("12 am", "2 am", "4 am", "6 am", "8 am", "10 am", "12 pm", "2 pm",
                        "4 pm", "6 pm"),
                BatteryChartViewModel.SELECTED_INDEX_ALL,
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS));
    }

    @Test
    public void refreshUi_normalCase_returnTrue() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        assertThat(mBatteryChartPreferenceController.refreshUi()).isTrue();
    }

    @Test
    public void refreshUi_batteryIndexedMapIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(null);
        assertThat(mBatteryChartPreferenceController.refreshUi()).isFalse();
    }

    @Test
    public void refreshUi_dailyChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mDailyChartView = null;
        assertThat(mBatteryChartPreferenceController.refreshUi()).isFalse();
    }

    @Test
    public void refreshUi_hourlyChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mHourlyChartView = null;
        assertThat(mBatteryChartPreferenceController.refreshUi()).isFalse();
    }

    @Test
    public void removeAndCacheAllPrefs_emptyContent_ignoreRemoveAll() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        mBatteryChartPreferenceController.mBatteryUsageMap = createBatteryUsageMap();
        doReturn(0).when(mAppListGroup).getPreferenceCount();

        mBatteryChartPreferenceController.refreshUi();
        verify(mAppListGroup, never()).removeAll();
    }

    @Test
    public void removeAndCacheAllPrefs_buildCacheAndRemoveAllPreference() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        mBatteryChartPreferenceController.mBatteryUsageMap = createBatteryUsageMap();
        doReturn(1).when(mAppListGroup).getPreferenceCount();
        doReturn(mPowerGaugePreference).when(mAppListGroup).getPreference(0);
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(PREF_KEY).when(mPowerGaugePreference).getKey();
        doReturn(mPowerGaugePreference).when(mAppListGroup).findPreference(PREF_KEY);
        // Ensures the testing data is correct.
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).isEmpty();

        mBatteryChartPreferenceController.refreshUi();

        assertThat(mBatteryChartPreferenceController.mPreferenceCache.get(PREF_KEY))
                .isEqualTo(mPowerGaugePreference);
        verify(mAppListGroup).removeAll();
    }

    @Test
    public void addPreferenceToScreen_emptyContent_ignoreAddPreference() {
        mBatteryChartPreferenceController.addPreferenceToScreen(
                new ArrayList<BatteryDiffEntry>());
        verify(mAppListGroup, never()).addPreference(any());
    }

    @Test
    public void addPreferenceToScreen_addPreferenceIntoScreen() {
        final String appLabel = "fake app label";
        doReturn(1).when(mAppListGroup).getPreferenceCount();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(appLabel).when(mBatteryDiffEntry).getAppLabel();
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(null).when(mAppListGroup).findPreference(PREF_KEY);
        doReturn(false).when(mBatteryDiffEntry).validForRestriction();

        mBatteryChartPreferenceController.addPreferenceToScreen(
                Arrays.asList(mBatteryDiffEntry));

        // Verifies the preference cache.
        final PowerGaugePreference pref =
                (PowerGaugePreference) mBatteryChartPreferenceController.mPreferenceCache
                        .get(PREF_KEY);
        assertThat(pref).isNotNull();
        // Verifies the added preference configuration.
        verify(mAppListGroup).addPreference(pref);
        assertThat(pref.getKey()).isEqualTo(PREF_KEY);
        assertThat(pref.getTitle()).isEqualTo(appLabel);
        assertThat(pref.getIcon()).isEqualTo(mDrawable);
        assertThat(pref.getOrder()).isEqualTo(1);
        assertThat(pref.getBatteryDiffEntry()).isSameInstanceAs(mBatteryDiffEntry);
        assertThat(pref.isSingleLineTitle()).isTrue();
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void addPreferenceToScreen_alreadyInScreen_notAddPreferenceAgain() {
        final String appLabel = "fake app label";
        doReturn(1).when(mAppListGroup).getPreferenceCount();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(appLabel).when(mBatteryDiffEntry).getAppLabel();
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(mPowerGaugePreference).when(mAppListGroup).findPreference(PREF_KEY);

        mBatteryChartPreferenceController.addPreferenceToScreen(
                Arrays.asList(mBatteryDiffEntry));

        verify(mAppListGroup, never()).addPreference(any());
    }

    @Test
    public void handlePreferenceTreeClick_notPowerGaugePreference_returnFalse() {
        assertThat(mBatteryChartPreferenceController.handlePreferenceTreeClick(mAppListGroup))
                .isFalse();

        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM);
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM);
    }

    @Test
    public void handlePreferenceTreeClick_forAppEntry_returnTrue() {
        doReturn(false).when(mBatteryHistEntry).isAppEntry();
        doReturn(mBatteryDiffEntry).when(mPowerGaugePreference).getBatteryDiffEntry();

        assertThat(mBatteryChartPreferenceController.handlePreferenceTreeClick(
                mPowerGaugePreference)).isTrue();
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM,
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        /* package name */ "none",
                        /* percentage of total */ 0);
    }

    @Test
    public void handlePreferenceTreeClick_forSystemEntry_returnTrue() {
        mBatteryChartPreferenceController.mBatteryUtils = mBatteryUtils;
        doReturn(true).when(mBatteryHistEntry).isAppEntry();
        doReturn(mBatteryDiffEntry).when(mPowerGaugePreference).getBatteryDiffEntry();

        assertThat(mBatteryChartPreferenceController.handlePreferenceTreeClick(
                mPowerGaugePreference)).isTrue();
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM,
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        /* package name */ "none",
                        /* percentage of total */ 0);
    }

    @Test
    public void setPreferenceSummary_setNullContentIfTotalUsageTimeIsZero() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryChartPreferenceController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ 0,
                        /*backgroundUsageTimeInMs=*/ 0));
        assertThat(pref.getSummary()).isNull();
    }

    @Test
    public void setPreferenceSummary_setBackgroundUsageTimeOnly() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryChartPreferenceController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ 0,
                        /*backgroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS));
        assertThat(pref.getSummary()).isEqualTo("Background: 1 min");
    }

    @Test
    public void setPreferenceSummary_setTotalUsageTimeLessThanAMinute() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryChartPreferenceController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ 100,
                        /*backgroundUsageTimeInMs=*/ 200));
        assertThat(pref.getSummary()).isEqualTo("Total: less than a min");
    }

    @Test
    public void setPreferenceSummary_setTotalTimeIfBackgroundTimeLessThanAMinute() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryChartPreferenceController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS,
                        /*backgroundUsageTimeInMs=*/ 200));
        assertThat(pref.getSummary())
                .isEqualTo("Total: 1 min\nBackground: less than a min");
    }

    @Test
    public void setPreferenceSummary_setTotalAndBackgroundUsageTime() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryChartPreferenceController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS,
                        /*backgroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS));
        assertThat(pref.getSummary()).isEqualTo("Total: 2 min\nBackground: 1 min");
    }

    @Test
    public void setPreferenceSummary_notAllowShownPackage_setSummayAsNull() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);
        final BatteryDiffEntry batteryDiffEntry =
                spy(createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS,
                        /*backgroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS));
        doReturn("com.android.googlequicksearchbox").when(batteryDiffEntry)
                .getPackageName();

        mBatteryChartPreferenceController.setPreferenceSummary(pref, batteryDiffEntry);
        assertThat(pref.getSummary()).isNull();
    }

    @Test
    public void onExpand_expandedIsTrue_addSystemEntriesToPreferenceGroup() {
        doReturn(1).when(mAppListGroup).getPreferenceCount();
        mBatteryChartPreferenceController.mBatteryUsageMap = createBatteryUsageMap();
        doReturn("label").when(mBatteryDiffEntry).getAppLabel();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();

        mBatteryChartPreferenceController.onExpand(/*isExpanded=*/ true);

        final ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        verify(mAppListGroup).addPreference(captor.capture());
        // Verifies the added preference.
        assertThat(captor.getValue().getKey()).isEqualTo(PREF_KEY);
        verify(mMetricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BATTERY_USAGE_EXPAND_ITEM,
                        true /*isExpanded*/);
    }

    @Test
    public void onExpand_expandedIsFalse_removeSystemEntriesFromPreferenceGroup() {
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(mPowerGaugePreference).when(mAppListGroup).findPreference(PREF_KEY);
        mBatteryChartPreferenceController.mBatteryUsageMap = createBatteryUsageMap();
        // Verifies the cache is empty first.
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).isEmpty();

        mBatteryChartPreferenceController.onExpand(/*isExpanded=*/ false);

        verify(mAppListGroup).findPreference(PREF_KEY);
        verify(mAppListGroup).removePreference(mPowerGaugePreference);
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).hasSize(1);
        verify(mMetricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BATTERY_USAGE_EXPAND_ITEM,
                        false /*isExpanded*/);
    }

    @Test
    public void refreshCategoryTitle_setLastFullChargeIntoBothTitleTextView() {
        mBatteryChartPreferenceController = createController();
        mBatteryChartPreferenceController.mAppListPrefGroup =
                spy(new PreferenceCategory(mContext));
        mBatteryChartPreferenceController.mExpandDividerPreference =
                spy(new ExpandDividerPreference(mContext));
        // Simulates select all condition.
        mBatteryChartPreferenceController.mDailyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        mBatteryChartPreferenceController.refreshCategoryTitle();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        // Verifies the title in the preference group.
        verify(mBatteryChartPreferenceController.mAppListPrefGroup)
                .setTitle(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo("App usage since last full charge");
        // Verifies the title in the expandable divider.
        captor = ArgumentCaptor.forClass(String.class);
        verify(mBatteryChartPreferenceController.mExpandDividerPreference)
                .setTitle(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo("System usage since last full charge");
    }

    @Test
    public void selectedSlotText_selectAllDaysAllHours_returnNull() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));
        mBatteryChartPreferenceController.mDailyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(null);
    }

    @Test
    public void selectedSlotText_onlyOneDayDataSelectAllHours_returnNull() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(null);
    }

    @Test
    public void selectedSlotText_selectADayAllHours_onlyDayText() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo("Sunday");
    }

    @Test
    public void selectedSlotText_onlyOneDayDataSelectAnHour_onlyHourText() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = 1;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(
                "10 am - 12 pm");
    }

    @Test
    public void selectedSlotText_SelectADayAnHour_dayAndHourText() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex = 8;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(
                "Sunday 4 pm - 6 pm");
    }

    @Test
    public void onSaveInstanceState_restoreSelectedIndexAndExpandState() {
        final int expectedDailyIndex = 1;
        final int expectedHourlyIndex = 2;
        final boolean isExpanded = true;
        final Bundle bundle = new Bundle();
        mBatteryChartPreferenceController.mDailyChartIndex = expectedDailyIndex;
        mBatteryChartPreferenceController.mHourlyChartIndex = expectedHourlyIndex;
        mBatteryChartPreferenceController.mIsExpanded = isExpanded;
        mBatteryChartPreferenceController.onSaveInstanceState(bundle);
        // Replaces the original controller with other values.
        mBatteryChartPreferenceController.mDailyChartIndex = -1;
        mBatteryChartPreferenceController.mHourlyChartIndex = -1;
        mBatteryChartPreferenceController.mIsExpanded = false;

        mBatteryChartPreferenceController.onCreate(bundle);
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(25));

        assertThat(mBatteryChartPreferenceController.mDailyChartIndex)
                .isEqualTo(expectedDailyIndex);
        assertThat(mBatteryChartPreferenceController.mHourlyChartIndex)
                .isEqualTo(expectedHourlyIndex);
        assertThat(mBatteryChartPreferenceController.mIsExpanded).isTrue();
    }

    @Test
    public void isValidToShowSummary_returnExpectedResult() {
        assertThat(mBatteryChartPreferenceController
                .isValidToShowSummary("com.google.android.apps.scone"))
                .isTrue();

        // Verifies the item which is defined in the array list.
        assertThat(mBatteryChartPreferenceController
                .isValidToShowSummary("com.android.googlequicksearchbox"))
                .isFalse();
    }

    private static Long generateTimestamp(int index) {
        // "2021-04-23 07:00:00 UTC" + index hours
        return 1619247600000L + index * DateUtils.HOUR_IN_MILLIS;
    }

    private static Map<Long, Map<String, BatteryHistEntry>> createBatteryHistoryMap(
            int numOfHours) {
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        for (int index = 0; index < numOfHours; index++) {
            final ContentValues values = new ContentValues();
            values.put("batteryLevel", Integer.valueOf(100 - index));
            values.put("consumePower", Integer.valueOf(100 - index));
            final BatteryHistEntry entry = new BatteryHistEntry(values);
            final Map<String, BatteryHistEntry> entryMap = new HashMap<>();
            entryMap.put("fake_entry_key" + index, entry);
            batteryHistoryMap.put(generateTimestamp(index), entryMap);
        }
        return batteryHistoryMap;
    }

    private Map<Integer, Map<Integer, BatteryDiffData>> createBatteryUsageMap() {
        final int selectedAll = BatteryChartViewModel.SELECTED_INDEX_ALL;
        return Map.of(
                selectedAll, Map.of(
                        selectedAll, new BatteryDiffData(
                                Arrays.asList(mBatteryDiffEntry),
                                Arrays.asList(mBatteryDiffEntry))),
                0, Map.of(
                        selectedAll, new BatteryDiffData(
                                Arrays.asList(mBatteryDiffEntry),
                                Arrays.asList(mBatteryDiffEntry)),
                        0, new BatteryDiffData(
                                Arrays.asList(mBatteryDiffEntry),
                                Arrays.asList(mBatteryDiffEntry))));
    }

    private BatteryDiffEntry createBatteryDiffEntry(
            long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        return new BatteryDiffEntry(
                mContext, foregroundUsageTimeInMs, backgroundUsageTimeInMs,
                /*consumePower=*/ 0, mBatteryHistEntry);
    }

    private BatteryChartPreferenceControllerV2 createController() {
        final BatteryChartPreferenceControllerV2 controller =
                new BatteryChartPreferenceControllerV2(
                        mContext, "app_list", /*lifecycle=*/ null,
                        mSettingsActivity, mFragment);
        controller.mPrefContext = mContext;
        return controller;
    }
}
