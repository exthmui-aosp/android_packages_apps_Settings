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
 *
 */

package com.android.settings.search.indexing;

import android.provider.SearchIndexableData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Holds Data sources for indexable data.
 * TODO (b/33577327) add getters and setters for data.
 */
public class PreIndexData {
    public List<SearchIndexableData> dataToUpdate;
    public Map<String, Set<String>> nonIndexableKeys;

    public PreIndexData() {
        dataToUpdate = new ArrayList<>();
        nonIndexableKeys = new HashMap<>();
    }

    public PreIndexData(PreIndexData other) {
        dataToUpdate = new ArrayList<>(other.dataToUpdate);
        nonIndexableKeys = new HashMap<>(other.nonIndexableKeys);
    }

    public PreIndexData copy() {
        return new PreIndexData(this);
    }

    public void clear() {
        dataToUpdate.clear();
        nonIndexableKeys.clear();
    }
}
