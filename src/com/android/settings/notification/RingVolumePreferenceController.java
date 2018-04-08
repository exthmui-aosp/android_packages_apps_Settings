/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.Objects;

public class RingVolumePreferenceController extends VolumeSeekBarPreferenceController {

    private static final String TAG = "RingVolumeController";
    private static final String KEY_RING_VOLUME = "ring_volume";

    private Vibrator mVibrator;
    private int mRingerMode = -1;
    private ComponentName mSuppressor;
    private final RingReceiver mReceiver = new RingReceiver();
    private final H mHandler = new H();

    public RingVolumePreferenceController(Context context) {
        this(context, KEY_RING_VOLUME);
    }

    public RingVolumePreferenceController(Context context, String key) {
        super(context, key);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
        updateRingerMode();
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver.register(true);
        updateEffectsSuppressor();
        updatePreferenceIcon();
    }

    @Override
    public void onPause() {
        super.onPause();
        mReceiver.register(false);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RING_VOLUME;
    }

    @Override
    public int getAvailabilityStatus() {
        return Utils.isVoiceCapable(mContext) && !mHelper.isSingleVolume()
                ? AVAILABLE : DISABLED_UNSUPPORTED;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_RING;
    }

    @Override
    public int getMuteIcon() {
        return R.drawable.ic_volume_ringer_vibrate;
    }

    private void updateRingerMode() {
        final int ringerMode = mHelper.getRingerModeInternal();
        if (mRingerMode == ringerMode) return;
        mRingerMode = ringerMode;
        updatePreferenceIcon();
    }

    private boolean wasRingerModeVibrate() {
        return mVibrator != null && mRingerMode == AudioManager.RINGER_MODE_SILENT
            && mHelper.getLastAudibleStreamVolume(getAudioStream()) == 0;
    }

    private void updateEffectsSuppressor() {
        final ComponentName suppressor = NotificationManager.from(mContext).getEffectsSuppressor();
        if (Objects.equals(suppressor, mSuppressor)) return;
        mSuppressor = suppressor;
        if (mPreference != null) {
            final String text = SuppressorHelper.getSuppressionText(mContext, suppressor);
            mPreference.setSuppressionText(text);
        }
        updatePreferenceIcon();
    }

    private void updatePreferenceIcon() {
        if (mPreference != null) {
            mPreference.showIcon(mSuppressor != null
                ? com.android.internal.R.drawable.ic_audio_ring_notif_mute
                : mRingerMode == AudioManager.RINGER_MODE_VIBRATE || wasRingerModeVibrate()
                    ? com.android.internal.R.drawable.ic_audio_ring_notif_vibrate
                    : com.android.internal.R.drawable.ic_audio_ring_notif);
        }
    }

    private final class H extends Handler {
        private static final int UPDATE_EFFECTS_SUPPRESSOR = 1;
        private static final int UPDATE_RINGER_MODE = 2;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_EFFECTS_SUPPRESSOR:
                    updateEffectsSuppressor();
                    break;
                case UPDATE_RINGER_MODE:
                    updateRingerMode();
                    break;
            }
        }
    }

    private class RingReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
            mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_EFFECTS_SUPPRESSOR);
            } else if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_RINGER_MODE);
            }
        }
    }

}
