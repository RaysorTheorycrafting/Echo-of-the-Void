package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyZombieRalePayload(float volume, float pitch) implements CustomPacketPayload {
    public static final Type<UncannyZombieRalePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_zombie_rale"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyZombieRalePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    UncannyZombieRalePayload::volume,
                    ByteBufCodecs.FLOAT,
                    UncannyZombieRalePayload::pitch,
                    UncannyZombieRalePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
