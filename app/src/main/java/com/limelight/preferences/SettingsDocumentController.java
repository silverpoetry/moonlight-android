package com.limelight.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.input.accessibility.KeyboardRemappingFileStore;
import com.limelight.platform.files.AndroidPrivateFileShare;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.utils.FileUriUtils;
import com.limelight.utils.UiToast;
import com.limelight.utils.concurrent.ExclusiveTaskExecutor;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.android.AndroidVirtualControlLayoutRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Lifecycle-bound Android adapter for settings document selection and the
 * versioned configuration archive workflow.
 */
final class SettingsDocumentController {
    interface DocumentLauncher {
        void launch(Intent intent);
    }

    static final int NO_PENDING_REQUEST = 0;
    static final int REQUEST_VIRTUAL_KEYBOARD_IMPORT = 1001;
    static final int REQUEST_VIRTUAL_GAMEPAD_IMPORT = 1002;
    static final int REQUEST_CONFIGURATION_IMPORT = 1003;
    static final int REQUEST_ACCESSIBILITY_IMPORT = 1007;
    static final int REQUEST_CLIPBOARD_DIRECTORY = 1009;

    private static final String CONFIGURATION_MIME_TYPE =
            "application/zip";

    private final Activity activity;
    private final SettingsRepository repository;
    private final AndroidVirtualControlLayoutRepository
            virtualControlLayoutRepository;
    private final SettingsDialogPresenter dialogPresenter;
    private final ConfigurationArchiveManager archiveManager;
    private final DocumentLauncher documentLauncher;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor =
            new ExclusiveTaskExecutor("settings-document-io");
    private final RequestState requestState;
    private volatile Runnable settingsChanged;
    private volatile boolean destroyed;
    private ConfigurationArchiveManager.PreparedImport preparedImport;

