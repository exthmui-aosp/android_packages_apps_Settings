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

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_DEFAULT;
import static android.app.AppOpsManager.MODE_ERRORED;
import static android.app.AppOpsManager.OP_REQUEST_INSTALL_PACKAGES;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@Suppress
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExternalSourcesSettingsTest {

    private static final String TAG = ExternalSourcesSettingsTest.class.getSimpleName();
    private static final String WM_DISMISS_KEYGUARD_COMMAND = "wm dismiss-keyguard";
    private static final long START_ACTIVITY_TIMEOUT = 5000;

    private Context mContext;
    private UiDevice mUiDevice;
    private PackageManager mPackageManager;
    private AppOpsManager mAppOpsManager;
    private List<UserInfo> mProfiles;
    private String mPackageName;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        mPackageName = InstrumentationRegistry.getContext().getPackageName();
        mPackageManager = mContext.getPackageManager();
        mAppOpsManager = mContext.getSystemService(AppOpsManager.class);
        mProfiles = mContext.getSystemService(UserManager.class).getProfiles(UserHandle.myUserId());
        resetAppOpModeForAllProfiles();
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mUiDevice.wakeUp();
        mUiDevice.executeShellCommand(WM_DISMISS_KEYGUARD_COMMAND);
    }

    private void resetAppOpModeForAllProfiles() throws Exception {
        for (UserInfo user : mProfiles) {
            final int uid = mPackageManager.getPackageUidAsUser(mPackageName, user.id);
            mAppOpsManager.setMode(OP_REQUEST_INSTALL_PACKAGES, uid, mPackageName, MODE_DEFAULT);
        }
    }

    private Intent createManageExternalSourcesListIntent() {
        final Intent manageExternalSourcesIntent = new Intent();
        manageExternalSourcesIntent.setAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        return manageExternalSourcesIntent;
    }

    private Intent createManageExternalSourcesAppIntent(String packageName) {
        final Intent intent = createManageExternalSourcesListIntent();
        intent.setData(Uri.parse("package:" + packageName));
        return intent;
    }

    private String getApplicationLabel(String packageName) throws Exception {
        final ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
        return mPackageManager.getApplicationLabel(info).toString();
    }

    private UiObject2 findAndVerifySwitchState(boolean checked) {
        final BySelector switchSelector = By.clazz(Switch.class).res("android:id/switch_widget");
        final UiObject2 switchPref = mUiDevice.wait(Until.findObject(switchSelector),
                START_ACTIVITY_TIMEOUT);
        assertNotNull("Switch not shown", switchPref);
        assertTrue("Switch in invalid state", switchPref.isChecked() == checked);
        return switchPref;
    }

    @Test
    public void testManageExternalSourcesList() throws Exception {
        final String testAppLabel = getApplicationLabel(mPackageName);

        mContext.startActivity(createManageExternalSourcesListIntent());
        final BySelector preferenceListSelector = By.clazz(ListView.class).res("android:id/list");
        final UiObject2 preferenceList = mUiDevice.wait(Until.findObject(preferenceListSelector),
                START_ACTIVITY_TIMEOUT);
        assertNotNull("App list not shown", preferenceList);

        final BySelector appLabelTextViewSelector = By.clazz(TextView.class)
                .res("android:id/title")
                .text(testAppLabel);
        List<UiObject2> listOfMatchingTextViews;
        do {
            listOfMatchingTextViews = preferenceList.findObjects(appLabelTextViewSelector);
            // assuming the number of profiles will be sufficiently small so that all the entries
            // for the same package will fit in one screen at some time during the scroll.
        } while (listOfMatchingTextViews.size() != mProfiles.size() &&
                preferenceList.scroll(Direction.DOWN, 0.2f));
        assertEquals("Test app not listed for each profile", mProfiles.size(),
                listOfMatchingTextViews.size());

        for (UiObject2 matchingObject : listOfMatchingTextViews) {
            matchingObject.click();
            findAndVerifySwitchState(true);
            mUiDevice.pressBack();
        }
    }

    private void testAppDetailScreenForAppOp(int appOpMode, int userId) throws Exception {
        final String testAppLabel = getApplicationLabel(mPackageName);
        final BySelector appDetailTitleSelector = By.clazz(TextView.class)
                .res("com.android.settings:id/app_detail_title")
                .text(testAppLabel);

        mAppOpsManager.setMode(OP_REQUEST_INSTALL_PACKAGES,
                mPackageManager.getPackageUidAsUser(mPackageName, userId), mPackageName, appOpMode);
        mContext.startActivityAsUser(createManageExternalSourcesAppIntent(mPackageName),
                UserHandle.of(userId));
        mUiDevice.wait(Until.findObject(appDetailTitleSelector), START_ACTIVITY_TIMEOUT);
        findAndVerifySwitchState(appOpMode == MODE_ALLOWED || appOpMode == MODE_DEFAULT);
        mUiDevice.pressBack();
    }

    @Test
    public void testManageExternalSourcesForApp() throws Exception {
        // App op MODE_DEFAULT is already tested in #testManageExternalSourcesList
        for (UserInfo user : mProfiles) {
            testAppDetailScreenForAppOp(MODE_ALLOWED, user.id);
            testAppDetailScreenForAppOp(MODE_ERRORED, user.id);
        }
    }

    private void testSwitchToggle(int fromAppOp, int toAppOp) throws Exception {
        final int packageUid = mPackageManager.getPackageUid(mPackageName, 0);
        final boolean initialState = (fromAppOp == MODE_ALLOWED || fromAppOp == MODE_DEFAULT);

        mAppOpsManager.setMode(OP_REQUEST_INSTALL_PACKAGES, packageUid, mPackageName, fromAppOp);
        mContext.startActivity(createManageExternalSourcesAppIntent(mPackageName));
        final UiObject2 switchPref = findAndVerifySwitchState(initialState);
        switchPref.click();
        Thread.sleep(1000);
        assertEquals("Toggling switch did not change app op", toAppOp,
                mAppOpsManager.checkOpNoThrow(OP_REQUEST_INSTALL_PACKAGES, packageUid,
                        mPackageName));
        mUiDevice.pressBack();
    }

    @Test
    public void testIfSwitchTogglesAppOp() throws Exception {
        testSwitchToggle(MODE_ALLOWED, MODE_ERRORED);
        testSwitchToggle(MODE_ERRORED, MODE_ALLOWED);
    }

    @After
    public void tearDown() throws Exception {
        mUiDevice.pressHome();
        resetAppOpModeForAllProfiles();
    }
}
