package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class UncannyMournerRenderer
        extends UncannySilhouetteRenderer<UncannyApprovedSpecialEntity> {
    public UncannyMournerRenderer(EntityRendererProvider.Context context) {
        super(context, new UncannyMournerModel(context.bakeLayer(ModelLayers.PLAYER)));
    }
}
