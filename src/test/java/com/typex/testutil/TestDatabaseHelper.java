package com.typex.testutil;

import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Manages isolated, temporary SQLite databases for automated unit and integration tests.
 * Guarantees tests never read or write ~/.typex/typex.db.
 */
public final class TestDatabaseHelper {

    private static final java.util.concurrent.atomic.AtomicLong USER_SEQ = new java.util.concurrent.atomic.AtomicLong(1);

    private TestDatabaseHelper() {}

    /**
     * Generates a guaranteed unique username for test users to prevent SQLite unique constraint violations.
     */
    public static String uniqueUsername(String prefix) {
        return (prefix != null ? prefix : "test_user") + "_" + System.nanoTime() + "_" + USER_SEQ.incrementAndGet();
    }

    /**
     * Creates and initializes a fresh isolated temporary SQLite database.
     */
    public static Path initIsolatedDatabase() throws SQLException {
        try {
            Path tempDb = Files.createTempFile("typex_isolated_test_", ".db");
            tempDb.toFile().deleteOnExit();
            DatabaseManager.getInstance().setDatabasePath(tempDb);
            DatabaseInitializer.initialize();
            return tempDb;
        } catch (IOException e) {
            throw new SQLException("Failed to create temporary isolated test database", e);
        }
    }

    /**
     * Resets DatabaseManager back to default configuration and cleans up temporary DB files.
     */
    public static void teardownIsolatedDatabase(Path tempDb) {
        DatabaseManager.getInstance().resetDatabasePath();
        if (tempDb != null) {
            try {
                Files.deleteIfExists(tempDb);
                Files.deleteIfExists(Paths.get(tempDb.toString() + "-wal"));
                Files.deleteIfExists(Paths.get(tempDb.toString() + "-shm"));
            } catch (IOException ignored) {}
        }
    }
}
