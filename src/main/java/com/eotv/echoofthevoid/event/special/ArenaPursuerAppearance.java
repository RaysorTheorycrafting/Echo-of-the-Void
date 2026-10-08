package com.eotv.echoofthevoid.event.special;

/**
 * Stable black silhouettes available to Elsewhere pursuers.
 *
 * <p>Each silhouette moves like the creature it shows: only the Spider climbs walls and pounces,
 * every other form walks on its own Vanilla-sized hitbox. Ids are persisted and synchronized, so
 * new forms are only ever appended.</p>
 */
public enum ArenaPursuerAppearance {
    HUMANOID(0, 0.6F, 1.95F, false),
    ZOMBIE(1, 0.6F, 1.95F, false),
    // Skeleton silhouette without a bow: the pursuer always fights in melee.
    SKELETON(2, 0.6F, 1.99F, false),
    VILLAGER(3, 0.6F, 1.95F, false),
    // Rendered at 0.72 scale, so its hitbox is the scaled Iron Golem one.
    IRON_GOLEM(4, 1.0F, 1.95F, false),
    SPIDER(5, 1.4F, 0.9F, true),
    SHEEP(6, 0.9F, 1.3F, false),
    COW(7, 0.9F, 1.4F, false),
    PIG(8, 0.9F, 0.9F, false);

    private static final ArenaPursuerAppearance[] VALUES = values();
    private final int id;
    private final float width;
    private final float height;
    private final boolean climbs;

    ArenaPursuerAppearance(int id, float width, float height, boolean climbs) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.climbs = climbs;
    }

    public int id() {
        return id;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    /** Only a creature that climbs in Vanilla may scale walls or leap at its target. */
    public boolean climbs() {
        return climbs;
    }

    public static int count() {
        return VALUES.length;
    }

    public static ArenaPursuerAppearance byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : HUMANOID;
    }
}
