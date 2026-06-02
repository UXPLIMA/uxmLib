package com.uxplima.uxmlib.hologram;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

/**
 * A {@link Hologram} with no Bukkit backing that records the calls made to it, so visibility logic can be
 * asserted without a live world. Shared across hologram test packages (the {@code follow} nameplate test
 * uses it too), so it lives in the main package as a reusable test double rather than a private inner class.
 */
public final class RecordingHologram implements Hologram {

    public boolean restricted;
    public final List<UUID> shown = new ArrayList<>();
    public final List<UUID> hidden = new ArrayList<>();
    public int removed;

    @Override
    public void setText(Component text) {}

    @Override
    public void moveTo(Location to, int interpolationTicks) {}

    @Override
    public void setTransform(Transform transform) {}

    @Override
    public boolean attachTo(Entity target) {
        return false;
    }

    @Override
    public void restrictToViewers() {
        restricted = true;
    }

    @Override
    public void show(Plugin plugin, Player viewer) {
        shown.add(viewer.getUniqueId());
    }

    @Override
    public void hide(Plugin plugin, Player viewer) {
        hidden.add(viewer.getUniqueId());
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return shown.contains(viewer.getUniqueId()) && !hidden.contains(viewer.getUniqueId());
    }

    @Override
    public void forgetViewer(UUID viewer) {}

    @Override
    public void remove() {
        removed++;
    }

    @Override
    public TextDisplay entity() {
        throw new UnsupportedOperationException("no entity in the recording double");
    }
}
