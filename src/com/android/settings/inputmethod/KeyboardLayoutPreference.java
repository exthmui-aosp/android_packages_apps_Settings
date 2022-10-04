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

package com.android.settings.inputmethod;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class KeyboardLayoutPreference extends Preference {

    private ImageView mCheckIcon;
    private boolean mIsMark;

    public KeyboardLayoutPreference(Context context, String layoutName, boolean defaultMark) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_check_icon);
        setTitle(layoutName);
        mIsMark = defaultMark;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mCheckIcon = (ImageView) holder.findViewById(R.id.keyboard_check_icon);
        setCheckMark(mIsMark);
    }

    public void setCheckMark(boolean isMark) {
        if (mCheckIcon != null) {
            mCheckIcon.setVisibility(isMark ? View.VISIBLE : View.INVISIBLE);
            mIsMark = isMark;
        }
    }
}
