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

package com.android.settings.panel;

import android.content.Context;
import android.provider.Settings;

public class PanelFeatureProviderImpl implements PanelFeatureProvider {

    @Override
    public PanelContent getPanel(Context context, String panelType) {
        switch (panelType) {
            case Settings.Panel.ACTION_INTERNET_CONNECTIVITY:
                return InternetConnectivityPanel.create(context);
            case Settings.Panel.ACTION_VOLUME:
                return VolumePanel.create(context);
        }

        throw new IllegalStateException("No matching panel for: "  + panelType);
    }
}
