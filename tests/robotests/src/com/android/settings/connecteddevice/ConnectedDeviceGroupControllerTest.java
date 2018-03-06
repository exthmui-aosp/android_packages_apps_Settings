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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.bluetooth.ConnectedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.usb.ConnectedUsbDeviceUpdater;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConnectedDeviceGroupControllerTest {
    private static final String PREFERENCE_KEY_1 = "pref_key_1";

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private ConnectedBluetoothDeviceUpdater mConnectedBluetoothDeviceUpdater;
    @Mock
    private ConnectedUsbDeviceUpdater mConnectedUsbDeviceUpdater;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    private PreferenceGroup mPreferenceGroup;
    private Context mContext;
    private Preference mPreference;
    private ConnectedDeviceGroupController mConnectedDeviceGroupController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY_1);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mPreferenceGroup = spy(new PreferenceScreen(mContext, null));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        doReturn(mContext).when(mDashboardFragment).getContext();

        mConnectedDeviceGroupController = new ConnectedDeviceGroupController(mDashboardFragment,
                mLifecycle, mConnectedBluetoothDeviceUpdater, mConnectedUsbDeviceUpdater);
        mConnectedDeviceGroupController.mPreferenceGroup = mPreferenceGroup;
    }

    @Test
    public void testOnDeviceAdded_firstAdd_becomeVisibleAndPreferenceAdded() {
        mConnectedDeviceGroupController.onDeviceAdded(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mPreferenceGroup.findPreference(PREFERENCE_KEY_1)).isEqualTo(mPreference);
    }

    @Test
    public void testOnDeviceRemoved_lastRemove_becomeInvisibleAndPreferenceRemoved() {
        mPreferenceGroup.addPreference(mPreference);

        mConnectedDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void testOnDeviceRemoved_notLastRemove_stillVisible() {
        mPreferenceGroup.setVisible(true);
        mPreferenceGroup.addPreference(mPreference);
        mPreferenceGroup.addPreference(new Preference(mContext));

        mConnectedDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    public void testDisplayPreference_becomeInvisible() {
        doReturn(mPreferenceGroup).when(mPreferenceScreen).findPreference(anyString());

        mConnectedDeviceGroupController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    public void testLifecycle() {
        // register the callback in onStart()
        mLifecycle.handleLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_START);
        verify(mConnectedBluetoothDeviceUpdater).registerCallback();
        verify(mConnectedUsbDeviceUpdater).registerCallback();

        // unregister the callback in onStop()
        mLifecycle.handleLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_STOP);
        verify(mConnectedBluetoothDeviceUpdater).unregisterCallback();
        verify(mConnectedUsbDeviceUpdater).unregisterCallback();
    }
}
