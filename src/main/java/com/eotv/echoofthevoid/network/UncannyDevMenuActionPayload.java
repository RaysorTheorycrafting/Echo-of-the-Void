package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyDevMenuActionPayload(String entryId) implements CustomPacketPayload {
    public static final Type<UncannyDevMenuActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_dev_menu_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDevMenuActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(512),
                    UncannyDevMenuActionPayload::entryId,
                    UncannyDevMenuActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
