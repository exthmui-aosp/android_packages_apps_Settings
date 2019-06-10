/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings;

import android.app.Activity;
import android.os.Bundle;

import com.android.settings.accessibility.AccessibilityGestureNavigationTutorial;
import com.android.settings.R;

/**
 * This activity is to create the tutorial dialog in gesture navigation settings since we couldn't
 * use the dialog utils because SystemNavigationGestureSettings extends RadioButtonPickerFragment,
 * not SettingsPreferenceFragment.
 */
public class SettingsTutorialDialogWrapperActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDialog();
    }

    private void showDialog() {
        AccessibilityGestureNavigationTutorial
                .showGestureNavigationSettingsTutorialDialog(this, dialog -> finish());
    }
}