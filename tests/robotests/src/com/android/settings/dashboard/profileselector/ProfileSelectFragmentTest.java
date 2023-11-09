/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.dashboard.profileselector;

import static android.content.Intent.EXTRA_USER_ID;

import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.EXTRA_PROFILE;
import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.PERSONAL_TAB;
import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.PRIVATE_TAB;
import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.WORK_TAB;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Flags;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragmentTest;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ProfileSelectFragmentTest {

    private Context mContext;
    private TestProfileSelectFragment mFragment;
    private FragmentActivity mActivity;
    private ShadowUserManager mUserManager;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(SettingsActivity.class).get());
        mFragment = spy(new TestProfileSelectFragment());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        mUserManager = ShadowUserManager.getShadow();
    }

    @Test
    public void getTabId_no_setCorrectTab() {
        assertThat(mFragment.getTabId(mActivity, null)).isEqualTo(PERSONAL_TAB);
    }

    @Test
    public void getTabId_setArgumentWork_setCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB, WORK_TAB);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(WORK_TAB);
    }

    @Test
    public void getTabId_setArgumentPrivate_setCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB, PRIVATE_TAB);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(PRIVATE_TAB);
    }

    @Test
    public void getTabId_setArgumentPersonal_setCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB, PERSONAL_TAB);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(PERSONAL_TAB);
    }

    @Test
    public void getTabId_setWorkId_getCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_USER_ID, 10);
        final Set<Integer> profileIds = new HashSet<>();
        profileIds.add(10);
        mUserManager.setManagedProfiles(profileIds);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(WORK_TAB);
    }

    @Test
    public void getTabId_setPrivateId_getCorrectTab() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_USER_ID, 11);
        mUserManager.setPrivateProfile(11, "private", 0);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(PRIVATE_TAB);
    }

    @Test
    public void getTabId_setPersonalId_getCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_USER_ID, 0);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(PERSONAL_TAB);
    }

    @Test
    public void getTabId_setPersonalIdByIntent_getCorrectTab() {
        final Set<Integer> profileIds = new HashSet<>();
        profileIds.add(10);
        mUserManager.setManagedProfiles(profileIds);
        final Intent intent = spy(new Intent());
        when(intent.getContentUserHint()).thenReturn(10);
        when(mActivity.getIntent()).thenReturn(intent);

        assertThat(mFragment.getTabId(mActivity, null)).isEqualTo(WORK_TAB);
    }

    @Test
    public void testGetFragments_whenOnlyPersonal_returnsOneFragment() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                mContext,
                null /* bundle */,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new);
        assertThat(fragments).hasLength(1);
    }

    @Test
    public void testGetFragments_whenPrivateDisabled_returnsOneFragment() {
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                mContext,
                null /* bundle */,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                new ProfileSelectFragment.PrivateSpaceInfoProvider() {
                    @Override
                    public boolean isPrivateSpaceLocked(Context context) {
                        return true;
                    }
                },
                new ProfileSelectFragment.ManagedProfileInfoProvider() {
                    @Override
                    public UserHandle getManagedProfile(Context context) {
                        return null;
                    }
                });
        assertThat(fragments).hasLength(1);
    }

    @Test
    public void testGetFragments_whenPrivateEnabled_returnsTwoFragments() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                mContext,
                null /* bundle */,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                new ProfileSelectFragment.PrivateSpaceInfoProvider() {
                    @Override
                    public boolean isPrivateSpaceLocked(Context context) {
                        return false;
                    }
                },
                new ProfileSelectFragment.ManagedProfileInfoProvider() {
                    @Override
                    public UserHandle getManagedProfile(Context context) {
                        return null;
                    }
                });
        assertThat(fragments).hasLength(2);
    }

    @Test
    public void testGetFragments_whenManagedProfile_returnsTwoFragments() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                mContext,
                null /* bundle */,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                new ProfileSelectFragment.PrivateSpaceInfoProvider() {
                    @Override
                    public boolean isPrivateSpaceLocked(Context context) {
                        return false;
                    }
                },
                new ProfileSelectFragment.ManagedProfileInfoProvider() {
                    @Override
                    public UserHandle getManagedProfile(Context context) {
                        return new UserHandle(123);
                    }
                });
        assertThat(fragments).hasLength(2);
    }

    @Test
    public void testGetFragments_whenAllProfiles_returnsThreeFragments() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                mContext,
                null /* bundle */,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                new ProfileSelectFragment.PrivateSpaceInfoProvider() {
                    @Override
                    public boolean isPrivateSpaceLocked(Context context) {
                        return false;
                    }
                },
                new ProfileSelectFragment.ManagedProfileInfoProvider() {
                    @Override
                    public UserHandle getManagedProfile(Context context) {
                        return new UserHandle(123);
                    }
                });
        assertThat(fragments).hasLength(3);
    }

    @Test
    public void testGetFragments_whenAvailableBundle_returnsFragmentsWithCorrectBundles() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Bundle bundle = new Bundle();
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                mContext,
                bundle,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                TestProfileSelectFragment::new,
                new ProfileSelectFragment.PrivateSpaceInfoProvider() {
                    @Override
                    public boolean isPrivateSpaceLocked(Context context) {
                        return false;
                    }
                },
                new ProfileSelectFragment.ManagedProfileInfoProvider() {
                    @Override
                    public UserHandle getManagedProfile(Context context) {
                        return new UserHandle(123);
                    }
                });
        assertThat(fragments).hasLength(3);
        assertThat(fragments[0].getArguments().getInt(EXTRA_PROFILE))
                .isEqualTo(ProfileSelectFragment.ProfileType.PERSONAL);
        assertThat(fragments[1].getArguments().getInt(EXTRA_PROFILE))
                .isEqualTo(ProfileSelectFragment.ProfileType.WORK);
        assertThat(fragments[2].getArguments().getInt(EXTRA_PROFILE))
                .isEqualTo(ProfileSelectFragment.ProfileType.PRIVATE);
    }

    public static class TestProfileSelectFragment extends ProfileSelectFragment {

        @Override
        public Fragment[] getFragments() {
            return new Fragment[]{
                    new SettingsPreferenceFragmentTest.TestFragment(), //0
                    new SettingsPreferenceFragmentTest.TestFragment(),
                    new SettingsPreferenceFragmentTest.TestFragment()
            };
        }
    }
}
