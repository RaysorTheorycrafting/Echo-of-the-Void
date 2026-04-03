package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyPetRefusalVisualPayload(int entityId, boolean active, int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyPetRefusalVisualPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_pet_refusal_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyPetRefusalVisualPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UncannyPetRefusalVisualPayload::entityId,
                    ByteBufCodecs.BOOL,
                    UncannyPetRefusalVisualPayload::active,
                    ByteBufCodecs.VAR_INT,
                    UncannyPetRefusalVisualPayload::durationTicks,
                    UncannyPetRefusalVisualPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

