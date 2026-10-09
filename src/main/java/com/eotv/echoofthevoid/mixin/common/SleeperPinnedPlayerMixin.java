package com.eotv.echoofthevoid.mixin.common;

import com.eotv.echoofthevoid.event.special.SleeperPins;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A pinned sleeper never counts as deeply asleep: the night cannot be skipped during the attack. */
@Mixin(Player.class)
public abstract class SleeperPinnedPlayerMixin {
    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void eotv$pinnedSleeperNeverSleepsLongEnough(CallbackInfoReturnable<Boolean> cir) {
        if (SleeperPins.isPinned((Player) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
