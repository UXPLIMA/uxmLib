package com.uxplima.uxmlib.storage.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Exercises the {@link Result} envelope: a non-throwing carrier for an operation that either yields a value
 * or a failure message, so callers can branch without a try/catch.
 */
class ResultTest {

    @Test
    void okCarriesItsValue() {
        Result<String> result = Result.ok("done");

        assertThat(result.isOk()).isTrue();
        assertThat(result.isError()).isFalse();
        assertThat(result.value()).isEqualTo("done");
        assertThat(result.orElse("fallback")).isEqualTo("done");
    }

    @Test
    void errorCarriesItsMessage() {
        Result<String> result = Result.error("table is locked");

        assertThat(result.isError()).isTrue();
        assertThat(result.isOk()).isFalse();
        assertThat(result.error()).isEqualTo("table is locked");
        assertThat(result.orElse("fallback")).isEqualTo("fallback");
    }

    @Test
    void readingTheValueOfAnErrorThrows() {
        Result<String> result = Result.error("nope");

        assertThatThrownBy(result::value).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readingTheErrorOfAnOkThrows() {
        Result<String> result = Result.ok("yes");

        assertThatThrownBy(result::error).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapTransformsTheValueOfAnOk() {
        Result<Integer> mapped = Result.ok("42").map(Integer::parseInt);

        assertThat(mapped.isOk()).isTrue();
        assertThat(mapped.value()).isEqualTo(42);
    }

    @Test
    void mapLeavesAnErrorUntouched() {
        Result<String> source = Result.error("boom");

        Result<Integer> mapped = source.map(Integer::parseInt);

        assertThat(mapped.isError()).isTrue();
        assertThat(mapped.error()).isEqualTo("boom");
    }
}