    SettingsDocumentController(
            Activity activity,
            SettingsRepository repository,
            AndroidVirtualControlLayoutRepository
                    virtualControlLayoutRepository,
            SettingsDialogPresenter dialogPresenter,
            DocumentLauncher documentLauncher,
            int restoredPendingRequestCode,
            Runnable settingsChanged) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.repository = Objects.requireNonNull(
                repository,
                "repository");
        this.virtualControlLayoutRepository = Objects.requireNonNull(
                virtualControlLayoutRepository,
                "virtualControlLayoutRepository");
        this.dialogPresenter = Objects.requireNonNull(
                dialogPresenter,
                "dialogPresenter");
        this.documentLauncher = Objects.requireNonNull(
                documentLauncher,
                "documentLauncher");
        requestState = new RequestState(restoredPendingRequestCode);
        this.settingsChanged = Objects.requireNonNull(
                settingsChanged,
                "settingsChanged");
        archiveManager = new ConfigurationArchiveManager(
                activity,
                repository);
    }

    boolean perform(String key) {
        if (destroyed) {
            return false;
        }
        SettingsDocumentAction action =
                SettingsDocumentActionRouter.resolve(key);
        if (action == null) {
            return false;
        }

        switch (action) {
            case IMPORT_VIRTUAL_KEYBOARD:
                openDocument(
                        "text/plain",
                        REQUEST_VIRTUAL_KEYBOARD_IMPORT);
                break;
            case IMPORT_VIRTUAL_GAMEPAD:
                openDocument(
                        "text/plain",
                        REQUEST_VIRTUAL_GAMEPAD_IMPORT);
                break;
            case IMPORT_CONFIGURATION:
                openDocument(
                        CONFIGURATION_MIME_TYPE,
                        REQUEST_CONFIGURATION_IMPORT);
                break;
            case IMPORT_ACCESSIBILITY_CONFIGURATION:
                openDocument(
                        "application/json",
                        REQUEST_ACCESSIBILITY_IMPORT);
                break;
            case SELECT_CLIPBOARD_DIRECTORY:
                openClipboardDirectory();
                break;
            case EXPORT_VIRTUAL_KEYBOARD:
                runOnIo(() -> exportVirtualControlLayout(false));
                break;
            case EXPORT_VIRTUAL_GAMEPAD:
                runOnIo(() -> exportVirtualControlLayout(true));
                break;
            case EXPORT_CONFIGURATION:
                runOnIo(this::exportConfiguration);
                break;
            default:
                throw new AssertionError(
                        "Unhandled document action: " + action);
        }
        return true;
    }

    boolean handleActivityResult(int resultCode, Intent data) {
        int requestCode = requestState.consume();
        if (!isKnownRequest(requestCode)) {
            return false;
        }
        if (destroyed ||
                resultCode != Activity.RESULT_OK ||
                data == null ||
                data.getData() == null) {
            return true;
        }

        Uri uri = data.getData();
        switch (requestCode) {
            case REQUEST_CLIPBOARD_DIRECTORY:
                persistClipboardDirectory(data, uri);
                break;
            case REQUEST_VIRTUAL_KEYBOARD_IMPORT:
                runOnIo(() -> importVirtualControlLayout(uri, false));
                break;
            case REQUEST_VIRTUAL_GAMEPAD_IMPORT:
                runOnIo(() -> importVirtualControlLayout(uri, true));
                break;
            case REQUEST_CONFIGURATION_IMPORT:
                runOnIo(() -> prepareConfigurationImport(uri));
                break;
            case REQUEST_ACCESSIBILITY_IMPORT:
                runOnIo(() -> importAccessibilityConfiguration(uri));
                break;
            default:
                throw new AssertionError(
                        "Unhandled document request: " + requestCode);
        }
        return true;
    }

    int getPendingRequestCode() {
        return requestState.peek();
    }

    void destroy() {
        destroyed = true;
        settingsChanged = null;
        discardPreparedImport(preparedImport);
        preparedImport = null;
        mainHandler.removeCallbacksAndMessages(null);
        ioExecutor.shutdownNow();
    }

    private void openDocument(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        if (requestCode == REQUEST_CONFIGURATION_IMPORT) {
            intent.putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    new String[] {
                            CONFIGURATION_MIME_TYPE,
                            "application/x-zip-compressed"
                    });
        }
        launch(intent, requestCode);
    }

    private void openClipboardDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        launch(intent, REQUEST_CLIPBOARD_DIRECTORY);
    }

    private void launch(Intent intent, int requestCode) {
        requestState.launch(
                requestCode,
                () -> documentLauncher.launch(intent));
    }

    private void exportConfiguration() {
        File archive = null;
        try {
            archive = archiveManager.createExportArchive();
            Uri uri = AndroidPrivateFileShare.stageReadOnly(
                    activity,
                    archive);
            runOnMain(() -> shareReadOnly(
                    uri,
                    CONFIGURATION_MIME_TYPE,
                    R.string.settings_export_configuration));
        }
        catch (Exception error) {
            showExportError("configuration archive", error);
        }
        finally {
            if (archive != null &&
                    archive.exists() &&
                    !archive.delete()) {
                LimeLog.warning(
                        "Unable to remove staged configuration archive");
            }
        }
    }

    private void prepareConfigurationImport(Uri uri) {
        try {
            ConfigurationArchiveManager.PreparedImport prepared =
                    archiveManager.prepareImport(uri);
            if (destroyed) {
                prepared.close();
                return;
            }
            mainHandler.post(() -> {
                if (destroyed) {
                    prepared.close();
                }
                else {
                    showConfigurationImport(prepared);
                }
            });
        }
        catch (Exception error) {
            showImportError("configuration archive", error);
        }
    }

    private void showConfigurationImport(
            ConfigurationArchiveManager.PreparedImport prepared) {
        if (destroyed) {
            prepared.close();
            return;
        }
        discardPreparedImport(preparedImport);
        preparedImport = prepared;
        dialogPresenter.showConfigurationImport(
                prepared.getComponents(),
                new SettingsDialogPresenter.ConfigurationImportListener() {
                    @Override
                    public void onSelected(
                            Set<ConfigurationArchiveComponent> components) {
                        startConfigurationImport(prepared, components);
                    }

                    @Override
                    public void onCancelled() {
                        if (preparedImport == prepared) {
                            preparedImport = null;
                        }
                        discardPreparedImport(prepared);
                    }
                });
    }

    private void startConfigurationImport(
            ConfigurationArchiveManager.PreparedImport prepared,
            Set<ConfigurationArchiveComponent> components) {
        if (preparedImport != prepared) {
            discardPreparedImport(prepared);
            return;
        }
        preparedImport = null;
        if (!runOnIo(() -> importConfiguration(prepared, components))) {
            discardPreparedImport(prepared);
        }
    }

    private void importConfiguration(
            ConfigurationArchiveManager.PreparedImport prepared,
            Set<ConfigurationArchiveComponent> components) {
        try {
            ConfigurationArchiveManager.ImportResult result =
                    archiveManager.importSelected(prepared, components);
            runOnMain(() -> completeConfigurationImport(result));
        }
        catch (Exception error) {
            discardPreparedImport(prepared);
            showImportError("configuration archive", error);
        }
    }

    private void completeConfigurationImport(
            ConfigurationArchiveManager.ImportResult result) {
        notifySettingsChanged();
        showToast(
                R.string.settings_configuration_import_succeeded,
                UiToast.LENGTH_SHORT);
        if (result.getImported().contains(
                ConfigurationArchiveComponent.APP_SETTINGS) &&
                result.getImportedLanguage() != null) {
            AndroidAppLocale.applyImportedLanguage(
                    activity,
                    result.getImportedLanguage());
        }
    }

    private void exportVirtualControlLayout(boolean gamepad) {
        try {
            Uri uri = virtualControlLayoutRepository.getShareUri(
                    selectedLayoutKey(gamepad));
            if (uri == null) {
                showToast(
                        R.string.virtual_control_layout_export_missing,
                        UiToast.LENGTH_SHORT);
                return;
            }
            runOnMain(() -> shareReadOnly(
                    uri,
                    "text/plain",
                    R.string.settings_share_configuration));
        }
        catch (IOException | RuntimeException error) {
            showExportError("virtual-control layout", error);
        }
    }

    private VirtualControlLayoutKey selectedLayoutKey(boolean gamepad) {
        VirtualControlSettings settings =
                VirtualControlSettingsLoader.load(repository);
        return gamepad
                ? VirtualControlLayoutKey.gamepad(
                        settings.getGamepadLayoutId(),
                        VirtualControlLayoutOrientation.LANDSCAPE)
                : VirtualControlLayoutKey.keyboard(
                        settings.getKeyboardLayoutId(),
                        VirtualControlLayoutOrientation.LANDSCAPE);
    }

    private void shareReadOnly(
            Uri uri,
            String mimeType,
            int chooserTitle) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType(mimeType);
        activity.startActivity(Intent.createChooser(
                intent,
                activity.getString(chooserTitle)));
    }

    private void showExportError(String subject, Exception error) {
        String detail = errorDetail(error);
        LimeLog.warning(
                "Unable to export " + subject + ": " + detail);
        showToast(
                activity.getString(
                        R.string.settings_export_failed,
                        detail),
                UiToast.LENGTH_SHORT);
    }

    private void persistClipboardDirectory(Intent data, Uri directory) {
        try {
            if (!FileUriUtils.persistUriPermission(
                    activity,
                    data,
                    directory)) {
                throw new SecurityException(
                        "Document provider returned no persistable URI permission");
            }
            repository.edit()
                    .put(
                            TransferSettingKeys
                                    .CLIPBOARD_FILE_DIRECTORY_URI,
                            directory.toString())
                    .apply();
            notifySettingsChanged();
        }
        catch (SecurityException error) {
            LimeLog.warning(
                    "Unable to persist clipboard directory: " +
                            error.getMessage());
            showToast(
                    R.string.settings_directory_permission_failed,
                    UiToast.LENGTH_SHORT);
        }
    }

    private void importVirtualControlLayout(Uri uri, boolean gamepad) {
        try {
            virtualControlLayoutRepository.importFrom(
                    activity.getContentResolver(),
                    uri,
                    selectedLayoutKey(gamepad));
            showToast(
                    R.string.virtual_control_layout_import_succeeded,
                    UiToast.LENGTH_SHORT);
        }
        catch (IOException | IllegalArgumentException error) {
            LimeLog.warning(
                    "Unable to import virtual-control layout: " +
                            error.getMessage());
            showToast(
                    R.string.virtual_control_layout_import_failed,
                    UiToast.LENGTH_SHORT);
        }
    }

    private void importAccessibilityConfiguration(Uri uri) {
        try {
            File destination = KeyboardRemappingFileStore.resolve(activity);
            replaceFileFromUri(
                    uri,
                    destination,
                    KeyboardRemappingFileStore.MAXIMUM_BYTES);
            showToast(
                    R.string.settings_import_succeeded,
                    UiToast.LENGTH_SHORT);
        }
        catch (Exception error) {
            showImportError("accessibility key mapping", error);
        }
    }

    private void showImportError(String subject, Exception error) {
        String detail = errorDetail(error);
        LimeLog.warning(
                "Unable to import " + subject + ": " + detail);
        showToast(
                activity.getString(
                        R.string.settings_import_failed,
                        detail),
                UiToast.LENGTH_SHORT);
    }

    private static String errorDetail(Exception error) {
        String detail = error.getMessage();
        return detail == null || detail.trim().isEmpty()
                ? error.getClass().getSimpleName()
                : detail;
    }

    private void showToast(int messageRes, int duration) {
        showToast(activity.getString(messageRes), duration);
    }

    private void showToast(String message, int duration) {
        runOnMain(() -> UiToast.makeText(
                activity,
                message,
                duration).show());
    }

    private void notifySettingsChanged() {
        runOnMain(() -> {
            Runnable callback = settingsChanged;
            if (callback != null) {
                callback.run();
            }
        });
    }

    private boolean runOnIo(Runnable action) {
        try {
            ioExecutor.execute(() -> {
                if (!destroyed) {
                    action.run();
                }
            });
            return true;
        }
        catch (RejectedExecutionException ignored) {
            // The owner was destroyed or another document operation is active.
            return false;
        }
    }

    private void runOnMain(Runnable action) {
        if (destroyed) {
            return;
        }
        mainHandler.post(() -> {
            if (!destroyed) {
                action.run();
            }
        });
    }

    private void replaceFileFromUri(
            Uri uri,
            File destination,
            long maximumBytes)
            throws IOException {
        AtomicFile atomicFile = new AtomicFile(destination);
        FileOutputStream output = null;
        try (InputStream input = openInput(uri)) {
            output = atomicFile.startWrite();
            copy(input, output, maximumBytes);
            atomicFile.finishWrite(output);
        }
        catch (IOException | RuntimeException error) {
            if (output != null) {
                atomicFile.failWrite(output);
            }
            throw error;
        }
    }

    private InputStream openInput(Uri uri) throws IOException {
        InputStream input = activity
                .getContentResolver()
                .openInputStream(uri);
        if (input == null) {
            throw new IOException(
                    "Document provider returned no readable stream");
        }
        return input;
    }

    private void copy(
            InputStream input,
            OutputStream output,
            long maximumBytes)
            throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Document operation canceled");
            }
            total += count;
            if (total > maximumBytes) {
                throw new IOException(activity.getString(
                        R.string.settings_import_too_large));
            }
            output.write(buffer, 0, count);
        }
        output.flush();
    }

    private static void discardPreparedImport(
            ConfigurationArchiveManager.PreparedImport prepared) {
        if (prepared != null) {
            prepared.close();
        }
    }

    private static boolean isKnownRequest(int requestCode) {
        switch (requestCode) {
            case REQUEST_VIRTUAL_KEYBOARD_IMPORT:
            case REQUEST_VIRTUAL_GAMEPAD_IMPORT:
            case REQUEST_CONFIGURATION_IMPORT:
            case REQUEST_ACCESSIBILITY_IMPORT:
            case REQUEST_CLIPBOARD_DIRECTORY:
                return true;
            default:
                return false;
        }
    }

    /** Lifecycle state for the single Activity Result launcher. */
    static final class RequestState {
        private int pendingRequestCode;

        RequestState(int restoredRequestCode) {
            pendingRequestCode = isKnownRequest(restoredRequestCode)
                    ? restoredRequestCode
                    : NO_PENDING_REQUEST;
        }

        void begin(int requestCode) {
            if (!isKnownRequest(requestCode)) {
                throw new IllegalArgumentException(
                        "Unknown settings document request: " +
                                requestCode);
            }
            if (pendingRequestCode != NO_PENDING_REQUEST) {
                throw new IllegalStateException(
                        "A settings document request is already active");
            }
            pendingRequestCode = requestCode;
        }

        void launch(int requestCode, Runnable launcher) {
            Objects.requireNonNull(launcher, "launcher");
            begin(requestCode);
            try {
                launcher.run();
            }
            catch (RuntimeException error) {
                cancel();
                throw error;
            }
        }

        int consume() {
            int requestCode = pendingRequestCode;
            pendingRequestCode = NO_PENDING_REQUEST;
            return requestCode;
        }

        int peek() {
            return pendingRequestCode;
        }

        void cancel() {
            pendingRequestCode = NO_PENDING_REQUEST;
        }
    }
}
