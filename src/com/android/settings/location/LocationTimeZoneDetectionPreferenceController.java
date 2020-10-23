/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.location;

import static android.app.time.TimeZoneCapabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.TimeZoneCapabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.TimeZoneCapabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.TimeZoneCapabilities.CAPABILITY_POSSESSED;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.location.LocationManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.concurrent.Executor;

/**
 * The controller for the "location time zone detection" entry in the Location settings
 * screen.
 */
public class LocationTimeZoneDetectionPreferenceController
        extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, TimeManager.TimeZoneDetectorListener {

    private final TimeManager mTimeManager;
    private final LocationManager mLocationManager;
    private TimeZoneCapabilitiesAndConfig mTimeZoneCapabilitiesAndConfig;
    private Preference mPreference;

    public LocationTimeZoneDetectionPreferenceController(Context context, String key) {
        super(context, key);
        mTimeManager = context.getSystemService(TimeManager.class);
        mLocationManager = context.getSystemService(LocationManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        // Register for updates to the user's time zone capabilities or configuration which could
        // require UI changes.
        Executor mainExecutor = mContext.getMainExecutor();
        mTimeManager.addTimeZoneDetectorListener(mainExecutor, this);
        // Setup the initial state of the summary.
        refreshUi();
    }

    @Override
    public void onStop() {
        mTimeManager.removeTimeZoneDetectorListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        TimeZoneCapabilities timeZoneCapabilities =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ false).getCapabilities();
        int capability = timeZoneCapabilities.getConfigureGeoDetectionEnabledCapability();

        // The preference only has two states: present and not present. The preference is never
        // present but disabled.
        if (capability == CAPABILITY_NOT_SUPPORTED || capability == CAPABILITY_NOT_ALLOWED) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (capability == CAPABILITY_NOT_APPLICABLE || capability == CAPABILITY_POSSESSED) {
            return AVAILABLE;
        } else {
            throw new IllegalStateException("Unknown capability=" + capability);
        }
    }

    @Override
    public CharSequence getSummary() {
        TimeZoneCapabilitiesAndConfig timeZoneCapabilitiesAndConfig =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ false);
        TimeZoneCapabilities capabilities = timeZoneCapabilitiesAndConfig.getCapabilities();
        int configureGeoDetectionEnabledCapability =
                capabilities.getConfigureGeoDetectionEnabledCapability();
        TimeZoneConfiguration configuration = timeZoneCapabilitiesAndConfig.getConfiguration();

        int summaryResId;
        if (configureGeoDetectionEnabledCapability == CAPABILITY_NOT_SUPPORTED) {
            // The preference should not be visible, but text is referenced in case this changes.
            summaryResId = R.string.location_time_zone_detection_not_supported;
        } else if (configureGeoDetectionEnabledCapability == CAPABILITY_NOT_ALLOWED) {
            // The preference should not be visible, but text is referenced in case this changes.
            summaryResId = R.string.location_time_zone_detection_not_allowed;
        } else if (configureGeoDetectionEnabledCapability == CAPABILITY_NOT_APPLICABLE) {
            // The TimeZoneCapabilities deliberately doesn't provide information about why the user
            // doesn't have the capability, but the user's "location enabled" being off and the
            // global automatic detection setting will always be considered overriding reasons why
            // location time zone detection cannot be used.
            if (!mLocationManager.isLocationEnabled()) {
                summaryResId = R.string.location_app_permission_summary_location_off;
            } else if (!configuration.isAutoDetectionEnabled()) {
                summaryResId = R.string.location_time_zone_detection_auto_is_off;
            } else {
                // This is in case there are other reasons in future why location time zone
                // detection is not applicable.
                summaryResId = R.string.location_time_zone_detection_not_applicable;
            }
        } else if (configureGeoDetectionEnabledCapability == CAPABILITY_POSSESSED) {
            boolean isGeoDetectionEnabled = configuration.isGeoDetectionEnabled();
            summaryResId = isGeoDetectionEnabled
                    ? R.string.location_time_zone_detection_on
                    : R.string.location_time_zone_detection_off;
        } else {
            // This is unexpected: getAvailabilityStatus() should ensure that the UI element isn't
            // even shown for known cases, or the capability is unknown.
            throw new IllegalStateException("Unexpected configureGeoDetectionEnabledCapability="
                    + configureGeoDetectionEnabledCapability);
        }
        return mContext.getString(summaryResId);
    }

    @Override
    public void onChange() {
        refreshUi();
    }

    private void refreshUi() {
        // Force a refresh of cached user capabilities and config before refreshing the summary.
        getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ true);
        refreshSummary(mPreference);
    }

    /**
     * Returns the current user capabilities and configuration. {@code forceRefresh} can be {@code
     * true} to discard any cached copy.
     */
    private TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig(boolean forceRefresh) {
        if (forceRefresh || mTimeZoneCapabilitiesAndConfig == null) {
            mTimeZoneCapabilitiesAndConfig = mTimeManager.getTimeZoneCapabilitiesAndConfig();
        }
        return mTimeZoneCapabilitiesAndConfig;
    }
}
