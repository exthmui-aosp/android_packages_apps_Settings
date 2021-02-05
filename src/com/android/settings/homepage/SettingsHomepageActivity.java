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

package com.android.settings.homepage;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.accounts.AvatarViewMixin;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.HideNonSystemOverlayMixin;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.overlay.FeatureFactory;

public class SettingsHomepageActivity extends FragmentActivity {

    private static final String TAG = "SettingsHomepageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_homepage_container);

        final View appBar = findViewById(R.id.app_bar_container);
        appBar.setMinimumHeight(getSearchBoxHeight());
        initHomepageContainer();

        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this /* activity */, toolbar, SettingsEnums.SETTINGS_HOMEPAGE);

        final ImageView avatarView = findViewById(R.id.account_avatar);
        getLifecycle().addObserver(new AvatarViewMixin(this, avatarView));
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));

        if (!getSystemService(ActivityManager.class).isLowRamDevice()) {
            // Only allow contextual features on high ram devices.
            if (FeatureFlagUtils.isEnabled(this, FeatureFlags.SILKY_HOME)) {
                showSuggestionFragment();
            }
            if (FeatureFlagUtils.isEnabled(this, FeatureFlags.CONTEXTUAL_HOME)) {
                showFragment(new ContextualCardsFragment(), R.id.contextual_cards_content);
            }
        }
        showFragment(new TopLevelSettings(), R.id.main_content);
        ((FrameLayout) findViewById(R.id.main_content))
                .getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    }

    private void showSuggestionFragment() {
        final Class<? extends Fragment> fragment = FeatureFactory.getFactory(this)
                .getSuggestionFeatureProvider(this).getContextualSuggestionFragment();
        if (fragment == null) {
            return;
        }

        try {
            showFragment(fragment.newInstance(), R.id.contextual_suggestion_content);
        } catch (IllegalAccessException | InstantiationException e) {
            Log.w(TAG, "Cannot show fragment", e);
        }
    }

    private void showFragment(Fragment fragment, int id) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final Fragment showFragment = fragmentManager.findFragmentById(id);

        if (showFragment == null) {
            fragmentTransaction.add(id, fragment);
        } else {
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
    }

    private void initHomepageContainer() {
        final View view = findViewById(R.id.homepage_container);
        // Prevent inner RecyclerView gets focus and invokes scrolling.
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    private int getSearchBoxHeight() {
        final int searchBarHeight = getResources().getDimensionPixelSize(R.dimen.search_bar_height);
        final int searchBarMarginTop = getResources().getDimensionPixelSize(
                R.dimen.search_bar_margin);
        final int searchBarMarginBottom = getResources().getDimensionPixelSize(
                R.dimen.search_bar_margin_bottom);
        return searchBarHeight + searchBarMarginTop + searchBarMarginBottom;
    }
}
