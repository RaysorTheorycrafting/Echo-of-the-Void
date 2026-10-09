package com.eotv.echoofthevoid.state;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * One tree slowly walking toward a player's base. Only the trunk base is stored: the tree itself is
 * re-read from the world before every move or cut, so player edits are always respected.
 *
 * @param nextMoveGameTime level game time (persistent across restarts) of the next move attempt
 * @param settled          the tree reached the base or gave up; it no longer moves but stays armed
 */
public record WanderingTreeRecord(
        UUID id,
        UUID owner,
        String dimension,
        long basePos,
        int moves,
        long nextMoveGameTime,
        int blockedAttempts,
        boolean settled) {

    public BlockPos base() {
        return BlockPos.of(basePos);
    }

    public WanderingTreeRecord movedTo(BlockPos newBase, long nextMove, boolean nowSettled) {
        return new WanderingTreeRecord(id, owner, dimension, newBase.asLong(), moves + 1, nextMove, 0, nowSettled);
    }

    public WanderingTreeRecord retryAt(long nextMove, int blocked, boolean nowSettled) {
        return new WanderingTreeRecord(id, owner, dimension, basePos, moves, nextMove, blocked, nowSettled);
    }

    public WanderingTreeRecord withMoves(int newMoves) {
        return new WanderingTreeRecord(id, owner, dimension, basePos, newMoves, nextMoveGameTime, blockedAttempts, settled);
    }

    public WanderingTreeRecord withNextMove(long nextMove) {
        return new WanderingTreeRecord(id, owner, dimension, basePos, moves, nextMove, blockedAttempts, settled);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("owner", owner);
        tag.putString("dimension", dimension);
        tag.putLong("pos", basePos);
        tag.putInt("moves", moves);
        tag.putLong("nextMove", nextMoveGameTime);
        tag.putInt("blocked", blockedAttempts);
        tag.putBoolean("settled", settled);
        return tag;
    }

    /** @return null when the entry is incomplete */
    public static WanderingTreeRecord load(CompoundTag tag) {
        if (!tag.hasUUID("id") || !tag.hasUUID("owner") || tag.getString("dimension").isBlank()) {
            return null;
        }
        return new WanderingTreeRecord(
                tag.getUUID("id"),
                tag.getUUID("owner"),
                tag.getString("dimension"),
                tag.getLong("pos"),
                Math.max(0, tag.getInt("moves")),
                tag.getLong("nextMove"),
                Math.max(0, tag.getInt("blocked")),
                tag.getBoolean("settled"));
    }
}
