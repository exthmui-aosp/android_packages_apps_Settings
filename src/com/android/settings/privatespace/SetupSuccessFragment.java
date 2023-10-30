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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/** Fragment for the final screen shown on successful completion of private space setup. */
public class SetupSuccessFragment extends Fragment {
    private static final String TAG = "SetupSuccessFragment";

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.privatespace_setup_success, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.privatespace_done_label)
                        .setListener(onClickNext())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

        return rootView;
    }

    private View.OnClickListener onClickNext() {
        return v -> {
            accessPrivateSpaceToast();
            // TODO(b/306228087): Replace with the intent to launch All Apps once it is working.
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startActivity(startMain);
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        };
    }

    private void accessPrivateSpaceToast() {
        Toast.makeText(getContext(), R.string.scrolldown_to_access, Toast.LENGTH_SHORT).show();
    }
}
