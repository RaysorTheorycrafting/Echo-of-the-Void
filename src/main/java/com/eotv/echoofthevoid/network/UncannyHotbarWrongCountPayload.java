package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyHotbarWrongCountPayload(int slot, int fakeCount, int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyHotbarWrongCountPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_hotbar_wrong_count"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyHotbarWrongCountPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UncannyHotbarWrongCountPayload::slot,
                    ByteBufCodecs.VAR_INT,
                    UncannyHotbarWrongCountPayload::fakeCount,
                    ByteBufCodecs.VAR_INT,
                    UncannyHotbarWrongCountPayload::durationTicks,
                    UncannyHotbarWrongCountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

