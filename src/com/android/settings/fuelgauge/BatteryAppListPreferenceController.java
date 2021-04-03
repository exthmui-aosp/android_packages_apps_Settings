/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.BatteryConsumer;
import android.os.BatteryUsageStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemBatteryConsumer;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that update the battery header view
 */
public class BatteryAppListPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnDestroy {
    @VisibleForTesting
    static final boolean USE_FAKE_DATA = false;
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 20;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;

    private final String mPreferenceKey;
    @VisibleForTesting
    PreferenceGroup mAppListGroup;
    private BatteryUsageStats mBatteryUsageStats;
    private ArrayMap<String, Preference> mPreferenceCache;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private final UserManager mUserManager;
    private final PackageManager mPackageManager;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private Context mPrefContext;

    /**
     * Battery attribution list configuration.
     */
    public interface Config {
        /**
         * Returns true if the attribution list should be shown.
         */
        boolean shouldShowBatteryAttributionList(Context context);
    }

    @VisibleForTesting
    static Config sConfig = new Config() {
        @Override
        public boolean shouldShowBatteryAttributionList(Context context) {
            if (USE_FAKE_DATA) {
                return true;
            }

            PowerProfile powerProfile = new PowerProfile(context);
            return powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL)
                    >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP;
        }
    };

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp = mAppListGroup.findPreference(entry.getKey());
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUserManager.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                        if (entry.isAppEntry()) {
                            pgp.setContentDescription(entry.name);
                        }
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = mActivity;
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public BatteryAppListPreferenceController(Context context, String preferenceKey,
            Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }

        mPreferenceKey = preferenceKey;
        mBatteryUtils = BatteryUtils.getInstance(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPackageManager = context.getPackageManager();
        mActivity = activity;
        mFragment = fragment;
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
    }

    @Override
    public void onDestroy() {
        if (mActivity.isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mAppListGroup = screen.findPreference(mPreferenceKey);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference instanceof PowerGaugePreference) {
            PowerGaugePreference pgp = (PowerGaugePreference) preference;
            BatteryEntry entry = pgp.getInfo();
            AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity,
                    mFragment, entry, pgp.getPercent());
            return true;
        }
        return false;
    }

    /**
     * Refreshes the list of battery consumers using the supplied BatteryUsageStats.
     */
    public void refreshAppListGroup(BatteryUsageStats batteryUsageStats, boolean showAllApps) {
        if (!isAvailable()) {
            return;
        }

        mBatteryUsageStats = USE_FAKE_DATA ? getFakeStats() : batteryUsageStats;
        mAppListGroup.setTitle(R.string.power_usage_list_summary);

        boolean addedSome = false;

        cacheRemoveAllPrefs(mAppListGroup);
        mAppListGroup.setOrderingAsAdded(false);

        if (sConfig.shouldShowBatteryAttributionList(mContext)) {
            final int dischargePercentage = getDischargePercentage(batteryUsageStats);
            final List<BatteryEntry> usageList = getCoalescedUsageList(showAllApps);
            final double totalPower = batteryUsageStats.getConsumedPower();
            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatteryEntry entry = usageList.get(i);

                final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                        entry.getConsumedPower(), totalPower, dischargePercentage);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }

                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(entry.getUid()));
                final Drawable badgedIcon = mUserManager.getBadgedIconForUser(entry.getIcon(),
                        userHandle);
                final CharSequence contentDescription = mUserManager.getBadgedLabelForUser(
                        entry.getLabel(), userHandle);

                final String key = entry.getKey();
                PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
                if (pref == null) {
                    pref = new PowerGaugePreference(mPrefContext, badgedIcon,
                            contentDescription, entry);
                    pref.setKey(key);
                }
                entry.percent = percentOfTotal;
                pref.setTitle(entry.getLabel());
                pref.setOrder(i + 1);
                pref.setPercent(percentOfTotal);
                pref.shouldShowAnomalyIcon(false);
                setUsageSummary(pref, entry);
                addedSome = true;
                mAppListGroup.addPreference(pref);
                if (mAppListGroup.getPreferenceCount() - getCachedCount()
                        > (MAX_ITEMS_TO_LIST + 1)) {
                    break;
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        removeCachedPrefs(mAppListGroup);

        BatteryEntry.startRequestQueue();
    }

    /**
     * Gets the BatteryEntry list by using the supplied BatteryUsageStats.
     */
    public List<BatteryEntry> getBatteryEntryList(
            BatteryUsageStats batteryUsageStats, boolean showAllApps) {
        mBatteryUsageStats = USE_FAKE_DATA ? getFakeStats() : batteryUsageStats;
        if (!sConfig.shouldShowBatteryAttributionList(mContext)) {
            return null;
        }
        final int dischargePercentage = getDischargePercentage(batteryUsageStats);
        final List<BatteryEntry> usageList = getCoalescedUsageList(showAllApps);
        final double totalPower = batteryUsageStats.getConsumedPower();
        for (int i = 0; i < usageList.size(); i++) {
            final BatteryEntry entry = usageList.get(i);
            final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                    entry.getConsumedPower(), totalPower, dischargePercentage);
            entry.percent = percentOfTotal;
        }
        return usageList;
    }

    private int getDischargePercentage(BatteryUsageStats batteryUsageStats) {
        int dischargePercentage = batteryUsageStats.getDischargePercentage();
        if (dischargePercentage < 0) {
            dischargePercentage = 0;
        }
        return dischargePercentage;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private List<BatteryEntry> getCoalescedUsageList(boolean showAllApps) {
        final SparseArray<BatteryEntry> batteryEntryList = new SparseArray<>();

        final ArrayList<BatteryEntry> results = new ArrayList<>();
        final List<UidBatteryConsumer> uidBatteryConsumers =
                mBatteryUsageStats.getUidBatteryConsumers();
        for (int i = 0, size = uidBatteryConsumers.size(); i < size; i++) {
            final UidBatteryConsumer consumer = uidBatteryConsumers.get(i);
            int realUid = consumer.getUid();

            // Check if this UID is a shared GID. If so, we combine it with the OWNER's
            // actual app UID.
            if (isSharedGid(consumer.getUid())) {
                realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                        UserHandle.getAppIdFromSharedAppGid(consumer.getUid()));
            }

            // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
            if (isSystemUid(realUid)
                    && !"mediaserver".equals(consumer.getPackageWithHighestDrain())) {
                // Use the system UID for all UIDs running in their own sandbox that
                // are not apps. We exclude mediaserver because we already are expected to
                // report that as a separate item.
                realUid = Process.SYSTEM_UID;
            }

            final String[] packages = mPackageManager.getPackagesForUid(consumer.getUid());
            if (mBatteryUtils.shouldHideUidBatteryConsumerUnconditionally(consumer, packages)) {
                continue;
            }

            final boolean isHidden = mBatteryUtils.shouldHideUidBatteryConsumer(consumer, packages);
            if (isHidden && !showAllApps) {
                continue;
            }

            final int index = batteryEntryList.indexOfKey(realUid);
            if (index < 0) {
                // New entry.
                batteryEntryList.put(realUid, new BatteryEntry(mContext, mHandler, mUserManager,
                        consumer, isHidden, packages, null));
            } else {
                // Combine BatterySippers if we already have one with this UID.
                final BatteryEntry existingSipper = batteryEntryList.valueAt(index);
                existingSipper.add(consumer);
            }
        }

        final List<SystemBatteryConsumer> systemBatteryConsumers =
                mBatteryUsageStats.getSystemBatteryConsumers();
        for (int i = 0, size = systemBatteryConsumers.size(); i < size; i++) {
            final SystemBatteryConsumer consumer = systemBatteryConsumers.get(i);
            if (!showAllApps && mBatteryUtils.shouldHideSystemBatteryConsumer(consumer)) {
                continue;
            }

            results.add(new BatteryEntry(mContext, mHandler, mUserManager,
                    consumer, /* isHidden */ true, null, null));
        }

        if (showAllApps) {
            final List<UserBatteryConsumer> userBatteryConsumers =
                    mBatteryUsageStats.getUserBatteryConsumers();
            for (int i = 0, size = userBatteryConsumers.size(); i < size; i++) {
                final UserBatteryConsumer consumer = userBatteryConsumers.get(i);
                results.add(new BatteryEntry(mContext, mHandler, mUserManager,
                        consumer, /* isHidden */ true, null, null));
            }
        }

        final int numUidSippers = batteryEntryList.size();

        for (int i = 0; i < numUidSippers; i++) {
            results.add(batteryEntryList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        results.sort(BatteryEntry.COMPARATOR);
        return results;
    }

    @VisibleForTesting
    void setUsageSummary(Preference preference, BatteryEntry entry) {
        // Only show summary when usage time is longer than one minute
        final long usageTimeMs = entry.getTimeInForegroundMs();
        if (shouldShowSummary(entry) && usageTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
            final CharSequence timeSequence =
                    StringUtil.formatElapsedTime(mContext, usageTimeMs, false, false);
            preference.setSummary(
                    entry.isHidden()
                            ? timeSequence
                            : TextUtils.expandTemplate(mContext.getText(R.string.battery_used_for),
                                    timeSequence));
        }
    }

    private void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    private boolean shouldShowSummary(BatteryEntry entry) {
        final CharSequence[] allowlistPackages = mContext.getResources()
                .getTextArray(R.array.allowlist_hide_summary_in_battery_usage);
        final String target = entry.getDefaultPackageName();

        for (CharSequence packageName : allowlistPackages) {
            if (TextUtils.equals(target, packageName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        final int appUid = UserHandle.getAppId(uid);
        return appUid >= Process.SYSTEM_UID && appUid < Process.FIRST_APPLICATION_UID;
    }

    private BatteryUsageStats getFakeStats() {
        BatteryUsageStats.Builder builder = new BatteryUsageStats.Builder(0, 0)
                .setDischargePercentage(100);

        float use = 500;
        for (@SystemBatteryConsumer.DrainType int drainType : new int[]{
                SystemBatteryConsumer.DRAIN_TYPE_AMBIENT_DISPLAY,
                SystemBatteryConsumer.DRAIN_TYPE_BLUETOOTH,
                SystemBatteryConsumer.DRAIN_TYPE_CAMERA,
                SystemBatteryConsumer.DRAIN_TYPE_FLASHLIGHT,
                SystemBatteryConsumer.DRAIN_TYPE_IDLE,
                SystemBatteryConsumer.DRAIN_TYPE_MEMORY,
                SystemBatteryConsumer.DRAIN_TYPE_MOBILE_RADIO,
                SystemBatteryConsumer.DRAIN_TYPE_PHONE,
                SystemBatteryConsumer.DRAIN_TYPE_SCREEN,
                SystemBatteryConsumer.DRAIN_TYPE_WIFI,
        }) {
            builder.getOrCreateSystemBatteryConsumerBuilder(drainType)
                    .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, use);
            use += 5;
        }

        use = 450;
        for (int i = 0; i < 100; i++) {
            builder.getOrCreateUidBatteryConsumerBuilder(
                            new FakeUid(Process.FIRST_APPLICATION_UID + i))
                    .setTimeInStateMs(UidBatteryConsumer.STATE_FOREGROUND, 10000 + i * 1000)
                    .setTimeInStateMs(UidBatteryConsumer.STATE_BACKGROUND, 20000 + i * 2000)
                    .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, use);
            use += 1;
        }

        // Simulate dex2oat process.
        builder.getOrCreateUidBatteryConsumerBuilder(new FakeUid(Process.FIRST_APPLICATION_UID))
                .setUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_CPU, 100000)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, 1000.0)
                .setPackageWithHighestDrain("dex2oat");

        builder.getOrCreateUidBatteryConsumerBuilder(new FakeUid(Process.FIRST_APPLICATION_UID + 1))
                .setUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_CPU, 100000)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, 1000.0)
                .setPackageWithHighestDrain("dex2oat");

        builder.getOrCreateUidBatteryConsumerBuilder(
                        new FakeUid(UserHandle.getSharedAppGid(Process.LOG_UID)))
                .setUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_CPU, 100000)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, 900.0);

        return builder.build();
    }

    private Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    private void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

    private int getCachedCount() {
        return mPreferenceCache != null ? mPreferenceCache.size() : 0;
    }

    private void addNotAvailableMessage() {
        final String NOT_AVAILABLE = "not_available";
        Preference notAvailable = getCachedPreference(NOT_AVAILABLE);
        if (notAvailable == null) {
            notAvailable = new Preference(mPrefContext);
            notAvailable.setKey(NOT_AVAILABLE);
            notAvailable.setTitle(R.string.power_usage_not_available);
            notAvailable.setSelectable(false);
            mAppListGroup.addPreference(notAvailable);
        }
    }
}
