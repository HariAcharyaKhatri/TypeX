package com.typex.database;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.sql.*;

public final class DatabaseManager {
    private static DatabaseManager instance;
    private Connection conn;
    private Connection proxyConn;
    private Path customDbPath = null;
    private final Object lock = new Object();

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void setDatabasePath(Path path) {
        synchronized (lock) {
            close();
            this.customDbPath = path;
        }
    }

    public void resetDatabasePath() {
        synchronized (lock) {
            close();
            this.customDbPath = null;
        }
    }

    public Path getDatabasePath() {
        synchronized (lock) {
            if (customDbPath != null) return customDbPath;
            return Paths.get(System.getProperty("user.home"), ".typex", "typex.db");
        }
    }

    public void initialize() throws SQLException {
        synchronized (lock) {
            if (conn != null) {
                try {
                    if (!conn.isClosed()) return;
                } catch (SQLException ignored) {}
            }
            try {
                Path db;
                if (customDbPath != null) {
                    if (customDbPath.getParent() != null) {
                        Files.createDirectories(customDbPath.getParent());
                    }
                    db = customDbPath;
                } else {
                    Path dir = Paths.get(System.getProperty("user.home"), ".typex");
                    Files.createDirectories(dir);
                    db = dir.resolve("typex.db");
                }
                conn = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath());
                try (Statement st = conn.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                    st.execute("PRAGMA journal_mode = WAL;");
                    st.execute("PRAGMA busy_timeout = 5000;");
                }
                proxyConn = createProxy(conn);
            } catch (Exception e) {
                throw new SQLException("Database initialization failed", e);
            }
        }
    }

    private Connection createProxy(Connection target) {
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    // Do not close the singleton underlying connection
                    return null;
                }
                if ("isClosed".equals(method.getName())) {
                    return target.isClosed();
                }
                try {
                    return method.invoke(target, args);
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    throw ite.getCause();
                }
            }
        );
    }

    public Connection getConnection() {
        synchronized (lock) {
            try {
                if (conn == null || conn.isClosed()) {
                    initialize();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get database connection", e);
            }
            return proxyConn != null ? proxyConn : conn;
        }
    }

    public Object getLock() {
        return lock;
    }

    public void executeTransaction(TransactionAction action) throws SQLException {
        synchronized (lock) {
            Connection c = getConnection();
            boolean autoCommit = c.getAutoCommit();
            if (!autoCommit) {
                // Nested transaction call: execute within the current active transaction boundary without committing early
                try {
                    action.execute(c);
                } catch (Exception e) {
                    if (e instanceof SQLException sqle) throw sqle;
                    throw new SQLException("Transaction action failed", e);
                }
                return;
            }
            try {
                c.setAutoCommit(false);
                action.execute(c);
                c.commit();
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException ignored) {}
                if (e instanceof SQLException sqle) throw sqle;
                throw new SQLException("Transaction failed", e);
            } finally {
                try { c.setAutoCommit(autoCommit); } catch (SQLException ignored) {}
            }
        }
    }

    public interface TransactionAction {
        void execute(Connection c) throws Exception;
    }

    public void close() {
        synchronized (lock) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {}
            conn = null;
            proxyConn = null;
        }
    }
}
