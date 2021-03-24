/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.autofill;

import static android.service.autofill.AutofillService.EXTRA_RESULT;

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

import android.annotation.UserIdInt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.service.autofill.IAutoFillService;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.IResultReceiver;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Queries available autofill services and adds preferences for those that declare passwords
 * settings.
 * <p>
 * The controller binds to each service to fetch the number of saved passwords in each.
 */
public class PasswordsPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "AutofillSettings";

    private final PackageManager mPm;
    private final IconDrawableFactory mIconFactory;
    private final List<AutofillServiceInfo> mServices;

    private LifecycleOwner mLifecycleOwner;

    public PasswordsPreferenceController(Context context, String preferenceKey) {
        this(context, preferenceKey,
                AutofillServiceInfo.getAvailableServices(context, UserHandle.myUserId()));
    }

    @VisibleForTesting
    public PasswordsPreferenceController(
            Context context, String preferenceKey, List<AutofillServiceInfo> availableServices) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
        for (int i = availableServices.size() - 1; i >= 0; i--) {
            final String passwordsActivity = availableServices.get(i).getPasswordsActivity();
            if (TextUtils.isEmpty(passwordsActivity)) {
                availableServices.remove(i);
            }
        }
        mServices = availableServices;
    }

    @OnLifecycleEvent(ON_CREATE)
    void onCreate(LifecycleOwner lifecycleOwner) {
        mLifecycleOwner = lifecycleOwner;
    }

    @Override
    public int getAvailabilityStatus() {
        return mServices.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final PreferenceGroup group = screen.findPreference(getPreferenceKey());
        // TODO(b/169455298): Show work profile passwords too.
        addPasswordPreferences(screen.getContext(), UserHandle.myUserId(), group);
    }

    private void addPasswordPreferences(
            Context prefContext, @UserIdInt int user, PreferenceGroup group) {
        for (int i = 0; i < mServices.size(); i++) {
            final AutofillServiceInfo service = mServices.get(i);
            final Preference pref = new Preference(prefContext);
            final ServiceInfo serviceInfo = service.getServiceInfo();
            pref.setTitle(serviceInfo.loadLabel(mPm));
            final Drawable icon =
                    mIconFactory.getBadgedIcon(
                            serviceInfo,
                            serviceInfo.applicationInfo,
                            user);
            Utils.setSafeIcon(pref, icon);
            pref.setIntent(
                    new Intent(Intent.ACTION_MAIN)
                            .setClassName(serviceInfo.packageName, service.getPasswordsActivity()));

            final MutableLiveData<Integer> passwordCount = new MutableLiveData<>();
            passwordCount.observe(
                    // TODO(b/169455298): Validate the result.
                    // TODO(b/169455298): Use a Quantity String resource.
                    mLifecycleOwner, count -> pref.setSummary("" + count + " passwords saved"));
            // TODO(b/169455298): Limit the number of concurrent queries.
            // TODO(b/169455298): Cache the results for some time.
            requestSavedPasswordCount(service, user, passwordCount);

            group.addPreference(pref);
        }
    }

    private void requestSavedPasswordCount(
            AutofillServiceInfo service, @UserIdInt int user, MutableLiveData<Integer> data) {
        final Intent intent =
                new Intent(AutofillService.SERVICE_INTERFACE)
                        .setComponent(service.getServiceInfo().getComponentName());
        final AutofillServiceConnection connection = new AutofillServiceConnection(mContext, data);
        if (mContext.bindServiceAsUser(
                intent, connection, Context.BIND_AUTO_CREATE, UserHandle.of(user))) {
            connection.mBound.set(true);
            mLifecycleOwner.getLifecycle().addObserver(connection);
        }
    }

    private static class AutofillServiceConnection implements ServiceConnection, LifecycleObserver {
        final WeakReference<Context> mContext;
        final MutableLiveData<Integer> mData;
        final AtomicBoolean mBound = new AtomicBoolean();

        AutofillServiceConnection(Context context, MutableLiveData<Integer> data) {
            mContext = new WeakReference<>(context);
            mData = data;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final IAutoFillService autofillService = IAutoFillService.Stub.asInterface(service);
            // TODO check if debug is logged on user build.
            Log.d(TAG, "Fetching password count from " + name);
            try {
                autofillService.onSavedPasswordCountRequest(
                        new IResultReceiver.Stub() {
                            @Override
                            public void send(int resultCode, Bundle resultData) {
                                Log.d(TAG, "Received password count result " + resultCode
                                        + " from " + name);
                                if (resultCode == 0 && resultData != null) {
                                    mData.postValue(resultData.getInt(EXTRA_RESULT));
                                }
                                unbind();
                            }
                        });
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to fetch password count: " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @OnLifecycleEvent(ON_DESTROY)
        void unbind() {
            if (!mBound.getAndSet(false)) {
                return;
            }
            final Context context = mContext.get();
            if (context != null) {
                context.unbindService(this);
            }
        }
    }
}
