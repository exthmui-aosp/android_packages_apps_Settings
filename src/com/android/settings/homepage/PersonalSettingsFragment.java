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

package com.android.settings.homepage;

import static com.android.settings.homepage.ContextualCardsAdapter.SPAN_COUNT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

public class PersonalSettingsFragment extends InstrumentedFragment {

    private static final String TAG = "PersonalSettingsFragment";

    private RecyclerView mCardsContainer;
    private GridLayoutManager mLayoutManager;
    private ContextualCardsAdapter mContextualCardsAdapter;
    private ContextualCardManager mContextualCardManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContextualCardManager = new ContextualCardManager(getContext(), getSettingsLifecycle());
        mContextualCardManager.loadContextualCards(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.settings_homepage, container, false);
        mCardsContainer = rootView.findViewById(R.id.card_container);
        mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT,
                GridLayoutManager.VERTICAL, false /* reverseLayout */);
        mCardsContainer.setLayoutManager(mLayoutManager);
        mContextualCardsAdapter = new ContextualCardsAdapter(getContext(),
                this /* lifecycleOwner */, mContextualCardManager);
        mCardsContainer.setAdapter(mContextualCardsAdapter);
        mContextualCardManager.setListener(mContextualCardsAdapter);

        return rootView;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_HOMEPAGE;
    }
}
