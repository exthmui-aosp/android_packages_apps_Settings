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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.AnimatedImageDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link AnimatedImagePreference}. */
@RunWith(RobolectricTestRunner.class)
public class AnimatedImagePreferenceTest {
    private Uri mImageUri;
    private View mRootView;
    private PreferenceViewHolder mViewHolder;
    private AnimatedImagePreference mAnimatedImagePreference;

    @Spy
    private ImageView mImageView;

    @Mock
    private AnimatedImageDrawable mAnimatedImageDrawable;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        final Context context = RuntimeEnvironment.application;
        final LayoutInflater inflater = LayoutInflater.from(context);
        mRootView = spy(inflater.inflate(R.layout.preference_animated_image, /* root= */ null));
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(mRootView));
        mImageView = spy(new ImageView(context));

        mAnimatedImagePreference = new AnimatedImagePreference(context);
        mImageUri = new Uri.Builder().build();
    }

    @Test
    public void readImageUri_animatedImage_startAnimation() {
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);
        doReturn(mAnimatedImageDrawable).when(mImageView).getDrawable();

        mAnimatedImagePreference.setImageUri(mImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(mAnimatedImageDrawable).start();
    }

    @Test
    public void setImageUri_viewNotExist_setFail() {
        doReturn(null).when(mRootView).findViewById(R.id.animated_img);

        mAnimatedImagePreference.setImageUri(mImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(mImageView, never()).setImageURI(mImageUri);
    }

    @Test
    public void setMaxHeight_success() {
        final int maxHeight = 100;
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);

        mAnimatedImagePreference.setMaxHeight(maxHeight);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        assertThat(mImageView.getMaxHeight()).isEqualTo(maxHeight);
    }
}
