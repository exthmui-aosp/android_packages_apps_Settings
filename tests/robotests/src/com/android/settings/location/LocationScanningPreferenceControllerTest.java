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

package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LocationScanningPreferenceControllerTest {

  private Context mContext;
  private LocationScanningPreferenceController mController;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mContext = spy(RuntimeEnvironment.application.getApplicationContext());
    mController = new LocationScanningPreferenceController(mContext);
  }

  @Test
  public void testLocationScanning_byDefault_shouldBeShown() {
    assertThat(mController.isAvailable()).isTrue();
  }

  @Test
  @Config(qualifiers = "mcc999")
  public void testLocationScanning_ifDisabled_shouldNotBeShown() {
    assertThat(mController.isAvailable()).isFalse();
  }
}