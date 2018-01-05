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

package com.android.settings.security;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import android.content.Context;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class LockScreenPreferenceController extends BasePreferenceController {

    static final String KEY_LOCKSCREEN_PREFERENCES = "lockscreen_preferences";

    private static final int MY_USER_ID = UserHandle.myUserId();
    private final LockPatternUtils mLockPatternUtils;

    public LockScreenPreferenceController(Context context) {
        super(context, KEY_LOCKSCREEN_PREFERENCES);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider().getLockPatternUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mLockPatternUtils.isSecure(MY_USER_ID)) {
            return mLockPatternUtils.isLockScreenDisabled(MY_USER_ID)
                    ? DISABLED_FOR_USER : AVAILABLE;
        } else {
            return mLockPatternUtils.getKeyguardStoredPasswordQuality(MY_USER_ID)
                    == PASSWORD_QUALITY_UNSPECIFIED
                    ? DISABLED_FOR_USER : AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference lockscreenPreferences = screen.findPreference(getPreferenceKey());
        if (lockscreenPreferences != null) {
            lockscreenPreferences.setSummary(
                    LockScreenNotificationPreferenceController.getSummaryResource(mContext));
        }
    }
}
