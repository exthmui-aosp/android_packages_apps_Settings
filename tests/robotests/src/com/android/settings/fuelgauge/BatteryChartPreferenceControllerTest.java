/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartPreferenceControllerTest {

    @Mock private InstrumentedPreferenceFragment mFragment;
    @Mock private SettingsActivity mSettingsActivity;
    @Mock private PreferenceGroup mAppListGroup;
    @Mock private PackageManager mPackageManager;
    @Mock private Drawable mDrawable;
    @Mock private BatteryHistEntry mBatteryHistEntry;
    @Mock private BatteryChartView mBatteryChartView;
    @Mock private PowerGaugePreference mPowerGaugePreference;
    @Mock private BatteryUtils mBatteryUtils;

    private Context mContext;
    private BatteryDiffEntry mBatteryDiffEntry;
    private BatteryChartPreferenceController mBatteryChartPreferenceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryChartPreferenceController =
            new BatteryChartPreferenceController(
                mContext, "app_list", /*lifecycle=*/ null,
                mSettingsActivity, mFragment);
        mBatteryChartPreferenceController.mPrefContext = mContext;
        mBatteryChartPreferenceController.mAppListPrefGroup = mAppListGroup;
        mBatteryChartPreferenceController.mBatteryChartView = mBatteryChartView;
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
        mBatteryChartPreferenceController.setBatteryHistoryMap(
            createBatteryHistoryMap(/*size=*/ 5));
    }

    @Test
    public void testOnDestroy_activityIsChanging_clearBatteryEntryCache() {
        doReturn(true).when(mSettingsActivity).isChangingConfigurations();
        // Ensures the testing environment is correct.
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void testOnDestroy_activityIsNotChanging_notClearBatteryEntryCache() {
        doReturn(false).when(mSettingsActivity).isChangingConfigurations();
        // Ensures the testing environment is correct.
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        assertThat(BatteryDiffEntry.sResourceCache).isNotEmpty();
    }

    @Test
    public void testOnDestroy_clearPreferenceCache() {
        final String prefKey = "preference fake key";
        // Ensures the testing environment is correct.
        mBatteryChartPreferenceController.mPreferenceCache.put(
            prefKey, mPowerGaugePreference);
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        // Verifies the result after onDestroy.
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).isEmpty();
    }

    @Test
    public void testOnDestroy_removeAllPreferenceFromPreferenceGroup() {
        mBatteryChartPreferenceController.onDestroy();
        verify(mAppListGroup).removeAll();
    }

    @Test
    public void testSetBatteryHistoryMap_createExpectedKeysAndLevels() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(
            createBatteryHistoryMap(/*size=*/ 5));

        // Verifies the created battery keys array.
        for (int index = 0; index < 25; index++) {
            assertThat(mBatteryChartPreferenceController.mBatteryHistoryKeys[index])
                // These values is are calculated by hand from createBatteryHistoryMap().
                .isEqualTo(index < 20 ? 0 : (index - 20 + 1));
        }
        // Verifies the created battery levels array.
        for (int index = 0; index < 13; index++) {
            assertThat(mBatteryChartPreferenceController.mBatteryHistoryLevels[index])
                // These values is are calculated by hand from createBatteryHistoryMap().
                .isEqualTo(index < 10 ? 0 : (100 - (index - 10) * 2));
        }
        assertThat(mBatteryChartPreferenceController.mBatteryIndexedMap).hasSize(13);
    }

    @Test
    public void testSetBatteryHistoryMap_largeSize_createExpectedKeysAndLevels() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(
            createBatteryHistoryMap(/*size=*/ 25));

        // Verifies the created battery keys array.
        for (int index = 0; index < 25; index++) {
          assertThat(mBatteryChartPreferenceController.mBatteryHistoryKeys[index])
              // These values is are calculated by hand from createBatteryHistoryMap().
              .isEqualTo(index + 1);
        }
        // Verifies the created battery levels array.
        for (int index = 0; index < 13; index++) {
          assertThat(mBatteryChartPreferenceController.mBatteryHistoryLevels[index])
              // These values is are calculated by hand from createBatteryHistoryMap().
              .isEqualTo(100 - index * 2);
        }
        assertThat(mBatteryChartPreferenceController.mBatteryIndexedMap).hasSize(13);
    }

    @Test
    public void testRefreshUi_batteryIndexedMapIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(null);
        assertThat(mBatteryChartPreferenceController.refreshUi(
            /*trapezoidIndex=*/ 1, /*isForce=*/ false)).isFalse();
    }

    @Test
    public void testRefreshUi_batteryChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mBatteryChartView = null;
        assertThat(mBatteryChartPreferenceController.refreshUi(
            /*trapezoidIndex=*/ 1, /*isForce=*/ false)).isFalse();
    }

    @Test
    public void testRefreshUi_trapezoidIndexIsNotChanged_ignoreRefresh() {
        final int trapezoidIndex = 1;
        mBatteryChartPreferenceController.mTrapezoidIndex = trapezoidIndex;
        assertThat(mBatteryChartPreferenceController.refreshUi(
            trapezoidIndex, /*isForce=*/ false)).isFalse();
    }

    @Test
    public void testRefreshUi_forceUpdate_refreshUi() {
        final int trapezoidIndex = 1;
        mBatteryChartPreferenceController.mTrapezoidIndex = trapezoidIndex;
        assertThat(mBatteryChartPreferenceController.refreshUi(
            trapezoidIndex, /*isForce=*/ true)).isTrue();
    }

    @Test
    public void testForceRefreshUi_updateTrapezoidIndexIntoSelectAll() {
        mBatteryChartPreferenceController.mTrapezoidIndex =
            BatteryChartView.SELECTED_INDEX_INVALID;
        mBatteryChartPreferenceController.setBatteryHistoryMap(
            createBatteryHistoryMap(/*size=*/ 25));

        assertThat(mBatteryChartPreferenceController.mTrapezoidIndex)
            .isEqualTo(BatteryChartView.SELECTED_INDEX_ALL);
    }

    @Test
    public void testRemoveAndCacheAllPrefs_emptyContent_ignoreRemoveAll() {
        final int trapezoidIndex = 1;
        doReturn(0).when(mAppListGroup).getPreferenceCount();

        mBatteryChartPreferenceController.refreshUi(
            trapezoidIndex, /*isForce=*/ true);
        verify(mAppListGroup, never()).removeAll();
    }

    @Test
    public void testRemoveAndCacheAllPrefs_buildCacheAndRemoveAllPreference() {
        final int trapezoidIndex = 1;
        final String prefKey = "preference fake key";
        doReturn(1).when(mAppListGroup).getPreferenceCount();
        doReturn(mPowerGaugePreference).when(mAppListGroup).getPreference(0);
        doReturn(prefKey).when(mPowerGaugePreference).getKey();
        // Ensures the testing data is correct.
        assertThat(mBatteryChartPreferenceController.mPreferenceCache).isEmpty();

        mBatteryChartPreferenceController.refreshUi(
            trapezoidIndex, /*isForce=*/ true);

        assertThat(mBatteryChartPreferenceController.mPreferenceCache.get(prefKey))
            .isEqualTo(mPowerGaugePreference);
        verify(mAppListGroup).removeAll();
    }

    @Test
    public void testAddPreferenceToScreen_emptyContent_ignoreAddPreference() {
        mBatteryChartPreferenceController.addPreferenceToScreen(
            new ArrayList<BatteryDiffEntry>());
        verify(mAppListGroup, never()).addPreference(any());
    }

    @Test
    public void testAddPreferenceToScreen_addPreferenceIntoScreen() {
        final String prefKey = "preference fake key";
        final String appLabel = "fake app label";
        doReturn(1).when(mAppListGroup).getPreferenceCount();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(appLabel).when(mBatteryDiffEntry).getAppLabel();
        doReturn(prefKey).when(mBatteryHistEntry).getKey();

        mBatteryChartPreferenceController.addPreferenceToScreen(
            Arrays.asList(mBatteryDiffEntry));

        // Verifies the preference cache.
        final PowerGaugePreference pref =
            (PowerGaugePreference) mBatteryChartPreferenceController.mPreferenceCache
                .get(prefKey);
        assertThat(pref).isNotNull();
        // Verifies the added preference configuration.
        verify(mAppListGroup).addPreference(pref);
        assertThat(pref.getKey()).isEqualTo(prefKey);
        assertThat(pref.getTitle()).isEqualTo(appLabel);
        assertThat(pref.getIcon()).isEqualTo(mDrawable);
        assertThat(pref.getOrder()).isEqualTo(1);
        assertThat(pref.getBatteryDiffEntry()).isSameInstanceAs(mBatteryDiffEntry);
    }

    @Test
    public void testHandlePreferenceTreeClick_notPowerGaugePreference_returnFalse() {
        assertThat(mBatteryChartPreferenceController.handlePreferenceTreeClick(mAppListGroup))
            .isFalse();
    }

    @Test
    public void testHandlePreferenceTreeClick_validPackageName_returnTrue() {
        doReturn(false).when(mBatteryHistEntry).isAppEntry();
        doReturn(mBatteryDiffEntry).when(mPowerGaugePreference).getBatteryDiffEntry();

        assertThat(mBatteryChartPreferenceController.handlePreferenceTreeClick(
            mPowerGaugePreference)).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick_appEntryWithInvalidPackage_returnFalse() {
        mBatteryChartPreferenceController.mBatteryUtils = mBatteryUtils;
        doReturn(true).when(mBatteryHistEntry).isAppEntry();
        doReturn(BatteryUtils.UID_NULL).when(mBatteryUtils)
            .getPackageUid(mBatteryHistEntry.mPackageName);
        doReturn(mBatteryDiffEntry).when(mPowerGaugePreference).getBatteryDiffEntry();

        assertThat(mBatteryChartPreferenceController.handlePreferenceTreeClick(
            mPowerGaugePreference)).isFalse();
    }

    private Map<Long, List<BatteryHistEntry>> createBatteryHistoryMap(int size) {
        final Map<Long, List<BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        for (int index = 0; index < size; index++) {
            final ContentValues values = new ContentValues();
            values.put("batteryLevel", Integer.valueOf(100 - index));
            final BatteryHistEntry entry = new BatteryHistEntry(values);
            batteryHistoryMap.put(Long.valueOf(index + 1), Arrays.asList(entry));
        }
        return batteryHistoryMap;
    }
}
