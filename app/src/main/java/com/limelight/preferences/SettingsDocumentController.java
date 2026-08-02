package com.limelight.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.input.accessibility.KeyboardRemappingFileStore;
import com.limelight.platform.files.AndroidPrivateFileShare;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.utils.FileUriUtils;
import com.limelight.utils.UiToast;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.android.AndroidVirtualControlLayoutRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Lifecycle-bound Android adapter for settings document selection, import,
 * export, and persisted document-tree permission.
 */
final class SettingsDocumentController {
    interface DocumentLauncher {
        void launch(Intent intent);
    }

    static final int NO_PENDING_REQUEST = 0;
    static final int REQUEST_VIRTUAL_KEYBOARD_IMPORT = 1001;
    static final int REQUEST_VIRTUAL_GAMEPAD_IMPORT = 1002;
    static final int REQUEST_HOSTS_IMPORT = 1003;
    static final int REQUEST_CERTIFICATE_IMPORT = 1004;
    static final int REQUEST_PRIVATE_KEY_IMPORT = 1005;
    static final int REQUEST_ACCESSIBILITY_IMPORT = 1007;
    static final int REQUEST_BACKGROUND = 1008;
    static final int REQUEST_CLIPBOARD_DIRECTORY = 1009;

    private static final String CERTIFICATE_FILE_NAME = "client.crt";
    private static final String PRIVATE_KEY_FILE_NAME = "client.key";
    private static final String HOST_SNAPSHOT_DIRECTORY =
            "host-backups";
    private static final String HOST_SNAPSHOT_FILE =
            "moonlight-hosts.db";
    private static final String HOST_DATABASE_MIME_TYPE =
            "application/vnd.sqlite3";
    private static final long MAX_DATA_IMPORT_BYTES = 4L * 1024L * 1024L;
    private static final long MAX_HOST_DATABASE_BYTES =
            64L * 1024L * 1024L;
    private static final long MAX_BACKGROUND_BYTES =
            64L * 1024L * 1024L;

