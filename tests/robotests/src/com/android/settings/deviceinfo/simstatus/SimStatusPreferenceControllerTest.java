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

package com.android.settings.deviceinfo.simstatus;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SimStatusPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private Preference mFirstSimPreference;
    @Mock
    private Preference mSecondSimPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Fragment mFragment;
    @Mock
    private PreferenceCategory mCategory;

    private Context mContext;
    private Resources mResources;
    private SlotSimStatus mSlotSimStatus;
    private SimStatusPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);

        mockService(Context.TELEPHONY_SERVICE, TelephonyManager.class, mTelephonyManager);

        mockService(Context.USER_SERVICE, UserManager.class, mUserManager);
        final List<Preference> preferencePool = new ArrayList<Preference>();
        preferencePool.add(mFirstSimPreference);
        preferencePool.add(mSecondSimPreference);

        mController = spy(new SimStatusPreferenceController(mContext, "sim_status") {
            @Override
            public Preference createNewPreference(Context context) {
                return preferencePool.remove(0);
            }
        });
        doReturn(BasePreferenceController.AVAILABLE).when(mController).getAvailabilityStatus();
        when(mScreen.getContext()).thenReturn(mContext);
        final String categoryKey = "device_detail_category";
        when(mScreen.findPreference(categoryKey)).thenReturn(mCategory);
        final String baseEntryKey = "sim_status";
        when(mScreen.findPreference(baseEntryKey)).thenReturn(mPreference);
        final String prefKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(prefKey);
        when(mPreference.isVisible()).thenReturn(true);
    }

    @Test
    public void displayPreference_multiSim_shouldAddSecondPreference() {
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);
        SlotSimStatus slotSimStatus = new SlotSimStatus(mContext);
        mController.init(mFragment, slotSimStatus);

        mController.displayPreference(mScreen);

        verify(mCategory).addPreference(mSecondSimPreference);
    }

    @Test
    public void updateState_singleSim_shouldSetSingleSimTitleAndSummary() {
        when(mTelephonyManager.getPhoneCount()).thenReturn(1);
        SlotSimStatus slotSimStatus = new SlotSimStatus(mContext);
        mController.init(mFragment, slotSimStatus);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mFirstSimPreference).setTitle(mContext.getString(R.string.sim_status_title));
        verify(mFirstSimPreference).setSummary(anyString());
    }

    @Test
    public void updateState_multiSim_shouldSetMultiSimTitleAndSummary() {
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);
        SlotSimStatus slotSimStatus = new SlotSimStatus(mContext);
        mController.init(mFragment, slotSimStatus);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mFirstSimPreference).setTitle(
                mContext.getString(R.string.sim_status_title_sim_slot, 1 /* sim slot */));
        verify(mSecondSimPreference).setTitle(
                mContext.getString(R.string.sim_status_title_sim_slot, 2 /* sim slot */));
        verify(mFirstSimPreference).setSummary(anyString());
        verify(mSecondSimPreference).setSummary(anyString());
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartDialogFragment() {
        when(mFragment.getChildFragmentManager()).thenReturn(
                mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS));
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);
        SlotSimStatus slotSimStatus = new SlotSimStatus(mContext);
        mController.init(mFragment, slotSimStatus);
        mController.displayPreference(mScreen);

        mController.handlePreferenceTreeClick(mFirstSimPreference);

        verify(mFragment).getChildFragmentManager();
    }

    private <T> void mockService(String serviceName, Class<T> serviceClass, T service) {
        when(mContext.getSystemServiceName(serviceClass)).thenReturn(serviceName);
        when(mContext.getSystemService(serviceName)).thenReturn(service);
    }
}
