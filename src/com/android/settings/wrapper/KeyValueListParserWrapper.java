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

package com.android.settings.wrapper;

import android.util.KeyValueListParser;

/**
 * This class replicates a subset of the {@link KeyValueListParser}. The class
 * exists so that we can use a thin wrapper around the PM in production code and a mock in tests.
 * We cannot directly mock or shadow the {@link KeyValueListParser}, because some of the methods
 * we rely on are newer than the API version supported by Robolectric.
 */
public class KeyValueListParserWrapper {
    private KeyValueListParser mParser;

    public KeyValueListParserWrapper(KeyValueListParser parser) {
        mParser = parser;
    }

    /**
     * Get real {@link KeyValueListParser}
     */
    public KeyValueListParser getKeyValueListParser() {
        return mParser;
    }

    /**
     * Resets the parser with a new string to parse. The string is expected to be in the following
     * format:
     * <pre>key1=value,key2=value,key3=value</pre>
     *
     * where the delimiter is a comma.
     *
     * @param str the string to parse.
     * @throws IllegalArgumentException if the string is malformed.
     */
    public void setString(String str) throws IllegalArgumentException {
        mParser.setString(str);
    }

    /**
     * Get the value for key as a boolean.
     * @param key The key to lookup.
     * @param defaultValue The value to return if the key was not found.
     * @return the boolean value associated with the key.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return mParser.getBoolean(key, defaultValue);
    }

    /**
     * Get the value for key as a string.
     * @param key The key to lookup.
     * @param defaultValue The value to return if the key was not found.
     * @return the string value associated with the key.
     */
    public String getString(String key, String defaultValue) {
        return mParser.getString(key, defaultValue);
    }

    /**
     * Get the value for key as a long.
     * @param key The key to lookup.
     * @param defaultValue The value to return if the key was not found, or the value was not a
     *                     long.
     * @return the long value associated with the key.
     */
    public long getLong(String key, long defaultValue) {
        return mParser.getLong(key, defaultValue);
    }
}
