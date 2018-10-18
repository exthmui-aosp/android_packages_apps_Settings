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

package com.android.settings.homepage.deviceinfo;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.slices.SettingsSliceProvider;

// This is a slice helper class for EmergencyInfo
public class EmergencyInfoSlice {
    /**
     * The path denotes the unique name of emergency info slice.
     */
    public static final String PATH_EMERGENCY_INFO_CARD = "emergency_info_card";

    /**
     * Backing Uri for the Emergency Info Slice.
     */
    public static final Uri EMERGENCY_INFO_CARD_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(PATH_EMERGENCY_INFO_CARD)
            .build();

    private static final String ACTION_EDIT_EMERGENCY_INFO = "android.settings.EDIT_EMERGENCY_INFO";

    public static Slice getSlice(Context context) {
        final ListBuilder listBuilder = new ListBuilder(context, EMERGENCY_INFO_CARD_URI,
                ListBuilder.INFINITY);
        listBuilder.addRow(
                new ListBuilder.RowBuilder()
                        .setTitle(context.getText(R.string.emergency_info_title))
                        .setSubtitle(
                                context.getText(R.string.emergency_info_contextual_card_summary))
                        .setPrimaryAction(generatePrimaryAction(context)));
        return listBuilder.build();
    }

    private static SliceAction generatePrimaryAction(Context context) {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0 /* requestCode */,
                        new Intent(ACTION_EDIT_EMERGENCY_INFO),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return SliceAction.create(
                pendingIntent,
                IconCompat.createWithResource(context, R.drawable.empty_icon),
                ListBuilder.SMALL_IMAGE,
                context.getText(R.string.emergency_info_title));
    }
}
