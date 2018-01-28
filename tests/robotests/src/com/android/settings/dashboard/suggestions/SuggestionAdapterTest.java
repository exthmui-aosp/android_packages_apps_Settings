/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.service.settings.suggestions.Suggestion;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;
    @Mock
    private SuggestionControllerMixin mSuggestionControllerMixin;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private SuggestionAdapter mSuggestionAdapter;
    private DashboardAdapter.DashboardItemHolder mSuggestionHolder;
    private List<Suggestion> mOneSuggestion;
    private List<Suggestion> mTwoSuggestions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        final Suggestion suggestion1 = new Suggestion.Builder("id1")
                .setTitle("Test suggestion 1")
                .build();
        final Suggestion suggestion2 = new Suggestion.Builder("id2")
                .setTitle("Test suggestion 2")
                .build();
        mOneSuggestion = new ArrayList<>();
        mOneSuggestion.add(suggestion1);
        mTwoSuggestions = new ArrayList<>();
        mTwoSuggestions.add(suggestion1);
        mTwoSuggestions.add(suggestion2);
    }

    @Test
    public void getItemCount_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mOneSuggestion, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mTwoSuggestions, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mOneSuggestion, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemViewType(0))
                .isEqualTo(R.layout.suggestion_tile);
    }

    @Test
    public void getItemType_hasButton_shouldReturnSuggestionWithButton() {
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion.Builder("id")
                .setFlags(Suggestion.FLAG_HAS_BUTTON)
                .setTitle("123")
                .setSummary("456")
                .build());
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                suggestions, new ArrayList<>());

        assertThat(mSuggestionAdapter.getItemViewType(0))
                .isEqualTo(R.layout.suggestion_tile_with_button);
    }

    @Test
    public void onBindViewHolder_shouldLog() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
                R.layout.suggestion_tile, new LinearLayout(mContext), true));
        mSuggestionHolder = new DashboardAdapter.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mOneSuggestion, new ArrayList<>());

        // Bind twice
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        // Log once
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mContext, MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                mOneSuggestion.get(0).getId());
    }

    @Test
    public void onBindViewHolder_itemViewShouldHandleClick()
            throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.performClick();

        verify(mSuggestionControllerMixin).launchSuggestion(suggestions.get(0));
        verify(suggestions.get(0).getPendingIntent()).send();
    }

    @Test
    public void getSuggestions_shouldReturnSuggestionWhenMatch() {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        assertThat(mSuggestionAdapter.getSuggestion(0)).isNotNull();
    }

    private void setupSuggestions(Context context, List<Suggestion> suggestions) {
        mSuggestionAdapter = new SuggestionAdapter(context, mSuggestionControllerMixin,
                suggestions, new ArrayList<>());
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
                new FrameLayout(RuntimeEnvironment.application),
                mSuggestionAdapter.getItemViewType(0));
    }

    private List<Suggestion> makeSuggestions(String... pkgNames) {
        final List<Suggestion> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            final Suggestion suggestion = new Suggestion.Builder(pkgName)
                    .setPendingIntent(mock(PendingIntent.class))
                    .build();
            suggestions.add(suggestion);
        }
        return suggestions;
    }
}
