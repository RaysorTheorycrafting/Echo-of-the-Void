package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared visual impulse for a fishing hook; the authoritative server fishing state is untouched. */
public record UncannyFishingTugPayload(int entityId, float xImpulse, float zImpulse)
        implements CustomPacketPayload {
    public static final Type<UncannyFishingTugPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_fishing_tug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyFishingTugPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UncannyFishingTugPayload::entityId,
                    ByteBufCodecs.FLOAT,
                    UncannyFishingTugPayload::xImpulse,
                    ByteBufCodecs.FLOAT,
                    UncannyFishingTugPayload::zImpulse,
                    UncannyFishingTugPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
