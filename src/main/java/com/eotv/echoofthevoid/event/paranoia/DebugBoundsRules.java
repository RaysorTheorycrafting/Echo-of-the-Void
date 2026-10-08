package com.eotv.echoofthevoid.event.paranoia;

/** Deterministic, runtime-independent limits for the hidden F3+B encounter. */
public final class DebugBoundsRules {
    public static final int MINIMUM_PHASE = 2;
    public static final int DURATION_TICKS = 20 * 36;
    public static final int PRESENCE_COUNT = 36;
    public static final double MINIMUM_RADIUS = 2.35D;
    public static final double MINIMUM_START_RADIUS = 13.0D;
    public static final double MAXIMUM_START_RADIUS = 24.0D;
    public static final double MINIMUM_APPROACH_SPEED = 0.012D;
    public static final double MAXIMUM_APPROACH_SPEED = 0.026D;
    public static final int MINIMUM_RETARGET_TICKS = 44;
    public static final int MAXIMUM_RETARGET_TICKS = 82;

    /** Presences notice the player one after another over the first ten seconds. */
    public static final int MAXIMUM_NOTICE_DELAY_TICKS = 20 * 10;
    /** A hunting presence lunges from here and never overlaps the player's own box. */
    public static final double STRIKE_DISTANCE = 1.6D;
    public static final double CONTACT_DISTANCE = 0.85D;
    /** Fake blows: felt (hurt shake, sound, shove) but harmless, and never a stream. */
    public static final int MAXIMUM_FAKE_HITS = 5;
    public static final int FAKE_HIT_COOLDOWN_TICKS = 22;
    public static final double WANDER_SPEED = 0.035D;
    /** A striker that has not gained ground for this long slips round behind the player. */
    public static final int STUCK_REPOSITION_TICKS = 60;
    /** Only the first few to notice come to blows; the rest stalk in a ring and stare. */
    public static final int MAXIMUM_STRIKERS = 6;
    public static final double MINIMUM_STALK_RADIUS = 4.5D;
    public static final double MAXIMUM_STALK_RADIUS = 9.0D;
    /** Where a striker waits after its blow, still close and still watching. */
    public static final double AFTER_STRIKE_RADIUS = 3.2D;
    public static final double GRAVITY = 0.08D;
    public static final double JUMP_VELOCITY = 0.42D;

    private DebugBoundsRules() {
    }

    /** Body classes copied from Vanilla mobs, so every box has a believable size and gait. */
    public enum Shape {
        SMALL(0.70D, 0.50D, 0.17D, 0.21D),      // cave-spider sized, quick
        TALL(0.60D, 2.90D, 0.12D, 0.15D),       // Enderman sized
        HEAVY(1.40D, 2.70D, 0.07D, 0.09D),      // Iron Golem sized, slow
        HUMANOID(0.60D, 1.95D, 0.11D, 0.14D),   // zombie sized
        FLYING(0.50D, 0.90D, 0.0D, 0.0D);       // bat sized, never strikes

        private final double width;
        private final double height;
        private final double minimumSpeed;
        private final double maximumSpeed;

        Shape(double width, double height, double minimumSpeed, double maximumSpeed) {
            this.width = width;
            this.height = height;
            this.minimumSpeed = minimumSpeed;
            this.maximumSpeed = maximumSpeed;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }

        public boolean strikes() {
            return this != FLYING;
        }
    }

    public static Shape shape(long seed, int index) {
        double roll = unit(seed + 0x632BE59BD9B4E019L * (index + 1L));
        if (roll < 0.08D) {
            return Shape.FLYING;
        }
        if (roll < 0.22D) {
            return Shape.SMALL;
        }
        if (roll < 0.34D) {
            return Shape.TALL;
        }
        if (roll < 0.42D) {
            return Shape.HEAVY;
        }
        return Shape.HUMANOID;
    }

    public static double stalkRadius(long seed, int index) {
        return MINIMUM_STALK_RADIUS
                + unit(seed ^ (0x2545F4914F6CDD1DL * (index + 1L))) * (MAXIMUM_STALK_RADIUS - MINIMUM_STALK_RADIUS);
    }

