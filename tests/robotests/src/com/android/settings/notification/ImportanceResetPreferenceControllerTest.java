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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ImportanceResetPreferenceControllerTest {

    private static final String KEY = "asst_importance_reset";
    private ImportanceResetPreferenceController mController;

    @Mock
    private PreferenceScreen mScreen;
    private LayoutPreference mPreference;
    private Context mContext;
    @Mock
    private NotificationBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new LayoutPreference(mContext, R.layout.reset_importance_button);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new ImportanceResetPreferenceController(mContext, KEY);
        mController.displayPreference(mScreen);

        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void onClick_callReset() {
        final View view = mPreference.findViewById(R.id.reset_importance_button);
        mController.updateState(mPreference);
        assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);
        view.performClick();

        verify(mBackend, times(1)).resetNotificationImportance();
    }
}
