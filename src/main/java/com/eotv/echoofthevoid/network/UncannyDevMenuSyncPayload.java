package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UncannyDevMenuSyncPayload(String greenIds, String orangeIds, boolean openMenu) implements CustomPacketPayload {
    public static final Type<UncannyDevMenuSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_dev_menu_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDevMenuSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(32767),
                    UncannyDevMenuSyncPayload::greenIds,
                    ByteBufCodecs.stringUtf8(32767),
                    UncannyDevMenuSyncPayload::orangeIds,
                    ByteBufCodecs.BOOL,
                    UncannyDevMenuSyncPayload::openMenu,
                    UncannyDevMenuSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
