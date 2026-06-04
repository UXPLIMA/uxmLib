/**
 * ReDoS-guarded regex execution. {@link com.uxplima.uxmlib.common.regex.TimedRegex} runs a caller-supplied
 * regex operation under a per-call timeout on an injected executor and actually stops a catastrophically
 * backtracking pattern (the JDK engine ignores {@code Future#cancel} during backtracking) by routing the
 * engine's per-character reads through an interrupt-aware {@link java.lang.CharSequence} view. Any plugin
 * that matches user-supplied patterns against user-supplied text can use it to stay fail-open under a
 * malicious or accidental ReDoS pattern.
 */
@NullMarked
package com.uxplima.uxmlib.common.regex;

import org.jspecify.annotations.NullMarked;
