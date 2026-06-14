package com.uxplima.uxmlib.nametag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Drives the renderer against a recording {@link FakeNametagPackets} and a controllable {@link FakeScheduler}.
 * No NMS is involved: the port returns sentinel records, so the test asserts exactly which packet structure
 * reached which viewer and that the refresh task is wired and cancelled.
 */
class NametagRendererTest {

    private ServerMock server;
    private FakeNametagPackets packets;
    private FakeScheduler scheduler;
    private NametagRenderer renderer;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        packets = new FakeNametagPackets();
        scheduler = new FakeScheduler();
        renderer = new NametagRenderer(packets, scheduler);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThatNullPointerException().isThrownBy(() -> new NametagRenderer(nullPackets(), scheduler));
        assertThatNullPointerException().isThrownBy(() -> new NametagRenderer(packets, nullScheduler()));
    }

    @Test
    void showAllocatesOneIdAndSendsASpawnBundleToEachOnlineViewer() {
        Player target = server.addPlayer("Target");
        Player a = server.addPlayer("Alice");
        Player b = server.addPlayer("Bob");

        renderer.show(target, Appearance.defaults(), Set.of(a.getUniqueId(), b.getUniqueId()), v -> line("x"));

        assertThat(packets.allocations()).isEqualTo(1);
        assertThat(bundlesTo(a)).hasSize(1);
        assertThat(bundlesTo(b)).hasSize(1);
        FakeNametagPackets.Bundle bundle = bundlesTo(a).get(0);
        assertThat(bundle.packets())
                .hasSize(3)
                .hasOnlyElementsOfTypes(
                        FakeNametagPackets.Spawn.class,
                        FakeNametagPackets.Metadata.class,
                        FakeNametagPackets.Mount.class);
    }

    @Test
    void offlineViewerInTheSetIsSkippedWithNoSendAndNoThrow() {
        Player target = server.addPlayer("Target");
        Player online = server.addPlayer("Online");
        UUID offline = UUID.randomUUID();

        assertThatCode(() -> renderer.show(
                        target, Appearance.defaults(), Set.of(online.getUniqueId(), offline), v -> line("x")))
                .doesNotThrowAnyException();

        assertThat(packets.sends).allMatch(s -> s.viewer().getUniqueId().equals(online.getUniqueId()));
        assertThat(bundlesTo(online)).hasSize(1);
    }

    @Test
    void eachViewerSeesItsOwnComponentInItsMetadataPacket() {
        Player target = server.addPlayer("Target");
        Player a = server.addPlayer("Alice");
        Player b = server.addPlayer("Bob");
        PerViewerText perViewer = viewer -> List.of(Component.text("hi " + viewer));

        renderer.show(target, Appearance.defaults(), Set.of(a.getUniqueId(), b.getUniqueId()), perViewer);

        assertThat(metadataTextTo(a)).isEqualTo(Component.text("hi " + a.getUniqueId()));
        assertThat(metadataTextTo(b)).isEqualTo(Component.text("hi " + b.getUniqueId()));
    }

    @Test
    void metadataPacketCarriesFullOpacity() {
        Player target = server.addPlayer("Target");
        Player a = server.addPlayer("Alice");

        renderer.show(target, Appearance.defaults(), Set.of(a.getUniqueId()), v -> line("x"));

        assertThat(metadataTo(a).opacity()).isEqualTo(Appearance.FULL_OPACITY);
    }

    @Test
    void updateSpawnsNewViewerRemovesDepartedAndRefreshesRemaining() {
        Player target = server.addPlayer("Target");
        Player stay = server.addPlayer("Stay");
        Player leave = server.addPlayer("Leave");
        Player join = server.addPlayer("Join");
        NametagHandle handle = renderer.show(
                target, Appearance.defaults(), Set.of(stay.getUniqueId(), leave.getUniqueId()), v -> line("x"));
        packets.sends.clear();

        handle.update(Set.of(stay.getUniqueId(), join.getUniqueId()), v -> line("y"), Appearance.defaults());

        // newcomer gets a full spawn bundle
        assertThat(bundlesTo(join)).hasSize(1);
        // departed viewer gets a remove packet
        assertThat(removesTo(leave)).hasSize(1);
        // remaining viewer gets a metadata refresh, no second spawn
        assertThat(bundlesTo(stay)).isEmpty();
        assertThat(metadataPacketsTo(stay)).hasSize(1);
    }

    @Test
    void entityIdIsStableAcrossUpdates() {
        Player target = server.addPlayer("Target");
        Player a = server.addPlayer("Alice");
        renderer.show(target, Appearance.defaults(), Set.of(a.getUniqueId()), v -> line("x"));
        int spawnId = ((FakeNametagPackets.Spawn) bundlesTo(a).get(0).packets().get(0)).entityId();
        packets.sends.clear();

        scheduler.tick();

        // The refresh sends a standalone metadata packet (a is a remaining viewer), not a fresh spawn bundle.
        assertThat(packets.allocations()).isEqualTo(1);
        assertThat(metadataPacketsTo(a)).hasSize(1);
        assertThat(metadataPacketsTo(a).get(0).entityId()).isEqualTo(spawnId);
    }

    @Test
    void refreshTaskTargetsTheNametagOwner() {
        Player target = server.addPlayer("Target");
        renderer.show(target, Appearance.defaults(), Set.of(), v -> line("x"));

        assertThat(scheduler.hasTimer()).isTrue();
        assertThat(scheduler.timerEntity()).isSameAs(target);
    }

    @Test
    void removeSendsARemovePacketToEveryViewerAndCancelsTheRefreshTask() {
        Player target = server.addPlayer("Target");
        Player a = server.addPlayer("Alice");
        Player b = server.addPlayer("Bob");
        NametagHandle handle =
                renderer.show(target, Appearance.defaults(), Set.of(a.getUniqueId(), b.getUniqueId()), v -> line("x"));
        packets.sends.clear();

        handle.remove();

        assertThat(removesTo(a)).hasSize(1);
        assertThat(removesTo(b)).hasSize(1);
        assertThat(scheduler.cancelled()).isTrue();
    }

    @Test
    void removeIsIdempotent() {
        Player target = server.addPlayer("Target");
        Player a = server.addPlayer("Alice");
        NametagHandle handle = renderer.show(target, Appearance.defaults(), Set.of(a.getUniqueId()), v -> line("x"));
        handle.remove();
        packets.sends.clear();

        assertThatCode(handle::remove).doesNotThrowAnyException();
        assertThat(packets.sends).isEmpty();
    }

    // --- helpers -----------------------------------------------------------------------------------------

    private static List<Component> line(String text) {
        return List.of(Component.text(text));
    }

    private List<FakeNametagPackets.Bundle> bundlesTo(Player viewer) {
        return packets.sends.stream()
                .filter(s -> s.viewer().getUniqueId().equals(viewer.getUniqueId()))
                .map(FakeNametagPackets.Sent::packet)
                .filter(FakeNametagPackets.Bundle.class::isInstance)
                .map(FakeNametagPackets.Bundle.class::cast)
                .toList();
    }

    private List<FakeNametagPackets.Remove> removesTo(Player viewer) {
        return packets.sends.stream()
                .filter(s -> s.viewer().getUniqueId().equals(viewer.getUniqueId()))
                .map(FakeNametagPackets.Sent::packet)
                .filter(FakeNametagPackets.Remove.class::isInstance)
                .map(FakeNametagPackets.Remove.class::cast)
                .toList();
    }

    private List<FakeNametagPackets.Metadata> metadataPacketsTo(Player viewer) {
        return packets.sends.stream()
                .filter(s -> s.viewer().getUniqueId().equals(viewer.getUniqueId()))
                .map(FakeNametagPackets.Sent::packet)
                .filter(FakeNametagPackets.Metadata.class::isInstance)
                .map(FakeNametagPackets.Metadata.class::cast)
                .toList();
    }

    private FakeNametagPackets.Metadata metadataTo(Player viewer) {
        // Pull the metadata packet out of the spawn bundle this viewer received.
        FakeNametagPackets.Bundle bundle = bundlesTo(viewer).get(0);
        return bundle.packets().stream()
                .filter(FakeNametagPackets.Metadata.class::isInstance)
                .map(FakeNametagPackets.Metadata.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private Component metadataTextTo(Player viewer) {
        return metadataTo(viewer).text();
    }

    @SuppressWarnings("NullAway") // intentionally feeds null to assert the constructor guard fires.
    private static NametagPackets nullPackets() {
        return null;
    }

    @SuppressWarnings("NullAway") // intentionally feeds null to assert the constructor guard fires.
    private static com.uxplima.uxmlib.scheduler.Scheduler nullScheduler() {
        return null;
    }
}
