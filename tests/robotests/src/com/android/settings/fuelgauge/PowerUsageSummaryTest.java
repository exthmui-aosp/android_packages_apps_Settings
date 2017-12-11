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
package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_HIGH_POWER_APPS;
import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_TOGGLE_APPS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link PowerUsageSummary}.
 */
// TODO: Improve this test class so that it starts up the real activity and fragment.
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
        })
public class PowerUsageSummaryTest {
    private static final String STUB_STRING = "stub_string";
    private static final int UID = 123;
    private static final int UID_2 = 234;
    private static final int POWER_MAH = 100;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_MS = 120 * 60 * 1000;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_US =
            TIME_SINCE_LAST_FULL_CHARGE_MS * 1000;
    private static final long USAGE_TIME_MS = 65 * 60 * 1000;
    private static final double TOTAL_POWER = 200;
    public static final String NEW_ML_EST_SUFFIX = "(New ML est)";
    public static final String OLD_EST_SUFFIX = "(Old est)";
    private static Intent sAdditionalBatteryInfoIntent;

    @BeforeClass
    public static void beforeClass() {
        sAdditionalBatteryInfoIntent = new Intent("com.example.app.ADDITIONAL_BATTERY_INFO");
    }

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock
    private MenuItem mToggleAppsMenu;
    @Mock
    private MenuItem mHighPowerMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private BatterySipper mNormalBatterySipper;
    @Mock
    private BatterySipper mScreenBatterySipper;
    @Mock
    private BatterySipper mCellBatterySipper;
    @Mock
    private LayoutPreference mBatteryLayoutPref;
    @Mock
    private TextView mBatteryPercentText;
    @Mock
    private TextView mSummary1;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mBatteryHelper;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private AnomalyDetectionPolicy mAnomalyDetectionPolicy;
    @Mock
    private BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;

