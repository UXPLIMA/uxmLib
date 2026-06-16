package com.uxplima.uxmlib.packet.npc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.packet.tablist.TabSkin;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.inventory.ItemStackMock;

/**
 * The {@link NpcPackets} port contract, exercised against a recording fake. The real packet construction is
 * compile-gated against the dev bundle and covered by the plugin smoke; this proves the port itself is
 * NMS-free, that ids are allocated monotonically, that the spawn reuses the player-info profile id (the link
 * the client needs to attach the skin), and that {@code bundle}/{@code send} behave as documented.
 */
class NpcPacketsContractTest {

    /**
     * The pose names {@code net.minecraft.world.entity.Pose} declares — the by-name target for {@link NpcPose}.
     * The list is fixed by the protocol, so pinning it here is stable; it proves every {@code NpcPose} server name
     * has a real {@code Pose} counterpart so the NMS {@code Pose.valueOf} never throws at render time.
     */
    private static final java.util.Set<String> SERVER_POSE_NAMES = java.util.Set.of(
            "STANDING",
            "FALL_FLYING",
            "SLEEPING",
            "SWIMMING",
            "SPIN_ATTACK",
            "CROUCHING",
            "LONG_JUMPING",
            "DYING",
            "CROAKING",
            "USING_TONGUE",
            "SITTING",
            "ROARING",
            "SNIFFING",
            "EMERGING",
            "DIGGING",
            "SLIDING",
            "SHOOTING",
            "INHALING");

    /** The 16 colour names {@code net.minecraft.ChatFormatting} declares — the by-name target for {@link NamedColor}. */
    private static final java.util.Set<String> CHAT_FORMATTING_COLORS = java.util.Set.of(
            "BLACK",
            "DARK_BLUE",
            "DARK_GREEN",
            "DARK_AQUA",
            "DARK_RED",
            "DARK_PURPLE",
            "GOLD",
            "GRAY",
            "DARK_GRAY",
            "BLUE",
            "GREEN",
            "AQUA",
            "RED",
            "LIGHT_PURPLE",
            "YELLOW",
            "WHITE");

