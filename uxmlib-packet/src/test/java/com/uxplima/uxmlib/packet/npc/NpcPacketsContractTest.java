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
