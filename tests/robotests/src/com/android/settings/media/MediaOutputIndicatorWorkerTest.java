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
 */

package com.android.settings.media;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.VolumeProvider;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;

import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class MediaOutputIndicatorWorkerTest {
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");

    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;

    private Context mContext;
    private MediaOutputIndicatorWorker mMediaOutputIndicatorWorker;
    private ShadowApplication mShadowApplication;
    private ContentResolver mResolver;
    private List<MediaController> mMediaControllers = new ArrayList<>();
    private PlaybackState mPlaybackState;
    private MediaController.PlaybackInfo mPlaybackInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowApplication = ShadowApplication.getInstance();
        mContext = spy(RuntimeEnvironment.application);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        mMediaOutputIndicatorWorker = new MediaOutputIndicatorWorker(mContext, URI);
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);
        mMediaControllers.add(mMediaController);
        when(mMediaSessionManager.getActiveSessions(any())).thenReturn(mMediaControllers);

        mResolver = mock(ContentResolver.class);
        doReturn(mResolver).when(mContext).getContentResolver();
    }

    @Test
    public void onSlicePinned_registerCallback() {
        mMediaOutputIndicatorWorker.onSlicePinned();
        verify(mBluetoothEventManager).registerCallback(mMediaOutputIndicatorWorker);
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void onSliceUnpinned_unRegisterCallback() {
        mMediaOutputIndicatorWorker.onSlicePinned();
        mMediaOutputIndicatorWorker.onSliceUnpinned();
        verify(mBluetoothEventManager).unregisterCallback(mMediaOutputIndicatorWorker);
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onReceive_shouldNotifyChange() {
        mMediaOutputIndicatorWorker.onSlicePinned();

        final Intent intent = new Intent(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
        for (BroadcastReceiver receiver : mShadowApplication.getReceiversForIntent(intent)) {
            receiver.onReceive(mContext, intent);
        }

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void getActiveLocalMediaController_localMediaPlaying_returnController() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isEqualTo(
                mMediaController);
    }

    @Test
    public void getActiveLocalMediaController_remoteMediaPlaying_returnNull() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isNull();
    }

    @Test
    public void getActiveLocalMediaController_localMediaStopped_returnNull() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isNull();
    }
}
