package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authored confirmation used by the developer menu; never trusted for gameplay state. */
public record UncannyDevMenuResultPayload(String entryId, boolean success, String message, String targetName)
        implements CustomPacketPayload {
    public static final Type<UncannyDevMenuResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_dev_menu_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDevMenuResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(512),
                    UncannyDevMenuResultPayload::entryId,
                    ByteBufCodecs.BOOL,
                    UncannyDevMenuResultPayload::success,
                    ByteBufCodecs.stringUtf8(2048),
                    UncannyDevMenuResultPayload::message,
                    ByteBufCodecs.stringUtf8(64),
                    UncannyDevMenuResultPayload::targetName,
                    UncannyDevMenuResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
