/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.os.UserHandle.USER_NULL;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.List;

/**
 * Settings screen for configuring, deleting or switching to a specific user.
 * It is shown when you tap on a user in the user management (UserSettings) screen.
 *
 * Arguments to this fragment must include the userId of the user (in EXTRA_USER_ID) for whom
 * to display controls.
 */
public class UserDetailsSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = UserDetailsSettings.class.getSimpleName();

    private static final String KEY_SWITCH_USER = "switch_user";
    private static final String KEY_ENABLE_TELEPHONY = "enable_calling";
    private static final String KEY_REMOVE_USER = "remove_user";

    /** Integer extra containing the userId to manage */
    static final String EXTRA_USER_ID = "user_id";

    private static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_CONFIRM_ENABLE_CALLING = 2;
    private static final int DIALOG_CONFIRM_ENABLE_CALLING_AND_SMS = 3;

    private UserManager mUserManager;
    @VisibleForTesting
    Preference mSwitchUserPref;
    private SwitchPreference mPhonePref;
    @VisibleForTesting
    Preference mRemoveUserPref;

    @VisibleForTesting
    UserInfo mUserInfo;
    private Bundle mDefaultGuestRestrictions;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER_DETAILS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        addPreferencesFromResource(R.xml.user_details_settings);

        initialize(context, getArguments());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchUserPref.setEnabled(canSwitchUserNow());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mRemoveUserPref) {
            if (canDeleteUser()) {
                showDialog(DIALOG_CONFIRM_REMOVE);
            }
            return true;
        } else if (preference == mSwitchUserPref) {
            if (canSwitchUserNow()) {
                switchUser();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            showDialog(mUserInfo.isGuest() ? DIALOG_CONFIRM_ENABLE_CALLING
                    : DIALOG_CONFIRM_ENABLE_CALLING_AND_SMS);
            return false;
        }
        enableCallsAndSms(false);
        return true;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE:
                return SettingsEnums.DIALOG_USER_REMOVE;
            case DIALOG_CONFIRM_ENABLE_CALLING:
                return SettingsEnums.DIALOG_USER_ENABLE_CALLING;
            case DIALOG_CONFIRM_ENABLE_CALLING_AND_SMS:
                return SettingsEnums.DIALOG_USER_ENABLE_CALLING_AND_SMS;
            default:
                return 0;
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE:
                return UserDialogs.createRemoveDialog(getActivity(), mUserInfo.id,
                        (dialog, which) -> removeUser());
            case DIALOG_CONFIRM_ENABLE_CALLING:
                return UserDialogs.createEnablePhoneCallsDialog(getActivity(),
                        (dialog, which) -> enableCallsAndSms(true));
            case DIALOG_CONFIRM_ENABLE_CALLING_AND_SMS:
                return UserDialogs.createEnablePhoneCallsAndSmsDialog(getActivity(),
                        (dialog, which) -> enableCallsAndSms(true));
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    @VisibleForTesting
    @Override
    protected void showDialog(int dialogId) {
        super.showDialog(dialogId);
    }

    @VisibleForTesting
    void initialize(Context context, Bundle arguments) {
        int userId = arguments != null ? arguments.getInt(EXTRA_USER_ID, USER_NULL) : USER_NULL;
        if (userId == USER_NULL) {
            throw new IllegalStateException("Arguments to this fragment must contain the user id");
        }
        mUserInfo = mUserManager.getUserInfo(userId);

        mSwitchUserPref = findPreference(KEY_SWITCH_USER);
        mPhonePref = findPreference(KEY_ENABLE_TELEPHONY);
        mRemoveUserPref = findPreference(KEY_REMOVE_USER);

        mSwitchUserPref.setTitle(
                context.getString(com.android.settingslib.R.string.user_switch_to_user,
                        mUserInfo.name));
        mSwitchUserPref.setOnPreferenceClickListener(this);

        if (!mUserManager.isAdminUser()) { // non admin users can't remove users and allow calls
            removePreference(KEY_ENABLE_TELEPHONY);
            removePreference(KEY_REMOVE_USER);
        } else {
            if (!Utils.isVoiceCapable(context)) { // no telephony
                removePreference(KEY_ENABLE_TELEPHONY);
            }

            if (!mUserInfo.isGuest()) {
                mPhonePref.setChecked(!mUserManager.hasUserRestriction(
                        UserManager.DISALLOW_OUTGOING_CALLS, new UserHandle(userId)));
                mRemoveUserPref.setTitle(R.string.user_remove_user);
            } else {
                // These are not for an existing user, just general Guest settings.
                // Default title is for calling and SMS. Change to calling-only here
                mPhonePref.setTitle(R.string.user_enable_calling);
                mDefaultGuestRestrictions = mUserManager.getDefaultGuestRestrictions();
                mPhonePref.setChecked(
                        !mDefaultGuestRestrictions.getBoolean(UserManager.DISALLOW_OUTGOING_CALLS));
                mRemoveUserPref.setTitle(R.string.user_exit_guest_title);
            }
            if (RestrictedLockUtilsInternal.hasBaseUserRestriction(context,
                    UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId())) {
                removePreference(KEY_REMOVE_USER);
            }

            mRemoveUserPref.setOnPreferenceClickListener(this);
            mPhonePref.setOnPreferenceChangeListener(this);
        }
    }

    @VisibleForTesting
    boolean canDeleteUser() {
        if (!mUserManager.isAdminUser()) {
            return false;
        }

        Context context = getActivity();
        if (context == null) {
            return false;
        }

        final RestrictedLockUtils.EnforcedAdmin removeDisallowedAdmin =
                RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context,
                        UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId());
        if (removeDisallowedAdmin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context,
                    removeDisallowedAdmin);
            return false;
        }
        return true;
    }

    @VisibleForTesting
    boolean canSwitchUserNow() {
        return mUserManager.getUserSwitchability() == UserManager.SWITCHABILITY_STATUS_OK;
    }

    @VisibleForTesting
    void switchUser() {
        try {
            ActivityManager.getService().switchUser(mUserInfo.id);
        } catch (RemoteException re) {
            Log.e(TAG, "Error while switching to other user.");
        } finally {
            finishFragment();
        }
    }

    private void enableCallsAndSms(boolean enabled) {
        mPhonePref.setChecked(enabled);
        if (mUserInfo.isGuest()) {
            mDefaultGuestRestrictions.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, !enabled);
            // SMS is always disabled for guest
            mDefaultGuestRestrictions.putBoolean(UserManager.DISALLOW_SMS, true);
            mUserManager.setDefaultGuestRestrictions(mDefaultGuestRestrictions);

            // Update the guest's restrictions, if there is a guest
            // TODO: Maybe setDefaultGuestRestrictions() can internally just set the restrictions
            // on any existing guest rather than do it here with multiple Binder calls.
            List<UserInfo> users = mUserManager.getUsers(true);
            for (UserInfo user : users) {
                if (user.isGuest()) {
                    UserHandle userHandle = UserHandle.of(user.id);
                    for (String key : mDefaultGuestRestrictions.keySet()) {
                        mUserManager.setUserRestriction(
                                key, mDefaultGuestRestrictions.getBoolean(key), userHandle);
                    }
                }
            }
        } else {
            UserHandle userHandle = UserHandle.of(mUserInfo.id);
            mUserManager.setUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS, !enabled,
                    userHandle);
            mUserManager.setUserRestriction(UserManager.DISALLOW_SMS, !enabled, userHandle);
        }
    }

    private void removeUser() {
        mUserManager.removeUser(mUserInfo.id);
        finishFragment();
    }
}
