package com.uxplima.uxmlib.scheduler;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * The {@link Scheduler} implementation over Paper's native schedulers. The same code runs on Paper and
 * Folia: {@code GlobalRegionScheduler}, {@code RegionScheduler}, the per-entity scheduler and
 * {@code AsyncScheduler} all exist on both, so no version detection is needed.
 */
public final class PaperScheduler implements Scheduler {

    // The entity scheduler takes a "retired" callback that runs if the entity is removed before the task
    // fires. The library has nothing to do in that case, so each call site passes an inline no-op.
    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public TaskHandle global(Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run()));
    }

    @Override
    public TaskHandle globalLater(Duration delay, Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), Ticks.fromDuration(delay)));
    }

    @Override
    public TaskHandle globalTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(
                        plugin,
                        t -> task.accept(new PaperTaskHandle(t)),
                        Ticks.fromDuration(delay),
                        Ticks.fromDuration(period)));
    }

    @Override
    public TaskHandle region(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(Bukkit.getRegionScheduler().run(plugin, location, t -> task.run()));
    }

    @Override
    public TaskHandle regionLater(Location location, Duration delay, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(
                Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> task.run(), Ticks.fromDuration(delay)));
    }

    @Override
    public TaskHandle regionTimer(Location location, Duration delay, Duration period, Consumer<TaskHandle> task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(Bukkit.getRegionScheduler()
                .runAtFixedRate(
                        plugin,
                        location,
                        t -> task.accept(new PaperTaskHandle(t)),
                        Ticks.fromDuration(delay),
                        Ticks.fromDuration(period)));
    }

    @Override
    public TaskHandle entity(Entity entity, Runnable task) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(entity.getScheduler().run(plugin, t -> task.run(), () -> {}));
    }

    @Override
    public TaskHandle entityLater(Entity entity, Duration delay, Runnable task) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(
                entity.getScheduler().runDelayed(plugin, t -> task.run(), () -> {}, Ticks.fromDuration(delay)));
    }

    @Override
    public TaskHandle entityTimer(Entity entity, Duration delay, Duration period, Consumer<TaskHandle> task) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(entity.getScheduler()
                .runAtFixedRate(
                        plugin,
                        t -> task.accept(new PaperTaskHandle(t)),
                        () -> {},
                        Ticks.fromDuration(delay),
                        Ticks.fromDuration(period)));
    }

    @Override
    public TaskHandle async(Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run()));
    }

    @Override
    public TaskHandle asyncLater(Duration delay, Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(
                Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), millis(delay), TimeUnit.MILLISECONDS));
    }

    @Override
    public TaskHandle asyncTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
        Objects.requireNonNull(task, "task");
        return new PaperTaskHandle(Bukkit.getAsyncScheduler()
                .runAtFixedRate(
                        plugin,
                        t -> task.accept(new PaperTaskHandle(t)),
                        millis(delay),
                        millis(period),
                        TimeUnit.MILLISECONDS));
    }

    private static long millis(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return Math.max(1L, duration.toMillis());
    }
}
