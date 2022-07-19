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

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** The view model of {@code BatteryChartViewV2} */
class BatteryChartViewModel {
    private static final String TAG = "BatteryChartViewModel";

    public static final int SELECTED_INDEX_ALL = -1;
    public static final int SELECTED_INDEX_INVALID = -2;

    // We need at least 2 levels to draw a trapezoid.
    private static final int MIN_LEVELS_DATA_SIZE = 2;

    private final List<Integer> mLevels;
    private final List<String> mTexts;
    private int mSelectedIndex;

    BatteryChartViewModel(
            @NonNull List<Integer> levels, @NonNull List<String> texts, int selectedIndex) {
        Preconditions.checkArgument(
                levels.size() == texts.size()
                        && levels.size() >= MIN_LEVELS_DATA_SIZE
                        && selectedIndex >= SELECTED_INDEX_ALL
                        && selectedIndex < levels.size(),
                String.format(Locale.getDefault(), "Invalid BatteryChartViewModel"
                                + "  levels.size: %d\ntexts.size: %d\nselectedIndex: %d.",
                        levels.size(), texts.size(), selectedIndex));
        mLevels = levels;
        mTexts = texts;
        mSelectedIndex = selectedIndex;
    }

    public int size() {
        return mLevels.size();
    }

    public List<Integer> levels() {
        return mLevels;
    }

    public List<String> texts() {
        return mTexts;
    }

    public int selectedIndex() {
        return mSelectedIndex;
    }

    public void setSelectedIndex(int index) {
        mSelectedIndex = index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLevels, mTexts, mSelectedIndex);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof BatteryChartViewModel)) {
            return false;
        }
        final BatteryChartViewModel batteryChartViewModel = (BatteryChartViewModel) other;
        return Objects.equals(mLevels, batteryChartViewModel.mLevels)
                && Objects.equals(mTexts, batteryChartViewModel.mTexts)
                && mSelectedIndex == batteryChartViewModel.mSelectedIndex;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "levels: %s\ntexts: %s\nselectedIndex: %d",
                Objects.toString(mLevels), Objects.toString(mTexts), mSelectedIndex);
    }
}
