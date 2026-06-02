package com.uxplima.uxmlib.storage.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.regex.Pattern;

import com.uxplima.uxmlib.storage.StorageException;
import com.uxplima.uxmlib.storage.sql.Database;
import com.uxplima.uxmlib.storage.sql.Dialect;

/**
 * Admin-style column-maintenance and table-reset DDL: the primitives a "rename a stat" or "reset the season"
 * operator command needs. {@link #dropColumn} and {@link #renameColumn} are dialect-aware
 * {@code ALTER TABLE}s; {@link #resetTable} is a {@code DELETE} that empties a table while keeping its schema.
 *
 * <p>This is the destructive sibling of {@link SchemaIntrospector#ensureColumn} (additive). Combine the two:
 * probe with the introspector, then drop/rename here.
 *
 * <p>DDL cannot use bound parameters, so every table and column name is validated against a strict identifier
 * allowlist before it reaches a statement — anything that could carry an injection is rejected first. Native
 * {@code RENAME COLUMN} (SQLite 3.25+) and {@code DROP COLUMN} (SQLite 3.35+) are used directly; our bundled
 * sqlite-jdbc is 3.49, so the table-rebuild dance older SQLite needed is not required.
 */
public final class SchemaOps {

    // A bare identifier or one dotted qualifier — the same shape SchemaIntrospector/SelectBuilder accept.
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private final Database database;

    public SchemaOps(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * Drop {@code column} from {@code table}. Uses native {@code ALTER TABLE ... DROP COLUMN}, supported across
     * SQLite 3.35+, MySQL/MariaDB, Postgres and H2.
     *
     * @throws IllegalArgumentException if a name is not a simple SQL identifier
     * @throws StorageException if the statement fails
     */
    public void dropColumn(String table, String column) {
        String tableName = identifier(table, "table");
        String columnName = identifier(column, "column");
        execute(
                "ALTER TABLE " + tableName + " DROP COLUMN " + columnName,
                "could not drop column " + tableName + "." + columnName);
    }

    /**
     * Rename {@code from} to {@code to} on {@code table}, preserving the column's data. Uses native
     * {@code ALTER TABLE ... RENAME COLUMN ... TO ...}, supported across SQLite 3.25+, MySQL 8/MariaDB 10.5+,
     * Postgres and H2.
     *
     * @throws IllegalArgumentException if a name is not a simple SQL identifier
     * @throws StorageException if the statement fails
     */
    public void renameColumn(String table, String from, String to) {
        String tableName = identifier(table, "table");
        String fromName = identifier(from, "from");
        String toName = identifier(to, "to");
        execute(
                "ALTER TABLE " + tableName + " RENAME COLUMN " + fromName + " TO " + toName,
                "could not rename column " + tableName + "." + fromName + " to " + toName);
    }

    /**
     * Empty {@code table} of every row with a {@code DELETE}, keeping its schema, indexes and triggers. The
     * admin "clear/reset" primitive (a season reset). Returns the number of rows removed.
     *
     * <p>Plain {@code DELETE} (not {@code TRUNCATE}) is used deliberately: it is transactional, returns an
     * affected-row count, and behaves identically across every backend — {@code TRUNCATE}'s semantics and
     * permissions vary by dialect.
     *
     * @throws IllegalArgumentException if {@code table} is not a simple SQL identifier
     * @throws StorageException if the statement fails
     */
    public int resetTable(String table) {
        String tableName = identifier(table, "table");
        try (Connection conn = database.connection();
                Statement statement = conn.createStatement()) {
            return statement.executeUpdate("DELETE FROM " + tableName);
        } catch (SQLException failure) {
            throw new StorageException("could not reset table " + tableName, failure);
        }
    }

    private void execute(String sql, String onFailure) {
        try (Connection conn = database.connection();
                Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException failure) {
            throw new StorageException(onFailure, failure);
        }
    }

    private static String identifier(String value, String what) {
        Objects.requireNonNull(value, what);
        if (!IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(what + " must be a simple SQL identifier (got '" + value + "')");
        }
        return value;
    }

    /** The dialect this helper targets, for callers branching on backend-specific schema. */
    public Dialect dialect() {
        return database.dialect();
    }
}
