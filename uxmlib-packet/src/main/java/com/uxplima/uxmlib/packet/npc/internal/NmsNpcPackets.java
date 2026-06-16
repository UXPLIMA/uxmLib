package com.uxplima.uxmlib.packet.npc.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import com.uxplima.uxmlib.npc.PacketSender;
import com.uxplima.uxmlib.packet.Bundles;
import com.uxplima.uxmlib.packet.Codecs;
import com.uxplima.uxmlib.packet.EntityIds;
import com.uxplima.uxmlib.packet.Reflect;
import com.uxplima.uxmlib.packet.npc.ByteAngle;
import com.uxplima.uxmlib.packet.npc.EquipmentSlot;
import com.uxplima.uxmlib.packet.npc.NamedColor;
import com.uxplima.uxmlib.packet.npc.NpcPackets;
import com.uxplima.uxmlib.packet.npc.NpcPose;
import com.uxplima.uxmlib.packet.tablist.TabSkin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
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
 *   <li><b>Spawning an entity.</b> Since 1.20.2 there is no separate add-player packet; every NPC — a fake
 *       player or a mob — spawns through {@code ClientboundAddEntityPacket}, so both spawn methods share one
 *       builder that differs only in the {@code EntityType} and the spawn UUID. A fake player passes {@code
 *       EntityType.PLAYER} and the profile id as the spawn UUID, which must equal the id from the player-info
 *       ADD entry or the client will not link the skin to the entity; a mob passes the type resolved from the
 *       entity-type registry and an opaque entity UUID with no skin to bind. That public constructor packs the
 *       raw degree rotations itself (via {@code Mth.packDegrees}), so spawn passes raw floats; the standalone
 *       look and teleport packets instead take a pre-packed byte, which {@link ByteAngle} produces with the same
 *       {@code floor} math so the two stay in step.
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
 *
 * <p>Three more construction notes cover the equipment and glow packets:
 *
 * <ul>
 *   <li><b>Equipment.</b> {@code ClientboundSetEquipmentPacket(int, List<Pair<EquipmentSlot, ItemStack>>)} takes
 *       the server's own {@code ItemStack}, so each Bukkit item is copied across with {@code
 *       CraftItemStack.asNMSCopy} — the standard bridge from the Bukkit item form to the server's.
 *   <li><b>Glow.</b> Glowing is the {@code glowing} bit of an entity's shared-flags byte (data item 0). The
 *       accessor and the bit index are both {@code protected static} on {@code Entity}, so they are read once at
 *       construction through {@link Reflect} (the same precedent as the nametag accessors), and the metadata
 *       packet sets that single byte.
 *   <li><b>Glow colour.</b> The client tints a glowing outline with the colour of the team the entity's name is
 *       on, so a colour is a {@code ClientboundSetPlayerTeamPacket.createAddOrModifyPacket} over a throwaway
 *       {@code PlayerTeam} carrying the {@code ChatFormatting} colour and the NPC's profile name as its member —
 *       the approach FancyNpcs uses.
 * </ul>
 */
public final class NmsNpcPackets implements NpcPackets {

    private final PacketSender sender;

    /** The {@code Byte} data item that holds an entity's shared flags (on-fire, sneaking, glowing, ...). */
    private final EntityDataAccessor<Byte> sharedFlagsAccessor;
    /** The shared-flags byte with only the glowing bit set, sent to switch the outline on. */
    private final byte glowingFlag;
    /** The {@code Pose} data item that holds an entity's body pose; read once, like the shared-flags accessor. */
    private final EntityDataAccessor<Pose> poseAccessor;

    public NmsNpcPackets(PacketSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
        // Read the shared-flags and pose accessors and the glowing bit index once here, off every hot path.
        // FLAG_GLOWING is the bit position (6); the wire value is the byte with that one bit set.
        this.sharedFlagsAccessor = Reflect.accessor(Entity.class, "DATA_SHARED_FLAGS_ID");
        int glowingBit = Reflect.accessor(Entity.class, "FLAG_GLOWING");
        this.glowingFlag = (byte) (1 << glowingBit);
        this.poseAccessor = Reflect.accessor(Entity.class, "DATA_POSE");
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
        // The spawn UUID is the profile id so the client links the skin; the head yaw matches the body yaw so the
        // NPC faces one way on spawn. PLAYER is the specialisation of the generic add-entity build below.
        return addEntity(entityId, profileId, EntityType.PLAYER, x, y, z, yaw, pitch);
    }

    @Override
    public Object spawnEntity(
            int entityId, UUID entityUuid, String entityTypeKey, double x, double y, double z, float yaw, float pitch) {
        Objects.requireNonNull(entityUuid, "entityUuid");
        Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        // A mob has no player-info entry, so entityUuid is just the opaque spawn UUID. Resolve the server entity
        // type from the key off the entity-type registry; an unresolved key is a guard failure (the plugin
        // validates first).
        return addEntity(entityId, entityUuid, resolveType(entityTypeKey), x, y, z, yaw, pitch);
    }

