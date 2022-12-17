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
package com.android.settings.network.telephony;

import android.content.Context;
import android.provider.DeviceConfig;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Preference controller for "Allow Null Algorithms"
 *
 * <p>This preference controller is toggling null algorithms is not a per-sim operation.
 */
public class NullAlgorithmsPreferenceController extends TelephonyTogglePreferenceController {

    // log tags have a char limit of 24 so we can't use the class name
    private static final String LOG_TAG = "NullAlgosController";

    private TelephonyManager mTelephonyManager;

    /**
     * Class constructor of "Allow Null Algorithms" toggle.
     *
     * @param context of settings
     * @param key     assigned within UI entry of XML file
     */
    public NullAlgorithmsPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
    }

    /**
     * Get the {@link com.android.settings.core.BasePreferenceController.AvailabilityStatus} for
     * this preference given a {@code subId}. This dictates whether the setting is available on
     * the device, and if it is not, offers some context as to why.
     */
    @Override
    public int getAvailabilityStatus(int subId) {
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG,
                    "Telephony manager not yet initialized. Marking availability as "
                            + "CONDITIONALLY_UNAVAILABLE");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (!DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_CELLULAR_SECURITY,
                TelephonyManager.PROPERTY_ENABLE_NULL_CIPHER_TOGGLE, false)) {
            Log.i(LOG_TAG, "Null cipher toggle is disabled by DeviceConfig");
            return CONDITIONALLY_UNAVAILABLE;
        }

        try {
            mTelephonyManager.isNullCipherAndIntegrityPreferenceEnabled();
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Null cipher enablement is unsupported: " + e.getMessage());
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    /**
     * Return {@code true} if null algorithms are currently allowed.
     *
     * <p><b>NOTE:</b> This method returns the active state of the preference controller and is not
     * the parameter passed into {@link #setChecked(boolean)}, which is instead the requested future
     * state.
     */
    @Override
    public boolean isChecked() {
        return mTelephonyManager.isNullCipherAndIntegrityPreferenceEnabled();
    }

    /**
     * Called when a user preference changes on the toggle. We pass this info on to the Telephony
     * Framework so that the modem can be updated with the user's preference.
     *
     * <p>See {@link com.android.settings.core.TogglePreferenceController#setChecked(boolean)} for
     * details.
     *
     * @param isChecked The toggle value that we're being requested to enforce. A value of {@code
     *                  false} denotes that null ciphers will be disabled by the modem after this
     *                  function
     *                  completes, if it is not already.
     */
    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            Log.i(LOG_TAG, "Enabling null algorithms");
        } else {
            Log.i(LOG_TAG, "Disabling null algorithms");
        }
        mTelephonyManager.setNullCipherAndIntegrityEnabled(isChecked);
        return true;
    }
}
