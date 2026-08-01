package com.limelight.computers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;

@RunWith(AndroidJUnit4.class)
public final class ComputerDatabaseManagerTest {
    private static final String DATABASE_NAME = "host-repository-test.db";

    private Context context;
    private File cryptoDirectory;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
        cryptoDirectory = new File(
                context.getCacheDir(),
                "host-repository-crypto");
        deleteRecursively(cryptoDirectory);
        assertTrue(cryptoDirectory.mkdirs());
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DATABASE_NAME);
        deleteRecursively(cryptoDirectory);
    }

    @Test
    public void migratesLegacyCertificateAndPreservesItAcrossMetadataWrites()
            throws Exception {
        X509Certificate certificate = createCertificate();
        createLegacyDatabase(certificate);

        ComputerDatabaseManager manager = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        ComputerDetails computer = manager.getComputerByUUID("host-id");
        assertNotNull(computer);
        assertArrayEquals(
                certificate.getEncoded(),
                computer.serverCert.getEncoded());

        computer.name = "Renamed host";
        computer.serverCert = null;
        manager.updateComputerMetadata(computer);
        ComputerDetails reloaded = manager.getComputerByUUID("host-id");
        assertEquals("Renamed host", reloaded.name);
        assertArrayEquals(
                certificate.getEncoded(),
                reloaded.serverCert.getEncoded());
        manager.close();

        assertCredentialWasMovedOutOfLegacyColumn();
    }

    @Test
    public void importAndDeleteMutateMetadataAndCredentialAtomically()
            throws Exception {
        X509Certificate certificate = createCertificate();
        ComputerDetails computer = new ComputerDetails();
        computer.uuid = "imported-host";
        computer.name = "Imported host";
        computer.serverCert = certificate;

        ComputerDatabaseManager manager = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        assertTrue(manager.importComputer(computer));
        ComputerDetails imported = manager.getComputerByUUID(computer.uuid);
        assertNotNull(imported);
        assertArrayEquals(
                certificate.getEncoded(),
                imported.serverCert.getEncoded());

        manager.deleteComputer(imported);
        assertNull(manager.getComputerByUUID(computer.uuid));
        manager.close();
    }

    @Test
    public void corruptDatabaseIsNeverSilentlyDeleted() throws Exception {
        File databaseFile = context.getDatabasePath(DATABASE_NAME);
        File parent = databaseFile.getParentFile();
        assertNotNull(parent);
        assertTrue(parent.exists() || parent.mkdirs());
        byte[] corruptBytes = new byte[]{1, 2, 3, 4, 5};
        try (FileOutputStream output = new FileOutputStream(databaseFile)) {
            output.write(corruptBytes);
        }

        assertThrows(
                RuntimeException.class,
                () -> new ComputerDatabaseManager(
                        context,
                        DATABASE_NAME,
                        false));
        assertTrue(databaseFile.exists());
        assertEquals(corruptBytes.length, databaseFile.length());
    }

    private X509Certificate createCertificate() {
        Context isolatedContext = new ContextWrapper(context) {
            @Override
            public File getFilesDir() {
                return cryptoDirectory;
            }
        };
        return new AndroidCryptoProvider(isolatedContext)
                .getClientCertificate();
    }

    private void createLegacyDatabase(X509Certificate certificate)
            throws Exception {
        try (SQLiteDatabase database = context.openOrCreateDatabase(
                DATABASE_NAME,
                Context.MODE_PRIVATE,
                null)) {
            database.execSQL(
                    "CREATE TABLE Computers(" +
                            "UUID TEXT PRIMARY KEY, " +
                            "ComputerName TEXT NOT NULL, " +
                            "Addresses TEXT NOT NULL, " +
                            "MacAddress TEXT, " +
                            "ServerCert BLOB)");
            ContentValues values = new ContentValues();
            values.put("UUID", "host-id");
            values.put("ComputerName", "Legacy host");
            values.put("Addresses", new JSONObject().toString());
            values.put("MacAddress", "00:11:22:33:44:55");
            values.put("ServerCert", certificate.getEncoded());
            assertTrue(database.insert("Computers", null, values) != -1);
        }
    }

    private void assertCredentialWasMovedOutOfLegacyColumn() {
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                context.getDatabasePath(DATABASE_NAME).getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY)) {
            try (Cursor cursor = database.rawQuery(
                    "SELECT ServerCert FROM Computers WHERE UUID='host-id'",
                    null)) {
                assertTrue(cursor.moveToFirst());
                assertTrue(cursor.isNull(0));
            }
            try (Cursor cursor = database.rawQuery(
                    "SELECT COUNT(*) FROM HostCredentials " +
                            "WHERE HostId='host-id' AND ServerCert IS NOT NULL",
                    null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(1, cursor.getInt(0));
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        assertTrue(file.delete());
    }
}
