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

package com.android.settings.homepage.contextualcards.slices;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ConnectedDeviceSliceTest {

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;

    private List<CachedBluetoothDevice> mCachedDevices = new ArrayList<CachedBluetoothDevice>();
    private Context mContext;
    private ConnectedDeviceSlice mConnectedDeviceSlice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mConnectedDeviceSlice = spy(new ConnectedDeviceSlice(mContext));
    }

    @Test
    public void getSlice_hasConnectedDevices_shouldBeCorrectSliceContent() {
        final String title = "BluetoothTitle";
        final String summary = "BluetoothSummary";
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_homepage_connected_device);
        final PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                new Intent("test action"), 0);
        doReturn(title).when(mCachedBluetoothDevice).getName();
        doReturn(summary).when(mCachedBluetoothDevice).getConnectionSummary();
        mCachedDevices.add(mCachedBluetoothDevice);
        doReturn(mCachedDevices).when(mConnectedDeviceSlice).getBluetoothConnectedDevices();
        doReturn(icon).when(mConnectedDeviceSlice).getConnectedDeviceIcon(any());
        doReturn(pendingIntent).when(mConnectedDeviceSlice).getBluetoothDetailIntent(any());
        final Slice slice = mConnectedDeviceSlice.getSlice();

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertTitle(sliceItems, title);
    }

    @Test
    public void getSlice_hasNoConnectedDevices_shouldReturnCorrectHeader() {
        final List<CachedBluetoothDevice> connectedBluetoothList = new ArrayList<>();
        doReturn(connectedBluetoothList).when(mConnectedDeviceSlice).getBluetoothConnectedDevices();
        final Slice slice = mConnectedDeviceSlice.getSlice();

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.no_connected_devices));
    }
}