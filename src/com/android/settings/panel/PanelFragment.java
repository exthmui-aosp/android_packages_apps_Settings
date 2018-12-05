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

package com.android.settings.panel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.overlay.FeatureFactory;

public class PanelFragment extends Fragment {

    private static final String TAG = "PanelFragment";

    private TextView mTitleView;
    private Button mSeeMoreButton;
    private Button mDoneButton;
    private RecyclerView mPanelSlices;

    @VisibleForTesting
    PanelSlicesAdapter mAdapter;

    private View.OnClickListener mDoneButtonListener = (v) -> {
        Log.d(TAG, "Closing dialog");
        getActivity().finish();
    };

    public PanelFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        final View view = inflater.inflate(R.layout.panel_layout, container, false);

        mPanelSlices = view.findViewById(R.id.panel_parent_layout);
        mSeeMoreButton = view.findViewById(R.id.see_more);
        mDoneButton = view.findViewById(R.id.done);
        mTitleView = view.findViewById(R.id.panel_title);

        final Bundle arguments = getArguments();
        final String panelType = arguments.getString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT);

        final PanelContent panel = FeatureFactory.getFactory(activity)
                .getPanelFeatureProvider()
                .getPanel(activity, panelType);

        mAdapter = new PanelSlicesAdapter(this, panel.getSlices());

        mPanelSlices.setHasFixedSize(true);
        mPanelSlices.setLayoutManager(new LinearLayoutManager((activity)));
        mPanelSlices.setAdapter(mAdapter);

        mTitleView.setText(panel.getTitle());

        mSeeMoreButton.setOnClickListener(getSeeMoreListener(panel.getSeeMoreIntent()));
        mDoneButton.setOnClickListener(mDoneButtonListener);

        return view;
    }

    private View.OnClickListener getSeeMoreListener(final Intent intent) {
        return (v) -> {
            final FragmentActivity activity = getActivity();
            activity.startActivity(intent);
            activity.finish();
        };
    }
}
