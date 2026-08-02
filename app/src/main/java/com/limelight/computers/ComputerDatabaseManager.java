package com.limelight.computers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.limelight.LimeLog;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Persistent host repository and the sole owner of pinned host certificates. */
public final class ComputerDatabaseManager implements HostRepository {
    public static final String COMPUTER_DB_NAME = "computers4.db";

    private static final int SCHEMA_VERSION = 6;
    private static final String COMPUTER_TABLE_NAME = "Computers";
    private static final String COMPUTER_UUID_COLUMN_NAME = "UUID";
    private static final String COMPUTER_NAME_COLUMN_NAME = "ComputerName";
    private static final String USER_ALIAS_COLUMN_NAME = "UserAlias";
    private static final String ADDRESSES_COLUMN_NAME = "Addresses";
    private static final String MAC_ADDRESS_COLUMN_NAME = "MacAddress";
    private static final String LEGACY_SERVER_CERT_COLUMN_NAME = "ServerCert";
    private static final String CREDENTIAL_TABLE_NAME = "HostCredentials";
    private static final String CREDENTIAL_HOST_ID_COLUMN_NAME = "HostId";
    private static final String CREDENTIAL_CERT_COLUMN_NAME = "ServerCert";

    private interface AddressFields {
        String LOCAL = "local";
        String REMOTE = "remote";
        String MANUAL = "manual";
        String IPV6 = "ipv6";
        String ADDRESS = "address";
        String PORT = "port";
    }

    private final File databaseFile;
    private final SQLiteDatabase computerDb;

    private static final class StoredHostRecord {
        final String hostId;
        final String name;
        final String userAlias;
        final String addresses;
        final String macAddress;
        final byte[] certificate;

        StoredHostRecord(
                String hostId,
                String name,
                String userAlias,
                String addresses,
                String macAddress,
                byte[] certificate) {
            this.hostId = hostId;
            this.name = name;
            this.userAlias = userAlias;
            this.addresses = addresses;
            this.macAddress = macAddress;
            this.certificate = certificate;
        }
    }

    public ComputerDatabaseManager(Context context) {
        this(context, COMPUTER_DB_NAME, true);
    }

    ComputerDatabaseManager(
            Context context,
            String databaseName,
            boolean migrateLegacyDatabases) {
        Objects.requireNonNull(context, "context");
        String checkedDatabaseName = Objects.requireNonNull(
                databaseName,
                "databaseName");
        databaseFile = context.getDatabasePath(checkedDatabaseName);
        SQLiteDatabaseFileHeader.requireValidIfPopulated(databaseFile);
        computerDb = context.openOrCreateDatabase(
                checkedDatabaseName,
                Context.MODE_PRIVATE,
                null);
        boolean initialized = false;
        try {
            initializeDb(context, migrateLegacyDatabases);
            initialized = true;
        }
        finally {
            if (!initialized) {
                computerDb.close();
            }
        }
    }

    /** Opens an exported database without mutating its schema. */
    public ComputerDatabaseManager(Context context, File file) {
        Objects.requireNonNull(context, "context");
        File checkedFile = Objects.requireNonNull(file, "file");
        if (!SQLiteDatabaseFileHeader.isValid(checkedFile)) {
            throw new IllegalArgumentException("Not a host database");
        }
        databaseFile = checkedFile;
        computerDb = SQLiteDatabase.openDatabase(
                checkedFile.getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY);
        boolean initialized = false;
        try {
            requireComputerTable();
            requireSupportedSnapshotVersion();
            initialized = true;
        }
        finally {
            if (!initialized) {
                computerDb.close();
            }
        }
    }

    @Override
    public void close() {
        computerDb.close();
    }

    private void initializeDb(
            Context context,
            boolean migrateLegacyDatabases) {
        createOrUpgradeCurrentSchema(computerDb);

        if (migrateLegacyDatabases) {
            migrateLegacyDatabase(
                    context,
                    LegacyDatabaseReader.readMigration(context));
            migrateLegacyDatabase(
                    context,
                    LegacyDatabaseReader2.readMigration(context));
            migrateLegacyDatabase(
                    context,
                    LegacyDatabaseReader3.readMigration(context));
        }
    }

