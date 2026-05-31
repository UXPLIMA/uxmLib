package com.uxplima.uxmlib.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the JDBC helpers against a real in-memory SQLite database — no Paper, no MockBukkit, just
 * a driver on the classpath. The same connection-backed schema is created per test.
 */
class SqlTest {

    private Database database;
    private Sql sql;

    @BeforeEach
    void setUp() {
        // A shared in-memory database name keeps the schema alive across pooled connections for the test.
        database = Database.builder()
                .jdbcUrl("jdbc:sqlite:file:uxmlibtest?mode=memory&cache=shared")
                .maxPoolSize(1)
                .build();
        sql = new Sql(database);
        sql.execute("CREATE TABLE players (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void insertsAndQueriesRows() {
        int inserted = sql.update("INSERT INTO players (id, name) VALUES (?, ?)", ps -> {
            ps.setInt(1, 1);
            ps.setString(2, "Steve");
        });
        assertThat(inserted).isEqualTo(1);

        List<String> names =
                sql.query("SELECT name FROM players ORDER BY id", StatementBinder.NONE, row -> row.getString("name"));
        assertThat(names).containsExactly("Steve");
    }

    @Test
    void queryFirstReturnsEmptyWhenNoRows() {
        Optional<String> found = sql.queryFirst(
                "SELECT name FROM players WHERE id = ?", ps -> ps.setInt(1, 99), row -> row.getString(1));
        assertThat(found).isEmpty();
    }

    @Test
    void queryFirstReturnsTheFirstRow() {
        sql.update("INSERT INTO players (id, name) VALUES (?, ?)", ps -> {
            ps.setInt(1, 7);
            ps.setString(2, "Alex");
        });
        Optional<String> found =
                sql.queryFirst("SELECT name FROM players WHERE id = ?", ps -> ps.setInt(1, 7), row -> row.getString(1));
        assertThat(found).contains("Alex");
    }

    @Test
    void wrapsSqlErrorsInStorageException() {
        assertThatThrownBy(() -> sql.execute("SELECT * FROM table_that_does_not_exist"))
                .isInstanceOf(StorageException.class);
    }
}
