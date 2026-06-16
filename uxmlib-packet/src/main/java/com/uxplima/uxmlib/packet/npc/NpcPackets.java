package com.uxplima.uxmlib.packet.npc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.packet.tablist.TabSkin;
import org.jspecify.annotations.Nullable;

/**
 * The seam between pure NPC logic and the NMS packet construction for a fake-player NPC. Every packet crosses
 * this boundary as an opaque {@link Object}, so this interface — and everything that depends on it — carries no
 * {@code net.minecraft} reference and stays unit-testable against a fake. The single implementation that builds
 * the real Mojang-mapped packets against the dev bundle lives behind this port in {@code npc.internal}.
 *
 * <p>A fake-player NPC is shown to a client in a fixed sequence: first a player-info entry carrying the name
 * and skin so the client knows how to render the body, then the entity spawn at a position and rotation, then
 * the look packets that aim its head and body. The client links the skin to the spawned entity because the
 * spawn UUID equals the player-info entry's profile id — so the spawn must reuse the same {@code profileId}.
 * The caller typically removes the player-info entry from the visible tab a moment after spawning (a short
 * delay so the client has parsed the entry); {@link #tabRemove(UUID)} builds that removal. Each method returns
 * one built packet; {@link #send(Player, Object)} writes it to a viewer's connection, so the same packet can be
 * sent to many viewers without rebuilding it.
 */
public interface NpcPackets {

    /** Allocate a fake-entity id from the shared server counter; it never collides with a real entity. */
    int allocateEntityId();

    /**
     * Build a player-info ADD packet that seats a fully-controlled profile (name, and the skin as a
     * {@code textures} property when present) so the client can render the NPC's body. The entry is marked
     * listed; the caller removes it from the visible tab shortly after the spawn with {@link #tabRemove(UUID)}.
     */
    Object tabAdd(UUID profileId, String name, @Nullable TabSkin skin);

    /** Build a player-info REMOVE packet that drops {@code profileId} from the tab list. */
    Object tabRemove(UUID profileId);

    /**
     * Build the entity-spawn packet for a fake player at {@code x,y,z} facing {@code yaw}/{@code pitch}. The
     * spawn UUID is {@code profileId} so the client links the skin from the matching player-info entry; the
     * head is aimed at {@code yaw} too, matching the body's initial facing. This is the {@code PLAYER}
     * specialisation of {@link #spawnEntity}: the player path needs the profile id as the spawn UUID so the
     * skin binds, whereas a mob carries an opaque entity UUID with no profile behind it.
     */
    Object spawnPlayer(int entityId, UUID profileId, double x, double y, double z, float yaw, float pitch);

    /**
     * Build the entity-spawn packet for a mob NPC of {@code entityTypeKey} (a namespaced or plain entity-type
     * key, e.g. {@code "minecraft:villager"} or {@code "villager"}) at {@code x,y,z} facing {@code yaw}/{@code
     * pitch}. Unlike {@link #spawnPlayer} there is no player-info entry: a mob spawns straight through the spawn
     * packet, so {@code entityUuid} is just the opaque entity UUID (no profile, no skin binding). The same
     * generic add-entity builder backs both methods; this one resolves the server entity type from the key.
     *
     * @throws IllegalArgumentException if {@code entityTypeKey} resolves to no known entity type. The caller is
     *     expected to validate the type before calling, so this is a defensive guard rather than a routine path.
     */
    Object spawnEntity(
            int entityId, UUID entityUuid, String entityTypeKey, double x, double y, double z, float yaw, float pitch);

    /** Build a head-rotation packet that aims only the NPC's head at {@code yaw}. */
    Object headLook(int entityId, float yaw);

    /** Build a rotation-only move packet that turns the NPC's body to {@code yaw}/{@code pitch}. */
    Object bodyLook(int entityId, float yaw, float pitch);

