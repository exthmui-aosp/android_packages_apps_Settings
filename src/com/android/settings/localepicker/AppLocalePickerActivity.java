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

package com.android.settings.localepicker;

import android.app.FragmentTransaction;
import android.app.LocaleManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.appinfo.AppLocaleDetails;
import com.android.settings.core.SettingsBaseActivity;

/**
 * TODO(b/223503670): Add unit test for AppLocalePickerActivity.
 * A activity to show the locale picker and information page.
 */
public class AppLocalePickerActivity extends SettingsBaseActivity
        implements LocalePickerWithRegion.LocaleSelectedListener {
    private static final String TAG = AppLocalePickerActivity.class.getSimpleName();

    private String mPackageName;
    private LocalePickerWithRegion mLocalePickerWithRegion;
    private AppLocaleDetails mAppLocaleDetails;
    private Context mContextAsUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent().getData();
        if (data == null) {
            Log.d(TAG, "There is no uri data.");
            finish();
            return;
        }
        mPackageName = data.getSchemeSpecificPart();
        if (TextUtils.isEmpty(mPackageName)) {
            Log.d(TAG, "There is no package name.");
            finish();
            return;
        }
        int uid = getIntent().getIntExtra(AppInfoBase.ARG_PACKAGE_UID, -1);
        if (uid == -1) {
            Log.w(TAG, "Unexpected user id");
            finish();
        }
        UserHandle userHandle = UserHandle.getUserHandleForUid(uid);
        mContextAsUser = createContextAsUser(userHandle, 0);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mLocalePickerWithRegion = LocalePickerWithRegion.createLanguagePicker(
                mContextAsUser,
                AppLocalePickerActivity.this,
                false /* translate only */,
                mPackageName);
        mAppLocaleDetails = AppLocaleDetails.newInstance(mPackageName);

        // Launch Locale picker part.
        launchLocalePickerPage();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
        if (localeInfo == null || localeInfo.getLocale() == null || localeInfo.isSystemLocale()) {
            setAppDefaultLocale("");
        } else {
            setAppDefaultLocale(localeInfo.getLocale().toLanguageTag());
        }
        finish();
    }

    /** Sets the app's locale to the supplied language tag */
    private void setAppDefaultLocale(String languageTag) {
        LocaleManager localeManager = mContextAsUser.getSystemService(LocaleManager.class);
        if (localeManager == null) {
            Log.w(TAG, "LocaleManager is null, cannot set default app locale");
            return;
        }
        localeManager.setApplicationLocales(mPackageName, LocaleList.forLanguageTags(languageTag));
    }

    private View launchAppLocaleDetailsPage() {
        FrameLayout appLocaleDetailsContainer = new FrameLayout(this);
        appLocaleDetailsContainer.setId(R.id.layout_app_locale_details);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_app_locale_details, mAppLocaleDetails)
                .commit();
        return appLocaleDetailsContainer;
    }

    @VisibleForTesting
    void launchLocalePickerPage() {
        // LocalePickerWithRegion use android.app.ListFragment. Thus, it can not use
        // getSupportFragmentManager() to add this into container.
        android.app.FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(
                new android.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(
                            android.app.FragmentManager fm,
                            android.app.Fragment f,
                            View v,
                            Bundle savedInstanceState) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState);
                        mLocalePickerWithRegion.getListView()
                                .addHeaderView(launchAppLocaleDetailsPage());
                    }
                }, true);
        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.content_frame, mLocalePickerWithRegion)
                .commit();
    }
}
