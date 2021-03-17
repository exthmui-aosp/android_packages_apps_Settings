/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.widget.SettingsMainSwitchBar.OnBeforeCheckedChangeListener;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * SettingsMainSwitchPreference is a Preference with a customized Switch.
 * This component is used as the main switch of the page
 * to enable or disable the prefereces on the page.
 */
public class SettingsMainSwitchPreference extends TwoStatePreference {
    private final List<OnBeforeCheckedChangeListener> mBeforeCheckedChangeListeners =
            new ArrayList<>();
    private final List<OnMainSwitchChangeListener> mSwitchChangeListeners = new ArrayList<>();

    private SettingsMainSwitchBar mMainSwitchBar;
    private CharSequence mTitle;
    private boolean mIsVisible;

    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    public SettingsMainSwitchPreference(Context context) {
        super(context);
        init(context, null);
    }

    public SettingsMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingsMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SettingsMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.setDividerAllowedAbove(true);
        holder.setDividerAllowedBelow(false);

        mMainSwitchBar = (SettingsMainSwitchBar) holder.findViewById(R.id.main_switch_bar);

        mMainSwitchBar.show();
        updateStatus(isChecked());
        registerListenerToSwitchBar();

        if (!mIsVisible) {
            mMainSwitchBar.hide();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.preference_widget_main_switch);
        mIsVisible = true;

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs,
                    androidx.preference.R.styleable.Preference, 0/*defStyleAttr*/,
                    0/*defStyleRes*/);
            final CharSequence title = TypedArrayUtils.getText(a,
                    androidx.preference.R.styleable.Preference_title,
                    androidx.preference.R.styleable.Preference_android_title);
            if (!TextUtils.isEmpty(title)) {
                setTitle(title.toString());
            }
            a.recycle();
        }
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setChecked(checked);
        }
    }

    /**
     * Return the SettingsMainSwitchBar
     */
    public final SettingsMainSwitchBar getSwitchBar() {
        return mMainSwitchBar;
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setTitle(mTitle);
        }
    }

    /**
     * Update the switch status of preference
     */
    public void updateStatus(boolean checked) {
        setChecked(checked);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setChecked(checked);
            mMainSwitchBar.setTitle(mTitle);
            mMainSwitchBar.setDisabledByAdmin(mEnforcedAdmin);
            mMainSwitchBar.show();
        }
    }

    /**
     * Show the MainSwitchBar
     */
    public void show() {
        mIsVisible = true;
        if (mMainSwitchBar != null) {
            mMainSwitchBar.show();
        }
    }

    /**
     * Hide the MainSwitchBar
     */
    public void hide() {
        mIsVisible = false;
        if (mMainSwitchBar != null) {
            mMainSwitchBar.hide();
        }
    }

    /**
     * Returns if the MainSwitchBar is visible.
     */
    public boolean isShowing() {
        if (mMainSwitchBar != null) {
            return mMainSwitchBar.isShowing();
        }
        return false;
    }

    /**
     * Update the status of switch but doesn't notify the mOnBeforeListener.
     */
    public void setCheckedInternal(boolean checked) {
        super.setChecked(checked);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setCheckedInternal(checked);
        }
    }

    /**
     * Enable or disable the text and switch.
     */
    public void setSwitchBarEnabled(boolean enabled) {
        setEnabled(enabled);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setEnabled(enabled);
        }
    }

    /**
     * Set the OnBeforeCheckedChangeListener.
     */
    public void setOnBeforeCheckedChangeListener(OnBeforeCheckedChangeListener listener) {
        if (mMainSwitchBar == null) {
            mBeforeCheckedChangeListeners.add(listener);
        } else {
            mMainSwitchBar.setOnBeforeCheckedChangeListener(listener);
        }
    }

    /**
     * Adds a listener for switch changes
     */
    public void addOnSwitchChangeListener(OnMainSwitchChangeListener listener) {
        if (mMainSwitchBar == null) {
            mSwitchChangeListeners.add(listener);
        } else {
            mMainSwitchBar.addOnSwitchChangeListener(listener);
        }
    }

    /**
     * Remove a listener for switch changes
     */
    public void removeOnSwitchChangeListener(OnMainSwitchChangeListener listener) {
        if (mMainSwitchBar == null) {
            mSwitchChangeListeners.remove(listener);
        } else {
            mMainSwitchBar.removeOnSwitchChangeListener(listener);
        }
    }

    /**
     * If admin is not null, disables the text and switch but keeps the view clickable.
     * Otherwise, calls setEnabled which will enables the entire view including
     * the text and switch.
     */
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        mEnforcedAdmin = admin;
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setDisabledByAdmin(mEnforcedAdmin);
        }
    }

    private void registerListenerToSwitchBar() {
        for (OnBeforeCheckedChangeListener listener : mBeforeCheckedChangeListeners) {
            mMainSwitchBar.setOnBeforeCheckedChangeListener(listener);
        }
        for (OnMainSwitchChangeListener listener : mSwitchChangeListeners) {
            mMainSwitchBar.addOnSwitchChangeListener(listener);
        }
        mBeforeCheckedChangeListeners.clear();
        mSwitchChangeListeners.clear();
    }
}