    /** Build a teleport packet that moves the NPC to {@code x,y,z} facing {@code yaw}/{@code pitch}. */
    Object teleport(int entityId, double x, double y, double z, float yaw, float pitch);

    /** Build a packet that despawns the NPC by its entity id. */
    Object remove(int entityId);

    /**
     * Build the set-equipment packet that dresses the NPC: each {@link EquipmentSlot} in {@code items} maps to the
     * {@link ItemStack} worn there. Equipment is inherently item data, so this port takes the Bukkit
     * {@link ItemStack} directly (the implementation copies it into the server's own item form). An empty map
     * produces a packet that strips every slot the NPC could wear, so a re-render that drops a slot clears it on
     * the client rather than leaving a stale item.
     */
    Object equipment(int entityId, Map<EquipmentSlot, ItemStack> items);

    /**
     * Build the metadata packet that turns the NPC's glowing outline on or off through the entity's shared-flags
     * byte (the {@code glowing} bit). White is the default outline colour; {@link #glowColor} tints it. A fresh
     * NPC carries no other shared flags (not on fire, not sneaking), so the packet sets only the one bit.
     */
    Object glow(int entityId, boolean glowing);

    /**
     * Build the metadata packet that freezes the NPC in {@code pose} through the entity's {@code DATA_POSE} field.
     * A pose only renders where the entity type supports it (a player or a humanoid mob), so an NPC whose type
     * cannot strike the pose simply ignores it — the packet is harmless either way. The accessor is read once at
     * construction, like the glow accessor, so this stays off the reflection path on every send.
     */
    Object pose(int entityId, NpcPose pose);

    /**
     * Build the attribute packet that resizes the NPC to {@code scale} through the {@code minecraft:generic.scale}
     * attribute (1.0 is the natural size; 2.0 is twice as tall, 0.5 half). The attribute applies to every entity
     * type. The plugin command is the primary clamp; the implementation guards only against a non-finite or
     * non-positive value, which the protocol cannot represent.
     */
    Object scale(int entityId, double scale);

    /**
     * Build the metadata packet that toggles an {@code AgeableMob}'s baby/adult state through the {@code
     * AgeableMob.DATA_BABY_ID} field — the boolean ({@code true} is a baby, {@code false} an adult) carried by the
     * breeding animal line (cows, pigs, …), the villager line, and the hoglin (all real {@code AgeableMob}
     * subclasses). The caller must only ever send this to a type that actually extends {@code AgeableMob}: the
     * baby flag's data index is allocated per class hierarchy, so it differs between {@code AgeableMob} and the
     * monster-family baby mobs (the zombie line, piglins, zoglins) — those carry their own baby field at a
     * different index and have their own builders ({@link #zombieBaby}, {@link #piglinBaby}, {@link #zoglinBaby}).
     * Sending this packet to one of them, or to a non-ageable type (a creeper, a slime), would land the value on
     * an unrelated field. Picking the right builder for the type is the plugin's per-type concern, not this one's.
     */
    Object baby(int entityId, boolean baby);

    /**
     * Build the metadata packet that toggles a zombie-line mob's baby/adult state through {@code
     * Zombie.DATA_BABY_ID} — the zombie family's own baby boolean, at a different data index than {@link #baby}'s
     * {@code AgeableMob} one because zombies extend {@code Monster}, not {@code AgeableMob}. Send this only to a
     * zombie, husk, drowned, zombie villager, or zombified piglin; any other type has no field at that index.
     */
    Object zombieBaby(int entityId, boolean baby);

    /**
     * Build the metadata packet that toggles a piglin's baby/adult state through {@code Piglin.DATA_BABY_ID} — the
     * piglin's own baby boolean, again at a different index than {@link #baby}'s because piglins extend {@code
     * Monster}. Send this only to a piglin (not a piglin brute, which has no baby form); any other type has no
     * field at that index.
     */
    Object piglinBaby(int entityId, boolean baby);

