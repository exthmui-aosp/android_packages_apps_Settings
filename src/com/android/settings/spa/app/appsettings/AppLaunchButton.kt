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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Launch
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.model.app.userHandle

class AppLaunchButton(private val context: Context) {
    private val packageManager = context.packageManager

    fun getActionButton(packageInfo: PackageInfo): ActionButton? =
        packageManager.getLaunchIntentForPackage(packageInfo.packageName)?.let { intent ->
            launchButton(intent, packageInfo.applicationInfo)
        }

    private fun launchButton(intent: Intent, app: ApplicationInfo) = ActionButton(
        text = context.getString(R.string.launch_instant_app),
        imageVector = Icons.Outlined.Launch,
    ) {
        context.startActivityAsUser(intent, app.userHandle)
    }
}
