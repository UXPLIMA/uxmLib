package com.uxplima.uxmlib.hologram.pool;

import java.util.Set;
import java.util.UUID;

import com.uxplima.uxmlib.hologram.Hologram;

/**
 * Computes the set of players who should currently see a hologram — same world and within the registered
 * squared radius. Pulled out as a seam so the {@link HologramPool}'s diff lifecycle can run against a fake
 * hologram and a canned player set in tests, while production reads the live entity location and world.
 */
@FunctionalInterface
interface NearbyPlayers {

    /** The UUIDs that should see {@code hologram} given its squared cull radius {@code radiusSquared}. */
    Set<UUID> desiredFor(Hologram hologram, double radiusSquared);
}
