/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.interactacrossprofiles;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowProcess;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class InteractAcrossProfilesSettingsTest {

    private static final int PERSONAL_PROFILE_ID = 0;
    private static final int WORK_PROFILE_ID = 10;
    private static final int WORK_UID = UserHandle.PER_USER_RANGE * WORK_PROFILE_ID;

    private static final String PERSONAL_CROSS_PROFILE_PACKAGE = "personalCrossProfilePackage";
    private static final String PERSONAL_NON_CROSS_PROFILE_PACKAGE =
            "personalNonCrossProfilePackage";
    private static final String WORK_CROSS_PROFILE_PACKAGE = "workCrossProfilePackage";
    private static final String WORK_NON_CROSS_PROFILE_PACKAGE =
            "workNonCrossProfilePackage";
    private static final List<String> PERSONAL_PROFILE_INSTALLED_PACKAGES =
            ImmutableList.of(PERSONAL_CROSS_PROFILE_PACKAGE, PERSONAL_NON_CROSS_PROFILE_PACKAGE);
    private static final List<String> WORK_PROFILE_INSTALLED_PACKAGES =
            ImmutableList.of(WORK_CROSS_PROFILE_PACKAGE, WORK_NON_CROSS_PROFILE_PACKAGE);

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final PackageManager mPackageManager = mContext.getPackageManager();
    private final UserManager mUserManager = mContext.getSystemService(UserManager.class);
    private final CrossProfileApps mCrossProfileApps =
            mContext.getSystemService(CrossProfileApps.class);
    private final InteractAcrossProfilesSettings mFragment = new InteractAcrossProfilesSettings();

    @Before
    public void setup() {
        ReflectionHelpers.setField(mFragment, "mPackageManager", mPackageManager);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mCrossProfileApps", mCrossProfileApps);
    }

    @Test
    public void collectConfigurableApps_fromPersonal_returnsPersonalPackages() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mUserManager).addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, 0/* profileFlags */);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                WORK_PROFILE_ID, WORK_PROFILE_INSTALLED_PACKAGES);
        shadowOf(mCrossProfileApps).addCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);
        shadowOf(mCrossProfileApps).addCrossProfilePackage(WORK_CROSS_PROFILE_PACKAGE);

        List<Pair<ApplicationInfo, UserHandle>> apps = mFragment.collectConfigurableApps();

        assertThat(apps.size()).isEqualTo(1);
        assertThat(apps.get(0).first.packageName).isEqualTo(PERSONAL_CROSS_PROFILE_PACKAGE);
    }

    @Test
    public void collectConfigurableApps_fromWork_returnsPersonalPackages() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mUserManager).addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, 0/* profileFlags */);
        ShadowProcess.setUid(WORK_UID);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                WORK_PROFILE_ID, WORK_PROFILE_INSTALLED_PACKAGES);
        shadowOf(mCrossProfileApps).addCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);
        shadowOf(mCrossProfileApps).addCrossProfilePackage(WORK_CROSS_PROFILE_PACKAGE);

        List<Pair<ApplicationInfo, UserHandle>> apps = mFragment.collectConfigurableApps();

        assertThat(apps.size()).isEqualTo(1);
        assertThat(apps.get(0).first.packageName).isEqualTo(PERSONAL_CROSS_PROFILE_PACKAGE);
    }

    @Test
    public void collectConfigurableApps_onlyOneProfile_returnsEmpty() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        shadowOf(mCrossProfileApps).addCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);

        List<Pair<ApplicationInfo, UserHandle>> apps = mFragment.collectConfigurableApps();

        assertThat(apps).isEmpty();
    }
}
