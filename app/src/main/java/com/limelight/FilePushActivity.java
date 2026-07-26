package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.binding.PlatformBinding;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.computers.IdentityManager;
import com.limelight.nvstream.filetransfer.DesktopFileUploader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilePushActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DesktopFileUpload");
        thread.setDaemon(true);
        return thread;
    });
    private List<Uri> sharedUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sharedUris = collectSharedUris(getIntent());
        if (sharedUris.isEmpty()) {
            Toast.makeText(this, "没有收到可推送的文件", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        showHostPicker();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showHostPicker() {
        List<ComputerDetails> pairedHosts = new ArrayList<>();
        ComputerDatabaseManager database = new ComputerDatabaseManager(this);
        try {
            for (ComputerDetails computer : database.getAllComputers()) {
                if (computer.serverCert != null && selectAddress(computer) != null) {
                    pairedHosts.add(computer);
                }
            }
        } finally {
            database.close();
        }

        if (pairedHosts.isEmpty()) {
            Toast.makeText(this, "没有已配对且可连接的主机",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String[] names = new String[pairedHosts.size()];
        for (int index = 0; index < pairedHosts.size(); index++) {
            names[index] = pairedHosts.get(index).name;
        }
        new AlertDialog.Builder(this)
                .setTitle("推送至哪台主机的桌面？")
                .setItems(names, (dialog, which) ->
                        beginUpload(pairedHosts.get(which)))
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void beginUpload(ComputerDetails computer) {
        final ProgressBar progress = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        final TextView status = new TextView(this);
        status.setText("正在检查文件…");
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        content.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        progressParams.topMargin =
                Math.round(16 * getResources().getDisplayMetrics().density);
        content.addView(progress, progressParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("推送至 " + computer.name)
                .setView(content)
                .setCancelable(false)
                .create();
        dialog.show();

        executor.execute(() -> {
            try {
                ComputerDetails.AddressTuple address = selectAddress(computer);
                if (address == null) {
                    throw new java.io.IOException("主机没有可用地址");
                }
                NvHTTP http = new NvHTTP(
                        address,
                        computer.httpsPort,
                        new IdentityManager(this).getUniqueId(),
                        computer.serverCert,
                        PlatformBinding.getCryptoProvider(this));
                DesktopFileUploader.upload(this, http, sharedUris,
                        (transferred, total) -> runOnUiThread(() -> {
                            int value = total == 0 ? 1000 :
                                    (int)Math.min(1000, transferred * 1000 / total);
                            progress.setProgress(value);
                            status.setText(Formatter.formatFileSize(
                                    FilePushActivity.this, transferred) + " / " +
                                    Formatter.formatFileSize(
                                            FilePushActivity.this, total));
                        }));
                runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(FilePushActivity.this,
                            "文件已推送到 " + computer.name + " 的桌面",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Throwable error) {
                LimeLog.warning("Desktop file upload failed: " + error.getMessage());
                runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(FilePushActivity.this,
                            "推送失败：" +
                                    (error.getMessage() == null ?
                                            "未知错误" :
                                            error.getMessage()),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
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

    private static List<Uri> collectSharedUris(Intent intent) {
        List<Uri> uris = new ArrayList<>();
        if (intent == null) {
            return uris;
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> values = intent.getParcelableArrayListExtra(
                    Intent.EXTRA_STREAM);
            if (values != null) {
                uris.addAll(values);
            }
        }
        else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri value = intent.getParcelableExtra(Intent.EXTRA_STREAM);
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
