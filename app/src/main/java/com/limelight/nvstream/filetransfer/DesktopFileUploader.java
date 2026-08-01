package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.net.Uri;

import com.limelight.nvstream.http.NvHTTP;
import com.limelight.transfer.FileManifest;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class DesktopFileUploader {
    public interface Listener {
        void onProgress(long transferredBytes, long totalBytes);
    }

    private DesktopFileUploader() {
    }

    public static void upload(Context context, NvHTTP http, List<Uri> sourceUris,
                              Listener listener) throws IOException {
        List<DesktopFileUploadSource> sources =
                DesktopFileUploadSourceEnumerator.enumerate(
                        context,
                        sourceUris);
        List<FileManifest.Entry> entries = new ArrayList<>(sources.size());
        for (DesktopFileUploadSource source : sources) {
            entries.add(source.getManifestEntry());
        }
        FileManifest manifest = FileManifest.validate(entries);
        byte[] encoded = FileManifest.encode(manifest.entries);
        String token = randomToken();
        NvHTTP.DesktopFileUploadResult transfer = http.beginDesktopFileUpload(
                encoded, token, UUID.randomUUID().toString());

        long transferred = 0;
        byte[] buffer = new byte[FileManifest.MAX_CHUNK_BYTES];
        for (int index = 0; index < manifest.entries.size(); index++) {
            FileManifest.Entry entry = manifest.entries.get(index);
            if (entry.type != FileManifest.TYPE_REGULAR) {
                continue;
            }
            Uri sourceUri = sources.get(index).getSourceUri();
            if (sourceUri == null) {
                throw new IOException("Shared file is no longer available");
            }

            try (InputStream input = context.getContentResolver()
                    .openInputStream(sourceUri)) {
                if (input == null) {
                    throw new IOException("Unable to open " + entry.path);
                }
                long offset = 0;
                while (offset < entry.size) {
                    int requested = (int)Math.min(buffer.length, entry.size - offset);
                    int length = readFully(input, buffer, requested);
                    if (length != requested) {
                        throw new IOException("Shared file changed during transfer");
                    }
                    byte[] chunk = length == buffer.length ?
                            buffer.clone() :
                            Arrays.copyOf(buffer, length);
                    http.uploadDesktopFileChunk(
                            transfer.id, token, index, offset, chunk);
                    offset += length;
                    transferred += length;
                    if (listener != null) {
                        listener.onProgress(transferred, manifest.totalFileBytes);
                    }
                }
                if (input.read() != -1) {
                    throw new IOException("Shared file changed during transfer");
                }
            }
        }
        http.completeDesktopFileUpload(transfer.id, token);
    }

    private static int readFully(InputStream input, byte[] buffer, int length)
            throws IOException {
        int total = 0;
        while (total < length) {
            int read = input.read(buffer, total, length - total);
            if (read < 0) {
                break;
            }
            if (read == 0) {
                int value = input.read();
                if (value < 0) {
                    break;
                }
                buffer[total++] = (byte)value;
            }
            else {
                total += read;
            }
        }
        return total;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        char[] alphabet = "0123456789abcdef".toCharArray();
        char[] encoded = new char[64];
        for (int index = 0; index < bytes.length; index++) {
            encoded[index * 2] = alphabet[(bytes[index] >>> 4) & 0xF];
            encoded[index * 2 + 1] = alphabet[bytes[index] & 0xF];
        }
        return new String(encoded);
    }

}
