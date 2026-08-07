package com.limelight.preferences;

import android.app.Activity;
import android.app.Dialog;

import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.ui.compose.settings.MaterialSettingsDialogFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lifecycle-bound presenter for Material 3 settings value editors. */
final class SettingsDialogPresenter {
    interface Listener {
        void onListValueSelected(SettingsItem item, String value);

        void onSliderValueSelected(SettingsItem item, int value);

        /** Returns a user-facing validation error, or {@code null}. */
        CharSequence onTextValueSubmitted(SettingsItem item, String value);

        /** Returns a user-facing validation error, or {@code null}. */
        CharSequence onCustomResolutionSubmitted(
                SettingsItem item,
                String value);

        void onCustomResolutionRemoved(String value);
    }

    interface ConfigurationImportListener {
        void onSelected(Set<ConfigurationArchiveComponent> components);

        void onCancelled();
    }

    private final Activity activity;
    private final SettingsStore store;
    private final Listener listener;
    private final MaterialSettingsDialogFactory dialogFactory;
    private Dialog activeDialog;
    private Runnable activeDialogDismissAction;
    private boolean destroyed;

    SettingsDialogPresenter(
            Activity activity,
            SettingsStore store,
            Listener listener) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.store = Objects.requireNonNull(store, "store");
        this.listener = Objects.requireNonNull(listener, "listener");
        dialogFactory = new MaterialSettingsDialogFactory(
                this.activity);
    }

    void showList(SettingsItem item) {
        if (destroyed) {
            return;
        }
        dismissActiveDialog();
        String current = item.type == SettingsItem.Type.INTEGER_LIST
                ? Integer.toString(store.getInt(item))
                : store.getString(item);
        if (StreamResolutionSettingKeys.RESOLUTION
                .getName()
                .equals(item.key)) {
            activeDialog = dialogFactory.showResolutionList(
                    item.title,
                    item.entries,
                    item.entryValues,
                    current,
                    store.get(StreamResolutionSettingKeys
                            .CUSTOM_RESOLUTIONS),
                    StreamResolutionCodec.DEFAULT_RESOLUTION,
                    value -> listener.onListValueSelected(item, value),
                    value -> listener.onCustomResolutionSubmitted(
                            item,
                            value),
                    listener::onCustomResolutionRemoved);
            trackDismissal(activeDialog);
            return;
        }
        switch (SettingsEditorCatalog.forItem(item)) {
            case DISCRETE_SLIDER:
                activeDialog = dialogFactory.showDiscreteListSlider(
                        item.title,
                        item.entries,
                        item.entryValues,
                        current,
                        value -> listener.onListValueSelected(item, value));
                break;
            case SEGMENTED:
                activeDialog = dialogFactory.showSegmentedList(
                        item.title,
                        item.entries,
                        item.entryValues,
                        current,
                        value -> listener.onListValueSelected(item, value));
                break;
            case LIST:
            default:
                activeDialog = dialogFactory.showList(
                        item.title,
                        item.entries,
                        item.entryValues,
                        current,
                        value -> listener.onListValueSelected(item, value));
                break;
        }
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

    void showConfigurationImport(
            Set<ConfigurationArchiveComponent> available,
            ConfigurationImportListener importListener) {
        Objects.requireNonNull(available, "available");
        Objects.requireNonNull(importListener, "importListener");
        if (destroyed || available.isEmpty()) {
            importListener.onCancelled();
            return;
        }
        dismissActiveDialog();
        ArrayList<ConfigurationArchiveComponent> components =
                new ArrayList<>();
        for (ConfigurationArchiveComponent component :
                ConfigurationArchiveComponent.values()) {
            if (available.contains(component)) {
                components.add(component);
            }
        }
        CharSequence[] entries = new CharSequence[components.size()];
        CharSequence[] summaries = new CharSequence[components.size()];
        String[] values = new String[components.size()];
        for (int index = 0; index < components.size(); index++) {
            ConfigurationArchiveComponent component = components.get(index);
            entries[index] = activity.getText(component.getTitleRes());
            summaries[index] = activity.getText(component.getSummaryRes());
            values[index] = component.getId();
        }
        AtomicBoolean submitted = new AtomicBoolean();
        activeDialog = dialogFactory.showMultiChoice(
                activity.getText(
                        com.limelight.R.string
                                .settings_import_selection_title),
                entries,
                summaries,
                values,
                selectedIds -> {
                    submitted.set(true);
                    EnumSet<ConfigurationArchiveComponent> selected =
                            EnumSet.noneOf(
                                    ConfigurationArchiveComponent.class);
                    for (String id : selectedIds) {
                        ConfigurationArchiveComponent component =
                                ConfigurationArchiveComponent.fromId(id);
                        if (component != null &&
                                available.contains(component)) {
                            selected.add(component);
                        }
                    }
                    importListener.onSelected(selected);
                });
        trackDismissal(
                activeDialog,
                () -> {
                    if (!submitted.get()) {
                        importListener.onCancelled();
                    }
                });
    }

    void destroy() {
        destroyed = true;
        dismissActiveDialog();
    }

    boolean isShowing() {
        return activeDialog != null && activeDialog.isShowing();
    }

    private void trackDismissal(Dialog dialog) {
        trackDismissal(dialog, null);
    }

    private void trackDismissal(Dialog dialog, Runnable dismissAction) {
        activeDialogDismissAction = dismissAction;
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) {
                activeDialog = null;
                Runnable action = activeDialogDismissAction;
                activeDialogDismissAction = null;
                if (action != null) {
                    action.run();
                }
            }
        });
    }

    private void dismissActiveDialog() {
        if (activeDialog != null) {
            Runnable action = activeDialogDismissAction;
            activeDialogDismissAction = null;
            activeDialog.setOnDismissListener(null);
            activeDialog.dismiss();
            activeDialog = null;
            if (action != null) {
                action.run();
            }
        }
    }

}
