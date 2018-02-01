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

package com.android.settings.widget;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class HighlightablePreferenceGroupAdapterTest {

    private static final String TEST_KEY = "key";

    @Mock
    private View mRoot;
    @Mock
    private PreferenceCategory mPreferenceCatetory;
    private Context mContext;
    private HighlightablePreferenceGroupAdapter mAdapter;
    private PreferenceViewHolder mViewHolder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mPreferenceCatetory.getContext()).thenReturn(mContext);
        mAdapter = new HighlightablePreferenceGroupAdapter(mPreferenceCatetory, TEST_KEY,
                false /* highlighted*/);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(
                View.inflate(mContext, R.layout.app_preference_item, null));
    }

    @Test
    public void requestHighlight_hasKey_notHighlightedBefore_shouldRequest() {
        mAdapter.requestHighlight(mRoot, mock(RecyclerView.class));

        verify(mRoot).postDelayed(any(),
                eq(HighlightablePreferenceGroupAdapter.DELAY_HIGHLIGHT_DURATION_MILLIS));
    }

    @Test
    public void requestHighlight_noKey_highlightedBefore_noRecyclerView_shouldNotRequest() {
        ReflectionHelpers.setField(mAdapter, "mHighlightKey", null);
        ReflectionHelpers.setField(mAdapter, "mHighlightRequested", false);
        mAdapter.requestHighlight(mRoot, mock(RecyclerView.class));

        ReflectionHelpers.setField(mAdapter, "mHighlightKey", TEST_KEY);
        ReflectionHelpers.setField(mAdapter, "mHighlightRequested", true);
        mAdapter.requestHighlight(mRoot, mock(RecyclerView.class));

        ReflectionHelpers.setField(mAdapter, "mHighlightKey", TEST_KEY);
        ReflectionHelpers.setField(mAdapter, "mHighlightRequested", false);
        mAdapter.requestHighlight(mRoot, null /* recyclerView */);

        verifyZeroInteractions(mRoot);
    }

    @Test
    public void updateBackground_notHighlightedRow_shouldNotSetHighlightedTag() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);

        mAdapter.updateBackground(mViewHolder, 0);

        assertThat(mViewHolder.itemView.getTag(R.id.preference_highlighted)).isNull();
    }

    @Test
    public void updateBackground_highlight_shouldChangeBackgroundAndSetHighlightedTag() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);

        mAdapter.updateBackground(mViewHolder, 10);
        assertThat(mViewHolder.itemView.getBackground()).isInstanceOf(ColorDrawable.class);
        assertThat(mViewHolder.itemView.getTag(R.id.preference_highlighted)).isEqualTo(true);
    }

    @Test
    public void updateBackground_reuseHightlightedRowForNormalRow_shouldResetBackgroundAndTag() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);
        mViewHolder.itemView.setTag(R.id.preference_highlighted, true);

        mAdapter.updateBackground(mViewHolder, 0);

        assertThat(mViewHolder.itemView.getBackground()).isNotInstanceOf(ColorDrawable.class);
        assertThat(mViewHolder.itemView.getTag(R.id.preference_highlighted)).isEqualTo(false);
    }

}
