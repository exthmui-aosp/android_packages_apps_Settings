/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.homepage.AdaptiveIconShapeDrawable;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.AdaptiveIcon;
import com.android.settings.widget.AdaptiveOutlineDrawable;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothUtils.ErrorListener;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;

import java.io.IOException;

/**
 * Utils is a helper class that contains constants for various
 * Android resource IDs, debug logging flags, and static methods
 * for creating dialogs.
 */
public final class Utils {

    private static final String TAG = "BluetoothUtils";

    static final boolean V = BluetoothUtils.V; // verbose logging
    static final boolean D =  BluetoothUtils.D;  // regular logging

    public static final int META_INT_ERROR = -1;

    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case BluetoothProfile.STATE_CONNECTED:
                return R.string.bluetooth_connected;
            case BluetoothProfile.STATE_CONNECTING:
                return R.string.bluetooth_connecting;
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_disconnected;
            case BluetoothProfile.STATE_DISCONNECTING:
                return R.string.bluetooth_disconnecting;
            default:
                return 0;
        }
    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context,
            AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener,
            CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    @VisibleForTesting
    static void showConnectingError(Context context, String name, LocalBluetoothManager manager) {
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().visible(context,
            SettingsEnums.PAGE_UNKNOWN, SettingsEnums.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR);
        showError(context, name, R.string.bluetooth_connecting_error_message, manager);
    }

    static void showError(Context context, String name, int messageResId) {
        showError(context, name, messageResId, getLocalBtManager(context));
    }

    private static void showError(Context context, String name, int messageResId,
            LocalBluetoothManager manager) {
        String message = context.getString(messageResId, name);
        Context activity = manager.getForegroundActivity();
        if (manager.isForegroundActivity()) {
            try {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.bluetooth_error_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "Cannot show error dialog.", e);
            }
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }

    public static String createRemoteName(Context context, BluetoothDevice device) {
        String mRemoteName = device != null ? device.getAliasName() : null;

        if (mRemoteName == null) {
            mRemoteName = context.getString(R.string.unknown);
        }
        return mRemoteName;
    }

    private static final ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onShowError(Context context, String name, int messageResId) {
            showError(context, name, messageResId);
        }
    };

    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
        @Override
        public void onBluetoothManagerInitialized(Context appContext,
                LocalBluetoothManager bluetoothManager) {
            BluetoothUtils.setErrorListener(mErrorListener);
        }
    };

    public static boolean isBluetoothScanningEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1;
    }

    public static boolean getBooleanMetaData(BluetoothDevice bluetoothDevice, int key) {
        if (bluetoothDevice == null) {
            return false;
        }

        return Boolean.parseBoolean(bluetoothDevice.getMetadata(key));
    }

    public static String getStringMetaData(BluetoothDevice bluetoothDevice, int key) {
        if (bluetoothDevice == null) {
            return null;
        }
        return bluetoothDevice.getMetadata(key);
    }

    public static int getIntMetaData(BluetoothDevice bluetoothDevice, int key) {
        if (bluetoothDevice == null) {
            return META_INT_ERROR;
        }
        try {
            return Integer.parseInt(bluetoothDevice.getMetadata(key));
        } catch (NumberFormatException e) {
            return META_INT_ERROR;
        }
    }

    /**
     * Get colorful bluetooth icon with description
     */
    public static Pair<Drawable, String> getBtRainbowDrawableWithDescription(Context context,
            CachedBluetoothDevice cachedDevice) {
        final Pair<Drawable, String> pair = BluetoothUtils.getBtClassDrawableWithDescription(
                context, cachedDevice);
        final boolean untetheredHeadset = Utils.getBooleanMetaData(cachedDevice.getDevice(),
                BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET);
        final int iconSize = context.getResources().getDimensionPixelSize(
                R.dimen.bt_nearby_icon_size);
        final Resources resources = context.getResources();

        // Deal with untethered headset
        if (untetheredHeadset) {
            final String uriString = Utils.getStringMetaData(cachedDevice.getDevice(),
                    BluetoothDevice.METADATA_MAIN_ICON);
            final Uri iconUri = uriString != null ? Uri.parse(uriString) : null;
            if (iconUri != null) {
                try {
                    final Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(), iconUri);
                    if (bitmap != null) {
                        final Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, iconSize,
                                iconSize, false);
                        bitmap.recycle();
                        final AdaptiveOutlineDrawable drawable = new AdaptiveOutlineDrawable(
                                resources, resizedBitmap);
                        return new Pair<>(drawable, pair.second);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to get drawable for: " + iconUri, e);
                }
            }
        }

        // Deal with normal headset
        final int[] iconFgColors = resources.getIntArray(R.array.bt_icon_fg_colors);
        final int[] iconBgColors = resources.getIntArray(R.array.bt_icon_bg_colors);

        // get color index based on mac address
        final int index =  Math.abs(cachedDevice.getAddress().hashCode()) % iconBgColors.length;
        pair.first.setColorFilter(iconFgColors[index], PorterDuff.Mode.SRC_ATOP);
        final Drawable adaptiveIcon = new AdaptiveIcon(context, pair.first);
        ((AdaptiveIcon) adaptiveIcon).setBackgroundColor(iconBgColors[index]);

        return new Pair<>(adaptiveIcon, pair.second);
    }

}
