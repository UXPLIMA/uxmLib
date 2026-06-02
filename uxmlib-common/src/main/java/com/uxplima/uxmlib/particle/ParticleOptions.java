package com.uxplima.uxmlib.particle;

import java.util.Objects;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * A particle plus exactly the extra data that particle requires, as a sealed set of records. Modelling the
 * payload per kind means the compiler rejects "DUST without a colour" or "FLAME with a block" at the call
 * site, replacing CMILib-style untyped {@code Object} casts. {@link #particle()} names the particle and
 * {@link #data()} is the payload handed to {@code spawnParticle} (or {@code null} when the particle takes
 * none); {@link Particles} reads these two to pick the matching overload.
 */
public sealed interface ParticleOptions
        permits ParticleOptions.Plain,
                ParticleOptions.Dust,
                ParticleOptions.DustTransition,
                ParticleOptions.Block,
                ParticleOptions.Item {

    /** The particle to spawn. */
    Particle particle();

    /** The extra data {@code spawnParticle} needs for this particle, or {@code null} if it takes none. */
    @Nullable Object data();

    /** A particle that carries no extra data, such as {@link Particle#FLAME} or {@link Particle#HEART}. */
    static Plain of(Particle particle) {
        return new Plain(particle);
    }

    /** Coloured {@link Particle#DUST} of the given {@link Color} and size (1.0 is the default scale). */
    static Dust dust(Color color, float size) {
        return new Dust(color, size);
    }

    /** {@link Particle#DUST_COLOR_TRANSITION} fading from one colour to another at the given size. */
    static DustTransition dustTransition(Color from, Color to, float size) {
        return new DustTransition(from, to, size);
    }

    /** A block-textured particle (e.g. {@link Particle#BLOCK}, {@code BLOCK_MARKER}, {@code FALLING_DUST}). */
    static Block block(Particle particle, BlockData blockData) {
        return new Block(particle, blockData);
    }

    /** An item-textured particle ({@link Particle#ITEM}). */
    static Item item(ItemStack item) {
        return new Item(item);
    }

    /** A particle that needs no extra data. */
    record Plain(Particle particle) implements ParticleOptions {
        public Plain {
            Objects.requireNonNull(particle, "particle");
            ParticleData.requireDataType(particle, Void.class);
        }

        @Override
        public @Nullable Object data() {
            return null;
        }
    }

    /** {@link Particle#DUST} with a colour and a size. */
    record Dust(Color color, float size) implements ParticleOptions {
        public Dust {
            Objects.requireNonNull(color, "color");
            ParticleData.requirePositiveSize(size);
        }

        @Override
        public Particle particle() {
            return Particle.DUST;
        }

        @Override
        public Particle.DustOptions data() {
            return new Particle.DustOptions(color, size);
        }
    }

    /** {@link Particle#DUST_COLOR_TRANSITION} fading from one colour to another. */
    record DustTransition(Color from, Color to, float size) implements ParticleOptions {
        public DustTransition {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            ParticleData.requirePositiveSize(size);
        }

        @Override
        public Particle particle() {
            return Particle.DUST_COLOR_TRANSITION;
        }

        @Override
        public Particle.DustTransition data() {
            return new Particle.DustTransition(from, to, size);
        }
    }

    /** A block-data particle; the explicit {@code particle} lets one record serve every block-textured kind. */
    record Block(Particle particle, BlockData blockData) implements ParticleOptions {
        public Block {
            Objects.requireNonNull(particle, "particle");
            Objects.requireNonNull(blockData, "blockData");
            ParticleData.requireDataType(particle, BlockData.class);
        }

        @Override
        public BlockData data() {
            return blockData;
        }
    }

    /** An {@link Particle#ITEM} particle textured from an {@link ItemStack}. */
    record Item(ItemStack item) implements ParticleOptions {
        public Item {
            Objects.requireNonNull(item, "item");
        }

        @Override
        public Particle particle() {
            return Particle.ITEM;
        }

        @Override
        public ItemStack data() {
            return item;
        }
    }
}
