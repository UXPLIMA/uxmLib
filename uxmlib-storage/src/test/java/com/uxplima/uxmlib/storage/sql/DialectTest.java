package com.uxplima.uxmlib.storage.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class DialectTest {

    private static final List<String> COLUMNS = List.of("uuid", "name", "coins");

    @Test
    void infersDialectFromJdbcUrl() {
        assertThat(Dialect.fromJdbcUrl("jdbc:sqlite:data.db")).isEqualTo(Dialect.SQLITE);
        assertThat(Dialect.fromJdbcUrl("jdbc:mysql://host/db")).isEqualTo(Dialect.MYSQL);
        assertThat(Dialect.fromJdbcUrl("jdbc:mariadb://host/db")).isEqualTo(Dialect.MYSQL);
        assertThat(Dialect.fromJdbcUrl("jdbc:postgresql://host/db")).isEqualTo(Dialect.POSTGRES);
        assertThat(Dialect.fromJdbcUrl("jdbc:h2:mem:test")).isEqualTo(Dialect.GENERIC);
    }

    @Test
    void sqliteUsesOnConflictUpdate() {
        assertThat(Dialect.SQLITE.upsert("players", "uuid", COLUMNS))
                .isEqualTo("INSERT INTO players (uuid, name, coins) VALUES (?, ?, ?) "
                        + "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, coins = excluded.coins");
    }

    @Test
    void postgresMatchesTheOnConflictShape() {
        assertThat(Dialect.POSTGRES.upsert("players", "uuid", COLUMNS))
                .startsWith("INSERT INTO players (uuid, name, coins) VALUES (?, ?, ?)")
                .contains("ON CONFLICT(uuid) DO UPDATE SET name = excluded.name");
    }

    @Test
    void mysqlUsesOnDuplicateKeyUpdate() {
        assertThat(Dialect.MYSQL.upsert("players", "uuid", COLUMNS))
                .isEqualTo("INSERT INTO players (uuid, name, coins) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE name = VALUES(name), coins = VALUES(coins)");
    }

    @Test
    void anIdOnlyTableDoesNotTryToUpdate() {
        assertThat(Dialect.SQLITE.upsert("t", "id", List.of("id"))).endsWith("ON CONFLICT(id) DO NOTHING");
        assertThat(Dialect.MYSQL.upsert("t", "id", List.of("id"))).endsWith("ON DUPLICATE KEY UPDATE id = id");
    }

    @Test
    void genericDialectHasNoPortableUpsert() {
        assertThatThrownBy(() -> Dialect.GENERIC.upsert("t", "id", COLUMNS))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
