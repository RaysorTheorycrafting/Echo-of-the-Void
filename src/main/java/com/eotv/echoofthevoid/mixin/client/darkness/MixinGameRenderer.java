package com.eotv.echoofthevoid.mixin.client.darkness;

import com.eotv.echoofthevoid.client.UncannyProgressiveDarknessEngine;
import com.eotv.echoofthevoid.client.darkness.UncannyLightmapAccess;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LightTexture lightTexture;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V",
                    shift = At.Shift.BEFORE))
    private void eotv$beforeLightTextureUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        UncannyLightmapAccess access = (UncannyLightmapAccess) this.lightTexture;
        UncannyProgressiveDarknessEngine.INSTANCE.updateLuminance(
                partialTick,
                this.minecraft,
                access.eotv$getPrevFlicker(),
                access.eotv$isDirty());
    }
}
