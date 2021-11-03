/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.homepage;

import android.content.ComponentName;

/** Activity for Slices to launch Settings deep link page */
public class SliceDeepLinkHomepageActivity extends SettingsHomepageActivity {
    @Override
    protected void setHomeActivity() {
        // do not overwrite homepage activity in SettingsApplication
    }

    @Override
    protected ComponentName getDeepLinkComponent() {
        return new ComponentName(getApplicationContext(), getClass());
    }
}
