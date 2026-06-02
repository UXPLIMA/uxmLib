package com.uxplima.uxmlib.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.kyori.adventure.text.Component;

import org.jspecify.annotations.Nullable;

/**
 * An AND-combination of conditions, each carrying its own {@link FailurePolicy} and optional failure
 * message. {@link #test(ConditionRequest)} evaluates the entries in order and reports whether they all
 * passed; as it goes it applies each failing entry's policy against the request — recording the message into
 * the request's error sink, raising the cancel flag, or short-circuiting the rest of the chain.
 *
 * <p>The list is "AND" in the sense that the result is true only when no entry failed. It deliberately does
 * <b>not</b> short-circuit on the first failure by default (only a {@link FailurePolicy#STOP_CHAIN} entry
 * does), so the error sink can collect every reason a request failed in one pass — the aggregating-validation
 * style used elsewhere in the library.
 */
public final class ConditionList {

    private final List<Entry> entries;

    private ConditionList(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    /** Start an empty builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A list of a single condition under the default {@link FailurePolicy#SEND_MESSAGE} policy. */
    public static ConditionList of(Condition condition, Component failureMessage) {
        return builder().require(condition, failureMessage).build();
    }

    /** The entries in evaluation order. */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Evaluate every entry against the request, applying each failing entry's policy. Returns {@code true}
     * only if no entry failed. Side effects (messages, cancel flag) land on the request.
     */
    public boolean test(ConditionRequest request) {
        Objects.requireNonNull(request, "request");
        boolean allPassed = true;
        for (Entry entry : entries) {
            if (entry.condition().test(request)) {
                continue;
            }
            allPassed = false;
            applyFailure(entry, request);
            if (entry.policy().stopsChain()) {
                break;
            }
        }
        return allPassed;
    }

    private void applyFailure(Entry entry, ConditionRequest request) {
        FailurePolicy policy = entry.policy();
        if (policy.recordsMessage()) {
            entry.failureMessage().ifPresent(request::addError);
        }
        if (policy.cancels()) {
            request.cancel();
        }
    }

    /** A condition paired with its failure policy and optional failure message. */
    public record Entry(Condition condition, FailurePolicy policy, @Nullable Component message) {

        /** Canonical constructor null-checks the condition and policy; the message is optional. */
        public Entry {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(policy, "policy");
        }

        /** The failure message, if this entry has one. */
        public Optional<Component> failureMessage() {
            return Optional.ofNullable(message);
        }
    }

    /** Assembles a {@link ConditionList} entry by entry. */
    public static final class Builder {

        private final List<Entry> entries = new ArrayList<>();

        private Builder() {}

        /** Add a condition with an explicit policy and failure message. */
        public Builder add(Condition condition, FailurePolicy policy, @Nullable Component failureMessage) {
            entries.add(new Entry(condition, policy, failureMessage));
            return this;
        }

        /** Add a required condition under the default {@link FailurePolicy#SEND_MESSAGE}. */
        public Builder require(Condition condition, Component failureMessage) {
            Objects.requireNonNull(failureMessage, "failureMessage");
            return add(condition, FailurePolicy.SEND_MESSAGE, failureMessage);
        }

        /** Add a silent required condition: no message recorded on failure. */
        public Builder requireSilent(Condition condition) {
            return add(condition, FailurePolicy.SILENCE, null);
        }

        /** Build the immutable list. */
        public ConditionList build() {
            return new ConditionList(entries);
        }
    }
}
