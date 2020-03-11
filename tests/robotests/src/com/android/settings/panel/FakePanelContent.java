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

package com.android.settings.panel;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;

import java.util.Arrays;
import java.util.List;

/**
 * Fake PanelContent for testing.
 */
public class FakePanelContent implements PanelContent {

    public static final String FAKE_ACTION = "fake_action";

    public static final CharSequence TITLE = "title";

    public static final List<Uri> SLICE_URIS = Arrays.asList(
        WIFI_SLICE_URI
    );

    public static final Intent INTENT = new Intent();

    private CharSequence mSubTitle;
    private IconCompat mIcon;
    private int mViewType;

    @Override
    public IconCompat getIcon() {
        return mIcon;
    }

    @Override
    public CharSequence getSubTitle() {
        return mSubTitle;
    }

    public void setIcon(IconCompat icon) {
        mIcon = icon;
    }

    public void setSubTitle(CharSequence subTitle) {
        mSubTitle = subTitle;
    }

    @Override
    public CharSequence getTitle() {
        return TITLE;
    }

    @Override
    public List<Uri> getSlices() {
        return SLICE_URIS;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return INTENT;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TESTING;
    }

    public void setViewType(int viewType) {
        mViewType = viewType;
    }

    @Override
    public int getViewType() {
        return mViewType;
    }
}
