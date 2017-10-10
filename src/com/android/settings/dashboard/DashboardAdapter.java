/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.R.id;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardData.SuggestionConditionHeaderData;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionControllerMixin;
import com.android.settings.dashboard.suggestions.SuggestionDismissController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionParser;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder>
        implements SummaryLoader.SummaryConsumer {
    public static final String TAG = "DashboardAdapter";
    private static final String STATE_SUGGESTION_LIST = "suggestion_list";
    private static final String STATE_SUGGESTION_LIST_V2 = "suggestion_list_v2";
    private static final String STATE_CATEGORY_LIST = "category_list";
    private static final String STATE_SUGGESTIONS_SHOWN_LOGGED = "suggestions_shown_logged";

    @VisibleForTesting
    static final String STATE_SUGGESTION_CONDITION_MODE = "suggestion_condition_mode";
    @VisibleForTesting
    static final int SUGGESTION_CONDITION_HEADER_POSITION = 0;
    @VisibleForTesting
    static final int MAX_SUGGESTION_TO_SHOW = 5;

    private final IconCache mCache;
    private final Context mContext;
    private final SuggestionControllerMixin mSuggestionControllerMixin;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private final ArrayList<String> mSuggestionsShownLogged;
    private boolean mFirstFrameDrawn;
    private RecyclerView mRecyclerView;
    private SuggestionParser mSuggestionParser;
    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionDismissController mSuggestionDismissHandler;
    private SuggestionDismissController.Callback mCallback;

    @VisibleForTesting
    DashboardData mDashboardData;

    private View.OnClickListener mTileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: get rid of setTag/getTag
            mDashboardFeatureProvider.openTileIntent((Activity) mContext, (Tile) v.getTag());
        }
    };

    public DashboardAdapter(Context context, Bundle savedInstanceState,
            List<Condition> conditions, SuggestionParser suggestionParser,
            SuggestionControllerMixin suggestionControllerMixin,
            SuggestionDismissController.Callback callback) {

        // @deprecated In favor of suggestionsV2 below.
        List<Tile> suggestions = null;
        List<Suggestion> suggestionsV2 = null;
        DashboardCategory category = null;
        int suggestionConditionMode = DashboardData.HEADER_MODE_DEFAULT;

        mContext = context;
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mSuggestionControllerMixin = suggestionControllerMixin;
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        mDashboardFeatureProvider = factory.getDashboardFeatureProvider(context);
        mCache = new IconCache(context);
        mSuggestionParser = suggestionParser;
        mCallback = callback;

        setHasStableIds(true);

        if (savedInstanceState != null) {
            suggestions = savedInstanceState.getParcelableArrayList(STATE_SUGGESTION_LIST);
            suggestionsV2 = savedInstanceState.getParcelableArrayList(STATE_SUGGESTION_LIST_V2);
            category = savedInstanceState.getParcelable(STATE_CATEGORY_LIST);
            suggestionConditionMode = savedInstanceState.getInt(
                    STATE_SUGGESTION_CONDITION_MODE, suggestionConditionMode);
            mSuggestionsShownLogged = savedInstanceState.getStringArrayList(
                    STATE_SUGGESTIONS_SHOWN_LOGGED);
        } else {
            mSuggestionsShownLogged = new ArrayList<>();
        }

        mDashboardData = new DashboardData.Builder()
                .setConditions(conditions)
                .setSuggestions(suggestions)
                .setSuggestionsV2(suggestionsV2)
                .setCategory(category)
                .setSuggestionConditionMode(suggestionConditionMode)
                .build();
    }

    public List<Tile> getSuggestions() {
        return mDashboardData.getSuggestions();
    }

    /**
     * @deprecated in favor of {@link #setCategory(DashboardCategory)} and
     * {@link #setSuggestionsV2(List)}.
     */
    @Deprecated
    public void setCategoriesAndSuggestions(DashboardCategory category, List<Tile> suggestions) {
        tintIcons(category, suggestions);

        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(suggestions.subList(0,
                        Math.min(suggestions.size(), MAX_SUGGESTION_TO_SHOW)))
                .setCategory(category)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    public void setSuggestionsV2(List<Suggestion> data) {
        // TODO: Tint icon
        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestionsV2(data)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    public void setCategory(DashboardCategory category) {
        tintIcons(category, null);
        final DashboardData prevData = mDashboardData;
        Log.d(TAG, "adapter setCategory called");
        mDashboardData = new DashboardData.Builder(prevData)
                .setCategory(category)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    public void setConditions(List<Condition> conditions) {
        final DashboardData prevData = mDashboardData;
        Log.d(TAG, "adapter setConditions called");
        mDashboardData = new DashboardData.Builder(prevData)
                .setConditions(conditions)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    /**
     * @deprecated in favor of {@link #onSuggestionDismissed(Suggestion)}.
     */
    @Deprecated
    public void onSuggestionDismissed(Tile suggestion) {
        final List<Tile> suggestions = mDashboardData.getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        if (suggestions.size() == 1) {
            // The only suggestion is dismissed, and the the empty suggestion container will
            // remain as the dashboard item. Need to refresh the dashboard list.
            final DashboardData prevData = mDashboardData;
            mDashboardData = new DashboardData.Builder(prevData)
                    .setSuggestions(null)
                    .build();
            notifyDashboardDataChanged(prevData);
        } else {
            mSuggestionAdapter.removeSuggestion(suggestion);
        }
    }

    public void onSuggestionDismissed(Suggestion suggestion) {
        final List<Suggestion> list = mDashboardData.getSuggestionsV2();
        if (list == null || list.size() == 0) {
            return;
        }
        if (list.size() == 1) {
            // The only suggestion is dismissed, and the the empty suggestion container will
            // remain as the dashboard item. Need to refresh the dashboard list.
            final DashboardData prevData = mDashboardData;
            mDashboardData = new DashboardData.Builder(prevData)
                    .setSuggestionsV2(null)
                    .build();
            notifyDashboardDataChanged(prevData);
        } else {
            mSuggestionAdapter.removeSuggestion(suggestion);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final int position = mDashboardData.getPositionByTile(tile);
        if (position != DashboardData.POSITION_NOT_FOUND) {
            // Since usually tile in parameter and tile in mCategories are same instance,
            // which is hard to be detected by DiffUtil, so we notifyItemChanged directly.
            notifyItemChanged(position, mDashboardData.getItemTypeByPosition(position));
        }
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (viewType == R.layout.suggestion_condition_header) {
            return new SuggestionAndConditionHeaderHolder(view);
        }
        if (viewType == R.layout.suggestion_condition_container) {
            return new SuggestionAndConditionContainerHolder(view);
        }
        return new DashboardItemHolder(view);
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        final int type = mDashboardData.getItemTypeByPosition(position);
        switch (type) {
            case R.layout.dashboard_tile:
                final Tile tile = (Tile) mDashboardData.getItemEntityByPosition(position);
                onBindTile(holder, tile);
                holder.itemView.setTag(tile);
                holder.itemView.setOnClickListener(mTileClickListener);
                break;
            case R.layout.suggestion_condition_container:
                onBindConditionAndSuggestion(
                        (SuggestionAndConditionContainerHolder) holder, position);
                break;
            case R.layout.suggestion_condition_header:
                onBindSuggestionConditionHeader((SuggestionAndConditionHeaderHolder) holder,
                        (SuggestionConditionHeaderData)
                                mDashboardData.getItemEntityByPosition(position));
                break;
            case R.layout.suggestion_condition_footer:
                holder.itemView.setOnClickListener(v -> {
                    mMetricsFeatureProvider.action(mContext,
                            MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND, false);
                    DashboardData prevData = mDashboardData;
                    mDashboardData = new DashboardData.Builder(prevData).setSuggestionConditionMode(
                            DashboardData.HEADER_MODE_COLLAPSED).build();
                    notifyDashboardDataChanged(prevData);
                    mRecyclerView.scrollToPosition(SUGGESTION_CONDITION_HEADER_POSITION);
                });
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return mDashboardData.getItemIdByPosition(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mDashboardData.getItemTypeByPosition(position);
    }

    @Override
    public int getItemCount() {
        return mDashboardData.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // save the view so that we can scroll it when expanding/collapsing the suggestion and
        // conditions.
        mRecyclerView = recyclerView;
    }

    public Object getItem(long itemId) {
        return mDashboardData.getItemEntityById(itemId);
    }

    /**
     * @deprecated in favor of {@link #getSuggestionV2(int)}.
     */
    @Deprecated
    public Tile getSuggestion(int position) {
        return mSuggestionAdapter.getSuggestion(position);
    }

    public Suggestion getSuggestionV2(int position) {
        return mSuggestionAdapter.getSuggestionsV2(position);
    }

    @VisibleForTesting
    void notifyDashboardDataChanged(DashboardData prevData) {
        if (mFirstFrameDrawn && prevData != null) {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DashboardData
                    .ItemsDataDiffCallback(prevData.getItemList(), mDashboardData.getItemList()));
            diffResult.dispatchUpdatesTo(this);
        } else {
            mFirstFrameDrawn = true;
            notifyDataSetChanged();
        }
    }

    @VisibleForTesting
    void onBindSuggestionConditionHeader(final SuggestionAndConditionHeaderHolder holder,
            SuggestionConditionHeaderData data) {
        final int curMode = mDashboardData.getSuggestionConditionMode();
        final int nextMode = data.hiddenSuggestionCount > 0
                && data.conditionCount > 0
                && curMode != DashboardData.HEADER_MODE_SUGGESTION_EXPANDED
                ? DashboardData.HEADER_MODE_SUGGESTION_EXPANDED
                : DashboardData.HEADER_MODE_FULLY_EXPANDED;

        final boolean hasConditions = data.conditionCount > 0;
        if (data.conditionCount > 0) {
            holder.icon.setImageIcon(data.conditionIcons.get(0));
            holder.icon.setVisibility(View.VISIBLE);
            if (data.conditionCount == 1) {
                holder.title.setText(data.title);
                holder.title.setTextColor(Utils.getColorAccent(mContext));
                holder.icons.setVisibility(View.INVISIBLE);
            } else {
                holder.title.setText(null);
                updateConditionIcons(data.conditionIcons, holder.icons);
                holder.icons.setVisibility(View.VISIBLE);
            }
        } else {
            holder.icon.setVisibility(View.INVISIBLE);
            holder.icons.setVisibility(View.INVISIBLE);
        }

        if (data.hiddenSuggestionCount > 0) {
            holder.summary.setTextColor(Color.BLACK);
            if (curMode == DashboardData.HEADER_MODE_COLLAPSED) {
                if (data.conditionCount > 0) {
                    holder.summary.setText(mContext.getResources().getQuantityString(
                            R.plurals.suggestions_collapsed_summary,
                            data.hiddenSuggestionCount, data.hiddenSuggestionCount));
                } else {
                    holder.title.setText(mContext.getResources().getQuantityString(
                            R.plurals.suggestions_collapsed_title,
                            data.hiddenSuggestionCount, data.hiddenSuggestionCount));
                    holder.title.setTextColor(Color.BLACK);
                    holder.summary.setText(null);
                }
            } else if (curMode == DashboardData.HEADER_MODE_DEFAULT) {
                if (data.conditionCount > 0) {
                    holder.summary.setText(mContext.getString(
                            R.string.suggestions_summary, data.hiddenSuggestionCount));
                } else {
                    holder.title.setText(mContext.getString(
                            R.string.suggestions_more_title, data.hiddenSuggestionCount));
                    holder.title.setTextColor(Color.BLACK);
                    holder.summary.setText(null);
                }
            }
        } else if (data.conditionCount > 1) {
            holder.summary.setTextColor(Utils.getColorAccent(mContext));
            holder.summary.setText(
                    mContext.getString(R.string.condition_summary, data.conditionCount));
        } else {
            holder.summary.setText(null);
        }

        final Resources res = mContext.getResources();
        final int padding = res.getDimensionPixelOffset(
                curMode == DashboardData.HEADER_MODE_COLLAPSED
                        ? R.dimen.suggestion_condition_header_padding_collapsed
                        : R.dimen.suggestion_condition_header_padding_expanded);
        holder.itemView.setPadding(0, padding, 0, padding);

        holder.itemView.setOnClickListener(v -> {
            if (hasConditions) {
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND, true);
            }
            DashboardData prevData = mDashboardData;
            final boolean wasCollapsed = curMode == DashboardData.HEADER_MODE_COLLAPSED;
            mDashboardData = new DashboardData.Builder(prevData)
                    .setSuggestionConditionMode(nextMode).build();
            notifyDashboardDataChanged(prevData);
            if (wasCollapsed) {
                mRecyclerView.scrollToPosition(SUGGESTION_CONDITION_HEADER_POSITION);
            }
        });
    }

    @VisibleForTesting
    void onBindConditionAndSuggestion(final SuggestionAndConditionContainerHolder holder,
            int position) {
        // If there is suggestions to show, it will be at position 0 as we don't show the suggestion
        // header anymore.
        final List<Suggestion> suggestionsV2 = mDashboardData.getSuggestionsV2();
        final List<Tile> suggestions = mDashboardData.getSuggestions();

        boolean conditionOnly = true;
        if (position == SUGGESTION_CONDITION_HEADER_POSITION) {
            if (suggestions != null && suggestions.size() > 0) {
                conditionOnly = false;
                mSuggestionAdapter = new SuggestionAdapter(mContext, (List<Tile>)
                        mDashboardData.getItemEntityByPosition(position),
                        null, mSuggestionsShownLogged);
                mSuggestionDismissHandler = new SuggestionDismissController(mContext,
                        holder.data, mSuggestionControllerMixin, mSuggestionParser, mCallback);
                holder.data.setAdapter(mSuggestionAdapter);
            } else if (suggestionsV2 != null && suggestionsV2.size() > 0) {
                conditionOnly = false;
                mSuggestionAdapter = new SuggestionAdapter(mContext, null,
                        (List<Suggestion>) mDashboardData.getItemEntityByPosition(position),
                        mSuggestionsShownLogged);
                mSuggestionDismissHandler = new SuggestionDismissController(mContext,
                        holder.data, mSuggestionControllerMixin, null /* parser */, mCallback);
                holder.data.setAdapter(mSuggestionAdapter);
            }
        }
        if (conditionOnly) {
            ConditionAdapter adapter = new ConditionAdapter(mContext,
                    (List<Condition>) mDashboardData.getItemEntityByPosition(position),
                    mDashboardData.getSuggestionConditionMode());
            adapter.addDismissHandling(holder.data);
            holder.data.setAdapter(adapter);
        }
        holder.data.setLayoutManager(new LinearLayoutManager(mContext));
    }

    private void onBindTile(DashboardItemHolder holder, Tile tile) {
        holder.icon.setImageDrawable(mCache.getIcon(tile.icon));
        holder.title.setText(tile.title);
        if (!TextUtils.isEmpty(tile.summary)) {
            holder.summary.setText(tile.summary);
            holder.summary.setVisibility(View.VISIBLE);
        } else {
            holder.summary.setVisibility(View.GONE);
        }
    }

    private void tintIcons(DashboardCategory category, List<Tile> suggestions) {
        if (!mDashboardFeatureProvider.shouldTintIcon()) {
            return;
        }
        // TODO: Better place for tinting?
        final TypedArray a = mContext.obtainStyledAttributes(new int[]{
                android.R.attr.colorControlNormal});
        final int tintColor = a.getColor(0, mContext.getColor(R.color.fallback_tintColor));
        a.recycle();
        if (category != null) {
            for (Tile tile : category.tiles) {
                if (tile.isIconTintable) {
                    // If this drawable is tintable, tint it to match the color.
                    tile.icon.setTint(tintColor);
                }
            }
        }
        if (suggestions != null) {
            for (Tile suggestion : suggestions) {
                if (suggestion.isIconTintable) {
                    suggestion.icon.setTint(tintColor);
                }
            }
        }
    }

    void onSaveInstanceState(Bundle outState) {
        final DashboardCategory category = mDashboardData.getCategory();
        final List<Tile> suggestions = mDashboardData.getSuggestions();
        final List<Suggestion> suggestionV2 = mDashboardData.getSuggestionsV2();
        if (suggestions != null) {
            outState.putParcelableArrayList(STATE_SUGGESTION_LIST, new ArrayList<>(suggestions));
        }
        if (suggestionV2 != null) {
            outState.putParcelableArrayList(STATE_SUGGESTION_LIST_V2,
                    new ArrayList<>(suggestionV2));
        }
        if (category != null) {
            outState.putParcelable(STATE_CATEGORY_LIST, category);
        }
        outState.putStringArrayList(STATE_SUGGESTIONS_SHOWN_LOGGED, mSuggestionsShownLogged);
        outState.putInt(STATE_SUGGESTION_CONDITION_MODE,
                mDashboardData.getSuggestionConditionMode());
    }

    private void updateConditionIcons(List<Icon> icons, ViewGroup parent) {
        if (icons == null || icons.size() < 2) {
            parent.setVisibility(View.INVISIBLE);
            return;
        }
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        parent.removeAllViews();
        for (int i = 1, size = icons.size(); i < size; i++) {
            ImageView icon = (ImageView) inflater.inflate(
                    R.layout.condition_header_icon, parent, false);
            icon.setImageIcon(icons.get(i));
            parent.addView(icon);
        }
        parent.setVisibility(View.VISIBLE);
    }

    public static class IconCache {
        private final Context mContext;
        private final ArrayMap<Icon, Drawable> mMap = new ArrayMap<>();

        public IconCache(Context context) {
            mContext = context;
        }

        public Drawable getIcon(Icon icon) {
            if (icon == null) {
                return null;
            }
            Drawable drawable = mMap.get(icon);
            if (drawable == null) {
                drawable = icon.loadDrawable(mContext);
                mMap.put(icon, drawable);
            }
            return drawable;
        }
    }

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(android.R.id.icon);
            title = itemView.findViewById(android.R.id.title);
            summary = itemView.findViewById(android.R.id.summary);
        }
    }

    public static class SuggestionAndConditionHeaderHolder extends DashboardItemHolder {
        public final LinearLayout icons;
        public final ImageView expandIndicator;

        public SuggestionAndConditionHeaderHolder(View itemView) {
            super(itemView);
            icons = itemView.findViewById(id.additional_icons);
            expandIndicator = itemView.findViewById(id.expand_indicator);
        }
    }

    public static class SuggestionAndConditionContainerHolder extends DashboardItemHolder {
        public final RecyclerView data;

        public SuggestionAndConditionContainerHolder(View itemView) {
            super(itemView);
            data = itemView.findViewById(id.data);
        }
    }

}
