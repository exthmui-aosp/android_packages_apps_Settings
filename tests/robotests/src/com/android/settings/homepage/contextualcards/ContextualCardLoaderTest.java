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

import static com.android.settings.homepage.contextualcards.ContextualCardLoader.DEFAULT_CARD_COUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardLoaderTest {

    private Context mContext;
    private ContextualCardLoader mContextualCardLoader;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContextualCardLoader = spy(new ContextualCardLoader(mContext));
    }

    @Test
    public void isCardEligibleToDisplay_customCard_returnTrue() {
        final ContextualCard customCard = new ContextualCard.Builder()
                .setName("custom_card")
                .setCardType(ContextualCard.CardType.DEFAULT)
                .setTitleText("custom_title")
                .setSummaryText("custom_summary")
                .build();

        assertThat(mContextualCardLoader.isCardEligibleToDisplay(customCard)).isTrue();
    }

    @Test
    public void isCardEligibleToDisplay_invalidScheme_returnFalse() {
        final String sliceUri = "contet://com.android.settings.slices/action/flashlight";

        assertThat(
                mContextualCardLoader.isCardEligibleToDisplay(
                        getContextualCard(sliceUri))).isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_noProvider_returnFalse() {
        final String sliceUri = "content://com.android.settings.test.slices/action/flashlight";

        assertThat(
                mContextualCardLoader.isCardEligibleToDisplay(
                        getContextualCard(sliceUri))).isFalse();
    }

    @Test
    public void getDisplayableCards_twoEligibleCards_shouldShowAll() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(2)
                .collect(Collectors.toList());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(any(List.class));

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(cards.size());
    }

    @Test
    public void getDisplayableCards_fiveEligibleCardsNoLarge_shouldShowDefaultCardCount() {
        final List<ContextualCard> fiveCards = getContextualCardListWithNoLargeCard();
        doReturn(fiveCards).when(mContextualCardLoader).filterEligibleCards(any(List.class));

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(
                fiveCards);

        assertThat(result).hasSize(DEFAULT_CARD_COUNT);
    }

    @Test
    public void getDisplayableCards_threeEligibleCardsOneLarge_shouldShowThreeCards() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(2)
                .collect(Collectors.toList());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(any(List.class));

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(3);
    }

    @Test
    public void getDisplayableCards_threeEligibleCardsTwoLarge_shouldShowTwoCards() {
        final List<ContextualCard> threeCards = getContextualCardList().stream().limit(3)
                .collect(Collectors.toList());
        doReturn(threeCards).when(mContextualCardLoader).filterEligibleCards(any(List.class));

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(
                threeCards);

        assertThat(result).hasSize(2);
    }

    @Test
    public void loadInBackground_legacyMode_shouldReturnNothing() {
        assertThat(mContext.getResources().getBoolean(R.bool.config_use_legacy_suggestion))
                .isTrue();

        assertThat(mContextualCardLoader.loadInBackground()).isEmpty();
    }

    private ContextualCard getContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_card")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }

    private List<ContextualCard> getContextualCardList() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_wifi")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/flashlight"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_connected")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_battery")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.BATTERY_INFO_SLICE_URI)
                .build());
        return cards;
    }

    private List<ContextualCard> getContextualCardListWithNoLargeCard() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_rotate")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/auto_rotate"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/flashlight"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_bt")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse("content://android.settings.test.slices/action/bluetooth"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_battery")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.BATTERY_INFO_SLICE_URI)
                .build());
        return cards;
    }
}
