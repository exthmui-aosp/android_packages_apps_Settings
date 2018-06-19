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

package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.android.settings.R;

import androidx.preference.CheckBoxPreference;

/**
 * {@link CheckBoxPreference} that used only to display app
 */
public class AppCheckBoxPreference extends CheckBoxPreference {
    public AppCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_app);
    }

    public AppCheckBoxPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app);
    }
}
