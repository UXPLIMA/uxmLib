package com.uxplima.uxmlib.packet.npc.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.uxplima.uxmlib.packet.npc.HorseVariant;
import com.uxplima.uxmlib.packet.npc.NamedColor;
import com.uxplima.uxmlib.packet.npc.NpcPackets;
import com.uxplima.uxmlib.packet.npc.NpcPose;
import com.uxplima.uxmlib.packet.tablist.TabSkin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
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
    /** The shared-flags byte with only the on-fire bit set; combined with the others in {@link #sharedFlags}. */
    private final byte onFireFlag;
    /** The shared-flags byte with only the invisible bit set; combined with the others in {@link #sharedFlags}. */
    private final byte invisibleFlag;
    /** The {@code Boolean} silent data item on {@code Entity}; read once like the shared-flags accessor. */
    private final EntityDataAccessor<Boolean> silentAccessor;
    /** The {@code Pose} data item that holds an entity's body pose; read once, like the shared-flags accessor. */
    private final EntityDataAccessor<Pose> poseAccessor;
    /** The {@code Boolean} baby/adult data item on {@code AgeableMob} (animals, villagers, hoglin); read once. */
    private final EntityDataAccessor<Boolean> babyAccessor;
    /** The {@code Boolean} baby/adult data item on the zombie line ({@code Zombie}); a separate index. Read once. */
    private final EntityDataAccessor<Boolean> zombieBabyAccessor;
    /** The {@code Boolean} baby/adult data item on {@code Piglin}; a separate index again. Read once. */
    private final EntityDataAccessor<Boolean> piglinBabyAccessor;
    /** The {@code Boolean} baby/adult data item on {@code Zoglin}; a separate index again. Read once. */
    private final EntityDataAccessor<Boolean> zoglinBabyAccessor;
    /** The {@code Integer} slime-size data item on {@code Slime}; read once. */
    private final EntityDataAccessor<Integer> slimeSizeAccessor;
    /** The {@code Boolean} powered data item on {@code Creeper}; read once. */
    private final EntityDataAccessor<Boolean> chargedAccessor;
    /** The {@code VillagerData} data item on {@code Villager}; read once. */
    private final EntityDataAccessor<net.minecraft.world.entity.npc.villager.VillagerData> villagerDataAccessor;
    /** The {@code Integer} packed colour+markings variant data item on {@code Horse}; read once. */
    private final EntityDataAccessor<Integer> horseVariantAccessor;
    /** The {@code Integer} coat-variant data item on {@code Llama}; read once. */
    private final EntityDataAccessor<Integer> llamaVariantAccessor;
    /** The {@code Byte} wool data item on {@code Sheep} (low 4 bits colour, bit 0x10 sheared); read once. */
    private final EntityDataAccessor<Byte> sheepWoolAccessor;
    /** The {@code Integer} collar-colour data item on {@code Wolf} (a DyeColor id, 0–15); read once. */
    private final EntityDataAccessor<Integer> wolfCollarAccessor;
    /** The {@code Integer} variant data item on {@code Parrot}; read once. */
    private final EntityDataAccessor<Integer> parrotVariantAccessor;
    /** The {@code Integer} variant data item on {@code Axolotl}; read once. */
    private final EntityDataAccessor<Integer> axolotlVariantAccessor;
    /** The {@code Integer} type data item on {@code Fox}; read once. */
    private final EntityDataAccessor<Integer> foxTypeAccessor;
    /** The {@code Integer} type data item on {@code Rabbit}; read once. */
    private final EntityDataAccessor<Integer> rabbitTypeAccessor;
    /** The {@code Holder<CatVariant>} variant data item on {@code Cat}; read once. The variant is resolved per call. */
    private final EntityDataAccessor<Holder<CatVariant>> catVariantAccessor;
    /** The {@code Holder<FrogVariant>} variant data item on {@code Frog}; read once. The variant is resolved per call. */
    private final EntityDataAccessor<Holder<FrogVariant>> frogVariantAccessor;

    public NmsNpcPackets(PacketSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
        // Read the shared-flags and pose accessors and the glowing bit index once here, off every hot path.
        // FLAG_GLOWING is the bit position (6); the wire value is the byte with that one bit set.
        this.sharedFlagsAccessor = Reflect.accessor(Entity.class, "DATA_SHARED_FLAGS_ID");
        int glowingBit = Reflect.accessor(Entity.class, "FLAG_GLOWING");
        this.glowingFlag = (byte) (1 << glowingBit);
        // The on-fire and invisible bits live in the same shared-flags byte, so sharedFlags composes all three
        // rather than letting glow/on-fire/invisible each overwrite the whole byte. Their bit indices are read off
        // Entity here, once, exactly like the glowing bit.
        int onFireBit = Reflect.accessor(Entity.class, "FLAG_ONFIRE");
        this.onFireFlag = (byte) (1 << onFireBit);
        int invisibleBit = Reflect.accessor(Entity.class, "FLAG_INVISIBLE");
        this.invisibleFlag = (byte) (1 << invisibleBit);
        this.silentAccessor = Reflect.accessor(Entity.class, "DATA_SILENT");
        this.poseAccessor = Reflect.accessor(Entity.class, "DATA_POSE");
        // The type-specific appearance accessors, each read once here off every hot path, exactly like glow/pose.
        // Each lives on the entity class that owns the property, so it is only ever sent to that type (the plugin
        // gates which property reaches which entity); a wrong index would land on an unrelated field otherwise.
        this.babyAccessor = Reflect.accessor(AgeableMob.class, "DATA_BABY_ID");
        // The zombie line, piglins, and zoglins extend Monster (not AgeableMob), so each carries its own baby
        // boolean at its own data index; sending the AgeableMob baby to them would land on an unrelated field.
        this.zombieBabyAccessor = Reflect.accessor(Zombie.class, "DATA_BABY_ID");
        this.piglinBabyAccessor = Reflect.accessor(Piglin.class, "DATA_BABY_ID");
        this.zoglinBabyAccessor = Reflect.accessor(Zoglin.class, "DATA_BABY_ID");
        this.slimeSizeAccessor = Reflect.accessor(Slime.class, "ID_SIZE");
        this.chargedAccessor = Reflect.accessor(Creeper.class, "DATA_IS_POWERED");
        this.villagerDataAccessor = Reflect.accessor(Villager.class, "DATA_VILLAGER_DATA");
        // The animal-variant accessors, each on the class that owns it, read once like the rest. The horse packs
        // colour and markings into one integer; the sheep keeps colour in the low nibble of its wool byte. Each is
        // only ever sent to its own type (the plugin gates which property reaches which entity).
        this.horseVariantAccessor = Reflect.accessor(Horse.class, "DATA_ID_TYPE_VARIANT");
        this.llamaVariantAccessor = Reflect.accessor(Llama.class, "DATA_VARIANT_ID");
        this.sheepWoolAccessor = Reflect.accessor(Sheep.class, "DATA_WOOL_ID");
        this.wolfCollarAccessor = Reflect.accessor(Wolf.class, "DATA_COLLAR_COLOR");
        this.parrotVariantAccessor = Reflect.accessor(Parrot.class, "DATA_VARIANT_ID");
        this.axolotlVariantAccessor = Reflect.accessor(Axolotl.class, "DATA_VARIANT");
        this.foxTypeAccessor = Reflect.accessor(Fox.class, "DATA_TYPE_ID");
        this.rabbitTypeAccessor = Reflect.accessor(Rabbit.class, "DATA_TYPE_ID");
        // The cat and frog variants are dynamic-registry values: their metadata field carries a Holder, not an int.
        // The accessor is read once here like the rest, but the Holder itself is resolved off the live server's
        // registry per call (see catVariant/frogVariant), since the variant set only exists on a running server.
        this.catVariantAccessor = Reflect.accessor(Cat.class, "DATA_VARIANT_ID");
        this.frogVariantAccessor = Reflect.accessor(Frog.class, "DATA_VARIANT_ID");
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
    public Object sharedFlags(int entityId, boolean onFire, boolean glowing, boolean invisible) {
        // Compose the three bits into one shared-flags byte so none overwrites the others' state. An unset flag
        // leaves its bit clear; an all-false call clears the byte to zero (no glow, not on fire, visible).
        byte flags = 0;
        if (onFire) {
            flags |= onFireFlag;
        }
        if (glowing) {
            flags |= glowingFlag;
        }
        if (invisible) {
            flags |= invisibleFlag;
        }
        SynchedEntityData.DataValue<Byte> value = SynchedEntityData.DataValue.create(sharedFlagsAccessor, flags);
        return new ClientboundSetEntityDataPacket(entityId, List.of(value));
    }

    @Override
    public Object silent(int entityId, boolean silent) {
        // DATA_SILENT lives on Entity, so it applies to any NPC type; ship it the same way glow ships its byte.
        return dataPacket(entityId, SynchedEntityData.DataValue.create(silentAccessor, silent));
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
    public Object baby(int entityId, boolean baby) {
        // Ships the AgeableMob baby boolean exactly the way glow ships its byte; the accessor carries the boolean
        // serializer, so DataValue.create needs nothing more than the accessor and the value.
        return dataPacket(entityId, SynchedEntityData.DataValue.create(babyAccessor, baby));
    }

    @Override
    public Object zombieBaby(int entityId, boolean baby) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(zombieBabyAccessor, baby));
    }

    @Override
    public Object piglinBaby(int entityId, boolean baby) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(piglinBabyAccessor, baby));
    }

    @Override
    public Object zoglinBaby(int entityId, boolean baby) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(zoglinBabyAccessor, baby));
    }

    @Override
    public Object villagerData(int entityId, String type, String profession, int level) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(profession, "profession");
        // Resolve the type/profession by name off their defaulted registries — an unknown name returns the registry
        // default rather than nothing — and pack them into the VillagerData record the metadata field carries.
        net.minecraft.world.entity.npc.villager.VillagerData data =
                new net.minecraft.world.entity.npc.villager.VillagerData(
                        holder(BuiltInRegistries.VILLAGER_TYPE, type),
                        holder(BuiltInRegistries.VILLAGER_PROFESSION, profession),
                        level);
        return dataPacket(entityId, SynchedEntityData.DataValue.create(villagerDataAccessor, data));
    }

    @Override
    public Object slimeSize(int entityId, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("slime size must be at least 1, was " + size);
        }
        return dataPacket(entityId, SynchedEntityData.DataValue.create(slimeSizeAccessor, size));
    }

    @Override
    public Object charged(int entityId, boolean charged) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(chargedAccessor, charged));
    }

    @Override
    public Object horseVariant(int entityId, int color, int markings) {
        // Colour and markings pack into the one integer the field carries, the same packing the server uses; the
        // pure HorseVariant helper does the masking so the bit layout stays testable off the NMS path.
        int packed = HorseVariant.pack(color, markings);
        return dataPacket(entityId, SynchedEntityData.DataValue.create(horseVariantAccessor, packed));
    }

    @Override
    public Object llamaVariant(int entityId, int variant) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(llamaVariantAccessor, variant));
    }

    @Override
    public Object sheepColor(int entityId, int color) {
        // The wool byte holds the colour in its low four bits and the sheared flag in bit 0x10; writing the colour
        // alone (the masked nibble) leaves the sheared bit clear, so the sheep renders unsheared in that colour.
        byte wool = (byte) (color & 0x0F);
        return dataPacket(entityId, SynchedEntityData.DataValue.create(sheepWoolAccessor, wool));
    }

    @Override
    public Object wolfCollar(int entityId, int color) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(wolfCollarAccessor, color));
    }

    @Override
    public Object parrotVariant(int entityId, int variant) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(parrotVariantAccessor, variant));
    }

    @Override
    public Object axolotlVariant(int entityId, int variant) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(axolotlVariantAccessor, variant));
    }

    @Override
    public Object foxType(int entityId, int type) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(foxTypeAccessor, type));
    }

    @Override
    public Object rabbitType(int entityId, int type) {
        return dataPacket(entityId, SynchedEntityData.DataValue.create(rabbitTypeAccessor, type));
    }

    @Override
    public @Nullable Object catVariant(int entityId, String name) {
        Objects.requireNonNull(name, "name");
        Holder<CatVariant> variant = dynamicHolder(Registries.CAT_VARIANT, name);
        return variant == null
                ? null
                : dataPacket(entityId, SynchedEntityData.DataValue.create(catVariantAccessor, variant));
    }

    @Override
    public @Nullable Object frogVariant(int entityId, String name) {
        Objects.requireNonNull(name, "name");
        Holder<FrogVariant> variant = dynamicHolder(Registries.FROG_VARIANT, name);
        return variant == null
                ? null
                : dataPacket(entityId, SynchedEntityData.DataValue.create(frogVariantAccessor, variant));
    }

    /**
     * Resolve {@code name} (plain or namespaced) to a {@link Holder} off a dynamic registry reached through the
     * live server's {@link MinecraftServer#registryAccess() registry access} — the cat- and frog-variant registries
     * are data-driven and live only on a running server, not in {@code BuiltInRegistries}, so they cannot be reached
     * at construction the way the villager registries are. Returns {@code null} (rather than throwing) when the
     * server is not yet up, the name is unparseable, or the registry has no such entry; the caller drops a
     * {@code null} packet so the render thread never throws on a typo or a too-early call.
     */
    private static <T> @Nullable Holder<T> dynamicHolder(ResourceKey<Registry<T>> registryKey, String name) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) {
                return null;
            }
            Identifier id = name.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0
                    ? Identifier.withDefaultNamespace(name)
                    : Identifier.tryParse(name);
            if (id == null) {
                return null;
            }
            Registry<T> registry = server.registryAccess().lookupOrThrow(registryKey);
            return registry.get(id).map(reference -> (Holder<T>) reference).orElse(null);
        } catch (RuntimeException registryUnavailable) {
            // The server registries can be missing or mid-build very early in boot; a variant is cosmetic, so a
            // failed lookup falls back to the entity's default variant rather than failing the spawn.
            return null;
        }
    }

    /** Wrap one already-built {@link SynchedEntityData.DataValue} into a single-field metadata packet. */
    private static ClientboundSetEntityDataPacket dataPacket(int entityId, SynchedEntityData.DataValue<?> value) {
        return new ClientboundSetEntityDataPacket(entityId, List.of(value));
    }

    /**
     * Resolve {@code name} (plain or namespaced) to a {@link Holder} off the defaulted {@code registry}, falling
     * back to the registry default when the name is unparseable or unknown — the registry's own default holder, so
     * a typo renders the default appearance rather than failing the spawn.
     */
    private static <T> Holder<T> holder(net.minecraft.core.DefaultedRegistry<T> registry, String name) {
        Identifier id = name.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0
                ? Identifier.withDefaultNamespace(name)
                : Identifier.tryParse(name);
        if (id != null) {
            Optional<? extends Holder<T>> found = registry.get(id);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return registry.get(registry.getDefaultKey()).orElseThrow();
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
    public Object collidable(String teamName, String memberName, @Nullable NamedColor color, boolean collidable) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(memberName, "memberName");
        // The client honours the team's collision rule and outline colour for an entity whose name is on the team,
        // the same team mechanism glowColor tints through; ALWAYS collides, NEVER passes through. Both ride one
        // packet because an entity is on only one team. A throwaway scoreboard is fine — the packet copies the
        // team's parameters and member list off it.
        PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
        team.setCollisionRule(collidable ? Team.CollisionRule.ALWAYS : Team.CollisionRule.NEVER);
        if (color != null) {
            team.setColor(ChatFormatting.valueOf(color.name()));
        }
        team.getPlayers().add(memberName);
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
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
