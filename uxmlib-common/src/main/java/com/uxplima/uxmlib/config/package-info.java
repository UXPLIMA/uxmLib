/**
 * Typed configuration over Configurate (HOCON). {@link com.uxplima.uxmlib.config.HoconConfig} loads a
 * file once, swaps it atomically on reload, exposes typed scalar reads, and maps whole subtrees onto
 * {@code @ConfigSerializable} types so config is data with IDE support, not string-keyed lookups.
 */
@NullMarked
package com.uxplima.uxmlib.config;

import org.jspecify.annotations.NullMarked;
