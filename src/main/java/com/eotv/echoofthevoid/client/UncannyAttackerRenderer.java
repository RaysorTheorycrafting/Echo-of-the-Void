package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class UncannyAttackerRenderer extends UncannySilhouetteRenderer<UncannyStalkerEntity> {
    public UncannyAttackerRenderer(EntityRendererProvider.Context context) {
        super(context, new UncannyAttackerModel(context.bakeLayer(ModelLayers.PLAYER)));
    }
}
