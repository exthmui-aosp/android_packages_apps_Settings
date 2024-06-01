/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedPreference;

/**
 * Preference item for showing an accessibility activity in a preference list
 */
public class AccessibilityActivityPreference extends RestrictedPreference {
    // Index of the first preference in a preference category.
    private static final int FIRST_PREFERENCE_IN_CATEGORY_INDEX = -1;
    private static final String TARGET_FRAGMENT =
            LaunchAccessibilityActivityPreferenceFragment.class.getName();
    private final AccessibilityShortcutInfo mA11yShortcutInfo;
    private final PackageManager mPm;
    private final ComponentName mComponentName;

    public AccessibilityActivityPreference(Context context, String packageName, int uid,
            AccessibilityShortcutInfo a11yShortcutInfo) {
        super(context, packageName, uid);
        mPm = context.getPackageManager();
        mA11yShortcutInfo = a11yShortcutInfo;
        mComponentName = a11yShortcutInfo.getComponentName();
        // setup basic info for a preference
        setKey(mComponentName.flattenToString());
        setTitle(a11yShortcutInfo.getActivityInfo().loadLabel(mPm));
        setSummary(a11yShortcutInfo.loadSummary(mPm));
        setFragment(TARGET_FRAGMENT);
        setIconSize(ICON_SIZE_MEDIUM);
        setIconSpaceReserved(true);
        setPersistent(false); // Disable SharedPreferences.
        setOrder(FIRST_PREFERENCE_IN_CATEGORY_INDEX);

        final Drawable icon = getA11yActivityIcon();
        setIcon(icon);

        final Bundle extras = getExtras();
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, mComponentName);

        setupDataForOpenFragment();
    }

    private Drawable getA11yActivityIcon() {
        ActivityInfo activityInfo = mA11yShortcutInfo.getActivityInfo();
        Drawable serviceIcon;
        if (activityInfo.getIconResource() == 0) {
            serviceIcon = ContextCompat.getDrawable(getContext(),
                    R.drawable.ic_accessibility_generic);
        } else {
            serviceIcon = activityInfo.loadIcon(mPm);
        }
        return Utils.getAdaptiveIcon(getContext(), serviceIcon, Color.WHITE);
    }

    private void setupDataForOpenFragment() {
        final String prefKey = getKey();
        final int imageRes = mA11yShortcutInfo.getAnimatedImageRes();
        final CharSequence intro = mA11yShortcutInfo.loadIntro(mPm);
        final CharSequence description = mA11yShortcutInfo.loadDescription(mPm);
        final String htmlDescription = mA11yShortcutInfo.loadHtmlDescription(mPm);
        final String settingsClassName = mA11yShortcutInfo.getSettingsActivityName();
        final String tileServiceClassName = mA11yShortcutInfo.getTileServiceName();
        final int metricsCategory = FeatureFactory.getFeatureFactory()
                .getAccessibilityMetricsFeatureProvider()
                .getDownloadedFeatureMetricsCategory(mComponentName);

        RestrictedPreferenceHelper.putBasicExtras(
                this, prefKey, getTitle(), intro, description, imageRes,
                htmlDescription, mComponentName, metricsCategory);
        RestrictedPreferenceHelper.putSettingsExtras(this, getPackageName(), settingsClassName);
        RestrictedPreferenceHelper.putTileServiceExtras(
                this, getPackageName(), tileServiceClassName);
    }
}
