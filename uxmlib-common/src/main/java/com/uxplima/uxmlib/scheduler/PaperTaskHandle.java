package com.uxplima.uxmlib.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.jspecify.annotations.Nullable;

/**
 * A {@link TaskHandle} backing onto a Paper {@link ScheduledTask}. A {@code null} task means the work
 * never started (the entity scheduler returns {@code null} when the entity has already been removed), so
 * the handle reports itself cancelled and {@link #cancel()} is a no-op.
 */
final class PaperTaskHandle implements TaskHandle {

    private final @Nullable ScheduledTask task;

    PaperTaskHandle(@Nullable ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return task == null || task.isCancelled();
    }
}
