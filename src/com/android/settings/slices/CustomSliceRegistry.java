/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.provider.SettingsSlicesContract.KEY_LOCATION;
import static android.provider.SettingsSlicesContract.KEY_WIFI;

import static com.android.settings.notification.ZenModePreferenceController.ZEN_MODE_KEY;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settingslib.media.MediaOutputSliceConstants;

/**
 * A registry of custom slice Uris.
 */
public class CustomSliceRegistry {

    /**
     * Uri for Airplane mode Slice.
     */
    public static final Uri AIRPLANE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_AIRPLANE_MODE)
            .build();

    /**
     * Uri for Battery Fix Slice.
     */
    public static final Uri BATTERY_FIX_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath(BatteryTipPreferenceController.PREF_NAME)
            .build();
    /**
     * Backing Uri for the Battery info Slice.
     */
    public static final Uri BATTERY_INFO_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("battery_card")
            .build();
    /**
     * Backing Uri for the Bluetooth Slice.
     */
    public static final Uri BLUETOOTH_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_BLUETOOTH)
            .build();

    /**
     * Backing Uri for Bluetooth devices Slice.
     */
    public static final Uri BLUETOOTH_DEVICES_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("bluetooth_devices")
            .build();

    /**
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri CONTEXTUAL_WIFI_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("contextual_wifi")
            .build();

    /**
     * Backing Uri for the Data usage Slice.
     */
    public static final Uri DATA_USAGE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("data_usage_card")
            .build();
    /**
     * Backing Uri for the Device info Slice.
     */
    public static final Uri DEVICE_INFO_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("device_info_card")
            .build();
    /**
     * Backing Uri for the Emergency Info Slice.
     */
    public static final Uri EMERGENCY_INFO_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("emergency_info_card")
            .build();
    /**
     * Slice Uri for Enhanced 4G slice
     */
    public static final Uri ENHANCED_4G_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("enhanced_4g_lte")
            .build();
    /**
     * Backing Uri for the Flashlight Slice.
     */
    public static final Uri FLASHLIGHT_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("flashlight")
            .build();
    /**
     * Backing Uri for the Location Slice.
     */
    public static final Uri LOCATION_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(KEY_LOCATION)
            .build();
    /**
     * Backing Uri for Low storage Slice.
     */
    public static final Uri LOW_STORAGE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("low_storage")
            .build();
    /**
     * Backing Uri for NFC Slice
     */
    public static final Uri NFC_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("toggle_nfc")
            .build();
    /**
     * Backing Uri for Notification channel Slice.
     */
    public static final Uri NOTIFICATION_CHANNEL_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("notification_channel")
            .build();
    /**
     * Backing Uri for the storage slice.
     */
    public static final Uri STORAGE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("storage_card")
            .build();
    /**
     * Full {@link Uri} for the Alarm volume Slice.
     */
    public static final Uri VOLUME_ALARM_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("alarm_volume")
            .build();
    /**
     * Full {@link Uri} for the Call Volume Slice.
     */
    public static final Uri VOLUME_CALL_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("call_volume")
            .build();
    /**
     * Full {@link Uri} for the Media Volume Slice.
     */
    public static final Uri VOLUME_MEDIA_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("media_volume")
            .build();
    /**
     * Full {@link Uri} for the Ringer volume Slice.
     */
    public static final Uri VOLUME_RINGER_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("ring_volume")
            .build();

    /**
     * Full {@link Uri} for the Wifi Calling Slice.
     */
    public static final Uri WIFI_CALLING_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath(WifiCallingSliceHelper.PATH_WIFI_CALLING)
            .build();
    /**
     * Full {@link Uri} for the Wifi Calling Preference Slice.
     */
    public static final Uri WIFI_CALLING_PREFERENCE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(WifiCallingSliceHelper.PATH_WIFI_CALLING_PREFERENCE)
            .build();
    /**
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri WIFI_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(KEY_WIFI)
            .build();

    /**
     * Backing Uri for the Zen Mode Slice.
     */
    public static final Uri ZEN_MODE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(ZEN_MODE_KEY)
            .build();

    /**
     * Backing Uri for the Media output Slice.
     */
    public static Uri MEDIA_OUTPUT_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(MediaOutputSliceConstants.KEY_MEDIA_OUTPUT)
            .build();
}
