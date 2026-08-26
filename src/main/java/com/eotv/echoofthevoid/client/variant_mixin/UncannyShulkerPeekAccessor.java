package com.eotv.echoofthevoid.client.variant_mixin;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Client-only access to the existing Vanilla shulker lid animation. */
@Mixin(Shulker.class)
public interface UncannyShulkerPeekAccessor {
    @Invoker("setRawPeekAmount")
    void echoofthevoid$setRawPeekAmount(int amount);
}
