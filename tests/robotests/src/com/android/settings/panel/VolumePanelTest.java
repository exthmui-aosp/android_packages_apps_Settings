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
 */

package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VolumePanelTest {

    private VolumePanel mPanel;

    @Before
    public void setUp() {
        mPanel = VolumePanel.create(RuntimeEnvironment.application);
    }

    @Test
    @Ignore("b/130896218")
    public void getSlices_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                CustomSliceRegistry.VOLUME_REMOTE_MEDIA_URI,
                CustomSliceRegistry.VOLUME_CALL_URI,
                CustomSliceRegistry.VOLUME_MEDIA_URI,
                CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI,
                CustomSliceRegistry.VOLUME_RINGER_URI,
                CustomSliceRegistry.VOLUME_ALARM_URI);
    }

    @Test
    public void getSeeMoreIntent_notNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNotNull();
    }
}
