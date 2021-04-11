/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.BatteryUsageStats;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

/**
 * Custom preference for displaying the battery level as chart graph.
 */
public class BatteryHistoryPreference extends Preference {
    private static final String TAG = "BatteryHistoryPreference";

    @VisibleForTesting
    BatteryInfo mBatteryInfo;

    private BatteryChartView mBatteryChartView;
    private BatteryChartPreferenceController mChartPreferenceController;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        final boolean isChartGraphEnabled =
            FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context)
                   .isChartGraphEnabled(context);
        Log.i(TAG, "isChartGraphEnabled: " + isChartGraphEnabled);
        if (isChartGraphEnabled) {
            setLayoutResource(R.layout.battery_chart_graph);
        }
        setSelectable(false);
    }

    void setBatteryUsageStats(@NonNull BatteryUsageStats batteryUsageStats) {
        BatteryInfo.getBatteryInfo(getContext(), info -> {
            mBatteryInfo = info;
            notifyChanged();
        }, batteryUsageStats, false);
    }

    void setChartPreferenceController(BatteryChartPreferenceController controller) {
        mChartPreferenceController = controller;
        if (mBatteryChartView != null) {
            mChartPreferenceController.setBatteryChartView(mBatteryChartView);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final long startTime = System.currentTimeMillis();
        if (mBatteryInfo == null) {
            return;
        }
        mBatteryChartView = (BatteryChartView) view.findViewById(R.id.battery_chart);
        if (mChartPreferenceController != null) {
            mChartPreferenceController.setBatteryChartView(mBatteryChartView);
        }
        BatteryUtils.logRuntime(TAG, "onBindViewHolder", startTime);
    }
}
