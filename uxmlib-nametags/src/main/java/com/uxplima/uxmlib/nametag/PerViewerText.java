package com.uxplima.uxmlib.nametag;

import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;

/**
 * Supplies the nametag lines a given viewer should see. The whole point of a packet nametag is that two
 * viewers can see different text for the same target (a per-viewer prefix, a localised name, a hidden line),
 * so the content is resolved per viewer rather than once for everyone.
 *
 * <p>Implementations must be cheap and side-effect-free: the renderer calls this on the target's region
 * thread, once per viewer, on every refresh tick.
 */
@FunctionalInterface
public interface PerViewerText {

    /**
     * The lines {@code viewer} should see, top to bottom. Must return at least one element; the single-line
     * renderer uses only the first.
     *
     * @param viewer the UUID of the player who will see the text
     * @return the ordered, non-empty list of lines for that viewer
     */
    List<Component> linesFor(UUID viewer);
}
