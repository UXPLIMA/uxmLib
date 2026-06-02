/**
 * A small fluent facade over Paper's server-side {@code Dialog} screens (the native dialog UI added in
 * 1.21.6). {@link com.uxplima.uxmlib.gui.dialog.DialogScreen} builds a notice (one acknowledge button) or a
 * confirmation (yes / no) with a title, body text, and buttons that map to a server-side callback, then
 * shows it to a player. The whole feature is version-gated: on a server older than 1.21.6 the Dialog API is
 * absent, so {@link com.uxplima.uxmlib.gui.dialog.DialogScreen#isSupported()} reports false and
 * {@code show} is a no-op rather than a crash.
 */
@NullMarked
package com.uxplima.uxmlib.gui.dialog;

import org.jspecify.annotations.NullMarked;
