/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.settingslib.R;

/**
 * FooterPreference that can have any key or ordering.
 */
public class NotificationFooterPreference extends Preference {

    public NotificationFooterPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(
                context, R.attr.footerPreferenceStyle, android.R.attr.preferenceStyle));
        init();
    }

    public NotificationFooterPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = holder.itemView.findViewById(android.R.id.title);
        title.setMovementMethod(new LinkMovementMethod());
        title.setClickable(false);
        title.setLongClickable(false);
    }

    private void init() {
        setIcon(R.drawable.ic_info_outline_24dp);
        setSelectable(false);
    }
}
