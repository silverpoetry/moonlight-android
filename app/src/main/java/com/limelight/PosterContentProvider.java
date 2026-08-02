package com.limelight;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.limelight.grid.assets.DiskAssetLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public final class PosterContentProvider extends ContentProvider {
    private static final String AUTHORITY_PREFIX = "poster.";
    public static final String PNG_MIME_TYPE = "image/png";
    private static final String BOXART_PATH = "boxart";
    private static final int BOXART_URI_ID = 1;
    private static final int APP_ID_PATH_INDEX = 2;
    private static final int COMPUTER_UUID_PATH_INDEX = 1;

    private DiskAssetLoader diskAssetLoader;
    private UriMatcher uriMatcher;

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (uriMatcher.match(uri) != BOXART_URI_ID) {
            throw new FileNotFoundException("Unknown poster URI");
        }
        if (!"r".equals(mode)) {
            throw new UnsupportedOperationException("This provider is only for read mode");
        }

        List<String> segments = uri.getPathSegments();
        String computerUuid = segments.get(COMPUTER_UUID_PATH_INDEX);
        if (!isSafePathComponent(computerUuid)) {
            throw new FileNotFoundException("Invalid computer identifier");
        }

        final int appId;
        try {
            appId = Integer.parseInt(segments.get(APP_ID_PATH_INDEX));
        }
        catch (NumberFormatException e) {
            throw new FileNotFoundException("Invalid application identifier");
        }

        File file = diskAssetLoader.getFile(computerUuid, appId);
        if (!isWithinBoxArtCache(file) || !file.isFile()) {
            throw new FileNotFoundException("Poster not found");
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is only for read mode");
    }

    @Override
    public String getType(Uri uri) {
        return PNG_MIME_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("This provider is only for read mode");
    }

    @Override
    public boolean onCreate() {
        diskAssetLoader = new DiskAssetLoader(getContext());
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(
                getAuthority(getContext()),
                BOXART_PATH + "/*/#",
                BOXART_URI_ID);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("This provider doesn't support query");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is read only");
    }

    public static String getAuthority(android.content.Context context) {
        return AUTHORITY_PREFIX + context.getPackageName();
    }

    public static Uri createBoxArtUri(
            android.content.Context context,
            String uuid,
            String appId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(getAuthority(context))
                .appendPath(BOXART_PATH)
                .appendPath(uuid)
                .appendPath(appId)
                .build();
    }

    private static boolean isSafePathComponent(String value) {
        return value != null &&
                !value.isEmpty() &&
                !".".equals(value) &&
                !"..".equals(value) &&
                value.indexOf('/') < 0 &&
                value.indexOf('\\') < 0 &&
                value.indexOf(File.separatorChar) < 0;
    }

    private boolean isWithinBoxArtCache(File file) {
        try {
            File root = new File(getContext().getCacheDir(), BOXART_PATH)
                    .getCanonicalFile();
            File candidate = file.getCanonicalFile();
            return candidate.getParentFile() != null &&
                    candidate.getParentFile().getParentFile() != null &&
                    root.equals(candidate.getParentFile().getParentFile());
        }
        catch (IOException e) {
            return false;
        }
    }
}
