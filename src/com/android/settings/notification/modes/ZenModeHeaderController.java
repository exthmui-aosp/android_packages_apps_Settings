/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.notification.modes;

import android.app.Flags;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.LayoutPreference;

import java.util.concurrent.TimeUnit;

public class ZenModeHeaderController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;
    private EntityHeaderController mHeaderController;

    ZenModeHeaderController(
            @NonNull  Context context,
            @NonNull String key,
            @NonNull DashboardFragment fragment,
            @Nullable ZenModesBackend backend) {
        super(context, key, backend);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return Flags.modesApi();
    }

    @Override
    public void updateState(Preference preference) {
        if (getAZR() == null || mFragment == null) {
            return;
        }

        if (mHeaderController == null) {
            final LayoutPreference pref = (LayoutPreference) preference;
            mHeaderController = EntityHeaderController.newInstance(
                    mFragment.getActivity(),
                    mFragment,
                    pref.findViewById(R.id.entity_header));
        }
        Drawable icon = null;
        try {
            icon = getMode().getIcon(mContext).get(200, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // no icon
        }
        mHeaderController.setIcon(icon)
                .setLabel(getAZR().getName())
                .done(false /* rebindActions */);
    }
}
