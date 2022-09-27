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

package com.android.settings.spa.app.appsettings

import android.content.pm.PackageInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.button.ActionButtons
import kotlinx.coroutines.flow.map

@Composable
fun AppButtons(packageInfoPresenter: PackageInfoPresenter) {
    val appButtonsHolder = remember { AppButtonsHolder(packageInfoPresenter) }
    appButtonsHolder.Dialogs()
    ActionButtons(actionButtons = appButtonsHolder.rememberActionsButtons().value)
}

private class AppButtonsHolder(private val packageInfoPresenter: PackageInfoPresenter) {
    private val appLaunchButton = AppLaunchButton(context = packageInfoPresenter.context)
    private val appDisableButton = AppDisableButton(packageInfoPresenter)
    private val appUninstallButton = AppUninstallButton(packageInfoPresenter)
    private val appForceStopButton = AppForceStopButton(packageInfoPresenter)

    @Composable
    fun rememberActionsButtons() = remember {
        packageInfoPresenter.flow.map { packageInfo ->
            if (packageInfo != null) getActionButtons(packageInfo) else emptyList()
        }
    }.collectAsState(initial = emptyList())

    private fun getActionButtons(packageInfo: PackageInfo): List<ActionButton> = listOfNotNull(
        appLaunchButton.getActionButton(packageInfo),
        appDisableButton.getActionButton(packageInfo),
        appUninstallButton.getActionButton(packageInfo),
        appForceStopButton.getActionButton(packageInfo),
    )

    @Composable
    fun Dialogs() {
        appDisableButton.DisableConfirmDialog()
        appForceStopButton.ForceStopConfirmDialog()
    }
}
