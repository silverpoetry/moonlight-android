package com.limelight.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;

import java.util.Objects;

/** Lifecycle-bound presenter for settings value editors. */
final class SettingsDialogPresenter {
    interface Listener {
        void onListValueSelected(SettingsItem item, String value);

        void onSliderValueSelected(SettingsItem item, int value);

        /** Returns a user-facing validation error, or {@code null}. */
        CharSequence onTextValueSubmitted(
                SettingsItem item,
                String value);
    }

    private final Activity activity;
    private final SettingsStore store;
    private final Listener listener;
    private AlertDialog activeDialog;
    private boolean destroyed;

    SettingsDialogPresenter(
            Activity activity,
            SettingsStore store,
            Listener listener) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.store = Objects.requireNonNull(store, "store");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    void showList(SettingsItem item) {
        if (destroyed) {
            return;
        }
        AlertDialog dialog = newDialog();
        LinearLayout panel = createDialogPanel(item.title);
        String current = store.getString(item);

        for (int index = 0; index < item.entryValues.length; index++) {
            String value = item.entryValues[index].toString();
            TextView row = createDialogRow(
                    item.entries[index],
                    value.equals(current));
            row.setOnClickListener(view -> {
                listener.onListValueSelected(item, value);
                dialog.dismiss();
            });
            panel.addView(row);
        }

        addDialogCancel(panel, dialog);
        showDialog(dialog, panel);
    }

    void showSlider(SettingsItem item) {
        if (destroyed) {
            return;
        }
        AlertDialog dialog = newDialog();
        LinearLayout panel = createDialogPanel(item.title);
        addDialogMessage(panel, item.dialogMessage);

        TextView valueText = new TextView(activity);
        valueText.setGravity(Gravity.CENTER);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(28);
        valueText.setTypeface(null, Typeface.BOLD);
        panel.addView(valueText);

        SeekBar seekBar = new SeekBar(activity);
        seekBar.setMax(item.max);
        if (item.keyStep > 0) {
            seekBar.setKeyProgressIncrement(item.keyStep);
        }
        seekBar.setProgress(item.round(store.getInt(item)));
        panel.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar source,
                            int progress,
                            boolean fromUser) {
                        int rounded = item.round(progress);
                        if (rounded != progress) {
                            source.setProgress(rounded);
                            return;
                        }
                        valueText.setText(
                                item.formatSliderValue(rounded));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar source) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar source) {
                    }
                });
        valueText.setText(item.formatSliderValue(seekBar.getProgress()));

        LinearLayout buttons = createDialogButtonRow();
        TextView cancel = createDialogButton(
                activity.getString(R.string.settings_cancel));
        TextView confirm = createDialogButton(
                activity.getString(R.string.settings_ok));
        buttons.addView(cancel);
        buttons.addView(confirm);
        panel.addView(buttons);
        cancel.setOnClickListener(view -> dialog.dismiss());
        confirm.setOnClickListener(view -> {
            listener.onSliderValueSelected(
                    item,
                    item.round(seekBar.getProgress()));
            dialog.dismiss();
        });

        showDialog(dialog, panel);
    }

    void showText(SettingsItem item) {
        if (destroyed) {
            return;
        }
        AlertDialog dialog = newDialog();
        LinearLayout panel = createDialogPanel(item.title);
        addDialogMessage(panel, item.dialogMessage);

        EditText input = new EditText(activity);
        input.setText(store.getText(item));
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0x88FFFFFF);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (item.isCustomBitrateEditor()) {
            input.setInputType(
                    InputType.TYPE_CLASS_NUMBER |
                            InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setFilters(new InputFilter[] {
                    new InputFilter.LengthFilter(5),
            });
        }
        panel.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = createDialogButtonRow();
        TextView cancel = createDialogButton(
                activity.getString(R.string.settings_cancel));
        TextView confirm = createDialogButton(
                activity.getString(R.string.settings_ok));
        buttons.addView(cancel);
        buttons.addView(confirm);
        panel.addView(buttons);
        cancel.setOnClickListener(view -> dialog.dismiss());
        confirm.setOnClickListener(view -> {
            String value = input.getText().toString();
            CharSequence validationError =
                    listener.onTextValueSubmitted(item, value);
            if (validationError == null) {
                dialog.dismiss();
            }
            else {
                UiToast.makeText(
                        activity,
                        validationError,
                        UiToast.LENGTH_SHORT).show();
            }
        });

        showDialog(dialog, panel);
        input.requestFocus();
    }

    void destroy() {
        destroyed = true;
        if (activeDialog != null) {
            activeDialog.setOnDismissListener(null);
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    boolean isShowing() {
        return activeDialog != null && activeDialog.isShowing();
    }

    private AlertDialog newDialog() {
        if (activeDialog != null) {
            activeDialog.dismiss();
        }
        return new AlertDialog.Builder(activity).create();
    }

    private LinearLayout createDialogPanel(CharSequence title) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_update_dialog_panel);
        panel.setPadding(dp(18), dp(16), dp(18), dp(14));

        TextView titleView = new TextView(activity);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(19);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, dp(12));
        panel.addView(titleView);
        return panel;
    }

    private void addDialogMessage(
            LinearLayout panel,
            CharSequence messageText) {
        if (TextUtils.isEmpty(messageText)) {
            return;
        }
        TextView message = new TextView(activity);
        message.setText(messageText);
        message.setTextColor(0xCCFFFFFF);
        message.setTextSize(13);
        message.setPadding(0, 0, 0, dp(10));
        panel.addView(message);
    }

    private TextView createDialogRow(
            CharSequence text,
            boolean selected) {
        TextView row = new TextView(activity);
        row.setText(selected ? "✓  " + text : "    " + text);
        row.setTextColor(Color.WHITE);
        row.setTextSize(15);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setSingleLine(false);
        row.setBackgroundResource(selected
                ? R.drawable.bg_settings_selected_card
                : R.drawable.ic_game_menu_btn_selector);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(7);
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout createDialogButtonRow() {
        LinearLayout buttons = new LinearLayout(activity);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(14), 0, 0);
        return buttons;
    }

    private TextView createDialogButton(String text) {
        TextView button = new TextView(activity);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(
                R.drawable.ic_game_menu_btn_selector);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(dp(92), dp(40));
        params.setMarginStart(dp(10));
        button.setLayoutParams(params);
        return button;
    }

    private void addDialogCancel(
            LinearLayout panel,
            AlertDialog dialog) {
        LinearLayout buttons = createDialogButtonRow();
        TextView cancel = createDialogButton(
                activity.getString(R.string.settings_cancel));
        buttons.addView(cancel);
        panel.addView(buttons);
        cancel.setOnClickListener(view -> dialog.dismiss());
    }

    private void showDialog(AlertDialog dialog, View panel) {
        activeDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) {
                activeDialog = null;
            }
        });
        dialog.setView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private int dp(float value) {
        return UiHelper.dpToPx(activity, value);
    }
}
