/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.internal.os.BatterySipper;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Wraps the power usage data of a BatterySipper with information about package name
 * and icon image.
 */
public class BatteryEntry {
    public static final int MSG_UPDATE_NAME_ICON = 1;
    public static final int MSG_REPORT_FULLY_DRAWN = 2;

    private static final String TAG = "BatteryEntry";
    private static final String PACKAGE_SYSTEM = "android";

    static final HashMap<String,UidToDetail> sUidCache = new HashMap<String,UidToDetail>();

    static final ArrayList<BatteryEntry> mRequestQueue = new ArrayList<BatteryEntry>();
    static Handler sHandler;

    static Locale sCurrentLocale = null;

    static private class NameAndIconLoader extends Thread {
        private boolean mAbort = false;

        public NameAndIconLoader() {
            super("BatteryUsage Icon Loader");
        }

        public void abort() {
            mAbort = true;
        }

        @Override
        public void run() {
            while (true) {
                BatteryEntry be;
                synchronized (mRequestQueue) {
                    if (mRequestQueue.isEmpty() || mAbort) {
                        if (sHandler != null) {
                            sHandler.sendEmptyMessage(MSG_REPORT_FULLY_DRAWN);
                        }
                        mRequestQueue.clear();
                        return;
                    }
                    be = mRequestQueue.remove(0);
                }
                be.loadNameAndIcon();
            }
        }
    }

    private static NameAndIconLoader mRequestThread;

    public static void startRequestQueue() {
        if (sHandler != null) {
            synchronized (mRequestQueue) {
                if (!mRequestQueue.isEmpty()) {
                    if (mRequestThread != null) {
                        mRequestThread.abort();
                    }
                    mRequestThread = new NameAndIconLoader();
                    mRequestThread.setPriority(Thread.MIN_PRIORITY);
                    mRequestThread.start();
                    mRequestQueue.notify();
                }
            }
        }
    }

    public static void stopRequestQueue() {
        synchronized (mRequestQueue) {
            if (mRequestThread != null) {
                mRequestThread.abort();
                mRequestThread = null;
                sHandler = null;
            }
        }
    }

    public static void clearUidCache() {
        sUidCache.clear();
    }

    public final Context context;
    private final BatterySipper mSipper;
    private final BatteryConsumer mBatteryConsumer;

    public String name;
    public Drawable icon;
    public int iconId; // For passing to the detail screen.
    private String mDefaultPackageName;

    static class UidToDetail {
        String name;
        String packageName;
        Drawable icon;
    }

    public BatteryEntry(Context context, Handler handler, UserManager um, BatterySipper sipper,
            BatteryConsumer batteryConsumer, String packageName) {
        sHandler = handler;
        this.context = context;
        this.mSipper = sipper;
        this.mBatteryConsumer = batteryConsumer;
        this.mDefaultPackageName = packageName;

        if (batteryConsumer instanceof UidBatteryConsumer) {
            UidBatteryConsumer uidBatteryConsumer = (UidBatteryConsumer) batteryConsumer;
            int uid = uidBatteryConsumer.getUid();
            PackageManager pm = context.getPackageManager();
            if (mDefaultPackageName == null) {
                String[] packages = pm.getPackagesForUid(uid);
                // Apps should only have one package
                if (packages != null && packages.length == 1) {
                    mDefaultPackageName = packages[0];
                } else {
                    mDefaultPackageName = uidBatteryConsumer.getPackageWithHighestDrain();
                }
            }
            if (mDefaultPackageName != null) {
                try {
                    ApplicationInfo appInfo =
                            pm.getApplicationInfo(mDefaultPackageName, 0 /* no flags */);
                    name = pm.getApplicationLabel(appInfo).toString();
                } catch (NameNotFoundException e) {
                    Log.d(TAG, "PackageManager failed to retrieve ApplicationInfo for: "
                            + mDefaultPackageName);
                    name = mDefaultPackageName;
                }
            }
            getQuickNameIconForUid(uid);
            return;
        }

        switch (sipper.drainType) {
            case IDLE:
                name = context.getResources().getString(R.string.power_idle);
                iconId = R.drawable.ic_settings_phone_idle;
                break;
            case CELL:
                name = context.getResources().getString(R.string.power_cell);
                iconId = R.drawable.ic_cellular_1_bar;
                break;
            case PHONE:
                name = context.getResources().getString(R.string.power_phone);
                iconId = R.drawable.ic_settings_voice_calls;
                break;
            case WIFI:
                name = context.getResources().getString(R.string.power_wifi);
                iconId = R.drawable.ic_settings_wireless;
                break;
            case BLUETOOTH:
                name = context.getResources().getString(R.string.power_bluetooth);
                iconId = com.android.internal.R.drawable.ic_settings_bluetooth;
                break;
            case SCREEN:
                name = context.getResources().getString(R.string.power_screen);
                iconId = R.drawable.ic_settings_display;
                break;
            case FLASHLIGHT:
                name = context.getResources().getString(R.string.power_flashlight);
                iconId = R.drawable.ic_settings_display;
                break;
            case APP:
                PackageManager pm = context.getPackageManager();
                sipper.mPackages = pm.getPackagesForUid(sipper.uidObj.getUid());
                // Apps should only have one package
                if (sipper.mPackages == null || sipper.mPackages.length != 1) {
                    name = sipper.packageWithHighestDrain;
                } else {
                    mDefaultPackageName = pm.getPackagesForUid(sipper.uidObj.getUid())[0];
                    try {
                        ApplicationInfo appInfo =
                                pm.getApplicationInfo(mDefaultPackageName, 0 /* no flags */);
                        name = pm.getApplicationLabel(appInfo).toString();
                    } catch (NameNotFoundException e) {
                        Log.d(TAG, "PackageManager failed to retrieve ApplicationInfo for: "
                                + mDefaultPackageName);
                        name = mDefaultPackageName;
                    }
                }
                break;
            case USER: {
                UserInfo info = um.getUserInfo(sipper.userId);
                if (info != null) {
                    icon = Utils.getUserIcon(context, um, info);
                    name = Utils.getUserLabel(context, info);
                } else {
                    icon = null;
                    name = context.getResources().getString(
                            R.string.running_process_item_removed_user_label);
                }
            } break;
            case UNACCOUNTED:
                name = context.getResources().getString(R.string.power_unaccounted);
                iconId = R.drawable.ic_android;
                break;
            case OVERCOUNTED:
                name = context.getResources().getString(R.string.power_overcounted);
                iconId = R.drawable.ic_android;
                break;
            case CAMERA:
                name = context.getResources().getString(R.string.power_camera);
                iconId = R.drawable.ic_settings_camera;
                break;
            case AMBIENT_DISPLAY:
                name = context.getResources().getString(R.string.ambient_display_screen_title);
                iconId = R.drawable.ic_settings_aod;
                break;
        }
        if (iconId > 0) {
            icon = context.getDrawable(iconId);
        }
        if ((name == null || iconId == 0) && sipper.uidObj != null) {
            getQuickNameIconForUid(sipper.uidObj.getUid());
        }
    }

