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

package com.android.settings.network

import android.content.Context
import android.os.OutcomeReceiver
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteManager.SatelliteException
import androidx.test.core.app.ApplicationProvider
import com.android.settings.network.SatelliteManagerUtil.requestIsEnabled
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class SatelliteManagerUtilTest {

    @JvmField
    @Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Spy
    var spyContext: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var mockSatelliteManager: SatelliteManager

    @Mock
    private lateinit var mockExecutor: Executor

    @Before
    fun setUp() {
        `when`(this.spyContext.getSystemService(SatelliteManager::class.java))
            .thenReturn(mockSatelliteManager)
    }

    @Test
    fun requestIsEnabled_resultIsTrue() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(
                eq(mockExecutor), any<OutcomeReceiver<Boolean, SatelliteException>>()
            )
        )
            .thenAnswer { invocation ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Boolean, SatelliteException>>(1)
                receiver.onResult(true)
                null
            }

        val result: ListenableFuture<Boolean> =
            requestIsEnabled(spyContext, mockExecutor)

        assertTrue(result.get())
    }

    @Test
    fun requestIsEnabled_resultIsFalse() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(
                eq(mockExecutor), any<OutcomeReceiver<Boolean, SatelliteException>>()
            )
        )
            .thenAnswer { invocation ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Boolean, SatelliteException>>(1)
                receiver.onResult(false)
                null
            }

        val result: ListenableFuture<Boolean> =
            requestIsEnabled(spyContext, mockExecutor)
        assertFalse(result.get())
    }


    @Test
    fun requestIsEnabled_exceptionFailure() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(
                eq(mockExecutor), any<OutcomeReceiver<Boolean, SatelliteException>>()
            )
        )
            .thenAnswer { invocation ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Boolean, SatelliteException>>(1)
                receiver.onError(SatelliteException(SatelliteManager.SATELLITE_RESULT_ERROR))
                null
            }

        val result = requestIsEnabled(spyContext, mockExecutor)

        assertFalse(result.get())
    }

    @Test
    fun requestIsEnabled_nullSatelliteManager() = runBlocking {
        `when`(spyContext.getSystemService(SatelliteManager::class.java)).thenReturn(null)

        val result: ListenableFuture<Boolean> = requestIsEnabled(spyContext, mockExecutor)

        assertFalse(result.get())
    }
}