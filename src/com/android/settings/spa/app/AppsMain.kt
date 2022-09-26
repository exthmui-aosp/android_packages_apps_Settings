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

package com.android.settings.spa.app

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.specialaccess.SpecialAppAccessPageProvider
import com.android.settingslib.spa.framework.common.SettingsEntry
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.toState
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.SettingsIcon

object AppsMainPageProvider : SettingsPageProvider {
    override val name = "AppsMain"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppsMain()
    }

    @Composable
    fun EntryItem() {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.apps_dashboard_title)
            override val summary =
                stringResource(R.string.app_and_notification_dashboard_summary).toState()
            override val onClick = navigator(name)
            override val icon = @Composable {
                SettingsIcon(imageVector = Icons.Outlined.Apps)
            }
        })
    }

    fun buildInjectEntry() =
        SettingsEntryBuilder.createInject(owner = SettingsPage.create(name)).setIsAllowSearch(false)

    override fun buildEntry(arguments: Bundle?): List<SettingsEntry> {
        val owner = SettingsPage.create(name, parameter = parameter, arguments = arguments)
        return listOf(
            AllAppListPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            SpecialAppAccessPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
        )
    }
}

@Composable
private fun AppsMain() {
    RegularScaffold(title = stringResource(R.string.apps_dashboard_title)) {
        AllAppListPageProvider.buildInjectEntry().build().UiLayout()
        SpecialAppAccessPageProvider.EntryItem()
    }
}
