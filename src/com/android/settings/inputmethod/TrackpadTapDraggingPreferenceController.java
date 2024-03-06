/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputSettings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class TrackpadTapDraggingPreferenceController extends TogglePreferenceController {

    public TrackpadTapDraggingPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return InputSettings.useTouchpadTapDragging(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setTouchpadTapDragging(mContext, isChecked);
        // TODO(b/321978150): add a metric for tap dragging settings changes.
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return InputSettings.isTouchpadTapDraggingFeatureFlagEnabled()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
