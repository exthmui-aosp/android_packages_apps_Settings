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

import android.app.AutomaticZenRule;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.service.notification.ZenModeConfig;
import android.view.View;
import android.widget.CheckBox;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Map;
import java.util.Objects;

public class ZenRulePreference extends TwoTargetPreference {
    private static final ManagedServiceSettings.Config CONFIG =
            ZenModeAutomationSettings.getConditionProviderConfig();
    final String mId;
    boolean appExists;
    final Fragment mParent;
    final Preference mPref;
    final Context mContext;
    final ZenModeBackend mBackend;
    final ZenServiceListing mServiceListing;
    final PackageManager mPm;
    final MetricsFeatureProvider mMetricsFeatureProvider;
    final AutomaticZenRule mRule;
    CharSequence mName;

    private boolean mIsSystemRule;
    private Intent mIntent;
    private boolean mChecked;
    private CheckBox mCheckBox;

    public ZenRulePreference(Context context,
            final Map.Entry<String, AutomaticZenRule> ruleEntry,
            Fragment parent, MetricsFeatureProvider metricsProvider) {
        super(context);
        setLayoutResource(R.layout.preference_checkable_two_target);
        mBackend = ZenModeBackend.getInstance(context);
        mContext = context;
        mRule = ruleEntry.getValue();
        mName = mRule.getName();
        mId = ruleEntry.getKey();
        mParent = parent;
        mPm = mContext.getPackageManager();
        mServiceListing = new ZenServiceListing(mContext, CONFIG);
        mServiceListing.reloadApprovedServices();
        mPref = this;
        mMetricsFeatureProvider = metricsProvider;
        mChecked = mRule.isEnabled();
        setAttributes(mRule);
        setWidgetLayoutResource(getSecondTargetResId());
    }

    protected int getSecondTargetResId() {
        if (mIntent != null) {
            return R.layout.zen_rule_widget;
        }
        return 0;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        View settingsWidget = view.findViewById(android.R.id.widget_frame);
        View divider = view.findViewById(R.id.two_target_divider);
        if (mIntent != null) {
            divider.setVisibility(View.VISIBLE);
            settingsWidget.setVisibility(View.VISIBLE);
            settingsWidget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(mIntent);
                }
            });
        } else {
            divider.setVisibility(View.GONE);
            settingsWidget.setVisibility(View.GONE);
            settingsWidget.setOnClickListener(null);
        }

        View checkboxContainer = view.findViewById(R.id.checkbox_container);
        if (checkboxContainer != null) {
            checkboxContainer.setOnClickListener(mOnCheckBoxClickListener);
        }
        mCheckBox = (CheckBox) view.findViewById(com.android.internal.R.id.checkbox);
        if (mCheckBox != null) {
            mCheckBox.setChecked(mChecked);
        }
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mCheckBox != null) {
            mCheckBox.setChecked(checked);
        }
    }

    public void setName(String name) {
        if (!Objects.equals(mName, name)) {
            mName = name;
            setTitle(mName);
        }
    }

    @Override
    public void onClick() {
        mOnCheckBoxClickListener.onClick(null);
    }

    private View.OnClickListener mOnCheckBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRule.setEnabled(!mChecked);
            mBackend.updateZenRule(mId, mRule);
            setChecked(mRule.isEnabled());
            setAttributes(mRule);
        }
    };

    protected void setAttributes(AutomaticZenRule rule) {
        final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(
                rule.getConditionId(), true);
        final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.getConditionId());
        mIsSystemRule = isSchedule || isEvent;

        try {
            ApplicationInfo info = mPm.getApplicationInfo(rule.getOwner().getPackageName(), 0);
            setSummary(computeRuleSummary(rule, mIsSystemRule, info.loadLabel(mPm)));
        } catch (PackageManager.NameNotFoundException e) {
            appExists = false;
            return;
        }

        appExists = true;
        setTitle(mName);
        setPersistent(false);

        final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                : isEvent ? ZenModeEventRuleSettings.ACTION : "";
        ServiceInfo si = mServiceListing.findService(rule.getOwner());
        ComponentName settingsActivity = AbstractZenModeAutomaticRulePreferenceController.
                getSettingsActivity(si);
        mIntent = AbstractZenModeAutomaticRulePreferenceController.getRuleIntent(action,
                settingsActivity, mId);
        setKey(mId);
    }

    private String computeRuleSummary(AutomaticZenRule rule, boolean isSystemRule,
            CharSequence providerLabel) {
        return (rule == null || !rule.isEnabled())
                ? mContext.getResources().getString(R.string.switch_off_text)
                : mContext.getResources().getString(R.string.switch_on_text);
    }
}
