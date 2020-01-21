/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.telephony.SubscriptionManager;

import com.android.ims.ImsManager;
import com.android.settings.SettingsActivity;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.network.telephony.MobileNetworkUtils;

public class WifiCallingSuggestionActivity extends SettingsActivity {

    public static boolean isSuggestionComplete(Context context) {
        final WifiCallingQueryImsState queryState =
                new WifiCallingQueryImsState(context,
                SubscriptionManager.getDefaultVoiceSubscriptionId());
        if (!ImsManager.isWfcEnabledByPlatform(context) ||
                !MobileNetworkUtils.isWfcProvisionedOnDevice(
                        SubscriptionManager.getDefaultVoiceSubscriptionId())) {
            return true;
        }
        return queryState.isEnabledByUser() && queryState.isAllowUserControl();
    }
}
