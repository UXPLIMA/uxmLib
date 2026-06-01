/**
 * Versioned schema migration: {@link com.uxplima.uxmlib.storage.migration.MigrationRunner} applies each
 * {@link com.uxplima.uxmlib.storage.migration.Migration} exactly once in version order, recording applied
 * versions so re-running is idempotent.
 */
@NullMarked
package com.uxplima.uxmlib.storage.migration;

import org.jspecify.annotations.NullMarked;
