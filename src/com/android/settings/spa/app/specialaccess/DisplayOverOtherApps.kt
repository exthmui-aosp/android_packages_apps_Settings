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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object DisplayOverOtherAppsAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "DisplayOverOtherApps"
    override fun createModel(context: Context) = DisplayOverOtherAppsListModel(context)
}

class DisplayOverOtherAppsListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.system_alert_window_settings
    override val switchTitleResId = R.string.permit_draw_overlay
    override val footerResId = R.string.allow_overlay_description
    override val appOp = AppOpsManager.OP_SYSTEM_ALERT_WINDOW
    override val permission = Manifest.permission.SYSTEM_ALERT_WINDOW

    override fun setAllowed(record: AppOpPermissionRecord, newAllowed: Boolean) {
        super.setAllowed(record, newAllowed)
        logPermissionChange(newAllowed)
    }

    private fun logPermissionChange(newAllowed: Boolean) {
        val category = when {
            newAllowed -> SettingsEnums.APP_SPECIAL_PERMISSION_APPDRAW_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_APPDRAW_DENY
        }
        FeatureFactory.getFactory(context).metricsFeatureProvider.action(context, category, "")
    }
}
