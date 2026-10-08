package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyMinerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Miner? shares Attacker?'s silhouette, but keeps the ordinary humanoid walk cycle. */
public final class UncannyMinerRenderer extends UncannySilhouetteRenderer<UncannyMinerEntity> {
    public UncannyMinerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
