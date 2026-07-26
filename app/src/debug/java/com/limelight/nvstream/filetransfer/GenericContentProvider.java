package com.limelight.nvstream.filetransfer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public final class GenericContentProvider extends ContentProvider {
    static final String FILE_NAME = "generic-provider-file.bin";
    static final byte[] FILE_CONTENT = new byte[] {1, 2, 3, 4, 5};

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if ("without-metadata".equals(uri.getLastPathSegment())) {
            throw new UnsupportedOperationException();
        }
        MatrixCursor cursor = new MatrixCursor(
                new String[] {
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE
                });
        cursor.addRow(new Object[] {FILE_NAME, FILE_CONTENT.length});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "application/octet-stream";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        File file = new File(getContext().getCacheDir(), FILE_NAME);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(FILE_CONTENT);
        } catch (IOException error) {
            FileNotFoundException failure =
                    new FileNotFoundException("Unable to create test content");
            failure.initCause(error);
            throw failure;
        }
        return ParcelFileDescriptor.open(
                file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
