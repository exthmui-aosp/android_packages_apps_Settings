/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link PaletteListView} */
@RunWith(RobolectricTestRunner.class)
public class PaletteListViewTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private PaletteListView mPaletteListView;

    @Before
    public void setUp() {
        mPaletteListView = new PaletteListView(mContext);
    }

    @Test
    public void setColors_applySameLengthArray_configureSuccessful() {
        final String[] colorName = {"White", "Black", "Yellow"};
        final String[] colorCode = {"#ffffff", "#000000", "#f9ab00"};

        assertThat(mPaletteListView.setPaletteListColors(colorName, colorCode)).isTrue();
    }

    @Test
    public void setColors_applyDifferentLengthArray_configureSuccessful() {
        final String[] colorName = {"White", "Black", "Yellow", "Orange", "Red"};
        final String[] colorCode = {"#ffffff", "#000000", "#f9ab00"};

        assertThat(mPaletteListView.setPaletteListColors(colorName, colorCode)).isTrue();
    }

    @Test
    public void setColors_configureFailed() {
        final String[] colorName = null;
        final String[] colorCode = null;

        assertThat(mPaletteListView.setPaletteListColors(colorName, colorCode)).isFalse();
    }
}
