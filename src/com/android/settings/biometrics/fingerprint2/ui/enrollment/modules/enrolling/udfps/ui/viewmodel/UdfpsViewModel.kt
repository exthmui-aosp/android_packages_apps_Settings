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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import kotlinx.coroutines.flow.flowOf

/** ViewModel used to drive UDFPS Enrollment through [UdfpsEnrollFragment] */
class UdfpsViewModel() : ViewModel() {

  /** Indicates what stage UDFPS enrollment is in. */
  val stageFlow = flowOf(StageViewModel.Center)

  class UdfpsEnrollmentFactory() : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return UdfpsViewModel() as T
    }
  }

  companion object {
    private val navStep = FingerprintNavigationStep.Enrollment::class
    private const val TAG = "UDFPSViewModel"
  }
}
