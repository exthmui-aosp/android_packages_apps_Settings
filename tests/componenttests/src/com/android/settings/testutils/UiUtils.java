/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.Activity;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public class UiUtils {

    public static void waitUntilCondition(long timeoutInMillis, Supplier<Boolean> condition) {
        long start = System.nanoTime();
        while (System.nanoTime() - start < (timeoutInMillis * 1000000)) {
            try {
                //Eat NPE from condition because there's a concurrency issue that when calling
                //findViewById when the view hierarchy is still rendering, it sometimes encounter
                //null views that may exist few milliseconds before, and causes a NPE.
                if (condition.get()) {
                    return;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean waitForActivitiesInStage(long timeoutInMillis, Stage stage) {
        final Collection<Activity> activities = new ArrayList<>();
        waitUntilCondition(Constants.ACTIVITY_LAUNCH_WAIT_TIMEOUT, () -> {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activities.addAll(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                            Stage.RESUMED)));
            return activities.size() > 0;
        });

        return activities.size() > 0;
    }
}
