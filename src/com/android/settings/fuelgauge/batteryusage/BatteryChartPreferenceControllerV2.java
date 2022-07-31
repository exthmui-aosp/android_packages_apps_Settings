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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controls the update for chart graph and the list items. */
public class BatteryChartPreferenceControllerV2 extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy,
        OnSaveInstanceState, OnResume, ExpandDividerPreference.OnExpandListener {
    private static final String TAG = "BatteryChartPreferenceControllerV2";
    private static final String KEY_FOOTER_PREF = "battery_graph_footer";
    private static final String PACKAGE_NAME_NONE = "none";

    private static final int TWENTY_FOUR_HOURS_TIME_SLOT_SIZE = 12;
    private static final long VALID_USAGE_TIME_DURATION = DateUtils.HOUR_IN_MILLIS * 2;
    private static final long VALID_DIFF_DURATION = DateUtils.MINUTE_IN_MILLIS * 3;

    // Keys for bundle instance to restore configurations.
    private static final String KEY_EXPAND_SYSTEM_INFO = "expand_system_info";
    private static final String KEY_DAILY_CHART_INDEX = "daily_chart_index";
    private static final String KEY_HOURLY_CHART_INDEX = "hourly_chart_index";

    private static int sUiMode = Configuration.UI_MODE_NIGHT_UNDEFINED;

    @VisibleForTesting
    Map<Integer, List<BatteryDiffEntry>> mBatteryIndexedMap;

    @VisibleForTesting
    Context mPrefContext;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    PreferenceGroup mAppListPrefGroup;
    @VisibleForTesting
    ExpandDividerPreference mExpandDividerPreference;
    @VisibleForTesting
    boolean mIsExpanded = false;

    @VisibleForTesting
    BatteryChartViewV2 mDailyChartView;
    @VisibleForTesting
    BatteryChartViewV2 mHourlyChartView;

    @VisibleForTesting
    int mDailyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
    @VisibleForTesting
    int mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;

    private boolean mIs24HourFormat;
    private boolean mIsFooterPrefAdded = false;
    private PreferenceScreen mPreferenceScreen;
    private FooterPreference mFooterPreference;
    private BatteryChartViewModel mDailyViewModel;
    private List<BatteryChartViewModel> mHourlyViewModels;

    private final String mPreferenceKey;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final CharSequence[] mNotAllowShowEntryPackages;
    private final CharSequence[] mNotAllowShowSummaryPackages;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Preference cache to avoid create new instance each time.
    @VisibleForTesting
    final Map<String, Preference> mPreferenceCache = new HashMap<>();
    @VisibleForTesting
    final List<BatteryDiffEntry> mSystemEntries = new ArrayList<>();

    public BatteryChartPreferenceControllerV2(
            Context context, String preferenceKey,
            Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context);
        mActivity = activity;
        mFragment = fragment;
        mPreferenceKey = preferenceKey;
        mIs24HourFormat = DateFormat.is24HourFormat(context);
        mMetricsFeatureProvider =
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
        mNotAllowShowEntryPackages =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideApplicationEntries(context);
        mNotAllowShowSummaryPackages =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideApplicationSummary(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mDailyChartIndex =
                savedInstanceState.getInt(KEY_DAILY_CHART_INDEX, mDailyChartIndex);
        mHourlyChartIndex =
                savedInstanceState.getInt(KEY_HOURLY_CHART_INDEX, mHourlyChartIndex);
        mIsExpanded =
                savedInstanceState.getBoolean(KEY_EXPAND_SYSTEM_INFO, mIsExpanded);
        Log.d(TAG, String.format("onCreate() dailyIndex=%d hourlyIndex=%d isExpanded=%b",
                mDailyChartIndex, mHourlyChartIndex, mIsExpanded));
    }

    @Override
    public void onResume() {
        final int currentUiMode =
                mContext.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
        if (sUiMode != currentUiMode) {
            sUiMode = currentUiMode;
            BatteryDiffEntry.clearCache();
            Log.d(TAG, "clear icon and label cache since uiMode is changed");
        }
        mIs24HourFormat = DateFormat.is24HourFormat(mContext);
        mMetricsFeatureProvider.action(mPrefContext, SettingsEnums.OPEN_BATTERY_USAGE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstance) {
        if (savedInstance == null) {
            return;
        }
        savedInstance.putInt(KEY_DAILY_CHART_INDEX, mDailyChartIndex);
        savedInstance.putInt(KEY_HOURLY_CHART_INDEX, mHourlyChartIndex);
        savedInstance.putBoolean(KEY_EXPAND_SYSTEM_INFO, mIsExpanded);
        Log.d(TAG, String.format("onSaveInstanceState() dailyIndex=%d hourlyIndex=%d isExpanded=%b",
                mDailyChartIndex, mHourlyChartIndex, mIsExpanded));
    }

    @Override
    public void onDestroy() {
        if (mActivity.isChangingConfigurations()) {
            BatteryDiffEntry.clearCache();
        }
        mHandler.removeCallbacksAndMessages(/*token=*/ null);
        mPreferenceCache.clear();
        if (mAppListPrefGroup != null) {
            mAppListPrefGroup.removeAll();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPrefContext = screen.getContext();
        mAppListPrefGroup = screen.findPreference(mPreferenceKey);
        mAppListPrefGroup.setOrderingAsAdded(false);
        mAppListPrefGroup.setTitle(
                mPrefContext.getString(R.string.battery_app_usage_for_past_24));
        mFooterPreference = screen.findPreference(KEY_FOOTER_PREF);
        // Removes footer first until usage data is loaded to avoid flashing.
        if (mFooterPreference != null) {
            screen.removePreference(mFooterPreference);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        final PowerGaugePreference powerPref = (PowerGaugePreference) preference;
        final BatteryDiffEntry diffEntry = powerPref.getBatteryDiffEntry();
        final BatteryHistEntry histEntry = diffEntry.mBatteryHistEntry;
        final String packageName = histEntry.mPackageName;
        final boolean isAppEntry = histEntry.isAppEntry();
        mMetricsFeatureProvider.action(
                /* attribution */ SettingsEnums.OPEN_BATTERY_USAGE,
                /* action */ isAppEntry
                        ? SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM
                        : SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM,
                /* pageId */ SettingsEnums.OPEN_BATTERY_USAGE,
                TextUtils.isEmpty(packageName) ? PACKAGE_NAME_NONE : packageName,
                (int) Math.round(diffEntry.getPercentOfTotal()));
        Log.d(TAG, String.format("handleClick() label=%s key=%s package=%s",
                diffEntry.getAppLabel(), histEntry.getKey(), histEntry.mPackageName));
        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, diffEntry, powerPref.getPercent(),
                isValidToShowSummary(packageName), getSlotInformation());
        return true;
    }

    @Override
    public void onExpand(boolean isExpanded) {
        mIsExpanded = isExpanded;
        mMetricsFeatureProvider.action(
                mPrefContext,
                SettingsEnums.ACTION_BATTERY_USAGE_EXPAND_ITEM,
                isExpanded);
        refreshExpandUi();
    }

    void setBatteryHistoryMap(
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        Log.d(TAG, "setBatteryHistoryMap() " + (batteryHistoryMap == null ? "null"
                : ("size=" + batteryHistoryMap.size())));
        final BatteryLevelData batteryLevelData =
                DataProcessor.getBatteryLevelData(mContext, batteryHistoryMap);
        Log.d(TAG, "getBatteryLevelData: " + batteryLevelData);
        if (batteryLevelData == null) {
            mDailyViewModel = null;
            mHourlyViewModels = null;
            addFooterPreferenceIfNeeded(false);
            return;
        }
        mDailyViewModel = new BatteryChartViewModel(
                batteryLevelData.getDailyBatteryLevels().getLevels(),
                generateTimestampDayOfWeekTexts(
                        mContext, batteryLevelData.getDailyBatteryLevels().getTimestamps()),
                mDailyChartIndex,
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS);
        mHourlyViewModels = new ArrayList<>();
        for (BatteryLevelData.PeriodBatteryLevelData hourlyBatteryLevelsPerDay :
                batteryLevelData.getHourlyBatteryLevelsPerDay()) {
            mHourlyViewModels.add(new BatteryChartViewModel(
                    hourlyBatteryLevelsPerDay.getLevels(),
                    generateTimestampHourTexts(
                            mContext, hourlyBatteryLevelsPerDay.getTimestamps()),
                    mHourlyChartIndex,
                    BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS));
        }
        refreshUi();
        // TODO: Loads item icon and label and build mBatteryIndexedMap.
    }

    void setBatteryChartView(@NonNull final BatteryChartViewV2 dailyChartView,
            @NonNull final BatteryChartViewV2 hourlyChartView) {
        if (mDailyChartView != dailyChartView || mHourlyChartView != hourlyChartView) {
            mHandler.post(() -> setBatteryChartViewInner(dailyChartView, hourlyChartView));
        }
    }

    private void setBatteryChartViewInner(@NonNull final BatteryChartViewV2 dailyChartView,
            @NonNull final BatteryChartViewV2 hourlyChartView) {
        mDailyChartView = dailyChartView;
        mDailyChartView.setOnSelectListener(trapezoidIndex -> {
            if (mDailyChartIndex == trapezoidIndex) {
                return;
            }
            Log.d(TAG, "onDailyChartSelect:" + trapezoidIndex);
            mDailyChartIndex = trapezoidIndex;
            mHourlyChartIndex = BatteryChartView.SELECTED_INDEX_ALL;
            refreshUi();
            // TODO: Change to log daily data.
        });
        mHourlyChartView = hourlyChartView;
        mHourlyChartView.setOnSelectListener(trapezoidIndex -> {
            if (mHourlyChartIndex == trapezoidIndex) {
                return;
            }
            Log.d(TAG, "onHourlyChartSelect:" + trapezoidIndex);
            mHourlyChartIndex = trapezoidIndex;
            refreshUi();
            mMetricsFeatureProvider.action(
                    mPrefContext,
                    trapezoidIndex == BatteryChartViewModel.SELECTED_INDEX_ALL
                            ? SettingsEnums.ACTION_BATTERY_USAGE_SHOW_ALL
                            : SettingsEnums.ACTION_BATTERY_USAGE_TIME_SLOT);
        });
        refreshUi();
    }

    @VisibleForTesting
    boolean refreshUi() {
        if (mBatteryIndexedMap == null || mDailyChartView == null || mHourlyChartView == null) {
            return false;
        }

        if (mDailyViewModel == null || mHourlyViewModels == null) {
            // Fail to get battery level data, show an empty hourly chart view.
            mDailyChartView.setVisibility(View.GONE);
            mHourlyChartView.setViewModel(null);
            return false;
        }

        if (isBatteryLevelDataInOneDay()) {
            // Only 1 day data, hide the daily chart view.
            mDailyChartView.setVisibility(View.GONE);
            mDailyChartIndex = 0;
        } else {
            mDailyChartView.setVisibility(View.VISIBLE);
            mDailyViewModel.setSelectedIndex(mDailyChartIndex);
            mDailyChartView.setViewModel(mDailyViewModel);
        }

        if (mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL) {
            // Multiple days are selected, hide the hourly chart view.
            mHourlyChartView.setVisibility(View.GONE);
        } else {
            mHourlyChartView.setVisibility(View.VISIBLE);
            final BatteryChartViewModel hourlyViewModel = mHourlyViewModels.get(mDailyChartIndex);
            hourlyViewModel.setSelectedIndex(mHourlyChartIndex);
            mHourlyChartView.setViewModel(hourlyViewModel);
        }

        mHandler.post(() -> {
            final long start = System.currentTimeMillis();
            removeAndCacheAllPrefs();
            addAllPreferences();
            refreshCategoryTitle();
            Log.d(TAG, String.format("refreshUi is finished in %d/ms",
                    (System.currentTimeMillis() - start)));
        });
        return true;
    }

    private void addAllPreferences() {
        // TODO: Get the right diff entry according to daily and hourly chart selection.
        final int index = mDailyChartIndex;
        final List<BatteryDiffEntry> entries =
                mBatteryIndexedMap.get(Integer.valueOf(index));
        addFooterPreferenceIfNeeded(entries != null && !entries.isEmpty());
        if (entries == null) {
            Log.w(TAG, "cannot find BatteryDiffEntry for:" + index);
            return;
        }
        // Separates data into two groups and sort them individually.
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        mSystemEntries.clear();
        entries.forEach(entry -> {
            final String packageName = entry.getPackageName();
            if (!isValidToShowEntry(packageName)) {
                Log.w(TAG, "ignore showing item:" + packageName);
                return;
            }
            if (entry.isSystemEntry()) {
                mSystemEntries.add(entry);
            } else {
                appEntries.add(entry);
            }
            // Validates the usage time if users click a specific slot.
            if (index >= 0) {
                validateUsageTime(entry);
            }
        });
        Collections.sort(appEntries, BatteryDiffEntry.COMPARATOR);
        Collections.sort(mSystemEntries, BatteryDiffEntry.COMPARATOR);
        Log.d(TAG, String.format("addAllPreferences() app=%d system=%d",
                appEntries.size(), mSystemEntries.size()));

        // Adds app entries to the list if it is not empty.
        if (!appEntries.isEmpty()) {
            addPreferenceToScreen(appEntries);
        }
        // Adds the expabable divider if we have system entries data.
        if (!mSystemEntries.isEmpty()) {
            if (mExpandDividerPreference == null) {
                mExpandDividerPreference = new ExpandDividerPreference(mPrefContext);
                mExpandDividerPreference.setOnExpandListener(this);
                mExpandDividerPreference.setIsExpanded(mIsExpanded);
            }
            mExpandDividerPreference.setOrder(
                    mAppListPrefGroup.getPreferenceCount());
            mAppListPrefGroup.addPreference(mExpandDividerPreference);
        }
        refreshExpandUi();
    }

    @VisibleForTesting
    void addPreferenceToScreen(List<BatteryDiffEntry> entries) {
        if (mAppListPrefGroup == null || entries.isEmpty()) {
            return;
        }
        int prefIndex = mAppListPrefGroup.getPreferenceCount();
        for (BatteryDiffEntry entry : entries) {
            boolean isAdded = false;
            final String appLabel = entry.getAppLabel();
            final Drawable appIcon = entry.getAppIcon();
            if (TextUtils.isEmpty(appLabel) || appIcon == null) {
                Log.w(TAG, "cannot find app resource for:" + entry.getPackageName());
                continue;
            }
            final String prefKey = entry.mBatteryHistEntry.getKey();
            PowerGaugePreference pref = mAppListPrefGroup.findPreference(prefKey);
            if (pref != null) {
                isAdded = true;
                Log.w(TAG, "preference should be removed for:" + entry.getPackageName());
            } else {
                pref = (PowerGaugePreference) mPreferenceCache.get(prefKey);
            }
            // Creates new innstance if cached preference is not found.
            if (pref == null) {
                pref = new PowerGaugePreference(mPrefContext);
                pref.setKey(prefKey);
                mPreferenceCache.put(prefKey, pref);
            }
            pref.setIcon(appIcon);
            pref.setTitle(appLabel);
            pref.setOrder(prefIndex);
            pref.setPercent(entry.getPercentOfTotal());
            pref.setSingleLineTitle(true);
            // Sets the BatteryDiffEntry to preference for launching detailed page.
            pref.setBatteryDiffEntry(entry);
            pref.setEnabled(entry.validForRestriction());
            setPreferenceSummary(pref, entry);
            if (!isAdded) {
                mAppListPrefGroup.addPreference(pref);
            }
            prefIndex++;
        }
    }

    private void removeAndCacheAllPrefs() {
        if (mAppListPrefGroup == null
                || mAppListPrefGroup.getPreferenceCount() == 0) {
            return;
        }
        final int prefsCount = mAppListPrefGroup.getPreferenceCount();
        for (int index = 0; index < prefsCount; index++) {
            final Preference pref = mAppListPrefGroup.getPreference(index);
            if (TextUtils.isEmpty(pref.getKey())) {
                continue;
            }
            mPreferenceCache.put(pref.getKey(), pref);
        }
        mAppListPrefGroup.removeAll();
    }

    private void refreshExpandUi() {
        if (mIsExpanded) {
            addPreferenceToScreen(mSystemEntries);
        } else {
            // Removes and recycles all system entries to hide all of them.
            for (BatteryDiffEntry entry : mSystemEntries) {
                final String prefKey = entry.mBatteryHistEntry.getKey();
                final Preference pref = mAppListPrefGroup.findPreference(prefKey);
                if (pref != null) {
                    mAppListPrefGroup.removePreference(pref);
                    mPreferenceCache.put(pref.getKey(), pref);
                }
            }
        }
    }

    @VisibleForTesting
    void refreshCategoryTitle() {
        final String slotInformation = getSlotInformation();
        Log.d(TAG, String.format("refreshCategoryTitle:%s", slotInformation));
        if (mAppListPrefGroup != null) {
            mAppListPrefGroup.setTitle(
                    getSlotInformation(/*isApp=*/ true, slotInformation));
        }
        if (mExpandDividerPreference != null) {
            mExpandDividerPreference.setTitle(
                    getSlotInformation(/*isApp=*/ false, slotInformation));
        }
    }

    private String getSlotInformation(boolean isApp, String slotInformation) {
        // TODO: Updates the right slot information from daily and hourly chart selection.
        // Null means we show all information without a specific time slot.
        if (slotInformation == null) {
            return isApp
                    ? mPrefContext.getString(R.string.battery_app_usage_for_past_24)
                    : mPrefContext.getString(R.string.battery_system_usage_for_past_24);
        } else {
            return isApp
                    ? mPrefContext.getString(R.string.battery_app_usage_for, slotInformation)
                    : mPrefContext.getString(R.string.battery_system_usage_for, slotInformation);
        }
    }

    private String getSlotInformation() {
        // TODO: Generate the right slot information from daily and hourly chart selection.
        return null;
    }

    @VisibleForTesting
    void setPreferenceSummary(
            PowerGaugePreference preference, BatteryDiffEntry entry) {
        final long foregroundUsageTimeInMs = entry.mForegroundUsageTimeInMs;
        final long backgroundUsageTimeInMs = entry.mBackgroundUsageTimeInMs;
        final long totalUsageTimeInMs = foregroundUsageTimeInMs + backgroundUsageTimeInMs;
        // Checks whether the package is allowed to show summary or not.
        if (!isValidToShowSummary(entry.getPackageName())) {
            preference.setSummary(null);
            return;
        }
        String usageTimeSummary = null;
        // Not shows summary for some system components without usage time.
        if (totalUsageTimeInMs == 0) {
            preference.setSummary(null);
            // Shows background summary only if we don't have foreground usage time.
        } else if (foregroundUsageTimeInMs == 0 && backgroundUsageTimeInMs != 0) {
            usageTimeSummary = buildUsageTimeInfo(backgroundUsageTimeInMs, true);
            // Shows total usage summary only if total usage time is small.
        } else if (totalUsageTimeInMs < DateUtils.MINUTE_IN_MILLIS) {
            usageTimeSummary = buildUsageTimeInfo(totalUsageTimeInMs, false);
        } else {
            usageTimeSummary = buildUsageTimeInfo(totalUsageTimeInMs, false);
            // Shows background usage time if it is larger than a minute.
            if (backgroundUsageTimeInMs > 0) {
                usageTimeSummary +=
                        "\n" + buildUsageTimeInfo(backgroundUsageTimeInMs, true);
            }
        }
        preference.setSummary(usageTimeSummary);
    }

    private String buildUsageTimeInfo(long usageTimeInMs, boolean isBackground) {
        if (usageTimeInMs < DateUtils.MINUTE_IN_MILLIS) {
            return mPrefContext.getString(
                    isBackground
                            ? R.string.battery_usage_background_less_than_one_minute
                            : R.string.battery_usage_total_less_than_one_minute);
        }
        final CharSequence timeSequence =
                StringUtil.formatElapsedTime(mPrefContext, usageTimeInMs,
                        /*withSeconds=*/ false, /*collapseTimeUnit=*/ false);
        final int resourceId =
                isBackground
                        ? R.string.battery_usage_for_background_time
                        : R.string.battery_usage_for_total_time;
        return mPrefContext.getString(resourceId, timeSequence);
    }

    @VisibleForTesting
    boolean isValidToShowSummary(String packageName) {
        return !contains(packageName, mNotAllowShowSummaryPackages);
    }

    @VisibleForTesting
    boolean isValidToShowEntry(String packageName) {
        return !contains(packageName, mNotAllowShowEntryPackages);
    }

    private void addFooterPreferenceIfNeeded(boolean containAppItems) {
        if (mIsFooterPrefAdded || mFooterPreference == null) {
            return;
        }
        mIsFooterPrefAdded = true;
        mFooterPreference.setTitle(mPrefContext.getString(
                containAppItems
                        ? R.string.battery_usage_screen_footer
                        : R.string.battery_usage_screen_footer_empty));
        mHandler.post(() -> mPreferenceScreen.addPreference(mFooterPreference));
    }

    private boolean isBatteryLevelDataInOneDay() {
        return mHourlyViewModels.size() == 1;
    }

    private static List<String> generateTimestampDayOfWeekTexts(
            @NonNull final Context context, @NonNull final List<Long> timestamps) {
        final ArrayList<String> texts = new ArrayList<>();
        for (Long timestamp : timestamps) {
            texts.add(ConvertUtils.utcToLocalTimeDayOfWeek(context, timestamp));
        }
        return texts;
    }

    private static List<String> generateTimestampHourTexts(
            @NonNull final Context context, @NonNull final List<Long> timestamps) {
        final boolean is24HourFormat = DateFormat.is24HourFormat(context);
        final ArrayList<String> texts = new ArrayList<>();
        for (Long timestamp : timestamps) {
            texts.add(ConvertUtils.utcToLocalTimeHour(context, timestamp, is24HourFormat));
        }
        return texts;
    }

    private static boolean contains(String target, CharSequence[] packageNames) {
        if (target != null && packageNames != null) {
            for (CharSequence packageName : packageNames) {
                if (TextUtils.equals(target, packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    static boolean validateUsageTime(BatteryDiffEntry entry) {
        final long foregroundUsageTimeInMs = entry.mForegroundUsageTimeInMs;
        final long backgroundUsageTimeInMs = entry.mBackgroundUsageTimeInMs;
        final long totalUsageTimeInMs = foregroundUsageTimeInMs + backgroundUsageTimeInMs;
        if (foregroundUsageTimeInMs > VALID_USAGE_TIME_DURATION
                || backgroundUsageTimeInMs > VALID_USAGE_TIME_DURATION
                || totalUsageTimeInMs > VALID_USAGE_TIME_DURATION) {
            Log.e(TAG, "validateUsageTime() fail for\n" + entry);
            return false;
        }
        return true;
    }

    // TODO: Change this method to fromLastFullCharged.

    /** Used for {@link AppBatteryPreferenceController}. */
    public static List<BatteryDiffEntry> getBatteryLast24HrUsageData(Context context) {
        final long start = System.currentTimeMillis();
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getBatteryHistory(context);
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            return null;
        }
        Log.d(TAG, String.format("getBatteryLast24HrData() size=%d time=%d/ms",
                batteryHistoryMap.size(), (System.currentTimeMillis() - start)));

        final List<Long> batteryHistoryKeyList =
                new ArrayList<>(batteryHistoryMap.keySet());
        Collections.sort(batteryHistoryKeyList);
        final long[] batteryHistoryKeys = new long[TWENTY_FOUR_HOURS_TIME_SLOT_SIZE + 1];
        for (int index = 0; index < batteryHistoryKeys.length; index++) {
            batteryHistoryKeys[index] = batteryHistoryKeyList.get(index);
        }

        final Map<Integer, List<BatteryDiffEntry>> batteryIndexedMap =
                ConvertUtils.getIndexedUsageMap(
                        context,
                        /*timeSlotSize=*/ TWENTY_FOUR_HOURS_TIME_SLOT_SIZE,
                        batteryHistoryKeys,
                        batteryHistoryMap,
                        /*purgeLowPercentageAndFakeData=*/ true);
        return batteryIndexedMap.get(BatteryChartViewModel.SELECTED_INDEX_ALL);
    }

    // TODO: Change this method to fromLastFullCharged.

    /** Used for {@link AppBatteryPreferenceController}. */
    public static BatteryDiffEntry getBatteryLast24HrUsageData(
            Context context, String packageName, int userId) {
        if (packageName == null) {
            return null;
        }
        final List<BatteryDiffEntry> entries = getBatteryLast24HrUsageData(context);
        if (entries == null) {
            return null;
        }
        for (BatteryDiffEntry entry : entries) {
            final BatteryHistEntry batteryHistEntry = entry.mBatteryHistEntry;
            if (batteryHistEntry != null
                    && batteryHistEntry.mConsumerType == ConvertUtils.CONSUMER_TYPE_UID_BATTERY
                    && batteryHistEntry.mUserId == userId
                    && packageName.equals(entry.getPackageName())) {
                return entry;
            }
        }
        return null;
    }
}
