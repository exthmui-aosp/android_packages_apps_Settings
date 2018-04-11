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
import android.view.View;

import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;

/**
 * A CheckboxPreference with a disabled checkbox. Differs from CheckboxPreference.setDisabled()
 * in that the text is not dimmed.
 */
public class DisabledCheckBoxPreference extends CheckBoxPreference {

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisabledCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View view = holder.findViewById(android.R.id.checkbox);
        view.setEnabled(false);
        holder.itemView.setEnabled(false);
    }

    @Override
    protected void performClick(View view) {
        // Do nothing
    }
}
