package com.eotv.echoofthevoid.mixin.client.darkness;

import com.eotv.echoofthevoid.client.UncannyProgressiveDarknessEngine;
import com.eotv.echoofthevoid.client.darkness.UncannyDynamicTextureAccess;
import com.mojang.blaze3d.platform.NativeImage;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicTexture.class)
public abstract class MixinDynamicTexture implements UncannyDynamicTextureAccess {
    @Shadow
    @Nullable
    private NativeImage pixels;

    private boolean eotv$lightmapHookEnabled;

    @Override
    public void eotv$markLightmapTexture() {
        this.eotv$lightmapHookEnabled = true;
    }

    @Inject(method = "upload", at = @At("HEAD"))
    private void eotv$onUpload(CallbackInfo ci) {
        if (!this.eotv$lightmapHookEnabled || this.pixels == null) {
            return;
        }

        UncannyProgressiveDarknessEngine engine = UncannyProgressiveDarknessEngine.INSTANCE;
        if (!engine.isActive()) {
            return;
        }

        engine.markUploadIntercepted();
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                int argb = this.pixels.getPixelRGBA(block, sky);
                int darkened = engine.darkenPixel(argb, block, sky);
                if (darkened != argb) {
                    this.pixels.setPixelRGBA(block, sky, darkened);
                }
            }
        }
    }
}
