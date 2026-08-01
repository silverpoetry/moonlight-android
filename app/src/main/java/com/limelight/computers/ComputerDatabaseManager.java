package com.limelight.computers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.limelight.LimeLog;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Persistent host repository and the sole owner of pinned host certificates. */
public final class ComputerDatabaseManager {
    public static final String COMPUTER_DB_NAME = "computers4.db";

    private static final int SCHEMA_VERSION = 5;
    private static final String COMPUTER_TABLE_NAME = "Computers";
    private static final String COMPUTER_UUID_COLUMN_NAME = "UUID";
    private static final String COMPUTER_NAME_COLUMN_NAME = "ComputerName";
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

    private final SQLiteDatabase computerDb;

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
        SQLiteDatabaseFileHeader.requireValidIfPopulated(
                context.getDatabasePath(checkedDatabaseName));
        computerDb = context.openOrCreateDatabase(
                checkedDatabaseName,
                Context.MODE_PRIVATE,
                null);
        initializeDb(context, migrateLegacyDatabases);
    }

    /** Opens an exported database without mutating its schema. */
    public ComputerDatabaseManager(Context context, File file) {
        Objects.requireNonNull(context, "context");
        computerDb = SQLiteDatabase.openDatabase(
                Objects.requireNonNull(file, "file").getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY);
        requireComputerTable();
    }

    public void close() {
        computerDb.close();
    }

    private void initializeDb(
            Context context,
            boolean migrateLegacyDatabases) {
        computerDb.beginTransaction();
        try {
            computerDb.execSQL(String.format(
                    (Locale) null,
                    "CREATE TABLE IF NOT EXISTS %s(" +
                            "%s TEXT PRIMARY KEY, " +
                            "%s TEXT NOT NULL, " +
                            "%s TEXT NOT NULL, " +
                            "%s TEXT, " +
                            "%s BLOB)",
                    COMPUTER_TABLE_NAME,
                    COMPUTER_UUID_COLUMN_NAME,
                    COMPUTER_NAME_COLUMN_NAME,
                    ADDRESSES_COLUMN_NAME,
                    MAC_ADDRESS_COLUMN_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME));
            computerDb.execSQL(String.format(
                    (Locale) null,
                    "CREATE TABLE IF NOT EXISTS %s(" +
                            "%s TEXT PRIMARY KEY, " +
                            "%s BLOB NOT NULL)",
                    CREDENTIAL_TABLE_NAME,
                    CREDENTIAL_HOST_ID_COLUMN_NAME,
                    CREDENTIAL_CERT_COLUMN_NAME));

            // Upgrade computers4.db in place. Moving then clearing the legacy
            // column makes HostCredentials the only credential owner.
            computerDb.execSQL(String.format(
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
            computerDb.execSQL(String.format(
                    (Locale) null,
                    "UPDATE %s SET %s=NULL WHERE %s IS NOT NULL",
                    COMPUTER_TABLE_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME,
                    LEGACY_SERVER_CERT_COLUMN_NAME));
            computerDb.setVersion(SCHEMA_VERSION);
            computerDb.setTransactionSuccessful();
        }
        finally {
            computerDb.endTransaction();
        }

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

    private void requireComputerTable() {
        if (!hasTable(COMPUTER_TABLE_NAME)) {
            throw new IllegalArgumentException("Not a host database");
        }
    }

    private boolean hasTable(String tableName) {
        try (Cursor cursor = computerDb.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName})) {
            return cursor.moveToFirst();
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
        ContentValues metadata = createComputerMetadata(details);
        computerDb.insertWithOnConflict(
                COMPUTER_TABLE_NAME,
                null,
                metadata,
                SQLiteDatabase.CONFLICT_IGNORE);

        if (details.serverCert != null) {
            ContentValues credential = createCredential(
                    details.uuid,
                    details.serverCert);
            computerDb.insertWithOnConflict(
                    CREDENTIAL_TABLE_NAME,
                    null,
                    credential,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public void deleteComputer(ComputerDetails details) {
        String hostId = requireHostId(details);
        computerDb.beginTransaction();
        try {
            computerDb.delete(
                    CREDENTIAL_TABLE_NAME,
                    CREDENTIAL_HOST_ID_COLUMN_NAME + "=?",
                    new String[]{hostId});
            computerDb.delete(
                    COMPUTER_TABLE_NAME,
                    COMPUTER_UUID_COLUMN_NAME + "=?",
                    new String[]{hostId});
            computerDb.setTransactionSuccessful();
        }
        finally {
            computerDb.endTransaction();
        }
    }

    public boolean updateComputerMetadata(ComputerDetails details) {
        return writeComputerMetadata(details);
    }

    /** Atomically imports metadata and an optional pinned certificate. */
    public boolean importComputer(ComputerDetails details) {
        computerDb.beginTransaction();
        try {
            boolean updated = writeComputerMetadata(details);
            if (details.serverCert != null) {
                writePinnedCertificate(details.uuid, details.serverCert);
            }
            computerDb.setTransactionSuccessful();
            return updated;
        }
        finally {
            computerDb.endTransaction();
        }
    }

    /** Explicit credential mutation used only after successful pairing/import. */
    public void updatePinnedCertificate(
            String hostId,
            X509Certificate certificate) {
        writePinnedCertificate(
                requireNonEmpty(hostId, "hostId"),
                Objects.requireNonNull(certificate, "certificate"));
    }

    private boolean writeComputerMetadata(ComputerDetails details) {
        return computerDb.insertWithOnConflict(
                COMPUTER_TABLE_NAME,
                null,
                createComputerMetadata(details),
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    private static ContentValues createComputerMetadata(
            ComputerDetails details) {
        ContentValues values = new ContentValues();
        values.put(COMPUTER_UUID_COLUMN_NAME, requireHostId(details));
        values.put(
                COMPUTER_NAME_COLUMN_NAME,
                requireNonEmpty(details.name, "computer name"));
        values.put(ADDRESSES_COLUMN_NAME, encodeAddresses(details));
        values.put(MAC_ADDRESS_COLUMN_NAME, details.macAddress);
        values.putNull(LEGACY_SERVER_CERT_COLUMN_NAME);
        return values;
    }

    private void writePinnedCertificate(
            String hostId,
            X509Certificate certificate) {
        ContentValues values = createCredential(hostId, certificate);
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
            String hostId,
            X509Certificate certificate) {
        ContentValues values = new ContentValues();
        values.put(
                CREDENTIAL_HOST_ID_COLUMN_NAME,
                requireNonEmpty(hostId, "hostId"));
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

    private static String requireHostId(ComputerDetails details) {
        return requireNonEmpty(
                Objects.requireNonNull(details, "details").uuid,
                "host ID");
    }

    private static String requireNonEmpty(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }

    private static String encodeAddresses(ComputerDetails details) {
        try {
            JSONObject addresses = new JSONObject();
            addresses.put(AddressFields.LOCAL, tupleToJson(details.localAddress));
            addresses.put(AddressFields.REMOTE, tupleToJson(details.remoteAddress));
            addresses.put(AddressFields.MANUAL, tupleToJson(details.manualAddress));
            addresses.put(AddressFields.IPV6, tupleToJson(details.ipv6Address));
            return addresses.toString();
        }
        catch (JSONException error) {
            throw new IllegalArgumentException(
                    "Unable to encode host endpoints",
                    error);
        }
    }

    public static JSONObject tupleToJson(
            ComputerDetails.AddressTuple tuple) throws JSONException {
        if (tuple == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        json.put(AddressFields.ADDRESS, tuple.address);
        json.put(AddressFields.PORT, tuple.port);
        return json;
    }

    public static ComputerDetails.AddressTuple tupleFromJson(
            JSONObject json,
            String name) throws JSONException {
        if (!json.has(name) || json.isNull(name)) {
            return null;
        }
        JSONObject address = json.getJSONObject(name);
        return new ComputerDetails.AddressTuple(
                address.getString(AddressFields.ADDRESS),
                address.getInt(AddressFields.PORT));
    }

    private ComputerDetails getComputerFromCursor(Cursor cursor) {
        ComputerDetails details = new ComputerDetails();
        details.uuid = cursor.getString(0);
        details.name = cursor.getString(1);
        try {
            JSONObject addresses = new JSONObject(cursor.getString(2));
            details.localAddress = tupleFromJson(addresses, AddressFields.LOCAL);
            details.remoteAddress = tupleFromJson(addresses, AddressFields.REMOTE);
            details.manualAddress = tupleFromJson(addresses, AddressFields.MANUAL);
            details.ipv6Address = tupleFromJson(addresses, AddressFields.IPV6);
        }
        catch (JSONException error) {
            throw new IllegalStateException(
                    "Stored host endpoints are invalid",
                    error);
        }
        details.externalPort = details.remoteAddress == null ?
                NvHTTP.DEFAULT_HTTP_PORT : details.remoteAddress.port;
        details.macAddress = cursor.getString(3);
        details.serverCert = decodeCertificate(cursor.getBlob(4));
        details.state = ComputerDetails.State.UNKNOWN;
        return details;
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

    private Cursor queryComputers(String selection, String[] arguments) {
        if (hasTable(CREDENTIAL_TABLE_NAME)) {
            StringBuilder sql = new StringBuilder()
                    .append("SELECT c.")
                    .append(COMPUTER_UUID_COLUMN_NAME)
                    .append(",c.")
                    .append(COMPUTER_NAME_COLUMN_NAME)
                    .append(",c.")
                    .append(ADDRESSES_COLUMN_NAME)
                    .append(",c.")
                    .append(MAC_ADDRESS_COLUMN_NAME)
                    .append(",COALESCE(k.")
                    .append(CREDENTIAL_CERT_COLUMN_NAME)
                    .append(",c.")
                    .append(LEGACY_SERVER_CERT_COLUMN_NAME)
                    .append(") FROM ")
                    .append(COMPUTER_TABLE_NAME)
                    .append(" c LEFT JOIN ")
                    .append(CREDENTIAL_TABLE_NAME)
                    .append(" k ON c.")
                    .append(COMPUTER_UUID_COLUMN_NAME)
                    .append("=k.")
                    .append(CREDENTIAL_HOST_ID_COLUMN_NAME);
            if (selection != null) {
                sql.append(" WHERE ").append(selection);
            }
            return computerDb.rawQuery(sql.toString(), arguments);
        }
        return computerDb.query(
                COMPUTER_TABLE_NAME,
                new String[]{
                        COMPUTER_UUID_COLUMN_NAME,
                        COMPUTER_NAME_COLUMN_NAME,
                        ADDRESSES_COLUMN_NAME,
                        MAC_ADDRESS_COLUMN_NAME,
                        LEGACY_SERVER_CERT_COLUMN_NAME},
                selection == null ? null : selection.replace("c.", ""),
                arguments,
                null,
                null,
                null);
    }

    public List<ComputerDetails> getAllComputers() {
        try (Cursor cursor = queryComputers(null, null)) {
            List<ComputerDetails> computers = new LinkedList<>();
            while (cursor.moveToNext()) {
                computers.add(getComputerFromCursor(cursor));
            }
            return computers;
        }
    }

    public ComputerDetails getComputerByName(String name) {
        try (Cursor cursor = queryComputers(
                "c." + COMPUTER_NAME_COLUMN_NAME + "=?",
                new String[]{name})) {
            return cursor.moveToFirst() ? getComputerFromCursor(cursor) : null;
        }
    }

    public ComputerDetails getComputerByUUID(String uuid) {
        try (Cursor cursor = queryComputers(
                "c." + COMPUTER_UUID_COLUMN_NAME + "=?",
                new String[]{uuid})) {
            return cursor.moveToFirst() ? getComputerFromCursor(cursor) : null;
        }
    }
}
