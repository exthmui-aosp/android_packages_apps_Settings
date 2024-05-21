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

package com.android.settings.biometrics.fingerprint2.lib.domain.interactor

import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import kotlinx.coroutines.flow.Flow

/**
 * Interface to obtain the necessary data for FingerprintEnrollment/Settings
 *
 * Note that this interface should not have dependencies on heavyweight libraries such as the
 * framework, hidl/aidl, etc. This makes it much easier to test and create fakes for.
 */
interface FingerprintManagerInteractor {
  /** Returns the list of current fingerprints. */
  val enrolledFingerprints: Flow<List<FingerprintData>>

  /** Returns the max enrollable fingerprints, note during SUW this might be 1 */
  val maxEnrollableFingerprints: Flow<Int>

  /** Returns true if a user can enroll a fingerprint false otherwise. */
  val canEnrollFingerprints: Flow<Boolean>

  /** Retrieves the sensor properties of a device */
  val sensorPropertiesInternal: Flow<FingerprintSensor?>

  /** Runs the authenticate flow */
  suspend fun authenticate(): FingerprintAuthAttemptModel

  /**
   * Generates a challenge with the provided [gateKeeperPasswordHandle] and on success returns a
   * challenge and challenge token. This info can be used for secure operations such as enrollment
   *
   * @param gateKeeperPasswordHandle GateKeeper password handle generated by a Confirm
   * @return A [Pair] of the challenge and challenge token
   */
  suspend fun generateChallenge(gateKeeperPasswordHandle: Long): Pair<Long, ByteArray>

  /**
   * Runs [FingerprintManager.enroll] with the [hardwareAuthToken] and [EnrollReason] for this
   * enrollment. If successful data in the [fingerprintEnrollState] should be populated.
   */
  suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
  ): Flow<FingerEnrollState>

  /**
   * Removes the given fingerprint, returning true if it was successfully removed and false
   * otherwise
   */
  suspend fun removeFingerprint(fp: FingerprintData): Boolean

  /** Renames the given fingerprint if one exists */
  suspend fun renameFingerprint(fp: FingerprintData, newName: String)

  /** Indicates if the device has side fingerprint */
  suspend fun hasSideFps(): Boolean
}
