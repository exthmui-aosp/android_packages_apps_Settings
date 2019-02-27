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

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.volume.MediaSessions;

import java.util.List;

public class RemoteVolumePreferenceController extends
    VolumeSeekBarPreferenceController {

    private static final String KEY_REMOTE_VOLUME = "remote_volume";
    @VisibleForTesting
    static final int REMOTE_VOLUME = 100;

    private MediaSessionManager mMediaSessionManager;
    private MediaSessions mMediaSessions;
    private MediaSession.Token mActiveToken;

    private MediaSessions.Callbacks mCallbacks = new MediaSessions.Callbacks() {
        @Override
        public void onRemoteUpdate(MediaSession.Token token, String name,
                MediaController.PlaybackInfo pi) {
            mActiveToken = token;
            mPreference.setMax(pi.getMaxVolume());
            mPreference.setVisible(true);
            setSliderPosition(pi.getCurrentVolume());
        }

        @Override
        public void onRemoteRemoved(MediaSession.Token t) {
            if (mActiveToken == t) {
                mActiveToken = null;
                mPreference.setVisible(false);
            }
        }

        @Override
        public void onRemoteVolumeChanged(MediaSession.Token token, int flags) {
            if (mActiveToken == token) {
                final MediaController mediaController = new MediaController(mContext, token);
                final MediaController.PlaybackInfo pi = mediaController.getPlaybackInfo();
                setSliderPosition(pi.getCurrentVolume());
            }
        }
    };

    public RemoteVolumePreferenceController(Context context) {
        super(context, KEY_REMOTE_VOLUME);
        mMediaSessionManager = context.getSystemService(MediaSessionManager.class);
        mMediaSessions = new MediaSessions(context, Looper.getMainLooper(), mCallbacks);
    }

    @Override
    public int getAvailabilityStatus() {
        final List<MediaController> controllers = mMediaSessionManager.getActiveSessions(null);
        for (MediaController mediaController : controllers) {
            final MediaController.PlaybackInfo pi = mediaController.getPlaybackInfo();
            if (isRemote(pi)) {
                mActiveToken = mediaController.getSessionToken();
                return AVAILABLE;
            }
        }

        // No active remote media at this point
        return CONDITIONALLY_UNAVAILABLE;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        super.onResume();
        mMediaSessions.init();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        super.onPause();
        mMediaSessions.destroy();
    }

    @Override
    public int getSliderPosition() {
        if (mPreference != null) {
            return mPreference.getProgress();
        }
        //TODO(b/126199571): get it from media controller
        return 0;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (mPreference != null) {
            mPreference.setProgress(position);
        }
        //TODO(b/126199571): set it through media controller
        return false;
    }

    @Override
    public int getMaxSteps() {
        if (mPreference != null) {
            return mPreference.getMax();
        }
        //TODO(b/126199571): get it from media controller
        return 0;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_REMOTE_VOLUME);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REMOTE_VOLUME;
    }

    @Override
    public int getAudioStream() {
        // This can be anything because remote volume controller doesn't rely on it.
        return REMOTE_VOLUME;
    }

    @Override
    public int getMuteIcon() {
        return R.drawable.ic_volume_remote_mute;
    }

    public static boolean isRemote(MediaController.PlaybackInfo pi) {
        return pi != null
                && pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE;
    }
}
