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

package com.android.settings.localepicker;

import android.os.LocaleList;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocalePicker;

import java.util.Locale;

public class LocaleFeatureProviderImpl implements LocaleFeatureProvider {
    @Override
    public String getLocaleNames() {
        final LocaleList locales = LocalePicker.getLocales();
        Locale[] arrLocalesWithoutExtension = new Locale[locales.size()];
        for (int i = 0; i < locales.size(); i++) {
            arrLocalesWithoutExtension[i] = locales.get(i).stripExtensions();
        }
        final Locale displayLocale = Locale.getDefault();
        return LocaleHelper.toSentenceCase(
                LocaleHelper.getDisplayLocaleList(
                        new LocaleList(arrLocalesWithoutExtension),
                        displayLocale,
                        2 /* Show up to two locales from the list */),
                displayLocale);
    }
}
