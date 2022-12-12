/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.widget.AppPreference;

/**
 * Custom preference for displaying battery usage info as a bar and an icon on
 * the left for the subsystem/app type.
 *
 * The battery usage info could be usage percentage or usage time. The preference
 * won't show any icon if it is null.
 */
public class PowerGaugePreference extends AppPreference {

    private static final double PERCENTAGE_TO_SHOW_THRESHOLD = 1f;

    private BatteryEntry mInfo;
    private BatteryDiffEntry mBatteryDiffEntry;
    private CharSequence mContentDescription;
    private CharSequence mProgress;
    private boolean mShowAnomalyIcon;

    public PowerGaugePreference(Context context, Drawable icon, CharSequence contentDescription,
            BatteryEntry info) {
        this(context, null, icon, contentDescription, info);
    }

    public PowerGaugePreference(Context context) {
        this(context, null, null, null, null);
    }

    public PowerGaugePreference(Context context, AttributeSet attrs) {
        this(context, attrs, null, null, null);
    }

    private PowerGaugePreference(Context context, AttributeSet attrs, Drawable icon,
            CharSequence contentDescription, BatteryEntry info) {
        super(context, attrs);
        if (icon != null) {
            setIcon(icon);
        }
        setWidgetLayoutResource(R.layout.preference_widget_summary);
        mInfo = info;
        mContentDescription = contentDescription;
        mShowAnomalyIcon = false;
    }

    /** Sets the content description. */
    public void setContentDescription(String name) {
        mContentDescription = name;
        notifyChanged();
    }

    /** Sets the percent of total. */
    public void setPercent(double percentOfTotal) {
        mProgress = percentOfTotal < PERCENTAGE_TO_SHOW_THRESHOLD
                ? "-" : Utils.formatPercentage(percentOfTotal, true);
        notifyChanged();
    }

    /** Gets the percent of total. */
    public String getPercent() {
        return mProgress.toString();
    }

    /** Sets the subtitle. */
    public void setSubtitle(CharSequence subtitle) {
        mProgress = subtitle;
        notifyChanged();
    }

    /** Gets the subtitle. */
    public CharSequence getSubtitle() {
        return mProgress;
    }

    /** Sets whether to show anomaly icon */
    public void shouldShowAnomalyIcon(boolean showAnomalyIcon) {
        mShowAnomalyIcon = showAnomalyIcon;
        notifyChanged();
    }

    /** Gets whether to show anomaly icon */
    public boolean showAnomalyIcon() {
        return mShowAnomalyIcon;
    }

    public void setBatteryDiffEntry(BatteryDiffEntry entry) {
        mBatteryDiffEntry = entry;
    }

    BatteryEntry getInfo() {
        return mInfo;
    }

    BatteryDiffEntry getBatteryDiffEntry() {
        return mBatteryDiffEntry;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        final TextView subtitle = (TextView) view.findViewById(R.id.widget_summary);
        subtitle.setText(mProgress);
        if (mShowAnomalyIcon) {
            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_warning_24dp, 0,
                    0, 0);
        } else {
            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        }
        if (mContentDescription != null) {
            final TextView titleView = (TextView) view.findViewById(android.R.id.title);
            titleView.setContentDescription(mContentDescription);
        }
    }
}
