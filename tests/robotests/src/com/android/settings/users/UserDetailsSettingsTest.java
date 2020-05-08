/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.users;

import static android.os.UserManager.SWITCHABILITY_STATUS_OK;
import static android.os.UserManager.SWITCHABILITY_STATUS_USER_IN_CALL;
import static android.os.UserManager.SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowDevicePolicyManager.class
})
public class UserDetailsSettingsTest {

    private static final String KEY_SWITCH_USER = "switch_user";
    private static final String KEY_ENABLE_TELEPHONY = "enable_calling";
    private static final String KEY_REMOVE_USER = "remove_user";

    private static final int DIALOG_CONFIRM_REMOVE = 1;

    @Mock
    private TelephonyManager mTelephonyManager;

    private ShadowUserManager mUserManager;

    @Mock
    private Preference mSwitchUserPref;
    @Mock
    private SwitchPreference mPhonePref;
    @Mock
    private Preference mRemoveUserPref;

    private FragmentActivity mActivity;
    private Context mContext;
    private UserDetailsSettings mFragment;
    private Bundle mArguments;
    private UserInfo mUserInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = spy(ActivityController.of(new FragmentActivity()).get());
        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new UserDetailsSettings());
        mArguments = new Bundle();

        UserManager userManager = (UserManager) mContext.getSystemService(
                Context.USER_SERVICE);
        mUserManager = Shadow.extract(userManager);

        doReturn(mTelephonyManager).when(mActivity).getSystemService(Context.TELEPHONY_SERVICE);

        ReflectionHelpers.setField(mFragment, "mUserManager", userManager);
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();

        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        doReturn("").when(mActivity).getString(anyInt(), anyString());

        doReturn(mSwitchUserPref).when(mFragment).findPreference(KEY_SWITCH_USER);
        doReturn(mPhonePref).when(mFragment).findPreference(KEY_ENABLE_TELEPHONY);
        doReturn(mRemoveUserPref).when(mFragment).findPreference(KEY_REMOVE_USER);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test(expected = IllegalStateException.class)
    public void initialize_nullArguments_shouldThrowException() {
        mFragment.initialize(mActivity, null);
    }

    @Test(expected = IllegalStateException.class)
    public void initialize_emptyArguments_shouldThrowException() {
        mFragment.initialize(mActivity, new Bundle());
    }

    @Test
    public void initialize_userSelected_shouldSetupSwitchPref() {
        setupSelectedUser();
        doReturn("Switch to " + mUserInfo.name)
                .when(mActivity).getString(anyInt(), anyString());

        mFragment.initialize(mActivity, mArguments);

        verify(mActivity).getString(com.android.settingslib.R.string.user_switch_to_user,
                mUserInfo.name);
        verify(mSwitchUserPref).setTitle("Switch to " + mUserInfo.name);
        verify(mSwitchUserPref).setOnPreferenceClickListener(mFragment);
        verify(mFragment, never()).removePreference(KEY_SWITCH_USER);
    }

    @Test
    public void initialize_guestSelected_shouldSetupSwitchPref() {
        setupSelectedGuest();
        doReturn("Switch to " + mUserInfo.name)
                .when(mActivity).getString(anyInt(), anyString());

        mFragment.initialize(mActivity, mArguments);

        verify(mActivity).getString(com.android.settingslib.R.string.user_switch_to_user,
                mUserInfo.name);
        verify(mSwitchUserPref).setTitle("Switch to " + mUserInfo.name);
        verify(mSwitchUserPref).setOnPreferenceClickListener(mFragment);
        verify(mFragment, never()).removePreference(KEY_SWITCH_USER);
    }

    @Test
    public void onResume_canSwitch_shouldEnableSwitchPref() {
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_OK);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(true);
    }

    @Test
    public void onResume_userInCall_shouldDisableSwitchPref() {
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_USER_IN_CALL);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void onResume_switchDisallowed_shouldDisableSwitchPref() {
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void onResume_systemUserLocked_shouldDisableSwitchPref() {
        mUserManager.setSwitchabilityStatus(UserManager.SWITCHABILITY_STATUS_SYSTEM_USER_LOCKED);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void initialize_adminWithTelephony_shouldShowPhonePreference() {
        setupSelectedUser();
        doReturn(true).when(mTelephonyManager).isVoiceCapable();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment, never()).removePreference(KEY_ENABLE_TELEPHONY);
        verify(mPhonePref).setOnPreferenceChangeListener(mFragment);
    }

    @Test
    public void initialize_adminNoTelephony_shouldNotShowPhonePreference() {
        setupSelectedUser();
        doReturn(false).when(mTelephonyManager).isVoiceCapable();
        mUserManager.setIsAdminUser(true);
        doReturn(null).when(mActivity).getSystemService(Context.TELEPHONY_SERVICE);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_ENABLE_TELEPHONY);
    }

    @Test
    public void initialize_nonAdminWithTelephony_shouldNotShowPhonePreference() {
        setupSelectedUser();
        doReturn(true).when(mTelephonyManager).isVoiceCapable();
        mUserManager.setIsAdminUser(false);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_ENABLE_TELEPHONY);
    }

    @Test
    public void initialize_adminSelectsSecondaryUser_shouldShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setTitle(R.string.user_remove_user);
        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_adminSelectsGuest_shouldShowRemovePreference() {
        setupSelectedGuest();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setTitle(R.string.user_exit_guest_title);
        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_nonAdmin_shouldNotShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(false);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_disallowRemoveUserRestriction_shouldNotShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mUserManager.addBaseUserRestriction(UserManager.DISALLOW_REMOVE_USER);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_userHasCallRestriction_shouldSetPhoneSwitchUnChecked() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mUserManager.setUserRestriction(mUserInfo.getUserHandle(),
                UserManager.DISALLOW_OUTGOING_CALLS, true);

        mFragment.initialize(mActivity, mArguments);

        verify(mPhonePref).setChecked(false);
    }

    @Test
    public void initialize_noCallRestriction_shouldSetPhoneSwitchChecked() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mPhonePref).setChecked(true);
    }

    @Test
    public void initialize_guestSelected_noCallRestriction_shouldSetPhonePreference() {
        setupSelectedGuest();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mPhonePref).setTitle(R.string.user_enable_calling);
        verify(mPhonePref).setChecked(true);
    }

    @Test
    public void initialize_guestSelected_callRestriction_shouldSetPhonePreference() {
        setupSelectedGuest();
        mUserManager.setIsAdminUser(true);
        mUserManager.addGuestUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS);

        mFragment.initialize(mActivity, mArguments);

        verify(mPhonePref).setTitle(R.string.user_enable_calling);
        verify(mPhonePref).setChecked(false);
    }

    @Test
    public void onPreferenceClick_switchClicked_canSwitch_shouldSwitch() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_OK);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mUserInfo = mUserInfo;

        mFragment.onPreferenceClick(mSwitchUserPref);

        verify(mFragment).switchUser();
    }

    @Test
    public void onPreferenceClick_switchClicked_canNotSwitch_doNothing() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mUserInfo = mUserInfo;

        mFragment.onPreferenceClick(mSwitchUserPref);

        verify(mFragment, never()).switchUser();
    }

    @Test
    public void onPreferenceClick_removeClicked_canDelete_shouldShowDialog() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceClick(mRemoveUserPref);

        verify(mFragment).canDeleteUser();
        verify(mFragment).showDialog(DIALOG_CONFIRM_REMOVE);
    }

    @Test
    public void onPreferenceClick_removeClicked_canNotDelete_doNothing() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(false);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceClick(mRemoveUserPref);

        verify(mFragment).canDeleteUser();
        verify(mFragment, never()).showDialog(DIALOG_CONFIRM_REMOVE);
    }

    @Test
    public void onPreferenceClick_unknownPreferenceClicked_doNothing() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;

        mFragment.onPreferenceClick(mock(UserPreference.class));

        verify(mFragment).onPreferenceClick(any());
        verifyNoMoreInteractions(mFragment);
    }

    @Test
    public void canDeleteUser_nonAdminUser_shouldReturnFalse() {
        mUserManager.setIsAdminUser(false);

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isFalse();
    }

    @Test
    public void canDeleteUser_adminSelectsUser_noRestrictions_shouldReturnTrue() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isTrue();
    }

    @Test
    public void canDeleteUser_adminSelectsUser_hasRemoveRestriction_shouldReturnFalse() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        ComponentName componentName = new ComponentName("test", "test");
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerComponentOnAnyUser(componentName);
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerUserId(UserHandle.myUserId());
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(UserHandle.myUserId(),
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        mUserManager.setUserRestrictionSources(
                UserManager.DISALLOW_REMOVE_USER,
                UserHandle.of(UserHandle.myUserId()),
                enforcingUsers
        );

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isFalse();
    }

    private void setupSelectedUser() {
        mArguments.putInt("user_id", 1);
        mUserInfo = new UserInfo(1, "Tom", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED,
                UserManager.USER_TYPE_FULL_SECONDARY);

        mUserManager.addProfile(mUserInfo);
    }

    private void setupSelectedGuest() {
        mArguments.putInt("user_id", 23);
        mUserInfo = new UserInfo(23, "Guest", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_GUEST,
                UserManager.USER_TYPE_FULL_GUEST);

        mUserManager.addProfile(mUserInfo);
    }
}
