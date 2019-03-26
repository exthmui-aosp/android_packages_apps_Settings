/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;

import com.android.settings.R;

import com.android.settingslib.wifi.AccessPoint;

import java.util.List;

/**
 * Here are the items shared by both WifiDppConfiguratorActivity & WifiDppEnrolleeActivity
 *
 * @see WifiQrCode
 */
public class WifiDppUtils {
    /**
     * The fragment tag specified to FragmentManager for container activities to manage fragments.
     */
    public static final String TAG_FRAGMENT_QR_CODE_SCANNER = "qr_code_scanner_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    public static final String TAG_FRAGMENT_QR_CODE_GENERATOR = "qr_code_generator_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    public static final String TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK =
            "choose_saved_wifi_network_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    public static final String TAG_FRAGMENT_ADD_DEVICE = "add_device_fragment";

    /** The data is from {@code com.android.settingslib.wifi.AccessPoint.securityToString} */
    public static final String EXTRA_WIFI_SECURITY = "security";

    /** The data corresponding to {@code WifiConfiguration} SSID */
    public static final String EXTRA_WIFI_SSID = "ssid";

    /** The data corresponding to {@code WifiConfiguration} preSharedKey */
    public static final String EXTRA_WIFI_PRE_SHARED_KEY = "preSharedKey";

    /** The data corresponding to {@code WifiConfiguration} hiddenSSID */
    public static final String EXTRA_WIFI_HIDDEN_SSID = "hiddenSsid";

    /** The data corresponding to {@code WifiConfiguration} networkId */
    public static final String EXTRA_WIFI_NETWORK_ID = "networkId";

    /** The data to recognize if it's a Wi-Fi hotspot for configuration */
    public static final String EXTRA_IS_HOTSPOT = "isHotspot";

    /** Used by {@link android.provider.Settings#ACTION_PROCESS_WIFI_EASY_CONNECT_URI} to
     * indicate test mode UI should be shown. Test UI does not make API calls. Value is a boolean.*/
    public static final String EXTRA_TEST = "test";

    /**
     * Default status code for Easy Connect
     */
    public static final int EASY_CONNECT_EVENT_FAILURE_NONE = 0;

    /**
     * Success status code for Easy Connect.
     */
    public static final int EASY_CONNECT_EVENT_SUCCESS = 1;

    /**
     * Returns whether the device support WiFi DPP.
     */
    public static boolean isWifiDppEnabled(Context context) {
        final WifiManager manager = context.getSystemService(WifiManager.class);
        return manager.isEasyConnectSupported();
    }

    /**
     * Returns an intent to launch QR code scanner for Wi-Fi DPP enrollee.
     *
     * After enrollee success, the callee activity will return connecting WifiConfiguration by
     * putExtra {@code WifiDialogActivity.KEY_WIFI_CONFIGURATION} for
     * {@code Activity#setResult(int resultCode, Intent data)}. The calling object should check
     * if it's available before using it.
     *
     * @param ssid The data corresponding to {@code WifiConfiguration} SSID
     * @return Intent for launching QR code scanner
     */
    public static Intent getEnrolleeQrCodeScannerIntent(String ssid) {
        final Intent intent = new Intent(
                WifiDppEnrolleeActivity.ACTION_ENROLLEE_QR_CODE_SCANNER);
        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        return intent;
    }

    private static String getPresharedKey(WifiManager wifiManager, WifiConfiguration config) {
        String preSharedKey = config.preSharedKey;

        final List<WifiConfiguration> wifiConfigs = wifiManager.getPrivilegedConfiguredNetworks();
        for (WifiConfiguration wifiConfig : wifiConfigs) {
            if (wifiConfig.networkId == config.networkId) {
                preSharedKey = wifiConfig.preSharedKey;
                break;
            }
        }

        return preSharedKey;
    }

    private static String removeFirstAndLastDoubleQuotes(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        int begin = 0;
        int end = str.length() - 1;
        if (str.charAt(begin) == '\"') {
            begin++;
        }
        if (str.charAt(end) == '\"') {
            end--;
        }
        return str.substring(begin, end+1);
    }

