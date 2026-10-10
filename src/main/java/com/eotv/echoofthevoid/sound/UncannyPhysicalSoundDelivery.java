package com.eotv.echoofthevoid.sound;

import com.eotv.echoofthevoid.diagnostics.UncannyDiagnostics;
import com.eotv.echoofthevoid.network.UncannyEntitySoundPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Delivery policy for a physical sound emitted by an entity that suppresses its Vanilla voice. */
public final class UncannyPhysicalSoundDelivery {
    private UncannyPhysicalSoundDelivery() {
    }

    /**
     * Emits a spatial sound that travels with the entity (user, 2026-10-09: a cry must stay on the
     * body and follow it). Level's entity overload cannot be used: the client drops every entity
     * sound while {@link Entity#isSilent()} is true, and these Specials use that flag only to
     * suppress inherited Monster ambience and footsteps. The mod's own payload follows the body
     * and falls back to the emission point when a client does not track the entity.
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
        double x = sourceEntity.getX();
        double y = sourceEntity.getY();
        double z = sourceEntity.getZ();
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                x,
                y,
                z,
                sound.getRange(volume),
                new UncannyEntitySoundPayload(
                        sourceEntity.getId(),
                        sound.getLocation().toString(),
                        source.getName(),
                        volume,
                        pitch,
                        level.getRandom().nextLong(),
                        x,
                        y,
                        z));
        UncannyDiagnostics.physicalSoundPlayed(
                sourceEntity,
                sound.getLocation().toString(),
                source.getName(),
                volume,
                pitch);
    }
}