    public Drawable getIcon() {
        return icon;
    }

    /**
     * Gets the application name
     */
    public String getLabel() {
        return name;
    }

    void getQuickNameIconForUid(final int uid) {
        // Locale sync to system config in Settings
        final Locale locale = Locale.getDefault();
        if (sCurrentLocale != locale) {
            clearUidCache();
            sCurrentLocale = locale;
        }

        final String uidString = Integer.toString(uid);
        if (sUidCache.containsKey(uidString)) {
            UidToDetail utd = sUidCache.get(uidString);
            mDefaultPackageName = utd.packageName;
            name = utd.name;
            icon = utd.icon;
            return;
        }
        PackageManager pm = context.getPackageManager();
        icon = pm.getDefaultActivityIcon();
        if (pm.getPackagesForUid(uid) == null) {
            if (uid == 0) {
                name = context.getResources().getString(R.string.process_kernel_label);
            } else if ("mediaserver".equals(name)) {
                name = context.getResources().getString(R.string.process_mediaserver_label);
            } else if ("dex2oat".equals(name)) {
                name = context.getResources().getString(R.string.process_dex2oat_label);
            }
            iconId = R.drawable.ic_power_system;
            icon = context.getDrawable(iconId);
        }

        if (sHandler != null) {
            synchronized (mRequestQueue) {
                mRequestQueue.add(this);
            }
        }
    }

    /**
     * Loads the app label and icon image and stores into the cache.
     */
    public void loadNameAndIcon() {
        // Bail out if the current sipper is not an App sipper.
        if (mSipper.uidObj == null) {
            return;
        }

        PackageManager pm = context.getPackageManager();
        final int uid = mSipper.uidObj.getUid();
        if (mSipper.mPackages == null) {
            mSipper.mPackages = pm.getPackagesForUid(uid);
        }

        final String[] packages = extractPackagesFromSipper(mSipper);
        if (packages != null) {
            String[] packageLabels = new String[packages.length];
            System.arraycopy(packages, 0, packageLabels, 0, packages.length);

            // Convert package names to user-facing labels where possible
            IPackageManager ipm = AppGlobals.getPackageManager();
            final int userId = UserHandle.getUserId(uid);
            for (int i = 0; i < packageLabels.length; i++) {
                try {
                    final ApplicationInfo ai = ipm.getApplicationInfo(packageLabels[i],
                            0 /* no flags */, userId);
                    if (ai == null) {
                        Log.d(TAG, "Retrieving null app info for package "
                                + packageLabels[i] + ", user " + userId);
                        continue;
                    }
                    CharSequence label = ai.loadLabel(pm);
                    if (label != null) {
                        packageLabels[i] = label.toString();
                    }
                    if (ai.icon != 0) {
                        mDefaultPackageName = packages[i];
                        icon = ai.loadIcon(pm);
                        break;
                    }
                } catch (RemoteException e) {
                    Log.d(TAG, "Error while retrieving app info for package "
                            + packageLabels[i] + ", user " + userId, e);
                }
            }

            if (packageLabels.length == 1) {
                name = packageLabels[0];
            } else {
                // Look for an official name for this UID.
                for (String pkgName : packages) {
                    try {
                        final PackageInfo pi = ipm.getPackageInfo(pkgName, 0 /* no flags */, userId);
                        if (pi == null) {
                            Log.d(TAG, "Retrieving null package info for package "
                                    + pkgName + ", user " + userId);
                            continue;
                        }
                        if (pi.sharedUserLabel != 0) {
                            final CharSequence nm = pm.getText(pkgName,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                name = nm.toString();
                                if (pi.applicationInfo.icon != 0) {
                                    mDefaultPackageName = pkgName;
                                    icon = pi.applicationInfo.loadIcon(pm);
                                }
                                break;
                            }
                        }
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error while retrieving package info for package "
                                + pkgName + ", user " + userId, e);
                    }
                }
            }
        }

