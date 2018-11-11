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

package com.android.settings.homepage.contextualcards.legacysuggestion;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class LegacySuggestionContextualCardRendererTest {
    @Mock
    private ControllerRendererPool mControllerRendererPool;
    @Mock
    private LegacySuggestionContextualCardController mController;
    private Context mContext;
    private LegacySuggestionContextualCardRenderer mRenderer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mRenderer = new LegacySuggestionContextualCardRenderer(mContext, mControllerRendererPool);
    }

    @Test
    public void bindView_shouldSetListener() {
        final int viewType = mRenderer.getViewType(true /* isHalfWidth */);
        final RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        final View card = LayoutInflater.from(mContext).inflate(viewType, recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(card);

        when(mControllerRendererPool.getController(mContext,
                ContextualCard.CardType.LEGACY_SUGGESTION)).thenReturn(mController);

        mRenderer.bindView(viewHolder, buildContextualCard());

        assertThat(card).isNotNull();
        assertThat(card.hasOnClickListeners()).isTrue();
    }

    @Test
    public void viewClick_shouldInvokeControllerPrimaryClick() {
        final int viewType = mRenderer.getViewType(true /* isHalfWidth */);
        final RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        final View card = LayoutInflater.from(mContext).inflate(viewType, recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(card);
        when(mControllerRendererPool.getController(mContext,
                ContextualCard.CardType.LEGACY_SUGGESTION)).thenReturn(mController);

        mRenderer.bindView(viewHolder, buildContextualCard());

        assertThat(card).isNotNull();
        card.performClick();

        verify(mController).onPrimaryClick(any(ContextualCard.class));
    }

    private ContextualCard buildContextualCard() {
        return new LegacySuggestionContextualCard.Builder()
                .setName("test_name")
                .setTitleText("test_title")
                .setSummaryText("test_summary")
                .setIconDrawable(mContext.getDrawable(R.drawable.ic_do_not_disturb_on_24dp))
                .build();
    }
}
