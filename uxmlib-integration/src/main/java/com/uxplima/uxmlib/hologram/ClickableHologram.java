package com.uxplima.uxmlib.hologram;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.Interaction;

/**
 * A text hologram with a native {@link Interaction} entity over it, so it can be clicked. Both entities
 * are spawned together; removing the clickable removes both. Create one through
 * {@link HologramInteractions#clickable} so its clicks are routed. The interaction box is sized in blocks
 * by the {@code width}/{@code height} given at spawn.
 */
public final class ClickableHologram {

    private final Hologram hologram;
    private final Interaction interaction;

    private ClickableHologram(Hologram hologram, Interaction interaction) {
        this.hologram = hologram;
        this.interaction = interaction;
    }

    static ClickableHologram spawn(HologramSpec spec, Location location, float width, float height) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(location, "location");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        Objects.requireNonNull(location.getWorld(), "location world");
        Hologram text = Holograms.spawn(spec, location);
        Interaction box = location.getWorld().spawn(location, Interaction.class, entity -> {
            entity.setInteractionWidth(width);
            entity.setInteractionHeight(height);
            entity.setResponsive(true);
            Markers.stamp(entity);
        });
        return new ClickableHologram(text, box);
    }

    /** The text hologram (move/restyle it through this). */
    public Hologram text() {
        return hologram;
    }

    /** The backing interaction entity (its UUID keys the click router). */
    public Interaction interaction() {
        return interaction;
    }

    /** Despawn both the text and the interaction entity. */
    public void remove() {
        hologram.remove();
        interaction.remove();
    }
}
