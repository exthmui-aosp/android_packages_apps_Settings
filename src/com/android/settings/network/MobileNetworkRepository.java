/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.network;

import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.mobile.dataservice.MobileNetworkDatabase;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoDao;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoDao;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoDao;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class MobileNetworkRepository extends SubscriptionManager.OnSubscriptionsChangedListener {

    private static final String TAG = "MobileNetworkRepository";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private MobileNetworkDatabase mMobileNetworkDatabase;
    private SubscriptionInfoDao mSubscriptionInfoDao;
    private UiccInfoDao mUiccInfoDao;
    private MobileNetworkInfoDao mMobileNetworkInfoDao;
    private List<SubscriptionInfoEntity> mAvailableSubInfoEntityList = new ArrayList<>();
    private List<SubscriptionInfoEntity> mActiveSubInfoEntityList = new ArrayList<>();
    private List<UiccInfoEntity> mUiccInfoEntityList = new ArrayList<>();
    private List<MobileNetworkInfoEntity> mMobileNetworkInfoEntityList = new ArrayList<>();
    private MobileNetworkCallback mCallback;
    private Context mContext;
    private AirplaneModeObserver mAirplaneModeObserver;
    private Uri mAirplaneModeSettingUri;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private int mPhysicalSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mLogicalSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mCardState = UiccSlotInfo.CARD_STATE_INFO_ABSENT;
    private int mPortIndex = TelephonyManager.INVALID_PORT_INDEX;
    private int mCardId = TelephonyManager.UNINITIALIZED_CARD_ID;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mIsEuicc = false;
    private boolean mIsRemovable = false;
    private boolean mIsActive = false;

    MobileNetworkRepository(Context context, MobileNetworkCallback mobileNetworkCallback) {
        mContext = context;
        mCallback = mobileNetworkCallback;
        mMobileNetworkDatabase = MobileNetworkDatabase.createDatabase(context);
        mSubscriptionInfoDao = mMobileNetworkDatabase.mSubscriptionInfoDao();
        mUiccInfoDao = mMobileNetworkDatabase.mUiccInfoDao();
        mMobileNetworkInfoDao = mMobileNetworkDatabase.mMobileNetworkInfoDao();
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mAirplaneModeObserver = new AirplaneModeObserver(new Handler(Looper.getMainLooper()));
        mAirplaneModeSettingUri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_MOBILE_NETWORK_DB_CREATED);
    }

    private class AirplaneModeObserver extends ContentObserver {
        AirplaneModeObserver(Handler handler) {
            super(handler);
        }

        public void register(Context context) {
            context.getContentResolver().registerContentObserver(mAirplaneModeSettingUri, false,
                    this);
        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(mAirplaneModeSettingUri)) {
                mCallback.onAirplaneModeChanged(isAirplaneModeOn());
            }
        }
    }

    public void addRegister(LifecycleOwner lifecycleOwner) {
        mSubscriptionManager.addOnSubscriptionsChangedListener(mContext.getMainExecutor(), this);
        mAirplaneModeObserver.register(mContext);
        observeAllSubInfo(lifecycleOwner);
        observeAllUiccInfo(lifecycleOwner);
        observeAllMobileNetworkInfo(lifecycleOwner);
    }

    public void removeRegister() {
        mSubscriptionManager.removeOnSubscriptionsChangedListener(this);
        mAirplaneModeObserver.unRegister(mContext);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
    }

    private void observeAllSubInfo(LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "Observe subInfo.");
        mMobileNetworkDatabase.queryAvailableSubInfos().observe(
                lifecycleOwner, this::onAvailableSubInfoChanged);
    }

    private void observeAllUiccInfo(LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "Observe UICC info.");
        mMobileNetworkDatabase.queryAllUiccInfo().observe(
                lifecycleOwner, this::onAllUiccInfoChanged);
    }

    private void observeAllMobileNetworkInfo(LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "Observe mobile network info.");
        mMobileNetworkDatabase.queryAllMobileNetworkInfo().observe(
                lifecycleOwner, this::onAllMobileNetworkInfoChanged);
    }

    public List<SubscriptionInfoEntity> getAvailableSubInfoEntityList() {
        return mAvailableSubInfoEntityList;
    }

    public List<SubscriptionInfoEntity> getActiveSubscriptionInfoList() {
        return mActiveSubInfoEntityList;
    }

    public List<UiccInfoEntity> getUiccInfoEntityList() {
        return mUiccInfoEntityList;
    }

    public List<MobileNetworkInfoEntity> getMobileNetworkInfoEntityList() {
        return mMobileNetworkInfoEntityList;
    }

    public int getSubInfosCount() {
        return mSubscriptionInfoDao.count();
    }

    public int getUiccInfosCount() {
        return mUiccInfoDao.count();
    }

    public int getMobileNetworkInfosCount() {
        return mMobileNetworkInfoDao.count();
    }

    private void getUiccInfoBySubscriptionInfo(UiccSlotInfo[] uiccSlotInfos,
            SubscriptionInfo subInfo) {
        for (int i = 0; i < uiccSlotInfos.length; i++) {
            UiccSlotInfo curSlotInfo = uiccSlotInfos[i];
            if (curSlotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT) {
                mIsEuicc = curSlotInfo.getIsEuicc();
                mCardState = curSlotInfo.getCardStateInfo();
                mIsRemovable = curSlotInfo.isRemovable();
                mCardId = subInfo.getCardId();

                Collection<UiccPortInfo> uiccPortInfos = curSlotInfo.getPorts();
                for (UiccPortInfo portInfo : uiccPortInfos) {
                    if (portInfo.getPortIndex() == subInfo.getPortIndex()
                            && portInfo.getLogicalSlotIndex() == subInfo.getSimSlotIndex()) {
                        mPhysicalSlotIndex = i;
                        mLogicalSlotIndex = portInfo.getLogicalSlotIndex();
                        mIsActive = portInfo.isActive();
                        mPortIndex = portInfo.getPortIndex();
                        break;
                    } else {
                        Log.d(TAG,
                                "Can not get port index and physicalSlotIndex for subId " + mSubId);
                    }
                }
                if (mPhysicalSlotIndex != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                    break;
                }
            } else {
                Log.d(TAG, "Can not get card state info");
            }
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_GET_UICC_INFO);
    }

    private void onAvailableSubInfoChanged(
            List<SubscriptionInfoEntity> availableSubInfoEntityList) {
        mAvailableSubInfoEntityList = availableSubInfoEntityList;
        mActiveSubInfoEntityList = mAvailableSubInfoEntityList.stream()
                .filter(SubscriptionInfoEntity::isActiveSubscription)
                .filter(SubscriptionInfoEntity::isSubscriptionVisible)
                .collect(Collectors.toList());
        Log.d(TAG, "onAvailableSubInfoChanged, availableSubInfoEntityList = "
                + availableSubInfoEntityList);
        mCallback.onAvailableSubInfoChanged(availableSubInfoEntityList);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_SUB_INFO_IS_CHANGED);
        setActiveSubInfoList(mActiveSubInfoEntityList);
    }

    private void setActiveSubInfoList(
            List<SubscriptionInfoEntity> activeSubInfoEntityList) {
        Log.d(TAG, "onActiveSubInfoChanged, activeSubInfoEntityList = " + activeSubInfoEntityList);
        mCallback.onActiveSubInfoChanged(mActiveSubInfoEntityList);
    }

    private void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
        mUiccInfoEntityList = uiccInfoEntityList;
        mCallback.onAllUiccInfoChanged(uiccInfoEntityList);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_UICC_INFO_IS_CHANGED);
    }

    private void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        mMobileNetworkInfoEntityList = mobileNetworkInfoEntityList;
        mCallback.onAllMobileNetworkInfoChanged(mobileNetworkInfoEntityList);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_MOBILE_NETWORK_INFO_IS_CHANGED);
    }

    public void insertSubInfo(Context context, SubscriptionInfo info) {
        mExecutor.execute(() -> {
            SubscriptionInfoEntity subInfoEntity =
                    convertToSubscriptionInfoEntity(context, info);
            if (subInfoEntity != null) {
                mMobileNetworkDatabase.insertSubsInfo(subInfoEntity);
                mMetricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_SUB_INFO);
            } else {
                Log.d(TAG, "Can not insert subInfo, the entity is null");
            }
        });
    }

    public void deleteAllInfoBySubId(String subId) {
        mExecutor.execute(() -> {
            mMobileNetworkDatabase.deleteSubInfoBySubId(subId);
            mMobileNetworkDatabase.deleteUiccInfoBySubId(subId);
            mMobileNetworkDatabase.deleteMobileNetworkInfoBySubId(subId);
        });
        mAvailableSubInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mActiveSubInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mUiccInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mMobileNetworkInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_DELETE_DATA);
    }

    public SubscriptionInfoEntity convertToSubscriptionInfoEntity(Context context,
            SubscriptionInfo subInfo) {
        mSubId = subInfo.getSubscriptionId();
        mTelephonyManager = context.getSystemService(
                TelephonyManager.class).createForSubscriptionId(mSubId);

        UiccSlotInfo[] uiccSlotInfos = mTelephonyManager.getUiccSlotsInfo();
        if (uiccSlotInfos == null || uiccSlotInfos.length == 0) {
            Log.d(TAG, "uiccSlotInfos = null or empty");
            return null;
        } else {
            getUiccInfoBySubscriptionInfo(uiccSlotInfos, subInfo);
            insertUiccInfo();
            insertMobileNetworkInfo(context);
            Log.d(TAG, "convert subscriptionInfo to entity for subId = " + mSubId);
            return new SubscriptionInfoEntity(String.valueOf(mSubId),
                    subInfo.getSimSlotIndex(),
                    subInfo.getCarrierId(), subInfo.getDisplayName().toString(),
                    subInfo.getCarrierName() != null ? subInfo.getCarrierName().toString() : "",
                    subInfo.getDataRoaming(), subInfo.getMccString(), subInfo.getMncString(),
                    subInfo.getCountryIso(), subInfo.isEmbedded(), mCardId,
                    subInfo.getPortIndex(), subInfo.isOpportunistic(),
                    String.valueOf(subInfo.getGroupUuid()),
                    subInfo.getSubscriptionType(),
                    SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, context).toString(),
                    SubscriptionUtil.isSubscriptionVisible(mSubscriptionManager, context, subInfo),
                    SubscriptionUtil.getFormattedPhoneNumber(context, subInfo),
                    SubscriptionUtil.getFirstRemovableSubscription(context) == null ? false
                            : SubscriptionUtil.getFirstRemovableSubscription(
                                    context).getSubscriptionId() == mSubId,
                    String.valueOf(SubscriptionUtil.getDefaultSimConfig(context, mSubId)),
                    SubscriptionUtil.getSubscriptionOrDefault(context, mSubId).getSubscriptionId()
                            == mSubId,
                    mSubscriptionManager.isValidSubscriptionId(mSubId),
                    mSubscriptionManager.isUsableSubscriptionId(mSubId),
                    mSubscriptionManager.isActiveSubscriptionId(mSubId),
                    true /*availableSubInfo*/,
                    mSubscriptionManager.getDefaultVoiceSubscriptionId() == mSubId,
                    mSubscriptionManager.getDefaultSmsSubscriptionId() == mSubId,
                    mSubscriptionManager.getDefaultDataSubscriptionId() == mSubId,
                    mSubscriptionManager.getDefaultSubscriptionId() == mSubId);
        }
    }

    public void insertUiccInfo() {
        mMobileNetworkDatabase.insertUiccInfo(convertToUiccInfoEntity());
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_UICC_INFO);
    }

    public void insertMobileNetworkInfo(Context context) {
        mMobileNetworkDatabase.insertMobileNetworkInfo(convertToMobileNetworkInfoEntity(context));
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_MOBILE_NETWORK_INFO);
    }

    public MobileNetworkInfoEntity convertToMobileNetworkInfoEntity(Context context) {
        return new MobileNetworkInfoEntity(String.valueOf(mSubId),
                MobileNetworkUtils.isContactDiscoveryEnabled(context, mSubId),
                MobileNetworkUtils.isContactDiscoveryVisible(context, mSubId),
                MobileNetworkUtils.isMobileDataEnabled(context),
                MobileNetworkUtils.isCdmaOptions(context, mSubId),
                MobileNetworkUtils.isGsmOptions(context, mSubId),
                MobileNetworkUtils.isWorldMode(context, mSubId),
                MobileNetworkUtils.shouldDisplayNetworkSelectOptions(context, mSubId),
                MobileNetworkUtils.isTdscdmaSupported(context, mSubId),
                MobileNetworkUtils.activeNetworkIsCellular(context),
                SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager)
        );
    }

    private UiccInfoEntity convertToUiccInfoEntity() {
        return new UiccInfoEntity(String.valueOf(mSubId), String.valueOf(mPhysicalSlotIndex),
                mLogicalSlotIndex, mCardId, mIsEuicc, isMultipleEnabledProfilesSupported(),
                mCardState, mIsRemovable, mIsActive, mPortIndex
        );
    }

    private boolean isMultipleEnabledProfilesSupported() {
        List<UiccCardInfo> cardInfos = mTelephonyManager.getUiccCardsInfo();
        if (cardInfos == null) {
            Log.w(TAG, "UICC card info list is empty.");
            return false;
        }
        return cardInfos.stream().anyMatch(
                cardInfo -> cardInfo.isMultipleEnabledProfilesSupported());
    }

    @Override
    public void onSubscriptionsChanged() {
        insertAvailableSubInfoToEntity(mSubscriptionManager.getAvailableSubscriptionInfoList());
    }

    private void insertAvailableSubInfoToEntity(List<SubscriptionInfo> availableInfoList) {
        if ((availableInfoList == null || availableInfoList.size() == 0)
                && mAvailableSubInfoEntityList.size() != 0) {
            Log.d(TAG, "availableSudInfoList from framework is empty, remove all subs");
            for (SubscriptionInfoEntity info : mAvailableSubInfoEntityList) {
                deleteAllInfoBySubId(info.subId);
            }
        } else if (availableInfoList != null) {
            for (SubscriptionInfo subInfo : availableInfoList) {
                if (availableInfoList.size() < mAvailableSubInfoEntityList.size()) {
                    Optional<SubscriptionInfoEntity> infoEntity =
                            mAvailableSubInfoEntityList.stream().filter(
                                    info -> subInfo.getSubscriptionId()
                                            != Integer.parseInt(info.subId)).findFirst();

                    if (infoEntity.isPresent()) {
                        Log.d(TAG, "delete sudInfo " + infoEntity.get().subId
                                + " from subInfoEntity");
                        deleteAllInfoBySubId(infoEntity.get().subId);
                    }
                }

                Log.d(TAG, "insert sudInfo " + subInfo.getSubscriptionId() + " to subInfoEntity");
                insertSubInfo(mContext, subInfo);
            }
        }
    }

    public boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Callback for clients to get the latest info changes if the framework or content observers.
     * updates the relevant info.
     */
    interface MobileNetworkCallback {
        void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList);

        void onActiveSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList);

        void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList);

        void onAllMobileNetworkInfoChanged(
                List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList);

        void onAirplaneModeChanged(boolean enabled);
    }
}
