/*
 * Copyright (C) 2022 The exTHmUI Open Source Project
 *
 * PDX-License-Identifier: Apache-2.0
 *
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ExthmVersionPreferenceController extends BasePreferenceController {

    private static final String TAG = "ExthmVersionPreferenceController";

    private static final String KEY_EXTHM_VERSION = "ro.exthm.version";

    public ExthmVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return SystemProperties.get(KEY_EXTHM_VERSION, mContext.getString(R.string.unknown));
    }
}