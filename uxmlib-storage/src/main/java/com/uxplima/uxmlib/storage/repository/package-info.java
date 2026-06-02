/**
 * Higher-level persistence over the JDBC core: {@link com.uxplima.uxmlib.storage.repository.Repository} is
 * a CRUD-by-id base, {@link com.uxplima.uxmlib.storage.repository.StorageProvider} puts SQL and a
 * {@link com.uxplima.uxmlib.storage.repository.FileStorageProvider flat-file} backend behind one interface,
 * {@link com.uxplima.uxmlib.storage.repository.CachedStorage} is a write-through cache / data-holder,
 * {@link com.uxplima.uxmlib.storage.repository.WriteBehindStorage} is the dirty-tracked write-behind variant
 * that coalesces many writes into one flush, and
 * {@link com.uxplima.uxmlib.storage.repository.Cache} is a thin Caffeine wrapper.
 */
@NullMarked
package com.uxplima.uxmlib.storage.repository;

import org.jspecify.annotations.NullMarked;
