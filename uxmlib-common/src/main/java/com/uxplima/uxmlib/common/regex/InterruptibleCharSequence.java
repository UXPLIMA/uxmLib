package com.uxplima.uxmlib.common.regex;

import java.util.Objects;

/**
 * A {@link CharSequence} view that throws {@link InterruptedRegexException} the moment the matching thread is
 * interrupted. The JDK regex engine never checks the interrupt flag during catastrophic backtracking, so
 * {@code Future#cancel} alone cannot stop a ReDoS pattern; routing the engine's per-character {@code charAt}
 * reads through this guard makes the engine bail when {@link TimedRegex} interrupts the timed-out task.
 */
final class InterruptibleCharSequence implements CharSequence {

    /** Unchecked so it propagates straight out of {@code Matcher.find()} without a checked signature. */
    static final class InterruptedRegexException extends RuntimeException {
        InterruptedRegexException() {
            super("timed regex match interrupted (timeout budget exceeded)");
        }
    }

    private final CharSequence delegate;

    InterruptibleCharSequence(CharSequence delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public char charAt(int index) {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedRegexException();
        }
        return delegate.charAt(index);
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new InterruptibleCharSequence(delegate.subSequence(start, end));
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
