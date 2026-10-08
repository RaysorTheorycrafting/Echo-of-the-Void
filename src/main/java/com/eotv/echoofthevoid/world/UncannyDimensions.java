package com.eotv.echoofthevoid.world;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Stable additive identifiers for the isolated Devourer? trial. */
public final class UncannyDimensions {
    public static final ResourceLocation ELSEWHERE_ID =
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "elsewhere");
    public static final ResourceKey<Level> ELSEWHERE =
            ResourceKey.create(Registries.DIMENSION, ELSEWHERE_ID);

    private UncannyDimensions() {
    }

    public static boolean isElsewhere(Level level) {
        return level != null && level.dimension() == ELSEWHERE;
    }
}
