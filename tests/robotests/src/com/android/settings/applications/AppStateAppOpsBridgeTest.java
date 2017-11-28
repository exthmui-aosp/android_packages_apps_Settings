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

package com.android.settings.applications;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.wrapper.IPackageManagerWrapper;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public final class AppStateAppOpsBridgeTest {

    @Mock private Context mContext;
    @Mock private UserManager mUserManager;
    @Mock private IPackageManagerWrapper mPackageManagerService;
    @Mock private AppOpsManager mAppOpsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(mAppOpsManager);
    }

    @Test
    public void getPermissionInfo_nullPackageInfo_shouldNotCrash() throws RemoteException {
        when(mPackageManagerService.getPackageInfo(anyString(), anyInt(), anyInt()))
            .thenReturn(null);
        TestAppStateAppOpsBridge appStateAppOpsBridge = new TestAppStateAppOpsBridge();

        appStateAppOpsBridge.getPermissionInfo("pkg1", 1);
        // should not crash
    }

    private class TestAppStateAppOpsBridge extends AppStateAppOpsBridge {
        public TestAppStateAppOpsBridge() {
            super(mContext, null, null, AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
                new String[] {Manifest.permission.SYSTEM_ALERT_WINDOW},
                mPackageManagerService);
        }

        @Override
        protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        }
    }
}
