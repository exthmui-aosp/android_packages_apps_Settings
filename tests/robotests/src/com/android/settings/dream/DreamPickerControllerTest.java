/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.widget.Button;

import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Ignore
public class DreamPickerControllerTest {
    private DreamPickerController mController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    private LayoutPreference mPreference;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mPreference = new LayoutPreference(mContext, R.layout.dream_picker_layout);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = new DreamPickerController(
                mContext,
                /* preferenceKey= */ "test",
                mBackend);

        mController.displayPreference(mScreen);
    }

    @Test
    public void isDisabledIfNoDreamsAvailable() {
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(0));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isEnabledIfDreamsAvailable() {
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(new DreamInfo()));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testDreamDisplayedInList() {
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(new DreamInfo()));
        mController.updateState(mPreference);

        RecyclerView view = mPreference.findViewById(R.id.dream_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(1);
    }

    @Test
    public void testPreviewButton() {
        final DreamInfo mockDreamInfo = new DreamInfo();
        mockDreamInfo.componentName = new ComponentName("package", "class");
        mockDreamInfo.isActive = true;

        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(mockDreamInfo));
        mController.updateState(mPreference);

        Button view = mPreference.findViewById(R.id.preview_button);
        view.performClick();
        verify(mBackend).preview(mockDreamInfo);
    }
}
