package com.eotv.echoofthevoid.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Swims a land creature after its prey as a player would (user, 2026-10-09: "comme Old Friend"):
 * sprint-swimming under water, diving after a player below it, surfacing to follow one above and
 * climbing out at the shore, instead of bobbing helplessly at the surface with Vanilla's FloatGoal.
 * Called after the creature's own tick while it is in water and hunting a player.
 */
public final class UncannySwimming {
    /** A sprint-swimming player under water, in blocks per tick. */
    public static final double SUBMERGED_SPEED = 0.20D;
    /** A player swimming at the surface (no sprint possible there). */
    public static final double SURFACE_SPEED = 0.11D;
    public static final double VERTICAL_SPEED = 0.10D;
    /** Share of the wanted velocity taken each tick: quick but never an instant snap. */
    public static final double BLEND = 0.35D;
    /** FloatGoal lifts a submerged mob by this much every tick; a dive cancels it. */
    private static final double FLOAT_GOAL_LIFT = 0.04D;

    private UncannySwimming() {
    }

    /** Horizontal speed a swimmer may reach, sprinting only when submerged as a player does. */
    public static double horizontalSpeed(boolean submerged) {
        return submerged ? SUBMERGED_SPEED : SURFACE_SPEED;
    }

    /** Wanted vertical speed: follow the prey's height, rise out of the water towards prey on land. */
    public static double verticalSpeed(double heightDifference, boolean preyInWater, boolean againstWall) {
        if (againstWall && heightDifference > -0.5D) {
            // At a bank: a player jumps out of the water.
            return 0.30D;
        }
        if (!preyInWater && heightDifference > 0.0D) {
            return VERTICAL_SPEED;
        }
        return Math.max(-VERTICAL_SPEED, Math.min(VERTICAL_SPEED, heightDifference * 0.12D));
    }

    public static double blend(double current, double wanted) {
        return current + (wanted - current) * BLEND;
    }

    /** @return true when it steered the creature this tick */
    public static boolean tickToward(Mob mob, Entity prey) {
        if (mob == null || prey == null || !mob.isInWater() || mob.isPassenger()
                || mob.noPhysics || mob.isNoGravity() || mob.isDeadOrDying()) {
            return false;
        }
        Vec3 offset = prey.position().subtract(mob.position());
        double horizontal = Math.hypot(offset.x, offset.z);
        double speed = horizontalSpeed(mob.isUnderWater());
        double wantX = horizontal < 1.0E-4D ? 0.0D : offset.x / horizontal * Math.min(speed, horizontal);
        double wantZ = horizontal < 1.0E-4D ? 0.0D : offset.z / horizontal * Math.min(speed, horizontal);
        double wantY = verticalSpeed(offset.y, prey.isInWater(), mob.horizontalCollision);
        Vec3 current = mob.getDeltaMovement();
        double nextY = blend(current.y, wantY);
        if (wantY < 0.0D) {
            nextY -= FLOAT_GOAL_LIFT;
        }
        mob.setDeltaMovement(blend(current.x, wantX), nextY, blend(current.z, wantZ));
        mob.hasImpulse = true;
        if (horizontal > 0.3D) {
            float yaw = (float) (Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0D);
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
            mob.yHeadRot = yaw;
        }
        return true;
    }
}
