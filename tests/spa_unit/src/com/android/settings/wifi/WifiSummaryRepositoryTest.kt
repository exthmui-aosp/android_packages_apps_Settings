/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.wifi.WifiStatusTracker
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class WifiSummaryRepositoryTest {

    private val mockWifiStatusTracker = mock<WifiStatusTracker>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val repository = WifiSummaryRepository(context) { mockWifiStatusTracker }

    @Test
    fun summaryFlow_wifiDisabled_returnOff() = runBlocking {
        mockWifiStatusTracker.enabled = false

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(context.getString(R.string.switch_off_text))
    }

    @Test
    fun summaryFlow_wifiDisconnected_returnDisconnected() = runBlocking {
        mockWifiStatusTracker.apply {
            enabled = true
            connected = false
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(context.getString(R.string.disconnected))
    }

    @Test
    fun summaryFlow_wifiConnected_returnSsid() = runBlocking {
        mockWifiStatusTracker.apply {
            enabled = true
            connected = true
            ssid = TEST_SSID
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(TEST_SSID)
    }

    @Test
    fun summaryFlow_wifiConnectedAndWithSpeedLabel_returnSsidWithSpeedLabel() = runBlocking {
        mockWifiStatusTracker.apply {
            enabled = true
            connected = true
            ssid = TEST_SSID
            statusLabel = STATUS_LABEL
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo("$TEST_SSID / $STATUS_LABEL")
    }

    private companion object {
        const val TEST_SSID = "Test Ssid"
        const val STATUS_LABEL = "Very Fast"
    }
}
