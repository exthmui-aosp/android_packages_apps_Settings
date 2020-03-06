/*
 * Copyright (C) 2019 The Android Open Source Project
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
 *
 */

package com.android.settings.media;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Process;
import android.text.TextUtils;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputSliceConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class,
        MediaOutputIndicatorSliceTest.ShadowSliceBackgroundWorker.class})
public class MediaOutputIndicatorSliceTest {

    private static final String TEST_DEVICE_1_NAME = "test_device_1_name";
    private static final String TEST_DEVICE_2_NAME = "test_device_2_name";
    private static final String TEST_PACKAGE_NAME = "com.test";

    private static MediaOutputIndicatorWorker sMediaOutputIndicatorWorker;

    private final List<MediaDevice> mDevices = new ArrayList<>();

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private MediaController mMediaController;
    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private MediaDevice mDevice1;
    @Mock
    private MediaDevice mDevice2;
    @Mock
    private Drawable mTestDrawable;

    private Context mContext;
    private MediaOutputIndicatorSlice mMediaOutputIndicatorSlice;
    private AudioManager mAudioManager;
    private MediaSession.Token mToken;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        sMediaOutputIndicatorWorker = spy(new MediaOutputIndicatorWorker(mContext,
                MEDIA_OUTPUT_INDICATOR_SLICE_URI));
        mToken = new MediaSession.Token(Process.myUid(), null);
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        // Setup Bluetooth environment
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        // Setup mock devices
        when(mDevice1.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(mDevice1.getIcon()).thenReturn(mTestDrawable);
        when(mDevice1.getMaxVolume()).thenReturn(100);
        when(mDevice1.isConnected()).thenReturn(true);
        when(mDevice2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(mDevice2.getIcon()).thenReturn(mTestDrawable);
        when(mDevice2.getMaxVolume()).thenReturn(100);
        when(mDevice2.isConnected()).thenReturn(false);

        mMediaOutputIndicatorSlice = new MediaOutputIndicatorSlice(mContext);
    }

    @Test
    public void getSlice_withConnectedDevice_verifyMetadata() {
        mDevices.add(mDevice1);
        mDevices.add(mDevice2);
        when(sMediaOutputIndicatorWorker.getMediaDevices()).thenReturn(mDevices);
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        assertThat(metadata.getTitle()).isEqualTo(mContext.getText(R.string.media_output_title));
        assertThat(metadata.getSubtitle()).isEqualTo(TEST_DEVICE_1_NAME);
        assertThat(metadata.isErrorSlice()).isFalse();
    }

    @Test
    public void getSlice_noConnectedDevice_returnErrorSlice() {
        mDevices.clear();
        when(sMediaOutputIndicatorWorker.getMediaDevices()).thenReturn(mDevices);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        assertThat(metadata.isErrorSlice()).isTrue();
    }

    @Test
    public void getSlice_audioModeIsInCommunication_returnErrorSlice() {
        mDevices.add(mDevice1);
        mDevices.add(mDevice2);
        when(sMediaOutputIndicatorWorker.getMediaDevices()).thenReturn(mDevices);
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        assertThat(metadata.isErrorSlice()).isTrue();
    }

    @Test
    public void getSlice_audioModeIsRingtone_returnErrorSlice() {
        mDevices.add(mDevice1);
        mDevices.add(mDevice2);
        when(sMediaOutputIndicatorWorker.getMediaDevices()).thenReturn(mDevices);
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();
        mAudioManager.setMode(AudioManager.MODE_RINGTONE);

        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        assertThat(metadata.isErrorSlice()).isTrue();
    }

    @Test
    public void getSlice_audioModeIsInCall_returnErrorSlice() {
        mDevices.add(mDevice1);
        mDevices.add(mDevice2);
        when(sMediaOutputIndicatorWorker.getMediaDevices()).thenReturn(mDevices);
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);

        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        assertThat(metadata.isErrorSlice()).isTrue();
    }

    @Test
    public void onNotifyChange_withActiveLocalMedia_verifyIntentExtra() {
        when(mMediaController.getSessionToken()).thenReturn(mToken);
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        doReturn(mMediaController).when(sMediaOutputIndicatorWorker)
                .getActiveLocalMediaController();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mMediaOutputIndicatorSlice.onNotifyChange(new Intent());
        verify(mContext).startActivity(intentCaptor.capture());

        assertThat(TextUtils.equals(TEST_PACKAGE_NAME, intentCaptor.getValue().getStringExtra(
                MediaOutputSliceConstants.EXTRA_PACKAGE_NAME))).isTrue();
        assertThat(mToken == intentCaptor.getValue().getExtras().getParcelable(
                MediaOutputSliceConstants.KEY_MEDIA_SESSION_TOKEN)).isTrue();
    }

    @Test
    public void onNotifyChange_withoutActiveLocalMedia_verifyIntentExtra() {
        doReturn(mMediaController).when(sMediaOutputIndicatorWorker)
                .getActiveLocalMediaController();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mMediaOutputIndicatorSlice.onNotifyChange(new Intent());
        verify(mContext).startActivity(intentCaptor.capture());

        assertThat(TextUtils.isEmpty(intentCaptor.getValue().getStringExtra(
                MediaOutputSliceConstants.EXTRA_PACKAGE_NAME))).isTrue();
        assertThat(intentCaptor.getValue().getExtras().getParcelable(
                MediaOutputSliceConstants.KEY_MEDIA_SESSION_TOKEN) == null).isTrue();
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {

        @Implementation
        public static SliceBackgroundWorker getInstance(Uri uri) {
            return sMediaOutputIndicatorWorker;
        }
    }
}
