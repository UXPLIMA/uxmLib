package com.uxplima.uxmlib.packet.npc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;

import com.uxplima.uxmlib.packet.tablist.TabSkin;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The {@link NpcPackets} port contract, exercised against a recording fake. The real packet construction is
 * compile-gated against the dev bundle and covered by the plugin smoke; this proves the port itself is
 * NMS-free, that ids are allocated monotonically, that the spawn reuses the player-info profile id (the link
 * the client needs to attach the skin), and that {@code bundle}/{@code send} behave as documented.
 */
class NpcPacketsContractTest {

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
    void bundleCopiesItsInput() {
        FakeNpcPackets packets = new FakeNpcPackets();
        List<Object> input = new ArrayList<>();
        input.add("a");

        FakeNpcPackets.Bundle bundle = (FakeNpcPackets.Bundle) packets.bundle(input);
        input.add("b");

        assertThat(bundle.packets()).containsExactly("a");
    }

    @Test
    void sendRecordsTheRecipient() {
        FakeNpcPackets packets = new FakeNpcPackets();
        Player viewer = (Player) java.lang.reflect.Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> null);

        packets.send(viewer, "packet");

        assertThat(packets.sends).containsExactly(new FakeNpcPackets.Sent(viewer, "packet"));
    }

    /** A recording fake whose packets are sentinel records so the contract can be asserted without NMS. */
    private static final class FakeNpcPackets implements NpcPackets {

        record TabAdd(UUID profileId, String name, @Nullable TabSkin skin) {}

        record TabRemove(UUID profileId) {}

        record Spawn(int entityId, UUID profileId, double x, double y, double z, float yaw, float pitch) {}

        record HeadLook(int entityId, float yaw) {}

        record BodyLook(int entityId, float yaw, float pitch) {}

        record Teleport(int entityId, double x, double y, double z, float yaw, float pitch) {}

        record Remove(int entityId) {}

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
        public Object bundle(List<Object> packets) {
            return new Bundle(List.copyOf(packets));
        }

        @Override
        public void send(Player viewer, Object packet) {
            sends.add(new Sent(viewer, packet));
        }
    }
}
