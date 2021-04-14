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
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemBatteryConsumer;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

/**
 * Wraps the power usage data of a BatterySipper with information about package name
 * and icon image.
 */
public class BatteryEntry {

    public static final class NameAndIcon {
        public final String name;
        public final String packageName;
        public final Drawable icon;
        public final int iconId;

        public NameAndIcon(String name, Drawable icon, int iconId) {
            this(name, /*packageName=*/ null, icon, iconId);
        }

        public NameAndIcon(
                String name, String packageName, Drawable icon, int iconId) {
            this.name = name;
            this.icon = icon;
            this.iconId = iconId;
            this.packageName = packageName;
        }
    }

    public static final int MSG_UPDATE_NAME_ICON = 1;
    public static final int MSG_REPORT_FULLY_DRAWN = 2;

    private static final String TAG = "BatteryEntry";
    private static final String PACKAGE_SYSTEM = "android";

    static final HashMap<String, UidToDetail> sUidCache = new HashMap<>();

    static final ArrayList<BatteryEntry> sRequestQueue = new ArrayList<BatteryEntry>();
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
                synchronized (sRequestQueue) {
                    if (sRequestQueue.isEmpty() || mAbort) {
                        if (sHandler != null) {
                            sHandler.sendEmptyMessage(MSG_REPORT_FULLY_DRAWN);
                        }
                        return;
                    }
                    be = sRequestQueue.remove(0);
                }
                final NameAndIcon nameAndIcon =
                    BatteryEntry.loadNameAndIcon(
                        be.mContext, be.getUid(), sHandler, be, be.mDefaultPackageName);
                if (nameAndIcon != null) {
                    be.icon = getNonNull(be.icon, nameAndIcon.icon);
                    be.name = getNonNull(be.name, nameAndIcon.name);
                    be.mDefaultPackageName = getNonNull(
                        be.mDefaultPackageName, nameAndIcon.packageName);
                }
            }
        }
    }

    private static NameAndIconLoader mRequestThread;

    public static void startRequestQueue() {
        if (sHandler != null) {
            synchronized (sRequestQueue) {
                if (!sRequestQueue.isEmpty()) {
                    if (mRequestThread != null) {
                        mRequestThread.abort();
                    }
                    mRequestThread = new NameAndIconLoader();
                    mRequestThread.setPriority(Thread.MIN_PRIORITY);
                    mRequestThread.start();
                    sRequestQueue.notify();
                }
            }
        }
    }

    public static void stopRequestQueue() {
        synchronized (sRequestQueue) {
            if (mRequestThread != null) {
                mRequestThread.abort();
                mRequestThread = null;
                sRequestQueue.clear();
                sHandler = null;
            }
        }
    }

    public static void clearUidCache() {
        sUidCache.clear();
    }

    public static final Comparator<BatteryEntry> COMPARATOR =
            (a, b) -> Double.compare(b.getConsumedPower(), a.getConsumedPower());

    private final Context mContext;
    private final BatteryConsumer mBatteryConsumer;
    private final boolean mIsHidden;

    public String name;
    public Drawable icon;
    public int iconId; // For passing to the detail screen.
    public double percent;
    private String mDefaultPackageName;
    private double mConsumedPower;

    static class UidToDetail {
        String name;
        String packageName;
        Drawable icon;
    }

    public BatteryEntry(Context context, Handler handler, UserManager um,
            @NonNull BatteryConsumer batteryConsumer, boolean isHidden, String[] packages,
            String packageName) {
        sHandler = handler;
        mContext = context;
        mBatteryConsumer = batteryConsumer;
        mIsHidden = isHidden;
        mDefaultPackageName = packageName;

        if (batteryConsumer instanceof UidBatteryConsumer) {
            mConsumedPower = batteryConsumer.getConsumedPower();

            UidBatteryConsumer uidBatteryConsumer = (UidBatteryConsumer) batteryConsumer;
            int uid = uidBatteryConsumer.getUid();
            if (mDefaultPackageName == null) {
                // Apps should only have one package
                if (packages != null && packages.length == 1) {
                    mDefaultPackageName = packages[0];
                } else {
                    mDefaultPackageName = uidBatteryConsumer.getPackageWithHighestDrain();
                }
            }
            if (mDefaultPackageName != null) {
                PackageManager pm = context.getPackageManager();
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
            getQuickNameIconForUid(uid, packages);
            return;
        } else if (batteryConsumer instanceof SystemBatteryConsumer) {
            mConsumedPower = batteryConsumer.getConsumedPower()
                    - ((SystemBatteryConsumer) batteryConsumer).getPowerConsumedByApps();
            final NameAndIcon nameAndIcon = getNameAndIconFromDrainType(
                    context, ((SystemBatteryConsumer) batteryConsumer).getDrainType());
            iconId = nameAndIcon.iconId;
            name = nameAndIcon.name;
        } else if (batteryConsumer instanceof UserBatteryConsumer) {
            mConsumedPower = batteryConsumer.getConsumedPower();
            final NameAndIcon nameAndIcon = getNameAndIconFromUserId(
                    context, ((UserBatteryConsumer) batteryConsumer).getUserId());
            icon = nameAndIcon.icon;
            name = nameAndIcon.name;
        }

        if (iconId != 0) {
            icon = context.getDrawable(iconId);
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

    void getQuickNameIconForUid(final int uid, final String[] packages) {
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

        if (packages == null || packages.length == 0) {
            final NameAndIcon nameAndIcon = getNameAndIconFromUid(mContext, name, uid);
            icon = nameAndIcon.icon;
            name = nameAndIcon.name;
        } else {
            icon = mContext.getPackageManager().getDefaultActivityIcon();
        }

        if (sHandler != null) {
            synchronized (sRequestQueue) {
                sRequestQueue.add(this);
            }
        }
    }

    /**
     * Loads the app label and icon image and stores into the cache.
     */
    public static NameAndIcon loadNameAndIcon(
            Context context,
            int uid,
            Handler handler,
            BatteryEntry batteryEntry,
            String defaultPackageName) {
        String name = null;
        Drawable icon = null;
        // Bail out if the current sipper is not an App sipper.
        if (uid == 0 || uid == Process.INVALID_UID) {
            return null;
        }

        final PackageManager pm = context.getPackageManager();
        final String[] packages;
        if (uid == Process.SYSTEM_UID) {
            packages = new String[] {PACKAGE_SYSTEM};
        } else {
            packages = pm.getPackagesForUid(uid);
        }

        if (packages != null) {
            final String[] packageLabels = new String[packages.length];
            System.arraycopy(packages, 0, packageLabels, 0, packages.length);

            // Convert package names to user-facing labels where possible
            final IPackageManager ipm = AppGlobals.getPackageManager();
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
                    final CharSequence label = ai.loadLabel(pm);
                    if (label != null) {
                        packageLabels[i] = label.toString();
                    }
                    if (ai.icon != 0) {
                        defaultPackageName = packages[i];
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
                                    defaultPackageName = pkgName;
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
        utd.packageName = defaultPackageName;

        sUidCache.put(uidString, utd);
        if (handler != null) {
            handler.sendMessage(sHandler.obtainMessage(MSG_UPDATE_NAME_ICON, batteryEntry));
        }
        return new NameAndIcon(name, defaultPackageName, icon, /*iconId=*/ 0);
    }

    /**
     * Returns a string that uniquely identifies this battery consumer.
     */
    public String getKey() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return Integer.toString(((UidBatteryConsumer) mBatteryConsumer).getUid());
        } else if (mBatteryConsumer instanceof SystemBatteryConsumer) {
            return "S|" + ((SystemBatteryConsumer) mBatteryConsumer).getDrainType();
        } else if (mBatteryConsumer instanceof UserBatteryConsumer) {
            return "U|" + ((UserBatteryConsumer) mBatteryConsumer).getUserId();
        } else {
            Log.w(TAG, "Unsupported BatteryConsumer: " + mBatteryConsumer);
            return "";
        }
    }

    /**
     * Returns true if the entry is hidden from the battery usage summary list.
     */
    public boolean isHidden() {
        return mIsHidden;
    }

    /**
     * Returns true if this entry describes an app (UID)
     */
    public boolean isAppEntry() {
        return mBatteryConsumer instanceof UidBatteryConsumer;
    }

    /**
     * Returns true if this entry describes a User.
     */
    public boolean isUserEntry() {
        if (mBatteryConsumer instanceof UserBatteryConsumer) {
            return true;
        }
        return false;
    }

    /**
     * Returns the package name that should be used to represent the UID described
     * by this entry.
     */
    public String getDefaultPackageName() {
        return mDefaultPackageName;
    }

    /**
     * Returns the UID of the app described by this entry.
     */
    public int getUid() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return ((UidBatteryConsumer) mBatteryConsumer).getUid();
        } else {
            return Process.INVALID_UID;
        }
    }

    /**
     * Returns the BatteryConsumer of the app described by this entry.
     */
    public BatteryConsumer getBatteryConsumer() {
        return mBatteryConsumer;
    }

    /**
     * Returns foreground foreground time (in milliseconds) that is attributed to this entry.
     */
    public long getTimeInForegroundMs() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return ((UidBatteryConsumer) mBatteryConsumer).getTimeInStateMs(
                    UidBatteryConsumer.STATE_FOREGROUND);
        } else {
            return mBatteryConsumer.getUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_USAGE);
        }
    }

    /**
     * Returns background activity time (in milliseconds) that is attributed to this entry.
     */
    public long getTimeInBackgroundMs() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return ((UidBatteryConsumer) mBatteryConsumer).getTimeInStateMs(
                    UidBatteryConsumer.STATE_BACKGROUND);
        } else {
            return 0;
        }
    }

    /**
     * Returns total amount of power (in milli-amp-hours) that is attributed to this entry.
     */
    public double getConsumedPower() {
        return mConsumedPower;
    }

    /**
     * Adds the consumed power of the supplied BatteryConsumer to this entry. Also
     * uses its package with highest drain, if necessary.
     */
    public void add(BatteryConsumer batteryConsumer) {
        mConsumedPower += batteryConsumer.getConsumedPower();
        if (mDefaultPackageName == null && batteryConsumer instanceof UidBatteryConsumer) {
            mDefaultPackageName =
                    ((UidBatteryConsumer) batteryConsumer).getPackageWithHighestDrain();
        }
    }

    /**
     * Gets name and icon resource from UserBatteryConsumer userId.
     */
    public static NameAndIcon getNameAndIconFromUserId(
            Context context, final int userId) {
        UserManager um = context.getSystemService(UserManager.class);
        UserInfo info = um.getUserInfo(userId);

        Drawable icon = null;
        String name = null;
        if (info != null) {
            icon = Utils.getUserIcon(context, um, info);
            name = Utils.getUserLabel(context, info);
        } else {
            name = context.getResources().getString(
                    R.string.running_process_item_removed_user_label);
        }
        return new NameAndIcon(name, icon, 0 /* iconId */);
    }

    /**
     * Gets name and icon resource from UidBatteryConsumer uid.
     */
    public static NameAndIcon getNameAndIconFromUid(
            Context context, String name, final int uid) {
        Drawable icon = context.getDrawable(R.drawable.ic_power_system);
        if (uid == 0) {
            name = context.getResources().getString(R.string.process_kernel_label);
        } else if ("mediaserver".equals(name)) {
            name = context.getResources().getString(R.string.process_mediaserver_label);
        } else if ("dex2oat".equals(name)) {
            name = context.getResources().getString(R.string.process_dex2oat_label);
        }
        return new NameAndIcon(name, icon, 0 /* iconId */);
    }

    /**
     * Gets name annd icon resource from SystemBatteryConsumer drain type.
     */
    public static NameAndIcon getNameAndIconFromDrainType(
            Context context, final int drainType) {
        String name = null;
        int iconId = 0;
        switch (drainType) {
            case SystemBatteryConsumer.DRAIN_TYPE_AMBIENT_DISPLAY:
                name = context.getResources().getString(R.string.ambient_display_screen_title);
                iconId = R.drawable.ic_settings_aod;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_BLUETOOTH:
                name = context.getResources().getString(R.string.power_bluetooth);
                iconId = com.android.internal.R.drawable.ic_settings_bluetooth;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_CAMERA:
                name = context.getResources().getString(R.string.power_camera);
                iconId = R.drawable.ic_settings_camera;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_MOBILE_RADIO:
                name = context.getResources().getString(R.string.power_cell);
                iconId = R.drawable.ic_cellular_1_bar;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_FLASHLIGHT:
                name = context.getResources().getString(R.string.power_flashlight);
                iconId = R.drawable.ic_settings_display;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_PHONE:
                name = context.getResources().getString(R.string.power_phone);
                iconId = R.drawable.ic_settings_voice_calls;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_SCREEN:
                name = context.getResources().getString(R.string.power_screen);
                iconId = R.drawable.ic_settings_display;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_WIFI:
                name = context.getResources().getString(R.string.power_wifi);
                iconId = R.drawable.ic_settings_wireless;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_IDLE:
            case SystemBatteryConsumer.DRAIN_TYPE_MEMORY:
                name = context.getResources().getString(R.string.power_idle);
                iconId = R.drawable.ic_settings_phone_idle;
                break;
            case SystemBatteryConsumer.DRAIN_TYPE_CUSTOM:
                name = null;
                iconId = R.drawable.ic_power_system;
                break;
        }
        return new NameAndIcon(name, null /* icon */, iconId);
    }

    private static <T> T getNonNull(T originalObj, T newObj) {
        return newObj != null ? newObj : originalObj;
    }
}
