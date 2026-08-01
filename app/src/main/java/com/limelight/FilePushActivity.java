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
import com.limelight.utils.UiToast;

import com.limelight.binding.PlatformBinding;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.computers.LegacyHostDetailsAdapter;
import com.limelight.computers.model.PersistedHost;
import com.limelight.computers.IdentityManager;
import com.limelight.nvstream.filetransfer.DesktopFileUploader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.UiHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FilePushActivity extends ComponentActivity {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "DesktopFileUpload");
                thread.setDaemon(true);
                return thread;
            });

    private List<Uri> sharedUris;
    private Future<?> uploadTask;
    private boolean uploadInProgress;
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

        sharedUris = collectSharedUris(getIntent());
        selectionTitleView.setText(getString(
                R.string.file_push_selection_count, sharedUris.size()));
        selectionDetailView.setText(R.string.file_push_selection_hint);

        if (sharedUris.isEmpty()) {
            showTerminalError(getString(R.string.file_push_no_shared_items));
            return;
        }
        showHostPicker();
    }

    @Override
    protected void onDestroy() {
        if (backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }
        if (uploadTask != null && !uploadTask.isDone()) {
            uploadTask.cancel(true);
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    private void handleBackNavigation() {
        if (uploadInProgress) {
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
        uploadInProgress = false;
        titleView.setText(R.string.desktop_file_share_target);
        subtitleView.setText(R.string.file_push_choose_host);
        hostSection.setVisibility(View.VISIBLE);
        progressPanel.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        actions.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.GONE);
        primaryAction.setVisibility(View.VISIBLE);
        primaryAction.setText(R.string.file_push_cancel);
        primaryAction.setOnClickListener(view -> finish());

        List<ComputerDetails> pairedHosts = loadPairedHosts();
        hostList.removeAllViews();
        if (pairedHosts.isEmpty()) {
            showTerminalError(getString(R.string.file_push_no_hosts));
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < pairedHosts.size(); index++) {
            ComputerDetails computer = pairedHosts.get(index);
            View item = inflater.inflate(
                    R.layout.item_file_push_host, hostList, false);
            TextView name = item.findViewById(R.id.file_push_host_name);
            TextView detail = item.findViewById(R.id.file_push_host_detail);
            ComputerDetails.AddressTuple address = selectAddress(computer);

            String computerName = hostDisplayName(computer, address);
            name.setText(computerName);
            detail.setText(getString(
                    computer.state == ComputerDetails.State.ONLINE ?
                            R.string.file_push_host_online :
                            R.string.file_push_host_paired,
                    address.address));
            item.setContentDescription(getString(
                    R.string.file_push_host_content_description, computerName));
            item.setOnClickListener(view -> beginUpload(computer));

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

    private List<ComputerDetails> loadPairedHosts() {
        List<ComputerDetails> pairedHosts = new ArrayList<>();
        ComputerDatabaseManager database = new ComputerDatabaseManager(this);
        try {
            for (PersistedHost host : database.getAllHosts()) {
                ComputerDetails computer = LegacyHostDetailsAdapter
                        .toComputerDetails(host);
                if (computer.serverCert != null &&
                        selectAddress(computer) != null) {
                    pairedHosts.add(computer);
                }
            }
        } finally {
            database.close();
        }
        Collections.sort(pairedHosts, new Comparator<ComputerDetails>() {
            @Override
            public int compare(
                    ComputerDetails left, ComputerDetails right) {
                String leftName = left.name == null ? "" : left.name;
                String rightName = right.name == null ? "" : right.name;
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return pairedHosts;
    }

    private void beginUpload(ComputerDetails computer) {
        if (uploadInProgress) {
            return;
        }
        ComputerDetails.AddressTuple address = selectAddress(computer);
        if (address == null) {
            showUploadError(getString(R.string.file_push_no_hosts));
            return;
        }

        uploadInProgress = true;
        titleView.setText(R.string.file_push_uploading_title);
        String computerName = hostDisplayName(computer, address);
        subtitleView.setText(getString(
                R.string.file_push_uploading_to, computerName));
        hostSection.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        progressPanel.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressBar.setProgress(0);
        progressStatusView.setText(R.string.file_push_preparing);
        progressBytesView.setText(R.string.file_push_keep_open);
        actions.setVisibility(View.GONE);

        uploadTask = executor.submit(() -> {
            try {
                NvHTTP http = new NvHTTP(
                        address,
                        computer.httpsPort,
                        new IdentityManager(this).getUniqueId(),
                        computer.serverCert,
                        PlatformBinding.getCryptoProvider(this));
                DesktopFileUploader.upload(this, http, sharedUris,
                        (transferred, total) -> runOnUiThread(() ->
                                updateProgress(transferred, total)));
                runOnUiThread(() -> showUploadComplete(computerName));
            } catch (Throwable error) {
                LimeLog.warning(
                        "Desktop file upload failed: " + error.getMessage());
                runOnUiThread(() -> showUploadError(
                        error.getMessage() == null ?
                                getString(R.string.file_push_unknown_error) :
                                error.getMessage()));
            }
        });
    }

    private void updateProgress(long transferred, long total) {
        if (!uploadInProgress || isFinishing() || isDestroyed()) {
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
        uploadInProgress = false;
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
        uploadInProgress = false;
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
        uploadInProgress = false;
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

    private static ComputerDetails.AddressTuple selectAddress(
            ComputerDetails computer) {
        if (computer.activeAddress != null) {
            return computer.activeAddress;
        }
        if (computer.manualAddress != null) {
            return computer.manualAddress;
        }
        if (computer.localAddress != null) {
            return computer.localAddress;
        }
        if (computer.ipv6Address != null) {
            return computer.ipv6Address;
        }
        return computer.remoteAddress;
    }

    private static String hostDisplayName(
            ComputerDetails computer,
            ComputerDetails.AddressTuple address) {
        if (computer.name != null && !computer.name.trim().isEmpty()) {
            return computer.name;
        }
        return address.address;
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
