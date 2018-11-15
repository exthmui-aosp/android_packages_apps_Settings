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

package com.android.settings.homepage.contextualcards;

import static android.provider.SettingsSlicesContract.KEY_WIFI;

import android.annotation.Nullable;

import com.android.settings.homepage.contextualcards.deviceinfo.BatterySlice;
import com.android.settings.homepage.contextualcards.slices.ConnectedDeviceSlice;
import com.android.settings.homepage.contextualcards.slices.LowStorageSlice;
import com.android.settings.intelligence.ContextualCardProto.ContextualCard;
import com.android.settings.intelligence.ContextualCardProto.ContextualCardList;
import com.android.settings.wifi.WifiSlice;

import com.google.android.settings.intelligence.libs.contextualcards.ContextualCardProvider;

/** Provides dynamic card for SettingsIntelligence. */
public class SettingsContextualCardProvider extends ContextualCardProvider {

    public static final String CARD_AUTHORITY = "com.android.settings.homepage.contextualcards";

    @Override
    @Nullable
    public ContextualCardList getContextualCards() {
        final ContextualCard wifiCard =
                ContextualCard.newBuilder()
                        .setSliceUri(WifiSlice.WIFI_URI.toString())
                        .setCardName(KEY_WIFI)
                        .setCardCategory(ContextualCard.Category.IMPORTANT)
                        .build();
        final ContextualCard batteryInfoCard =
                ContextualCard.newBuilder()
                        .setSliceUri(BatterySlice.BATTERY_CARD_URI.toString())
                        .setCardName(BatterySlice.PATH_BATTERY_INFO)
                        .setCardCategory(ContextualCard.Category.DEFAULT)
                        .build();
        final ContextualCard connectedDeviceCard =
                ContextualCard.newBuilder()
                        .setSliceUri(ConnectedDeviceSlice.CONNECTED_DEVICE_URI.toString())
                        .setCardName(ConnectedDeviceSlice.PATH_CONNECTED_DEVICE)
                        .setCardCategory(ContextualCard.Category.IMPORTANT)
                        .build();
        final ContextualCard lowStorageCard =
                ContextualCard.newBuilder()
                        .setSliceUri(LowStorageSlice.LOW_STORAGE_URI.toString())
                        .setCardName(LowStorageSlice.PATH_LOW_STORAGE)
                        .setCardCategory(ContextualCard.Category.IMPORTANT)
                        .build();
        final ContextualCardList cards = ContextualCardList.newBuilder()
                .addCard(wifiCard)
                .addCard(batteryInfoCard)
                .addCard(connectedDeviceCard)
                .addCard(lowStorageCard)
                .build();

        return cards;
    }
}
