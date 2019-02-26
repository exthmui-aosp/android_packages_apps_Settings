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

package com.android.settings.wifi.slice;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiSsid;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.slice.Slice;

import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;

/**
 * {@link CustomSliceable} for Wi-Fi, used by contextual homepage.
 */
public class ContextualWifiSlice extends WifiSlice {

    private static final String TAG = "ContextualWifiSlice";
    @VisibleForTesting
    boolean mPreviouslyDisplayed;

    public ContextualWifiSlice(Context context) {
        super(context);
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        if (!mPreviouslyDisplayed && !TextUtils.equals(getActiveSSID(), WifiSsid.NONE)) {
            Log.d(TAG, "Wifi is connected, no point showing any suggestion.");
            return null;
        }
        // Set mPreviouslyDisplayed to true - we will show *something* on the screen. So we should
        // keep showing this card to keep UI stable, even if wifi connects to a network later.
        mPreviouslyDisplayed = true;

        return super.getSlice();
    }
}
