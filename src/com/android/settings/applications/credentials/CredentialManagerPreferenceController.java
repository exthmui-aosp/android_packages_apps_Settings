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

package com.android.settings.applications.credentials;

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialManager;
import android.credentials.CredentialProviderInfo;
import android.credentials.SetEnabledProvidersException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/** Queries available credential manager providers and adds preferences for them. */
public class CredentialManagerPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "CredentialManagerPreferenceController";
    private static final String ALTERNATE_INTENT = "android.settings.SYNC_SETTINGS";
    private static final String PRIMARY_INTENT = "android.settings.CREDENTIAL_PROVIDER";
    private static final int MAX_SELECTABLE_PROVIDERS = 5;

    private final PackageManager mPm;
    private final IconDrawableFactory mIconFactory;
    private final List<CredentialProviderInfo> mServices;
    private final Set<String> mEnabledPackageNames;
    private final @Nullable CredentialManager mCredentialManager;
    private final Executor mExecutor;
    private final Map<String, SwitchPreference> mPrefs = new HashMap<>(); // key is package name
    private final List<ServiceInfo> mPendingServiceInfos = new ArrayList<>();

    private @Nullable FragmentManager mFragmentManager = null;
    private @Nullable Delegate mDelegate = null;

    public CredentialManagerPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
        mServices = new ArrayList<>();
        mEnabledPackageNames = new HashSet<>();
        mExecutor = ContextCompat.getMainExecutor(mContext);
        mCredentialManager =
                getCredentialManager(context, preferenceKey.equals("credentials_test"));
    }

    private @Nullable CredentialManager getCredentialManager(Context context, boolean isTest) {
        if (isTest) {
            return null;
        }

        Object service = context.getSystemService(Context.CREDENTIAL_SERVICE);

        if (service != null && CredentialManager.isServiceEnabled(context)) {
            return (CredentialManager) service;
        }

        return null;
    }

    @VisibleForTesting
    public boolean isConnected() {
        return mCredentialManager != null;
    }

    /**
     * Initializes the controller with the parent fragment and adds the controller to observe its
     * lifecycle. Also stores the fragment manager which is used to open dialogs.
     *
     * @param fragment the fragment to use as the parent
     * @param fragmentManager the fragment manager to use
     * @param intent the intent used to start the activity
     * @param delegate the delegate to send results back to
     */
    public void init(
            DashboardFragment fragment,
            FragmentManager fragmentManager,
            @Nullable Intent launchIntent,
            @NonNull Delegate delegate) {
        fragment.getSettingsLifecycle().addObserver(this);
        mFragmentManager = fragmentManager;
        setDelegate(delegate);
        verifyReceivedIntent(launchIntent);
    }

    /**
     * Parses and sets the package component name. Returns a boolean as to whether this was
     * successful.
     */
    @VisibleForTesting
    boolean verifyReceivedIntent(Intent launchIntent) {
        if (launchIntent == null || launchIntent.getAction() == null) {
            return false;
        }

        final String action = launchIntent.getAction();
        final boolean isCredProviderAction =
                TextUtils.equals(action, PRIMARY_INTENT);
        final boolean isExistingAction = TextUtils.equals(action, ALTERNATE_INTENT);
        final boolean isValid = isCredProviderAction || isExistingAction;

        if (!isValid) {
            return false;
        }

        // After this point we have received a set credential manager provider intent
        // so we should return a cancelled result if the data we got is no good.
        if (launchIntent.getData() == null) {
            setActivityResult(Activity.RESULT_CANCELED);
            return false;
        }

        String packageName = launchIntent.getData().getSchemeSpecificPart();
        if (packageName == null) {
            setActivityResult(Activity.RESULT_CANCELED);
            return false;
        }

        mPendingServiceInfos.clear();
        for (CredentialProviderInfo cpi : mServices) {
            final ServiceInfo serviceInfo = cpi.getServiceInfo();
            if (serviceInfo.packageName.equals(packageName)) {
                mPendingServiceInfos.add(serviceInfo);
            }
        }

        // Don't set the result as RESULT_OK here because we should wait for the user to
        // enable the provider.
        if (!mPendingServiceInfos.isEmpty()) {
            return true;
        }

        setActivityResult(Activity.RESULT_CANCELED);
        return false;
    }

    @VisibleForTesting
    void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }

    private void setActivityResult(int resultCode) {
        if (mDelegate == null) {
            Log.e(TAG, "Missing delegate");
            return;
        }
        mDelegate.setActivityResult(resultCode);
    }

    private void handleIntent() {
        List<ServiceInfo> pendingServiceInfos = new ArrayList<>(mPendingServiceInfos);
        mPendingServiceInfos.clear();
        if (pendingServiceInfos.isEmpty()) {
            return;
        }

        ServiceInfo serviceInfo = pendingServiceInfos.get(0);
        ApplicationInfo appInfo = serviceInfo.applicationInfo;
        CharSequence appName = "";
        if (appInfo.nonLocalizedLabel != null) {
            appName = appInfo.loadLabel(mPm);
        }

        // Stop if there is no name.
        if (TextUtils.isEmpty(appName)) {
            return;
        }

        NewProviderConfirmationDialogFragment fragment =
                newNewProviderConfirmationDialogFragment(serviceInfo.packageName, appName);
        if (fragment == null || mFragmentManager == null) {
            return;
        }

        fragment.show(mFragmentManager, NewProviderConfirmationDialogFragment.TAG);
    }

    @OnLifecycleEvent(ON_CREATE)
    void onCreate(LifecycleOwner lifecycleOwner) {
        if (mCredentialManager == null) {
            return;
        }

        setAvailableServices(
                lifecycleOwner,
                mCredentialManager.getCredentialProviderServices(
                        getUser(), CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY));
    }

    @VisibleForTesting
    void setAvailableServices(
            LifecycleOwner lifecycleOwner, List<CredentialProviderInfo> availableServices) {
        mServices.clear();
        mServices.addAll(availableServices);

        // If there is a pending dialog then show it.
        handleIntent();

        mEnabledPackageNames.clear();
        for (CredentialProviderInfo cpi : availableServices) {
            if (cpi.isEnabled()) {
                mEnabledPackageNames.add(cpi.getServiceInfo().packageName);
            }
        }

        for (String packageName : mPrefs.keySet()) {
            mPrefs.get(packageName).setChecked(mEnabledPackageNames.contains(packageName));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mServices.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        // Since the UI is being cleared, clear any refs.
        mPrefs.clear();

        PreferenceGroup group = screen.findPreference(getPreferenceKey());
        Context context = screen.getContext();
        mPrefs.putAll(buildPreferenceList(context, group));
    }

    /** Aggregates the list of services and builds a list of UI prefs to show. */
    @VisibleForTesting
    public Map<String, SwitchPreference> buildPreferenceList(
            Context context, PreferenceGroup group) {
        // Group the services by package name.
        Map<String, List<CredentialProviderInfo>> groupedInfos = new HashMap<>();
        for (CredentialProviderInfo cpi : mServices) {
            String packageName = cpi.getServiceInfo().packageName;
            if (!groupedInfos.containsKey(packageName)) {
                groupedInfos.put(packageName, new ArrayList<>());
            }

            groupedInfos.get(packageName).add(cpi);
        }

        // Build the pref list.
        Map<String, SwitchPreference> output = new HashMap<>();
        for (String packageName : groupedInfos.keySet()) {
            List<CredentialProviderInfo> infos = groupedInfos.get(packageName);
            CredentialProviderInfo firstInfo = infos.get(0);
            ServiceInfo firstServiceInfo = firstInfo.getServiceInfo();
            CharSequence title = firstInfo.getLabel(context);
            Drawable icon = firstInfo.getServiceIcon(context);

            if (infos.size() > 1) {
                // If there is more than one then group them under the package.
                ApplicationInfo appInfo = firstServiceInfo.applicationInfo;
                if (appInfo.nonLocalizedLabel != null) {
                    title = appInfo.loadLabel(mPm);
                }
                icon = mIconFactory.getBadgedIcon(appInfo, getUser());
            }

            // If there is no title then show the package manager.
            if (TextUtils.isEmpty(title)) {
                title = firstServiceInfo.packageName;
            }

            // Build the pref and add it to the output & group.
            SwitchPreference pref = addProviderPreference(context, title, icon, packageName, firstInfo.getSettingsSubtitle());
            output.put(packageName, pref);
            group.addPreference(pref);
        }

        return output;
    }

    /** Creates a preference object based on the provider info. */
    @VisibleForTesting
    public SwitchPreference createPreference(Context context, CredentialProviderInfo service) {
        CharSequence label = service.getLabel(context);
        return addProviderPreference(
                context,
                label == null ? "" : label,
                service.getServiceIcon(mContext),
                service.getServiceInfo().packageName,
                service.getSettingsSubtitle());
    }

    /**
     * Enables the package name as an enabled credential manager provider.
     *
     * @param packageName the package name to enable
     */
    @VisibleForTesting
    public boolean togglePackageNameEnabled(String packageName) {
        if (mEnabledPackageNames.size() >= MAX_SELECTABLE_PROVIDERS) {
            return false;
        } else {
            mEnabledPackageNames.add(packageName);
            commitEnabledPackages();
            return true;
        }
    }

    /**
     * Disables the package name as a credential manager provider.
     *
     * @param packageName the package name to disable
     */
    @VisibleForTesting
    public void togglePackageNameDisabled(String packageName) {
        mEnabledPackageNames.remove(packageName);
        commitEnabledPackages();
    }

    /** Returns the enabled credential manager provider package names. */
    @VisibleForTesting
    public Set<String> getEnabledProviders() {
        return mEnabledPackageNames;
    }

    /**
     * Returns the enabled credential manager provider flattened component names that can be stored
     * in the setting.
     */
    @VisibleForTesting
    public List<String> getEnabledSettings() {
        // Get all the component names that match the enabled package names.
        List<String> enabledServices = new ArrayList<>();
        for (CredentialProviderInfo service : mServices) {
            ComponentName cn = service.getServiceInfo().getComponentName();
            if (mEnabledPackageNames.contains(service.getServiceInfo().packageName)) {
                enabledServices.add(cn.flattenToString());
            }
        }

        return enabledServices;
    }

    private SwitchPreference addProviderPreference(
            @NonNull Context prefContext,
            @NonNull CharSequence title,
            @Nullable Drawable icon,
            @NonNull String packageName,
            @Nullable CharSequence subtitle) {
        final SwitchPreference pref = new SwitchPreference(prefContext);
        pref.setTitle(title);
        pref.setChecked(mEnabledPackageNames.contains(packageName));

        if (icon != null) {
            pref.setIcon(Utils.getSafeIcon(icon));
        }

        if (subtitle != null) {
            pref.setSummary(subtitle);
        }

        pref.setOnPreferenceClickListener(
                p -> {
                    boolean isChecked = pref.isChecked();

                    if (isChecked) {
                        // Show the error if too many enabled.
                        if (!togglePackageNameEnabled(packageName)) {
                            final DialogFragment fragment = newErrorDialogFragment();

                            if (fragment == null || mFragmentManager == null) {
                                return true;
                            }

                            fragment.show(mFragmentManager, ErrorDialogFragment.TAG);

                            // The user set the check to true so we need to set it back.
                            pref.setChecked(false);
                        }

                        return true;
                    } else {
                        // If we are disabling the last enabled provider then show a warning.
                        if (mEnabledPackageNames.size() <= 1) {
                            final DialogFragment fragment =
                                    newConfirmationDialogFragment(packageName, title, pref);

                            if (fragment == null || mFragmentManager == null) {
                                return true;
                            }

                            fragment.show(mFragmentManager, ConfirmationDialogFragment.TAG);
                        } else {
                            togglePackageNameDisabled(packageName);
                        }
                    }

                    return true;
                });

        return pref;
    }

    private void commitEnabledPackages() {
        // Commit using the CredMan API.
        if (mCredentialManager == null) {
            return;
        }

        List<String> enabledServices = getEnabledSettings();
        mCredentialManager.setEnabledProviders(
                enabledServices,
                getUser(),
                mExecutor,
                new OutcomeReceiver<Void, SetEnabledProvidersException>() {
                    @Override
                    public void onResult(Void result) {
                        Log.i(TAG, "setEnabledProviders success");
                    }

                    @Override
                    public void onError(SetEnabledProvidersException e) {
                        Log.e(TAG, "setEnabledProviders error: " + e.toString());
                    }
                });
    }

    /** Create the new provider confirmation dialog. */
    private @Nullable NewProviderConfirmationDialogFragment
            newNewProviderConfirmationDialogFragment(
                    @NonNull String packageName, @NonNull CharSequence appName) {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {
                        completeEnableProviderDialogBox(whichButton, packageName);
                    }
                };

        return new NewProviderConfirmationDialogFragment(host, packageName, appName);
    }

    @VisibleForTesting
    void completeEnableProviderDialogBox(int whichButton, String packageName) {
        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
            if (togglePackageNameEnabled(packageName)) {
                // Enable all prefs.
                if (mPrefs.containsKey(packageName)) {
                    mPrefs.get(packageName).setChecked(true);
                }
                setActivityResult(Activity.RESULT_OK);
            } else {
                // There are too many providers so set the result as cancelled.
                setActivityResult(Activity.RESULT_CANCELED);

                // Show the error if too many enabled.
                final DialogFragment fragment = newErrorDialogFragment();

                if (fragment == null || mFragmentManager == null) {
                    return;
                }

                fragment.show(mFragmentManager, ErrorDialogFragment.TAG);
            }
        } else {
            // The user clicked the cancel button so send that result back.
            setActivityResult(Activity.RESULT_CANCELED);
        }
    }

    private @Nullable ErrorDialogFragment newErrorDialogFragment() {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {}
                };

        return new ErrorDialogFragment(host);
    }

    private @Nullable ConfirmationDialogFragment newConfirmationDialogFragment(
            @NonNull String packageName,
            @NonNull CharSequence appName,
            @NonNull SwitchPreference pref) {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {
                        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                            // Since the package is now enabled then we
                            // should remove it from the enabled list.
                            togglePackageNameDisabled(packageName);
                        } else if (whichButton == DialogInterface.BUTTON_NEGATIVE) {
                            // Set the checked back to true because we
                            // backed out of turning this off.
                            pref.setChecked(true);
                        }
                    }
                };

        return new ConfirmationDialogFragment(host, packageName, appName);
    }

    private int getUser() {
        UserHandle workUser = getWorkProfileUser();
        return workUser != null ? workUser.getIdentifier() : UserHandle.myUserId();
    }

    /** Called when the dialog button is clicked. */
    private static interface DialogHost {
        void onDialogClick(int whichButton);
    }

    /** Called to send messages back to the parent fragment. */
    public static interface Delegate {
        void setActivityResult(int resultCode);
    }

    /** Dialog fragment parent class. */
    private abstract static class CredentialManagerDialogFragment extends DialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "CredentialManagerDialogFragment";
        public static final String PACKAGE_NAME_KEY = "package_name";
        public static final String APP_NAME_KEY = "app_name";

        private DialogHost mDialogHost;

        CredentialManagerDialogFragment(DialogHost dialogHost) {
            super();
            mDialogHost = dialogHost;
        }

        public DialogHost getDialogHost() {
            return mDialogHost;
        }
    }

    /** Dialog showing error when too many providers are selected. */
    public static class ErrorDialogFragment extends CredentialManagerDialogFragment {

        ErrorDialogFragment(DialogHost dialogHost) {
            super(dialogHost);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getContext().getString(R.string.credman_error_message_title))
                    .setMessage(getContext().getString(R.string.credman_error_message))
                    .setPositiveButton(android.R.string.ok, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {}
    }

    /**
     * Confirmation dialog fragment shows a dialog to the user to confirm that they are disabling a
     * provider.
     */
    public static class ConfirmationDialogFragment extends CredentialManagerDialogFragment {

        ConfirmationDialogFragment(
                DialogHost dialogHost, @NonNull String packageName, @NonNull CharSequence appName) {
            super(dialogHost);

            final Bundle argument = new Bundle();
            argument.putString(PACKAGE_NAME_KEY, packageName);
            argument.putCharSequence(APP_NAME_KEY, appName);
            setArguments(argument);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final String title =
                    getContext()
                            .getString(
                                    R.string.credman_confirmation_message_title,
                                    bundle.getCharSequence(
                                            CredentialManagerDialogFragment.APP_NAME_KEY));

            return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setMessage(getContext().getString(R.string.credman_confirmation_message))
                    .setPositiveButton(R.string.credman_confirmation_message_positive_button, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            getDialogHost().onDialogClick(which);
        }
    }

    /**
     * Confirmation dialog fragment shows a dialog to the user to confirm that they would like to
     * enable the new provider.
     */
    public static class NewProviderConfirmationDialogFragment
            extends CredentialManagerDialogFragment {

        NewProviderConfirmationDialogFragment(
                DialogHost dialogHost, @NonNull String packageName, @NonNull CharSequence appName) {
            super(dialogHost);

            final Bundle argument = new Bundle();
            argument.putString(PACKAGE_NAME_KEY, packageName);
            argument.putCharSequence(APP_NAME_KEY, appName);
            setArguments(argument);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final Context context = getContext();
            final String title =
                    context.getString(
                            R.string.credman_enable_confirmation_message_title,
                            bundle.getCharSequence(CredentialManagerDialogFragment.APP_NAME_KEY));

            return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setMessage(context.getString(R.string.credman_enable_confirmation_message))
                    .setPositiveButton(
                            R.string.credman_enable_confirmation_message_positive_button, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            getDialogHost().onDialogClick(which);
        }
    }
}