    private static void createOrUpgradeCurrentSchema(
            SQLiteDatabase database) {
        int storedVersion = database.getVersion();
        if (storedVersion > SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Host database schema is newer than this application");
        }
        database.beginTransaction();
        try {
            database.execSQL(String.format(
                    (Locale) null,
                    "CREATE TABLE IF NOT EXISTS %s(" +
                            "%s TEXT PRIMARY KEY, " +
                            "%s TEXT NOT NULL, " +
                            "%s TEXT, " +
                            "%s TEXT NOT NULL, " +
                            "%s TEXT, " +
                            "%s BLOB)",
                    COMPUTER_TABLE_NAME,
                    COMPUTER_UUID_COLUMN_NAME,
                    COMPUTER_NAME_COLUMN_NAME,
                    USER_ALIAS_COLUMN_NAME,
                    ADDRESSES_COLUMN_NAME,
                    MAC_ADDRESS_COLUMN_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME));
            if (!hasColumn(
                    database,
                    COMPUTER_TABLE_NAME,
                    USER_ALIAS_COLUMN_NAME)) {
                database.execSQL(String.format(
                        (Locale) null,
                        "ALTER TABLE %s ADD COLUMN %s TEXT",
                        COMPUTER_TABLE_NAME,
                        USER_ALIAS_COLUMN_NAME));
            }
            database.execSQL(String.format(
                    (Locale) null,
                    "CREATE TABLE IF NOT EXISTS %s(" +
                            "%s TEXT PRIMARY KEY, " +
                            "%s BLOB NOT NULL)",
                    CREDENTIAL_TABLE_NAME,
                    CREDENTIAL_HOST_ID_COLUMN_NAME,
                    CREDENTIAL_CERT_COLUMN_NAME));

            // Upgrade computers4.db in place. Moving then clearing the legacy
            // column makes HostCredentials the only credential owner.
            database.execSQL(String.format(
                    (Locale) null,
                    "INSERT OR IGNORE INTO %s(%s, %s) " +
                            "SELECT %s, %s FROM %s WHERE %s IS NOT NULL",
                    CREDENTIAL_TABLE_NAME,
                    CREDENTIAL_HOST_ID_COLUMN_NAME,
                    CREDENTIAL_CERT_COLUMN_NAME,
                    COMPUTER_UUID_COLUMN_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME,
                    COMPUTER_TABLE_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME));
            database.execSQL(String.format(
                    (Locale) null,
                    "UPDATE %s SET %s=NULL WHERE %s IS NOT NULL",
                    COMPUTER_TABLE_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME));
            database.setVersion(SCHEMA_VERSION);
            database.setTransactionSuccessful();
        }
        finally {
            database.endTransaction();
        }
    }

    private void requireComputerTable() {
        if (!hasTable(COMPUTER_TABLE_NAME)) {
            throw new IllegalArgumentException("Not a host database");
        }
    }

    private void requireSupportedSnapshotVersion() {
        if (computerDb.getVersion() > SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Host snapshot schema is newer than this application");
        }
    }

    private boolean hasTable(String tableName) {
        return hasTable(computerDb, tableName);
    }