    private final Activity activity;
    private final SettingsRepository repository;
    private final AndroidVirtualControlLayoutRepository
            virtualControlLayoutRepository;
    private final DocumentLauncher documentLauncher;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "settings-document-io");
                thread.setDaemon(true);
                return thread;
            });
    private volatile Runnable settingsChanged;
    private volatile boolean destroyed;
    private final RequestState requestState;

    SettingsDocumentController(
            Activity activity,
            SettingsRepository repository,
            AndroidVirtualControlLayoutRepository
                    virtualControlLayoutRepository,
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
        this.documentLauncher = Objects.requireNonNull(
                documentLauncher,
                "documentLauncher");
        requestState = new RequestState(restoredPendingRequestCode);
        this.settingsChanged = Objects.requireNonNull(
                settingsChanged,
                "settingsChanged");
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
            case IMPORT_HOSTS:
                openDocument("*/*", REQUEST_HOSTS_IMPORT);
                break;
            case IMPORT_CERTIFICATE:
                openDocument("*/*", REQUEST_CERTIFICATE_IMPORT);
                break;
            case IMPORT_PRIVATE_KEY:
                openDocument("*/*", REQUEST_PRIVATE_KEY_IMPORT);
                break;
            case IMPORT_ACCESSIBILITY_CONFIGURATION:
                openDocument(
                        "application/json",
                        REQUEST_ACCESSIBILITY_IMPORT);
                break;
            case SELECT_BACKGROUND:
                openDocument("image/*", REQUEST_BACKGROUND);
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
            case EXPORT_HOSTS:
                runOnIo(this::exportHosts);
                break;
            case EXPORT_CERTIFICATE:
                runOnIo(() -> exportFile(
                        new File(
                                activity.getFilesDir(),
                                CERTIFICATE_FILE_NAME),
                        "*/*"));
                break;
            case EXPORT_PRIVATE_KEY:
                runOnIo(() -> exportFile(
                        new File(
                                activity.getFilesDir(),
                                PRIVATE_KEY_FILE_NAME),
                        "*/*"));
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
            case REQUEST_HOSTS_IMPORT:
                runOnIo(() -> importHosts(uri));
                break;
            case REQUEST_CERTIFICATE_IMPORT:
                runOnIo(() -> importFile(uri, CERTIFICATE_FILE_NAME));
                break;
            case REQUEST_PRIVATE_KEY_IMPORT:
                runOnIo(() -> importFile(uri, PRIVATE_KEY_FILE_NAME));
                break;
            case REQUEST_ACCESSIBILITY_IMPORT:
                runOnIo(() -> importAccessibilityConfiguration(uri));
                break;
            case REQUEST_BACKGROUND:
                runOnIo(() -> importBackground(uri));
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
        mainHandler.removeCallbacksAndMessages(null);
        ioExecutor.shutdownNow();
    }

    private void openDocument(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
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

    private VirtualControlLayoutKey selectedLayoutKey(
            boolean gamepad) {
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

    private void exportFile(File file, String mimeType) {
        if (!file.isFile()) {
            return;
        }
        try {
            Uri uri = AndroidPrivateFileShare.stageReadOnly(
                    activity,
                    file);
            runOnMain(() -> shareReadOnly(
                    uri,
                    mimeType,
                    R.string.settings_share_data));
        }
        catch (IOException | RuntimeException error) {
            showExportError(file.getName(), error);
        }
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
        String detail = error.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = error.getClass().getSimpleName();
        }
        LimeLog.warning(
                "Unable to export " + subject + ": " + detail);
        showToast(
                activity.getString(
                        R.string.settings_export_failed,
                        detail),
                UiToast.LENGTH_SHORT);
    }

    private void exportHosts() {
        File snapshot = new File(
                new File(
                        activity.getCacheDir(),
                        HOST_SNAPSHOT_DIRECTORY),
                HOST_SNAPSHOT_FILE);
        ComputerDatabaseManager manager = null;
        try {
            manager = new ComputerDatabaseManager(activity);
            manager.writePortableSnapshot(snapshot);
            exportFile(
                    snapshot,
                    HOST_DATABASE_MIME_TYPE);
        }
        catch (Exception error) {
            String detail = error.getMessage();
            if (detail == null || detail.trim().isEmpty()) {
                detail = error.getClass().getSimpleName();
            }
            LimeLog.warning(
                    "Unable to export host database: " + detail);
            showToast(
                    activity.getString(
                            R.string.settings_export_failed,
                            detail),
                    UiToast.LENGTH_SHORT);
        }
        finally {
            closeDatabase(manager);
        }
    }

    private void persistClipboardDirectory(
            Intent data,
            Uri directory) {
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

    private void importVirtualControlLayout(
            Uri uri,
            boolean gamepad) {
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

    private void importHosts(Uri uri) {
        ComputerDatabaseManager importManager = null;
        ComputerDatabaseManager destinationManager = null;
        File databaseFile = null;
        try {
            databaseFile = File.createTempFile(
                    "hosts-import-",
                    ".db",
                    activity.getCacheDir());
            copyUriToFile(
                    uri,
                    databaseFile,
                    MAX_HOST_DATABASE_BYTES);

            importManager = new ComputerDatabaseManager(
                    activity,
                    databaseFile);
            destinationManager =
                    new ComputerDatabaseManager(activity);
            destinationManager.restoreFrom(importManager);
            showToast(
                    R.string.settings_hosts_import_succeeded,
                    UiToast.LENGTH_SHORT);
        }
        catch (Exception error) {
            showImportError("host database", error);
        }
        finally {
            closeDatabase(importManager);
            closeDatabase(destinationManager);
            if (databaseFile != null &&
                    databaseFile.exists() &&
                    !databaseFile.delete()) {
                LimeLog.warning(
                        "Unable to delete imported host database cache file");
            }
        }
    }

    private void importFile(Uri uri, String displayName) {
        try {
            replaceFileFromUri(
                    uri,
                    new File(activity.getFilesDir(), displayName),
                    MAX_DATA_IMPORT_BYTES);
            showToast(
                    R.string.settings_import_succeeded,
                    UiToast.LENGTH_SHORT);
        }
        catch (Exception error) {
            showImportError(displayName, error);
        }
    }

    private void importAccessibilityConfiguration(Uri uri) {
        try {
            File destination =
                    KeyboardRemappingFileStore.resolve(activity);
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

    private void importBackground(Uri uri) {
        try {
            String displayName =
                    "settings_background_" +
                            System.currentTimeMillis() +
                            ".png";
            replaceFileFromUri(
                    uri,
                    new File(activity.getFilesDir(), displayName),
                    MAX_BACKGROUND_BYTES);
            repository.edit()
                    .put(
                            AppPresentationSettingKeys.BACKGROUND_FILE,
                            displayName)
                    .apply();
            notifySettingsChanged();
        }
        catch (Exception error) {
            showImportError("background", error);
        }
    }

    private void showImportError(String subject, Exception error) {
        String detail = error.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = error.getClass().getSimpleName();
        }
        LimeLog.warning(
                "Unable to import " + subject + ": " +
                        detail);
        showToast(
                activity.getString(
                        R.string.settings_import_failed,
                        detail),
                UiToast.LENGTH_SHORT);
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

    private void runOnIo(Runnable action) {
        try {
            ioExecutor.execute(() -> {
                if (!destroyed) {
                    action.run();
                }
            });
        }
        catch (RejectedExecutionException ignored) {
            // The Activity was destroyed between result dispatch and enqueue.
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

    private void copyUriToFile(
            Uri uri,
            File destination,
            long maximumBytes)
            throws IOException {
        try (InputStream input = openInput(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            copy(input, output, maximumBytes);
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
            FileOutputStream output,
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

    private static void closeDatabase(
            ComputerDatabaseManager manager) {
        if (manager != null) {
            try {
                manager.close();
            }
            catch (RuntimeException error) {
                LimeLog.warning(
                        "Unable to close imported host database: " +
                                error.getMessage());
            }
        }
    }

    private static boolean isKnownRequest(int requestCode) {
        switch (requestCode) {
            case REQUEST_VIRTUAL_KEYBOARD_IMPORT:
            case REQUEST_VIRTUAL_GAMEPAD_IMPORT:
            case REQUEST_HOSTS_IMPORT:
            case REQUEST_CERTIFICATE_IMPORT:
            case REQUEST_PRIVATE_KEY_IMPORT:
            case REQUEST_ACCESSIBILITY_IMPORT:
            case REQUEST_BACKGROUND:
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
