/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

class AddSourceWaitForResponseState extends AudioStreamStateHandler {
    @VisibleForTesting
    static final int AUDIO_STREAM_ADD_SOURCE_WAIT_FOR_RESPONSE_STATE_SUMMARY =
            R.string.audio_streams_add_source_wait_for_response_summary;

    @VisibleForTesting static final int ADD_SOURCE_WAIT_FOR_RESPONSE_TIMEOUT_MILLIS = 20000;

    @Nullable private static AddSourceWaitForResponseState sInstance = null;

    private AddSourceWaitForResponseState() {}

    static AddSourceWaitForResponseState getInstance() {
        if (sInstance == null) {
            sInstance = new AddSourceWaitForResponseState();
        }
        return sInstance;
    }

    @Override
    void performAction(
            AudioStreamPreference preference,
            AudioStreamsProgressCategoryController controller,
            AudioStreamsHelper helper) {
        mHandler.removeCallbacksAndMessages(preference);
        var metadata = preference.getAudioStreamMetadata();
        if (metadata != null) {
            helper.addSource(metadata);
            // Cache the metadata that used for add source, if source is added successfully, we
            // will save it persistently.
            mAudioStreamsRepository.cacheMetadata(metadata);

            // It's possible that onSourceLost() is not notified even if the source is no longer
            // valid. When calling addSource() for a source that's already lost, no callback
            // will be sent back. So we remove the preference and pop up a dialog if it's state
            // has not been changed after waiting for a certain time.
            mHandler.postDelayed(
                    () -> {
                        if (preference.isShown()
                                && preference.getAudioStreamState() == getStateEnum()) {
                            controller.handleSourceFailedToConnect(
                                    preference.getAudioStreamBroadcastId());
                            ThreadUtils.postOnMainThread(
                                    () -> {
                                        if (controller.getFragment() != null) {
                                            AudioStreamsDialogFragment.show(
                                                    controller.getFragment(),
                                                    getBroadcastUnavailableNoRetryDialog(
                                                            preference.getContext(),
                                                            AudioStreamsHelper.getBroadcastName(
                                                                    metadata)));
                                        }
                                    });
                        }
                    },
                    preference,
                    ADD_SOURCE_WAIT_FOR_RESPONSE_TIMEOUT_MILLIS);
        }
    }

    @Override
    int getSummary() {
        return AUDIO_STREAM_ADD_SOURCE_WAIT_FOR_RESPONSE_STATE_SUMMARY;
    }

    @Override
    AudioStreamsProgressCategoryController.AudioStreamState getStateEnum() {
        return AudioStreamsProgressCategoryController.AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE;
    }

    private AudioStreamsDialogFragment.DialogBuilder getBroadcastUnavailableNoRetryDialog(
            Context context, String broadcastName) {
        return new AudioStreamsDialogFragment.DialogBuilder(context)
                .setTitle(context.getString(R.string.audio_streams_dialog_stream_is_not_available))
                .setSubTitle1(broadcastName)
                .setSubTitle2(context.getString(R.string.audio_streams_is_not_playing))
                .setRightButtonText(context.getString(R.string.audio_streams_dialog_close))
                .setRightButtonOnClickListener(AlertDialog::dismiss);
    }
}
