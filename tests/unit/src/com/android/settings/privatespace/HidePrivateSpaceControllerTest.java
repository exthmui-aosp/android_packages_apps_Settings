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

package com.android.settings.privatespace;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(AndroidJUnit4.class)
public class HidePrivateSpaceControllerTest {
    @Mock private Context mContext;
    private HidePrivateSpaceController mHidePrivateSpaceController;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        final String preferenceKey = "private_space_hidden";

        mHidePrivateSpaceController = new HidePrivateSpaceController(mContext, preferenceKey);
    }

    /** Tests that the controller is always available. */
    @Test
    public void getAvailabilityStatus_returnsAvailable() {
        assertThat(mHidePrivateSpaceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
