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

package com.android.settings.testutils;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ComponentController;
import org.robolectric.util.ReflectionHelpers;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Controller class for driving fragment lifecycles, similar to {@link ActivityController}.
 */
// TODO(b/111195167) - Duplicated from org.robolectric.android.controller.FragmentController.
@Deprecated
public class FragmentController<F extends Fragment> extends
        ComponentController<FragmentController<F>, F> {
    private final F fragment;
    private final ActivityController<? extends FragmentActivity> activityController;

    public static <F extends Fragment> FragmentController<F> of(F fragment) {
        return of(fragment, FragmentControllerActivity.class, null, null);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment,
            Class<? extends FragmentActivity> activityClass) {
        return of(fragment, activityClass, null, null);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment, Intent intent) {
        return new FragmentController<>(fragment, FragmentControllerActivity.class, intent);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment, Bundle arguments) {
        return new FragmentController<>(fragment, FragmentControllerActivity.class, arguments);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment, Intent intent,
            Bundle arguments) {
        return new FragmentController<>(fragment, FragmentControllerActivity.class, intent,
                arguments);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment,
            Class<? extends FragmentActivity> activityClass, Intent intent) {
        return new FragmentController<>(fragment, activityClass, intent);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment,
            Class<? extends FragmentActivity> activityClass, Bundle arguments) {
        return new FragmentController<>(fragment, activityClass, arguments);
    }

    public static <F extends Fragment> FragmentController<F> of(F fragment,
            Class<? extends FragmentActivity> activityClass,
            Intent intent, Bundle arguments) {
        return new FragmentController<>(fragment, activityClass, intent, arguments);
    }

    private FragmentController(F fragment, Class<? extends FragmentActivity> activityClass,
            Intent intent) {
        this(fragment, activityClass, intent, null);
    }

    private FragmentController(F fragment, Class<? extends FragmentActivity> activityClass,
            Bundle arguments) {
        this(fragment, activityClass, null, arguments);
    }

    private FragmentController(F fragment, Class<? extends FragmentActivity> activityClass,
            Intent intent, Bundle arguments) {
        super(fragment, intent);
        this.fragment = fragment;
        if (arguments != null) {
            this.fragment.setArguments(arguments);
        }
        this.activityController = ActivityController.of(
                ReflectionHelpers.callConstructor(activityClass), intent);
    }

    /**
     * Creates the activity with {@link Bundle} and adds the fragment to the view with ID {@code
     * contentViewId}.
     */
    public FragmentController<F> create(final int contentViewId, final Bundle bundle) {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.create(
                        bundle).get().getSupportFragmentManager().beginTransaction().add(
                        contentViewId, fragment).commit();
            }
        });
        return this;
    }

    /**
     * Creates the activity with {@link Bundle} and adds the fragment to it. Note that the fragment
     * will be added to the view with ID 1.
     */
    public FragmentController<F> create(Bundle bundle) {
        return create(1, bundle);
    }

    @Override
    public FragmentController<F> create() {
        return create(null);
    }

    @Override
    public FragmentController<F> destroy() {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.destroy();
            }
        });
        return this;
    }

    public FragmentController<F> start() {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.start();
            }
        });
        return this;
    }

    public FragmentController<F> resume() {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.resume();
            }
        });
        return this;
    }

    public FragmentController<F> pause() {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.pause();
            }
        });
        return this;
    }

    public FragmentController<F> visible() {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.visible();
            }
        });
        return this;
    }

    public FragmentController<F> stop() {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.stop();
            }
        });
        return this;
    }

    public FragmentController<F> saveInstanceState(final Bundle outState) {
        shadowMainLooper.runPaused(new Runnable() {
            @Override
            public void run() {
                activityController.saveInstanceState(outState);
            }
        });
        return this;
    }

    private static class FragmentControllerActivity extends FragmentActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            LinearLayout view = new LinearLayout(this);
            view.setId(1);

            setContentView(view);
        }
    }
}
