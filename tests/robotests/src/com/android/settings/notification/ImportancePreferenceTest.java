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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceViewHolder;

@RunWith(RobolectricTestRunner.class)
public class ImportancePreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        assertThat(preference.getLayoutResource()).isEqualTo(
                R.layout.notif_importance_preference);
    }

    @Test
    public void onBindViewHolder_nonConfigurable() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        Drawable unselected = mock(Drawable.class);
        Drawable selected = mock(Drawable.class);
        preference.selectedBackground = selected;
        preference.unselectedBackground = unselected;

        preference.setConfigurable(false);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.silence).isEnabled()).isFalse();
        assertThat(holder.itemView.findViewById(R.id.alert).isEnabled()).isFalse();

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(selected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground())
                .isEqualTo(unselected);

        // other button
        preference.setImportance(IMPORTANCE_LOW);
        holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(unselected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground()).isEqualTo(selected);
    }

    @Test
    public void onBindViewHolder_selectButtonAndText() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        Drawable unselected = mock(Drawable.class);
        Drawable selected = mock(Drawable.class);
        preference.selectedBackground = selected;
        preference.unselectedBackground = unselected;

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);

        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(selected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground())
                .isEqualTo(unselected);
        assertThat(((TextView) holder.itemView.findViewById(R.id.description)).getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_default));
    }

    @Test
    public void onClick_changesUICallsListener() {
        final ImportancePreference preference = spy(new ImportancePreference(mContext));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        Drawable unselected = mock(Drawable.class);
        Drawable selected = mock(Drawable.class);
        preference.selectedBackground = selected;
        preference.unselectedBackground = unselected;

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        Button silenceButton = holder.itemView.findViewById(R.id.silence);

        silenceButton.callOnClick();

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(unselected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground()).isEqualTo(selected);
        assertThat(((TextView) holder.itemView.findViewById(R.id.description)).getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_low));

        verify(preference, times(1)).callChangeListener(IMPORTANCE_LOW);
    }

    @Test
    public void setImportanceSummary_status() {
        TextView tv = new TextView(mContext);

        final ImportancePreference preference = spy(new ImportancePreference(mContext));

        preference.setDisplayInStatusBar(true);
        preference.setDisplayOnLockscreen(false);

        preference.setImportanceSummary(tv, IMPORTANCE_LOW);

        assertThat(tv.getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_low_status));
    }

    @Test
    public void setImportanceSummary_lock() {
        TextView tv = new TextView(mContext);

        final ImportancePreference preference = spy(new ImportancePreference(mContext));

        preference.setDisplayInStatusBar(false);
        preference.setDisplayOnLockscreen(true);

        preference.setImportanceSummary(tv, IMPORTANCE_LOW);

        assertThat(tv.getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_low_lock));
    }

    @Test
    public void setImportanceSummary_statusLock() {
        TextView tv = new TextView(mContext);

        final ImportancePreference preference = spy(new ImportancePreference(mContext));

        preference.setDisplayInStatusBar(true);
        preference.setDisplayOnLockscreen(true);

        preference.setImportanceSummary(tv, IMPORTANCE_LOW);

        assertThat(tv.getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_low_status_lock));
    }

    @Test
    public void setImportanceSummary_statusLock_default() {
        TextView tv = new TextView(mContext);

        final ImportancePreference preference = spy(new ImportancePreference(mContext));

        preference.setDisplayInStatusBar(true);
        preference.setDisplayOnLockscreen(true);

        preference.setImportanceSummary(tv, IMPORTANCE_DEFAULT);

        assertThat(tv.getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_default));
    }
}
