package com.uxplima.uxmlib.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Small helpers for prepared-statement work over a {@link Database}, so callers do not re-derive the
 * borrow-connection / prepare / bind / iterate / close-everything dance. Every method uses
 * try-with-resources and prepared statements — never string concatenation — and borrows a fresh pooled
 * connection per call. Checked {@link SQLException}s are wrapped in {@link StorageException}.
 */
public final class Sql {

    private final Database database;

    public Sql(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Run a query and map every row, in result-set order. */
    public <T> List<T> query(String sql, StatementBinder binder, RowMapper<T> mapper) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(binder, "binder");
        Objects.requireNonNull(mapper, "mapper");
        try (Connection conn = database.connection();
                PreparedStatement statement = conn.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rows.next()) {
                    results.add(mapper.map(rows));
                }
                return List.copyOf(results);
            }
        } catch (SQLException failure) {
            throw new StorageException("query failed: " + sql, failure);
        }
    }

    /** Run a query and map the first row, if any. */
    public <T> Optional<T> queryFirst(String sql, StatementBinder binder, RowMapper<T> mapper) {
        List<T> results = query(sql, binder, mapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /** Run an INSERT/UPDATE/DELETE (or DDL) and return the affected row count. */
    public int update(String sql, StatementBinder binder) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(binder, "binder");
        try (Connection conn = database.connection();
                PreparedStatement statement = conn.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException failure) {
            throw new StorageException("update failed: " + sql, failure);
        }
    }

    /** Run a parameterless statement (typically DDL). */
    public void execute(String sql) {
        update(sql, StatementBinder.NONE);
    }
}
