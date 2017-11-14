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
package com.android.settings.network;

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to set the private dns
 */
public class PrivateDnsModeDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener, RadioGroup.OnCheckedChangeListener, TextWatcher {

    private static final String TAG = "PrivateDnsModeDialogFragment";
    // DNS_MODE -> RadioButton id
    private static final Map<String, Integer> PRIVATE_DNS_MAP;

    static {
        PRIVATE_DNS_MAP = new HashMap<>();
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OFF, R.id.private_dns_mode_off);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OPPORTUNISTIC, R.id.private_dns_mode_opportunistic);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME, R.id.private_dns_mode_provider);
    }

    @VisibleForTesting
    static final String MODE_KEY = Settings.Global.PRIVATE_DNS_MODE;
    @VisibleForTesting
    static final String HOSTNAME_KEY = Settings.Global.PRIVATE_DNS_SPECIFIER;

    @VisibleForTesting
    EditText mEditText;
    @VisibleForTesting
    RadioGroup mRadioGroup;
    @VisibleForTesting
    String mMode;

    public static void show(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(TAG) == null) {
            final PrivateDnsModeDialogFragment fragment = new PrivateDnsModeDialogFragment();
            fragment.show(fragmentManager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();

        return new AlertDialog.Builder(context)
                .setTitle(R.string.select_private_dns_configuration_title)
                .setView(buildPrivateDnsView(context))
                .setPositiveButton(R.string.save, this)
                .setNegativeButton(R.string.dlg_cancel, null)
                .create();
    }

    private View buildPrivateDnsView(final Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        final String mode = Settings.Global.getString(contentResolver, MODE_KEY);
        final View view = LayoutInflater.from(context).inflate(R.layout.private_dns_mode_dialog,
                null);

        mEditText = view.findViewById(R.id.private_dns_mode_provider_hostname);
        mEditText.addTextChangedListener(this);
        mEditText.setText(Settings.Global.getString(contentResolver, HOSTNAME_KEY));

        mRadioGroup = view.findViewById(R.id.private_dns_radio_group);
        mRadioGroup.setOnCheckedChangeListener(this);
        mRadioGroup.check(PRIVATE_DNS_MAP.getOrDefault(mode, R.id.private_dns_mode_opportunistic));

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //TODO(b/34953048): add metric action
        if (mMode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            // Only clickable if hostname is valid, so we could save it safely
            Settings.Global.putString(getContext().getContentResolver(), HOSTNAME_KEY,
                    mEditText.getText().toString());
        }

        Settings.Global.putString(getContext().getContentResolver(), MODE_KEY, mMode);
    }

    @Override
    public int getMetricsCategory() {
        //TODO(b/68030013): add metric id
        return 0;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.private_dns_mode_off:
                mMode = PRIVATE_DNS_MODE_OFF;
                mEditText.setEnabled(false);
                break;
            case R.id.private_dns_mode_opportunistic:
                mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
                mEditText.setEnabled(false);
                break;
            case R.id.private_dns_mode_provider:
                mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
                mEditText.setEnabled(true);
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // TODO(b/68030013): Disable the "positive button" ("Save") when appearsValid is false.
        final boolean valid = isWeaklyValidatedHostname(s.toString());
    }

    private boolean isWeaklyValidatedHostname(String hostname) {
        // TODO(b/34953048): Find and use a better validation method.  Specifically:
        //     [1] this should reject IP string literals, and
        //     [2] do the best, simplest, future-proof verification that
        //         the input approximates a DNS hostname.
        final String WEAK_HOSTNAME_REGEX = "^[a-zA-Z0-9_.-]+$";
        return hostname.matches(WEAK_HOSTNAME_REGEX);
    }

}
