package com.uxplima.uxmlib.storage.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Binds parameters onto a {@link PreparedStatement} before it runs. Use the 1-based positional setters
 * ({@code ps.setString(1, ...)}). {@link StatementBinder#NONE} binds nothing, for parameterless SQL.
 */
@FunctionalInterface
public interface StatementBinder {

    /** A binder that sets no parameters. */
    StatementBinder NONE = statement -> {};

    /** Set the statement's parameters. */
    void bind(PreparedStatement statement) throws SQLException;
}
