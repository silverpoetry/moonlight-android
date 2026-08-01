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
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public final class ComputerDatabaseManagerTest {
    private static final String DATABASE_NAME = "host-repository-test.db";
    private static final String SOURCE_DATABASE_NAME =
            "host-repository-source-test.db";
    private static final String LEGACY_DATABASE_NAME = "computers3.db";
    private static final String MISSING_DATABASE_NAME =
            "missing-legacy-host-repository-test.db";

    private Context context;
    private File cryptoDirectory;
    private File snapshotFile;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
        context.deleteDatabase(SOURCE_DATABASE_NAME);
        context.deleteDatabase(LEGACY_DATABASE_NAME);
        context.deleteDatabase(MISSING_DATABASE_NAME);
        cryptoDirectory = new File(
                context.getCacheDir(),
                "host-repository-crypto");
        snapshotFile = new File(
                context.getCacheDir(),
                "host-repository-snapshot-test.db");
        SQLiteDatabase.deleteDatabase(snapshotFile);
        deleteRecursively(cryptoDirectory);
        assertTrue(cryptoDirectory.mkdirs());
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DATABASE_NAME);
        context.deleteDatabase(SOURCE_DATABASE_NAME);
        context.deleteDatabase(LEGACY_DATABASE_NAME);
        context.deleteDatabase(MISSING_DATABASE_NAME);
        SQLiteDatabase.deleteDatabase(snapshotFile);
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
        ComputerDetails computer = findComputer(manager, "host-id");
        assertNotNull(computer);
        assertArrayEquals(
                certificate.getEncoded(),
                computer.serverCert.getEncoded());

        computer.name = "Renamed host";
        computer.serverCert = null;
        updateComputerMetadata(manager, computer);
        ComputerDetails reloaded = findComputer(manager, "host-id");
        assertEquals("Renamed host", reloaded.name);
        assertArrayEquals(
                certificate.getEncoded(),
                reloaded.serverCert.getEncoded());
        manager.close();

        assertCredentialWasMovedOutOfLegacyColumn();
        assertUserAliasColumnWasAdded();
    }

    @Test
    public void canonicalIdentityPreservesExistingMixedCaseStorageKey()
            throws Exception {
        X509Certificate certificate = createCertificate();
        createLegacyDatabase(certificate, "Mixed-Case-Host");
        ComputerDatabaseManager manager = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);

        ComputerDetails computer = findComputer(
                manager,
                "mixed-case-host");
        assertNotNull(computer);
        computer.name = "Updated host";
        updateComputerMetadata(manager, computer);
        manager.updatePinnedCertificate(
                HostId.of("MIXED-CASE-HOST"),
                certificate);
        manager.close();

        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                context.getDatabasePath(DATABASE_NAME).getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY);
                Cursor metadata = database.rawQuery(
                        "SELECT UUID, ComputerName FROM Computers",
                        null);
                Cursor credential = database.rawQuery(
                        "SELECT HostId FROM HostCredentials",
                        null)) {
            assertTrue(metadata.moveToFirst());
            assertEquals("Mixed-Case-Host", metadata.getString(0));
            assertEquals("Updated host", metadata.getString(1));
            assertFalse(metadata.moveToNext());
            assertTrue(credential.moveToFirst());
            assertEquals("Mixed-Case-Host", credential.getString(0));
            assertFalse(credential.moveToNext());
        }
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
        assertTrue(importComputer(manager, computer));
        ComputerDetails imported = findComputer(manager, computer.uuid);
        assertNotNull(imported);
        assertArrayEquals(
                certificate.getEncoded(),
                imported.serverCert.getEncoded());

        manager.deleteHost(HostId.of(imported.uuid));
        assertNull(findComputer(manager, computer.uuid));
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
    public void newerSchemaIsRejectedAndPreserved() {
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
            database.setVersion(7);
        }

        assertThrows(
                IllegalStateException.class,
                () -> new ComputerDatabaseManager(
                        context,
                        DATABASE_NAME,
                        false));
        try (SQLiteDatabase preserved = SQLiteDatabase.openDatabase(
                context.getDatabasePath(DATABASE_NAME).getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(7, preserved.getVersion());
            assertTrue(tableExists(preserved, "Computers"));
        }
    }

    @Test
    public void newerPortableSnapshotIsRejectedAndPreserved() {
        try (SQLiteDatabase database =
                     SQLiteDatabase.openOrCreateDatabase(
                             snapshotFile,
                             null)) {
            database.execSQL(
                    "CREATE TABLE Computers(" +
                            "UUID TEXT PRIMARY KEY, " +
                            "ComputerName TEXT NOT NULL, " +
                            "Addresses TEXT NOT NULL, " +
                            "MacAddress TEXT, " +
                            "ServerCert BLOB)");
            database.setVersion(7);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> new ComputerDatabaseManager(
                        context,
                        snapshotFile));
        try (SQLiteDatabase preserved = SQLiteDatabase.openDatabase(
                snapshotFile.getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(7, preserved.getVersion());
            assertTrue(tableExists(preserved, "Computers"));
        }
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

        assertTrue(manager.getAllHosts().isEmpty());
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
        assertTrue(importComputer(initial, current));
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
                findComputer(migrated, "existing-host").name);
        assertEquals(
                "Imported host",
                findComputer(migrated, "imported-host").name);
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

        assertNull(findComputer(manager, "valid-host"));
        assertNull(findComputer(manager, "invalid-host"));
        manager.close();
        assertTrue(context.getDatabasePath(LEGACY_DATABASE_NAME).exists());
    }

    @Test
    public void portableSnapshotRoundTripsMetadataAndCredential()
            throws Exception {
        X509Certificate certificate = createCertificate();
        ComputerDetails original = new ComputerDetails();
        original.uuid = "snapshot-host";
        original.name = "Snapshot host";
        original.localAddress = new ComputerDetails.AddressTuple(
                "192.168.1.10",
                47989);
        original.serverCert = certificate;

        ComputerDatabaseManager source = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        assertTrue(importComputer(source, original));
        source.writePortableSnapshot(snapshotFile);
        source.close();

        assertTrue(snapshotFile.isFile());
        ComputerDatabaseManager snapshot =
                new ComputerDatabaseManager(context, snapshotFile);
        ComputerDetails restored = findComputer(
                snapshot,
                original.uuid);
        assertNotNull(restored);
        assertEquals(original.name, restored.name);
        assertEquals(original.localAddress, restored.localAddress);
        assertArrayEquals(
                certificate.getEncoded(),
                restored.serverCert.getEncoded());
        snapshot.close();
    }

    @Test
    public void userAliasSurvivesMetadataWriteAndPortableSnapshot() {
        HostRecord original = new HostRecord(
                new HostIdentity(
                        HostId.of("aliased-host"),
                        "Advertised host",
                        "Living room"),
                Collections.singletonList(new HostEndpoint(
                        HostEndpoint.Kind.LOCAL_IPV4,
                        "192.168.1.20",
                        47989)),
                "00:11:22:33:44:55");
        ComputerDatabaseManager source = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        assertTrue(source.importHost(new PersistedHost(original, null)));
        assertEquals(
                original,
                source.findHost(HostId.of("ALIASED-HOST")).getRecord());

        source.writePortableSnapshot(snapshotFile);
        source.close();

        ComputerDatabaseManager snapshot =
                new ComputerDatabaseManager(context, snapshotFile);
        PersistedHost restored = snapshot.findHost(
                HostId.of("aliased-host"));
        assertNotNull(restored);
        assertEquals(original, restored.getRecord());
        assertEquals(
                "Living room",
                restored.getRecord().getIdentity().getUserAlias());
        snapshot.close();
    }

    @Test
    public void corruptImportIsRejectedBeforeSQLiteAndPreserved()
            throws Exception {
        byte[] corruptBytes = new byte[]{9, 8, 7, 6};
        try (FileOutputStream output =
                     new FileOutputStream(snapshotFile)) {
            output.write(corruptBytes);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> new ComputerDatabaseManager(
                        context,
                        snapshotFile));
        assertTrue(snapshotFile.exists());
        assertEquals(corruptBytes.length, snapshotFile.length());
    }

    @Test
    public void restoreIsAtomicWhenAnySourceRecordIsInvalid() {
        ComputerDetails current = new ComputerDetails();
        current.uuid = "current-host";
        current.name = "Current host";
        ComputerDatabaseManager destination = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        assertTrue(importComputer(destination, current));

        createCurrentSchemaSourceWithInvalidRecord();
        ComputerDatabaseManager source = new ComputerDatabaseManager(
                context,
                context.getDatabasePath(SOURCE_DATABASE_NAME));

        assertThrows(
                IllegalArgumentException.class,
                () -> destination.restoreFrom(source));
        assertNotNull(findComputer(destination, "current-host"));
        assertNull(findComputer(destination, "valid-source-host"));
        assertNull(findComputer(destination, "invalid-source-host"));

        source.close();
        destination.close();
    }

    @Test
    public void restoreReplacesCredentialAndClearsMissingCredential()
            throws Exception {
        X509Certificate certificate = createCertificate();
        ComputerDatabaseManager destination = new ComputerDatabaseManager(
                context,
                DATABASE_NAME,
                false);
        ComputerDetails destinationHost = new ComputerDetails();
        destinationHost.uuid = "restore-host";
        destinationHost.name = "Old host";
        destinationHost.serverCert = certificate;
        assertTrue(importComputer(destination, destinationHost));

        ComputerDatabaseManager source = new ComputerDatabaseManager(
                context,
                SOURCE_DATABASE_NAME,
                false);
        ComputerDetails sourceHost = new ComputerDetails();
        sourceHost.uuid = destinationHost.uuid;
        sourceHost.name = "Restored host";
        assertTrue(importComputer(source, sourceHost));

        assertEquals(1, destination.restoreFrom(source));
        ComputerDetails restored = findComputer(
                destination,
                destinationHost.uuid);
        assertEquals("Restored host", restored.name);
        assertNull(restored.serverCert);

        source.close();
        destination.close();
    }

    private static ComputerDetails findComputer(
            ComputerDatabaseManager manager,
            String hostId) {
        PersistedHost host = manager.findHost(HostId.of(hostId));
        return host == null
                ? null
                : LegacyHostDetailsAdapter.toComputerDetails(host);
    }

    private static boolean importComputer(
            ComputerDatabaseManager manager,
            ComputerDetails computer) {
        return manager.importHost(
                LegacyHostDetailsAdapter.toPersistedHost(computer));
    }

    private static void updateComputerMetadata(
            ComputerDatabaseManager manager,
            ComputerDetails computer) {
        manager.updateHostMetadata(
                LegacyHostDetailsAdapter.toHostRecord(computer));
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
        createLegacyDatabase(certificate, "host-id");
    }

    private void createLegacyDatabase(
            X509Certificate certificate,
            String hostId) throws Exception {
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
            values.put("UUID", hostId);
            values.put("ComputerName", "Legacy host");
            values.put("Addresses", new JSONObject().toString());
            values.put("MacAddress", "00:11:22:33:44:55");
            values.put("ServerCert", certificate.getEncoded());
            assertTrue(database.insert("Computers", null, values) != -1);
            database.setVersion(5);
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

    private void createCurrentSchemaSourceWithInvalidRecord() {
        try (SQLiteDatabase database = context.openOrCreateDatabase(
                SOURCE_DATABASE_NAME,
                Context.MODE_PRIVATE,
                null)) {
            database.execSQL(
                    "CREATE TABLE Computers(" +
                            "UUID TEXT PRIMARY KEY, " +
                            "ComputerName TEXT NOT NULL, " +
                            "Addresses TEXT NOT NULL, " +
                            "MacAddress TEXT, " +
                            "ServerCert BLOB)");
            database.execSQL(
                    "CREATE TABLE HostCredentials(" +
                            "HostId TEXT PRIMARY KEY, " +
                            "ServerCert BLOB NOT NULL)");
            insertStoredHost(
                    database,
                    "valid-source-host",
                    "Valid source host",
                    new JSONObject().toString());
            insertStoredHost(
                    database,
                    "invalid-source-host",
                    "Invalid source host",
                    "{");
        }
    }

    private static void insertStoredHost(
            SQLiteDatabase database,
            String hostId,
            String name,
            String addresses) {
        ContentValues values = new ContentValues();
        values.put("UUID", hostId);
        values.put("ComputerName", name);
        values.put("Addresses", addresses);
        values.putNull("MacAddress");
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

    private void assertUserAliasColumnWasAdded() {
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                context.getDatabasePath(DATABASE_NAME).getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = database.rawQuery(
                        "SELECT UserAlias FROM Computers " +
                                "WHERE UUID='host-id'",
                        null)) {
            assertEquals(6, database.getVersion());
            assertTrue(cursor.moveToFirst());
            assertTrue(cursor.isNull(0));
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

    private static boolean tableExists(
            SQLiteDatabase database,
            String tableName) {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM sqlite_master " +
                        "WHERE type='table' AND name=?",
                new String[]{tableName})) {
            return cursor.moveToFirst();
        }
    }
}
