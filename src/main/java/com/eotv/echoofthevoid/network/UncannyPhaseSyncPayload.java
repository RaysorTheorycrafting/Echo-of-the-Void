package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyPhaseSyncPayload(int phaseIndex) implements CustomPacketPayload {
    public static final Type<UncannyPhaseSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_phase_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyPhaseSyncPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, UncannyPhaseSyncPayload::phaseIndex, UncannyPhaseSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

