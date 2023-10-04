/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.applications.appcompat;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_16_9;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_4_3;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * App specific activity to show aspect ratio overrides
 */
public class UserAspectRatioDetails extends AppInfoBase implements
        RadioWithImagePreference.OnClickListener {
    private static final String TAG = UserAspectRatioDetails.class.getSimpleName();

    private static final String KEY_HEADER_SUMMARY = "app_aspect_ratio_summary";
    private static final String KEY_HEADER_BUTTONS = "header_view";
    private static final String KEY_PREF_FULLSCREEN = "fullscreen_pref";
    private static final String KEY_PREF_HALF_SCREEN = "half_screen_pref";
    private static final String KEY_PREF_DISPLAY_SIZE = "display_size_pref";
    private static final String KEY_PREF_16_9 = "16_9_pref";
    private static final String KEY_PREF_4_3 = "4_3_pref";
    @VisibleForTesting
    static final String KEY_PREF_DEFAULT = "app_default_pref";
    @VisibleForTesting
    static final String KEY_PREF_3_2 = "3_2_pref";

    private final List<RadioWithImagePreference> mAspectRatioPreferences = new ArrayList<>();

    @NonNull private UserAspectRatioManager mUserAspectRatioManager;
    @NonNull private String mSelectedKey = KEY_PREF_DEFAULT;

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserAspectRatioManager = new UserAspectRatioManager(getContext());
        initPreferences();
        try {
            final int userAspectRatio = mUserAspectRatioManager
                    .getUserMinAspectRatioValue(mPackageName, mUserId);
            mSelectedKey = getSelectedKey(userAspectRatio);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get user min aspect ratio");
        }
        refreshUi();
    }

    @Override
    public void onRadioButtonClicked(@NonNull RadioWithImagePreference selected) {
        final String selectedKey = selected.getKey();
        if (mSelectedKey.equals(selectedKey)) {
            return;
        }
        final int userAspectRatio = getSelectedUserMinAspectRatio(selectedKey);
        try {
            getAspectRatioManager().setUserMinAspectRatio(mPackageName, mUserId, userAspectRatio);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to set user min aspect ratio");
            return;
        }
        // Only update to selected aspect ratio if nothing goes wrong
        mSelectedKey = selectedKey;
        updateAllPreferences(mSelectedKey);
        Log.d(TAG, "Killing application process " + mPackageName);
        try {
            final IActivityManager am = ActivityManager.getService();
            am.stopAppForUser(mPackageName, mUserId);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to stop application " + mPackageName);
        }
    }

    @Override
    public int getMetricsCategory() {
        // TODO(b/292566895): add metrics for logging
        return 0;
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        updateAllPreferences(mSelectedKey);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    private void launchApplication() {
        Intent launchIntent = mPm.getLaunchIntentForPackage(mPackageName)
                .addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        if (launchIntent != null) {
            getContext().startActivityAsUser(launchIntent, new UserHandle(mUserId));
        }
    }

    @PackageManager.UserMinAspectRatio
    private int getSelectedUserMinAspectRatio(@NonNull String selectedKey) {
        switch (selectedKey) {
            case KEY_PREF_FULLSCREEN:
                return USER_MIN_ASPECT_RATIO_FULLSCREEN;
            case KEY_PREF_HALF_SCREEN:
                return USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
            case KEY_PREF_DISPLAY_SIZE:
                return USER_MIN_ASPECT_RATIO_DISPLAY_SIZE;
            case KEY_PREF_3_2:
                return USER_MIN_ASPECT_RATIO_3_2;
            case KEY_PREF_4_3:
                return USER_MIN_ASPECT_RATIO_4_3;
            case KEY_PREF_16_9:
                return USER_MIN_ASPECT_RATIO_16_9;
            default:
                return USER_MIN_ASPECT_RATIO_UNSET;
        }
    }

    @NonNull
    private String getSelectedKey(@PackageManager.UserMinAspectRatio int userMinAspectRatio) {
        switch (userMinAspectRatio) {
            case USER_MIN_ASPECT_RATIO_FULLSCREEN:
                return KEY_PREF_FULLSCREEN;
            case USER_MIN_ASPECT_RATIO_SPLIT_SCREEN:
                return KEY_PREF_HALF_SCREEN;
            case USER_MIN_ASPECT_RATIO_DISPLAY_SIZE:
                return KEY_PREF_DISPLAY_SIZE;
            case USER_MIN_ASPECT_RATIO_3_2:
                return KEY_PREF_3_2;
            case USER_MIN_ASPECT_RATIO_4_3:
                return KEY_PREF_4_3;
            case USER_MIN_ASPECT_RATIO_16_9:
                return KEY_PREF_16_9;
            default:
                return KEY_PREF_DEFAULT;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Preference pref = EntityHeaderController
                .newInstance(getActivity(), this, null /* header */)
                .setIcon(Utils.getBadgedIcon(getContext(), mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .done(getActivity(), getPrefContext());

        getPreferenceScreen().addPreference(pref);
    }

    private void initPreferences() {
        addPreferencesFromResource(R.xml.user_aspect_ratio_details);

        final String summary = getContext().getResources().getString(
                R.string.aspect_ratio_main_summary, Build.MODEL);
        findPreference(KEY_HEADER_SUMMARY).setTitle(summary);

        ((ActionButtonsPreference) findPreference(KEY_HEADER_BUTTONS))
                .setButton1Text(R.string.launch_instant_app)
                .setButton1Icon(R.drawable.ic_settings_open)
                .setButton1OnClickListener(v -> launchApplication());

        addPreference(KEY_PREF_DEFAULT, USER_MIN_ASPECT_RATIO_UNSET);
        addPreference(KEY_PREF_FULLSCREEN, USER_MIN_ASPECT_RATIO_FULLSCREEN);
        addPreference(KEY_PREF_DISPLAY_SIZE, USER_MIN_ASPECT_RATIO_DISPLAY_SIZE);
        addPreference(KEY_PREF_HALF_SCREEN, USER_MIN_ASPECT_RATIO_SPLIT_SCREEN);
        addPreference(KEY_PREF_16_9, USER_MIN_ASPECT_RATIO_16_9);
        addPreference(KEY_PREF_4_3, USER_MIN_ASPECT_RATIO_4_3);
        addPreference(KEY_PREF_3_2, USER_MIN_ASPECT_RATIO_3_2);
    }

    private void addPreference(@NonNull String key,
            @PackageManager.UserMinAspectRatio int aspectRatio) {
        final RadioWithImagePreference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        if (!mUserAspectRatioManager.hasAspectRatioOption(aspectRatio, mPackageName)) {
            pref.setVisible(false);
            return;
        }
        pref.setTitle(mUserAspectRatioManager.getAccessibleEntry(aspectRatio, mPackageName));
        pref.setOnClickListener(this);
        mAspectRatioPreferences.add(pref);
    }

    private void updateAllPreferences(@NonNull String selectedKey) {
        for (RadioWithImagePreference pref : mAspectRatioPreferences) {
            pref.setChecked(selectedKey.equals(pref.getKey()));
        }
    }

    @VisibleForTesting
    UserAspectRatioManager getAspectRatioManager() {
        return mUserAspectRatioManager;
    }
}
