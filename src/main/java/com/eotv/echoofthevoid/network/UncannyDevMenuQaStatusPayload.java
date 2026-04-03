package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyDevMenuQaStatusPayload(String entryId, boolean validatedGreen) implements CustomPacketPayload {
    public static final Type<UncannyDevMenuQaStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_dev_menu_qa_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDevMenuQaStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(512),
                    UncannyDevMenuQaStatusPayload::entryId,
                    ByteBufCodecs.BOOL,
                    UncannyDevMenuQaStatusPayload::validatedGreen,
                    UncannyDevMenuQaStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
