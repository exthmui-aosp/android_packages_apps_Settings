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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toolbar;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.accounts.AvatarViewMixin;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.overlay.FeatureFactory;

public class SettingsHomepageActivity extends SettingsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateWindowProperties();
        setContentView(R.layout.settings_homepage_container);
        setHomepageContainerPaddingTop();

        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this /* activity */, toolbar, SettingsEnums.SETTINGS_HOMEPAGE);

        final ImageView avatarView = findViewById(R.id.account_avatar);
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(this, avatarView);
        getLifecycle().addObserver(avatarViewMixin);

        if (!getSystemService(ActivityManager.class).isLowRamDevice()) {
            // Only allow contextual feature on high ram devices.
            showFragment(new ContextualCardsFragment(), R.id.contextual_cards_content);
        }
        showFragment(new TopLevelSettings(), R.id.main_content);
        ((FrameLayout) findViewById(R.id.main_content))
                .getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
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

    private void updateWindowProperties() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                decorView.getSystemUiVisibility() |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        getWindow().setStatusBarColor(getColor(R.color.homepage_status_bar_color));
    }

    @VisibleForTesting
    void setHomepageContainerPaddingTop() {
        final View view = this.findViewById(R.id.homepage_container);

        final int statusBarHeight = getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height);
        final int searchBarHeight = getResources().getDimensionPixelSize(R.dimen.search_bar_height);
        final int searchBarMargin = getResources().getDimensionPixelSize(R.dimen.search_bar_margin);

        // The top padding is the height of status bar + height of action bar(48dp) + top/bottom
        // margins(16dp)
        final int paddingTop = statusBarHeight + searchBarHeight + searchBarMargin * 2;
        view.setPadding(0 /* left */, paddingTop, 0 /* right */, 0 /* bottom */);
    }
}