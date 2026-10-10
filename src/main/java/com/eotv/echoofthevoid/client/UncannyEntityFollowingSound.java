package com.eotv.echoofthevoid.client;

import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * A positional sound that travels with its entity, like Vanilla's EntityBoundSoundInstance, but
 * which plays for silent entities and keeps sounding where the body was when it vanishes.
 */
final class UncannyEntityFollowingSound extends AbstractTickableSoundInstance {
    @Nullable
    private final Entity entity;

    UncannyEntityFollowingSound(
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch,
            long seed,
            @Nullable Entity entity,
            double fallbackX,
            double fallbackY,
            double fallbackZ) {
        super(sound, source, RandomSource.create(seed));
        this.entity = entity;
        this.volume = volume;
        this.pitch = pitch;
        this.x = entity != null ? entity.getX() : fallbackX;
        this.y = entity != null ? entity.getY() : fallbackY;
        this.z = entity != null ? entity.getZ() : fallbackZ;
    }

    @Override
    public void tick() {
        if (this.entity != null && !this.entity.isRemoved()) {
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }
}
