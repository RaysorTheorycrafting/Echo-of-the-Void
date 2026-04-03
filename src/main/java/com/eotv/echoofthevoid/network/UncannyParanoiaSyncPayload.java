package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyParanoiaSyncPayload(boolean hunterFogActive, boolean giantSunActive) implements CustomPacketPayload {
    public static final Type<UncannyParanoiaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_paranoia_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyParanoiaSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    UncannyParanoiaSyncPayload::hunterFogActive,
                    ByteBufCodecs.BOOL,
                    UncannyParanoiaSyncPayload::giantSunActive,
                    UncannyParanoiaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
