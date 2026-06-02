package com.uxplima.uxmlib.particle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * The mapping from a {@link ParticleOptions} record to its {@code particle()} + {@code data()} pair is pure,
 * so these tests assert the chosen particle and payload per kind without spawning anything. MockBukkit is
 * started only because constructing {@link ItemStack}/{@link BlockData} needs a server.
 */
class ParticleOptionsTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void plainCarriesItsParticleAndNoData() {
        ParticleOptions.Plain flame = ParticleOptions.of(Particle.FLAME);
        assertThat(flame.particle()).isEqualTo(Particle.FLAME);
        assertThat(flame.data()).isNull();
    }

    @Test
    void plainRejectsAParticleThatRequiresData() {
        assertThatIllegalArgumentException().isThrownBy(() -> ParticleOptions.of(Particle.DUST));
    }

    @Test
    void dustMapsToDustParticleWithColourAndSize() {
        ParticleOptions.Dust dust = ParticleOptions.dust(Color.RED, 1.5f);
        assertThat(dust.particle()).isEqualTo(Particle.DUST);
        Particle.DustOptions data = dust.data();
        assertThat(data.getColor()).isEqualTo(Color.RED);
        assertThat(data.getSize()).isEqualTo(1.5f);
    }

    @Test
    void dustRejectsANonPositiveSize() {
        assertThatIllegalArgumentException().isThrownBy(() -> ParticleOptions.dust(Color.RED, 0f));
        assertThatIllegalArgumentException().isThrownBy(() -> ParticleOptions.dust(Color.RED, -1f));
    }

    @Test
    void dustTransitionMapsToTransitionParticleWithBothColours() {
        ParticleOptions.DustTransition transition = ParticleOptions.dustTransition(Color.RED, Color.BLUE, 2f);
        assertThat(transition.particle()).isEqualTo(Particle.DUST_COLOR_TRANSITION);
        Particle.DustTransition data = transition.data();
        assertThat(data.getColor()).isEqualTo(Color.RED);
        assertThat(data.getToColor()).isEqualTo(Color.BLUE);
        assertThat(data.getSize()).isEqualTo(2f);
    }

    @Test
    void blockCarriesItsBlockDataAndChosenParticle() {
        BlockData stone = server.createBlockData(Material.STONE);
        ParticleOptions.Block block = ParticleOptions.block(Particle.BLOCK, stone);
        assertThat(block.particle()).isEqualTo(Particle.BLOCK);
        assertThat(block.data()).isEqualTo(stone);
    }

    @Test
    void blockRejectsAParticleThatDoesNotTakeBlockData() {
        BlockData stone = server.createBlockData(Material.STONE);
        assertThatIllegalArgumentException().isThrownBy(() -> ParticleOptions.block(Particle.FLAME, stone));
    }

    @Test
    void itemMapsToItemParticleWithTheStack() {
        ItemStack apple = new ItemStack(Material.APPLE);
        ParticleOptions.Item item = ParticleOptions.item(apple);
        assertThat(item.particle()).isEqualTo(Particle.ITEM);
        assertThat(item.data()).isEqualTo(apple);
    }
}
