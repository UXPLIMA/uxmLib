package com.uxplima.uxmlib.packet.npc.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.uxplima.uxmlib.npc.PacketSender;
import com.uxplima.uxmlib.packet.Bundles;
import com.uxplima.uxmlib.packet.Codecs;
import com.uxplima.uxmlib.packet.EntityIds;
import com.uxplima.uxmlib.packet.npc.ByteAngle;
import com.uxplima.uxmlib.packet.npc.NpcPackets;
import com.uxplima.uxmlib.packet.tablist.TabSkin;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The sole NMS-bearing class of the packet NPC layer: it builds the real Mojang-mapped packets that spawn and
 * steer a fake-player NPC per viewer and writes them through the connection. Quarantining {@code net.minecraft}
 * to one class follows the same precedent as {@code uxmlib-npc}'s {@code ChannelResolver}, the nametag
 * renderer's {@code NmsNametagPackets}, and the tablist renderer's {@code NmsTabListPackets}.
 *
 * <p>Built against the Mojang-mapped 1.21.11 dev bundle; Paper's runtime remapper maps these back to the
 * server's own mappings at load. Two construction notes that are easy to get wrong:
 *
 * <ul>
 *   <li><b>Spawning a player.</b> Since 1.20.2 there is no separate add-player packet; a fake player spawns
 *       through {@code ClientboundAddEntityPacket} with {@code EntityType.PLAYER}. The spawn UUID must equal
 *       the profile id from the player-info ADD entry, or the client will not link the skin to the entity.
 *       That public constructor packs the raw degree rotations itself (via {@code Mth.packDegrees}), so spawn
 *       passes raw floats; the standalone look and teleport packets instead take a pre-packed byte, which
 *       {@link ByteAngle} produces with the same {@code floor} math so the two stay in step.
 *   <li><b>Head rotation.</b> {@code ClientboundRotateHeadPacket} exposes only an {@code Entity}-bound public
 *       constructor and a private buffer one, so it is built through its public stream codec exactly like the
 *       passenger packet in {@code NmsNametagPackets} — write the wire form (entity id, head-yaw byte), decode.
 * </ul>
 *
 * <p>The player-info ADD entry carries the name and skin so the client can render the body; it is assembled
 * through Paper's public {@code ClientboundPlayerInfoUpdatePacket(EnumSet<Action>, List<Entry>)} constructor,
 * the same path {@code NmsTabListPackets} uses. The {@code GameProfile} {@code textures} property is the same
 * five lines as the tablist builder; it is replicated here rather than shared to avoid coupling the two
 * renderers through an extracted helper.
 */
public final class NmsNpcPackets implements NpcPackets {

    private final PacketSender sender;

    public NmsNpcPackets(PacketSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public int allocateEntityId() {
        return EntityIds.next();
    }

    @Override
    public Object tabAdd(UUID profileId, String name, @Nullable TabSkin skin) {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(name, "name");
        EnumSet<Action> actions = EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_LISTED);
        Entry entry = new Entry(
                profileId, profileFor(profileId, name, skin), true, 0, GameType.DEFAULT_MODE, null, true, 0, null);
        return new ClientboundPlayerInfoUpdatePacket(actions, List.of(entry));
    }

    @Override
    public Object tabRemove(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId");
        return new ClientboundPlayerInfoRemovePacket(List.of(profileId));
    }

    @Override
    public Object spawnPlayer(int entityId, UUID profileId, double x, double y, double z, float yaw, float pitch) {
        Objects.requireNonNull(profileId, "profileId");
        // The spawn UUID is the profile id so the client links the skin; the ctor packs the raw degrees itself,
        // and the head yaw matches the body yaw so the NPC faces one way on spawn.
        return new ClientboundAddEntityPacket(
                entityId, profileId, x, y, z, pitch, yaw, EntityType.PLAYER, 0, Vec3.ZERO, yaw);
    }

    @Override
    public Object headLook(int entityId, float yaw) {
        byte headYaw = ByteAngle.of(yaw);
        return Codecs.decodeVia(ClientboundRotateHeadPacket.STREAM_CODEC, buffer -> {
            buffer.writeVarInt(entityId);
            buffer.writeByte(headYaw);
        });
    }

    @Override
    public Object bodyLook(int entityId, float yaw, float pitch) {
        return new ClientboundMoveEntityPacket.Rot(entityId, ByteAngle.of(yaw), ByteAngle.of(pitch), true);
    }

    @Override
    public Object teleport(int entityId, double x, double y, double z, float yaw, float pitch) {
        PositionMoveRotation change = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch);
        return new ClientboundTeleportEntityPacket(entityId, change, Set.<Relative>of(), true);
    }

    @Override
    public Object remove(int entityId) {
        return new ClientboundRemoveEntitiesPacket(entityId);
    }

    @Override
    public Object bundle(List<Object> packets) {
        return Bundles.of(packets);
    }

    @Override
    public void send(Player viewer, Object packet) {
        sender.send(viewer, packet);
    }

    /** The {@code GameProfile} for the NPC, carrying the skin as a {@code textures} property when present. */
    private static GameProfile profileFor(UUID profileId, String name, @Nullable TabSkin skin) {
        GameProfile profile = new GameProfile(profileId, name);
        if (skin != null) {
            profile.properties().put("textures", new Property("textures", skin.textureValue(), skin.signature()));
        }
        return profile;
    }
}
