package com.uxplima.uxmlib.condition;

/**
 * What a {@link ConditionList} does when one of its conditions fails. The policy is attached per condition
 * (via {@link ConditionList.Entry}), so a list can, say, silently skip an optional check while still messaging
 * and stopping on a required one.
 */
public enum FailurePolicy {

    /**
     * Record the condition's failure message into the request's error sink and keep evaluating the rest of
     * the list. The default: collect every reason a request failed so the caller can show them all at once.
     */
    SEND_MESSAGE(true, false, false),

    /**
     * Record the message <em>and</em> raise the request's cancel flag, then keep evaluating. Use when a
     * failure should both inform the player and cancel the gated event.
     */
    CANCEL(true, true, false),

    /** Fail without recording any message and keep evaluating — a quiet, message-free required check. */
    SILENCE(false, false, false),

    /**
     * Record the message and stop evaluating the rest of the list immediately (short-circuit). Use for a
     * gate that makes any later check meaningless once it fails.
     */
    STOP_CHAIN(true, false, true);

    private final boolean recordsMessage;
    private final boolean cancels;
    private final boolean stopsChain;

    FailurePolicy(boolean recordsMessage, boolean cancels, boolean stopsChain) {
        this.recordsMessage = recordsMessage;
        this.cancels = cancels;
        this.stopsChain = stopsChain;
    }

    /** Whether a failure under this policy adds the condition's message to the error sink. */
    public boolean recordsMessage() {
        return recordsMessage;
    }

    /** Whether a failure under this policy raises the request's cancel flag. */
    public boolean cancels() {
        return cancels;
    }

    /** Whether a failure under this policy stops the rest of the list from being evaluated. */
    public boolean stopsChain() {
        return stopsChain;
    }
}
