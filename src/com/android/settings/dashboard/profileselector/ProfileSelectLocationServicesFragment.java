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

package com.android.settings.dashboard.profileselector;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.location.LocationServices;
import com.android.settings.location.LocationServicesForPrivateProfile;
import com.android.settings.location.LocationServicesForWork;

/**
 * Location Services page for personal/managed profile.
 */
public class ProfileSelectLocationServicesFragment extends ProfileSelectFragment {

    @Override
    public Fragment[] getFragments() {
        return ProfileSelectFragment.getFragments(
                getContext(),
                null /* bundle */,
                LocationServices::new,
                LocationServicesForWork::new,
                LocationServicesForPrivateProfile::new);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_services_header;
    }

    @Override
    protected boolean forceUpdateHeight() {
        return true;
    }
}
