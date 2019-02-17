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

package com.android.settings.slices;

import android.content.IntentFilter;

/**
 * A collection of API making a PreferenceController "sliceable"
 */
public interface Sliceable {
    /**
     * @return an {@link IntentFilter} that includes all broadcasts which can affect the state of
     * this Setting.
     */
    default IntentFilter getIntentFilter() {
        return null;
    }

    /**
     * Determines if the controller should be used as a Slice.
     * <p>
     * Important criteria for a Slice are:
     * - Must be secure
     * - Must not be a privacy leak
     * - Must be understandable as a stand-alone Setting.
     * <p>
     * This does not guarantee the setting is available.
     *
     * @return {@code true} if the controller should be used externally as a Slice.
     */
    default boolean isSliceable() {
        return false;
    }

    /**
     * @return {@code true} if the setting update asynchronously.
     * <p>
     * For example, a Wifi controller would return true, because it needs to update the radio
     * and wait for it to turn on.
     */
    default boolean hasAsyncUpdate() {
        return false;
    }
}
