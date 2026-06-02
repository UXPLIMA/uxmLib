package com.uxplima.uxmlib.condition.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An ordered sequence of {@link Action}s, parsed once from a list of config strings and run together against
 * one {@link ActionContext}. {@link #run(ActionContext)} fires each action in declaration order; this is the
 * unit a condition's "on success" / "on failure" hook drives, and the unit a menu click or event handler runs.
 *
 * <p>The list does not schedule anything itself — it exposes whether it {@link #hasAsyncActions() contains any
 * async-flagged action} so a driver can decide once whether the whole run needs a thread hop, but the choice
 * of {@code Scheduler} lane stays with the caller. Running in order on the calling thread keeps the engine
 * pure and unit-testable; production wiring routes the call through the library {@code Scheduler}.
 */
public final class ActionList {

    private final List<Action> actions;

    private ActionList(List<Action> actions) {
        this.actions = List.copyOf(actions);
    }

    /** Parse a list of config action strings once into an immutable {@link ActionList}. */
    public static ActionList parse(List<String> lines) {
        Objects.requireNonNull(lines, "lines");
        List<Action> parsed = new ArrayList<>(lines.size());
        for (String line : lines) {
            parsed.add(ActionParser.parse(line).action());
        }
        return new ActionList(parsed);
    }

    /** An action list wrapping already-built actions (for example from a programmatic source). */
    public static ActionList of(List<Action> actions) {
        Objects.requireNonNull(actions, "actions");
        return new ActionList(actions);
    }

    /** The actions in run order. */
    public List<Action> actions() {
        return actions;
    }

    /** Whether any action in this list is flagged to run off the main thread. */
    public boolean hasAsyncActions() {
        return actions.stream().anyMatch(Action::async);
    }

    /** Run every action in declaration order against the context. */
    public void run(ActionContext context) {
        Objects.requireNonNull(context, "context");
        for (Action action : actions) {
            action.run(context);
        }
    }
}
