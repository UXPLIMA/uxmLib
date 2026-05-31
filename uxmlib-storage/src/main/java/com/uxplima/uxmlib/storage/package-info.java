/**
 * Plain-JDBC persistence plumbing — no jOOQ, no Flyway, no Paper dependency. {@link
 * com.uxplima.uxmlib.storage.Database} is a HikariCP-pooled handle built by {@link
 * com.uxplima.uxmlib.storage.DatabaseBuilder}, with SQLite as the zero-config default and
 * MySQL/MariaDB/PostgreSQL as opt-in network backends. {@link com.uxplima.uxmlib.storage.Sql} runs
 * parameterised queries and updates through a {@link com.uxplima.uxmlib.storage.RowMapper}, and
 * {@link com.uxplima.uxmlib.storage.Cache} is a thin Caffeine wrapper for read-through caching.
 */
@NullMarked
package com.uxplima.uxmlib.storage;

import org.jspecify.annotations.NullMarked;