    private List<BatterySipper> mUsageList;
    private Context mRealContext;
    private TestFragment mFragment;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryMeterView mBatteryMeterView;
    private PowerGaugePreference mScreenUsagePref;
    private PowerGaugePreference mLastFullChargePref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRealContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mPowerManager);

        mScreenUsagePref = new PowerGaugePreference(mRealContext);
        mLastFullChargePref = new PowerGaugePreference(mRealContext);
        mFragment = spy(new TestFragment(mContext));
        mFragment.initFeatureProvider();
        mBatteryMeterView = new BatteryMeterView(mRealContext);
        mBatteryMeterView.mDrawable = new BatteryMeterView.BatteryMeterDrawable(mRealContext, 0);
        doNothing().when(mFragment).restartBatteryStatsLoader();
        doReturn(mock(LoaderManager.class)).when(mFragment).getLoaderManager();

        when(mFragment.getActivity()).thenReturn(mSettingsActivity);
        when(mToggleAppsMenu.getItemId()).thenReturn(MENU_TOGGLE_APPS);
        when(mHighPowerMenu.getItemId()).thenReturn(MENU_HIGH_POWER_APPS);
        when(mFeatureFactory.powerUsageFeatureProvider.getAdditionalBatteryInfoIntent())
                .thenReturn(sAdditionalBatteryInfoIntent);
        when(mBatteryHelper.getTotalPower()).thenReturn(TOTAL_POWER);
        when(mBatteryHelper.getStats().computeBatteryRealtime(anyLong(), anyInt())).thenReturn(
                TIME_SINCE_LAST_FULL_CHARGE_US);

        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        mNormalBatterySipper.totalPowerMah = POWER_MAH;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        mCellBatterySipper.drainType = BatterySipper.DrainType.CELL;
        mCellBatterySipper.totalPowerMah = POWER_MAH;

        when(mBatteryLayoutPref.findViewById(R.id.summary1)).thenReturn(mSummary1);
        when(mBatteryLayoutPref.findViewById(R.id.battery_percent)).thenReturn(mBatteryPercentText);
        when(mBatteryLayoutPref.findViewById(R.id.battery_header_icon))
                .thenReturn(mBatteryMeterView);
        mFragment.setBatteryLayoutPreference(mBatteryLayoutPref);

        mScreenBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        mScreenBatterySipper.usageTimeMs = USAGE_TIME_MS;

        mUsageList = new ArrayList<>();
        mUsageList.add(mNormalBatterySipper);
        mUsageList.add(mScreenBatterySipper);
        mUsageList.add(mCellBatterySipper);

        mFragment.mStatsHelper = mBatteryHelper;
        when(mBatteryHelper.getUsageList()).thenReturn(mUsageList);
        mFragment.mScreenUsagePref = mScreenUsagePref;
        mFragment.mLastFullChargePref = mLastFullChargePref;
        mFragment.mBatteryUtils = spy(new BatteryUtils(mRealContext));
    }

    @Test
    public void testOptionsMenu_menuHighPower_metricEventInvoked() {
        mFragment.onOptionsItemSelected(mHighPowerMenu);

        verify(mFeatureFactory.metricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_OPTIMIZATION);
    }

    @Test
    public void testOptionsMenu_menuAppToggle_metricEventInvoked() {
        mFragment.onOptionsItemSelected(mToggleAppsMenu);
        mFragment.mShowAllApps = false;

        verify(mFeatureFactory.metricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_APPS_TOGGLE, true);
    }

    @Test
    public void testOptionsMenu_toggleAppsEnabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isPowerAccountingToggleEnabled())
                .thenReturn(true);
        mFragment.mShowAllApps = false;

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, MENU_TOGGLE_APPS, Menu.NONE, R.string.show_all_apps);
    }

    @Test
    public void testOptionsMenu_clickToggleAppsMenu_dataChanged() {
        testToggleAllApps(true);
        testToggleAllApps(false);
    }

    private void testToggleAllApps(final boolean isShowApps) {
        mFragment.mShowAllApps = isShowApps;

        mFragment.onOptionsItemSelected(mToggleAppsMenu);
        assertThat(mFragment.mShowAllApps).isEqualTo(!isShowApps);
    }

    @Test
    public void testFindBatterySipperByType_findTypeScreen() {
        BatterySipper sipper = mFragment.findBatterySipperByType(mUsageList,
                BatterySipper.DrainType.SCREEN);

        assertThat(sipper).isSameAs(mScreenBatterySipper);
    }

    @Test
    public void testFindBatterySipperByType_findTypeApp() {
        BatterySipper sipper = mFragment.findBatterySipperByType(mUsageList,
                BatterySipper.DrainType.APP);

        assertThat(sipper).isSameAs(mNormalBatterySipper);
    }

    @Test
    public void testUpdateScreenPreference_showCorrectSummary() {
        doReturn(mScreenBatterySipper).when(mFragment).findBatterySipperByType(any(), any());
        doReturn(mRealContext).when(mFragment).getContext();
        final CharSequence expectedSummary = Utils.formatElapsedTime(mRealContext, USAGE_TIME_MS,
                false);

        mFragment.updateScreenPreference();

        assertThat(mScreenUsagePref.getSubtitle()).isEqualTo(expectedSummary);
    }

    @Test
    public void testUpdateLastFullChargePreference_showCorrectSummary() {
        doReturn(mRealContext).when(mFragment).getContext();

        mFragment.updateLastFullChargePreference(TIME_SINCE_LAST_FULL_CHARGE_MS);

        assertThat(mLastFullChargePref.getSubtitle()).isEqualTo("2 hr. ago");
    }

    @Test
    public void testUpdatePreference_usageListEmpty_shouldNotCrash() {
        when(mBatteryHelper.getUsageList()).thenReturn(new ArrayList<BatterySipper>());
        doReturn(STUB_STRING).when(mFragment).getString(anyInt(), any());
        doReturn(mRealContext).when(mFragment).getContext();

        // Should not crash when update
        mFragment.updateScreenPreference();
    }

    @Test
    public void testNonIndexableKeys_MatchPreferenceKeys() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks = PowerUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.power_usage_summary);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = RuntimeEnvironment.application;
        final PowerUsageSummary fragment = new PowerUsageSummary();
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(context,
                fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : fragment.getPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }

    @Test
    public void testUpdateAnomalySparseArray() {
        mFragment.mAnomalySparseArray = new SparseArray<>();
        final List<Anomaly> anomalies = new ArrayList<>();
        final Anomaly anomaly1 = new Anomaly.Builder().setUid(UID).build();
        final Anomaly anomaly2 = new Anomaly.Builder().setUid(UID).build();
        final Anomaly anomaly3 = new Anomaly.Builder().setUid(UID_2).build();
        anomalies.add(anomaly1);
        anomalies.add(anomaly2);
        anomalies.add(anomaly3);

        mFragment.updateAnomalySparseArray(anomalies);

        assertThat(mFragment.mAnomalySparseArray.get(UID)).containsExactly(anomaly1, anomaly2);
        assertThat(mFragment.mAnomalySparseArray.get(UID_2)).containsExactly(anomaly3);
    }

    @Test
    public void testInitAnomalyDetectionIfPossible_detectionEnabled_init() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();
        doReturn(mAnomalyDetectionPolicy).when(mFragment).getAnomalyDetectionPolicy();
        when(mAnomalyDetectionPolicy.isAnomalyDetectionEnabled()).thenReturn(true);

        mFragment.restartAnomalyDetectionIfPossible();

        verify(mLoaderManager).restartLoader(eq(PowerUsageSummary.ANOMALY_LOADER), eq(Bundle.EMPTY),
                any());
    }

    @Test
    public void testShowBothEstimates_summariesAreBothModified() {
        doReturn(new TextView(mRealContext)).when(mBatteryLayoutPref).findViewById(R.id.summary2);
        doReturn(new TextView(mRealContext)).when(mBatteryLayoutPref).findViewById(R.id.summary1);
        mFragment.onLongClick(new View(mRealContext));
        TextView summary1 = mFragment.mBatteryLayoutPref.findViewById(R.id.summary1);
        TextView summary2 = mFragment.mBatteryLayoutPref.findViewById(R.id.summary2);
        Robolectric.flushBackgroundThreadScheduler();
        assertThat(summary2.getText().toString().contains(NEW_ML_EST_SUFFIX));
        assertThat(summary1.getText().toString().contains(OLD_EST_SUFFIX));
    }

    @Test
    public void testSaveInstanceState_showAllAppsRestored() {
        Bundle bundle = new Bundle();
        mFragment.mShowAllApps = true;
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();

        mFragment.onSaveInstanceState(bundle);
        mFragment.restoreSavedInstance(bundle);

        assertThat(mFragment.mShowAllApps).isTrue();
    }

    @Test
    public void testDebugMode() {
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).isEstimateDebugEnabled();

        mFragment.restartBatteryInfoLoader();
        ArgumentCaptor<View.OnLongClickListener> listener = ArgumentCaptor.forClass(
                View.OnLongClickListener.class);
        verify(mSummary1).setOnLongClickListener(listener.capture());

        // Calling the listener should disable it.
        listener.getValue().onLongClick(mSummary1);
        verify(mSummary1).setOnLongClickListener(null);

        // Restarting the loader should reset the listener.
        mFragment.restartBatteryInfoLoader();
        verify(mSummary1, times(2)).setOnLongClickListener(any(View.OnLongClickListener.class));
    }

    @Test
    public void testRestartBatteryStatsLoader_notClearHeader_quickUpdateNotInvoked() {
        mFragment.mBatteryHeaderPreferenceController = mBatteryHeaderPreferenceController;

        mFragment.restartBatteryStatsLoader(false /* clearHeader */);

        verify(mBatteryHeaderPreferenceController, never()).quickUpdateHeaderPreference();
    }

    public static class TestFragment extends PowerUsageSummary {
        private Context mContext;

        public TestFragment(Context context) {
            mContext = context;
        }

        @Override
        public Context getContext() {
            return mContext;
        }


        @Override
        protected void refreshUi() {
            // Leave it empty for toggle apps menu test
        }
    }
}
