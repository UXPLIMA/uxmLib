package com.uxplima.uxmlib.storage.sync;

import java.util.Objects;

/**
 * One row a peer changed, as a {@link RowSyncPoller} surfaces it: the {@code key} (the primary-key value read
 * from the configured key column, used for the dirty check and to address the local cache), the {@code version}
 * the peer stamped (the value the poller's cursor advanced to), and the {@code value} the caller's
 * {@link com.uxplima.uxmlib.storage.sql.RowMapper RowMapper} mapped the row to.
 *
 * @param key the primary-key value of the changed row
 * @param version the monotonically increasing version the peer stamped on the row
 * @param value the mapped domain value to apply locally
 * @param <T> the mapped value type
 */
public record ChangedRow<T>(Object key, long version, T value) {

    public ChangedRow {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }
}
