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
     * head is aimed at {@code yaw} too, matching the body's initial facing.
     */
    Object spawnPlayer(int entityId, UUID profileId, double x, double y, double z, float yaw, float pitch);

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
