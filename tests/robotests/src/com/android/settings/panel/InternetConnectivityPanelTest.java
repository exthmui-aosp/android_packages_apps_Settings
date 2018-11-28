/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)

public class InternetConnectivityPanelTest {

    private InternetConnectivityPanel mPanel;

    @Before
    public void setUp() {
        mPanel = InternetConnectivityPanel.create(RuntimeEnvironment.application);
    }

    @Test
    public void getSlices_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(CustomSliceRegistry.WIFI_SLICE_URI,
                CustomSliceRegistry.AIRPLANE_URI);
    }
}
