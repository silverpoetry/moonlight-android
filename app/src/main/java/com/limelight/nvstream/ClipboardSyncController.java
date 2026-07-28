package com.limelight.nvstream;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.SystemClock;

import android.support.v4.content.FileProvider;

import com.limelight.LimeLog;
import com.limelight.nvstream.filetransfer.ClipboardFileDownloader;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.jni.MoonBridge;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

class ClipboardSyncController implements ClipboardManager.OnPrimaryClipChangedListener,
        MoonBridge.ClipboardListener {
    private static final int MAX_TEXT_BYTES = 1024 * 1024;
    private static final int MAX_INLINE_PNG_BYTES = 1024 * 1024;
    private static final long MAX_BLOB_BYTES = 32L * 1024L * 1024L;
    private static final long MAX_IMAGE_PIXELS = 32L * 1024L * 1024L;
    private static final long CACHE_RETENTION_MS = 24L * 60L * 60L * 1000L;
    private static final long REMOTE_WRITE_SUPPRESSION_MS = 1500;
    private static final String SENSITIVE_EXTRA = "android.content.extra.IS_SENSITIVE";
    private static final String STATE_PREFERENCES = "clipboard_sync_state";
    private static final String LAST_HANDLED_KEY = "last_handled_key";

    private final Context context;
    private final ClipboardManager clipboardManager;
    private final SharedPreferences statePreferences;
    private final NvHTTP nvHttp;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ClipboardIO");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong localGeneration = new AtomicLong();
    private final AtomicLong remoteGeneration = new AtomicLong();
    private final AtomicLong clipboardChangeSequence = new AtomicLong();

    private volatile boolean started;
    private volatile boolean ready;
    private volatile int hostCapabilities;
    private volatile boolean hasPersistentClipboardState;
    private volatile String lastObservedKey;
    private volatile String pendingLocalKey;
    private volatile String pendingRemoteKey;
    private volatile long pendingRemoteKeyExpiresAt;

    ClipboardSyncController(Context context, NvHTTP nvHttp) {
        this.context = context.getApplicationContext();
        this.nvHttp = nvHttp;
        clipboardManager = (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
        statePreferences = this.context.getSharedPreferences(
                STATE_PREFERENCES, Context.MODE_PRIVATE);
        hasPersistentClipboardState =
                statePreferences.contains(LAST_HANDLED_KEY);
        lastObservedKey = statePreferences.getString(
                LAST_HANDLED_KEY, null);
    }

    synchronized void start() {
        if (started || clipboardManager == null) {
            return;
        }

        started = true;
        ready = false;
        hostCapabilities = 0;
        MoonBridge.setClipboardListener(this);
        mainHandler.post(() -> {
            if (!started) {
                return;
            }
            cleanupOldCacheFiles();
            clipboardManager.addPrimaryClipChangedListener(this);
        });
    }

    synchronized void stop() {
        if (!started) {
            return;
        }

        started = false;
        ready = false;
        localGeneration.incrementAndGet();
        remoteGeneration.incrementAndGet();
        MoonBridge.setClipboardListener(null);
        mainHandler.post(() -> clipboardManager.removePrimaryClipChangedListener(this));
        ioExecutor.shutdownNow();
        pendingLocalKey = null;
        pendingRemoteKey = null;
        pendingRemoteKeyExpiresAt = 0;
    }

    void onFocusGained() {
        mainHandler.post(() -> inspectLocalClipboard(true));
    }

    @Override
    public void onPrimaryClipChanged() {
        clipboardChangeSequence.incrementAndGet();
        inspectLocalClipboard(true);
    }

    @Override
    public void onClipboardReady(int capabilities) {
        boolean firstReady = !ready;
        hostCapabilities = capabilities;
        ready = true;

        // Publish on reconnect only when Android's clipboard changed while the
        // stream was disconnected. On the first run, establish a baseline so
        // stale local content cannot overwrite the host clipboard.
        if (firstReady) {
            mainHandler.post(() -> {
                if (hasPersistentClipboardState) {
                    inspectLocalClipboard(true);
                }
                else {
                    inspectLocalClipboard(false);
                    if (!hasPersistentClipboardState) {
                        rememberHandledKey("");
                    }
                }
            });
        }
    }

    @Override
    public void onClipboardContent(byte mimeType, long originId, long itemId, byte[] data) {
        if (!started || data == null) {
            return;
        }

        if (mimeType == MoonBridge.LI_CLIPBOARD_MIME_TEXT_UTF8 &&
                canReceiveFromHost(MoonBridge.LI_CLIPBOARD_CAP_TEXT) &&
                isValidUtf8Text(data)) {
            long generation = remoteGeneration.incrementAndGet();
            applyInboundText(data, generation);
        }
        else if (mimeType == MoonBridge.LI_CLIPBOARD_MIME_PNG &&
                canReceiveFromHost(MoonBridge.LI_CLIPBOARD_CAP_PNG) &&
                isValidPngHeader(data, MAX_INLINE_PNG_BYTES)) {
            long generation = remoteGeneration.incrementAndGet();
            ioExecutor.execute(() ->
                    applyInboundPng(originId, itemId, data, generation));
        }
        else if (mimeType == MoonBridge.LI_CLIPBOARD_MIME_FILE_OFFER &&
                canReceiveFromHost(MoonBridge.LI_CLIPBOARD_CAP_FILES) &&
                canReceiveFromHost(
                    MoonBridge.LI_CLIPBOARD_CAP_FILE_STREAMS) &&
                decodeFileOfferId(data) != null) {
            remoteGeneration.incrementAndGet();
        }
        else if (mimeType == MoonBridge.LI_CLIPBOARD_MIME_BLOB_REFERENCE) {
            BlobReference reference = decodeBlobReference(data);
            if (reference != null &&
                    canReceiveFromHost(MoonBridge.LI_CLIPBOARD_CAP_BLOB) &&
                    canReceiveFromHost(reference.targetMime ==
                            MoonBridge.LI_CLIPBOARD_MIME_PNG ?
                            MoonBridge.LI_CLIPBOARD_CAP_PNG :
                            MoonBridge.LI_CLIPBOARD_CAP_TEXT) &&
                    (reference.targetMime == MoonBridge.LI_CLIPBOARD_MIME_TEXT_UTF8 ||
                            reference.targetMime == MoonBridge.LI_CLIPBOARD_MIME_PNG)) {
                long generation = remoteGeneration.incrementAndGet();
                ioExecutor.execute(() ->
                        applyInboundBlob(originId, itemId, reference, generation));
            }
        }
    }

    void downloadRemoteFiles(Uri destinationTree,
                             NvConnection.ClipboardFileDownloadListener listener) {
        if (!started || !ready || destinationTree == null || nvHttp == null) {
            mainHandler.post(() -> listener.onError(
                    "剪贴板同步尚未连接"));
            return;
        }

        ioExecutor.execute(() -> {
            try {
                long originId = MoonBridge.getClipboardOriginId();
                if (originId == 0) {
                    throw new IOException("Clipboard session is unavailable");
                }
                NvHTTP.ClipboardFileReference pulled =
                        nvHttp.pullClipboardFiles(originId);
                LimeLog.info("Remote clipboard file offer prepared: " +
                        pulled.id);
                int topLevelCount = ClipboardFileDownloader.download(
                        context,
                        nvHttp,
                        destinationTree,
                        pulled.id,
                        originId,
                        (transferred, total) -> mainHandler.post(() ->
                                listener.onProgress(transferred, total)));
                mainHandler.post(() -> listener.onComplete(topLevelCount));
            } catch (FileNotFoundException error) {
                mainHandler.post(() -> listener.onError(
                        "远端剪贴板中没有文件或文件夹"));
            } catch (Throwable error) {
                LimeLog.warning("Clipboard file download failed: " +
                        error.getMessage());
                mainHandler.post(() -> listener.onError(
                        error.getMessage() == null ?
                                "文件拉取失败" :
                                error.getMessage()));
            }
        });
    }

    private void inspectLocalClipboard(boolean dispatchChanges) {
        if (!started || clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
            if (dispatchChanges) {
                localGeneration.incrementAndGet();
            }
            return;
        }

        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            if (dispatchChanges) {
                localGeneration.incrementAndGet();
            }
            return;
        }
        ClipDescription description = clipData.getDescription();
        if (description == null || isSensitive(description)) {
            if (dispatchChanges) {
                localGeneration.incrementAndGet();
            }
            return;
        }

        ClipData.Item item = clipData.getItemAt(0);
        Uri imageUri = findImageUri(description, item);
        if (imageUri != null && canUse(MoonBridge.LI_CLIPBOARD_CAP_PNG)) {
            String key = "uri:" + imageUri + ':' + clipTimestamp(description);
            if (consumeRemoteWrite(key)) {
                localGeneration.incrementAndGet();
                rememberHandledKey(key);
                return;
            }
            if (!dispatchChanges || isAlreadyHandled(key)) {
                rememberHandledKey(key);
                return;
            }
            dispatchLocalImage(imageUri, key);
            return;
        }

        if (canUse(MoonBridge.LI_CLIPBOARD_CAP_TEXT) &&
                description.hasMimeType("text/*")) {
            CharSequence text = item.getText();
            if (text == null) {
                text = item.coerceToText(context);
            }
            if (text == null) {
                return;
            }

            byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_TEXT_BYTES) {
                return;
            }
            String key = "text:" + hexSha256(bytes) + ':' + clipTimestamp(description);
            if (consumeRemoteWrite(key)) {
                localGeneration.incrementAndGet();
                rememberHandledKey(key);
                return;
            }
            if (!dispatchChanges || isAlreadyHandled(key)) {
                rememberHandledKey(key);
                return;
            }

            localGeneration.incrementAndGet();
            pendingLocalKey = key;
            int result = MoonBridge.sendClipboardContent(
                    MoonBridge.LI_CLIPBOARD_MIME_TEXT_UTF8, bytes);
            if (result == 0) {
                rememberHandledKey(key);
            }
            else {
                LimeLog.warning("Failed to announce clipboard text");
            }
            pendingLocalKey = null;
            return;
        }

        if (dispatchChanges) {
            localGeneration.incrementAndGet();
        }
    }

    private boolean canUse(int capability) {
        return ready && (hostCapabilities & MoonBridge.LI_CLIPBOARD_CAP_CAN_RECEIVE) != 0 &&
                (hostCapabilities & capability) != 0;
    }

    private boolean canReceiveFromHost(int capability) {
        return ready &&
                (hostCapabilities & MoonBridge.LI_CLIPBOARD_CAP_CAN_SEND) != 0 &&
                (hostCapabilities & capability) != 0;
    }

    private boolean consumeRemoteWrite(String key) {
        String expected = pendingRemoteKey;
        if (expected == null) {
            return false;
        }
        if (SystemClock.uptimeMillis() > pendingRemoteKeyExpiresAt) {
            pendingRemoteKey = null;
            pendingRemoteKeyExpiresAt = 0;
            return false;
        }
        if (expected.equals(key) || key.startsWith(expected + ':')) {
            return true;
        }
        pendingRemoteKey = null;
        pendingRemoteKeyExpiresAt = 0;
        return false;
    }

    private boolean isAlreadyHandled(String key) {
        return fingerprintKey(key).equals(lastObservedKey) ||
                key.equals(pendingLocalKey);
    }

    private void rememberHandledKey(String key) {
        String fingerprint = fingerprintKey(key);
        lastObservedKey = fingerprint;
        hasPersistentClipboardState = true;
        statePreferences.edit()
                .putString(LAST_HANDLED_KEY, fingerprint)
                .apply();
    }

    private String fingerprintKey(String key) {
        return hexSha256(key.getBytes(StandardCharsets.UTF_8));
    }

    private void dispatchLocalImage(Uri uri, String key) {
        long generation = localGeneration.incrementAndGet();
        pendingLocalKey = key;
        ioExecutor.execute(() -> {
            File pngFile = null;
            try {
                pngFile = materializePng(uri);
                if (pngFile == null || generation != localGeneration.get() || !started) {
                    return;
                }

                int result;
                if (pngFile.length() <= MAX_INLINE_PNG_BYTES) {
                    byte[] png = readFileBounded(pngFile, MAX_INLINE_PNG_BYTES);
                    result = MoonBridge.sendClipboardContent(MoonBridge.LI_CLIPBOARD_MIME_PNG, png);
                }
                else if ((hostCapabilities & MoonBridge.LI_CLIPBOARD_CAP_BLOB) != 0 &&
                        nvHttp != null) {
                    NvHTTP.ClipboardBlobUploadResult upload = nvHttp.uploadClipboardBlob(
                            "image/png",
                            pngFile,
                            MoonBridge.getClipboardOriginId(),
                            UUID.randomUUID().toString());
                    if (generation != localGeneration.get() || !started) {
                        return;
                    }
                    result = MoonBridge.sendClipboardBlobReference(
                            MoonBridge.LI_CLIPBOARD_MIME_PNG,
                            upload.id,
                            (int)upload.size,
                            upload.sha256);
                }
                else {
                    LimeLog.warning("Clipboard PNG exceeds inline limit and blob transport is unavailable");
                    return;
                }

                if (result == 0) {
                    rememberHandledKey(key);
                }
                else {
                    LimeLog.warning("Failed to announce clipboard PNG");
                }
            } catch (Throwable error) {
                LimeLog.warning("Clipboard image processing failed: " + error.getMessage());
            } finally {
                if (key.equals(pendingLocalKey)) {
                    pendingLocalKey = null;
                }
                if (pngFile != null) {
                    pngFile.delete();
                }
            }
        });
    }

    private void applyInboundText(byte[] data, long generation) {
        if (!isValidUtf8Text(data)) {
            return;
        }
        String value = new String(data, StandardCharsets.UTF_8);

        String keyPrefix = "text:" + hexSha256(data);
        mainHandler.post(() -> {
            if (!isCurrentRemoteGeneration(generation)) {
                return;
            }
            pendingRemoteKey = keyPrefix;
            pendingRemoteKeyExpiresAt =
                    SystemClock.uptimeMillis() + REMOTE_WRITE_SUPPRESSION_MS;
            try {
                ClipData clip = ClipData.newPlainText("Sunshine", value);
                markRemoteClip(clip);
                clipboardManager.setPrimaryClip(clip);
                ClipData appliedClip = clipboardManager.getPrimaryClip();
                ClipDescription appliedDescription =
                        appliedClip == null ? null : appliedClip.getDescription();
                String appliedKey = keyPrefix + ':' + clipTimestamp(appliedDescription);
                rememberHandledKey(appliedKey);
            } catch (Throwable error) {
                pendingRemoteKey = null;
                pendingRemoteKeyExpiresAt = 0;
                LimeLog.warning("Failed to set remote clipboard text: " + error.getMessage());
            }
        });
    }

    private void applyInboundPng(long originId, long itemId, byte[] data,
                                 long generation) {
        if (!isCurrentRemoteGeneration(generation) ||
                !isValidPngHeader(data, MAX_INLINE_PNG_BYTES)) {
            return;
        }
        File target = inboundCacheFile(originId, itemId, data);
        try (FileOutputStream output = new FileOutputStream(target)) {
            output.write(data);
        } catch (IOException error) {
            target.delete();
            LimeLog.warning("Failed to cache remote clipboard PNG: " + error.getMessage());
            return;
        }
        if (!isValidPngFile(target)) {
            target.delete();
            return;
        }
        applyInboundImageUri(target, generation);
    }

    private void applyInboundBlob(long originId, long itemId,
                                  BlobReference reference, long generation) {
        if (!isCurrentRemoteGeneration(generation) ||
                reference.size <= 0 ||
                reference.size > MAX_BLOB_BYTES ||
                nvHttp == null) {
            return;
        }

        File target = new File(clipboardCacheDirectory(),
                "blob-" + Long.toUnsignedString(originId) + '-' +
                        Long.toUnsignedString(itemId) + '-' + reference.id + ".tmp");
        try {
            nvHttp.downloadClipboardBlob(reference.id,
                    reference.targetMime == MoonBridge.LI_CLIPBOARD_MIME_PNG ?
                            "image/png" :
                            "text/plain",
                    MoonBridge.getClipboardOriginId(),
                    reference.size,
                    reference.sha256,
                    target);
            if (!isCurrentRemoteGeneration(generation)) {
                target.delete();
                return;
            }

            if (reference.targetMime == MoonBridge.LI_CLIPBOARD_MIME_TEXT_UTF8) {
                byte[] text = readFileBounded(target, MAX_TEXT_BYTES);
                target.delete();
                applyInboundText(text, generation);
            }
            else if (reference.targetMime == MoonBridge.LI_CLIPBOARD_MIME_PNG) {
                if (!isValidPngFile(target)) {
                    target.delete();
                    return;
                }
                File immutable = inboundCacheFile(originId, itemId, reference.sha256);
                if (!target.renameTo(immutable)) {
                    copyFile(target, immutable, MAX_BLOB_BYTES);
                    target.delete();
                }
                applyInboundImageUri(immutable, generation);
            }
        } catch (Throwable error) {
            target.delete();
            LimeLog.warning("Clipboard blob download failed: " + error.getMessage());
        }
    }

    private void applyInboundImageUri(File file, long generation) {
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider",
                    file);
        } catch (Throwable error) {
            LimeLog.warning("Failed to expose clipboard image: " + error.getMessage());
            return;
        }

        mainHandler.post(() -> {
            if (!isCurrentRemoteGeneration(generation)) {
                return;
            }
            try {
                ClipData clip = ClipData.newUri(context.getContentResolver(), "Sunshine image", uri);
                markRemoteClip(clip);
                String keyPrefix = "uri:" + uri;
                pendingRemoteKey = keyPrefix;
                pendingRemoteKeyExpiresAt =
                        SystemClock.uptimeMillis() + REMOTE_WRITE_SUPPRESSION_MS;
                clipboardManager.setPrimaryClip(clip);
                ClipData appliedClip = clipboardManager.getPrimaryClip();
                ClipDescription appliedDescription =
                        appliedClip == null ? null : appliedClip.getDescription();
                String appliedKey = keyPrefix + ':' + clipTimestamp(appliedDescription);
                rememberHandledKey(appliedKey);
            } catch (Throwable error) {
                pendingRemoteKey = null;
                pendingRemoteKeyExpiresAt = 0;
                LimeLog.warning("Failed to set remote clipboard image: " + error.getMessage());
            }
        });
    }

    private Uri findImageUri(ClipDescription description, ClipData.Item item) {
        Uri uri = item.getUri();
        if (uri == null) {
            return null;
        }

        String resolvedType = null;
        try {
            resolvedType = context.getContentResolver().getType(uri);
        } catch (SecurityException ignored) {
            // The MIME type declared by the clipboard remains usable even if
            // the provider withholds type access until its stream is opened.
        }
        if ((resolvedType != null && resolvedType.startsWith("image/")) ||
                description.hasMimeType("image/*")) {
            return uri;
        }
        return null;
    }

    private boolean isCurrentRemoteGeneration(long generation) {
        return started && generation == remoteGeneration.get();
    }

    private boolean isSensitive(ClipDescription description) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        PersistableBundle extras = description.getExtras();
        return extras != null && extras.getBoolean(SENSITIVE_EXTRA, false);
    }

    private void markRemoteClip(ClipData clip) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return;
        }

        ClipDescription description = clip.getDescription();
        PersistableBundle existing = description.getExtras();
        PersistableBundle extras = existing == null ?
                new PersistableBundle() :
                new PersistableBundle(existing);
        extras.putBoolean(ClipDescription.EXTRA_IS_REMOTE_DEVICE, true);
        description.setExtras(extras);
    }

    private long clipTimestamp(ClipDescription description) {
        if (description != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long timestamp = description.getTimestamp();
            if (timestamp != 0) {
                return timestamp;
            }
        }
        return clipboardChangeSequence.get();
    }

    private File materializePng(Uri uri) throws IOException {
        String type = context.getContentResolver().getType(uri);
        if ("image/png".equalsIgnoreCase(type)) {
            File copy = temporaryCacheFile("outbound-", ".png");
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(copy)) {
                if (input == null) {
                    throw new IOException("Unable to open clipboard image");
                }
                copyStream(input, output, MAX_BLOB_BYTES);
            } catch (IOException error) {
                copy.delete();
                throw error;
            }
            if (!isValidPngFile(copy)) {
                copy.delete();
                throw new IOException("Invalid PNG clipboard image");
            }
            return copy;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                (long)bounds.outWidth * bounds.outHeight > MAX_IMAGE_PIXELS) {
            throw new IOException("Clipboard image dimensions exceed limit");
        }

        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input);
        }
        if (bitmap == null) {
            throw new IOException("Unable to decode clipboard image");
        }

        File png = temporaryCacheFile("outbound-", ".png");
        try (FileOutputStream output = new FileOutputStream(png)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Unable to encode clipboard image as PNG");
            }
        } catch (IOException error) {
            png.delete();
            throw error;
        } finally {
            bitmap.recycle();
        }
        if (png.length() > MAX_BLOB_BYTES || !isValidPngFile(png)) {
            png.delete();
            throw new IOException("Encoded clipboard image exceeds limit");
        }
        return png;
    }

    private boolean isValidPngFile(File file) {
        if (!file.isFile() || file.length() < 24 || file.length() > MAX_BLOB_BYTES) {
            return false;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] header = new byte[24];
            if (input.read(header) != header.length ||
                    !isValidPngHeader(header, MAX_BLOB_BYTES)) {
                return false;
            }
        } catch (IOException error) {
            return false;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        return bounds.outWidth > 0 && bounds.outHeight > 0 &&
                (long)bounds.outWidth * bounds.outHeight <= MAX_IMAGE_PIXELS;
    }

    private BlobReference decodeBlobReference(byte[] data) {
        if (data.length < 41 || data[0] != 1 || data[3] != 0) {
            return null;
        }
        byte targetMime = data[1];
        int idLength = data[2] & 0xFF;
        if ((targetMime != MoonBridge.LI_CLIPBOARD_MIME_TEXT_UTF8 &&
                targetMime != MoonBridge.LI_CLIPBOARD_MIME_PNG) ||
                idLength == 0 || idLength > 64 || data.length != 40 + idLength) {
            return null;
        }

        long size = ((long)data[4] & 0xFF) |
                (((long)data[5] & 0xFF) << 8) |
                (((long)data[6] & 0xFF) << 16) |
                (((long)data[7] & 0xFF) << 24);
        byte[] sha256 = Arrays.copyOfRange(data, 8, 40);
        String id = new String(data, 40, idLength, StandardCharsets.US_ASCII);
        if (size <= 0 || size > MAX_BLOB_BYTES || !isCanonicalUuid(id)) {
            return null;
        }
        return new BlobReference(targetMime, size, sha256, id);
    }

    private String decodeFileOfferId(byte[] data) {
        if (data.length < 9 ||
                data[0] != 'M' ||
                data[1] != 'L' ||
                data[2] != 'F' ||
                data[3] != 'O' ||
                data[4] != 1 ||
                data[6] != 0 ||
                data[7] != 0) {
            return null;
        }
        int idLength = data[5] & 0xFF;
        if (idLength == 0 ||
                idLength > 64 ||
                data.length != 8 + idLength) {
            return null;
        }
        String id = new String(
                data,
                8,
                idLength,
                StandardCharsets.US_ASCII);
        return isCanonicalUuid(id) ? id : null;
    }

    private File clipboardCacheDirectory() {
        File directory = new File(context.getCacheDir(), "clipboard");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private File temporaryCacheFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix, clipboardCacheDirectory());
    }

    private File inboundCacheFile(long originId, long itemId, byte[] digestSource) {
        String digest = hexSha256(digestSource);
        return new File(clipboardCacheDirectory(),
                "inbound-" + Long.toUnsignedString(originId) + '-' +
                        Long.toUnsignedString(itemId) + '-' + digest.substring(0, 16) + ".png");
    }

    private void cleanupOldCacheFiles() {
        File[] files = clipboardCacheDirectory().listFiles();
        if (files == null) {
            return;
        }

        Uri activeClipboardUri = null;
        try {
            ClipData activeClip = clipboardManager.getPrimaryClip();
            if (activeClip != null && activeClip.getItemCount() != 0) {
                activeClipboardUri = activeClip.getItemAt(0).getUri();
            }
        } catch (SecurityException ignored) {
        }

        long cutoff = System.currentTimeMillis() - CACHE_RETENTION_MS;
        for (File file : files) {
            if (!file.isFile() || file.lastModified() >= cutoff) {
                continue;
            }
            if (activeClipboardUri != null) {
                try {
                    Uri fileUri = FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider",
                            file);
                    if (activeClipboardUri.equals(fileUri)) {
                        continue;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            file.delete();
        }
    }

    private static byte[] readFileBounded(File file, long limit) throws IOException {
        if (!file.isFile() || file.length() > limit || file.length() > Integer.MAX_VALUE) {
            throw new IOException("Clipboard file exceeds limit");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream((int)file.length());
        try (FileInputStream input = new FileInputStream(file)) {
            copyStream(input, output, limit);
        }
        return output.toByteArray();
    }

    private static void copyFile(File source, File destination, long limit) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            copyStream(input, output, limit);
        } catch (IOException error) {
            destination.delete();
            throw error;
        }
    }

    private static void copyStream(InputStream input, java.io.OutputStream output,
                                   long limit) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new IOException("Clipboard data exceeds limit");
            }
            output.write(buffer, 0, read);
        }
    }

    private static boolean containsNull(byte[] data) {
        for (byte value : data) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidUtf8Text(byte[] data) {
        if (data.length > MAX_TEXT_BYTES || containsNull(data)) {
            return false;
        }
        String value = new String(data, StandardCharsets.UTF_8);
        return Arrays.equals(value.getBytes(StandardCharsets.UTF_8), data);
    }

    private static boolean isValidPngHeader(byte[] data, long maximumSize) {
        byte[] signature = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (data.length < 24 || data.length > maximumSize) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        if (data[12] != 'I' || data[13] != 'H' ||
                data[14] != 'D' || data[15] != 'R') {
            return false;
        }

        long width = readUnsignedIntBigEndian(data, 16);
        long height = readUnsignedIntBigEndian(data, 20);
        return width != 0 &&
                height != 0 &&
                width <= MAX_IMAGE_PIXELS / height;
    }

    private static long readUnsignedIntBigEndian(byte[] data, int offset) {
        return ((long)data[offset] & 0xFF) << 24 |
                ((long)data[offset + 1] & 0xFF) << 16 |
                ((long)data[offset + 2] & 0xFF) << 8 |
                ((long)data[offset + 3] & 0xFF);
    }

    private static boolean isCanonicalUuid(String value) {
        try {
            return value != null &&
                    UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static String hexSha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder output = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                output.append(String.format("%02x", value & 0xFF));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    private static final class BlobReference {
        final byte targetMime;
        final long size;
        final byte[] sha256;
        final String id;

        BlobReference(byte targetMime, long size, byte[] sha256, String id) {
            this.targetMime = targetMime;
            this.size = size;
            this.sha256 = sha256;
            this.id = id;
        }
    }
}
