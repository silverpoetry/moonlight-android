package com.limelight.computers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
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
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public final class ComputerDatabaseManagerTest {
    private static final String DATABASE_NAME = "host-repository-test.db";
    private static final String LEGACY_DATABASE_NAME = "computers3.db";
    private static final String MISSING_DATABASE_NAME =
            "missing-legacy-host-repository-test.db";

    private Context context;
    private File cryptoDirectory;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
        context.deleteDatabase(LEGACY_DATABASE_NAME);
        context.deleteDatabase(MISSING_DATABASE_NAME);
        cryptoDirectory = new File(
                context.getCacheDir(),
                "host-repository-crypto");
        deleteRecursively(cryptoDirectory);
        assertTrue(cryptoDirectory.mkdirs());
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DATABASE_NAME);
        context.deleteDatabase(LEGACY_DATABASE_NAME);
        context.deleteDatabase(MISSING_DATABASE_NAME);
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

    @Test
    public void missingLegacyDatabaseNeverReachesSQLiteReader() {
        AtomicBoolean readerCalled = new AtomicBoolean();

        LegacyHostDatabaseMigration migration =
                LegacyHostDatabaseMigration.read(
                        context,
                        MISSING_DATABASE_NAME,
                        database -> {
                            readerCalled.set(true);
                            throw new AssertionError(
                                    "Missing database must not be opened");
                        });

        assertFalse(migration.isReady());
        assertFalse(readerCalled.get());
        assertFalse(context.getDatabasePath(MISSING_DATABASE_NAME).exists());
    }

    @Test
    public void corruptLegacyDatabaseIsPreservedAndCurrentRepositoryOpens()
            throws Exception {
        File legacyFile = context.getDatabasePath(LEGACY_DATABASE_NAME);
        File parent = legacyFile.getParentFile();
        assertNotNull(parent);
        assertTrue(parent.exists() || parent.mkdirs());
        byte[] corruptBytes = new byte[]{4, 3, 2, 1};
        try (FileOutputStream output = new FileOutputStream(legacyFile)) {
            output.write(corruptBytes);
        }

        ComputerDatabaseManager manager = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                true);

        assertTrue(manager.getAllComputers().isEmpty());
        manager.close();
        assertTrue(legacyFile.exists());
        assertEquals(corruptBytes.length, legacyFile.length());
    }

    @Test
    public void successfulLegacyMigrationDoesNotOverwriteCurrentHost()
            throws Exception {
        ComputerDetails current = new ComputerDetails();
        current.uuid = "existing-host";
        current.name = "Current host";

        ComputerDatabaseManager initial = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        assertTrue(initial.importComputer(current));
        initial.close();

        try (SQLiteDatabase legacy = createLegacy3Database()) {
            insertLegacy3Computer(
                    legacy,
                    "existing-host",
                    "Stale legacy host");
            insertLegacy3Computer(
                    legacy,
                    "imported-host",
                    "Imported host");
        }

        ComputerDatabaseManager migrated = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                true);

        assertEquals(
                "Current host",
                migrated.getComputerByUUID("existing-host").name);
        assertEquals(
                "Imported host",
                migrated.getComputerByUUID("imported-host").name);
        migrated.close();
        assertFalse(context.getDatabasePath(LEGACY_DATABASE_NAME).exists());
    }

    @Test
    public void failedLegacyImportRollsBackAndPreservesSource() {
        try (SQLiteDatabase legacy = createLegacy3Database()) {
            insertLegacy3Computer(legacy, "valid-host", "Valid host");
            insertLegacy3Computer(legacy, "invalid-host", null);
        }

        ComputerDatabaseManager manager = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                true);

        assertNull(manager.getComputerByUUID("valid-host"));
        assertNull(manager.getComputerByUUID("invalid-host"));
        manager.close();
        assertTrue(context.getDatabasePath(LEGACY_DATABASE_NAME).exists());
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

    private SQLiteDatabase createLegacy3Database() {
        SQLiteDatabase database = context.openOrCreateDatabase(
                LEGACY_DATABASE_NAME,
                Context.MODE_PRIVATE,
                null);
        database.execSQL(
                "CREATE TABLE Computers(" +
                        "UUID TEXT PRIMARY KEY, " +
                        "ComputerName TEXT, " +
                        "Addresses TEXT NOT NULL, " +
                        "MacAddress TEXT, " +
                        "ServerCert BLOB)");
        return database;
    }

    private static void insertLegacy3Computer(
            SQLiteDatabase database,
            String hostId,
            String name) {
        ContentValues values = new ContentValues();
        values.put("UUID", hostId);
        values.put("ComputerName", name);
        values.put("Addresses", "192.168.1.2_47989;;;");
        values.put("MacAddress", "00:11:22:33:44:55");
        values.putNull("ServerCert");
        assertTrue(database.insert("Computers", null, values) != -1);
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
