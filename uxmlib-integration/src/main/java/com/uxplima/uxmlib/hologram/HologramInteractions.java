package com.uxplima.uxmlib.hologram;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

/**
 * Routes clicks on clickable holograms. A {@link ClickableHologram} spawns a native {@link Interaction}
 * entity sized to its text and registers its UUID here with a callback; the single listener matches an
 * interact event to that UUID and fires the callback, debounced per player so a click is not double-fired.
 * Register it once with {@link #install()} and spawn clickable holograms through {@link #clickable}.
 */
public final class HologramInteractions implements Listener {

    private static final long DEBOUNCE_MS = 150L;

    private final Plugin plugin;
    private final Map<UUID, Consumer<HologramClick>> callbacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public HologramInteractions(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Register the interact listener. Call once on enable. */
    public void install() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** Spawn a {@link ClickableHologram} at {@code spec}'s location, wiring {@code onClick} to its clicks. */
    public ClickableHologram clickable(
            HologramSpec spec,
            org.bukkit.Location location,
            float width,
            float height,
            Consumer<HologramClick> onClick) {
        Objects.requireNonNull(onClick, "onClick");
        ClickableHologram hologram = ClickableHologram.spawn(spec, location, width, height);
        callbacks.put(hologram.interaction().getUniqueId(), onClick);
        return hologram;
    }

    /** Stop routing clicks for {@code hologram} (call when removing it). */
    public void forget(ClickableHologram hologram) {
        Objects.requireNonNull(hologram, "hologram");
        callbacks.remove(hologram.interaction().getUniqueId());
    }

    @EventHandler
    void onRightClick(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Interaction interaction) {
            fire(interaction.getUniqueId(), new HologramClick(event.getPlayer(), HologramClick.Type.RIGHT));
        }
    }

    private void fire(UUID interactionId, HologramClick click) {
        Consumer<HologramClick> callback = callbacks.get(interactionId);
        if (callback != null && notDebounced(click.player().getUniqueId())) {
            callback.accept(click);
        }
    }

    private boolean notDebounced(UUID player) {
        long now = System.currentTimeMillis();
        Long previous = lastClick.put(player, now);
        return previous == null || now - previous >= DEBOUNCE_MS;
    }
}
