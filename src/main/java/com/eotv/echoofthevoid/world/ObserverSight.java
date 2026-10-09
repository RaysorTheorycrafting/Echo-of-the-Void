package com.eotv.echoofthevoid.world;

import com.eotv.echoofthevoid.event.paranoia.nativeevent.WanderingTreeRules;
import java.util.Collection;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared "could a player see this?" test for effects that must only happen unseen.
 *
 * <p>A point counts as seen when it lies inside an 80 degree cone around a player's look direction
 * (wider than any field-of-view setting) and nothing fully opaque stands between the eyes and the
 * point. Glass, leaves, fences and slabs never hide anything, and unloaded chunks count as
 * transparent so the check never loads terrain: every uncertainty errs on the side of "seen".</p>
 */
public final class ObserverSight {
    private ObserverSight() {
    }

    /** Farthest distance at which a player can possibly render the point. */
    public static double viewRadius(ServerLevel level) {
        return Math.max(96.0D, (level.getServer().getPlayerList().getViewDistance() + 1) * 16.0D);
    }

    /**
     * @param clearance any observer closer than this to a point counts as seeing it, even facing away
     * @param reached   cells that belong to the observed thing: a ray reaching one of them sees it
     */
    public static boolean isSeenByAny(
            ServerLevel level,
            Collection<Vec3> points,
            Collection<? extends Player> observers,
            double clearance,
            Predicate<BlockPos> reached) {
        double viewRadius = viewRadius(level);
        for (Player observer : observers) {
            if (!observer.isAlive() || observer.isSpectator() || observer.level() != level) {
                continue;
            }
            Vec3 eye = observer.getEyePosition();
            Vec3 look = observer.getLookAngle();
            for (Vec3 point : points) {
                double distanceSqr = eye.distanceToSqr(point);
                if (distanceSqr <= clearance * clearance) {
                    return true;
                }
                if (distanceSqr > viewRadius * viewRadius) {
                    continue;
                }
                Vec3 to = point.subtract(eye);
                if (WanderingTreeRules.isInViewCone(look.x, look.y, look.z, to.x, to.y, to.z)
                        && hasLineOfSight(level, eye, point, reached)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True when no fully opaque block stands between {@code eye} and {@code point}. */
    public static boolean hasLineOfSight(ServerLevel level, Vec3 eye, Vec3 point, Predicate<BlockPos> reached) {
        BlockPos eyeBlock = BlockPos.containing(eye);
        BlockPos targetBlock = BlockPos.containing(point);
        Boolean blocked = BlockGetter.traverseBlocks(eye, point, level, (lvl, pos) -> {
            if (pos.equals(targetBlock) || reached.test(pos)) {
                return Boolean.FALSE;
            }
            if (pos.equals(eyeBlock) || !lvl.isLoaded(pos)) {
                return null;
            }
            BlockState state = lvl.getBlockState(pos);
            return state.isSolidRender(lvl, pos) ? Boolean.TRUE : null;
        }, lvl -> Boolean.FALSE);
        return !Boolean.TRUE.equals(blocked);
    }

    /**
     * Stricter test for "the player is looking at it": a narrow cone (default {@code 0.9}, about 25
     * degrees) plus line of sight. Used to count deliberate stares, not peripheral glimpses.
     */
    public static boolean isStaredAt(Player observer, Vec3 point, double dotThreshold, double maxDistance) {
        if (!observer.isAlive() || observer.isSpectator() || !(observer.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 eye = observer.getEyePosition();
        Vec3 to = point.subtract(eye);
        double distance = to.length();
        if (distance > maxDistance) {
            return false;
        }
        if (distance > 1.0E-3D && observer.getLookAngle().normalize().dot(to.scale(1.0D / distance)) < dotThreshold) {
            return false;
        }
        return hasLineOfSight(level, eye, point, pos -> false);
    }
}
