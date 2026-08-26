package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Additive dev-only request. The legacy one-field action payload remains registered unchanged. */
public record UncannyDevMenuRunPayload(String entryId, String targetName, int spawnDistance)
        implements CustomPacketPayload {
    public static final Type<UncannyDevMenuRunPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_dev_menu_run"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDevMenuRunPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(512),
                    UncannyDevMenuRunPayload::entryId,
                    ByteBufCodecs.stringUtf8(64),
                    UncannyDevMenuRunPayload::targetName,
                    ByteBufCodecs.VAR_INT,
                    UncannyDevMenuRunPayload::spawnDistance,
                    UncannyDevMenuRunPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
