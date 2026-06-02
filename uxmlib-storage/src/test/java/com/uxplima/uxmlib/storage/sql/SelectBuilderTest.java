package com.uxplima.uxmlib.storage.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Renders SQL and proves identifiers/operators are validated, not blindly inlined. */
class SelectBuilderTest {

    @Test
    void rendersAFullStatement() {
        Query q = SelectBuilder.from("players")
                .columns("name", "coins")
                .where("world", "world_nether")
                .where("coins", ">=", 100)
                .orderByDescending("coins")
                .limit(10)
                .build();
        assertThat(q.sql())
                .isEqualTo(
                        "SELECT name, coins FROM players WHERE world = ? AND coins >= ? ORDER BY coins DESC LIMIT 10");
        assertThat(q.parameters()).containsExactly("world_nether", 100);
    }

    @Test
    void selectsStarWhenNoColumns() {
        Query q = SelectBuilder.from("players").build();
        assertThat(q.sql()).isEqualTo("SELECT * FROM players");
    }

    @Test
    void allowsDottedIdentifiers() {
        Query q = SelectBuilder.from("schema.players").columns("players.name").build();
        assertThat(q.sql()).isEqualTo("SELECT players.name FROM schema.players");
    }

    @Test
    void rejectsAnInjectingTableName() {
        assertThatThrownBy(() -> SelectBuilder.from("players; DROP TABLE players"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnInjectingColumnName() {
        assertThatThrownBy(() -> SelectBuilder.from("players").columns("name) UNION SELECT password FROM admin --"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnInjectingOrderByColumn() {
        assertThatThrownBy(() -> SelectBuilder.from("players").orderBy("coins; DELETE FROM players"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUnknownOperator() {
        assertThatThrownBy(() -> SelectBuilder.from("players").where("coins", ">= 0 OR 1=1 --", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalisesAllowedOperators() {
        Query q = SelectBuilder.from("players").where("name", "like", "Steve%").build();
        assertThat(q.sql()).isEqualTo("SELECT * FROM players WHERE name LIKE ?");
    }

    @Test
    void rendersAnInClauseWithOnePlaceholderPerValue() {
        Query q = SelectBuilder.from("players")
                .whereIn("world", "world", "world_nether")
                .build();
        assertThat(q.sql()).isEqualTo("SELECT * FROM players WHERE world IN (?, ?)");
        assertThat(q.parameters()).containsExactly("world", "world_nether");
    }

    @Test
    void rejectsAnEmptyInClause() {
        assertThatThrownBy(() -> SelectBuilder.from("players").whereIn("world"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rendersAnOrGroupInParentheses() {
        Query q = SelectBuilder.from("players")
                .where("active", true)
                .whereAny(group -> group.eq("name", "Steve").eq("name", "Alex"))
                .build();
        assertThat(q.sql()).isEqualTo("SELECT * FROM players WHERE active = ? AND (name = ? OR name = ?)");
        assertThat(q.parameters()).containsExactly(true, "Steve", "Alex");
    }

    @Test
    void rejectsAnEmptyOrGroup() {
        assertThatThrownBy(() -> SelectBuilder.from("players").whereAny(group -> {}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rendersCaseInsensitiveLike() {
        Query q = SelectBuilder.from("players")
                .whereLikeIgnoreCase("name", "steve%")
                .build();
        assertThat(q.sql()).isEqualTo("SELECT * FROM players WHERE LOWER(name) LIKE LOWER(?)");
        assertThat(q.parameters()).containsExactly("steve%");
    }

    @Test
    void rendersLimitAndOffset() {
        Query q =
                SelectBuilder.from("players").orderBy("id").limit(10).offset(20).build();
        assertThat(q.sql()).isEqualTo("SELECT * FROM players ORDER BY id ASC LIMIT 10 OFFSET 20");
    }

    @Test
    void rejectsANegativeOffset() {
        assertThatThrownBy(() -> SelectBuilder.from("players").offset(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