    static String getSecurityString(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.SAE)) {
            return WifiQrCode.SECURITY_SAE;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK) ||
                config.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WifiQrCode.SECURITY_WPA_PSK;
        }
        return (config.wepKeys[0] == null) ?
                WifiQrCode.SECURITY_NO_PASSWORD : WifiQrCode.SECURITY_WEP;
    }

    /**
     * Returns an intent to launch QR code generator. It may return null if the security is not
     * supported by QR code generator.
     *
     * Do not use this method for Wi-Fi hotspot network, use
     * {@code getHotspotConfiguratorIntentOrNull} instead.
     *
     * @param context     The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param accessPoint An instance of {@link AccessPoint}
     * @return Intent for launching QR code generator
     */
    public static Intent getConfiguratorQrCodeGeneratorIntentOrNull(Context context,
            WifiManager wifiManager, AccessPoint accessPoint) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (isSupportConfiguratorQrCodeGenerator(accessPoint)) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        } else {
            return null;
        }

        final WifiConfiguration wifiConfiguration = accessPoint.getConfig();
        setConfiguratorIntentExtra(intent, wifiManager, wifiConfiguration);

        return intent;
    }

    /**
     * Returns an intent to launch QR code scanner. It may return null if the security is not
     * supported by QR code scanner.
     *
     * @param context     The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param accessPoint An instance of {@link AccessPoint}
     * @return Intent for launching QR code scanner
     */
    public static Intent getConfiguratorQrCodeScannerIntentOrNull(Context context,
            WifiManager wifiManager, AccessPoint accessPoint) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (isSupportConfiguratorQrCodeScanner(context, accessPoint)) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        } else {
            return null;
        }

        final WifiConfiguration wifiConfiguration = accessPoint.getConfig();
        setConfiguratorIntentExtra(intent, wifiManager, wifiConfiguration);

        if (wifiConfiguration.networkId == WifiConfiguration.INVALID_NETWORK_ID) {
            throw new IllegalArgumentException("Invalid network ID");
        } else {
            intent.putExtra(EXTRA_WIFI_NETWORK_ID, wifiConfiguration.networkId);
        }

        return intent;
    }

    /**
     * Returns an intent to launch QR code generator for the Wi-Fi hotspot. It may return null if
     * the security is not supported by QR code generator.
     *
     * @param context The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param wifiConfiguration {@link WifiConfiguration} of the Wi-Fi hotspot
     * @return Intent for launching QR code generator
     */
    public static Intent getHotspotConfiguratorIntentOrNull(Context context,
            WifiManager wifiManager, WifiConfiguration wifiConfiguration) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (isSupportHotspotConfiguratorQrCodeGenerator(wifiConfiguration)) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        } else {
            return null;
        }

        setConfiguratorIntentExtra(intent, wifiManager, wifiConfiguration);

        intent.putExtra(EXTRA_WIFI_NETWORK_ID, WifiConfiguration.INVALID_NETWORK_ID);
        intent.putExtra(EXTRA_IS_HOTSPOT, true);

        return intent;
    }

    /**
     * Set all extra except {@code EXTRA_WIFI_NETWORK_ID} for the intent to
     * launch configurator activity later.
     *
     * @param intent the target to set extra
     * @param wifiManager an instance of {@code WifiManager}
     * @param wifiConfiguration the Wi-Fi network for launching configurator activity
     */
    private static void setConfiguratorIntentExtra(Intent intent, WifiManager wifiManager,
            WifiConfiguration wifiConfiguration) {
        final String ssid = removeFirstAndLastDoubleQuotes(wifiConfiguration.SSID);
        final String security = getSecurityString(wifiConfiguration);
        String preSharedKey = wifiConfiguration.preSharedKey;

        if (preSharedKey != null) {
            // When the value of this key is read, the actual key is not returned, just a "*".
            // Call privileged system API to obtain actual key.
            preSharedKey = removeFirstAndLastDoubleQuotes(getPresharedKey(wifiManager,
                    wifiConfiguration));
        }

        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        if (!TextUtils.isEmpty(security)) {
            intent.putExtra(EXTRA_WIFI_SECURITY, security);
        }
        if (!TextUtils.isEmpty(preSharedKey)) {
            intent.putExtra(EXTRA_WIFI_PRE_SHARED_KEY, preSharedKey);
        }
    }

    /**
     * Shows authentication screen to confirm credentials (pin, pattern or password) for the current
     * user of the device.
     *
     * @param context The {@code Context} used to get {@code KeyguardManager} service
     * @param successRunnable The {@code Runnable} which will be executed if the user does not setup
     *                        device security or if lock screen is unlocked
     */
    public static void showLockScreen(Context context, Runnable successRunnable) {
        final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            final BiometricPrompt.AuthenticationCallback authenticationCallback =
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                    BiometricPrompt.AuthenticationResult result) {
                            successRunnable.run();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            //Do nothing
                        }
            };

            final BiometricPrompt.Builder builder = new BiometricPrompt.Builder(context)
                    .setTitle(context.getText(R.string.wifi_dpp_lockscreen_title));

            if (keyguardManager.isDeviceSecure()) {
                builder.setDeviceCredentialAllowed(true);
            }

            final BiometricPrompt bp = builder.build();
            final Handler handler = new Handler(Looper.getMainLooper());
            bp.authenticate(new CancellationSignal(),
                    runnable -> handler.post(runnable),
                    authenticationCallback);
        } else {
            successRunnable.run();
        }
    }

    public static boolean isSupportConfiguratorQrCodeScanner(Context context,
            AccessPoint accessPoint) {
        if (!isWifiDppEnabled(context)) {
            return false;
        }

        // DPP 1.0 only supports SAE and PSK.
        final int security = accessPoint.getSecurity();
        if (security == AccessPoint.SECURITY_SAE || security == AccessPoint.SECURITY_PSK) {
            return true;
        }

        return false;
    }

    public static boolean isSupportConfiguratorQrCodeGenerator(AccessPoint accessPoint) {
        // QR code generator produces QR code with ZXing's Wi-Fi network config format,
        // it supports PSK and WEP and non security
        final int security = accessPoint.getSecurity();
        if (security == AccessPoint.SECURITY_PSK || security == AccessPoint.SECURITY_WEP ||
                security == AccessPoint.SECURITY_NONE) {
            return true;
        }

        return false;
    }

    private static boolean isSupportHotspotConfiguratorQrCodeGenerator(
            WifiConfiguration wifiConfiguration) {
        // QR code generator produces QR code with ZXing's Wi-Fi network config format,
        // it supports PSK and WEP and non security
        // KeyMgmt.NONE is for WEP or non security
        return wifiConfiguration.allowedKeyManagement.get(KeyMgmt.WPA2_PSK) ||
                wifiConfiguration.allowedKeyManagement.get(KeyMgmt.NONE);
    }
}
