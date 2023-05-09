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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.utils.ActivityControllerWrapper;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HapClientProfile;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tests for {@link AccessibilityHearingAidPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class AccessibilityHearingAidPreferenceControllerTest {
    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final String TEST_DEVICE_ADDRESS_2 = "00:A2:A2:A2:A2:A2";
    private static final String TEST_DEVICE_NAME = "TEST_HEARING_AID_BT_DEVICE_NAME";
    private static final String HEARING_AID_PREFERENCE = "hearing_aid_preference";

    private BluetoothAdapter mBluetoothAdapter;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private Activity mContext;
    private Preference mHearingAidPreference;
    private AccessibilityHearingAidPreferenceController mPreferenceController;
    private ShadowApplication mShadowApplication;

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedSubBluetoothDevice;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private BluetoothEventManager mEventManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private HearingAidProfile mHearingAidProfile;
    @Mock
    private HapClientProfile mHapClientProfile;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowApplication = ShadowApplication.getInstance();

        mContext = spy((Activity) ActivityControllerWrapper.setup(
                Robolectric.buildActivity(Activity.class)).get());
        setupEnvironment();

        mHearingAidPreference = new Preference(mContext);
        mHearingAidPreference.setKey(HEARING_AID_PREFERENCE);
        mPreferenceController = new AccessibilityHearingAidPreferenceController(mContext,
                HEARING_AID_PREFERENCE);
        mPreferenceController.setPreference(mHearingAidPreference);
        mHearingAidPreference.setSummary("");
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void getSummary_connectedAshaHearingAidRightSide_connectedRightSideSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_RIGHT);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHearingAid.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString().contentEquals(
                "TEST_HEARING_AID_BT_DEVICE_NAME, right only")).isTrue();
    }

    @Test
    public void getSummary_connectedAshaHearingAidBothSide_connectedBothSideSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);
        when(mCachedSubBluetoothDevice.isConnected()).thenReturn(true);
        when(mCachedBluetoothDevice.getSubDevice()).thenReturn(mCachedSubBluetoothDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHearingAid.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString().contentEquals(
                "TEST_HEARING_AID_BT_DEVICE_NAME, left and right")).isTrue();
    }

    @Test
    public void getSummary_connectedLeAudioHearingAidLeftSide_connectedLeftSideSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(new HashSet<>());
        when(mHapClientProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHapClient.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString().contentEquals(
                "TEST_HEARING_AID_BT_DEVICE_NAME, left only")).isTrue();
    }

    @Test
    public void getSummary_connectedLeAudioHearingAidRightSide_connectedRightSideSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_RIGHT);
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(new HashSet<>());
        when(mHapClientProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHapClient.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString().contentEquals(
                "TEST_HEARING_AID_BT_DEVICE_NAME, right only")).isTrue();
    }

    @Test
    public void getSummary_connectedLeAudioHearingAidLeftAndRightSide_connectedSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT_AND_RIGHT);
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(new HashSet<>());
        when(mHapClientProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHapClient.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString().contentEquals(
                "TEST_HEARING_AID_BT_DEVICE_NAME, left and right")).isTrue();
    }

    @Test
    public void getSummary_connectedLeAudioHearingAidBothSide_connectedBothSideSummary() {
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(generateMemberDevices());
        when(mCachedSubBluetoothDevice.isConnected()).thenReturn(true);
        when(mHapClientProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHapClient.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString()).isEqualTo(
                "TEST_HEARING_AID_BT_DEVICE_NAME, left and right");
    }

    @Test
    public void getSummary_connectedMultipleHearingAids_connectedMultipleDevicesSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(
                generateMultipleHearingAidDeviceList());

        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHearingAid.EXTRA_STATE, BluetoothHearingAid.STATE_CONNECTED);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary().toString().contentEquals(
                "TEST_HEARING_AID_BT_DEVICE_NAME +1 more")).isTrue();
    }

    @Test
    public void getSummary_bluetoothOff_disconnectedSummary() {
        mPreferenceController.onStart();
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        sendIntent(intent);

        assertThat(mHearingAidPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.accessibility_hearingaid_not_connected_summary));
    }

    @Ignore
    @Test
    public void handleHearingAidPreferenceClick_noHearingAid_launchHearingAidInstructionDialog() {
        mPreferenceController = spy(new AccessibilityHearingAidPreferenceController(mContext,
                HEARING_AID_PREFERENCE));
        mPreferenceController.setPreference(mHearingAidPreference);
        doNothing().when(mPreferenceController).launchHearingAidInstructionDialog();
        mPreferenceController.handlePreferenceTreeClick(mHearingAidPreference);

        verify(mPreferenceController).launchHearingAidInstructionDialog();
    }

    @Ignore
    @Test
    public void handleHearingAidPreferenceClick_withHearingAid_launchBluetoothDeviceDetailSetting
            () {
        mPreferenceController = spy(new AccessibilityHearingAidPreferenceController(mContext,
                HEARING_AID_PREFERENCE));
        mPreferenceController.setPreference(mHearingAidPreference);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        mPreferenceController.handlePreferenceTreeClick(mHearingAidPreference);

        verify(mPreferenceController).launchBluetoothDeviceDetailSetting(mCachedBluetoothDevice);
    }

    @Test
    public void onSupportHearingAidProfile_isAvailable() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);
        mPreferenceController = new AccessibilityHearingAidPreferenceController(mContext,
                HEARING_AID_PREFERENCE);
        mPreferenceController.setPreference(mHearingAidPreference);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void onSupportHapClientProfile_isAvailable() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HAP_CLIENT);
        mPreferenceController = new AccessibilityHearingAidPreferenceController(mContext,
                HEARING_AID_PREFERENCE);
        mPreferenceController.setPreference(mHearingAidPreference);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void onNotSupportAnyHearingAidRelatedProfile_isNotAvailable() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mPreferenceController = new AccessibilityHearingAidPreferenceController(mContext,
                HEARING_AID_PREFERENCE);
        mPreferenceController.setPreference(mHearingAidPreference);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void getConnectedHearingAidDevice_doNotReturnSubDevice() {
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());
        when(mLocalBluetoothManager.getCachedDeviceManager().isSubDevice(mBluetoothDevice))
                .thenReturn(true);

        assertThat(mPreferenceController.getConnectedHearingAidDevice()).isNull();
    }

    @Test
    @Config(shadows = ShadowAlertDialogCompat.class)
    public void onActiveDeviceChanged_hearingAidProfile_launchHearingAidPairingDialog() {
        final FragmentActivity mActivity = Robolectric.setupActivity(FragmentActivity.class);
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);
        mPreferenceController.setFragmentManager(mActivity.getSupportFragmentManager());

        mPreferenceController.onActiveDeviceChanged(mCachedBluetoothDevice,
                BluetoothProfile.HEARING_AID);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
    }

    @Test
    public void onServiceConnected_onHearingAidProfileConnected_updateSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        mPreferenceController.onServiceConnected();

        assertThat(mHearingAidPreference.getSummary().toString()).isEqualTo(
                "TEST_HEARING_AID_BT_DEVICE_NAME, left only");
    }

    @Test
    public void onServiceConnected_onHapClientProfileConnected_updateSummary() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_RIGHT);
        when(mHapClientProfile.getConnectedDevices()).thenReturn(generateHearingAidDeviceList());

        mPreferenceController.onStart();
        mPreferenceController.onServiceConnected();

        assertThat(mHearingAidPreference.getSummary().toString()).isEqualTo(
                "TEST_HEARING_AID_BT_DEVICE_NAME, right only");
    }

    private void setupEnvironment() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShadowBluetoothAdapter = Shadow.extract(mBluetoothAdapter);
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HAP_CLIENT);
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);
        mBluetoothAdapter.enable();

        doReturn(mEventManager).when(mLocalBluetoothManager).getEventManager();
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
        when(mLocalBluetoothProfileManager.getHapClientProfile()).thenReturn(mHapClientProfile);
        when(mHearingAidProfile.isProfileReady()).thenReturn(true);
        when(mHapClientProfile.isProfileReady()).thenReturn(true);
        when(mCachedDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mCachedBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME);
    }

    private void sendIntent(Intent intent) {
        for (BroadcastReceiver receiver : mShadowApplication.getReceiversForIntent(intent)) {
            receiver.onReceive(mContext, intent);
        }
    }

    private List<BluetoothDevice> generateHearingAidDeviceList() {
        final List<BluetoothDevice> deviceList = new ArrayList<>(1);
        deviceList.add(mBluetoothDevice);
        return deviceList;
    }

    private List<BluetoothDevice> generateMultipleHearingAidDeviceList() {
        // Generates different Bluetooth devices for testing multiple devices
        final List<BluetoothDevice> deviceList = new ArrayList<>(2);
        deviceList.add(mBluetoothDevice);
        deviceList.add(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2));
        return deviceList;
    }

    private Set<CachedBluetoothDevice> generateMemberDevices() {
        final Set<CachedBluetoothDevice> memberDevices = new HashSet<>();
        memberDevices.add(mCachedSubBluetoothDevice);
        return memberDevices;
    }
}
