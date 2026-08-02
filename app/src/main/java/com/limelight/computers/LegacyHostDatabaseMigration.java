package com.limelight.computers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.limelight.LimeLog;
import com.limelight.computers.model.PersistedHost;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A validated, read-only snapshot of one legacy host database.
 *
 * <p>The source remains untouched until its contents have been committed to
 * the current repository. Missing and unreadable sources never reach SQLite,
 * which avoids framework exception spam and preserves damaged data for
 * recovery.</p>
 */
final class LegacyHostDatabaseMigration {
    interface Reader {
        List<PersistedHost> read(SQLiteDatabase database);
    }

    private final String databaseName;
    private final List<PersistedHost> hosts;
    private final boolean ready;

    private LegacyHostDatabaseMigration(
            String databaseName,
            List<PersistedHost> hosts,
            boolean ready) {
        this.databaseName = databaseName;
        this.hosts = hosts;
        this.ready = ready;
    }

    static LegacyHostDatabaseMigration read(
            Context context,
            String databaseName,
            Reader reader) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(databaseName, "databaseName");
        Objects.requireNonNull(reader, "reader");

        File databaseFile = context.getDatabasePath(databaseName);
        if (!databaseFile.exists()) {
            return unavailable(databaseName);
        }
        if (!SQLiteDatabaseFileHeader.isValid(databaseFile)) {
            LimeLog.warning(
                    "Legacy host database is invalid; preserving it for recovery");
            return unavailable(databaseName);
        }

        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                databaseFile.getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY)) {
            List<PersistedHost> records = Objects.requireNonNull(
                    reader.read(database),
                    "legacy database reader result");
            return new LegacyHostDatabaseMigration(
                    databaseName,
                    Collections.unmodifiableList(new ArrayList<>(records)),
                    true);
        }
        catch (RuntimeException error) {
            LimeLog.warning(
                    "Legacy host database cannot be read; preserving it for recovery");
            return unavailable(databaseName);
        }
    }

    private static LegacyHostDatabaseMigration unavailable(
            String databaseName) {
        return new LegacyHostDatabaseMigration(
                databaseName,
                Collections.emptyList(),
                false);
    }

    boolean isReady() {
        return ready;
    }

    List<PersistedHost> getHosts() {
        return hosts;
    }

    void retire(Context context) {
        if (!ready) {
            throw new IllegalStateException(
                    "Cannot retire an unreadable legacy database");
        }
        if (!context.deleteDatabase(databaseName) &&
                context.getDatabasePath(databaseName).exists()) {
            LimeLog.warning(
                    "Migrated legacy host database could not be retired");
        }
    }
}