    @BeforeEach
    void setUp() {
        // A mocked server is needed only to mint a real ItemStack for the equipment contract; the rest is pure.
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void allocatesMonotonicIds() {
        FakeNpcPackets packets = new FakeNpcPackets();

        assertThat(packets.allocateEntityId()).isEqualTo(1);
        assertThat(packets.allocateEntityId()).isEqualTo(2);
    }

    @Test
    void tabAddCarriesNameAndSkin() {
        FakeNpcPackets packets = new FakeNpcPackets();
        UUID profileId = UUID.randomUUID();
        TabSkin skin = TabSkin.unsigned("tex");

        FakeNpcPackets.TabAdd add = (FakeNpcPackets.TabAdd) packets.tabAdd(profileId, "Guide", skin);

        assertThat(add.profileId()).isEqualTo(profileId);
        assertThat(add.name()).isEqualTo("Guide");
        assertThat(add.skin()).isEqualTo(skin);
    }

    @Test
    void spawnReusesTheProfileIdSoTheSkinLinks() {
        FakeNpcPackets packets = new FakeNpcPackets();
        UUID profileId = UUID.randomUUID();
        int entityId = packets.allocateEntityId();

        FakeNpcPackets.Spawn spawn =
                (FakeNpcPackets.Spawn) packets.spawnPlayer(entityId, profileId, 1.0, 2.0, 3.0, 90.0f, 0.0f);

        assertThat(spawn.entityId()).isEqualTo(entityId);
        assertThat(spawn.profileId()).isEqualTo(profileId);
        assertThat(spawn.yaw()).isEqualTo(90.0f);
    }

    @Test
    void spawnEntityCarriesTheTypeKeyAndEntityUuid() {
        FakeNpcPackets packets = new FakeNpcPackets();
        UUID entityUuid = UUID.randomUUID();
        int entityId = packets.allocateEntityId();

        FakeNpcPackets.SpawnEntity spawn = (FakeNpcPackets.SpawnEntity)
                packets.spawnEntity(entityId, entityUuid, "minecraft:villager", 1.0, 2.0, 3.0, 90.0f, 0.0f);

        assertThat(spawn.entityId()).isEqualTo(entityId);
        assertThat(spawn.entityUuid()).isEqualTo(entityUuid);
        assertThat(spawn.entityTypeKey()).isEqualTo("minecraft:villager");
        assertThat(spawn.yaw()).isEqualTo(90.0f);
    }

    @Test
    void bundleCopiesItsInput() {
        FakeNpcPackets packets = new FakeNpcPackets();
        List<Object> input = new ArrayList<>();
        input.add("a");

        FakeNpcPackets.Bundle bundle = (FakeNpcPackets.Bundle) packets.bundle(input);
        input.add("b");

        assertThat(bundle.packets()).containsExactly("a");
    }

    @Test
    void equipmentCarriesEachSlotItem() {
        FakeNpcPackets packets = new FakeNpcPackets();
        int entityId = packets.allocateEntityId();
        ItemStack item = item();
        Map<EquipmentSlot, ItemStack> items = Map.of(EquipmentSlot.HEAD, item);

        FakeNpcPackets.Equipment equipment = (FakeNpcPackets.Equipment) packets.equipment(entityId, items);

        assertThat(equipment.entityId()).isEqualTo(entityId);
        assertThat(equipment.items()).containsEntry(EquipmentSlot.HEAD, item);
    }

    @Test
    void glowCarriesTheToggle() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.Glow on = (FakeNpcPackets.Glow) packets.glow(7, true);
        FakeNpcPackets.Glow off = (FakeNpcPackets.Glow) packets.glow(7, false);

        assertThat(on.glowing()).isTrue();
        assertThat(off.glowing()).isFalse();
    }

    @Test
    void sharedFlagsComposeOnFireGlowAndInvisibleIntoOnePacket() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.SharedFlags flags = (FakeNpcPackets.SharedFlags) packets.sharedFlags(7, true, false, true);

