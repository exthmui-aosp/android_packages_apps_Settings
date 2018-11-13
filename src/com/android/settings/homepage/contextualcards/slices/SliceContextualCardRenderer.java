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

package com.android.settings.homepage.contextualcards.slices;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.widget.EventInfo;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardRenderer;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;

import java.util.Map;

/**
 * Card renderer for {@link ContextualCard} built as slices.
 */
public class SliceContextualCardRenderer implements ContextualCardRenderer,
        SliceView.OnSliceActionListener {
    public static final int VIEW_TYPE = R.layout.homepage_slice_tile;

    private static final String TAG = "SliceCardRenderer";

    @VisibleForTesting
    final Map<String, LiveData<Slice>> mSliceLiveDataMap;

    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final ControllerRendererPool mControllerRendererPool;

    public SliceContextualCardRenderer(Context context, LifecycleOwner lifecycleOwner,
            ControllerRendererPool controllerRendererPool) {
        mContext = context;
        mLifecycleOwner = lifecycleOwner;
        mSliceLiveDataMap = new ArrayMap<>();
        mControllerRendererPool = controllerRendererPool;
    }

    @Override
    public int getViewType(boolean isHalfWidth) {
        return VIEW_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view) {
        return new SliceViewHolder(view);
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard card) {
        final SliceViewHolder cardHolder = (SliceViewHolder) holder;
        final Uri uri = card.getSliceUri();

        //TODO(b/116063073): The URI check should be done earlier when we are performing final
        // filtering after having the full list.
        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Log.w(TAG, "Invalid uri, skipping slice: " + uri);
            return;
        }

        cardHolder.sliceView.setScrollable(false);
        cardHolder.sliceView.setTag(uri);
        //TODO(b/114009676): We will soon have a field to decide what slice mode we should set.
        cardHolder.sliceView.setMode(SliceView.MODE_LARGE);
        LiveData<Slice> sliceLiveData = mSliceLiveDataMap.get(uri.toString());

        if (sliceLiveData == null) {
            sliceLiveData = SliceLiveData.fromUri(mContext, uri);
            mSliceLiveDataMap.put(uri.toString(), sliceLiveData);
        }

        sliceLiveData.removeObservers(mLifecycleOwner);
        sliceLiveData.observe(mLifecycleOwner, slice -> {
            if (slice == null) {
                Log.w(TAG, "Slice is null");
            }
            cardHolder.sliceView.setSlice(slice);
        });

        // Set this listener so we can log the interaction users make on the slice
        cardHolder.sliceView.setOnSliceActionListener(this);

        initDismissalActions(cardHolder, card);
    }

    private void initDismissalActions(SliceViewHolder cardHolder, ContextualCard card) {
        final ViewFlipper viewFlipper = cardHolder.itemView.findViewById(R.id.viewFlipper);
        cardHolder.sliceView.setOnLongClickListener(v -> {
            viewFlipper.showNext();
            return true;
        });

        final Button btnKeep = cardHolder.itemView.findViewById(R.id.keep);
        btnKeep.setOnClickListener(v -> {
            viewFlipper.showPrevious();
        });

        final Button btnRemove = cardHolder.itemView.findViewById(R.id.remove);
        btnRemove.setOnClickListener(v -> {
            mControllerRendererPool.getController(mContext, card.getCardType()).onDismissed(
                    card);
        });
    }

    @Override
    public void onSliceAction(@NonNull EventInfo eventInfo, @NonNull SliceItem sliceItem) {
        //TODO(b/79698338): Log user interaction
    }

    public static class SliceViewHolder extends RecyclerView.ViewHolder {
        public final SliceView sliceView;

        public SliceViewHolder(View view) {
            super(view);
            sliceView = view.findViewById(R.id.slice_view);
        }
    }
}
