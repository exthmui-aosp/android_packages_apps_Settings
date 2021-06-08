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
package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.slices.SliceData;
import com.android.settingslib.widget.SeekBarPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsSliderPreferenceControllerTest {

    private FakeSliderPreferenceController mSliderController;

    private SeekBarPreference mPreference;

    @Before
    public void setUp() {
        mPreference = new SeekBarPreference(ApplicationProvider.getApplicationContext());
        mSliderController = new FakeSliderPreferenceController(
                ApplicationProvider.getApplicationContext(), "key");

        mPreference.setContinuousUpdates(true);
        mPreference.setMin(mSliderController.getMin());
        mPreference.setMax(mSliderController.getMax());
    }

    @Test
    public void onPreferenceChange_updatesPosition() {
        final int newValue = 28;

        mSliderController.onPreferenceChange(mPreference, newValue);

        assertThat(mSliderController.getSliderPosition()).isEqualTo(newValue);
    }

    @Test
    public void updateState_setsPreferenceToCurrentValue() {
        final int newValue = 28;
        mSliderController.setSliderPosition(newValue);

        mSliderController.updateState(mPreference);

        assertThat(mPreference.getProgress()).isEqualTo(newValue);
    }

    @Test
    public void testSliceType_returnsSliceType() {
        assertThat(mSliderController.getSliceType()).isEqualTo(SliceData.SliceType.SLIDER);
    }

    private class FakeSliderPreferenceController extends SliderPreferenceController {

        private static final int MAX_STEPS = 2112;
        private int mPosition;

        private FakeSliderPreferenceController(Context context, String key) {
            super(context, key);
        }

        @Override
        public int getSliderPosition() {
            return mPosition;
        }

        @Override
        public boolean setSliderPosition(int position) {
            mPosition = position;
            return true;
        }

        @Override
        public int getMax() {
            return MAX_STEPS;
        }

        @Override
        public int getMin() {
            return 0;
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }
}
