package com.limelight.preferences;

import android.app.Activity;
import android.app.Dialog;

import com.limelight.ui.compose.settings.MaterialSettingsDialogFactory;

import java.util.Objects;

/** Lifecycle-bound presenter for Material 3 settings value editors. */
final class SettingsDialogPresenter {
    interface Listener {
        void onListValueSelected(SettingsItem item, String value);

        void onSliderValueSelected(SettingsItem item, int value);

        /** Returns a user-facing validation error, or {@code null}. */
        CharSequence onTextValueSubmitted(SettingsItem item, String value);
    }

    private final SettingsStore store;
    private final Listener listener;
    private final MaterialSettingsDialogFactory dialogFactory;
    private Dialog activeDialog;
    private boolean destroyed;

    SettingsDialogPresenter(
            Activity activity,
            SettingsStore store,
            Listener listener) {
        this.store = Objects.requireNonNull(store, "store");
        this.listener = Objects.requireNonNull(listener, "listener");
        dialogFactory = new MaterialSettingsDialogFactory(
                Objects.requireNonNull(activity, "activity"));
    }

    void showList(SettingsItem item) {
        if (destroyed) {
            return;
        }
        dismissActiveDialog();
        String current = item.type == SettingsItem.Type.INTEGER_LIST
                ? Integer.toString(store.getInt(item))
                : store.getString(item);
        activeDialog = dialogFactory.showList(
                item.title,
                item.entries,
                item.entryValues,
                current,
                value -> listener.onListValueSelected(item, value));
        trackDismissal(activeDialog);
    }

    void showSlider(SettingsItem item) {
        if (destroyed) {
            return;
        }
        dismissActiveDialog();
        activeDialog = dialogFactory.showSlider(
                item.title,
                item.dialogMessage,
                item.min,
                item.max,
                item.step,
                item.round(store.getInt(item)),
                item::formatSliderValue,
                item::round,
                value -> listener.onSliderValueSelected(item, value));
        trackDismissal(activeDialog);
    }

    void showText(SettingsItem item) {
        if (destroyed) {
            return;
        }
        dismissActiveDialog();
        activeDialog = dialogFactory.showText(
                item.title,
                item.dialogMessage,
                store.getText(item),
                item.isCustomBitrateEditor(),
                item.isCustomBitrateEditor() ? 5 : 0,
                value -> listener.onTextValueSubmitted(item, value));
        trackDismissal(activeDialog);
    }

    void destroy() {
        destroyed = true;
        dismissActiveDialog();
    }

    boolean isShowing() {
        return activeDialog != null && activeDialog.isShowing();
    }

    private void trackDismissal(Dialog dialog) {
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) {
                activeDialog = null;
            }
        });
    }

    private void dismissActiveDialog() {
        if (activeDialog != null) {
            activeDialog.setOnDismissListener(null);
            activeDialog.dismiss();
            activeDialog = null;
        }
    }
}