        final String uidString = Integer.toString(uid);
        if (name == null) {
            name = uidString;
        }

        if (icon == null) {
            icon = pm.getDefaultActivityIcon();
        }

        UidToDetail utd = new UidToDetail();
        utd.name = name;
        utd.icon = icon;
        utd.packageName = mDefaultPackageName;
        sUidCache.put(uidString, utd);
        if (sHandler != null) {
            sHandler.sendMessage(sHandler.obtainMessage(MSG_UPDATE_NAME_ICON, this));
        }
    }

    static String[] extractPackagesFromSipper(BatterySipper sipper) {
        // Only use system package if uid is system uid, so it could find a consistent name and icon
        return sipper.getUid() == Process.SYSTEM_UID
                ? new String[]{PACKAGE_SYSTEM}
                : sipper.mPackages;
    }

    /**
     * Returns true if this entry describes an app (UID)
     */
    public boolean isAppEntry() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return true;
        } else {
            return mSipper.drainType == BatterySipper.DrainType.APP;
        }
    }

    /**
     * Returns true if this entry describes a User.
     */
    public boolean isUserEntry() {
        if (mBatteryConsumer instanceof UserBatteryConsumer) {
            return true;
        } else {
            return mSipper.drainType == BatterySipper.DrainType.USER;
        }
    }

    /**
     * Returns the package name that should be used to represent the UID described
     * by this entry.
     */
    public String getDefaultPackageName() {
        if (mDefaultPackageName != null) {
            return mDefaultPackageName;
        }
        if (ArrayUtils.isEmpty(mSipper.mPackages)) {
            return null;
        } else {
            return mSipper.mPackages[0];
        }
    }

    /**
     * Returns the UID of the app described by this entry.
     */
    public int getUid() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return ((UidBatteryConsumer) mBatteryConsumer).getUid();
        } else if (mBatteryConsumer != null) {
            return Process.INVALID_UID;
        } else {
            return mSipper.getUid();
        }
    }

    /**
     * Returns foreground foreground time (in milliseconds) that is attributed to this entry.
     */
    public long getTimeInForegroundMs(BatteryUtils batteryUtils) {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return ((UidBatteryConsumer) mBatteryConsumer).getTimeInStateMs(
                    UidBatteryConsumer.STATE_FOREGROUND);
        } else if (mBatteryConsumer != null) {
            return mBatteryConsumer.getUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_USAGE);
        } else if (mSipper.drainType == BatterySipper.DrainType.APP) {
            return batteryUtils.getProcessTimeMs(
                    BatteryUtils.StatusType.FOREGROUND, mSipper.uidObj,
                    BatteryStats.STATS_SINCE_CHARGED);
        } else {
            return mSipper.usageTimeMs;
        }
    }

    /**
     * Returns background activity time (in milliseconds) that is attributed to this entry.
     */
    public long getTimeInBackgroundMs(BatteryUtils batteryUtils) {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return ((UidBatteryConsumer) mBatteryConsumer).getTimeInStateMs(
                    UidBatteryConsumer.STATE_BACKGROUND);
        } else if (mBatteryConsumer != null) {
            return 0;
        } else  if (mSipper.drainType == BatterySipper.DrainType.APP) {
            return batteryUtils.getProcessTimeMs(
                    BatteryUtils.StatusType.BACKGROUND, mSipper.uidObj,
                    BatteryStats.STATS_SINCE_CHARGED);
        } else {
            return 0;
        }
    }

    /**
     * Returns total amount of power (in milli-amp-hours) that is attributed to this entry.
     */
    public double getConsumedPower() {
        if (mBatteryConsumer != null) {
            return mBatteryConsumer.getConsumedPower();
        }
        return (int) mSipper.totalPowerMah;
    }
}
