package com.uxplima.uxmlib.storage.sql;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of a skip-bad-row load (see {@link Sql#queryResilient}): the {@code rows} a {@link RowMapper}
 * mapped successfully, plus the {@code skipped} count of rows it rejected (logged and dropped rather than
 * aborting the whole load). A {@code skipped} of {@code 0} means every row mapped cleanly.
 *
 * @param rows the successfully mapped rows, in result-set order
 * @param skipped how many rows the mapper rejected and were dropped
 */
public record LoadResult<T>(List<T> rows, int skipped) {

    public LoadResult {
        Objects.requireNonNull(rows, "rows");
        if (skipped < 0) {
            throw new IllegalArgumentException("skipped must be >= 0 (got " + skipped + ")");
        }
        rows = List.copyOf(rows);
    }

    /** Whether at least one row was rejected during the load. */
    public boolean hadSkips() {
        return skipped > 0;
    }
}