    private static boolean hasTable(
            SQLiteDatabase database,
            String tableName) {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName})) {
            return cursor.moveToFirst();
        }
    }

    private static boolean hasColumn(
            SQLiteDatabase database,
            String tableName,
            String columnName) {
        try (Cursor cursor = database.rawQuery(
                "PRAGMA table_info(" + tableName + ")",
                null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void migrateLegacyDatabase(
            Context context,
            LegacyHostDatabaseMigration migration) {
        if (!migration.isReady()) {
            return;
        }

        try {
            importLegacyComputers(migration.getComputers());
        }
        catch (RuntimeException error) {
            LimeLog.warning(
                    "Legacy host migration failed; preserving its source database");
            return;
        }
        migration.retire(context);
    }

    private void importLegacyComputers(List<ComputerDetails> computers) {
        computerDb.beginTransaction();
        try {
            for (ComputerDetails computer : computers) {
                insertLegacyComputer(computer);
            }
            computerDb.setTransactionSuccessful();
        }
        finally {
            computerDb.endTransaction();
        }
    }

    private void insertLegacyComputer(ComputerDetails details) {
        PersistedHost host = LegacyHostDetailsAdapter.toPersistedHost(
                details);
        ContentValues metadata = createHostMetadata(
                host.getRecord(),
                details.uuid);
        computerDb.insertWithOnConflict(
                COMPUTER_TABLE_NAME,
                null,
                metadata,
                SQLiteDatabase.CONFLICT_IGNORE);

        if (host.getPinnedCertificate() != null) {
            ContentValues credential = createCredential(
                    details.uuid,
                    host.getPinnedCertificate());
            computerDb.insertWithOnConflict(
                    CREDENTIAL_TABLE_NAME,
                    null,
                    credential,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    @Override
    public void deleteHost(HostId hostId) {
        String encodedHostId = resolveStoredHostId(
                Objects.requireNonNull(hostId, "hostId"));
        if (encodedHostId == null) {
            return;
        }
        computerDb.beginTransaction();
        try {
            computerDb.delete(
                    CREDENTIAL_TABLE_NAME,
                    CREDENTIAL_HOST_ID_COLUMN_NAME + "=?",
                    new String[]{encodedHostId});
            computerDb.delete(
                    COMPUTER_TABLE_NAME,
                    COMPUTER_UUID_COLUMN_NAME + "=?",
                    new String[]{encodedHostId});
            computerDb.setTransactionSuccessful();
        }
        finally {
            computerDb.endTransaction();
        }
    }

    @Override
    public boolean updateHostMetadata(HostRecord record) {
        return writeHostMetadata(record);
    }

    /** Atomically imports metadata and an optional pinned certificate. */
    @Override
    public boolean importHost(PersistedHost host) {
        PersistedHost source = Objects.requireNonNull(host, "host");
        computerDb.beginTransaction();
        try {
            boolean updated = writeHostMetadata(source.getRecord());
            if (source.getPinnedCertificate() != null) {
                writePinnedCertificate(
                        source.getRecord().getIdentity().getId(),
                        source.getPinnedCertificate());
            }
            computerDb.setTransactionSuccessful();
            return updated;
        }
        finally {
            computerDb.endTransaction();
        }
    }

    /** Writes a closed, self-contained snapshot without sharing the live DB. */
    public void writePortableSnapshot(File destination) {
        File checkedDestination = Objects.requireNonNull(
                destination,
                "destination");
        if (sameFile(databaseFile, checkedDestination)) {
            throw new IllegalArgumentException(
                    "Snapshot destination must differ from its source");
        }

        List<StoredHostRecord> records = readStoredHostRecords();
        File parent = checkedDestination.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            throw new IllegalStateException(
                    "Unable to create host snapshot directory");
        }
        if (checkedDestination.exists() &&
                !SQLiteDatabase.deleteDatabase(checkedDestination) &&
                checkedDestination.exists()) {
            throw new IllegalStateException(
                    "Unable to replace previous host snapshot");
        }

        try (SQLiteDatabase snapshot =
                     SQLiteDatabase.openOrCreateDatabase(
                             checkedDestination,
                             null)) {
            createOrUpgradeCurrentSchema(snapshot);
            replaceStoredHostRecords(snapshot, records, false);
        }
        catch (RuntimeException error) {
            SQLiteDatabase.deleteDatabase(checkedDestination);
            throw error;
        }

        if (!SQLiteDatabaseFileHeader.isValid(checkedDestination)) {
            SQLiteDatabase.deleteDatabase(checkedDestination);
            throw new IllegalStateException(
                    "Host snapshot was not finalized correctly");
        }
    }

    /** Atomically merges all validated records from a portable snapshot. */
    public int restoreFrom(ComputerDatabaseManager source) {
        List<StoredHostRecord> records = Objects.requireNonNull(
                source,
                "source").readStoredHostRecords();
        replaceStoredHostRecords(computerDb, records, true);
        return records.size();
    }

    private List<StoredHostRecord> readStoredHostRecords() {
        try (Cursor cursor = queryComputers(null, null)) {
            List<StoredHostRecord> records = new ArrayList<>();
            while (cursor.moveToNext()) {
                records.add(readStoredHostRecord(cursor));
            }
            return records;
        }
    }

    private static StoredHostRecord readStoredHostRecord(Cursor cursor) {
        String hostId = requireNonEmpty(cursor.getString(0), "host ID");
        String name = requireNonEmpty(cursor.getString(1), "computer name");
        String addresses = requireValidAddresses(cursor.getString(2));
        String macAddress = cursor.getString(3);
        String userAlias = cursor.getString(5);
        // Validate every domain field before a restore transaction starts.
        // The snapshot retains the exact stored key and JSON for compatibility,
        // while malformed identity or metadata can never enter the live DB.
        new HostRecord(
                new HostIdentity(
                        HostId.of(hostId),
                        name,
                        userAlias),
                decodeEndpoints(addresses),
                macAddress);
        byte[] certificate = cursor.getBlob(4);
        if (certificate != null) {
            requireValidCertificate(certificate);
        }
        return new StoredHostRecord(
                hostId,
                name,
                userAlias,
                addresses,
                macAddress,
                certificate);
    }

    private static String requireValidAddresses(String encoded) {
        String value = requireNonEmpty(encoded, "host endpoints");
        decodeEndpoints(value);
        return value;
    }

    private static void requireValidCertificate(byte[] encoded) {
        try {
            CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(encoded));
        }
        catch (CertificateException error) {
            throw new IllegalArgumentException(
                    "Stored pinned host certificate is invalid",
                    error);
        }
    }

    private static void replaceStoredHostRecords(
            SQLiteDatabase destination,
            List<StoredHostRecord> records,
            boolean clearMissingCredentials) {
        destination.beginTransaction();
        try {
            for (StoredHostRecord record : records) {
                ContentValues metadata = new ContentValues();
                metadata.put(COMPUTER_UUID_COLUMN_NAME, record.hostId);
                metadata.put(COMPUTER_NAME_COLUMN_NAME, record.name);
                metadata.put(USER_ALIAS_COLUMN_NAME, record.userAlias);
                metadata.put(ADDRESSES_COLUMN_NAME, record.addresses);
                metadata.put(MAC_ADDRESS_COLUMN_NAME, record.macAddress);
                metadata.putNull(LEGACY_SERVER_CERT_COLUMN_NAME);
                if (destination.insertWithOnConflict(
                        COMPUTER_TABLE_NAME,
                        null,
                        metadata,
                        SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                    throw new IllegalStateException(
                            "Unable to restore host metadata");
                }

                if (record.certificate != null) {
                    ContentValues credential = new ContentValues();
                    credential.put(
                            CREDENTIAL_HOST_ID_COLUMN_NAME,
                            record.hostId);
                    credential.put(
                            CREDENTIAL_CERT_COLUMN_NAME,
                            record.certificate);
                    if (destination.insertWithOnConflict(
                            CREDENTIAL_TABLE_NAME,
                            null,
                            credential,
                            SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                        throw new IllegalStateException(
                                "Unable to restore host credential");
                    }
                }
                else if (clearMissingCredentials) {
                    destination.delete(
                            CREDENTIAL_TABLE_NAME,
                            CREDENTIAL_HOST_ID_COLUMN_NAME + "=?",
                            new String[]{record.hostId});
                }
            }
            destination.setVersion(SCHEMA_VERSION);
            destination.setTransactionSuccessful();
        }
        finally {
            destination.endTransaction();
        }
    }

    private static boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(
                    second.getCanonicalFile());
        }
        catch (IOException error) {
            return first.getAbsoluteFile().equals(
                    second.getAbsoluteFile());
        }
    }

    /** Explicit credential mutation used only after successful pairing/import. */
    @Override
    public void updatePinnedCertificate(
            HostId hostId,
            X509Certificate certificate) {
        writePinnedCertificate(
                Objects.requireNonNull(hostId, "hostId"),
                Objects.requireNonNull(certificate, "certificate"));
    }

    private boolean writeHostMetadata(HostRecord record) {
        HostRecord host = Objects.requireNonNull(record, "record");
        HostId hostId = host.getIdentity().getId();
        String storedHostId = resolveStoredHostId(hostId);
        return computerDb.insertWithOnConflict(
                COMPUTER_TABLE_NAME,
                null,
                createHostMetadata(
                        host,
                        storedHostId == null
                                ? hostId.getValue()
                                : storedHostId),
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    private static ContentValues createHostMetadata(
            HostRecord record,
            String storedHostId) {
        HostRecord host = Objects.requireNonNull(record, "record");
        ContentValues values = new ContentValues();
        values.put(
                COMPUTER_UUID_COLUMN_NAME,
                requireNonEmpty(storedHostId, "host ID"));
        values.put(
                COMPUTER_NAME_COLUMN_NAME,
                host.getIdentity().getAdvertisedName());
        values.put(
                USER_ALIAS_COLUMN_NAME,
                host.getIdentity().getUserAlias());
        values.put(ADDRESSES_COLUMN_NAME, encodeEndpoints(host));
        values.put(MAC_ADDRESS_COLUMN_NAME, host.getMacAddress());
        values.putNull(LEGACY_SERVER_CERT_COLUMN_NAME);
        return values;
    }

    private void writePinnedCertificate(
            HostId hostId,
            X509Certificate certificate) {
        String storedHostId = resolveStoredHostId(hostId);
        if (storedHostId == null) {
            throw new IllegalArgumentException(
                    "Cannot persist a credential for an unknown host");
        }
        ContentValues values = createCredential(
                storedHostId,
                certificate);
        if (computerDb.insertWithOnConflict(
                CREDENTIAL_TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE) == -1) {
            throw new IllegalStateException(
                    "Unable to persist pinned server certificate");
        }
    }

    private static ContentValues createCredential(
            String storedHostId,
            X509Certificate certificate) {
        ContentValues values = new ContentValues();
        values.put(
                CREDENTIAL_HOST_ID_COLUMN_NAME,
                requireNonEmpty(storedHostId, "host ID"));
        try {
            values.put(
                    CREDENTIAL_CERT_COLUMN_NAME,
                    Objects.requireNonNull(certificate, "certificate")
                            .getEncoded());
        }
        catch (CertificateEncodingException error) {
            throw new IllegalArgumentException(
                    "Pinned server certificate cannot be encoded",
                    error);
        }
        return values;
    }

    private static String requireNonEmpty(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }

    private static String encodeEndpoints(HostRecord record) {
        try {
            JSONObject addresses = new JSONObject();
            addresses.put(
                    AddressFields.LOCAL,
                    endpointToJson(record.getEndpoint(
                            HostEndpoint.Kind.LOCAL_IPV4)));
            addresses.put(
                    AddressFields.REMOTE,
                    endpointToJson(record.getEndpoint(
                            HostEndpoint.Kind.REMOTE)));
            addresses.put(
                    AddressFields.MANUAL,
                    endpointToJson(record.getEndpoint(
                            HostEndpoint.Kind.MANUAL)));
            addresses.put(
                    AddressFields.IPV6,
                    endpointToJson(record.getEndpoint(
                            HostEndpoint.Kind.LOCAL_IPV6)));
            return addresses.toString();
        }
        catch (JSONException error) {
            throw new IllegalArgumentException(
                    "Unable to encode host endpoints",
                    error);
        }
    }

    private static JSONObject endpointToJson(
            HostEndpoint endpoint) throws JSONException {
        if (endpoint == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        json.put(AddressFields.ADDRESS, endpoint.getAddress());
        json.put(AddressFields.PORT, endpoint.getPort());
        return json;
    }

    private static HostEndpoint endpointFromJson(
            JSONObject json,
            String name,
            HostEndpoint.Kind kind) throws JSONException {
        if (!json.has(name) || json.isNull(name)) {
            return null;
        }
        JSONObject address = json.getJSONObject(name);
        return new HostEndpoint(
                kind,
                address.getString(AddressFields.ADDRESS),
                address.getInt(AddressFields.PORT));
    }

    private PersistedHost getHostFromCursor(Cursor cursor) {
        HostRecord record = new HostRecord(
                new HostIdentity(
                        HostId.of(cursor.getString(0)),
                        cursor.getString(1),
                        cursor.getString(5)),
                decodeEndpoints(cursor.getString(2)),
                cursor.getString(3));
        return new PersistedHost(
                record,
                decodeCertificate(cursor.getBlob(4)));
    }

    private static List<HostEndpoint> decodeEndpoints(String encoded) {
        List<HostEndpoint> endpoints = new ArrayList<>(4);
        try {
            JSONObject addresses = new JSONObject(requireNonEmpty(
                    encoded,
                    "host endpoints"));
            addEndpoint(
                    endpoints,
                    endpointFromJson(
                            addresses,
                            AddressFields.LOCAL,
                            HostEndpoint.Kind.LOCAL_IPV4));
            addEndpoint(
                    endpoints,
                    endpointFromJson(
                            addresses,
                            AddressFields.REMOTE,
                            HostEndpoint.Kind.REMOTE));
            addEndpoint(
                    endpoints,
                    endpointFromJson(
                            addresses,
                            AddressFields.MANUAL,
                            HostEndpoint.Kind.MANUAL));
            addEndpoint(
                    endpoints,
                    endpointFromJson(
                            addresses,
                            AddressFields.IPV6,
                            HostEndpoint.Kind.LOCAL_IPV6));
            return endpoints;
        }
        catch (JSONException | IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Stored host endpoints are invalid",
                    error);
        }
    }

    private static void addEndpoint(
            List<HostEndpoint> endpoints,
            HostEndpoint endpoint) {
        if (endpoint != null) {
            endpoints.add(endpoint);
        }
    }

    private static X509Certificate decodeCertificate(byte[] encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(encoded));
        }
        catch (CertificateException error) {
            // Keep the bytes in storage for recovery; only omit the unusable
            // runtime credential from this read.
            LimeLog.warning("Stored pinned host certificate is invalid");
            return null;
        }
    }

    private String resolveStoredHostId(HostId hostId) {
        try (Cursor cursor = queryComputers(
                "c." + COMPUTER_UUID_COLUMN_NAME +
                        "=? COLLATE NOCASE",
                new String[]{Objects.requireNonNull(
                        hostId,
                        "hostId").getValue()})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    private Cursor queryComputers(String selection, String[] arguments) {
        boolean hasCredentials = hasTable(CREDENTIAL_TABLE_NAME);
        boolean hasUserAlias = hasColumn(
                computerDb,
                COMPUTER_TABLE_NAME,
                USER_ALIAS_COLUMN_NAME);
        StringBuilder sql = new StringBuilder()
                .append("SELECT c.")
                .append(COMPUTER_UUID_COLUMN_NAME)
                .append(",c.")
                .append(COMPUTER_NAME_COLUMN_NAME)
                .append(",c.")
                .append(ADDRESSES_COLUMN_NAME)
                .append(",c.")
                .append(MAC_ADDRESS_COLUMN_NAME)
                .append(',');
        if (hasCredentials) {
            sql.append("COALESCE(k.")
                    .append(CREDENTIAL_CERT_COLUMN_NAME)
                    .append(",c.")
                    .append(LEGACY_SERVER_CERT_COLUMN_NAME)
                    .append(')');
        }
        else {
            sql.append("c.")
                    .append(LEGACY_SERVER_CERT_COLUMN_NAME);
        }
        sql.append(',');
        if (hasUserAlias) {
            sql.append("c.").append(USER_ALIAS_COLUMN_NAME);
        }
        else {
            sql.append("NULL");
        }
        sql.append(" FROM ")
                .append(COMPUTER_TABLE_NAME)
                .append(" c");
        if (hasCredentials) {
            sql.append(" LEFT JOIN ")
                    .append(CREDENTIAL_TABLE_NAME)
                    .append(" k ON c.")
                    .append(COMPUTER_UUID_COLUMN_NAME)
                    .append("=k.")
                    .append(CREDENTIAL_HOST_ID_COLUMN_NAME);
        }
        if (selection != null) {
            sql.append(" WHERE ").append(selection);
        }
        return computerDb.rawQuery(sql.toString(), arguments);
    }

    @Override
    public List<PersistedHost> getAllHosts() {
        try (Cursor cursor = queryComputers(null, null)) {
            List<PersistedHost> hosts = new ArrayList<>();
            while (cursor.moveToNext()) {
                hosts.add(getHostFromCursor(cursor));
            }
            return hosts;
        }
    }

    @Override
    public PersistedHost findHostByName(String name) {
        String advertisedName = Objects.requireNonNull(
                name,
                "advertisedName");
        try (Cursor cursor = queryComputers(
                "c." + COMPUTER_NAME_COLUMN_NAME + "=?",
                new String[]{advertisedName})) {
            return cursor.moveToFirst() ? getHostFromCursor(cursor) : null;
        }
    }

    @Override
    public PersistedHost findHost(HostId hostId) {
        try (Cursor cursor = queryComputers(
                "c." + COMPUTER_UUID_COLUMN_NAME +
                        "=? COLLATE NOCASE",
                new String[]{Objects.requireNonNull(
                        hostId,
                        "hostId").getValue()})) {
            return cursor.moveToFirst() ? getHostFromCursor(cursor) : null;
        }
    }
}
