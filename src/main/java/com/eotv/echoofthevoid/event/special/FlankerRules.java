package com.eotv.echoofthevoid.event.special;

/**
 * Minecraft-free rules of the Flanker? pincer (rework, user carte blanche 2026-10-09).
 *
 * <p>Two roles swap with the target's gaze. The <b>bait</b> is the member being looked at: it stays
 * a few blocks in front, strafing, and backs off when charged. The <b>blade</b> is the other: it
 * circles outside the field of view, closes in from behind, dashes, strikes and breaks off. Turning
 * to face the blade makes it the bait and the old bait, now behind, the blade. When the blade dashes
 * the bait lunges from the front: caught between them, the target takes both blows.</p>
 *
 * <p>Strength: the pair together is one Attacker?. Each member carries half its toughness and the
 * same blow; the pair shares a single strike rhythm (one landed blow per Attacker? cadence, a pincer
 * costing two).</p>
 *
 * <p>Angles are measured from the target's horizontal view direction, signed by the cross product
 * {@code view × offset}: 0 is straight ahead, ±180 straight behind.</p>
 */
public final class FlankerRules {
    /** A player's horizontal field of view at the usual settings is about ±50°; a little margin. */
    public static final double VIEW_HALF_ANGLE = 55.0D;
    /** The blade only strikes from well behind the target's shoulder line. */
    public static final double STRIKE_MIN_ANGLE = 105.0D;
    public static final double STRIKE_REACH = 2.6D;
    /** Behind and this close, the blade dashes in. */
    public static final double DASH_DISTANCE = 5.5D;
    /** Each replan moves the blade this far around the target, toward its back. */
    public static final double ORBIT_STEP_DEGREES = 40.0D;
    public static final double MIN_ORBIT_RADIUS = 3.5D;
    public static final double MAX_ORBIT_RADIUS = 9.0D;
    public static final double BAIT_DISTANCE = 6.0D;
    public static final double BAIT_BACKOFF_DISTANCE = 4.5D;
    public static final double BAIT_STRAFE_DEGREES = 25.0D;
    /** After a blow the blade breaks off to the side for this long. */
    public static final int BLADE_RECOVER_TICKS = 24;
    /** The bait's blow counts as a pincer only this soon after the blade's. */
    public static final int PINCER_WINDOW_TICKS = 8;
    /** The member looked at stays the bait until the other is looked at clearly more (cosine). */
    public static final double ROLE_SWAP_MARGIN = 0.25D;
    /** Formation before the hunt: blade behind, bait ahead, both this close, for this long. */
    public static final double FORMATION_BLADE_MIN_ANGLE = 110.0D;
    public static final double FORMATION_BAIT_MAX_ANGLE = 70.0D;
    public static final double FORMATION_MAX_DISTANCE = 14.0D;
    public static final int FORMATION_STABLE_TICKS = 10;
    public static final int MIN_AGE_BEFORE_HUNT_TICKS = 40;
    /** Terrain that never allows the formation does not stop the hunt for ever. */
    public static final int APPROACH_TIMEOUT_TICKS = 240;

    // Speeds as multiples of a sprinting player's ground speed.
    public static final double BAIT_SPRINT_RATIO = 1.1D;
    public static final double BACKOFF_SPRINT_RATIO = 1.5D;
    public static final double ORBIT_SPRINT_RATIO = 1.45D;
    public static final double DASH_SPRINT_RATIO = 1.85D;

    /** A sprinting player's movement term: speed 0.1 × sprint 1.3. */
    private static final double SPRINTING_PLAYER_SPEED = 0.13D;

    private FlankerRules() {
    }

    /** Pair-wide gap between two landed blows: the Attacker? cadence the damage is calibrated for. */
    public static int pairStrikeGapTicks() {
        return HuntingSpecialRules.flankerAttackIntervalTicks(CombatParityRules.FLANKER_PAIR_MEMBER);
    }

    /** Signed angle in degrees of an offset seen from the target, in (-180, 180]. */
    public static double signedAngleFromView(double viewX, double viewZ, double offsetX, double offsetZ) {
        double cross = viewX * offsetZ - viewZ * offsetX;
        double dot = viewX * offsetX + viewZ * offsetZ;
        if (Math.abs(cross) < 1.0E-9D && Math.abs(dot) < 1.0E-9D) {
            return 0.0D;
        }
        return Math.toDegrees(Math.atan2(cross, dot));
    }

    public static boolean isInView(double signedAngle) {
        return Math.abs(signedAngle) <= VIEW_HALF_ANGLE;
    }

    public static boolean canStrikeFrom(double signedAngle) {
        return Math.abs(signedAngle) >= STRIKE_MIN_ANGLE;
    }

    /** One orbit step toward the target's back, on the side the blade already is. */
    public static double orbitAngle(double signedAngle) {
        double side = signedAngle >= 0.0D ? 1.0D : -1.0D;
        return side * Math.min(180.0D, Math.abs(signedAngle) + ORBIT_STEP_DEGREES);
    }

    /** Closes in while circling, never nearer than the dash distance until it is behind. */
    public static double orbitRadius(double distance, double signedAngle) {
        double closer = distance - (canStrikeFrom(signedAngle) ? 2.0D : 0.5D);
        double floor = canStrikeFrom(signedAngle) ? MIN_ORBIT_RADIUS : DASH_DISTANCE;
        return Math.max(floor, Math.min(MAX_ORBIT_RADIUS, closer));
    }

    /** The view direction turned by a signed angle, as {x, z}; consistent with signedAngleFromView. */
    public static double[] rotate(double viewX, double viewZ, double signedAngle) {
        double radians = Math.toRadians(signedAngle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new double[] {viewX * cos - viewZ * sin, viewX * sin + viewZ * cos};
    }

    /** Which member is the bait, from how directly each is looked at (cosines), with hysteresis. */
    public static int chooseBait(int currentBait, double cosineMember0, double cosineMember1) {
        double current = currentBait == 0 ? cosineMember0 : cosineMember1;
        double other = currentBait == 0 ? cosineMember1 : cosineMember0;
        return other > current + ROLE_SWAP_MARGIN ? 1 - currentBait : currentBait;
    }

    public static boolean isFormation(double bladeAngle, double baitAngle, double bladeDistance, double baitDistance) {
        return Math.abs(bladeAngle) >= FORMATION_BLADE_MIN_ANGLE
                && Math.abs(baitAngle) <= FORMATION_BAIT_MAX_ANGLE
                && bladeDistance <= FORMATION_MAX_DISTANCE
                && baitDistance <= FORMATION_MAX_DISTANCE;
    }

    /**
     * Navigation speed modifier giving a ground speed of {@code sprintRatio} times a sprinting
     * player's. A mob's forward input equals its speed, so its ground speed grows with the square
     * of {@code attribute × modifier}, where a player's input is a full step.
     */
    public static double navigationModifier(double movementAttribute, double sprintRatio) {
        double speed = Math.sqrt(SPRINTING_PLAYER_SPEED * Math.max(0.1D, sprintRatio));
        return speed / Math.max(0.05D, movementAttribute);
    }
}
