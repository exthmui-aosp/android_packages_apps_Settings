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
package com.android.settings.connecteddevice;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller to maintain the {@link android.support.v7.preference.PreferenceGroup} for all
 * connected devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 *
 * TODO(b/69333961): add real impl
 */
public class ConnectedDeviceGroupController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback {
    private BluetoothDeviceUpdater mBluetoothController;

    public ConnectedDeviceGroupController(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        mBluetoothController.registerCallback();
    }

    @Override
    public void onStop() {
        mBluetoothController.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void onDeviceAdded(Preference preference) {

    }

    @Override
    public void onDeviceRemoved(Preference preference) {

    }
}
