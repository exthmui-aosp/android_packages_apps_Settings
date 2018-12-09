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

package com.android.settings.network;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

// This controls a header at the top of the Network & internet page that only appears when there
// are two or more active mobile subscriptions. It shows an overview of available network
// connections with an entry for wifi (if connected) and an entry for each subscription.
public class MultiNetworkHeaderController extends BasePreferenceController {

    public MultiNetworkHeaderController(Context context, String key) {
      super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return UNSUPPORTED_ON_DEVICE;
    }
}