    /**
     * Build the metadata packet that toggles a zoglin's baby/adult state through {@code Zoglin.DATA_BABY_ID} — the
     * zoglin's own baby boolean, at a different index than {@link #baby}'s because zoglins extend {@code Monster}.
     * Send this only to a zoglin; any other type has no field at that index.
     */
    Object zoglinBaby(int entityId, boolean baby);

    /**
     * Build the metadata packet that sets a villager's appearance — its biome {@code type} (the registry name of a
     * villager type, e.g. {@code plains}/{@code desert}), its {@code profession} (e.g. {@code farmer}/{@code
     * librarian}), and its {@code level} (1–5, the badge tier) — through the villager's {@code DATA_VILLAGER_DATA}
     * field. The type and profession are resolved by name off the server's villager-type and villager-profession
     * registries, both defaulted registries: an unknown name falls back to the registry default rather than
     * throwing, so a typo renders the default villager rather than failing the spawn. Send this only to a villager;
     * any other type has no villager-data field at that index.
     *
     * @param type the villager-type registry name (plain or namespaced), defaulted when unknown
     * @param profession the villager-profession registry name (plain or namespaced), defaulted when unknown
     * @param level the badge level the client renders (the protocol uses 1–5)
     */
    Object villagerData(int entityId, String type, String profession, int level);

    /**
     * Build the metadata packet that sets a slime's (or magma cube's) size through the {@code ID_SIZE} field — an
     * integer where larger is bigger and the body's collision box and render scale follow it. The protocol stores
     * the size as {@code size}; the client renders a {@code size}-block-ish cube. Send this only to a slime or
     * magma cube; any other type has no size field at that index. The plugin clamps the value to a sane range
     * before calling — this builder only guards against the impossible.
     *
     * @param size the slime size (the plugin clamps it; 1 is the smallest natural slime)
     * @throws IllegalArgumentException if {@code size} is below 1, which the slime size field cannot represent
     */
    Object slimeSize(int entityId, int size);

    /**
     * Build the metadata packet that toggles a creeper's charged (powered) state through the creeper's {@code
     * DATA_IS_POWERED} field — the boolean that renders the blue electrified aura a charged creeper carries. Send
     * this only to a creeper; any other type has no powered field at that index.
     */
    Object charged(int entityId, boolean charged);

    /**
     * Build the scoreboard-team packet that tints the NPC's glow to {@code color}. The client colours a glowing
     * entity's outline with the colour of the team its name is a member of, so this packet creates (or modifies) a
     * team named {@code teamName}, sets its colour, and seats {@code memberName} as a member. For a fake player
     * the member name is its profile name (the same name carried on the player-info entry, capped at 16 chars). A
     * {@code null} colour leaves the team colourless, which renders the default white outline.
     *
     * @param teamName the team name to create or modify (stable per NPC so a re-colour reuses it)
     * @param memberName the NPC's profile name, seated as the team's sole member
     * @param color the glow colour, or {@code null} for the default white outline
     */
    Object glowColor(String teamName, String memberName, @Nullable NamedColor color);

    /**
     * Build the scoreboard-team packet that removes the glow-colour team {@code teamName} from the client. A team
     * created by {@link #glowColor} is client-side state that outlives the entity — despawning the fake player does
     * not drop it — so a viewer who no longer sees the NPC, or sees it stop glowing, must be sent this to clear the
     * orphaned team (otherwise the colour can later bind to a real player who happens to share the seated name).
     * Removing a team the client never had is a harmless no-op, so this is safe to send on every despawn path.
     *
     * @param teamName the team name to remove (the same name {@link #glowColor} created)
     */
    Object glowColorRemove(String teamName);

    /** Wrap several already-built packets into one bundle so a tab-add + spawn arrives as one atomic frame. */
    Object bundle(List<Object> packets);

    /** Write {@code packet} to {@code viewer}'s connection. A no-op if the connection cannot be resolved. */
    void send(Player viewer, Object packet);
}
