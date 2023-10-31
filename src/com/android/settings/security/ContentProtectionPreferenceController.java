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
package com.android.settings.security;

import static android.view.contentprotection.flags.Flags.settingUiEnabled;

import static com.android.internal.R.string.config_defaultContentProtectionService;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.core.BasePreferenceController;

public class ContentProtectionPreferenceController extends BasePreferenceController {

    public ContentProtectionPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!settingUiEnabled() || getContentProtectionServiceComponentName() == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @VisibleForTesting
    @Nullable
    protected String getContentProtectionServiceFlatComponentName() {
        return mContext.getString(config_defaultContentProtectionService);
    }

    @Nullable
    private ComponentName getContentProtectionServiceComponentName() {
        String flatComponentName = getContentProtectionServiceFlatComponentName();
        if (flatComponentName == null) {
            return null;
        }
        return ComponentName.unflattenFromString(flatComponentName);
    }
}
