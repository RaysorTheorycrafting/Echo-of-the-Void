package com.eotv.echoofthevoid.mixin.client.darkness;

import com.eotv.echoofthevoid.client.darkness.UncannyDynamicTextureAccess;
import com.eotv.echoofthevoid.client.darkness.UncannyLightmapAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class MixinLightTexture implements UncannyLightmapAccess {
    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Shadow
    private float blockLightRedFlicker;

    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void eotv$afterInit(GameRenderer renderer, Minecraft minecraft, CallbackInfo ci) {
        ((UncannyDynamicTextureAccess) this.lightTexture).eotv$markLightmapTexture();
    }

    @Override
    public float eotv$getPrevFlicker() {
        return this.blockLightRedFlicker;
    }

    @Override
    public boolean eotv$isDirty() {
        return this.updateLightTexture;
    }
}
