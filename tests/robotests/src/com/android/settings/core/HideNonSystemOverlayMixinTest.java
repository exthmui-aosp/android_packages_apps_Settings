/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class HideNonSystemOverlayMixinTest {

    private ActivityController<TestActivity> mActivityController;

    @Before
    public void setUp() {
        RuntimeEnvironment.application.setTheme(R.style.Theme_AppCompat);
        mActivityController = Robolectric.buildActivity(TestActivity.class);
    }

    @Test
    public void startActivity_shouldHideNonSystemOverlay() {
        mActivityController.setup();
        TestActivity activity = mActivityController.get();

        // Activity start: HIDE_NON_SYSTEM_OVERLAY should be set.
        final WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        assertThat(attrs.privateFlags & SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)
                .isNotEqualTo(0);
    }

    @Test
    public void stopActivity_shouldUnhideNonSystemOverlay() {
        mActivityController.setup().stop();
        TestActivity activity = mActivityController.get();

        final WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        assertThat(attrs.privateFlags & SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)
                .isEqualTo(0);
    }

    public static class TestActivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
        }
    }
}
