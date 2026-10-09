package com.eotv.echoofthevoid.mixin.common;

import com.eotv.echoofthevoid.event.special.SleeperPins;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Leave Bed" and the morning do nothing while a Sleeper? holds the player down. */
@Mixin(ServerPlayer.class)
public abstract class SleeperPinnedServerPlayerMixin {
    @Inject(method = "stopSleepInBed(ZZ)V", at = @At("HEAD"), cancellable = true)
    private void eotv$cannotLeaveWhilePinned(boolean wakeImmediately, boolean updateLevel, CallbackInfo ci) {
        if (SleeperPins.isPinned((ServerPlayer) (Object) this)) {
            ci.cancel();
        }
    }
}
