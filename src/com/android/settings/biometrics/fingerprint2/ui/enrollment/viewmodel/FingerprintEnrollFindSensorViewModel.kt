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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel

import android.hardware.fingerprint.FingerprintManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.shared.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollStateViewModel
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Models the UI state for [FingerprintEnrollFindSensorV2Fragment]. */
class FingerprintEnrollFindSensorViewModel(
  private val navigationViewModel: FingerprintEnrollNavigationViewModel,
  private val fingerprintEnrollViewModel: FingerprintEnrollViewModel,
  private val gatekeeperViewModel: FingerprintGatekeeperViewModel,
  accessibilityViewModel: AccessibilityViewModel,
  foldStateViewModel: FoldStateViewModel,
  orientationStateViewModel: OrientationStateViewModel
) : ViewModel() {
  /** Represents the stream of sensor type. */
  val sensorType: Flow<FingerprintSensorType> =
    fingerprintEnrollViewModel.sensorType.shareIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(),
      1
    )
  private val _isUdfps: Flow<Boolean> =
    sensorType.map {
      it == FingerprintSensorType.UDFPS_OPTICAL || it == FingerprintSensorType.UDFPS_ULTRASONIC
    }
  private val _isSfps: Flow<Boolean> = sensorType.map { it == FingerprintSensorType.POWER_BUTTON }
  private val _isRearSfps: Flow<Boolean> = sensorType.map { it == FingerprintSensorType.REAR }

  /** Represents the stream of showing primary button. */
  val showPrimaryButton: Flow<Boolean> = _isUdfps.filter { it }

  private val _showSfpsLottie = _isSfps.filter { it }
  /** Represents the stream of showing sfps lottie and the information Pair(isFolded, rotation). */
  val sfpsLottieInfo: Flow<Pair<Boolean, Int>> =
    combineTransform(
      _showSfpsLottie,
      foldStateViewModel.isFolded,
      orientationStateViewModel.rotation,
    ) { _, isFolded, rotation ->
      emit(Pair(isFolded, rotation))
    }

  private val _showUdfpsLottie = _isUdfps.filter { it }
  /** Represents the stream of showing udfps lottie and whether accessibility is enabled. */
  val udfpsLottieInfo: Flow<Boolean> =
    _showUdfpsLottie.combine(accessibilityViewModel.isAccessibilityEnabled) {
      _,
      isAccessibilityEnabled ->
      isAccessibilityEnabled
    }

  /** Represents the stream of showing rfps animation. */
  val showRfpsAnimation: Flow<Boolean> = _isRearSfps.filter { it }

  private val _showErrorDialog: MutableStateFlow<Pair<Int, Boolean>?> = MutableStateFlow(null)
  /** Represents the stream of showing error dialog. */
  val showErrorDialog = _showErrorDialog.filterNotNull()

  init {
    // Start or end enroll flow
    viewModelScope.launch {
      combine(
          fingerprintEnrollViewModel.sensorType,
          gatekeeperViewModel.hasValidGatekeeperInfo,
          gatekeeperViewModel.gatekeeperInfo,
          navigationViewModel.navigationViewModel
        ) { sensorType, hasValidGatekeeperInfo, gatekeeperInfo, navigationViewModel ->
          val shouldStartEnroll =
            navigationViewModel.currStep == Education &&
              sensorType != FingerprintSensorType.UDFPS_OPTICAL &&
              sensorType != FingerprintSensorType.UDFPS_ULTRASONIC &&
              hasValidGatekeeperInfo
          if (shouldStartEnroll) (gatekeeperInfo as GatekeeperInfo.GatekeeperPasswordInfo).token
          else null
        }
        .collect { token ->
          if (token != null) {
            fingerprintEnrollViewModel.startEnroll(token, EnrollReason.FindSensor)
          } else {
            fingerprintEnrollViewModel.stopEnroll()
          }
        }
    }

    // Enroll progress flow
    viewModelScope.launch {
      combine(
          navigationViewModel.enrollType,
          fingerprintEnrollViewModel.enrollFlow.filterNotNull()
        ) { enrollType, enrollFlow ->
          Pair(enrollType, enrollFlow)
        }
        .collect { (enrollType, enrollFlow) ->
          when (enrollFlow) {
            // TODO: Cancel the enroll() when EnrollProgress is received instead of proceeding to
            // Enrolling page. Otherwise Enrolling page will receive the EnrollError.
            is FingerEnrollStateViewModel.EnrollProgress -> proceedToEnrolling()
            is FingerEnrollStateViewModel.EnrollError -> {
              val errMsgId = enrollFlow.errMsgId
              if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                proceedToEnrolling()
              } else {
                _showErrorDialog.update { Pair(errMsgId, enrollType == SetupWizard) }
              }
            }
            is FingerEnrollStateViewModel.EnrollHelp -> {}
          }
        }
    }
  }

  /** Proceed to EnrollEnrolling page. */
  fun proceedToEnrolling() {
    navigationViewModel.nextStep()
  }

  class FingerprintEnrollFindSensorViewModelFactory(
    private val navigationViewModel: FingerprintEnrollNavigationViewModel,
    private val fingerprintEnrollViewModel: FingerprintEnrollViewModel,
    private val gatekeeperViewModel: FingerprintGatekeeperViewModel,
    private val accessibilityViewModel: AccessibilityViewModel,
    private val foldStateViewModel: FoldStateViewModel,
    private val orientationStateViewModel: OrientationStateViewModel
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return FingerprintEnrollFindSensorViewModel(
        navigationViewModel,
        fingerprintEnrollViewModel,
        gatekeeperViewModel,
        accessibilityViewModel,
        foldStateViewModel,
        orientationStateViewModel
      )
        as T
    }
  }
}
