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

package com.android.settings.spa

import com.android.settings.spa.app.AllAppListPageProvider
import com.android.settings.spa.app.AppsMainPageProvider
import com.android.settings.spa.app.appsettings.AppSettingsProvider
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.AllFilesAccessAppListProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.MediaManagementAppsAppListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.PictureInPictureListProvider
import com.android.settings.spa.app.specialaccess.SpecialAppAccessPageProvider
import com.android.settings.spa.development.UsageStatsPageProvider
import com.android.settings.spa.home.HomePageProvider
import com.android.settings.spa.notification.AppListNotificationsPageProvider
import com.android.settings.spa.notification.NotificationMainPageProvider
import com.android.settingslib.spa.framework.common.SettingsEntryRepository
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListTemplate

object SpaEnvironment {
    val settingsPageProviders: SettingsPageProviderRepository by
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val togglePermissionAppListTemplate = TogglePermissionAppListTemplate(
            allProviders = listOf(
                AllFilesAccessAppListProvider,
                DisplayOverOtherAppsAppListProvider,
                MediaManagementAppsAppListProvider,
                ModifySystemSettingsAppListProvider,
                PictureInPictureListProvider,
                InstallUnknownAppsListProvider,
                AlarmsAndRemindersAppListProvider,
            ),
        )
        SettingsPageProviderRepository(
            allPageProviders = listOf(
                HomePageProvider,
                AppsMainPageProvider,
                AllAppListPageProvider,
                AppSettingsProvider,
                SpecialAppAccessPageProvider,
                NotificationMainPageProvider,
                AppListNotificationsPageProvider,
                UsageStatsPageProvider,
            ) + togglePermissionAppListTemplate.createPageProviders(),
            rootPages = listOf(
                SettingsPage.create(HomePageProvider.name),
            ),
        )
    }
    val settingsEntryRepository: SettingsEntryRepository by
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SettingsEntryRepository(settingsPageProviders)
    }

    // TODO: add other environment setup here.
}
