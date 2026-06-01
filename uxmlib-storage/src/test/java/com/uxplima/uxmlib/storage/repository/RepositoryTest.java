package com.uxplima.uxmlib.storage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.uxplima.uxmlib.storage.sql.Database;
import com.uxplima.uxmlib.storage.sql.Sql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Covers the Repository CRUD base against in-memory SQLite. */
class RepositoryTest {

    record Player(String uuid, String name, int coins) {}

    /** A concrete repository over a players table. */
    static final class Players extends Repository<String, Player> {
        Players(Sql sql) {
            super(
                    sql,
                    "players",
                    "uuid",
                    List.of("uuid", "name", "coins"),
                    row -> new Player(row.getString("uuid"), row.getString("name"), row.getInt("coins")));
        }

        @Override
        protected void bind(PreparedStatement statement, Player entity) throws SQLException {
            statement.setString(1, entity.uuid());
            statement.setString(2, entity.name());
            statement.setInt(3, entity.coins());
        }
    }

    private Database database;
    private Players players;

    @BeforeEach
    void setUp() {
        database = Database.builder()
                .jdbcUrl("jdbc:sqlite:file:uxmlibrepo?mode=memory&cache=shared")
                .maxPoolSize(1)
                .build();
        Sql sql = new Sql(database);
        sql.execute("CREATE TABLE players (uuid TEXT PRIMARY KEY, name TEXT NOT NULL, coins INTEGER NOT NULL)");
        players = new Players(sql);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void savesAndFindsById() {
        players.save(new Player("u1", "Steve", 50));
        assertThat(players.findById("u1")).get().extracting(Player::name).isEqualTo("Steve");
        assertThat(players.findById("missing")).isEmpty();
    }

    @Test
    void saveUpsertsOnTheSameId() {
        players.save(new Player("u1", "Steve", 50));
        players.save(new Player("u1", "Steve", 999)); // same id -> replace, not a duplicate
        assertThat(players.findAll()).hasSize(1);
        assertThat(players.findById("u1")).get().extracting(Player::coins).isEqualTo(999);
    }

    @Test
    void existsAndDelete() {
        players.save(new Player("u1", "Steve", 50));
        assertThat(players.exists("u1")).isTrue();
        assertThat(players.deleteById("u1")).isTrue();
        assertThat(players.exists("u1")).isFalse();
        assertThat(players.deleteById("u1")).isFalse(); // already gone
    }

    @Test
    void findAllReturnsEverything() {
        players.save(new Player("u1", "Steve", 1));
        players.save(new Player("u2", "Alex", 2));
        assertThat(players.findAll()).hasSize(2);
    }
}
