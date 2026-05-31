package com.uxplima.uxmlib.storage;

/**
 * An unchecked wrapper for a {@link java.sql.SQLException} raised while running a query or update, so
 * callers handle one library type instead of the JDBC checked exception.
 */
public final class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
