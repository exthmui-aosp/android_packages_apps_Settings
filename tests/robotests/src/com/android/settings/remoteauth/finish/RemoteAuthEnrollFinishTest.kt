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

package com.android.settings.remoteauth.finish


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.android.setupdesign.GlifLayout
import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteAuthEnrollFinishTest {
    private var mContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testRemoteAuthenticatorEnrollFinish_hasHeader() {
        launchFragmentInContainer<RemoteAuthEnrollFinish>(
            Bundle(),
            com.google.android.setupdesign.R.style.SudThemeGlif,
        ).onFragment {
            assertThat((it.view as GlifLayout).headerText)
                .isEqualTo(
                    mContext.getString(R.string.security_settings_remoteauth_enroll_finish_title)
                )
        }
    }

    @Test
    fun testRemoteAuthenticatorEnrollFinish_hasDescription() {
        launchFragmentInContainer<RemoteAuthEnrollFinish>(
            Bundle(),
            com.google.android.setupdesign.R.style.SudThemeGlif,
        ).onFragment {
            assertThat((it.view as GlifLayout).descriptionText)
                .isEqualTo(
                    mContext.getString(
                        R.string.security_settings_remoteauth_enroll_finish_description
                    )
                )
        }
    }
}
