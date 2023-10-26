/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/** Fragment educating about the usage of Private Space. */
public class PrivateSpaceEducation extends Fragment {
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.privatespace_education_screen, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.privatespace_setup_button_label)
                        .setListener(onSetup())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        mixin.getPrimaryButtonView().setFilterTouchesWhenObscured(true);
        mixin.setSecondaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.privatespace_cancel_label)
                        .setListener(onCancel())
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(
                                androidx.appcompat.R.style
                                        .Base_TextAppearance_AppCompat_Widget_Button)
                        .build());
        mixin.getSecondaryButtonView().setFilterTouchesWhenObscured(true);

        return rootView;
    }

    private View.OnClickListener onSetup() {
        return v -> {
            if (PrivateSpaceMaintainer.getInstance(getContext()).createPrivateSpace()) {
                finishActivity();
            }
        };
    }

    private View.OnClickListener onCancel() {
        return v -> {
            finishActivity();
        };
    }

    private void finishActivity() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
