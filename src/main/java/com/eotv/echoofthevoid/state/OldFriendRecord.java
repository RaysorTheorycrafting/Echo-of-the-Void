package com.eotv.echoofthevoid.state;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/**
 * The one "old friend" of a world: a real player found in the host's files who "joined" and now
 * sits in the tab list. {@code remainingTicks} counts the target's online time until the attack.
 *
 * @param textures cached skin property ("value|signature"), empty when the skin could not be fetched
 */
public record OldFriendRecord(
        UUID friendId,
        String friendName,
        UUID targetId,
        Stage stage,
        long remainingTicks,
        String textures) {

    public enum Stage {
        PRESENT,
        ATTACKING,
        DONE
    }

    public OldFriendRecord withStage(Stage newStage) {
        return new OldFriendRecord(friendId, friendName, targetId, newStage, remainingTicks, textures);
    }

    public OldFriendRecord withRemaining(long ticks) {
        return new OldFriendRecord(friendId, friendName, targetId, stage, Math.max(0L, ticks), textures);
    }

    public OldFriendRecord withTextures(String newTextures) {
        return new OldFriendRecord(friendId, friendName, targetId, stage, remainingTicks,
                newTextures == null ? "" : newTextures);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("friend", friendId);
        tag.putString("name", friendName);
        tag.putUUID("target", targetId);
        tag.putString("stage", stage.name());
        tag.putLong("remaining", remainingTicks);
        tag.putString("textures", textures);
        return tag;
    }

    /** @return null for an absent or incomplete entry */
    public static OldFriendRecord load(CompoundTag tag) {
        if (!tag.hasUUID("friend") || !tag.hasUUID("target") || tag.getString("name").isBlank()) {
            return null;
        }
        Stage stage;
        try {
            stage = Stage.valueOf(tag.getString("stage"));
        } catch (IllegalArgumentException exception) {
            stage = Stage.DONE;
        }
        return new OldFriendRecord(tag.getUUID("friend"), tag.getString("name"), tag.getUUID("target"),
                stage, Math.max(0L, tag.getLong("remaining")), tag.getString("textures"));
    }
}
