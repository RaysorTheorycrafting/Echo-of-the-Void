package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyFalseRecipeToastPayload(String title, String subtitle) implements CustomPacketPayload {
    public static final Type<UncannyFalseRecipeToastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_false_recipe_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyFalseRecipeToastPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(96),
                    UncannyFalseRecipeToastPayload::title,
                    ByteBufCodecs.stringUtf8(128),
                    UncannyFalseRecipeToastPayload::subtitle,
                    UncannyFalseRecipeToastPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

