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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSeekBarPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdjustVolumeRestrictedPreferenceControllerTest {

    private static final String KEY = "key";
    private AccountRestrictionHelper mAccountHelper;

    @Mock
    UserManager mUserManager;

    private Context mContext;
    private AdjustVolumeRestrictedPreferenceControllerTestable mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mAccountHelper = new AccountRestrictionHelper(mContext);
        mController =
            new AdjustVolumeRestrictedPreferenceControllerTestable(mContext, mAccountHelper, KEY);
    }

    @Test
    public void updateState_hasBaseRestriction_shouldDisable() {
        RestrictedSeekBarPreference preference = mock(RestrictedSeekBarPreference.class);
        when(RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId())).thenReturn(true);

        mController.updateState(preference);

        assertThat(preference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_NoBaseRestriction_shouldCheckRestriction() {
        RestrictedSeekBarPreference preference = spy(new RestrictedSeekBarPreference(mContext));

        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(null);
        when(RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId())).thenReturn(false);

        mController.updateState(preference);

        verify(preference).checkRestrictionAndSetDisabled(
            eq(UserManager.DISALLOW_ADJUST_VOLUME), anyInt());
    }

    private class AdjustVolumeRestrictedPreferenceControllerTestable
        extends AdjustVolumeRestrictedPreferenceController {

        private AdjustVolumeRestrictedPreferenceControllerTestable(Context context,
            AccountRestrictionHelper helper, String key) {
            super(context, helper, key);
        }

        @Override
        public int getAvailabilityStatus() {
            return 0;
        }

        @Override
        public String getPreferenceKey() {
            return KEY;
        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            return false;
        }

        @Override
        public int getSliderPosition() {
            return 0;
        }

        @Override
        public boolean setSliderPosition(int position) {
            return false;
        }

        @Override
        public int getMax() {
            return 0;
        }

        @Override
        public int getMin() {
            return 0;
        }
    }
}
