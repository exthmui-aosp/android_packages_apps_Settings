/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.android.settings.network.mobileDataEnabledFlow
import com.android.settings.wifi.WifiPickerTrackerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class TelephonyRepository(
    private val context: Context,
    private val subscriptionsChangedFlow: Flow<Unit> = context.subscriptionsChangedFlow(),
) {
    fun isMobileDataPolicyEnabledFlow(
        subId: Int,
        @TelephonyManager.MobileDataPolicy policy: Int,
    ): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)

        val telephonyManager = context.telephonyManager(subId)

        return subscriptionsChangedFlow.map {
            telephonyManager.isMobileDataPolicyEnabled(policy)
                .also { Log.d(TAG, "[$subId] isMobileDataPolicyEnabled($policy): $it") }
        }.conflate().flowOn(Dispatchers.Default)
    }

    fun setMobileDataPolicyEnabled(
        subId: Int,
        @TelephonyManager.MobileDataPolicy policy: Int,
        enabled: Boolean,
    ) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return

        val telephonyManager = context.telephonyManager(subId)
        Log.d(TAG, "[$subId] setMobileDataPolicyEnabled($policy): $enabled")
        telephonyManager.setMobileDataPolicyEnabled(policy, enabled)
    }

    fun isDataEnabledFlow(subId: Int): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)

        return context.mobileDataEnabledFlow(subId)
            .map {
                val telephonyManager = context.telephonyManager(subId)
                telephonyManager.isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER)
            }
            .catch {
                Log.w(TAG, "[$subId] isDataEnabledFlow: exception", it)
                emit(false)
            }
            .onEach { Log.d(TAG, "[$subId] isDataEnabledFlow: isDataEnabled() = $it") }
            .conflate()
            .flowOn(Dispatchers.Default)
    }

    fun setMobileData(
        subId: Int,
        enabled: Boolean,
        wifiPickerTrackerHelper: WifiPickerTrackerHelper? = null
    ) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return

        Log.d(TAG, "setMobileData: $enabled")
        MobileNetworkUtils.setMobileDataEnabled(
            context,
            subId,
            enabled /* enabled */,
            true /* disableOtherSubscriptions */
        )

        if (wifiPickerTrackerHelper != null
            && !wifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(subId)
        ) {
            wifiPickerTrackerHelper.setCarrierNetworkEnabled(enabled)
        }
    }

    private companion object {
        private const val TAG = "TelephonyRepository"
    }
}

/** Creates an instance of a cold Flow for Telephony callback of given [subId]. */
fun <T> Context.telephonyCallbackFlow(
    subId: Int,
    block: ProducerScope<T>.() -> TelephonyCallback,
): Flow<T> = telephonyManager(subId).telephonyCallbackFlow(block)

/** Creates an instance of a cold Flow for Telephony callback. */
fun <T> TelephonyManager.telephonyCallbackFlow(
    block: ProducerScope<T>.() -> TelephonyCallback,
): Flow<T> = callbackFlow {
    val callback = block()

    registerTelephonyCallback(Dispatchers.Default.asExecutor(), callback)

    awaitClose { unregisterTelephonyCallback(callback) }
}.conflate().flowOn(Dispatchers.Default)

fun Context.telephonyManager(subId: Int): TelephonyManager =
    getSystemService(TelephonyManager::class.java)!!
        .createForSubscriptionId(subId)
