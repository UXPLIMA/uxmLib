package com.uxplima.uxmlib.scheduler;

/**
 * A handle to a scheduled task. Returned by every {@link Scheduler} method so the caller can cancel a
 * delayed or repeating task and check whether it is still live.
 */
public interface TaskHandle {

    /** Cancel the task. Safe to call more than once; a no-op once the task has finished or been cancelled. */
    void cancel();

    /** Whether the task has been cancelled (or never started, e.g. its entity was already removed). */
    boolean isCancelled();
}
