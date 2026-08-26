package com.eotv.echoofthevoid.sound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/** Delivery policy for a physical sound emitted by an entity that suppresses its Vanilla voice. */
public final class UncannyPhysicalSoundDelivery {
    private UncannyPhysicalSoundDelivery() {
    }

    /**
     * Emits from the entity's current coordinates instead of using Level's entity overload.
     * The latter deliberately drops every sound when {@link Entity#isSilent()} is true, while
     * these Specials use that flag only to suppress inherited Monster ambience and footsteps.
     */
    public static void playFromEntity(
            ServerLevel level,
            Entity sourceEntity,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        if (level == null || sourceEntity == null || sound == null || source == null) {
            return;
        }
        level.playSound(
                null,
                sourceEntity.getX(),
                sourceEntity.getY(),
                sourceEntity.getZ(),
                sound,
                source,
                volume,
                pitch);
    }
}
