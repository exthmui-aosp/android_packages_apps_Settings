/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.dashboard;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

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
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
        })
public class DashboardAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mContext;
    @Mock
    private View mView;
    @Mock
    private Condition mCondition;
    @Mock
    private Resources mResources;
    private FakeFeatureFactory mFactory;
    private DashboardAdapter mDashboardAdapter;
    private DashboardAdapter.SuggestionAndConditionHeaderHolder mSuggestionHolder;
    private DashboardData.SuggestionConditionHeaderData mSuggestionHeaderData;
    private List<Condition> mConditionList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest();
        when(mFactory.dashboardFeatureProvider.shouldTintIcon()).thenReturn(true);
        when(mFactory.suggestionsFeatureProvider
                .getSuggestionIdentifier(any(Context.class), any(Tile.class)))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    return ((Tile) args[1]).intent.getComponent().getPackageName();
                });

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getQuantityString(any(int.class), any(int.class), any()))
                .thenReturn("");

        mConditionList = new ArrayList<>();
        mConditionList.add(mCondition);
        when(mCondition.shouldShow()).thenReturn(true);
        mDashboardAdapter = new DashboardAdapter(mContext, null, mConditionList, null, null, null);
        mSuggestionHeaderData = new DashboardData.SuggestionConditionHeaderData(mConditionList, 1);
        when(mView.getTag()).thenReturn(mCondition);
    }

    @Test
    public void testSuggestionsLogs_nullSuggestionsList_shouldNotCrash() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3", "pkg4", "pkg5"));
        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);

        // set suggestions to null
        final DashboardData prevData = mDashboardAdapter.mDashboardData;
        mDashboardAdapter.mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(null)
                .build();

        mSuggestionHolder.itemView.callOnClick();
        // no crash
    }

    @Test
    public void testSuggestionDismissed_notOnlySuggestion_updateSuggestionOnly() {
        final DashboardAdapter adapter =
                spy(new DashboardAdapter(mContext, null, null, null, null, null));
        final List<Tile> suggestions = makeSuggestions("pkg1", "pkg2", "pkg3");
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        adapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        final Tile suggestionToRemove = suggestions.get(1);
        adapter.onSuggestionDismissed(suggestionToRemove);

        assertThat(adapter.mDashboardData).isEqualTo(dashboardData);
        assertThat(suggestions.size()).isEqualTo(2);
        assertThat(suggestions.contains(suggestionToRemove)).isFalse();
        verify(adapter, never()).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSuggestionDismissed_moreThanTwoSuggestions_defaultMode_shouldNotCrash() {
        final RecyclerView data = new RecyclerView(RuntimeEnvironment.application);
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);
        final List<Tile> suggestions =
                makeSuggestions("pkg1", "pkg2", "pkg3", "pkg4");
        final DashboardAdapter adapter = spy(new DashboardAdapter(mContext, null /*savedInstance */,
                null /* conditions */, null /* suggestionParser */,
                null /* suggestionControllerMixin */, null /* callback */));
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        adapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);
        // default mode, only displaying 2 suggestions

        adapter.onSuggestionDismissed(suggestions.get(1));

        // verify operations that access the lists will not cause ConcurrentModificationException
        assertThat(holder.data.getAdapter().getItemCount()).isEqualTo(1);
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        // should not crash
    }

    @Test
    public void testSuggestionDismissed_onlySuggestion_updateDashboardData() {
        DashboardAdapter adapter =
                spy(new DashboardAdapter(mContext, null, null, null, null, null));
        final List<Tile> suggestions = makeSuggestions("pkg1");
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        adapter.onSuggestionDismissed(suggestions.get(0));

        assertThat(adapter.mDashboardData).isNotEqualTo(dashboardData);
        verify(adapter).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSetCategoriesAndSuggestions_iconTinted() {
        TypedArray mockTypedArray = mock(TypedArray.class);
        doReturn(mockTypedArray).when(mContext).obtainStyledAttributes(any(int[].class));
        doReturn(0x89000000).when(mockTypedArray).getColor(anyInt(), anyInt());

        List<Tile> packages = makeSuggestions("pkg1");
        Icon mockIcon = mock(Icon.class);
        packages.get(0).isIconTintable = true;
        packages.get(0).icon = mockIcon;

        mDashboardAdapter.setCategoriesAndSuggestions(null /* category */, packages);

        verify(mockIcon).setTint(eq(0x89000000));
    }

    @Test
    public void testSetCategories_iconTinted() {
        TypedArray mockTypedArray = mock(TypedArray.class);
        doReturn(mockTypedArray).when(mContext).obtainStyledAttributes(any(int[].class));
        doReturn(0x89000000).when(mockTypedArray).getColor(anyInt(), anyInt());

        final DashboardCategory category = new DashboardCategory();
        final Icon mockIcon = mock(Icon.class);
        final Tile tile = new Tile();
        tile.isIconTintable = true;
        tile.icon = mockIcon;
        category.addTile(tile);

        mDashboardAdapter.setCategory(category);

        verify(mockIcon).setTint(eq(0x89000000));
    }

    @Test
    public void testSetCategoriesAndSuggestions_limitSuggestionSize() {
        List<Tile> packages =
                makeSuggestions("pkg1", "pkg2", "pkg3", "pkg4", "pkg5", "pkg6", "pkg7");
        mDashboardAdapter.setCategoriesAndSuggestions(null /* category */, packages);

        assertThat(mDashboardAdapter.mDashboardData.getSuggestions().size())
                .isEqualTo(DashboardAdapter.MAX_SUGGESTION_TO_SHOW);
    }

    @Test
    public void testBindConditionAndSuggestion_shouldSetSuggestionAdapterAndNoCrash() {
        mDashboardAdapter = new DashboardAdapter(mContext, null, null, null, null, null);
        final List<Tile> suggestions = makeSuggestions("pkg1");
        final DashboardCategory category = new DashboardCategory();
        category.addTile(mock(Tile.class));

        mDashboardAdapter.setCategoriesAndSuggestions(category, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        mDashboardAdapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        verify(data).setAdapter(any(SuggestionAdapter.class));
        // should not crash
    }

    @Test
    public void testBindConditionAndSuggestion_v2_shouldSetSuggestionAdapterAndNoCrash() {
        mDashboardAdapter = new DashboardAdapter(mContext, null, null, null, null, null);
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1");
        final DashboardCategory category = new DashboardCategory();
        category.addTile(mock(Tile.class));

        mDashboardAdapter.setSuggestionsV2(suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        mDashboardAdapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        verify(data).setAdapter(any(SuggestionAdapter.class));
        // should not crash
    }

    @Test
    public void testBindConditionAndSuggestion_emptySuggestion_shouldSetConditionAdpater() {
        final Bundle savedInstance = new Bundle();
        savedInstance.putInt(DashboardAdapter.STATE_SUGGESTION_CONDITION_MODE,
                DashboardData.HEADER_MODE_FULLY_EXPANDED);
        mDashboardAdapter = new DashboardAdapter(mContext, savedInstance, mConditionList,
                null /* SuggestionParser */, null /* suggestionControllerMixin */,
                null /* SuggestionDismissController.Callback */);

        final List<Tile> suggestions = new ArrayList<>();
        final DashboardCategory category = new DashboardCategory();
        category.addTile(mock(Tile.class));
        mDashboardAdapter.setCategoriesAndSuggestions(category, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        mDashboardAdapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        verify(data).setAdapter(any(ConditionAdapter.class));
    }

    /**
     * @deprecated in favor of {@link #makeSuggestionsV2(String...)}
     */
    @Deprecated
    private List<Tile> makeSuggestions(String... pkgNames) {
        final List<Tile> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            Tile suggestion = new Tile();
            suggestion.intent = new Intent("action");
            suggestion.intent.setComponent(new ComponentName(pkgName, "cls"));
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    private List<Suggestion> makeSuggestionsV2(String... pkgNames) {
        final List<Suggestion> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            final Suggestion suggestion = new Suggestion.Builder(pkgName)
                    .setPendingIntent(mock(PendingIntent.class))
                    .build();
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    private void setupSuggestions(List<Tile> suggestions) {
        mDashboardAdapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        final Context context = RuntimeEnvironment.application;
        mSuggestionHolder = new DashboardAdapter.SuggestionAndConditionHeaderHolder(
                LayoutInflater.from(context).inflate(
                        R.layout.suggestion_condition_header, new RelativeLayout(context), true));
    }


}
