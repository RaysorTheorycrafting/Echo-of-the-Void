package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared client presentation of one already-embedded arrow's temporary orientation. */
public record UncannyArrowGazePayload(
        int entityId,
        boolean active,
        float yaw,
        float pitch,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyArrowGazePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_arrow_gaze"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyArrowGazePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyArrowGazePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyArrowGazePayload(
                            buffer.readVarInt(), buffer.readBoolean(), buffer.readFloat(),
                            buffer.readFloat(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyArrowGazePayload payload) {
                    buffer.writeVarInt(payload.entityId());
                    buffer.writeBoolean(payload.active());
                    buffer.writeFloat(payload.yaw());
                    buffer.writeFloat(payload.pitch());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
