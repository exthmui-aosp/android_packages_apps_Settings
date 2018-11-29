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

package com.android.settings.wifi.dpp;

import android.os.Bundle;

import com.android.settings.R;

/**
 * After sharing a saved Wi-Fi network, {@code WifiDppConfiguratorActivity} start with this fragment
 * to generate a Wi-Fi DPP QR code for other device to initiate as an enrollee.
 */
public class WifiDppQrCodeGeneratorFragment extends WifiDppQrCodeBaseFragment {
    @Override
    protected int getLayout() {
        return R.layout.wifi_dpp_qrcode_generator_fragment;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
