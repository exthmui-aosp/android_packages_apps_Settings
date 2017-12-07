/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.util.DebugUtils;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

import com.android.settings.R;

import java.io.File;

public class StorageWizardInit extends StorageWizardBase {
    private RadioButton mRadioExternal;
    private RadioButton mRadioInternal;

    private boolean mIsPermittedToAdopt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_init);

        mIsPermittedToAdopt = UserManager.get(this).isAdminUser()
                && !ActivityManager.isUserAMonkey();

        setIllustrationType(ILLUSTRATION_SETUP);
        setHeaderText(R.string.storage_wizard_init_title, mDisk.getDescription());

        mRadioExternal = (RadioButton) findViewById(R.id.storage_wizard_init_external_title);
        mRadioInternal = (RadioButton) findViewById(R.id.storage_wizard_init_internal_title);

        mRadioExternal.setOnCheckedChangeListener(mRadioListener);
        mRadioInternal.setOnCheckedChangeListener(mRadioListener);

        findViewById(R.id.storage_wizard_init_external_summary).setPadding(
                mRadioExternal.getCompoundPaddingLeft(), 0,
                mRadioExternal.getCompoundPaddingRight(), 0);
        findViewById(R.id.storage_wizard_init_internal_summary).setPadding(
                mRadioExternal.getCompoundPaddingLeft(), 0,
                mRadioExternal.getCompoundPaddingRight(), 0);

        getNextButton().setEnabled(false);

        if (!mDisk.isAdoptable()) {
            // If not adoptable, we only have one choice
            mRadioExternal.setChecked(true);
            onNavigateNext();
            finish();
        } else if (!mIsPermittedToAdopt) {
            // TODO: Show a message about why this is disabled for guest and
            // that only an admin user can adopt an sd card.
            mRadioInternal.setEnabled(false);
        } else if (mVolume != null && mVolume.getType() == VolumeInfo.TYPE_PUBLIC
                && mVolume.isMountedReadable()) {
            // Device is mounted, so classify contents to possibly pick a
            // recommended default operation.
            new ClassifyTask().execute(mVolume.getPath());
        }
    }

    private final OnCheckedChangeListener mRadioListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (buttonView == mRadioExternal) {
                    mRadioInternal.setChecked(false);
                    setIllustrationType(ILLUSTRATION_PORTABLE);
                } else if (buttonView == mRadioInternal) {
                    mRadioExternal.setChecked(false);
                    setIllustrationType(ILLUSTRATION_INTERNAL);
                }
                getNextButton().setEnabled(true);
            }
        }
    };

    @Override
    public void onNavigateNext() {
        if (mRadioExternal.isChecked()) {
            if (mVolume != null && mVolume.getType() == VolumeInfo.TYPE_PUBLIC
                    && mVolume.getState() != VolumeInfo.STATE_UNMOUNTABLE) {
                // Remember that user made decision
                mStorage.setVolumeInited(mVolume.getFsUuid(), true);

                final Intent intent = new Intent(this, StorageWizardReady.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                startActivity(intent);

            } else {
                // Gotta format to get there
                final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
                startActivity(intent);
            }

        } else if (mRadioInternal.isChecked()) {
            final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, true);
            startActivity(intent);
        }
    }

    /**
     * Task that classifies the contents of a mounted storage device, and sets a
     * recommended default operation based on result.
     */
    public class ClassifyTask extends AsyncTask<File, Void, Integer> {
        @Override
        protected Integer doInBackground(File... params) {
            int classes = Environment.classifyExternalStorageDirectory(params[0]);
            Log.v(TAG, "Classified " + params[0] + " as "
                    + DebugUtils.flagsToString(Environment.class, "HAS_", classes));
            return classes;
        }

        @Override
        protected void onPostExecute(Integer classes) {
            if (classes == 0) {
                // Empty is strong signal for adopt
                mRadioInternal.setChecked(true);
            } else if ((classes & (Environment.HAS_PICTURES | Environment.HAS_DCIM)) != 0) {
                // Photos is strong signal for portable
                mRadioExternal.setChecked(true);
            }
        }
    }
}
