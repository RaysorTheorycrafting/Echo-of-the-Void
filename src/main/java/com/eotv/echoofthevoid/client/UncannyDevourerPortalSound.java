package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyDevourerEntity;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Distorted portal hum bound to a Devourer?'s chest: a physical, positional loop that every
 * tracking client hears from the creature itself, fading out while it sinks back through.
 */
public final class UncannyDevourerPortalSound extends AbstractTickableSoundInstance {
    private static final float BASE_VOLUME = 0.85F;
    private final UncannyDevourerEntity devourer;

    private UncannyDevourerPortalSound(UncannyDevourerEntity devourer) {
        super(UncannySoundRegistry.DEVOURER_PORTAL_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.devourer = devourer;
        this.looping = true;
        this.delay = 0;
        this.volume = BASE_VOLUME;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        follow();
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof UncannyDevourerEntity devourer) {
            Minecraft.getInstance().getSoundManager().queueTickingSound(new UncannyDevourerPortalSound(devourer));
        }
    }

    @Override
    public void tick() {
        if (devourer.isRemoved() || !devourer.isAlive()) {
            stop();
            return;
        }
        follow();
        this.volume = devourer.isSinking() ? Math.max(0.0F, this.volume - BASE_VOLUME / 40.0F) : BASE_VOLUME;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    private void follow() {
        this.x = devourer.getX();
        this.y = devourer.getY() + devourer.getBbHeight() * 0.6D;
        this.z = devourer.getZ();
    }
}
