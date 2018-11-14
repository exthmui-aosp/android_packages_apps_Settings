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

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardController;
import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;

/**
 * Card controller for {@link ContextualCard} built as slices.
 */
public class SliceContextualCardController implements ContextualCardController {

    private static final String TAG = "SliceCardController";

    private ContextualCardUpdateListener mCardUpdateListener;

    @Override
    public int getCardType() {
        return ContextualCard.CardType.SLICE;
    }

    @Override
    public void onPrimaryClick(ContextualCard card) {

    }

    @Override
    public void onActionClick(ContextualCard card) {
        //TODO(b/113783548): Implement feedback mechanism
    }

    @Override
    public void onDismissed(ContextualCard card) {
        //TODO(b/113783548): Mark this card as dismissed in db and reload loader.
    }

    @Override
    public void setCardUpdateListener(ContextualCardUpdateListener listener) {
            mCardUpdateListener = listener;
    }
}
