package com.limelight;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.core.content.IntentCompat;

import com.limelight.ui.filepush.FilePushController;
import com.limelight.ui.filepush.FilePushTarget;
import com.limelight.ui.filepush.android.AndroidFilePushHostCatalog;
import com.limelight.ui.filepush.android.AndroidFilePushUploader;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;

import java.util.ArrayList;
import java.util.List;

public class FilePushActivity extends ComponentActivity {
    private FilePushController controller;
    private BackNavigationRegistration backNavigationRegistration;

    private TextView titleView;
    private TextView subtitleView;
    private TextView selectionTitleView;
    private TextView selectionDetailView;
    private LinearLayout hostSection;
    private LinearLayout hostList;
    private LinearLayout progressPanel;
    private ProgressBar progressBar;
    private TextView progressStatusView;
    private TextView progressBytesView;
    private TextView errorView;
    private LinearLayout actions;
    private TextView primaryAction;
    private TextView secondaryAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_file_push);
        UiHelper.notifyNewRootView(this);
        UiHelper.setStatusBarLightMode(getWindow(), false);
        backNavigationRegistration =
                BackNavigationRegistration.register(this, this::handleBackNavigation);

        bindViews();
        constrainPanelWidth();

        List<Uri> sharedUris = collectSharedUris(getIntent());
        selectionTitleView.setText(getString(
                R.string.file_push_selection_count, sharedUris.size()));
        selectionDetailView.setText(R.string.file_push_selection_hint);

        if (sharedUris.isEmpty()) {
            showTerminalError(getString(R.string.file_push_no_shared_items));
            return;
        }
        controller = FilePushController.create(
                new AndroidFilePushHostCatalog(this),
                new AndroidFilePushUploader(this, sharedUris),
                this::runOnUiThread);
        showHostPicker();
    }

    @Override
    protected void onDestroy() {
        if (backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }
        if (controller != null) {
            controller.destroy();
            controller = null;
        }
        super.onDestroy();
    }

    private void handleBackNavigation() {
        if (controller != null &&
                controller.isUploadInProgress()) {
            UiToast.makeText(this, R.string.file_push_in_progress,
                    UiToast.LENGTH_SHORT).show();
            return;
        }
        finish();
    }

    private void bindViews() {
        titleView = findViewById(R.id.file_push_title);
        subtitleView = findViewById(R.id.file_push_subtitle);
        selectionTitleView = findViewById(R.id.file_push_selection_title);
        selectionDetailView = findViewById(R.id.file_push_selection_detail);
        hostSection = findViewById(R.id.file_push_host_section);
        hostList = findViewById(R.id.file_push_host_list);
        progressPanel = findViewById(R.id.file_push_progress_panel);
        progressBar = findViewById(R.id.file_push_progress);
        progressStatusView = findViewById(R.id.file_push_progress_status);
        progressBytesView = findViewById(R.id.file_push_progress_bytes);
        errorView = findViewById(R.id.file_push_error);
        actions = findViewById(R.id.file_push_actions);
        primaryAction = findViewById(R.id.file_push_primary_action);
        secondaryAction = findViewById(R.id.file_push_secondary_action);
    }

    private void constrainPanelWidth() {
        View scrollView = findViewById(R.id.file_push_scroll);
        int availableWidth = getResources().getDisplayMetrics().widthPixels -
                UiHelper.dpToPx(this, 32);
        int width = Math.min(availableWidth, UiHelper.dpToPx(this, 520));
        ViewGroup.LayoutParams params = scrollView.getLayoutParams();
        params.width = Math.max(width, 1);
        scrollView.setLayoutParams(params);
    }

    private void showHostPicker() {
        titleView.setText(R.string.desktop_file_share_target);
        subtitleView.setText(R.string.file_push_choose_host);
        hostSection.setVisibility(View.VISIBLE);
        hostList.removeAllViews();
        progressPanel.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressBar.setProgress(0);
        progressStatusView.setText(R.string.file_push_preparing);
        progressBytesView.setText(R.string.file_push_keep_open);
        errorView.setVisibility(View.GONE);
        actions.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.GONE);
        primaryAction.setVisibility(View.VISIBLE);
        primaryAction.setText(R.string.file_push_cancel);
        primaryAction.setOnClickListener(view -> finish());

        if (controller == null ||
                controller.loadHosts(this::onHostsLoaded) !=
                        FilePushController.RequestStatus.ACCEPTED) {
            showTerminalError(getString(
                    R.string.file_push_unknown_error));
        }
    }

    private void onHostsLoaded(
            FilePushController.Result<List<FilePushTarget>> result) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (!result.isSuccessful()) {
            showTerminalError(errorMessage(result.getFailure()));
            return;
        }

        List<FilePushTarget> pairedHosts = result.getValue();
        progressPanel.setVisibility(View.GONE);
        hostList.removeAllViews();
        if (pairedHosts.isEmpty()) {
            showTerminalError(getString(R.string.file_push_no_hosts));
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < pairedHosts.size(); index++) {
            FilePushTarget target = pairedHosts.get(index);
            View item = inflater.inflate(
                    R.layout.item_file_push_host, hostList, false);
            TextView name = item.findViewById(R.id.file_push_host_name);
            TextView detail = item.findViewById(R.id.file_push_host_detail);

            name.setText(target.getDisplayName());
            detail.setText(getString(
                    R.string.file_push_host_paired,
                    target.getAddress()));
            item.setContentDescription(getString(
                    R.string.file_push_host_content_description,
                    target.getDisplayName()));
            item.setOnClickListener(view -> beginUpload(target));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (index > 0) {
                params.topMargin = UiHelper.dpToPx(this, 7);
            }
            hostList.addView(item, params);
        }

        if (hostList.getChildCount() > 0) {
            hostList.getChildAt(0).requestFocus();
        }
    }

    private void beginUpload(FilePushTarget target) {
        if (controller == null) {
            return;
        }
        FilePushController.RequestStatus status = controller.upload(
                target,
                new FilePushController.UploadCallback() {
                    @Override
                    public void onProgress(
                            long transferredBytes,
                            long totalBytes) {
                        updateProgress(
                                transferredBytes,
                                totalBytes);
                    }

                    @Override
                    public void onCompleted(
                            FilePushController.Result<FilePushTarget>
                                    result) {
                        if (result.isSuccessful()) {
                            showUploadComplete(
                                    result.getValue().getDisplayName());
                        }
                        else {
                            LimeLog.warning(
                                    "Desktop file upload failed: " +
                                            result.getFailure()
                                                    .getClass()
                                                    .getSimpleName());
                            showUploadError(errorMessage(
                                    result.getFailure()));
                        }
                    }
                });
        if (status != FilePushController.RequestStatus.ACCEPTED) {
            return;
        }

        titleView.setText(R.string.file_push_uploading_title);
        subtitleView.setText(getString(
                R.string.file_push_uploading_to,
                target.getDisplayName()));
        hostSection.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        progressPanel.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressBar.setProgress(0);
        progressStatusView.setText(R.string.file_push_preparing);
        progressBytesView.setText(R.string.file_push_keep_open);
        actions.setVisibility(View.GONE);
    }

    private void updateProgress(long transferred, long total) {
        if (controller == null ||
                !controller.isUploadInProgress() ||
                isFinishing() || isDestroyed()) {
            return;
        }
        if (total > 0) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress((int) Math.min(
                    1000, transferred * 1000 / total));
            progressBytesView.setText(getString(
                    R.string.file_transfer_progress_bytes,
                    Formatter.formatFileSize(this, transferred),
                    Formatter.formatFileSize(this, total)));
        } else {
            progressBar.setIndeterminate(true);
            progressBytesView.setText(getString(
                    R.string.file_transfer_progress_transferred,
                    Formatter.formatFileSize(this, transferred)));
        }
    }

    private void showUploadComplete(String computerName) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        titleView.setText(R.string.file_push_complete_title);
        subtitleView.setText(getString(
                R.string.file_push_complete_message, computerName));
        progressBar.setIndeterminate(false);
        progressBar.setProgress(1000);
        progressStatusView.setText(R.string.file_push_complete_title);
        actions.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.GONE);
        primaryAction.setVisibility(View.VISIBLE);
        primaryAction.setText(R.string.file_transfer_done);
        primaryAction.setOnClickListener(view -> finish());
        primaryAction.requestFocus();
    }

    private void showUploadError(String message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        titleView.setText(R.string.file_push_failed_title);
        subtitleView.setText(R.string.file_push_choose_host);
        hostSection.setVisibility(View.GONE);
        progressPanel.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(getString(R.string.file_push_failed_message, message));
        actions.setVisibility(View.VISIBLE);

        secondaryAction.setVisibility(View.VISIBLE);
        secondaryAction.setText(R.string.file_transfer_close);
        secondaryAction.setOnClickListener(view -> finish());
        primaryAction.setVisibility(View.VISIBLE);
        primaryAction.setText(R.string.file_push_retry);
        primaryAction.setOnClickListener(view -> showHostPicker());
        primaryAction.requestFocus();
    }

    private void showTerminalError(String message) {
        titleView.setText(R.string.file_push_failed_title);
        subtitleView.setText(R.string.desktop_file_share_target);
        hostSection.setVisibility(View.GONE);
        progressPanel.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(message);
        actions.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.GONE);
        primaryAction.setVisibility(View.VISIBLE);
        primaryAction.setText(R.string.file_transfer_close);
        primaryAction.setOnClickListener(view -> finish());
        primaryAction.requestFocus();
    }

    private String errorMessage(Exception failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? getString(R.string.file_push_unknown_error)
                : message;
    }

    private static List<Uri> collectSharedUris(Intent intent) {
        List<Uri> uris = new ArrayList<>();
        if (intent == null) {
            return uris;
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> values =
                    IntentCompat.getParcelableArrayListExtra(
                            intent,
                            Intent.EXTRA_STREAM,
                            Uri.class);
            if (values != null) {
                uris.addAll(values);
            }
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri value = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri.class);
            if (value != null) {
                uris.add(value);
            }
        }
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int index = 0; index < clipData.getItemCount(); index++) {
                Uri value = clipData.getItemAt(index).getUri();
                if (value != null && !uris.contains(value)) {
                    uris.add(value);
                }
            }
        }
        return uris;
    }
}
