package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UncannySpecialRewardRulesTest {
    @Test
    void everySpecialUsesReducedRareIndependentRewardRolls() {
        assertTrue(UncannySpecialRewardRules.resolve(0.0F, 0.0F).shard());
        assertTrue(UncannySpecialRewardRules.resolve(0.0F, 0.0F).shardPiece());
        assertTrue(UncannySpecialRewardRules.resolve(0.0749F, 0.2999F).shard());
        assertTrue(UncannySpecialRewardRules.resolve(0.0749F, 0.2999F).shardPiece());
        assertFalse(UncannySpecialRewardRules.resolve(0.075F, 0.30F).shard());
        assertFalse(UncannySpecialRewardRules.resolve(0.075F, 0.30F).shardPiece());
        assertFalse(UncannySpecialRewardRules.resolve(-0.1F, -0.1F).shard());
        assertFalse(UncannySpecialRewardRules.resolve(-0.1F, -0.1F).shardPiece());

        double shardEquivalentPerKill = UncannySpecialRewardRules.SHARD_DROP_CHANCE
                + UncannySpecialRewardRules.SHARD_PIECE_DROP_CHANCE / 9.0D;
        double formerPulseEquivalent = 0.10D + 0.50D / 9.0D;
        assertTrue(shardEquivalentPerKill < formerPulseEquivalent);
        assertTrue(shardEquivalentPerKill < 0.11D);
    }

    @Test
    void grandWardenAlwaysAwardsAnImportantBoundedShardStack() {
        assertEquals(6, UncannySpecialRewardRules.grandWardenShardCount(0));
        assertEquals(10, UncannySpecialRewardRules.grandWardenShardCount(4));
        assertThrows(IllegalArgumentException.class,
                () -> UncannySpecialRewardRules.grandWardenShardCount(-1));
        assertThrows(IllegalArgumentException.class,
                () -> UncannySpecialRewardRules.grandWardenShardCount(5));
    }
}
