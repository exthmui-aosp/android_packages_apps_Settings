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

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import static com.android.settings.notification.BubblePreferenceController.SYSTEM_WIDE_OFF;
import static com.android.settings.notification.BubblePreferenceController.SYSTEM_WIDE_ON;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class BubblePreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private FragmentManager mFragmentManager;

    private BubblePreferenceController mController;
    private BubblePreferenceController mAppPageController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        when(mFragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));
        mController = spy(new BubblePreferenceController(mContext, mFragmentManager, mBackend,
                false /* isAppPage */));
        mAppPageController = spy(new BubblePreferenceController(mContext, mFragmentManager,
                mBackend, true /* isAppPage */));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(RestrictedSwitchPreference.class));
        mController.onPreferenceChange(mock(RestrictedSwitchPreference.class), true);
    }

    @Test
    public void testIsAvailable_notIfAppBlocked() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class), null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfChannelBlocked() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channel_notIfAppOff() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsNotAvailable_ifOffGlobally_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null);
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfOffGlobally_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null);
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_app_evenIfOffGlobally() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mAppPageController.onResume(appRow, null, null, null);
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);

        assertTrue(mAppPageController.isAvailable());
    }

    @Test
    public void testIsAvailable_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_defaultChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(appRow, channel, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channelAppOff() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testUpdateState_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null,
                mock(RestrictedLockUtils.EnforcedAdmin.class));

        Preference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void testUpdateState_channelNotBlockable() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByCriticalDeviceFunction()).thenReturn(true);
        mController.onResume(appRow, channel, null, null);

        Preference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState_channel() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.canBubble()).thenReturn(true);
        mController.onResume(appRow, channel, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertTrue(pref.isChecked());

        when(channel.canBubble()).thenReturn(false);
        mController.onResume(appRow, channel, null, null);
        mController.updateState(pref);

        assertFalse(pref.isChecked());
    }

    @Test
    public void testUpdateState_app() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.label = "App!";
        appRow.allowBubbles = true;
        mController.onResume(appRow, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);
        assertTrue(pref.isChecked());

        appRow.allowBubbles = false;
        mController.onResume(appRow, null, null, null);

        mController.updateState(pref);
        assertFalse(pref.isChecked());

        assertNotNull(pref.getSummary());
        assertTrue(pref.getSummary().toString().contains(appRow.label));
    }

    @Test
    public void testUpdateState_app_offGlobally() {
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.label = "App!";
        appRow.allowBubbles = true;
        mController.onResume(appRow, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);
        assertFalse(pref.isChecked());
    }

    @Test
    public void testOnPreferenceChange_on_channel() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        channel.setAllowBubbles(false);
        mController.onResume(appRow, channel, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);
        assertTrue(channel.canBubble());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testOnPreferenceChange_off_channel() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_HIGH);
        channel.setAllowBubbles(true);
        mController.onResume(appRow, channel, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, false);
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
        assertFalse(channel.canBubble());
    }

    @Test
    public void testOnPreferenceChange_on_app() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = false;
        mController.onResume(appRow, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);

        assertTrue(appRow.allowBubbles);
        verify(mBackend, times(1)).setAllowBubbles(any(), anyInt(), eq(true));
    }

    @Test
    public void testOnPreferenceChange_off_app() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        mController.onResume(appRow, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, false);

        assertFalse(appRow.allowBubbles);
        verify(mBackend, times(1)).setAllowBubbles(any(), anyInt(), eq(false));
    }

    @Test
    public void testOnPreferenceChange_on_app_offGlobally() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = false;
        mController.onResume(appRow, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);

        assertFalse(appRow.allowBubbles);
        verify(mBackend, never()).setAllowBubbles(any(), anyInt(), eq(true));
        verify(mFragmentManager, times(1)).beginTransaction();
    }
}
