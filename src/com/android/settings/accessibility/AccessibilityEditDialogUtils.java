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

package com.android.settings.accessibility;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.android.settings.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility class for creating the edit dialog.
 */
public class AccessibilityEditDialogUtils {

    /**
     * IntDef enum for dialog type that indicates different dialog for user to choose the shortcut
     * type.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DialogType.EDIT_SHORTCUT_GENERIC,
            DialogType.EDIT_SHORTCUT_MAGNIFICATION,
    })

    private @interface DialogType {
        int EDIT_SHORTCUT_GENERIC = 0;
        int EDIT_SHORTCUT_MAGNIFICATION = 1;
    }

    /**
     * Method to show the edit shortcut dialog.
     *
     * @param context A valid context
     * @param dialogTitle The title of edit shortcut dialog
     * @param listener The listener to determine the action of edit shortcut dialog
     * @return A edit shortcut dialog for showing
     */
    public static AlertDialog showEditShortcutDialog(Context context, CharSequence dialogTitle,
            DialogInterface.OnClickListener listener) {
        final AlertDialog alertDialog = createDialog(context, DialogType.EDIT_SHORTCUT_GENERIC,
                dialogTitle, listener);
        alertDialog.show();

        return alertDialog;
    }

    /**
     * Method to show the edit shortcut dialog in Magnification.
     *
     * @param context A valid context
     * @param dialogTitle The title of edit shortcut dialog
     * @param listener The listener to determine the action of edit shortcut dialog
     * @return A edit shortcut dialog for showing in Magnification
     */
    public static AlertDialog showMagnificationEditShortcutDialog(Context context,
            CharSequence dialogTitle, DialogInterface.OnClickListener listener) {
        final AlertDialog alertDialog = createDialog(context,
                DialogType.EDIT_SHORTCUT_MAGNIFICATION, dialogTitle, listener);
        alertDialog.show();

        return alertDialog;
    }

    private static AlertDialog createDialog(Context context, int dialogType,
            CharSequence dialogTitle, DialogInterface.OnClickListener listener) {

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createEditDialogContentView(context, dialogType))
                .setTitle(dialogTitle)
                .setPositiveButton(R.string.save, listener)
                .setNegativeButton(R.string.cancel,
                        (DialogInterface dialog, int which) -> dialog.dismiss())
                .create();

        return alertDialog;
    }

    /**
     * Get a content View for the edit shortcut dialog.
     *
     * @param context A valid context
     * @param dialogType The type of edit shortcut dialog
     * @return A content view suitable for viewing
     */
    private static View createEditDialogContentView(Context context, int dialogType) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View contentView = null;

        switch (dialogType) {
            case DialogType.EDIT_SHORTCUT_GENERIC:
                contentView = inflater.inflate(
                        R.layout.accessibility_edit_shortcut, null);
                initSoftwareShortcut(context, contentView);
                initHardwareShortcut(context, contentView);
                break;
            case DialogType.EDIT_SHORTCUT_MAGNIFICATION:
                contentView = inflater.inflate(
                        R.layout.accessibility_edit_shortcut_magnification, null);
                initSoftwareShortcut(context, contentView);
                initHardwareShortcut(context, contentView);
                initMagnifyShortcut(context, contentView);
                initAdvancedWidget(contentView);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return contentView;
    }

    private static void setupShortcutWidget(View view, CharSequence titleText,
            CharSequence summaryText, int imageResId) {
        final CheckBox checkBox = view.findViewById(R.id.checkbox);
        checkBox.setText(titleText);
        final TextView summary = view.findViewById(R.id.summary);
        summary.setText(summaryText);
        final ImageView image = view.findViewById(R.id.image);
        image.setImageResource(imageResId);
    }

    private static void initSoftwareShortcut(Context context, View view) {
        final View dialogView = view.findViewById(R.id.software_shortcut);
        final TextView summary = dialogView.findViewById(R.id.summary);
        final int lineHeight = summary.getLineHeight();
        setupShortcutWidget(dialogView, retrieveTitle(context),
                retrieveSummary(context, lineHeight), retrieveImageResId(context));
    }

    private static void initHardwareShortcut(Context context, View view) {
        final View dialogView = view.findViewById(R.id.hardware_shortcut);
        final String title = context.getString(
                R.string.accessibility_shortcut_edit_dialog_title_hardware);
        final String summary = context.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_hardware);
        setupShortcutWidget(dialogView, title, summary,
                R.drawable.illustration_accessibility_button);
    }

    private static void initMagnifyShortcut(Context context, View view) {
        final View dialogView = view.findViewById(R.id.triple_tap_shortcut);
        final String title = context.getString(
                R.string.accessibility_shortcut_edit_dialog_title_triple_tap);
        final String summary = context.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_triple_tap);
        setupShortcutWidget(dialogView, title, summary,
                R.drawable.illustration_accessibility_button);
    }

    private static void initAdvancedWidget(View view) {
        final LinearLayout advanced = view.findViewById(R.id.advanced_shortcut);
        final View tripleTap = view.findViewById(R.id.triple_tap_shortcut);
        advanced.setOnClickListener((View v) -> {
            advanced.setVisibility(View.GONE);
            tripleTap.setVisibility(View.VISIBLE);
        });
    }

    private static boolean isGestureNavigateEnabled(Context context) {
        return context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode)
                == NAV_BAR_MODE_GESTURAL;
    }

    private static CharSequence retrieveTitle(Context context) {
        return context.getString(isGestureNavigateEnabled(context)
                ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                : R.string.accessibility_shortcut_edit_dialog_title_software);
    }

    private static CharSequence retrieveSummary(Context context, int lineHeight) {
        return isGestureNavigateEnabled(context)
                ? context.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_gesture)
                : getSummaryStringWithIcon(context, lineHeight);
    }

    private static int retrieveImageResId(Context context) {
        return isGestureNavigateEnabled(context)
                ? R.drawable.illustration_accessibility_button
                : R.drawable.illustration_accessibility_button;
    }

    private static SpannableString getSummaryStringWithIcon(Context context, int lineHeight) {
        final String summary = context
                .getString(R.string.accessibility_shortcut_edit_dialog_summary_software);
        final SpannableString spannableMessage = SpannableString.valueOf(summary);

        // Icon
        final int indexIconStart = summary.indexOf("%s");
        final int indexIconEnd = indexIconStart + 2;
        final Drawable icon = context.getDrawable(R.drawable.ic_accessibility_new);
        final ImageSpan imageSpan = new ImageSpan(icon);
        imageSpan.setContentDescription("");
        icon.setTint(getThemeAttrColor(context, android.R.attr.textColorPrimary));
        icon.setBounds(0, 0, lineHeight, lineHeight);
        spannableMessage.setSpan(
                imageSpan, indexIconStart, indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableMessage;
    }

    /**
     * Returns the color associated with the specified attribute in the context's theme.
     */
    @ColorInt
    private static int getThemeAttrColor(final Context context, final int attributeColor) {
        final int colorResId = getAttrResourceId(context, attributeColor);
        return ContextCompat.getColor(context, colorResId);
    }

    /**
     * Returns the identifier of the resolved resource assigned to the given attribute.
     */
    private static int getAttrResourceId(final Context context, final int attributeColor) {
        final int[] attrs = {attributeColor};
        final TypedArray typedArray = context.obtainStyledAttributes(attrs);
        final int colorResId = typedArray.getResourceId(0, 0);
        typedArray.recycle();
        return colorResId;
    }
}
