package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared presentation of a vanilla-style lead whose far end has no entity. */
public record UncannyEmptyLeadPayload(
        int visualId,
        boolean visible,
        double anchorX,
        double anchorY,
        double anchorZ,
        double endX,
        double endY,
        double endZ,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyEmptyLeadPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_empty_lead"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyEmptyLeadPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyEmptyLeadPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyEmptyLeadPayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyEmptyLeadPayload payload) {
                    buffer.writeVarInt(payload.visualId());
                    buffer.writeBoolean(payload.visible());
                    buffer.writeDouble(payload.anchorX());
                    buffer.writeDouble(payload.anchorY());
                    buffer.writeDouble(payload.anchorZ());
                    buffer.writeDouble(payload.endX());
                    buffer.writeDouble(payload.endY());
                    buffer.writeDouble(payload.endZ());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
