/**
 * The JDBC core: {@link com.uxplima.uxmlib.storage.sql.Database} is a HikariCP-pooled handle built by
 * {@link com.uxplima.uxmlib.storage.sql.DatabaseBuilder} (SQLite default, MySQL/MariaDB/PostgreSQL opt-in)
 * with {@code transaction} for a unit of work; {@link com.uxplima.uxmlib.storage.sql.Sql} and
 * {@link com.uxplima.uxmlib.storage.sql.TxSql} run parameterised queries/updates through a
 * {@link com.uxplima.uxmlib.storage.sql.RowMapper}, and {@link com.uxplima.uxmlib.storage.sql.SelectBuilder}
 * renders an injection-safe {@link com.uxplima.uxmlib.storage.sql.Query}.
 */
@NullMarked
package com.uxplima.uxmlib.storage.sql;

import org.jspecify.annotations.NullMarked;
