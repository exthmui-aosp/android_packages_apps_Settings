/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialogCompat.class,
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioSharingJoinDialogFragmentTest {

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, /* groupId= */ 1, /* isActive= */ true);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, /* groupId= */ 2, /* isActive= */ false);
    private static final AudioSharingJoinDialogFragment.DialogEventListener EMPTY_EVENT_LISTENER =
            new AudioSharingJoinDialogFragment.DialogEventListener() {
                @Override
                public void onShareClick() {}

                @Override
                public void onCancelClick() {}
            };

    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    private Fragment mParent;
    private AudioSharingJoinDialogFragment mFragment;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        AlertDialog latestAlertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        if (latestAlertDialog != null) {
            latestAlertDialog.dismiss();
            ShadowAlertDialogCompat.reset();
        }
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        mFragment = new AudioSharingJoinDialogFragment();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mFragment.getContext());
        when(mLocalBtManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
    }

    @Test
    public void getMetricsCategory_notInSharing_correctValue() {
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        int category = mFragment.getMetricsCategory();
        shadowMainLooper().idle();
        assertThat(category).isEqualTo(SettingsEnums.DIALOG_START_AUDIO_SHARING);
    }

    @Test
    public void getMetricsCategory_inSharing_correctValue() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        int category = mFragment.getMetricsCategory();
        shadowMainLooper().idle();
        assertThat(category).isEqualTo(SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE);
    }

    @Test
    public void onCreateDialog_flagOff_dialogNotExist() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mFragment.show(mParent, new ArrayList<>(), mCachedDevice2, EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_flagOn_dialogShowTextForSingleDevice() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mFragment.show(mParent, new ArrayList<>(), mCachedDevice2, EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString()).isEqualTo(TEST_DEVICE_NAME2);
    }

    @Test
    public void onCreateDialog_flagOn_dialogShowTextForTwoDevice() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        mFragment.show(mParent, list, mCachedDevice2, EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_share_dialog_subtitle,
                                TEST_DEVICE_NAME1,
                                TEST_DEVICE_NAME2));
    }

    @Test
    public void onCreateDialog_dialogIsShowing_updateDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        mFragment.show(mParent, list, mCachedDevice2, EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        // Update the content
        ArrayList<AudioSharingDeviceItem> list2 = new ArrayList<>();
        list2.add(TEST_DEVICE_ITEM2);
        mFragment.show(mParent, list2, mCachedDevice1, EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_share_dialog_subtitle,
                                TEST_DEVICE_NAME2,
                                TEST_DEVICE_NAME1));
    }

    @Test
    public void onCreateDialog_clickCancel_dialogDismiss() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mFragment.show(mParent, new ArrayList<>(), mCachedDevice2, EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        dialog.findViewById(R.id.negative_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void onCreateDialog_clickBtn_callbackTriggered() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AtomicBoolean isShareBtnClicked = new AtomicBoolean(false);
        mFragment.show(
                mParent,
                new ArrayList<>(),
                mCachedDevice2,
                new AudioSharingJoinDialogFragment.DialogEventListener() {
                    @Override
                    public void onShareClick() {
                        isShareBtnClicked.set(true);
                    }

                    @Override
                    public void onCancelClick() {}
                });
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        dialog.findViewById(R.id.positive_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isShareBtnClicked.get()).isTrue();
    }

    @Test
    public void onCreateDialog_clickCancel_callbackTriggered() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AtomicBoolean isCancelBtnClicked = new AtomicBoolean(false);
        mFragment.show(
                mParent,
                new ArrayList<>(),
                mCachedDevice2,
                new AudioSharingJoinDialogFragment.DialogEventListener() {
                    @Override
                    public void onShareClick() {}

                    @Override
                    public void onCancelClick() {
                        isCancelBtnClicked.set(true);
                    }
                });
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        dialog.findViewById(R.id.negative_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isCancelBtnClicked.get()).isTrue();
    }
}
