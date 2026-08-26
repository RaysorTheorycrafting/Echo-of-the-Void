package com.eotv.echoofthevoid.event.special;

/** Pure reward probabilities shared by every Special death. */
public final class UncannySpecialRewardRules {
    public static final float SHARD_DROP_CHANCE = 0.075F;
    public static final float SHARD_PIECE_DROP_CHANCE = 0.30F;
    public static final int GRAND_WARDEN_MIN_SHARDS = 6;
    public static final int GRAND_WARDEN_MAX_SHARDS = 10;

    private UncannySpecialRewardRules() {
    }

    public static RewardRoll resolve(float shardRoll, float shardPieceRoll) {
        return new RewardRoll(
                shardRoll >= 0.0F && shardRoll < SHARD_DROP_CHANCE,
                shardPieceRoll >= 0.0F && shardPieceRoll < SHARD_PIECE_DROP_CHANCE);
    }

    public static int grandWardenShardCount(int boundedRoll) {
        int size = GRAND_WARDEN_MAX_SHARDS - GRAND_WARDEN_MIN_SHARDS + 1;
        if (boundedRoll < 0 || boundedRoll >= size) {
            throw new IllegalArgumentException("boundedRoll must be in [0, " + size + ")");
        }
        return GRAND_WARDEN_MIN_SHARDS + boundedRoll;
    }

    public record RewardRoll(boolean shard, boolean shardPiece) {
    }
}
