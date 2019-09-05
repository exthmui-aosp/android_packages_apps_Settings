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

package com.android.settings.enterprise;

import static org.mockito.Mockito.when;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class FailedPasswordWipeManagedProfilePreferenceControllerTest
    extends FailedPasswordWipePreferenceControllerTestBase {

    public FailedPasswordWipeManagedProfilePreferenceControllerTest() {
        super("failed_password_wipe_managed_profile");
    }

    @Override
    public void setUp() {
        super.setUp();
        mController = new FailedPasswordWipeManagedProfilePreferenceController(mContext);
    }

    @Override
    public void setMaximumFailedPasswordsBeforeWipe(int maximum) {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getMaximumFailedPasswordsBeforeWipeInManagedProfile()).thenReturn(maximum);
    }
}
