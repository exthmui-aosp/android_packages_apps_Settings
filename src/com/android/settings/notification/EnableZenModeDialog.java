package com.android.settings.notification;

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

import static android.util.Log.wtf;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

public class EnableZenModeDialog extends InstrumentedDialogFragment {

    private static final String TAG = "EnableZenModeDialog";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    private static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);

    @VisibleForTesting
    public static final int FOREVER_CONDITION_INDEX = 0;
    @VisibleForTesting
    public static final int COUNTDOWN_CONDITION_INDEX = 1;
    @VisibleForTesting
    public static final int COUNTDOWN_ALARM_CONDITION_INDEX = 2;
    @VisibleForTesting
    protected Activity mActivity;

    private static final int SECONDS_MS = 1000;
    private static final int MINUTES_MS = 60 * SECONDS_MS;

    @VisibleForTesting
    protected Uri mForeverId;
    private int mBucketIndex = -1;

    private AlarmManager mAlarmManager;
    private int mUserId;
    private boolean mAttached;

    @VisibleForTesting
    protected Context mContext;

    private RadioGroup mZenRadioGroup;
    @VisibleForTesting
    protected LinearLayout mZenRadioGroupContent;
    private int MAX_MANUAL_DND_OPTIONS = 3;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        NotificationManager noMan = (NotificationManager) getContext().
                getSystemService(Context.NOTIFICATION_SERVICE);
        mContext = getContext();
        mForeverId =  Condition.newId(mContext).appendPath("forever").build();
        mAlarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        mUserId = mContext.getUserId();
        mAttached = false;

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.zen_mode_settings_turn_on_dialog_title)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_enable_dialog_turn_on,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int checkedId = mZenRadioGroup.getCheckedRadioButtonId();
                                ConditionTag tag = getConditionTagAt(checkedId);

                                if (isForever(tag.condition)) {
                                    MetricsLogger.action(getContext(),
                                            MetricsProto.MetricsEvent.
                                                    NOTIFICATION_ZEN_MODE_TOGGLE_ON_FOREVER);
                                } else if (isAlarm(tag.condition)) {
                                    MetricsLogger.action(getContext(),
                                            MetricsProto.MetricsEvent.
                                                    NOTIFICATION_ZEN_MODE_TOGGLE_ON_ALARM);
                                } else if (isCountdown(tag.condition)) {
                                    MetricsLogger.action(getContext(),
                                            MetricsProto.MetricsEvent.
                                                    NOTIFICATION_ZEN_MODE_TOGGLE_ON_COUNTDOWN);
                                } else {
                                    wtf(TAG, "Invalid manual condition: " + tag.condition);
                                }
                                // always triggers priority-only dnd with chosen condition
                                noMan.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                                        getRealConditionId(tag.condition), TAG);
                            }
                        });

        View contentView = getContentView();
        bindConditions(forever());
        builder.setView(contentView);
        return builder.create();
    }

    private void hideAllConditions() {
        final int N = mZenRadioGroupContent.getChildCount();
        for (int i = 0; i < N; i++) {
            mZenRadioGroupContent.getChildAt(i).setVisibility(View.GONE);
        }
    }

    protected View getContentView() {
        if (mActivity == null) {
            mActivity = getActivity();
        }
        final LayoutInflater inflater = mActivity.getLayoutInflater();
        View contentView = inflater.inflate(R.layout.zen_mode_turn_on_dialog_container, null);
        ScrollView container = (ScrollView) contentView.findViewById(R.id.container);

        mZenRadioGroup = container.findViewById(R.id.zen_radio_buttons);
        mZenRadioGroupContent = container.findViewById(R.id.zen_radio_buttons_content);

        for (int i = 0; i < MAX_MANUAL_DND_OPTIONS; i++) {
            final View radioButton = inflater.inflate(R.layout.zen_mode_radio_button,
                    mZenRadioGroup, false);
            mZenRadioGroup.addView(radioButton);
            radioButton.setId(i);

            final View radioButtonContent = inflater.inflate(R.layout.zen_mode_condition,
                    mZenRadioGroupContent, false);
            radioButtonContent.setId(i + MAX_MANUAL_DND_OPTIONS);
            mZenRadioGroupContent.addView(radioButtonContent);
        }
        hideAllConditions();
        return contentView;
    }

    @Override
    public int getMetricsCategory() {
       return MetricsProto.MetricsEvent.NOTIFICATION_ZEN_MODE_ENABLE_DIALOG;
    }

    @VisibleForTesting
    protected void bind(final Condition condition, final View row, final int rowId) {
        if (condition == null) throw new IllegalArgumentException("condition must not be null");
        final boolean enabled = condition.state == Condition.STATE_TRUE;
        final ConditionTag tag = row.getTag() != null ? (ConditionTag) row.getTag() :
                new ConditionTag();
        row.setTag(tag);
        final boolean first = tag.rb == null;
        if (tag.rb == null) {
            tag.rb = (RadioButton) mZenRadioGroup.getChildAt(rowId);
        }
        tag.condition = condition;
        final Uri conditionId = getConditionId(tag.condition);
        if (DEBUG) Log.d(TAG, "bind i=" + mZenRadioGroupContent.indexOfChild(row) + " first="
                + first + " condition=" + conditionId);
        tag.rb.setEnabled(enabled);
        tag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tag.rb.setChecked(true);
                    if (DEBUG) Log.d(TAG, "onCheckedChanged " + conditionId);
                    MetricsLogger.action(mContext,
                            MetricsProto.MetricsEvent.QS_DND_CONDITION_SELECT);
                    announceConditionSelection(tag);
                }
            }
        });

        updateUi(tag, row, condition, enabled, rowId, conditionId);
        row.setVisibility(View.VISIBLE);
    }

    @VisibleForTesting
    protected ConditionTag getConditionTagAt(int index) {
        return (ConditionTag) mZenRadioGroupContent.getChildAt(index).getTag();
    }

    @VisibleForTesting
    protected void bindConditions(Condition c) {
        // forever
        bind(forever(), mZenRadioGroupContent.getChildAt(FOREVER_CONDITION_INDEX),
                FOREVER_CONDITION_INDEX);
        if (c == null) {
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isForever(c)) {
            getConditionTagAt(FOREVER_CONDITION_INDEX).rb.setChecked(true);
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else {
            if (isAlarm(c)) {
                bindGenericCountdown();
                bindNextAlarm(c);
                getConditionTagAt(COUNTDOWN_ALARM_CONDITION_INDEX).rb.setChecked(true);
            } else if (isCountdown(c)) {
                bindNextAlarm(getTimeUntilNextAlarmCondition());
                bind(c, mZenRadioGroupContent.getChildAt(COUNTDOWN_CONDITION_INDEX),
                        COUNTDOWN_CONDITION_INDEX);
                getConditionTagAt(COUNTDOWN_CONDITION_INDEX).rb.setChecked(true);
            } else {
                wtf(TAG, "Invalid manual condition: " + c);
            }
        }
    }

    public static Uri getConditionId(Condition condition) {
        return condition != null ? condition.id : null;
    }

    public Condition forever() {
        Uri foreverId = Condition.newId(mContext).appendPath("forever").build();
        return new Condition(foreverId, foreverSummary(mContext), "", "", 0 /*icon*/,
                Condition.STATE_TRUE, 0 /*flags*/);
    }

    public long getNextAlarm() {
        final AlarmManager.AlarmClockInfo info = mAlarmManager.getNextAlarmClock(mUserId);
        return info != null ? info.getTriggerTime() : 0;
    }

    @VisibleForTesting
    protected boolean isAlarm(Condition c) {
        return c != null && ZenModeConfig.isValidCountdownToAlarmConditionId(c.id);
    }

    @VisibleForTesting
    protected boolean isCountdown(Condition c) {
        return c != null && ZenModeConfig.isValidCountdownConditionId(c.id);
    }

    private boolean isForever(Condition c) {
        return c != null && mForeverId.equals(c.id);
    }

    private Uri getRealConditionId(Condition condition) {
        return isForever(condition) ? null : getConditionId(condition);
    }

    private String foreverSummary(Context context) {
        return context.getString(com.android.internal.R.string.zen_mode_forever);
    }

    private static void setToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    // Returns a time condition if the next alarm is within the next week.
    @VisibleForTesting
    protected Condition getTimeUntilNextAlarmCondition() {
        GregorianCalendar weekRange = new GregorianCalendar();
        setToMidnight(weekRange);
        weekRange.add(Calendar.DATE, 6);
        final long nextAlarmMs = getNextAlarm();
        if (nextAlarmMs > 0) {
            GregorianCalendar nextAlarm = new GregorianCalendar();
            nextAlarm.setTimeInMillis(nextAlarmMs);
            setToMidnight(nextAlarm);

            if (weekRange.compareTo(nextAlarm) >= 0) {
                return ZenModeConfig.toNextAlarmCondition(mContext, nextAlarmMs,
                        ActivityManager.getCurrentUser());
            }
        }
        return null;
    }

    @VisibleForTesting
    protected void bindGenericCountdown() {
        mBucketIndex = DEFAULT_BUCKET_INDEX;
        Condition countdown = ZenModeConfig.toTimeCondition(mContext,
                MINUTE_BUCKETS[mBucketIndex], ActivityManager.getCurrentUser());
        if (!mAttached || getConditionTagAt(COUNTDOWN_CONDITION_INDEX).condition == null) {
            bind(countdown, mZenRadioGroupContent.getChildAt(COUNTDOWN_CONDITION_INDEX),
                    COUNTDOWN_CONDITION_INDEX);
        }
    }

    private void updateUi(ConditionTag tag, View row, Condition condition,
            boolean enabled, int rowId, Uri conditionId) {
        if (tag.lines == null) {
            tag.lines = row.findViewById(android.R.id.content);
        }
        if (tag.line1 == null) {
            tag.line1 = (TextView) row.findViewById(android.R.id.text1);
        }

        if (tag.line2 == null) {
            tag.line2 = (TextView) row.findViewById(android.R.id.text2);
        }

        final String line1 = !TextUtils.isEmpty(condition.line1) ? condition.line1
                : condition.summary;
        final String line2 = condition.line2;
        tag.line1.setText(line1);
        if (TextUtils.isEmpty(line2)) {
            tag.line2.setVisibility(View.GONE);
        } else {
            tag.line2.setVisibility(View.VISIBLE);
            tag.line2.setText(line2);
        }
        tag.lines.setEnabled(enabled);
        tag.lines.setAlpha(enabled ? 1 : .4f);

        tag.lines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tag.rb.setChecked(true);
            }
        });

        // minus button
        final ImageView button1 = (ImageView) row.findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTimeButton(row, tag, false /*down*/, rowId);
            }
        });

        // plus button
        final ImageView button2 = (ImageView) row.findViewById(android.R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTimeButton(row, tag, true /*up*/, rowId);
            }
        });

        final long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        if (rowId == COUNTDOWN_CONDITION_INDEX && time > 0) {
            button1.setVisibility(View.VISIBLE);
            button2.setVisibility(View.VISIBLE);
            if (mBucketIndex > -1) {
                button1.setEnabled(mBucketIndex > 0);
                button2.setEnabled(mBucketIndex < MINUTE_BUCKETS.length - 1);
            } else {
                final long span = time - System.currentTimeMillis();
                button1.setEnabled(span > MIN_BUCKET_MINUTES * MINUTES_MS);
                final Condition maxCondition = ZenModeConfig.toTimeCondition(mContext,
                        MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser());
                button2.setEnabled(!Objects.equals(condition.summary, maxCondition.summary));
            }

            button1.setAlpha(button1.isEnabled() ? 1f : .5f);
            button2.setAlpha(button2.isEnabled() ? 1f : .5f);
        } else {
            button1.setVisibility(View.GONE);
            button2.setVisibility(View.GONE);
        }
    }

    @VisibleForTesting
    protected void bindNextAlarm(Condition c) {
        View alarmContent = mZenRadioGroupContent.getChildAt(COUNTDOWN_ALARM_CONDITION_INDEX);
        ConditionTag tag = (ConditionTag) alarmContent.getTag();

        if (c != null && (!mAttached || tag == null || tag.condition == null)) {
            bind(c, alarmContent, COUNTDOWN_ALARM_CONDITION_INDEX);
        }

        // hide the alarm radio button if there isn't a "next alarm condition"
        tag = (ConditionTag) alarmContent.getTag();
        boolean showAlarm = tag != null && tag.condition != null;
        mZenRadioGroup.getChildAt(COUNTDOWN_ALARM_CONDITION_INDEX).setVisibility(
                showAlarm ? View.VISIBLE : View.GONE);
        alarmContent.setVisibility(showAlarm ? View.VISIBLE : View.GONE);
    }

    private void onClickTimeButton(View row, ConditionTag tag, boolean up, int rowId) {
        MetricsLogger.action(mContext, MetricsProto.MetricsEvent.QS_DND_TIME, up);
        Condition newCondition = null;
        final int N = MINUTE_BUCKETS.length;
        if (mBucketIndex == -1) {
            // not on a known index, search for the next or prev bucket by time
            final Uri conditionId = getConditionId(tag.condition);
            final long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
            final long now = System.currentTimeMillis();
            for (int i = 0; i < N; i++) {
                int j = up ? i : N - 1 - i;
                final int bucketMinutes = MINUTE_BUCKETS[j];
                final long bucketTime = now + bucketMinutes * MINUTES_MS;
                if (up && bucketTime > time || !up && bucketTime < time) {
                    mBucketIndex = j;
                    newCondition = ZenModeConfig.toTimeCondition(mContext,
                            bucketTime, bucketMinutes, ActivityManager.getCurrentUser(),
                            false /*shortVersion*/);
                    break;
                }
            }
            if (newCondition == null) {
                mBucketIndex = DEFAULT_BUCKET_INDEX;
                newCondition = ZenModeConfig.toTimeCondition(mContext,
                        MINUTE_BUCKETS[mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            // on a known index, simply increment or decrement
            mBucketIndex = Math.max(0, Math.min(N - 1, mBucketIndex + (up ? 1 : -1)));
            newCondition = ZenModeConfig.toTimeCondition(mContext,
                    MINUTE_BUCKETS[mBucketIndex], ActivityManager.getCurrentUser());
        }
        bind(newCondition, row, rowId);
        tag.rb.setChecked(true);
        announceConditionSelection(tag);
    }

    private void announceConditionSelection(ConditionTag tag) {
        // condition will always be priority-only
        String modeText = mContext.getString(R.string.zen_interruption_level_priority);
        if (tag.line1 != null) {
            mZenRadioGroupContent.announceForAccessibility(mContext.getString(
                    R.string.zen_mode_and_condition, modeText, tag.line1.getText()));
        }
    }

    // used as the view tag on condition rows
    @VisibleForTesting
    protected static class ConditionTag {
        public RadioButton rb;
        public View lines;
        public TextView line1;
        public TextView line2;
        public Condition condition;
    }
}
