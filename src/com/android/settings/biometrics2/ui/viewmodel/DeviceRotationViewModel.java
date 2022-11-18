/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.viewmodel;

import static android.hardware.display.DisplayManager.DisplayListener;

import android.app.Application;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class DeviceRotationViewModel extends AndroidViewModel {

    private static final boolean DEBUG = false;
    private static final String TAG = "RotationViewModel";

    private final DisplayManager mDisplayManager;
    @NonNull private final DisplayInfo mDisplayInfo = new DisplayInfo();
    private final DisplayListener mDisplayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            final int rotation = getRotation();
            if (DEBUG) {
                Log.d(TAG, "onDisplayChanged(" + displayId + "), rotation:" + rotation);
            }
            mLiveData.postValue(rotation);
        }
    };

    @NonNull private final MutableLiveData<Integer> mLiveData =
            new MutableLiveData<>(getRotation());

    public DeviceRotationViewModel(@NonNull Application application) {
        super(application);
        mDisplayManager = application.getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(mDisplayListener,
                application.getMainThreadHandler());
    }

    /**
     * Returns current rotation
     */
    @Surface.Rotation
    private int getRotation() {
        getApplication().getDisplay().getDisplayInfo(mDisplayInfo);
        return mDisplayInfo.rotation;
    }

    /**
     * Returns RotationLiveData
     */
    public LiveData<Integer> getLiveData() {
        return mLiveData;
    }

    @Override
    protected void onCleared() {
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
        super.onCleared();
    }
}
