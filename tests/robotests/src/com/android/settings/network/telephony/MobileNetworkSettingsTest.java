/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.settings.core.FeatureFlags;
import com.android.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import androidx.fragment.app.FragmentActivity;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class MobileNetworkSettingsTest {
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private NetworkStatsManager mNetworkStatsManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private FragmentActivity mActivity;

    private Context mContext;
    private MobileNetworkSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(NetworkStatsManager.class)).thenReturn(mNetworkStatsManager);
        ShadowEntityHeaderController.setUseMock(mock(EntityHeaderController.class));

        mFragment = spy(new MobileNetworkSettings());
        final Bundle args = new Bundle();
        final int subscriptionId = 1234;
        args.putInt(Settings.EXTRA_SUB_ID, subscriptionId);
        mFragment.setArguments(args);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getSystemService(NetworkPolicyManager.class)).thenReturn(
                mNetworkPolicyManager);
    }

    @Test
    public void onAttach_noV2Flag_noCrash() {
        mFragment.onAttach(mContext);
    }

    @Test
    public void onAttach_v2Flag_noCrash() {
        FeatureFlagPersistent.setEnabled(mContext, FeatureFlags.NETWORK_INTERNET_V2, true);
        mFragment.onAttach(mContext);
    }

    @Test
    public void createPreferenceControllers_noV2Flag_noDataUsageSummaryController() {
        final List<AbstractPreferenceController> controllers =
                mFragment.createPreferenceControllers(mContext);
        assertThat(controllers.stream().filter(
                c -> c.getClass().equals(DataUsageSummaryPreferenceController.class))
                .count())
                .isEqualTo(0);
    }

    @Test
    public void createPreferenceControllers_v2Flag_createsDataUsageSummaryController() {
        FeatureFlagPersistent.setEnabled(mContext, FeatureFlags.NETWORK_INTERNET_V2, true);

        final List<AbstractPreferenceController> controllers =
                mFragment.createPreferenceControllers(mContext);
        assertThat(controllers.stream().filter(
                c -> c.getClass().equals(DataUsageSummaryPreferenceController.class))
                .count())
                .isEqualTo(1);
    }
}
