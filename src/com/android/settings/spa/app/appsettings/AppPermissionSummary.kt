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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.OnPermissionsChangedListener
import android.icu.text.ListFormatter
import androidx.lifecycle.LiveData
import com.android.settings.R
import com.android.settingslib.applications.PermissionsSummaryHelper
import com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback
import com.android.settingslib.spaprivileged.model.app.userHandle

data class AppPermissionSummaryState(
    val summary: String,
    val enabled: Boolean,
)

class AppPermissionSummaryLiveData(
    private val context: Context,
    private val app: ApplicationInfo,
) : LiveData<AppPermissionSummaryState>() {
    private val contextAsUser = context.createContextAsUser(app.userHandle, 0)
    private val packageManager = contextAsUser.packageManager

    private val onPermissionsChangedListener = OnPermissionsChangedListener { uid ->
        if (uid == app.uid) update()
    }

    override fun onActive() {
        packageManager.addOnPermissionsChangeListener(onPermissionsChangedListener)
        update()
    }

    override fun onInactive() {
        packageManager.removeOnPermissionsChangeListener(onPermissionsChangedListener)
    }

    private fun update() {
        PermissionsSummaryHelper.getPermissionSummary(
            contextAsUser, app.packageName, permissionsCallback
        )
    }

    private val permissionsCallback = object : PermissionsResultCallback {
        override fun onPermissionSummaryResult(
            requestedPermissionCount: Int,
            additionalGrantedPermissionCount: Int,
            grantedGroupLabels: List<CharSequence>,
        ) {
            if (requestedPermissionCount == 0) {
                postValue(noPermissionRequestedState())
                return
            }
            val labels = getDisplayLabels(additionalGrantedPermissionCount, grantedGroupLabels)
            val summary = when {
                labels.isEmpty() -> {
                    context.getString(R.string.runtime_permissions_summary_no_permissions_granted)
                }

                else -> ListFormatter.getInstance().format(labels)
            }
            postValue(AppPermissionSummaryState(summary = summary, enabled = true))
        }
    }

    private fun noPermissionRequestedState() = AppPermissionSummaryState(
        summary = context.getString(R.string.runtime_permissions_summary_no_permissions_requested),
        enabled = false,
    )

    private fun getDisplayLabels(
        additionalGrantedPermissionCount: Int,
        grantedGroupLabels: List<CharSequence>,
    ): List<CharSequence> = when (additionalGrantedPermissionCount) {
        0 -> grantedGroupLabels
        else -> {
            grantedGroupLabels +
                // N additional permissions.
                context.resources.getQuantityString(
                    R.plurals.runtime_permissions_additional_count,
                    additionalGrantedPermissionCount,
                    additionalGrantedPermissionCount,
                )
        }
    }
}
