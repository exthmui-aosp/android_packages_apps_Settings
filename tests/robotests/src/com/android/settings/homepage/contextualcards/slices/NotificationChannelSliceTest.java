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

package com.android.settings.homepage.contextualcards.slices;

import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.util.ArrayMap;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.notification.NotificationBackend.NotificationsSentState;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class NotificationChannelSliceTest {
    private static final String APP_LABEL = "Example App";
    private static final int CHANNEL_COUNT = 3;
    private static final String CHANNEL_NAME_PREFIX = "channel";
    private static final int NOTIFICATION_COUNT =
            NotificationChannelSlice.MIN_NOTIFICATION_SENT_COUNT + 1;
    private static final String PACKAGE_NAME = "com.test.notification.channel.slice";
    private static final int UID = 2019;

    @Mock
    private NotificationBackend mNotificationBackend;
    private Context mContext;
    private IconCompat mIcon;
    private NotificationChannelSlice mNotificationChannelSlice;
    private ShadowPackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Shadow PackageManager to add mock package.
        mPackageManager = shadowOf(mContext.getPackageManager());

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mNotificationChannelSlice = spy(new NotificationChannelSlice(mContext));

        doReturn(UID).when(mNotificationChannelSlice).getApplicationUid(any(String.class));
        mIcon = IconCompat.createWithResource(mContext, R.drawable.ic_settings_24dp);
        doReturn(mIcon).when(mNotificationChannelSlice).getApplicationIcon(any(String.class));

        // Assign mock NotificationBackend to build notification related data.
        mNotificationChannelSlice.mNotificationBackend = mNotificationBackend;
    }

    @After
    public void tearDown() {
        mPackageManager.removePackage(PACKAGE_NAME);
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    @Config(shadows = ShadowRestrictedLockUtilsInternal.class)
    public void getSlice_hasSuggestedApp_shouldHaveNotificationChannelTitle() {
        addMockPackageToPackageManager(true /* isRecentlyInstalled */,
                ApplicationInfo.FLAG_INSTALLED);
        mockNotificationBackend(CHANNEL_COUNT, NOTIFICATION_COUNT, false /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.manage_app_notification, APP_LABEL));
    }

    @Test
    public void getSlice_noRecentlyInstalledApp_shouldHaveNoSuggestedAppTitle() {
        addMockPackageToPackageManager(false /* isRecentlyInstalled */,
                ApplicationInfo.FLAG_INSTALLED);
        mockNotificationBackend(CHANNEL_COUNT, NOTIFICATION_COUNT, false /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.no_suggested_app));
    }

    @Test
    public void getSlice_noMultiChannelApp_shouldHaveNoSuggestedAppTitle() {
        addMockPackageToPackageManager(true /* isRecentlyInstalled */,
                ApplicationInfo.FLAG_INSTALLED);
        mockNotificationBackend(1 /* channelCount */, NOTIFICATION_COUNT, false /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.no_suggested_app));
    }

    @Test
    public void getSlice_insufficientNotificationSentCount_shouldHaveNoSuggestedAppTitle() {
        addMockPackageToPackageManager(true /* isRecentlyInstalled */,
                ApplicationInfo.FLAG_INSTALLED);
        mockNotificationBackend(CHANNEL_COUNT, 1 /* notificationCount */, false /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.no_suggested_app));
    }

    @Test
    public void getSlice_isSystemApp_shouldHaveNoSuggestedAppTitle() {
        addMockPackageToPackageManager(true /* isRecentlyInstalled */, ApplicationInfo.FLAG_SYSTEM);
        mockNotificationBackend(CHANNEL_COUNT, NOTIFICATION_COUNT, false /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.no_suggested_app));
    }

    @Test
    public void getSlice_isNotificationBanned_shouldHaveNoSuggestedAppTitle() {
        addMockPackageToPackageManager(true /* isRecentlyInstalled */,
                ApplicationInfo.FLAG_INSTALLED);
        mockNotificationBackend(CHANNEL_COUNT, NOTIFICATION_COUNT, true /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.no_suggested_app));
    }

    @Test
    @Config(shadows = ShadowRestrictedLockUtilsInternal.class)
    public void getSlice_exceedDefaultRowCount_shouldOnlyShowDefaultRows() {
        addMockPackageToPackageManager(true /* isRecentlyInstalled */,
                ApplicationInfo.FLAG_INSTALLED);
        mockNotificationBackend(NotificationChannelSlice.DEFAULT_EXPANDED_ROW_COUNT * 2,
                NOTIFICATION_COUNT, false /* banned */);

        final Slice slice = mNotificationChannelSlice.getSlice();

        // Get the number of RowBuilders from Slice.
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();
        // The header of this slice is built by RowBuilder. Hence, the row count will contain it.
        assertThat(rows).isEqualTo(NotificationChannelSlice.DEFAULT_EXPANDED_ROW_COUNT + 1);
    }

    private void addMockPackageToPackageManager(boolean isRecentlyInstalled, int flags) {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.name = APP_LABEL;
        applicationInfo.uid = UID;
        applicationInfo.flags = flags;

        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_NAME;
        packageInfo.applicationInfo = applicationInfo;
        packageInfo.firstInstallTime = createAppInstallTime(isRecentlyInstalled);
        mPackageManager.addPackage(packageInfo);
    }

    private long createAppInstallTime(boolean isRecentlyInstalled) {
        if (isRecentlyInstalled) {
            return System.currentTimeMillis() - NotificationChannelSlice.DURATION_END_DAYS;
        }

        return System.currentTimeMillis();
    }

    private void mockNotificationBackend(int channelCount, int notificationCount, boolean banned) {
        final List<NotificationChannel> channels = buildNotificationChannel(channelCount);
        final AppRow appRow = buildAppRow(channelCount, notificationCount, banned);

        doReturn(buildNotificationChannelGroups(channels)).when(mNotificationBackend).getGroups(
                any(String.class), any(int.class));
        doReturn(appRow).when(mNotificationBackend).loadAppRow(any(Context.class),
                any(PackageManager.class), any(PackageInfo.class));
        doReturn(channelCount).when(mNotificationBackend).getChannelCount(
                any(String.class), any(int.class));
    }

    private AppRow buildAppRow(int channelCount, int sentCount, boolean banned) {
        final AppRow appRow = new AppRow();
        appRow.banned = banned;
        appRow.channelCount = channelCount;
        appRow.sentByApp = new NotificationsSentState();
        appRow.sentByApp.sentCount = sentCount;
        appRow.sentByChannel = buildNotificationSentStates(channelCount, sentCount);

        return appRow;
    }

    private List<NotificationChannel> buildNotificationChannel(int channelCount) {
        final List<NotificationChannel> channels = new ArrayList<>();
        for (int i = 0; i < channelCount; i++) {
            channels.add(new NotificationChannel(CHANNEL_NAME_PREFIX + i, CHANNEL_NAME_PREFIX + i,
                    IMPORTANCE_NONE));
        }

        return channels;
    }

    private ParceledListSlice<NotificationChannelGroup> buildNotificationChannelGroups(
            List<NotificationChannel> channels) {
        final NotificationChannelGroup notificationChannelGroup = new NotificationChannelGroup(
                "group", "group");
        notificationChannelGroup.setBlocked(false);
        notificationChannelGroup.setChannels(channels);

        return new ParceledListSlice(Arrays.asList(notificationChannelGroup));
    }

    private Map<String, NotificationsSentState> buildNotificationSentStates(int channelCount,
            int sentCount) {
        final Map<String, NotificationBackend.NotificationsSentState> states = new ArrayMap<>();
        for (int i = 0; i < channelCount; i++) {
            final NotificationsSentState state = new NotificationsSentState();
            state.sentCount = sentCount;
            states.put(CHANNEL_NAME_PREFIX + i, state);
        }

        return states;
    }
}