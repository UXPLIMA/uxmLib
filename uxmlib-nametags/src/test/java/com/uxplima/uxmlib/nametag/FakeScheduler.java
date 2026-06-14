package com.uxplima.uxmlib.nametag;

import java.time.Duration;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.uxplima.uxmlib.scheduler.Scheduler;
import com.uxplima.uxmlib.scheduler.TaskHandle;
import org.jspecify.annotations.Nullable;

/**
 * A Scheduler that captures the single {@link #entityTimer} the renderer starts, so a test can drive a refresh
 * tick with {@link #tick()} and observe that the returned handle was cancelled. Every method the renderer does
 * not use throws, so an accidental use of another scheduler family is caught.
 */
final class FakeScheduler implements Scheduler {

    private @Nullable Consumer<TaskHandle> timerTask;
    private @Nullable Entity timerEntity;
    private boolean cancelled;

    private final TaskHandle handle = new TaskHandle() {
        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    };

    @Override
    public TaskHandle entityTimer(Entity entity, Duration delay, Duration period, Consumer<TaskHandle> task) {
        this.timerEntity = entity;
        this.timerTask = task;
        this.cancelled = false;
        return handle;
    }

    /** Fire the captured entity timer once, as a tick would. */
    void tick() {
        Consumer<TaskHandle> task = timerTask;
        if (task != null) {
            task.accept(handle);
        }
    }

    boolean cancelled() {
        return cancelled;
    }

    @Nullable Entity timerEntity() {
        return timerEntity;
    }

    boolean hasTimer() {
        return timerTask != null;
    }

    // Unused Scheduler members for these tests.
    @Override
    public TaskHandle global(Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle globalLater(Duration delay, Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle globalTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle region(Location location, Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle regionLater(Location location, Duration delay, Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle regionTimer(Location location, Duration delay, Duration period, Consumer<TaskHandle> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle entity(Entity entity, Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle entityLater(Entity entity, Duration delay, Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle async(Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle asyncLater(Duration delay, Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskHandle asyncTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
        throw new UnsupportedOperationException();
    }
}