    public static int noticeDelayTicks(long seed, int index) {
        return (int) Math.floor(unit(seed ^ (0x5851F42D4C957F2DL * (index + 1L))) * MAXIMUM_NOTICE_DELAY_TICKS);
    }

    /** Blocks per tick once hunting: Vanilla walking speeds for the body class. */
    public static double huntSpeed(long seed, int index, Shape shape) {
        double unit = unit(seed ^ (0xA24BAED4963EE407L * (index + 1L)));
        return shape.minimumSpeed + unit * (shape.maximumSpeed - shape.minimumSpeed);
    }

    public static boolean canStartNaturally(
            int phase,
            boolean hitboxesEnabled,
            boolean alreadyConsumed,
            boolean alive,
            boolean spectator,
            boolean sleeping,
            boolean majorEventPaused) {
        return phase >= MINIMUM_PHASE
                && hitboxesEnabled
                && !alreadyConsumed
                && alive
                && !spectator
                && !sleeping
                && !majorEventPaused;
    }

    public static double startRadius(long seed, int index) {
        double unit = unit(seed + 0x9E3779B97F4A7C15L * (index + 1L));
        return MINIMUM_START_RADIUS + unit * (MAXIMUM_START_RADIUS - MINIMUM_START_RADIUS);
    }

    public static double angleRadians(long seed, int index, int count) {
        int safeCount = Math.max(1, count);
        double jitter = (unit(seed ^ (0xD1B54A32D192ED03L * (index + 1L))) - 0.5D) * 0.28D;
        return Math.PI * 2.0D * index / safeCount + jitter;
    }

    public static double radiusAt(double startRadius, int elapsedTicks, int durationTicks) {
        double progress = Math.max(0.0D, Math.min(1.0D, elapsedTicks / (double) Math.max(1, durationTicks)));
        double eased = progress * progress * (3.0D - 2.0D * progress);
        return Math.max(MINIMUM_RADIUS, startRadius + (MINIMUM_RADIUS - startRadius) * eased);
    }

    public static double approachSpeed(long seed, int index) {
        return MINIMUM_APPROACH_SPEED
                + unit(seed ^ (0xA24BAED4963EE407L * (index + 1L)))
                        * (MAXIMUM_APPROACH_SPEED - MINIMUM_APPROACH_SPEED);
    }

    public static int retargetInterval(long seed, int index, int generation) {
        long ticket = seed
                ^ (0x8CB92BA72F3D8DD7L * (index + 1L))
                ^ (0xDB4F0B9175AE2165L * (generation + 1L));
        return MINIMUM_RETARGET_TICKS
                + (int) Math.floor(unit(ticket) * (MAXIMUM_RETARGET_TICKS - MINIMUM_RETARGET_TICKS + 1));
    }

    public static Step targetOffset(long seed, int index, int generation) {
        long ticket = seed
                ^ (0xC6BC279692B5CC83L * (index + 1L))
                ^ (0x9E3779B97F4A7C15L * (generation + 1L));
        double angle = unit(ticket) * Math.PI * 2.0D;
        double distance = 0.65D + unit(ticket ^ 0xD1B54A32D192ED03L) * 2.4D;
        return new Step(Math.cos(angle) * distance, Math.sin(angle) * distance);
    }

    public static Step stepToward(
            double currentX,
            double currentZ,
            double targetX,
            double targetZ,
            double maximumStep) {
        double dx = targetX - currentX;
        double dz = targetZ - currentZ;
        double distance = Math.hypot(dx, dz);
        double remainingApproach = Math.max(0.0D, distance - MINIMUM_RADIUS);
        double step = Math.max(0.0D, Math.min(Math.max(0.0D, maximumStep), remainingApproach));
        if (distance < 1.0E-8D || step <= 0.0D) {
            return new Step(currentX, currentZ);
        }
        return new Step(currentX + dx / distance * step, currentZ + dz / distance * step);
    }

    public static double unit(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    public record Step(double x, double z) {
    }
}
