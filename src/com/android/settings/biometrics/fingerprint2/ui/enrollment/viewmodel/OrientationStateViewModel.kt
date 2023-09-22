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

import android.content.Context
import android.view.OrientationEventListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.internal.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/** Represents all of the information on orientation state and rotation state. */
class OrientationStateViewModel(private val context: Context) : ViewModel() {

  /** A flow that contains the orientation info */
  val orientation: Flow<Int> = callbackFlow {
    val orientationEventListener =
      object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
          trySend(orientation)
        }
      }
    orientationEventListener.enable()
    awaitClose { orientationEventListener.disable() }
  }

  /** A flow that contains the rotation info */
  val rotation: Flow<Int> =
    callbackFlow {
        val orientationEventListener =
          object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
              trySend(getRotationFromDefault(context.display!!.rotation))
            }
          }
        orientationEventListener.enable()
        awaitClose { orientationEventListener.disable() }
      }
      .stateIn(
        viewModelScope, // This is going to tied to the view model scope
        SharingStarted.WhileSubscribed(), // When no longer subscribed, we removeTheListener
        context.display!!.rotation
      )

  fun getRotationFromDefault(rotation: Int): Int {
    val isReverseDefaultRotation =
      context.resources.getBoolean(R.bool.config_reverseDefaultRotation)
    return if (isReverseDefaultRotation) {
      (rotation + 1) % 4
    } else {
      rotation
    }
  }

  class OrientationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {
      return OrientationStateViewModel(context) as T
    }
  }
}
