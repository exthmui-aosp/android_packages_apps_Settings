/*
 * Copyright 2018 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowBluetoothAdapter.class)
public class PreviouslyConnectedDevicePreferenceControllerTest {

    private final String KEY = "test_key";

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Mock
    private DockUpdater mDockUpdater;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private Preference mSeeAllPreference;

    private Context mContext;
    private PreviouslyConnectedDevicePreferenceController mPreConnectedDeviceController;
    private PreferenceGroup mPreferenceGroup;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mPreConnectedDeviceController =
                new PreviouslyConnectedDevicePreferenceController(mContext, KEY);
        mPreConnectedDeviceController.setBluetoothDeviceUpdater(mBluetoothDeviceUpdater);
        mPreConnectedDeviceController.setSavedDockUpdater(mDockUpdater);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());

        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        mPreferenceGroup.setVisible(false);
        mPreConnectedDeviceController.setPreferenceGroup(mPreferenceGroup);
        mPreConnectedDeviceController.mSeeAllPreference = mSeeAllPreference;
    }

    @Test
    public void callbackCanRegisterAndUnregister() {
        // register the callback in onStart()
        mPreConnectedDeviceController.onStart();
        verify(mBluetoothDeviceUpdater).registerCallback();
        verify(mDockUpdater).registerCallback();
        verify(mContext).registerReceiver(mPreConnectedDeviceController.mReceiver,
                mPreConnectedDeviceController.mIntentFilter);

        // unregister the callback in onStop()
        mPreConnectedDeviceController.onStop();
        verify(mBluetoothDeviceUpdater).unregisterCallback();
        verify(mDockUpdater).unregisterCallback();
        verify(mContext).unregisterReceiver(mPreConnectedDeviceController.mReceiver);
    }

    @Test
    public void getAvailabilityStatus_noBluetoothDockFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mPreConnectedDeviceController.setSavedDockUpdater(null);

        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasBluetoothFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mPreConnectedDeviceController.setSavedDockUpdater(null);

        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_haveDockFeature_returnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
            AVAILABLE);
    }

    @Test
    public void onDeviceAdded_addDevicePreference_displayIt() {
        mPreConnectedDeviceController.onDeviceAdded(new Preference(mContext));

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void onDeviceAdded_addFourDevicePreference_onlyDisplayThree() {
        mPreConnectedDeviceController.onDeviceAdded(new Preference(mContext));
        mPreConnectedDeviceController.onDeviceAdded(new Preference(mContext));
        mPreConnectedDeviceController.onDeviceAdded(new Preference(mContext));
        mPreConnectedDeviceController.onDeviceAdded(new Preference(mContext));

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(3);
    }

    @Test
    public void onDeviceRemoved_removeLastDevice_setInvisible() {
        final Preference preference = new Preference(mContext);
        mPreferenceGroup.addPreference(preference);

        mPreConnectedDeviceController.onDeviceRemoved(preference);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void updatePreferenceVisibility_bluetoothIsEnable_shouldShowCorrectText() {
        mShadowBluetoothAdapter.setEnabled(true);
        mPreConnectedDeviceController.updatePreferenceVisibility();

        verify(mSeeAllPreference).setSummary("");
    }

    @Test
    public void updatePreferenceVisibility_bluetoothIsDisable_shouldShowCorrectText() {
        mShadowBluetoothAdapter.setEnabled(false);
        mPreConnectedDeviceController.updatePreferenceVisibility();

        verify(mSeeAllPreference).setSummary(
                mContext.getString(R.string.connected_device_see_all_summary));
    }
}
