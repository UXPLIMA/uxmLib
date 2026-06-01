package com.uxplima.uxmlib.storage.sql;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The SQL backend a {@link Database} speaks, inferred from its JDBC URL. Used to emit backend-correct SQL
 * where the dialects diverge — most importantly the insert-or-update (upsert) statement, which
 * SQLite/Postgres spell with {@code ON CONFLICT} and MySQL/MariaDB with {@code ON DUPLICATE KEY}.
 */
public enum Dialect {
    SQLITE,
    MYSQL,
    POSTGRES,
    GENERIC;

    /** The dialect for {@code jdbcUrl} (read from its {@code jdbc:<backend>:} prefix), {@link #GENERIC} if unknown. */
    public static Dialect fromJdbcUrl(String jdbcUrl) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        String url = jdbcUrl.toLowerCase(Locale.ROOT);
        if (url.startsWith("jdbc:sqlite:")) {
            return SQLITE;
        }
        if (url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:mariadb:")) {
            return MYSQL;
        }
        if (url.startsWith("jdbc:postgresql:") || url.startsWith("jdbc:postgres:")) {
            return POSTGRES;
        }
        return GENERIC;
    }

    /**
     * Build an upsert: insert a row, or update the non-id columns of the row that already has the same
     * {@code idColumn}. {@code columns} is every bound column in order, including {@code idColumn}.
     *
     * @throws UnsupportedOperationException if this dialect has no portable upsert (override {@code save})
     */
    public String upsert(String table, String idColumn, List<String> columns) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(idColumn, "idColumn");
        Objects.requireNonNull(columns, "columns");
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String insert = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        List<String> updates = columns.stream().filter(c -> !c.equals(idColumn)).toList();
        return switch (this) {
            case SQLITE, POSTGRES -> insert + onConflict(idColumn, updates);
            case MYSQL -> insert + onDuplicateKey(updates, idColumn);
            case GENERIC -> throw new UnsupportedOperationException(
                    "no portable upsert for this JDBC backend; override Repository.save()");
        };
    }

    private static String onConflict(String idColumn, List<String> updates) {
        if (updates.isEmpty()) {
            return " ON CONFLICT(" + idColumn + ") DO NOTHING";
        }
        String set = updates.stream().map(c -> c + " = excluded." + c).collect(Collectors.joining(", "));
        return " ON CONFLICT(" + idColumn + ") DO UPDATE SET " + set;
    }

    private static String onDuplicateKey(List<String> updates, String idColumn) {
        if (updates.isEmpty()) {
            return " ON DUPLICATE KEY UPDATE " + idColumn + " = " + idColumn;
        }
        String set = updates.stream().map(c -> c + " = VALUES(" + c + ")").collect(Collectors.joining(", "));
        return " ON DUPLICATE KEY UPDATE " + set;
    }
}
