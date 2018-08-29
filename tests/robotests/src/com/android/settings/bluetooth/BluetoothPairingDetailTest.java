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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class BluetoothPairingDetailTest {
    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";

    @Mock
    private Resources mResource;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock
    private PreferenceGroup mPreferenceGroup;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    private BluetoothPairingDetail mFragment;
    private Context mContext;
    private BluetoothProgressCategory mAvailableDevicesCategory;
    private FooterPreference mFooterPreference;
    private BluetoothAdapter mBluetoothAdapter;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mFragment = spy(new BluetoothPairingDetail());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mResource).when(mFragment).getResources();

        mAvailableDevicesCategory = spy(new BluetoothProgressCategory(mContext));
        mFooterPreference = new FooterPreference(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);

        mFragment.mBluetoothAdapter = mBluetoothAdapter;
        mFragment.mLocalManager = mLocalManager;
        mFragment.mDeviceListGroup = mPreferenceGroup;
        mFragment.mAlwaysDiscoverable = new AlwaysDiscoverable(mContext);
    }

    @Test
    public void initPreferencesFromPreferenceScreen_findPreferences() {
        doReturn(mAvailableDevicesCategory).when(mFragment)
            .findPreference(BluetoothPairingDetail.KEY_AVAIL_DEVICES);
        doReturn(mFooterPreference).when(mFragment)
            .findPreference(BluetoothPairingDetail.KEY_FOOTER_PREF);

        mFragment.initPreferencesFromPreferenceScreen();

        assertThat(mFragment.mAvailableDevicesCategory).isEqualTo(mAvailableDevicesCategory);
        assertThat(mFragment.mFooterPreference).isEqualTo(mFooterPreference);
    }

    @Test
    public void startScanning_startScanAndRemoveDevices() {
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
        mFragment.mDeviceListGroup = mAvailableDevicesCategory;

        mFragment.enableScanning();

        verify(mFragment).startScanning();
        verify(mAvailableDevicesCategory).removeAll();
    }

    @Test
    public void updateContent_stateOn_addDevices() {
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
        mFragment.mFooterPreference = mFooterPreference;
        doNothing().when(mFragment).addDeviceCategory(any(), anyInt(), any(), anyBoolean());

        mFragment.updateContent(BluetoothAdapter.STATE_ON);

        verify(mFragment).addDeviceCategory(mAvailableDevicesCategory,
                R.string.bluetooth_preference_found_media_devices,
                BluetoothDeviceFilter.ALL_FILTER, false);
        assertThat(mBluetoothAdapter.getScanMode())
                .isEqualTo(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
    }

    @Test
    public void updateContent_stateOff_finish() {
        mFragment.updateContent(BluetoothAdapter.STATE_OFF);

        verify(mFragment).finish();
    }

    @Test
    public void updateBluetooth_bluetoothOff_turnOnBluetooth() {
        mShadowBluetoothAdapter.setEnabled(false);

        mFragment.updateBluetooth();

        assertThat(mBluetoothAdapter.isEnabled()).isTrue();
    }

    @Test
    public void updateBluetooth_bluetoothOn_updateState() {
        mShadowBluetoothAdapter.setEnabled(true);
        doNothing().when(mFragment).updateContent(anyInt());

        mFragment.updateBluetooth();

        verify(mFragment).updateContent(anyInt());
    }

    @Test
    public void onScanningStateChanged_restartScanAfterInitialScanning() {
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
        mFragment.mFooterPreference = mFooterPreference;
        mFragment.mDeviceListGroup = mAvailableDevicesCategory;
        doNothing().when(mFragment).addDeviceCategory(any(), anyInt(), any(), anyBoolean());

        // Initial Bluetooth ON will trigger scan enable, list clear and scan start
        mFragment.updateContent(BluetoothAdapter.STATE_ON);
        verify(mFragment).enableScanning();
        assertThat(mAvailableDevicesCategory.getPreferenceCount()).isEqualTo(0);
        verify(mFragment).startScanning();

        // Subsequent scan started event will not trigger start/stop nor list clear
        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(1)).startScanning();
        verify(mAvailableDevicesCategory, times(1)).setProgress(true);

        // Subsequent scan finished event will trigger scan start without list clean
        mFragment.onScanningStateChanged(false);
        verify(mFragment, times(2)).startScanning();
        verify(mAvailableDevicesCategory, times(2)).setProgress(true);

        // Subsequent scan started event will not trigger any change
        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(2)).startScanning();
        verify(mAvailableDevicesCategory, times(3)).setProgress(true);
        verify(mFragment, never()).stopScanning();

        // Disable scanning will trigger scan stop
        mFragment.disableScanning();
        verify(mFragment, times(1)).stopScanning();

        // Subsequent scan start event will not trigger any change besides progress circle
        mFragment.onScanningStateChanged(true);
        verify(mAvailableDevicesCategory, times(4)).setProgress(true);

        // However, subsequent scan finished event won't trigger new scan start and will stop
        // progress circle from spinning
        mFragment.onScanningStateChanged(false);
        verify(mAvailableDevicesCategory, times(1)).setProgress(false);
        verify(mFragment, times(2)).startScanning();
        verify(mFragment, times(1)).stopScanning();

        // Verify that clean up only happen once at initialization
        verify(mAvailableDevicesCategory, times(1)).removeAll();
    }

    @Test
    public void onBluetoothStateChanged_whenTurnedOnBTShowToast() {
        doNothing().when(mFragment).updateContent(anyInt());

        mFragment.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);

        verify(mFragment).showBluetoothTurnedOnToast();
    }

    @Test
    public void onConnectionStateChanged_connected_finish() {
        mFragment.mSelectedDevice = mBluetoothDevice;
        doReturn(mBluetoothDevice).when(mCachedBluetoothDevice).getDevice();

        mFragment.onConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothAdapter.STATE_CONNECTED);

        verify(mFragment).finish();
    }
}