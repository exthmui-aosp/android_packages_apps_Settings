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

package com.android.settings.biometrics.fingerprint2.data.repository

import android.graphics.Point
import android.graphics.Rect
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintEnrollInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.SensorStrength
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * This class is used to simulate enroll data. This has two major use cases. 1). Ease of Development
 * 2). Bug Fixes
 */
class UdfpsEnrollDebugRepositoryImpl :
  FingerprintEnrollInteractor, FingerprintSensorRepository, SimulatedTouchEventsRepository {

  override suspend fun enroll(hardwareAuthToken: ByteArray?, enrollReason: EnrollReason) = flow {
    emit(FingerEnrollState.OverlayShown)
    delay(200)
    emit(FingerEnrollState.EnrollHelp(helpMsgId, "Hello world"))
    delay(200)
    emit(FingerEnrollState.EnrollProgress(15, 16))
    delay(300)
    emit(FingerEnrollState.EnrollHelp(helpMsgId, "Hello world"))
    delay(1000)
    emit(FingerEnrollState.EnrollProgress(14, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(13, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(12, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(11, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(10, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(9, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(8, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(7, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(6, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(5, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(4, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(3, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(2, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(1, 16))
    delay(500)
    emit(FingerEnrollState.EnrollProgress(0, 16))
  }

  /** Provides touch events to the UdfpsEnrollFragment */
  override val touchExplorationDebug: Flow<Point> = flow {
    delay(2000)
    emit(pointToLeftOfSensor(sensorRect))
    delay(2000)
    emit(pointBelowSensor(sensorRect))
    delay(2000)
    emit(pointToRightOfSensor(sensorRect))
    delay(2000)
    emit(pointAboveSensor(sensorRect))
  }

  override val fingerprintSensor: Flow<FingerprintSensor> = flowOf(sensorProps)

  private fun pointToLeftOfSensor(sensorLocation: Rect) =
    Point(sensorLocation.right + 5, sensorLocation.centerY())

  private fun pointToRightOfSensor(sensorLocation: Rect) =
    Point(sensorLocation.left - 5, sensorLocation.centerY())

  private fun pointBelowSensor(sensorLocation: Rect) =
    Point(sensorLocation.centerX(), sensorLocation.bottom + 5)

  private fun pointAboveSensor(sensorLocation: Rect) =
    Point(sensorLocation.centerX(), sensorLocation.top - 5)

  companion object {

    private val helpMsgId: Int = 1
    private val sensorLocationInternal = Pair(540, 1713)
    private val sensorRadius = 100
    private val sensorRect =
      Rect(
        this.sensorLocationInternal.first - sensorRadius,
        this.sensorLocationInternal.second - sensorRadius,
        this.sensorLocationInternal.first + sensorRadius,
        this.sensorLocationInternal.second + sensorRadius,
      )
    val sensorProps =
      FingerprintSensor(
        1,
        SensorStrength.STRONG,
        5,
        FingerprintSensorType.UDFPS_OPTICAL,
        sensorRect,
        sensorRadius,
      )
  }
}
