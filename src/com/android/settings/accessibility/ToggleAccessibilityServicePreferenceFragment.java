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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;

import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.ShortcutType;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/** Fragment for providing toggle bar and basic accessibility service setup. */
public class ToggleAccessibilityServicePreferenceFragment extends
        ToggleFeaturePreferenceFragment implements ShortcutPreference.OnClickListener {

    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcutType";
    // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
    private static final String KEY_SHORTCUT_TYPE = Settings.System.MASTER_MONO;
    private ShortcutPreference mShortcutPreference;
    private int mShortcutType = ShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

    public static final int ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION = 1;
    private CharSequence mDialogTitle;
    private LockPatternUtils mLockPatternUtils;


    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateSwitchBarToggleSwitch();
                }
            };

    private Dialog mDialog;

    private final View.OnClickListener mViewOnClickListener =
            (View view) -> {
                if (view.getId() == R.id.permission_enable_allow_button) {
                    if (isFullDiskEncrypted()) {
                        String title = createConfirmCredentialReasonMessage();
                        Intent intent = ConfirmDeviceCredentialActivity.createIntent(title, null);
                        startActivityForResult(intent,
                                ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION);
                    } else {
                        handleConfirmServiceEnabled(true);
                        if (isServiceSupportAccessibilityButton()) {
                            showDialog(DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL);
                        }
                    }
                } else if (view.getId() == R.id.permission_enable_deny_button) {
                    handleConfirmServiceEnabled(false);
                } else {
                    throw new IllegalArgumentException();
                }
                mDialog.dismiss();
            };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_SERVICE;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater infalter) {
        // Do not call super. We don't want to see the "Help & feedback" option on this page so as
        // not to confuse users who think they might be able to send feedback about a specific
        // accessibility service from this page.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(getActivity());
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
        mSettingsContentObserver.register(getContentResolver());
        updateSwitchBarToggleSwitch();
        super.onResume();
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
        AccessibilityUtils.setAccessibilityServiceState(getActivity(), toggledService, enabled);
    }

    // IMPORTANT: Refresh the info since there are dynamically changing
    // capabilities. For
    // example, before JellyBean MR2 the user was granting the explore by touch
    // one.
    private AccessibilityServiceInfo getAccessibilityServiceInfo() {
        List<AccessibilityServiceInfo> serviceInfos = AccessibilityManager.getInstance(
                getActivity()).getInstalledAccessibilityServiceList();
        final int serviceInfoCount = serviceInfos.size();
        for (int i = 0; i < serviceInfoCount; i++) {
            AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
            ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
            if (mComponentName.getPackageName().equals(resolveInfo.serviceInfo.packageName)
                    && mComponentName.getClassName().equals(resolveInfo.serviceInfo.name)) {
                return serviceInfo;
            }
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogType.ENABLE_WARNING: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createCapabilitiesDialog(getActivity(), info, mViewOnClickListener);
                break;
            }
            case DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL: {
                if (AccessibilityUtil.isGestureNavigateEnabled(getContext())) {
                    mDialog = AccessibilityGestureNavigationTutorial
                            .showGestureNavigationTutorialDialog(getActivity());
                } else {
                    mDialog = AccessibilityGestureNavigationTutorial
                            .showAccessibilityButtonTutorialDialog(getActivity());
                }
                break;
            }
            case DialogType.EDIT_SHORTCUT: {
                final CharSequence dialogTitle = getActivity().getString(
                        R.string.accessibility_shortcut_edit_dialog_title, mDialogTitle);
                mDialog = AccessibilityEditDialogUtils.showEditShortcutDialog(getActivity(),
                        dialogTitle, this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(mDialog);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
            }
        }
        return mDialog;
    }

    private void initializeDialogCheckBox(Dialog dialog) {
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
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogType.ENABLE_WARNING:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_ENABLE;
            case DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL:
                return AccessibilityUtil.isGestureNavigateEnabled(getContext())
                        ? SettingsEnums.DIALOG_TOGGLE_SCREEN_GESTURE_NAVIGATION
                        : SettingsEnums.DIALOG_TOGGLE_SCREEN_ACCESSIBILITY_BUTTON;
            case DialogType.EDIT_SHORTCUT:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT;
            default:
                return 0;
        }
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
        final String switchBarText = (info == null) ? "" :
                getString(R.string.accessibility_service_master_switch_title,
                info.getResolveInfo().loadLabel(getPackageManager()));
        switchBar.setSwitchBarText(switchBarText, switchBarText);
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
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setOrder(-1);
        mShortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        mShortcutPreference.setOnClickListener(this);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        // Put the shortcutPreference before settingsPreference.
        mShortcutPreference.setOrder(-1);
        preferenceScreen.addPreference(mShortcutPreference);
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
    }

    public String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    private void updateSwitchBarToggleSwitch() {
        final boolean checked = AccessibilityUtils.getEnabledServicesFromSettings(getActivity())
                .contains(mComponentName);
        mSwitchBar.setCheckedInternal(checked);
    }

    /**
     * Return whether the device is encrypted with legacy full disk encryption. Newer devices
     * should be using File Based Encryption.
     *
     * @return true if device is encrypted
     */
    private boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION) {
            if (resultCode == Activity.RESULT_OK) {
                handleConfirmServiceEnabled(true);
                // The user confirmed that they accept weaker encryption when
                // enabling the accessibility service, so change encryption.
                // Since we came here asynchronously, check encryption again.
                if (isFullDiskEncrypted()) {
                    mLockPatternUtils.clearEncryptionPassword();
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, 0);
                }
            } else {
                handleConfirmServiceEnabled(false);
            }
        }
    }

    private boolean isServiceSupportAccessibilityButton() {
        final AccessibilityManager ams = (AccessibilityManager) getContext().getSystemService(
                Context.ACCESSIBILITY_SERVICE);
        final List<AccessibilityServiceInfo> services = ams.getInstalledAccessibilityServiceList();

        for (AccessibilityServiceInfo info : services) {
            if ((info.flags & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0) {
                ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
                if (serviceInfo != null && TextUtils.equals(serviceInfo.name,
                        getAccessibilityServiceInfo().getResolveInfo().serviceInfo.name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleConfirmServiceEnabled(boolean confirmed) {
        mSwitchBar.setCheckedInternal(confirmed);
        getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, confirmed);
        onPreferenceToggled(mPreferenceKey, confirmed);
    }

    private String createConfirmCredentialReasonMessage() {
        int resId = R.string.enable_service_password_reason;
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING: {
                resId = R.string.enable_service_pattern_reason;
            }
            break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX: {
                resId = R.string.enable_service_pin_reason;
            }
            break;
        }
        return getString(resId, getAccessibilityServiceInfo().getResolveInfo()
                .loadLabel(getPackageManager()));
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (checked) {
                    mSwitchBar.setCheckedInternal(false);
                    getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, false);
                    showDialog(DialogType.ENABLE_WARNING);
                } else {
                    handleConfirmServiceEnabled(false);
                }
                return true;
            }
        });
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
        showDialog(DialogType.EDIT_SHORTCUT);
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        super.onProcessArguments(arguments);
        // Settings title and intent.
        String settingsTitle = arguments.getString(AccessibilitySettings.EXTRA_SETTINGS_TITLE);
        String settingsComponentName = arguments.getString(
                AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(settingsComponentName.toString()));
            if (!getPackageManager().queryIntentActivities(settingsIntent, 0).isEmpty()) {
                mSettingsTitle = settingsTitle;
                mSettingsIntent = settingsIntent;
                setHasOptionsMenu(true);
            }
        }

        mComponentName = arguments.getParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME);

        // Settings animated image.
        int animatedImageRes = arguments.getInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES);
        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(mComponentName.getPackageName())
                .appendPath(String.valueOf(animatedImageRes))
                .build();

        // Settings html description.
        mHtmlDescription = arguments.getCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION);

        // Get Accessibility service name.
        mDialogTitle = getAccessibilityServiceInfo().getResolveInfo().loadLabel(
                getPackageManager());
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogType {
        int ENABLE_WARNING = 1;
        int LAUNCH_ACCESSIBILITY_TUTORIAL = 2;
        int EDIT_SHORTCUT = 3;
    }
}
