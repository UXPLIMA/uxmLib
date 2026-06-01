package com.uxplima.uxmlib.config;

/**
 * Thrown when a config file cannot be read, written, or mapped. Wraps Configurate's checked
 * {@code ConfigurateException} so callers handle one unchecked library type.
 */
public final class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
