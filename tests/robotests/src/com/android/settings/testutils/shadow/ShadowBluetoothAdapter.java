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

package com.android.settings.testutils.shadow;

import android.bluetooth.BluetoothAdapter;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(value = BluetoothAdapter.class, inheritImplementationMethods = true)
public class ShadowBluetoothAdapter extends org.robolectric.shadows.ShadowBluetoothAdapter {

    private String mName;
    private int mScanMode;
    private int mState;

    /**
     * Do nothing, implement it to avoid null pointer error inside BluetoothAdapter
     */
    @Implementation
    public List<Integer> getSupportedProfiles() {
        return new ArrayList<Integer>();
    }

    public void setName(String name) {
        mName = name;
    }

    @Implementation
    public String getName() {
        return mName;
    }

    @Implementation
    public void setScanMode(int scanMode) {
        mScanMode = scanMode;
    }

    @Implementation
    public int getScanMode() {
        return mScanMode;
    }

    @Implementation
    public int getConnectionState() {
        return mState;
    }

    public void setConnectionState(int state) {
        mState = state;
    }
}
