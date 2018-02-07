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
package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.TwoTargetPreference;

/**
 * Shows an app icon, title and summary. Has a second switch touch target.
 */
public class NotificationAppPreference extends TwoTargetPreference {

    private int mProgress;
    private boolean mProgressVisible;
    private Switch mSwitch;
    private boolean mChecked;
    private boolean mEnableSwitch = true;

    public NotificationAppPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app);
    }

    public NotificationAppPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_app);
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_master_switch;
    }

    public void setProgress(int amount) {
        mProgress = amount;
        mProgressVisible = true;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        view.findViewById(R.id.summary_container)
                .setVisibility(TextUtils.isEmpty(getSummary()) ? View.GONE : View.VISIBLE);
        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (mProgressVisible) {
            progress.setProgress(mProgress);
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }

        final View widgetView = view.findViewById(android.R.id.widget_frame);
        if (widgetView != null) {
            widgetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSwitch != null && !mSwitch.isEnabled()) {
                        return;
                    }
                    setChecked(!mChecked);
                    if (!callChangeListener(mChecked)) {
                        setChecked(!mChecked);
                    } else {
                        persistBoolean(mChecked);
                    }
                }
            });
        }

        mSwitch = (Switch) view.findViewById(R.id.switchWidget);
        if (mSwitch != null) {
            mSwitch.setContentDescription(getTitle());
            mSwitch.setChecked(mChecked);
            mSwitch.setEnabled(mEnableSwitch);
        }
    }

    public boolean isChecked() {
        return mSwitch != null && mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    public void setSwitchEnabled(boolean enabled) {
        mEnableSwitch = enabled;
        if (mSwitch != null) {
            mSwitch.setEnabled(enabled);
        }
    }

    /**
     * If admin is not null, disables the switch.
     * Otherwise, keep it enabled.
     */
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        setSwitchEnabled(admin == null);
    }

    public Switch getSwitch() {
        return mSwitch;
    }
}
