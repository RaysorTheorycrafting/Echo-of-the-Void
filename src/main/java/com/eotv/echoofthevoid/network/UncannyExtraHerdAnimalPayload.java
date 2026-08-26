package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared visual-only adult animal following a real herd member. */
public record UncannyExtraHerdAnimalPayload(
        int visualId,
        boolean active,
        int anchorEntityId,
        String entityTypeId,
        double offsetX,
        double offsetY,
        double offsetZ,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyExtraHerdAnimalPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_extra_herd_animal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyExtraHerdAnimalPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyExtraHerdAnimalPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyExtraHerdAnimalPayload(
                            buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readUtf(128),
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyExtraHerdAnimalPayload payload) {
                    buffer.writeVarInt(payload.visualId());
                    buffer.writeBoolean(payload.active());
                    buffer.writeVarInt(payload.anchorEntityId());
                    buffer.writeUtf(payload.entityTypeId(), 128);
                    buffer.writeDouble(payload.offsetX());
                    buffer.writeDouble(payload.offsetY());
                    buffer.writeDouble(payload.offsetZ());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