        assertThat(flags.entityId()).isEqualTo(7);
        assertThat(flags.onFire()).isTrue();
        assertThat(flags.glowing()).isFalse();
        assertThat(flags.invisible()).isTrue();
    }

    @Test
    void silentCarriesTheEntityAndToggle() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.Silent on = (FakeNpcPackets.Silent) packets.silent(7, true);
        FakeNpcPackets.Silent off = (FakeNpcPackets.Silent) packets.silent(7, false);

        assertThat(on.entityId()).isEqualTo(7);
        assertThat(on.silent()).isTrue();
        assertThat(off.silent()).isFalse();
    }

    @Test
    void collidableCarriesTheTeamMemberColourAndRule() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.Collidable on =
                (FakeNpcPackets.Collidable) packets.collidable("uxmnpc_guide", "guide", NamedColor.RED, true);
        FakeNpcPackets.Collidable off =
                (FakeNpcPackets.Collidable) packets.collidable("uxmnpc_guide", "guide", null, false);

        assertThat(on.teamName()).isEqualTo("uxmnpc_guide");
        assertThat(on.memberName()).isEqualTo("guide");
        assertThat(on.color()).isEqualTo(NamedColor.RED);
        assertThat(on.collidable()).isTrue();
        assertThat(off.color()).isNull();
        assertThat(off.collidable()).isFalse();
    }

    @Test
    void poseCarriesTheEntityAndPose() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.PoseSet posed = (FakeNpcPackets.PoseSet) packets.pose(7, NpcPose.SITTING);

        assertThat(posed.entityId()).isEqualTo(7);
        assertThat(posed.pose()).isEqualTo(NpcPose.SITTING);
    }

    @Test
    void scaleCarriesTheEntityAndScale() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.Scale scaled = (FakeNpcPackets.Scale) packets.scale(7, 2.5);

        assertThat(scaled.entityId()).isEqualTo(7);
        assertThat(scaled.scale()).isEqualTo(2.5);
    }

    @Test
    void babyCarriesTheEntityAndToggle() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.Baby on = (FakeNpcPackets.Baby) packets.baby(7, true);
        FakeNpcPackets.Baby off = (FakeNpcPackets.Baby) packets.baby(7, false);

        assertThat(on.entityId()).isEqualTo(7);
        assertThat(on.baby()).isTrue();
        assertThat(off.baby()).isFalse();
    }

    @Test
    void monsterFamilyBabiesAreSeparateBuildersFromTheAgeableOne() {
        // The zombie line, piglins, and zoglins extend Monster, not AgeableMob, so their baby flag lives at a
        // different data index; each gets its own builder so a value never lands on the wrong field.
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.ZombieBaby zombie = (FakeNpcPackets.ZombieBaby) packets.zombieBaby(7, true);
        FakeNpcPackets.PiglinBaby piglin = (FakeNpcPackets.PiglinBaby) packets.piglinBaby(8, true);
        FakeNpcPackets.ZoglinBaby zoglin = (FakeNpcPackets.ZoglinBaby) packets.zoglinBaby(9, false);

        assertThat(zombie.entityId()).isEqualTo(7);
        assertThat(zombie.baby()).isTrue();
        assertThat(piglin.entityId()).isEqualTo(8);
        assertThat(piglin.baby()).isTrue();
        assertThat(zoglin.entityId()).isEqualTo(9);
        assertThat(zoglin.baby()).isFalse();
    }

    @Test
    void villagerDataCarriesTypeProfessionAndLevel() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.VillagerData data =
                (FakeNpcPackets.VillagerData) packets.villagerData(7, "desert", "librarian", 3);

        assertThat(data.entityId()).isEqualTo(7);
        assertThat(data.type()).isEqualTo("desert");
        assertThat(data.profession()).isEqualTo("librarian");
        assertThat(data.level()).isEqualTo(3);
    }

    @Test
    void slimeSizeCarriesTheEntityAndSize() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.SlimeSize sized = (FakeNpcPackets.SlimeSize) packets.slimeSize(7, 4);

        assertThat(sized.entityId()).isEqualTo(7);
        assertThat(sized.size()).isEqualTo(4);
    }

    @Test
    void chargedCarriesTheEntityAndToggle() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.Charged on = (FakeNpcPackets.Charged) packets.charged(7, true);
        FakeNpcPackets.Charged off = (FakeNpcPackets.Charged) packets.charged(7, false);

        assertThat(on.entityId()).isEqualTo(7);
        assertThat(on.charged()).isTrue();
        assertThat(off.charged()).isFalse();
    }

    @Test
    void horseVariantCarriesTheEntityAndTheColourAndMarkings() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.HorseVariant variant = (FakeNpcPackets.HorseVariant) packets.horseVariant(7, 3, 2);

        assertThat(variant.entityId()).isEqualTo(7);
        assertThat(variant.color()).isEqualTo(3);
        assertThat(variant.markings()).isEqualTo(2);
    }

    @Test
    void llamaVariantCarriesTheEntityAndVariant() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.LlamaVariant variant = (FakeNpcPackets.LlamaVariant) packets.llamaVariant(7, 2);

        assertThat(variant.entityId()).isEqualTo(7);
        assertThat(variant.variant()).isEqualTo(2);
    }

    @Test
    void sheepColorCarriesTheEntityAndColour() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.SheepColor color = (FakeNpcPackets.SheepColor) packets.sheepColor(7, 14);

        assertThat(color.entityId()).isEqualTo(7);
        assertThat(color.color()).isEqualTo(14);
    }

    @Test
    void parrotVariantCarriesTheEntityAndVariant() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.ParrotVariant variant = (FakeNpcPackets.ParrotVariant) packets.parrotVariant(7, 4);

        assertThat(variant.entityId()).isEqualTo(7);
        assertThat(variant.variant()).isEqualTo(4);
    }

    @Test
    void axolotlVariantCarriesTheEntityAndVariant() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.AxolotlVariant variant = (FakeNpcPackets.AxolotlVariant) packets.axolotlVariant(7, 3);

        assertThat(variant.entityId()).isEqualTo(7);
        assertThat(variant.variant()).isEqualTo(3);
    }

    @Test
    void foxTypeCarriesTheEntityAndType() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.FoxType fox = (FakeNpcPackets.FoxType) packets.foxType(7, 1);

        assertThat(fox.entityId()).isEqualTo(7);
        assertThat(fox.type()).isEqualTo(1);
    }

    @Test
    void rabbitTypeCarriesTheEntityAndTypeIncludingTheKillerVariant() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.RabbitType rabbit = (FakeNpcPackets.RabbitType) packets.rabbitType(7, 99);

        assertThat(rabbit.entityId()).isEqualTo(7);
        // 99 is the killer (toast) rabbit on the wire, carried straight through as the raw type id.
        assertThat(rabbit.type()).isEqualTo(99);
    }

    @Test
    void catVariantCarriesTheEntityAndName() {
        // Cat and frog variants are dynamic-registry values resolved by name; the NMS impl reaches the live
        // server registry, so the port carries only the name and the contract proves it round-trips through.
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.CatVariant cat = (FakeNpcPackets.CatVariant) packets.catVariant(7, "calico");

        assertThat(cat.entityId()).isEqualTo(7);
        assertThat(cat.name()).isEqualTo("calico");
    }

    @Test
    void frogVariantCarriesTheEntityAndName() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.FrogVariant frog = (FakeNpcPackets.FrogVariant) packets.frogVariant(7, "warm");

        assertThat(frog.entityId()).isEqualTo(7);
        assertThat(frog.name()).isEqualTo("warm");
    }

    @Test
    void everyNpcPoseNamesAServerPose() {
        // The NMS impl resolves a Pose by the constant's server name; this proves each NpcPose has a counterpart so
        // the by-name Pose.valueOf never throws at render time. GLIDING maps to the server's FALL_FLYING.
        for (NpcPose pose : NpcPose.values()) {
            assertThat(SERVER_POSE_NAMES).contains(pose.serverName());
        }
        assertThat(NpcPose.GLIDING.serverName()).isEqualTo("FALL_FLYING");
        assertThat(NpcPose.STANDING.serverName()).isEqualTo("STANDING");
    }

    @Test
    void glowColorCarriesTheTeamMemberAndColor() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.GlowColor colored =
                (FakeNpcPackets.GlowColor) packets.glowColor("uxmnpc_guide", "guide", NamedColor.RED);

        assertThat(colored.teamName()).isEqualTo("uxmnpc_guide");
        assertThat(colored.memberName()).isEqualTo("guide");
        assertThat(colored.color()).isEqualTo(NamedColor.RED);
    }

    @Test
    void glowColorRemoveCarriesTheTeamName() {
        FakeNpcPackets packets = new FakeNpcPackets();

        FakeNpcPackets.GlowColorRemove removed = (FakeNpcPackets.GlowColorRemove) packets.glowColorRemove("guide");

        assertThat(removed.teamName()).isEqualTo("guide");
    }

    @Test
    void everyNamedColorNamesAChatFormatting() {
        // The NMS impl maps a NamedColor onto ChatFormatting by name; this proves each constant has a counterpart
        // so the by-name mapping never throws at render time. The 16 ChatFormatting colour names are fixed by the
        // protocol, so pinning them here is stable.
        for (NamedColor color : NamedColor.values()) {
            assertThat(CHAT_FORMATTING_COLORS).contains(color.name());
        }
    }

    @Test
    void sendRecordsTheRecipient() {
        FakeNpcPackets packets = new FakeNpcPackets();
        Player viewer = (Player) java.lang.reflect.Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> null);

        packets.send(viewer, "packet");

        assertThat(packets.sends).containsExactly(new FakeNpcPackets.Sent(viewer, "packet"));
    }

    /** A real (mocked) {@link ItemStack} the equipment contract carries through the fake. */
    private static ItemStack item() {
        return new ItemStackMock(Material.DIAMOND_HELMET);
    }

    /** A recording fake whose packets are sentinel records so the contract can be asserted without NMS. */
    private static final class FakeNpcPackets implements NpcPackets {

        record TabAdd(UUID profileId, String name, @Nullable TabSkin skin) {}

        record TabRemove(UUID profileId) {}

        record Spawn(int entityId, UUID profileId, double x, double y, double z, float yaw, float pitch) {}

        record SpawnEntity(
                int entityId,
                UUID entityUuid,
                String entityTypeKey,
                double x,
                double y,
                double z,
                float yaw,
                float pitch) {}

        record HeadLook(int entityId, float yaw) {}

        record BodyLook(int entityId, float yaw, float pitch) {}

        record Teleport(int entityId, double x, double y, double z, float yaw, float pitch) {}

        record Remove(int entityId) {}

        record Equipment(int entityId, Map<EquipmentSlot, ItemStack> items) {}

        record Glow(int entityId, boolean glowing) {}

        record SharedFlags(int entityId, boolean onFire, boolean glowing, boolean invisible) {}

        record Silent(int entityId, boolean silent) {}

        record Collidable(String teamName, String memberName, @Nullable NamedColor color, boolean collidable) {}

        record PoseSet(int entityId, NpcPose pose) {}

        record Scale(int entityId, double scale) {}

        record Baby(int entityId, boolean baby) {}

        record ZombieBaby(int entityId, boolean baby) {}

        record PiglinBaby(int entityId, boolean baby) {}

        record ZoglinBaby(int entityId, boolean baby) {}

        record VillagerData(int entityId, String type, String profession, int level) {}

        record SlimeSize(int entityId, int size) {}

        record Charged(int entityId, boolean charged) {}

        record HorseVariant(int entityId, int color, int markings) {}

        record LlamaVariant(int entityId, int variant) {}

        record SheepColor(int entityId, int color) {}

        record ParrotVariant(int entityId, int variant) {}

        record AxolotlVariant(int entityId, int variant) {}

        record FoxType(int entityId, int type) {}

        record RabbitType(int entityId, int type) {}

        record CatVariant(int entityId, String name) {}

        record FrogVariant(int entityId, String name) {}

        record GlowColor(String teamName, String memberName, @Nullable NamedColor color) {}

        record GlowColorRemove(String teamName) {}

        record Bundle(List<Object> packets) {}

        record Sent(Player viewer, Object packet) {}

        private final AtomicInteger nextId = new AtomicInteger(1);
        private final List<Sent> sends = new ArrayList<>();

        @Override
        public int allocateEntityId() {
            return nextId.getAndIncrement();
        }

        @Override
        public Object tabAdd(UUID profileId, String name, @Nullable TabSkin skin) {
            return new TabAdd(profileId, name, skin);
        }

        @Override
        public Object tabRemove(UUID profileId) {
            return new TabRemove(profileId);
        }

        @Override
        public Object spawnPlayer(int entityId, UUID profileId, double x, double y, double z, float yaw, float pitch) {
            return new Spawn(entityId, profileId, x, y, z, yaw, pitch);
        }

        @Override
        public Object spawnEntity(
                int entityId,
                UUID entityUuid,
                String entityTypeKey,
                double x,
                double y,
                double z,
                float yaw,
                float pitch) {
            return new SpawnEntity(entityId, entityUuid, entityTypeKey, x, y, z, yaw, pitch);
        }

        @Override
        public Object headLook(int entityId, float yaw) {
            return new HeadLook(entityId, yaw);
        }

        @Override
        public Object bodyLook(int entityId, float yaw, float pitch) {
            return new BodyLook(entityId, yaw, pitch);
        }

        @Override
        public Object teleport(int entityId, double x, double y, double z, float yaw, float pitch) {
            return new Teleport(entityId, x, y, z, yaw, pitch);
        }

        @Override
        public Object remove(int entityId) {
            return new Remove(entityId);
        }

        @Override
        public Object equipment(int entityId, Map<EquipmentSlot, ItemStack> items) {
            return new Equipment(entityId, new LinkedHashMap<>(items));
        }

        @Override
        public Object glow(int entityId, boolean glowing) {
            return new Glow(entityId, glowing);
        }

        @Override
        public Object sharedFlags(int entityId, boolean onFire, boolean glowing, boolean invisible) {
            return new SharedFlags(entityId, onFire, glowing, invisible);
        }

        @Override
        public Object silent(int entityId, boolean silent) {
            return new Silent(entityId, silent);
        }

        @Override
        public Object collidable(String teamName, String memberName, @Nullable NamedColor color, boolean collidable) {
            return new Collidable(teamName, memberName, color, collidable);
        }

        @Override
        public Object pose(int entityId, NpcPose pose) {
            return new PoseSet(entityId, pose);
        }

        @Override
        public Object scale(int entityId, double scale) {
            return new Scale(entityId, scale);
        }

        @Override
        public Object baby(int entityId, boolean baby) {
            return new Baby(entityId, baby);
        }

        @Override
        public Object zombieBaby(int entityId, boolean baby) {
            return new ZombieBaby(entityId, baby);
        }

        @Override
        public Object piglinBaby(int entityId, boolean baby) {
            return new PiglinBaby(entityId, baby);
        }

        @Override
        public Object zoglinBaby(int entityId, boolean baby) {
            return new ZoglinBaby(entityId, baby);
        }

        @Override
        public Object villagerData(int entityId, String type, String profession, int level) {
            return new VillagerData(entityId, type, profession, level);
        }

        @Override
        public Object slimeSize(int entityId, int size) {
            return new SlimeSize(entityId, size);
        }

        @Override
        public Object charged(int entityId, boolean charged) {
            return new Charged(entityId, charged);
        }

        @Override
        public Object horseVariant(int entityId, int color, int markings) {
            return new HorseVariant(entityId, color, markings);
        }

        @Override
        public Object llamaVariant(int entityId, int variant) {
            return new LlamaVariant(entityId, variant);
        }

        @Override
        public Object sheepColor(int entityId, int color) {
            return new SheepColor(entityId, color);
        }

        @Override
        public Object parrotVariant(int entityId, int variant) {
            return new ParrotVariant(entityId, variant);
        }

        @Override
        public Object axolotlVariant(int entityId, int variant) {
            return new AxolotlVariant(entityId, variant);
        }

        @Override
        public Object foxType(int entityId, int type) {
            return new FoxType(entityId, type);
        }

        @Override
        public Object rabbitType(int entityId, int type) {
            return new RabbitType(entityId, type);
        }

        @Override
        public Object catVariant(int entityId, String name) {
            return new CatVariant(entityId, name);
        }

        @Override
        public Object frogVariant(int entityId, String name) {
            return new FrogVariant(entityId, name);
        }

        @Override
        public Object glowColor(String teamName, String memberName, @Nullable NamedColor color) {
            return new GlowColor(teamName, memberName, color);
        }

        @Override
        public Object glowColorRemove(String teamName) {
            return new GlowColorRemove(teamName);
        }

        @Override
        public Object bundle(List<Object> packets) {
            return new Bundle(List.copyOf(packets));
        }

        @Override
        public void send(Player viewer, Object packet) {
            sends.add(new Sent(viewer, packet));
        }
    }
}
