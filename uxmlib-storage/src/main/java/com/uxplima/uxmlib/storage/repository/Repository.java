package com.uxplima.uxmlib.storage.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.uxplima.uxmlib.storage.sql.RowMapper;
import com.uxplima.uxmlib.storage.sql.Sql;
import com.uxplima.uxmlib.storage.sql.StatementBinder;

/**
 * A CRUD-by-id base over a single table, so a plugin gets {@code findById}/{@code findAll}/{@code save}/
 * {@code deleteById}/{@code exists} without re-writing the SQL each time. Subclass it (or build one) by
 * supplying the table name, the id column, a {@link RowMapper} to read a row, and how to bind an entity's
 * columns for an upsert. Reads/writes block, so wrap them off-thread for hot paths.
 *
 * @param <I> the id type
 * @param <T> the entity type
 */
public abstract class Repository<I, T> {

    private final Sql sql;
    private final String table;
    private final String idColumn;
    private final List<String> columns;
    private final RowMapper<T> mapper;

    /**
     * @param columns every column written by {@link #bind}, in order, including the id column
     */
    protected Repository(Sql sql, String table, String idColumn, List<String> columns, RowMapper<T> mapper) {
        this.sql = Objects.requireNonNull(sql, "sql");
        this.table = Identifiers.require(table, "table");
        this.idColumn = Identifiers.require(idColumn, "idColumn");
        this.columns =
                columns.stream().map(c -> Identifiers.require(c, "column")).toList();
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /** Bind every column of {@code entity} onto {@code statement} in the order given to the constructor. */
    protected abstract void bind(PreparedStatement statement, T entity) throws SQLException;

    /** The entity with id {@code id}, or empty. */
    public Optional<T> findById(I id) {
        Objects.requireNonNull(id, "id");
        return sql.queryFirst(
                "SELECT * FROM " + table + " WHERE " + idColumn + " = ?", ps -> ps.setObject(1, id), mapper);
    }

    /** Every entity in the table. */
    public List<T> findAll() {
        return sql.query("SELECT * FROM " + table, StatementBinder.NONE, mapper);
    }

    /** Whether an entity with id {@code id} exists. */
    public boolean exists(I id) {
        Objects.requireNonNull(id, "id");
        return sql.queryFirst(
                        "SELECT 1 FROM " + table + " WHERE " + idColumn + " = ?",
                        ps -> ps.setObject(1, id),
                        row -> true)
                .isPresent();
    }

    /** Insert {@code entity}, or update the row with the same id (a cross-dialect upsert). */
    public void save(T entity) {
        Objects.requireNonNull(entity, "entity");
        sql.update(sql.dialect().upsert(table, idColumn, columns), ps -> bind(ps, entity));
    }

    /** Delete the entity with id {@code id}; returns whether a row was removed. */
    public boolean deleteById(I id) {
        Objects.requireNonNull(id, "id");
        return sql.update("DELETE FROM " + table + " WHERE " + idColumn + " = ?", ps -> ps.setObject(1, id)) > 0;
    }
}
