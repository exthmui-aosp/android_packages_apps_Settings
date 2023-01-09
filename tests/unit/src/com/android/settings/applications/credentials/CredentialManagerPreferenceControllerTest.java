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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CredentialManagerPreferenceControllerTest {

    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCredentialsPreferenceCategory;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mCredentialsPreferenceCategory = new PreferenceCategory(mContext);
        mCredentialsPreferenceCategory.setKey("credentials_test");
        mScreen.addPreference(mCredentialsPreferenceCategory);
    }

    @Test
    // Tests that getAvailabilityStatus() does not throw an exception if it's called before the
    // Controller is initialized (this can happen during indexing).
    public void getAvailabilityStatus_withoutInit_returnsUnavailable() {
        CredentialManagerPreferenceController controller =
                new CredentialManagerPreferenceController(
                        mContext, mCredentialsPreferenceCategory.getKey());
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noServices_returnsUnavailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_withServices_returnsAvailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createServiceInfo()));
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_noServices_noPreferencesAdded() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        controller.displayPreference(mScreen);
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_withServices_preferencesAdded() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createServiceInfo()));
        controller.displayPreference(mScreen);
        assertThat(controller.isConnected()).isFalse();
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void getAvailabilityStatus_handlesToggleAndSave() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(
                        Lists.newArrayList(
                                createServiceInfo("com.android.provider1", "ClassA"),
                                createServiceInfo("com.android.provider1", "ClassB"),
                                createServiceInfo("com.android.provider2", "ClassA"),
                                createServiceInfo("com.android.provider3", "ClassA"),
                                createServiceInfo("com.android.provider4", "ClassA"),
                                createServiceInfo("com.android.provider5", "ClassA"),
                                createServiceInfo("com.android.provider6", "ClassA")));
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(controller.isConnected()).isFalse();

        // Ensure that we stay under 5 providers.
        assertThat(controller.togglePackageNameEnabled("com.android.provider1")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider2")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider3")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider4")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider5")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider6")).isFalse();

        // Check that they are all actually registered.
        Set<String> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(5);
        assertThat(enabledProviders.contains("com.android.provider1")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider2")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider3")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider4")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider5")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider6")).isFalse();

        // Check that the settings string has the right component names.
        List<String> enabledServices = controller.getEnabledSettings();
        assertThat(enabledServices.size()).isEqualTo(6);
        assertThat(enabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider1/ClassB")).isTrue();
        assertThat(enabledServices.contains("com.android.provider2/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider5/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider6/ClassA")).isFalse();

        // Toggle the provider disabled.
        controller.togglePackageNameDisabled("com.android.provider2");

        // Check that the provider was removed from the list of providers.
        Set<String> currentlyEnabledProviders = controller.getEnabledProviders();
        assertThat(currentlyEnabledProviders.size()).isEqualTo(4);
        assertThat(currentlyEnabledProviders.contains("com.android.provider1")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider2")).isFalse();
        assertThat(currentlyEnabledProviders.contains("com.android.provider3")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider4")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider5")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider6")).isFalse();

        // Check that the provider was removed from the list of services stored in the setting.
        List<String> currentlyEnabledServices = controller.getEnabledSettings();
        assertThat(currentlyEnabledServices.size()).isEqualTo(5);
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassB")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider5/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider6/ClassA")).isFalse();
    }

    private CredentialManagerPreferenceController createControllerWithServices(
            List<ServiceInfo> availableServices) {
        CredentialManagerPreferenceController controller =
                new CredentialManagerPreferenceController(
                        mContext, mCredentialsPreferenceCategory.getKey());
        controller.init(() -> mock(Lifecycle.class), availableServices);
        return controller;
    }

    private ServiceInfo createServiceInfo() {
        return createServiceInfo("com.android.provider", "CredManProvider");
    }

    private ServiceInfo createServiceInfo(String packageName, String className) {
        ServiceInfo si = new ServiceInfo();
        si.packageName = packageName;
        si.name = className;
        si.nonLocalizedLabel = "test";

        si.applicationInfo = new ApplicationInfo();
        si.applicationInfo.packageName = packageName;
        si.applicationInfo.nonLocalizedLabel = "test";

        return si;
    }
}
