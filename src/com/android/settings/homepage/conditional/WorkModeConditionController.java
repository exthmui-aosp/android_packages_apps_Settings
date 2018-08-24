/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import com.android.settings.Settings;

import java.util.List;
import java.util.Objects;

public class WorkModeConditionController implements ConditionalCardController {

    static final int ID = Objects.hash("WorkModeConditionController");

    private static final IntentFilter FILTER = new IntentFilter();

    static {
        FILTER.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        FILTER.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
    }

    private final Context mAppContext;
    private final UserManager mUm;
    private final ConditionManager mConditionManager;
    private final Receiver mReceiver;

    private UserHandle mUserHandle;

    public WorkModeConditionController(Context appContext, ConditionManager manager) {
        mAppContext = appContext;
        mUm = mAppContext.getSystemService(UserManager.class);
        mConditionManager = manager;
        mReceiver = new Receiver();
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        updateUserHandle();
        return mUserHandle != null && mUm.isQuietModeEnabled(mUserHandle);
    }

    @Override
    public void onPrimaryClick(Context context) {
        context.startActivity(new Intent(context,
                Settings.AccountDashboardActivity.class));
    }

    @Override
    public void onActionClick() {
        if (mUserHandle != null) {
            mUm.requestQuietModeEnabled(false, mUserHandle);
        }
    }

    @Override
    public void startMonitoringStateChange() {
        mAppContext.registerReceiver(mReceiver, FILTER);
    }

    @Override
    public void stopMonitoringStateChange() {
        mAppContext.unregisterReceiver(mReceiver);
    }

    private void updateUserHandle() {
        List<UserInfo> profiles = mUm.getProfiles(UserHandle.myUserId());
        final int profilesCount = profiles.size();
        mUserHandle = null;
        for (int i = 0; i < profilesCount; i++) {
            UserInfo userInfo = profiles.get(i);
            if (userInfo.isManagedProfile()) {
                // We assume there's only one managed profile, otherwise UI needs to change.
                mUserHandle = userInfo.getUserHandle();
                break;
            }
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(action, Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    || TextUtils.equals(action, Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)) {
                mConditionManager.onConditionChanged();
            }
        }
    }
}
