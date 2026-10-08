package com.eotv.echoofthevoid.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared ending for every creature that sinks into the ground (user, 2026-10-08):
 * <ul>
 *   <li>the body always goes fully under before it is removed (step sized from its height);</li>
 *   <li>if its feet break into open air below (a cave or a room under the floor) it dissolves at
 *       once instead of sliding down through someone's ceiling;</li>
 *   <li>removal is never a silent pop: a puff of black smoke and a soft exhale mark it.</li>
 * </ul>
 */
public final class UncannySinkTransition {
    /** Extra depth below the full body height, covering client interpolation lag. */
    public static final double EXTRA_DEPTH = 0.9D;

    private UncannySinkTransition() {
    }

    /** Per-tick descent that buries the whole body within {@code ticks}, never slower than {@code base}. */
    public static double step(Entity entity, double base, int ticks) {
        return Math.max(base, (entity.getBbHeight() + EXTRA_DEPTH) / Math.max(1, ticks));
    }

    /**
     * True once the sinking feet have left solid ground for open, non-fluid space: someone below
     * would see the body come through. Water and lava still hide a sinking body, so they do not count.
     */
    public static boolean breaksIntoOpenSpace(Entity entity) {
        Level level = entity.level();
        BlockPos feet = BlockPos.containing(entity.getX(), entity.getY() - 0.05D, entity.getZ());
        if (!level.hasChunkAt(feet)) {
            return false;
        }
        BlockState state = level.getBlockState(feet);
        return state.getCollisionShape(level, feet).isEmpty() && level.getFluidState(feet).isEmpty();
    }

    /** Smoke and a soft exhale where the body is; the client removal hides inside the puff. */
    public static void effects(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        double height = entity.getBbHeight();
        double width = entity.getBbWidth();
        double centreY = entity.getY() + height * 0.5D;
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), centreY, entity.getZ(), 14, width * 0.4D, height * 0.35D, width * 0.4D, 0.01D);
        level.sendParticles(ParticleTypes.SQUID_INK,
                entity.getX(), centreY, entity.getZ(), 8, width * 0.35D, height * 0.3D, width * 0.35D, 0.02D);
        level.playSound(null, entity.getX(), centreY, entity.getZ(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.55F, 0.55F);
    }

    public static void vanish(Entity entity) {
        effects(entity);
        entity.discard();
    }
}
