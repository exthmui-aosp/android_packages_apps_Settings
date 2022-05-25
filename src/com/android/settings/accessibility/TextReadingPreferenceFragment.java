/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.TextReadingResetController.ResetStateListener;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Accessibility settings for adjusting the system features which are related to the reading. For
 * example, bold text, high contrast text, display size, font size and so on.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class TextReadingPreferenceFragment extends DashboardFragment {
    public static final String EXTRA_LAUNCHED_FROM = "launched_from";
    private static final String TAG = "TextReadingPreferenceFragment";
    private static final String CATEGORY_FOR_ANYTHING_ELSE =
            "com.android.settings.suggested.category.DISPLAY_SETTINGS";
    static final String FONT_SIZE_KEY = "font_size";
    static final String DISPLAY_SIZE_KEY = "display_size";
    static final String BOLD_TEXT_KEY = "toggle_force_bold_text";
    static final String HIGH_TEXT_CONTRAST_KEY = "toggle_high_text_contrast_preference";
    static final String RESET_KEY = "reset";
    private static final String PREVIEW_KEY = "preview";
    private static final String NEED_RESET_SETTINGS = "need_reset_settings";
    private FontWeightAdjustmentPreferenceController mFontWeightAdjustmentController;
    private int mEntryPoint = EntryPoint.UNKNOWN_ENTRY;

    /**
     * The entry point which launches the {@link TextReadingPreferenceFragment}.
     *
     * <p>This should only be used for logging.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            EntryPoint.UNKNOWN_ENTRY,
            EntryPoint.SUW_VISION_SETTINGS,
            EntryPoint.SUW_ANYTHING_ELSE,
            EntryPoint.DISPLAY_SETTINGS,
            EntryPoint.ACCESSIBILITY_SETTINGS,
    })
    @interface EntryPoint {
        int UNKNOWN_ENTRY = 0;
        int SUW_VISION_SETTINGS = 1;
        int SUW_ANYTHING_ELSE = 2;
        int DISPLAY_SETTINGS = 3;
        int ACCESSIBILITY_SETTINGS = 4;
    }

    @VisibleForTesting
    List<ResetStateListener> mResetStateListeners;

    @VisibleForTesting
    boolean mNeedResetSettings;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mNeedResetSettings = false;
        mResetStateListeners = getResetStateListeners();

        if (icicle != null && icicle.getBoolean(NEED_RESET_SETTINGS)) {
            mResetStateListeners.forEach(ResetStateListener::resetState);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final View rootView = getActivity().getWindow().peekDecorView();
        if (rootView != null) {
            rootView.setAccessibilityPaneTitle(getString(
                    R.string.accessibility_text_reading_options_title));
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_text_reading_options;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        updateEntryPoint();

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final FontSizeData fontSizeData = new FontSizeData(context);
        final DisplaySizeData displaySizeData = createDisplaySizeData(context);

        final TextReadingPreviewController previewController = new TextReadingPreviewController(
                context, PREVIEW_KEY, fontSizeData, displaySizeData);
        previewController.setEntryPoint(mEntryPoint);
        controllers.add(previewController);

        final PreviewSizeSeekBarController fontSizeController = new PreviewSizeSeekBarController(
                context, FONT_SIZE_KEY, fontSizeData);
        fontSizeController.setInteractionListener(previewController);
        controllers.add(fontSizeController);

        final PreviewSizeSeekBarController displaySizeController = new PreviewSizeSeekBarController(
                context, DISPLAY_SIZE_KEY, displaySizeData);
        displaySizeController.setInteractionListener(previewController);
        controllers.add(displaySizeController);

        mFontWeightAdjustmentController =
                new FontWeightAdjustmentPreferenceController(context, BOLD_TEXT_KEY);
        mFontWeightAdjustmentController.setEntryPoint(mEntryPoint);
        controllers.add(mFontWeightAdjustmentController);

        final HighTextContrastPreferenceController highTextContrastController =
                new HighTextContrastPreferenceController(context, HIGH_TEXT_CONTRAST_KEY);
        highTextContrastController.setEntryPoint(mEntryPoint);
        controllers.add(highTextContrastController);

        final TextReadingResetController resetController =
                new TextReadingResetController(context, RESET_KEY,
                        v -> showDialog(DialogEnums.DIALOG_RESET_SETTINGS));
        resetController.setEntryPoint(mEntryPoint);
        controllers.add(resetController);

        return controllers;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DialogEnums.DIALOG_RESET_SETTINGS) {
            return new AlertDialog.Builder(getPrefContext())
                    .setTitle(R.string.accessibility_text_reading_confirm_dialog_title)
                    .setMessage(R.string.accessibility_text_reading_confirm_dialog_message)
                    .setPositiveButton(
                            R.string.accessibility_text_reading_confirm_dialog_reset_button,
                            this::onPositiveButtonClicked)
                    .setNegativeButton(R.string.cancel, /* listener= */ null)
                    .create();
        }

        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DialogEnums.DIALOG_RESET_SETTINGS) {
            return SettingsEnums.DIALOG_RESET_SETTINGS;
        }

        return super.getDialogMetricsCategory(dialogId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mNeedResetSettings) {
            outState.putBoolean(NEED_RESET_SETTINGS, true);
        }
    }

    @VisibleForTesting
    DisplaySizeData createDisplaySizeData(Context context) {
        return new DisplaySizeData(context);
    }

    private void updateEntryPoint() {
        final Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(EXTRA_LAUNCHED_FROM)) {
            mEntryPoint = bundle.getInt(EXTRA_LAUNCHED_FROM, EntryPoint.UNKNOWN_ENTRY);
            return;
        }

        final Intent intent = getIntent();
        if (intent == null) {
            mEntryPoint = EntryPoint.UNKNOWN_ENTRY;
            return;
        }

        final Set<String> categories = intent.getCategories();
        mEntryPoint = categories != null && categories.contains(CATEGORY_FOR_ANYTHING_ELSE)
                ? EntryPoint.SUW_ANYTHING_ELSE : EntryPoint.UNKNOWN_ENTRY;
    }

    private void onPositiveButtonClicked(DialogInterface dialog, int which) {
        // To avoid showing the dialog again, probably the onDetach() of SettingsDialogFragment
        // was interrupted by unexpectedly recreating the activity.
        removeDialog(DialogEnums.DIALOG_RESET_SETTINGS);

        if (mFontWeightAdjustmentController.isChecked()) {
            // TODO(b/228956791): Consider replacing or removing it once the root cause is
            //  clarified and the better method is available.
            // Probably has the race condition issue between "Bold text" and  the other features
            // including "Display Size", “Font Size” if they would be enabled at the same time,
            // so our workaround is that the “Bold text” would be reset first and then do the
            // remaining to avoid flickering problem.
            mNeedResetSettings = true;
            mFontWeightAdjustmentController.resetState();
        } else {
            mResetStateListeners.forEach(ResetStateListener::resetState);
        }

        Toast.makeText(getPrefContext(), R.string.accessibility_text_reading_reset_message,
                Toast.LENGTH_SHORT).show();
    }

    private List<ResetStateListener> getResetStateListeners() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        getPreferenceControllers().forEach(controllers::addAll);
        return controllers.stream().filter(c -> c instanceof ResetStateListener).map(
                c -> (ResetStateListener) c).collect(Collectors.toList());
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_text_reading_options);
}
