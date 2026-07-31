package com.limelight.ui.clipboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Formatter;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import com.limelight.R;
import com.limelight.nvstream.NvConnection;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.transfer.TransferSettingsLoader;
import com.limelight.utils.FileUriUtils;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;

import java.util.Objects;

/**
 * Owns the Storage Access Framework and progress UI for remote clipboard file
 * downloads during a streaming Activity.
 */
public final class RemoteClipboardFileTransferController {
    private static final int DIRECTORY_REQUEST_CODE = 1107;

    private final Activity activity;
    private final NvConnection connection;
    private final SettingsRepository settingsRepository;
    private final ClipboardFileTransferSession session =
            new ClipboardFileTransferSession();
    private AlertDialog transferDialog;

    public RemoteClipboardFileTransferController(
            Activity activity,
            NvConnection connection,
            SettingsRepository settingsRepository) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.connection = Objects.requireNonNull(connection, "connection");
        this.settingsRepository = Objects.requireNonNull(
                settingsRepository,
                "settingsRepository");
    }

    public void pullRemoteFiles() {
        if (session.isTransferInProgress()) {
            UiToast.makeText(
                    activity,
                    R.string.clipboard_file_pull_in_progress,
                    UiToast.LENGTH_SHORT).show();
            return;
        }

        TransferSettings settings =
                TransferSettingsLoader.load(settingsRepository);
        String configured =
                settings.getClipboardFileDirectoryUri();
        if (settings.hasClipboardFileDirectory()) {
            try {
                Uri directory = Uri.parse(configured);
                DocumentFile document =
                        DocumentFile.fromTreeUri(activity, directory);
                if (document != null &&
                        document.isDirectory() &&
                        document.canWrite()) {
                    download(directory);
                    return;
                }
            }
            catch (RuntimeException ignored) {
                // The persisted provider or URI can disappear between runs.
            }
            settingsRepository.edit()
                    .remove(TransferSettingKeys
                            .CLIPBOARD_FILE_DIRECTORY_URI)
                    .apply();
        }

        if (!session.beginDirectorySelection()) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try {
            activity.startActivityForResult(intent, DIRECTORY_REQUEST_CODE);
        }
        catch (RuntimeException error) {
            session.endDirectorySelection();
            UiToast.makeText(
                    activity,
                    "无法打开目录选择器",
                    UiToast.LENGTH_LONG).show();
        }
    }

    public boolean onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (requestCode != DIRECTORY_REQUEST_CODE) {
            return false;
        }

        session.endDirectorySelection();
        if (resultCode != Activity.RESULT_OK ||
                data == null ||
                data.getData() == null) {
            return true;
        }

        Uri directory = data.getData();
        try {
            if (!FileUriUtils.persistUriPermission(
                    activity, data, directory)) {
                throw new SecurityException(
                        "Document provider returned no persistable URI permission");
            }
            settingsRepository.edit()
                    .put(
                            TransferSettingKeys
                                    .CLIPBOARD_FILE_DIRECTORY_URI,
                            directory.toString())
                    .apply();
            download(directory);
        }
        catch (SecurityException error) {
            UiToast.makeText(
                    activity,
                    "无法保留该目录的访问权限",
                    UiToast.LENGTH_LONG).show();
        }
        return true;
    }

    public boolean isSelectingDirectory() {
        return session.isSelectingDirectory();
    }

    public void destroy() {
        session.destroy();
        dismissTransferDialog();
    }

    private void download(Uri directory) {
        long generation = session.beginTransfer();
        if (generation == 0) {
            return;
        }

        String destinationName = getDestinationName(directory);
        TransferDialogViews views = createTransferDialog(destinationName);
        connection.downloadRemoteClipboardFiles(
                directory,
                new NvConnection.ClipboardFileDownloadListener() {
                    @Override
                    public void onProgress(long transferredBytes,
                                           long totalBytes) {
                        if (!isCurrentTransfer(generation, views.dialog)) {
                            return;
                        }
                        showProgress(
                                views,
                                transferredBytes,
                                totalBytes);
                    }

                    @Override
                    public void onComplete(int topLevelItemCount) {
                        if (!isCurrentTransfer(generation, views.dialog) ||
                                !session.finishTransfer(generation)) {
                            return;
                        }
                        showComplete(
                                views,
                                destinationName,
                                topLevelItemCount);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isCurrentTransfer(generation, views.dialog) ||
                                !session.finishTransfer(generation)) {
                            return;
                        }
                        showError(views, directory, message);
                    }
                });
    }

    private TransferDialogViews createTransferDialog(
            String destinationName) {
        View dialogView = activity.getLayoutInflater().inflate(
                R.layout.dialog_clipboard_file_transfer,
                null);
        TransferDialogViews views = new TransferDialogViews(dialogView);
        views.destination.setText(activity.getString(
                R.string.clipboard_file_pull_destination,
                destinationName));
        views.progress.setIndeterminate(true);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        views.dialog = dialog;
        transferDialog = dialog;
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setBackgroundDrawableResource(
                    android.R.color.transparent);
            int availableWidth =
                    activity.getResources().getDisplayMetrics().widthPixels -
                            UiHelper.dpToPx(activity, 32);
            dialogWindow.setLayout(
                    Math.max(
                            1,
                            Math.min(
                                    availableWidth,
                                    UiHelper.dpToPx(activity, 520))),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return views;
    }

    private void showProgress(TransferDialogViews views,
                              long transferredBytes,
                              long totalBytes) {
        views.status.setText(R.string.clipboard_file_pull_progress);
        if (totalBytes > 0) {
            views.progress.setIndeterminate(false);
            views.progress.setProgress((int) Math.min(
                    1000,
                    transferredBytes * 1000 / totalBytes));
            views.bytes.setText(activity.getString(
                    R.string.file_transfer_progress_bytes,
                    Formatter.formatFileSize(
                            activity, transferredBytes),
                    Formatter.formatFileSize(
                            activity, totalBytes)));
        }
        else {
            views.progress.setIndeterminate(true);
            views.bytes.setText(activity.getString(
                    R.string.file_transfer_progress_transferred,
                    Formatter.formatFileSize(
                            activity, transferredBytes)));
        }
    }

    private void showComplete(TransferDialogViews views,
                              String destinationName,
                              int topLevelItemCount) {
        views.title.setText(
                R.string.clipboard_file_pull_complete_title);
        views.subtitle.setText(activity.getString(
                R.string.clipboard_file_pull_complete_subtitle,
                destinationName));
        views.status.setText(activity.getString(
                R.string.clipboard_file_pull_complete_count,
                topLevelItemCount));
        views.progress.setIndeterminate(false);
        views.progress.setProgress(1000);
        views.bytes.setText(activity.getString(
                R.string.clipboard_file_pull_destination,
                destinationName));
        views.actions.setVisibility(View.VISIBLE);
        views.secondaryAction.setVisibility(View.GONE);
        views.primaryAction.setText(R.string.file_transfer_done);
        views.primaryAction.setOnClickListener(
                view -> dismissTransferDialog(views.dialog));
        views.primaryAction.requestFocus();
    }

    private void showError(TransferDialogViews views,
                           Uri directory,
                           String message) {
        views.title.setText(
                R.string.clipboard_file_pull_failed_title);
        views.subtitle.setText(
                R.string.clipboard_file_pull_failed_subtitle);
        views.status.setText(message);
        views.progress.setVisibility(View.GONE);
        views.bytes.setVisibility(View.GONE);
        views.actions.setVisibility(View.VISIBLE);
        views.secondaryAction.setVisibility(View.VISIBLE);
        views.secondaryAction.setText(R.string.file_transfer_close);
        views.secondaryAction.setOnClickListener(
                view -> dismissTransferDialog(views.dialog));
        views.primaryAction.setText(R.string.file_transfer_retry);
        views.primaryAction.setOnClickListener(view -> {
            dismissTransferDialog(views.dialog);
            download(directory);
        });
        views.primaryAction.requestFocus();
    }

    private boolean isCurrentTransfer(long generation,
                                      AlertDialog dialog) {
        return session.isCurrentTransfer(generation) &&
                transferDialog == dialog &&
                !activity.isFinishing() &&
                !activity.isDestroyed();
    }

    private String getDestinationName(Uri directory) {
        try {
            DocumentFile document =
                    DocumentFile.fromTreeUri(activity, directory);
            if (document != null &&
                    document.getName() != null &&
                    !document.getName().isEmpty()) {
                return document.getName();
            }
        }
        catch (RuntimeException ignored) {
            // Use the stable fallback for an unavailable document provider.
        }
        return activity.getString(
                R.string.clipboard_file_pull_destination_unknown);
    }

    private void dismissTransferDialog(AlertDialog dialog) {
        dialog.dismiss();
        if (transferDialog == dialog) {
            transferDialog = null;
        }
    }

    private void dismissTransferDialog() {
        if (transferDialog != null) {
            transferDialog.dismiss();
            transferDialog = null;
        }
    }

    private static final class TransferDialogViews {
        final TextView title;
        final TextView subtitle;
        final TextView destination;
        final TextView status;
        final ProgressBar progress;
        final TextView bytes;
        final LinearLayout actions;
        final TextView secondaryAction;
        final TextView primaryAction;
        AlertDialog dialog;

        TransferDialogViews(View dialogView) {
            title = dialogView.findViewById(
                    R.id.clipboard_file_transfer_title);
            subtitle = dialogView.findViewById(
                    R.id.clipboard_file_transfer_subtitle);
            destination = dialogView.findViewById(
                    R.id.clipboard_file_transfer_destination);
            status = dialogView.findViewById(
                    R.id.clipboard_file_transfer_status);
            progress = dialogView.findViewById(
                    R.id.clipboard_file_transfer_progress);
            bytes = dialogView.findViewById(
                    R.id.clipboard_file_transfer_bytes);
            actions = dialogView.findViewById(
                    R.id.clipboard_file_transfer_actions);
            secondaryAction = dialogView.findViewById(
                    R.id.clipboard_file_transfer_secondary_action);
            primaryAction = dialogView.findViewById(
                    R.id.clipboard_file_transfer_primary_action);
        }
    }
}
