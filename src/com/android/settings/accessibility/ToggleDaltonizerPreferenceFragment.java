/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.ShortcutType;
import com.android.settings.accessibility.AccessibilityUtil.State;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public final class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements DaltonizerRadioButtonPreferenceController.OnChangeListener,
        SwitchBar.OnSwitchChangeListener, ShortcutPreference.OnClickListener {

    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String PREFERENCE_KEY = "daltonizer_mode_deuteranomaly";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcutType";
    // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
    private static final String KEY_SHORTCUT_TYPE = Settings.System.MASTER_MONO;
    private static final int DIALOG_ID_EDIT_SHORTCUT = 1;
    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();
    private ShortcutPreference mShortcutPreference;
    private int mShortcutType = ShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        if (sControllers.size() == 0) {
            final Resources resources = context.getResources();
            final String[] daltonizerKeys = resources.getStringArray(
                    R.array.daltonizer_mode_keys);

            for (int i = 0; i < daltonizerKeys.length; i++) {
                sControllers.add(new DaltonizerRadioButtonPreferenceController(
                        context, lifecycle, daltonizerKeys[i]));
            }
        }
        return sControllers;
    }

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initShortcutPreference(savedInstanceState);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mShortcutType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(this);
            ((DaltonizerRadioButtonPreferenceController) controller).displayPreference(
                    getPreferenceScreen());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(null);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            final CharSequence dialogTitle = getActivity().getString(
                    R.string.accessibility_shortcut_edit_dialog_title_daltonizer);
            final AlertDialog dialog = AccessibilityEditDialogUtils.showEditShortcutDialog(
                    getActivity(),
                    dialogTitle, this::callOnAlertDialogCheckboxClicked);
            initializeDialogCheckBox(dialog);
            return dialog;
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    private void initializeDialogCheckBox(AlertDialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        updateAlertDialogCheckState();
        updateAlertDialogEnableState();
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mSoftwareTypeCheckBox, ShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, ShortcutType.HARDWARE);
    }

    private void updateAlertDialogEnableState() {
        if (!mSoftwareTypeCheckBox.isChecked()) {
            mHardwareTypeCheckBox.setEnabled(false);
        } else if (!mHardwareTypeCheckBox.isChecked()) {
            mSoftwareTypeCheckBox.setEnabled(false);
        } else {
            mSoftwareTypeCheckBox.setEnabled(true);
            mHardwareTypeCheckBox.setEnabled(true);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @ShortcutType int type) {
        checkBox.setChecked((mShortcutType & type) == type);
        checkBox.setOnClickListener(v -> {
            updateShortcutType(false);
            updateAlertDialogEnableState();
        });
    }

    private void updateShortcutType(boolean saveToDB) {
        mShortcutType = ShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mShortcutType |= ShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mShortcutType |= ShortcutType.HARDWARE;
        }
        if (saveToDB) {
            setShortcutType(mShortcutType);
        }
    }

    private void setSecureIntValue(String key, @ShortcutType int value) {
        Settings.Secure.putIntForUser(getPrefContext().getContentResolver(),
                key, value, getPrefContext().getContentResolver().getUserId());
    }

    private void setShortcutType(@ShortcutType int type) {
        setSecureIntValue(KEY_SHORTCUT_TYPE, type);
    }

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getShortcutType(context);
        final CharSequence softwareTitle =
                context.getText(AccessibilityUtil.isGestureNavigateEnabled(context)
                        ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                        : R.string.accessibility_shortcut_edit_dialog_title_software);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & ShortcutType.SOFTWARE) == ShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & ShortcutType.HARDWARE) == ShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);
        return AccessibilityUtil.capitalize(joinStrings);
    }

    @ShortcutType
    private int getShortcutType(Context context) {
        return getSecureIntValue(context, KEY_SHORTCUT_TYPE, ShortcutType.SOFTWARE);
    }

    @ShortcutType
    private int getSecureIntValue(Context context, String key, @ShortcutType int defaultValue) {
        return Settings.Secure.getIntForUser(
                context.getContentResolver(),
                key, defaultValue, context.getContentResolver().getUserId());
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateShortcutType(true);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            return SettingsEnums.DIALOG_DALTONIZER_EDIT_SHORTCUT;
        }
        return 0;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_color_correction;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_daltonizer_settings;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? State.OFF : State.ON);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        final String switchBarText = getString(R.string.accessibility_service_master_switch_title,
                getString(R.string.accessibility_display_daltonizer_preference_title));
        switchBar.setSwitchBarText(switchBarText, switchBarText);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, isChecked ? State.ON : State.OFF);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mSwitchBar.setCheckedInternal(
                Settings.Secure.getInt(getContentResolver(), ENABLED, State.OFF) == State.ON);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        if (preference.getChecked()) {
            // TODO(b/142530063): Enable shortcut when checkbox is checked.
        } else {
            // TODO(b/142530063): Disable shortcut when checkbox is unchecked.
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mShortcutType = getShortcutType(getPrefContext());
        showDialog(DIALOG_ID_EDIT_SHORTCUT);
    }

    private void initShortcutPreference(Bundle savedInstanceState) {
        // Restore the Shortcut type
        if (savedInstanceState != null) {
            mShortcutType = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE, ShortcutType.DEFAULT);
        }
        if (mShortcutType == ShortcutType.DEFAULT) {
            mShortcutType = getShortcutType(getPrefContext());
        }

        // Initial ShortcutPreference widget
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        mShortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        mShortcutPreference.setOnClickListener(this);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        // Put the shortcutPreference before radioButtonPreference.
        final RadioButtonPreference radioButtonPreference = findPreference(PREFERENCE_KEY);
        mShortcutPreference.setOrder(radioButtonPreference.getOrder() - 1);
        preferenceScreen.addPreference(mShortcutPreference);
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_daltonizer_settings);
}
