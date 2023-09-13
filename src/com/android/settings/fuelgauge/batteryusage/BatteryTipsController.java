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

package com.android.settings.fuelgauge.batteryusage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.function.Function;

/** Controls the update for battery tips card */
public class BatteryTipsController extends BasePreferenceController {

    private static final String TAG = "BatteryTipsController";
    private static final String ROOT_PREFERENCE_KEY = "battery_tips_category";
    private static final String CARD_PREFERENCE_KEY = "battery_tips_card";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    @VisibleForTesting
    BatteryTipsCardPreference mCardPreference;

    public BatteryTipsController(Context context) {
        super(context, ROOT_PREFERENCE_KEY);
        final FeatureFactory featureFactory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCardPreference = screen.findPreference(CARD_PREFERENCE_KEY);
    }

    private <T> T getInfo(PowerAnomalyEvent powerAnomalyEvent,
                          Function<WarningBannerInfo, T> warningBannerInfoSupplier,
                          Function<WarningItemInfo, T> warningItemInfoSupplier) {
        if (warningBannerInfoSupplier != null && powerAnomalyEvent.hasWarningBannerInfo()) {
            return warningBannerInfoSupplier.apply(powerAnomalyEvent.getWarningBannerInfo());
        } else if (warningItemInfoSupplier != null && powerAnomalyEvent.hasWarningItemInfo()) {
            return warningItemInfoSupplier.apply(powerAnomalyEvent.getWarningItemInfo());
        }
        return null;
    }

    private String getStringFromResource(int resourceId, int resourceIndex) {
        if (resourceId < 0) {
            return null;
        }
        final String[] stringArray = mContext.getResources().getStringArray(resourceId);
        return (resourceIndex >= 0 && resourceIndex < stringArray.length)
                ? stringArray[resourceIndex] : null;
    }

    private int getResourceId(int resourceId, int resourceIndex, String defType) {
        final String key = getStringFromResource(resourceId, resourceIndex);
        return TextUtils.isEmpty(key) ? 0
                : mContext.getResources().getIdentifier(key, defType, mContext.getPackageName());
    }

    private String getString(PowerAnomalyEvent powerAnomalyEvent,
                             Function<WarningBannerInfo, String> warningBannerInfoSupplier,
                             Function<WarningItemInfo, String> warningItemInfoSupplier,
                             int resourceId, int resourceIndex) {
        String string =
                getInfo(powerAnomalyEvent, warningBannerInfoSupplier, warningItemInfoSupplier);
        return (!TextUtils.isEmpty(string) || resourceId < 0) ? string
                : getStringFromResource(resourceId, resourceIndex);
    }

    void handleBatteryTipsCardUpdated(PowerAnomalyEvent powerAnomalyEvent) {
        if (powerAnomalyEvent == null) {
            mCardPreference.setVisible(false);
            return;
        }

        // Get card icon and color styles
        final int cardStyleId = powerAnomalyEvent.getType().getNumber();
        final int iconResId = getResourceId(
                R.array.battery_tips_card_icons, cardStyleId, "drawable");
        final int colorResId = getResourceId(
                R.array.battery_tips_card_colors, cardStyleId, "color");

        // Get card preference strings and navigate fragment info
        final PowerAnomalyKey powerAnomalyKey = powerAnomalyEvent.hasKey()
                ? powerAnomalyEvent.getKey() : null;
        final int resourceIndex = powerAnomalyKey != null ? powerAnomalyKey.getNumber() : -1;

        String titleString = getString(powerAnomalyEvent, WarningBannerInfo::getTitleString,
                WarningItemInfo::getTitleString, R.array.power_anomaly_titles, resourceIndex);
        if (titleString.isEmpty()) {
            mCardPreference.setVisible(false);
            return;
        }

        String mainBtnString = getString(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonString, WarningItemInfo::getMainButtonString,
                R.array.power_anomaly_main_btn_strings, resourceIndex);
        String dismissBtnString = getString(powerAnomalyEvent,
                WarningBannerInfo::getCancelButtonString, WarningItemInfo::getCancelButtonString,
                R.array.power_anomaly_dismiss_btn_strings, resourceIndex);

        String destinationClassName = getInfo(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonDestination, null);
        Integer sourceMetricsCategory = getInfo(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonSourceMetricsCategory, null);
        String preferenceHighlightKey = getInfo(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonSourceHighlightKey, null);

        // Update card preference and main button fragment launcher
        mCardPreference.setAnomalyEventId(powerAnomalyEvent.getEventId());
        mCardPreference.setPowerAnomalyKey(powerAnomalyKey);
        mCardPreference.setTitle(titleString);
        mCardPreference.setIconResourceId(iconResId);
        mCardPreference.setMainButtonStrokeColorResourceId(colorResId);
        mCardPreference.setMainButtonLabel(mainBtnString);
        mCardPreference.setDismissButtonLabel(dismissBtnString);
        mCardPreference.setMainButtonLauncherInfo(
                destinationClassName, sourceMetricsCategory, preferenceHighlightKey);
        mCardPreference.setVisible(true);

        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW, powerAnomalyEvent.getEventId());
    }
}
