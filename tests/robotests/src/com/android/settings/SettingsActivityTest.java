/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.core.OnActivityResultListener;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class SettingsActivityTest {

    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private ActivityManager.TaskDescription mTaskDescription;
    private SettingsActivity mActivity;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mActivity = spy(new SettingsActivity());
    }

    @Test
    public void onCreate_deviceNotProvisioned_shouldDisableSearch() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        final Intent intent = new Intent(mContext, Settings.class);
        final SettingsActivity activity =
            Robolectric.buildActivity(SettingsActivity.class, intent).create(Bundle.EMPTY).get();

        assertThat(activity.findViewById(R.id.search_bar).getVisibility())
            .isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onCreate_deviceProvisioned_shouldEnableSearch() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        final Intent intent = new Intent(mContext, Settings.class);
        final SettingsActivity activity =
            Robolectric.buildActivity(SettingsActivity.class, intent).create(Bundle.EMPTY).get();

        assertThat(activity.findViewById(R.id.search_bar).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void launchSettingFragment_nullExtraShowFragment_shouldNotCrash() {
        when(mActivity.getSupportFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));

        doReturn(RuntimeEnvironment.application.getClassLoader()).when(mActivity).getClassLoader();

        mActivity.launchSettingFragment(null, true, mock(Intent.class));
    }

    @Test
    public void setTaskDescription_shouldUpdateIcon() {
        mActivity.setTaskDescription(mTaskDescription);

        verify(mTaskDescription).setIcon(anyInt());
    }

    @Test
    public void onActivityResult_shouldDelegateToListener() {
        final List<Fragment> fragments = new ArrayList<>();
        fragments.add(new Fragment());
        fragments.add(new ListenerFragment());

        final FragmentManager manager = mock(FragmentManager.class);
        when(mActivity.getSupportFragmentManager()).thenReturn(manager);
        when(manager.getFragments()).thenReturn(fragments);

        mActivity.onActivityResult(0, 0, new Intent());

        assertThat(((ListenerFragment) fragments.get(1)).mOnActivityResultCalled).isTrue();
    }

    public static class ListenerFragment extends Fragment implements OnActivityResultListener {

        public boolean mOnActivityResultCalled;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            mOnActivityResultCalled = true;
        }
    }
}
