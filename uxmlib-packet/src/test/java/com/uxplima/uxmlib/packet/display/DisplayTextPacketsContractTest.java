package com.uxplima.uxmlib.packet.display;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

/**
 * The {@link DisplayTextPackets} port contract, exercised against a recording fake. The real
 * {@code ClientboundSetEntityDataPacket} construction is compile-gated against the dev bundle and covered by
 * the plugin smoke; this proves the port itself is NMS-free, that an override packet carries the text for the
 * entity id it targets, and that the same packet can be sent to a chosen viewer (the per-viewer override).
 */
class DisplayTextPacketsContractTest {

    @Test
    void overrideCarriesTheTextForTheEntityId() {
        FakeDisplayTextPackets packets = new FakeDisplayTextPackets();
        Component text = Component.text("hello");

        FakeDisplayTextPackets.TextOverride built =
                (FakeDisplayTextPackets.TextOverride) packets.textOverride(42, text);

        assertThat(built.entityId()).isEqualTo(42);
        assertThat(built.text()).isEqualTo(text);
    }

    @Test
    void sendRecordsTheRecipient() {
        FakeDisplayTextPackets packets = new FakeDisplayTextPackets();
        Player viewer = viewer();
        Object packet = packets.textOverride(7, Component.text("x"));

        packets.send(viewer, packet);

        assertThat(packets.sends).containsExactly(new FakeDisplayTextPackets.Sent(viewer, packet));
    }

    @Test
    void onePerViewerOverrideTargetsTheChosenViewerOnly() {
        // Two viewers of one shared entity each get their own text: the override is per-recipient, not broadcast.
        FakeDisplayTextPackets packets = new FakeDisplayTextPackets();
        Player alice = viewer();
        Player bob = viewer();

        packets.send(alice, packets.textOverride(9, Component.text("for alice")));
        packets.send(bob, packets.textOverride(9, Component.text("for bob")));

        assertThat(packets.sends).hasSize(2);
        assertThat(packets.sends.get(0).viewer()).isSameAs(alice);
        assertThat(((FakeDisplayTextPackets.TextOverride) packets.sends.get(0).packet()).text())
                .isEqualTo(Component.text("for alice"));
        assertThat(packets.sends.get(1).viewer()).isSameAs(bob);
        assertThat(((FakeDisplayTextPackets.TextOverride) packets.sends.get(1).packet()).text())
                .isEqualTo(Component.text("for bob"));
    }

    private static Player viewer() {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> null);
    }

    /** A recording fake whose override packet is a sentinel record so the contract can be asserted without NMS. */
    private static final class FakeDisplayTextPackets implements DisplayTextPackets {

        record TextOverride(int entityId, Component text) {}

        record Sent(Player viewer, Object packet) {}

        private final List<Sent> sends = new ArrayList<>();

        @Override
        public Object textOverride(int entityId, Component text) {
            return new TextOverride(entityId, text);
        }

        @Override
        public void send(Player viewer, Object packet) {
            sends.add(new Sent(viewer, packet));
        }
    }
}
