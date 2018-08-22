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

package com.android.settings.applications;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.datausage.AppStateDataUsageBridge;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowApplicationsState;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowApplicationsState.class})
public class SpecialAppAccessPreferenceControllerTest {

    private Context mContext;
    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private PreferenceScreen mScreen;

    private SpecialAppAccessPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(new int[]{0});
        mController = new SpecialAppAccessPreferenceController(mContext, "test_key");
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);

        mController.mSession = mSession;
    }

    @Test
    public void getAvailabilityState_unsearchable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final ArrayList<ApplicationsState.AppEntry> apps = new ArrayList<>();
        final ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.hasLauncherEntry = true;
        entry.info = new ApplicationInfo();
        entry.extraInfo = new AppStateDataUsageBridge.DataUsageState(
                true /* whitelisted */, false /* blacklisted */);
        apps.add(entry);
        when(mSession.getAllApps()).thenReturn(apps);

        mController.displayPreference(mScreen);
        mController.onExtraInfoUpdated();

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getResources().getQuantityString(
                        R.plurals.special_access_summary, 1, 1));
    }
}
