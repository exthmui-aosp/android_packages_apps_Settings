/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class LocationEnablerTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private LocationEnabler.LocationModeChangeListener mListener;

    private Context mContext;
    private LocationEnabler mEnabler;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mEnabler = spy(new LocationEnabler(mContext, mListener, mLifecycle));
    }

    @Test
    public void onResume_shouldSetActiveAndRegisterListener() {
        mEnabler.onResume();

        verify(mContext).registerReceiver(eq(mEnabler.mReceiver),
                eq(mEnabler.INTENT_FILTER_LOCATION_MODE_CHANGED));
    }

    @Test
    public void onResume_shouldRefreshLocationMode() {
        mEnabler.onResume();

        verify(mEnabler).refreshLocationMode();
    }

    @Test
    public void onPause_shouldUnregisterListener() {
        mEnabler.onPause();

        verify(mContext).unregisterReceiver(mEnabler.mReceiver);
    }

    @Test
    public void onReceive_shouldRefreshLocationMode() {
        mEnabler.onResume();
        reset(mListener);
        mEnabler.mReceiver.onReceive(mContext, new Intent());

        verify(mListener).onLocationModeChanged(anyInt(), anyBoolean());
    }

    @Test
    public void isEnabled_locationOff_shouldReturnFalse() {
        assertThat(mEnabler.isEnabled(Settings.Secure.LOCATION_MODE_OFF)).isFalse();
    }

    @Test
    public void isEnabled_restricted_shouldReturnFalse() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);

        assertThat(mEnabler.isEnabled(Settings.Secure.LOCATION_MODE_OFF)).isFalse();
    }

    @Test
    public void isEnabled_locationONotRestricted_shouldReturnTrue() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);

        assertThat(mEnabler.isEnabled(Settings.Secure.LOCATION_MODE_BATTERY_SAVING)).isTrue();
    }

    @Test
    public void refreshLocationMode_shouldCallOnLocationModeChanged() {
        mEnabler.refreshLocationMode();

        verify(mListener).onLocationModeChanged(anyInt(), anyBoolean());
    }

    @Test
    public void setLocationMode_restricted_shouldSetCurrentMode() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_BATTERY_SAVING);

        mEnabler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);

        verify(mListener)
                .onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, true);
    }

    @Test
    public void setLocationMode_notRestricted_shouldUpdateSecureSettings() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_BATTERY_SAVING);

        mEnabler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_BATTERY_SAVING))
                .isEqualTo(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
    }

    @Test
    public void setLocationMode_notRestricted_shouldRefreshLocation() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_BATTERY_SAVING);

        mEnabler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);

        verify(mEnabler).refreshLocationMode();
    }

    @Test
    public void setLocationMode_notRestricted_shouldBroadcastUpdate() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_BATTERY_SAVING);

        mEnabler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);

        verify(mContext).sendBroadcast(argThat(actionMatches(mEnabler.MODE_CHANGING_ACTION)),
                eq(WRITE_SECURE_SETTINGS));
    }

    @Test
    public void isManagedProfileRestrictedByBase_notManagedProfile_shouldReturnFalse() {
        assertThat(mEnabler.isManagedProfileRestrictedByBase()).isFalse();
    }

    @Test
    public void isManagedProfileRestrictedByBase_notRestricted_shouldReturnFalse() {
        mockManagedProfile();
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());

        assertThat(mEnabler.isManagedProfileRestrictedByBase()).isFalse();
    }

    @Test
    public void isManagedProfileRestrictedByBase_hasManagedProfile_shouldReturnFalse() {
        mockManagedProfile();
        doReturn(true).when(mEnabler).hasShareLocationRestriction(anyInt());

        assertThat(mEnabler.isManagedProfileRestrictedByBase()).isTrue();
    }

    private void mockManagedProfile() {
        final List<UserHandle> userProfiles = new ArrayList<>();
        final UserHandle userHandle = mock(UserHandle.class);
        when(userHandle.getIdentifier()).thenReturn(5);
        userProfiles.add(userHandle);
        when(mUserManager.getUserProfiles()).thenReturn(userProfiles);
        when(mUserManager.getUserHandle()).thenReturn(1);
        when(mUserManager.getUserInfo(5))
                .thenReturn(new UserInfo(5, "user 5", UserInfo.FLAG_MANAGED_PROFILE));
    }

    private static ArgumentMatcher<Intent> actionMatches(String expected) {
        return intent -> TextUtils.equals(expected, intent.getAction());
    }
}
