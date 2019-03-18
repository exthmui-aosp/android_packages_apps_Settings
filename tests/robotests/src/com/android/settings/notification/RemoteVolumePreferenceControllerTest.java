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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.session.ISessionController;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RemoteVolumePreferenceControllerTest {
    private static final int CURRENT_POS = 5;
    private static final int MAX_POS = 10;

    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;
    @Mock
    private ISessionController mStub;
    @Mock
    private ISessionController mStub2;
    private MediaSession.Token mToken;
    private MediaSession.Token mToken2;
    private RemoteVolumePreferenceController mController;
    private Context mContext;
    private List<MediaController> mActiveSessions;
    private MediaController.PlaybackInfo mPlaybackInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);
        mActiveSessions = new ArrayList<>();
        mActiveSessions.add(mMediaController);
        when(mMediaSessionManager.getActiveSessions(null)).thenReturn(
                mActiveSessions);
        mToken = new MediaSession.Token(mStub);
        mToken2 = new MediaSession.Token(mStub2);

        mController = new RemoteVolumePreferenceController(mContext);
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE, 0, MAX_POS, CURRENT_POS, null);
        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
    }

    @Test
    public void isAvailable_containRemoteMedia_returnTrue() {
        when(mMediaController.getPlaybackInfo()).thenReturn(
                new MediaController.PlaybackInfo(MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                        0, 0, 0, null));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noRemoteMedia_returnFalse() {
        when(mMediaController.getPlaybackInfo()).thenReturn(
                new MediaController.PlaybackInfo(MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                        0, 0, 0, null));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getMuteIcon_returnMuteIcon() {
        assertThat(mController.getMuteIcon()).isEqualTo(R.drawable.ic_volume_remote_mute);
    }

    @Test
    public void getAudioStream_returnRemoteVolume() {
        assertThat(mController.getAudioStream()).isEqualTo(
                RemoteVolumePreferenceController.REMOTE_VOLUME);
    }

    @Test
    public void getSliderPosition_controllerNull_returnZero() {
        mController.mMediaController = null;

        assertThat(mController.getSliderPosition()).isEqualTo(0);
    }

    @Test
    public void getSliderPosition_controllerExists_returnValue() {
        mController.mMediaController = mMediaController;

        assertThat(mController.getSliderPosition()).isEqualTo(CURRENT_POS);
    }

    @Test
    public void getMaxSteps_controllerNull_returnZero() {
        mController.mMediaController = null;

        assertThat(mController.getMaxSteps()).isEqualTo(0);
    }

    @Test
    public void getMaxSteps_controllerExists_returnValue() {
        mController.mMediaController = mMediaController;

        assertThat(mController.getMaxSteps()).isEqualTo(MAX_POS);
    }

    @Test
    public void setSliderPosition_controllerNull_returnFalse() {
        mController.mMediaController = null;

        assertThat(mController.setSliderPosition(CURRENT_POS)).isFalse();
    }

    @Test
    public void setSliderPosition_controllerExists_returnTrue() {
        mController.mMediaController = mMediaController;

        assertThat(mController.setSliderPosition(CURRENT_POS)).isTrue();
        verify(mMediaController).setVolumeTo(CURRENT_POS, 0 /* flags */);
    }

    @Test
    public void onRemoteUpdate_firstToken_updateTokenAndPreference() {
        mController.mPreference = new VolumeSeekBarPreference(mContext);
        mController.mActiveToken = null;

        mController.mCallbacks.onRemoteUpdate(mToken, "token", mPlaybackInfo);

        assertThat(mController.mActiveToken).isEqualTo(mToken);
        assertThat(mController.mPreference.isVisible()).isTrue();
        assertThat(mController.mPreference.getMax()).isEqualTo(MAX_POS);
        assertThat(mController.mPreference.getProgress()).isEqualTo(CURRENT_POS);
    }

    @Test
    public void onRemoteUpdate_differentToken_doNothing() {
        mController.mActiveToken = mToken;

        mController.mCallbacks.onRemoteUpdate(mToken2, "token2", mPlaybackInfo);

        assertThat(mController.mActiveToken).isEqualTo(mToken);
    }

    @Test
    public void onRemoteRemoved_tokenRemoved_setInvisible() {
        mController.mPreference = new VolumeSeekBarPreference(mContext);
        mController.mActiveToken = mToken;

        mController.mCallbacks.onRemoteRemoved(mToken);

        assertThat(mController.mActiveToken).isNull();
        assertThat(mController.mPreference.isVisible()).isFalse();
    }

    @Test
    public void onRemoteVolumeChanged_volumeChanged_updateIt() {
        mController.mPreference = new VolumeSeekBarPreference(mContext);
        mController.mPreference.setMax(MAX_POS);
        mController.mActiveToken = mToken;
        mController.mMediaController = mMediaController;

        mController.mCallbacks.onRemoteVolumeChanged(mToken, 0 /* flags */);

        assertThat(mController.mPreference.getProgress()).isEqualTo(CURRENT_POS);
    }
}