    /**
     * The generic {@code ClientboundAddEntityPacket} builder both spawn methods share. The public constructor
     * packs the raw degree rotations itself (via {@code Mth.packDegrees}), so it takes raw floats; data is 0 and
     * the delta movement zero for a static NPC, and the head yaw matches the body yaw so the NPC faces one way on
     * spawn.
     */
    private static ClientboundAddEntityPacket addEntity(
            int entityId, UUID spawnUuid, EntityType<?> type, double x, double y, double z, float yaw, float pitch) {
        return new ClientboundAddEntityPacket(entityId, spawnUuid, x, y, z, pitch, yaw, type, 0, Vec3.ZERO, yaw);
    }

    /**
     * Resolve a namespaced-or-plain entity-type key to the server {@link EntityType}. {@code "villager"} is
     * defaulted to the {@code minecraft} namespace; an unparseable or unknown key resolves to nothing and is
     * rejected, matching the port's documented guard.
     */
    private static EntityType<?> resolveType(String entityTypeKey) {
        Identifier id = entityTypeKey.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0
                ? Identifier.withDefaultNamespace(entityTypeKey)
                : Identifier.tryParse(entityTypeKey);
        EntityType<?> type = id == null
                ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            throw new IllegalArgumentException("Unknown entity type key: " + entityTypeKey);
        }
        return type;
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
    public Object equipment(int entityId, Map<EquipmentSlot, ItemStack> items) {
        Objects.requireNonNull(items, "items");
        List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> slots =
                new ArrayList<>(items.size());
        for (Map.Entry<EquipmentSlot, ItemStack> entry : items.entrySet()) {
            slots.add(Pair.of(toNmsSlot(entry.getKey()), CraftItemStack.asNMSCopy(entry.getValue())));
        }
        return new ClientboundSetEquipmentPacket(entityId, slots);
    }

    @Override
    public Object glow(int entityId, boolean glowing) {
        // A fresh NPC carries no other shared flags, so the byte is the glowing bit alone (or zero to clear it).
        byte flags = glowing ? glowingFlag : 0;
        SynchedEntityData.DataValue<Byte> value = SynchedEntityData.DataValue.create(sharedFlagsAccessor, flags);
        return new ClientboundSetEntityDataPacket(entityId, List.of(value));
    }

    @Override
    public Object pose(int entityId, NpcPose pose) {
        Objects.requireNonNull(pose, "pose");
        // Resolve the server pose from the constant's server name (GLIDING -> FALL_FLYING, the rest by name) and
        // ship it the same way glow ships its byte: DataValue.create derives the POSE serializer from the accessor.
        Pose nmsPose = Pose.valueOf(pose.serverName());
        SynchedEntityData.DataValue<Pose> value = SynchedEntityData.DataValue.create(poseAccessor, nmsPose);
        return new ClientboundSetEntityDataPacket(entityId, List.of(value));
    }

    @Override
    public Object scale(int entityId, double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException("scale must be finite and positive, was " + scale);
        }
        // The packet's public constructor reads each instance's base value and modifiers into a snapshot, so an
        // instance carrying just the scale base value (no modifiers) is the simplest way past the private snapshot
        // constructor without a registry-buffer round-trip.
        AttributeInstance instance = new AttributeInstance(Attributes.SCALE, ignored -> {});
        instance.setBaseValue(scale);
        return new ClientboundUpdateAttributesPacket(entityId, List.of(instance));
    }

    @Override
    public Object glowColor(String teamName, String memberName, @Nullable NamedColor color) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(memberName, "memberName");
        // A throwaway scoreboard is fine: the packet copies the team's parameters and member list off it, and a
        // PlayerTeam needs a Scoreboard only to construct.
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        if (color != null) {
            team.setColor(ChatFormatting.valueOf(color.name()));
        }
        team.getPlayers().add(memberName);
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
    }

    @Override
    public Object glowColorRemove(String teamName) {
        Objects.requireNonNull(teamName, "teamName");
        // The remove packet carries only the team name off the team object, so a throwaway one suffices; the
        // client drops the named team if it has it and ignores the packet otherwise.
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        return ClientboundSetPlayerTeamPacket.createRemovePacket(team);
    }

    @Override
    public Object bundle(List<Object> packets) {
        return Bundles.of(packets);
    }

    @Override
    public void send(Player viewer, Object packet) {
        sender.send(viewer, packet);
    }

    /** Map a port-side equipment slot onto the matching server slot — the single place the two names meet. */
    private static net.minecraft.world.entity.EquipmentSlot toNmsSlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
        };
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
