package com.uxplima.uxmlib.packet;

import net.minecraft.world.entity.Entity;

/**
 * Allocates fake-entity ids from the shared server counter. Ids it hands out never collide with a real entity
 * because they come from the same monotonic source the server itself uses for spawning.
 */
public final class EntityIds {

    private EntityIds() {}

    /** The next free entity id from the shared server counter. */
    public static int next() {
        return Entity.nextEntityId();
    }
}
