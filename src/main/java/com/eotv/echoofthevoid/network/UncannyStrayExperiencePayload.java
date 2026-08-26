package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared visual-only experience orbs moving toward an empty point. */
public record UncannyStrayExperiencePayload(
        int visualId,
        boolean active,
        double startX,
        double startY,
        double startZ,
        double targetX,
        double targetY,
        double targetZ,
        int orbCount,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyStrayExperiencePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_stray_experience"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyStrayExperiencePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyStrayExperiencePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyStrayExperiencePayload(
                            buffer.readVarInt(), buffer.readBoolean(),
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                            buffer.readVarInt(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyStrayExperiencePayload payload) {
                    buffer.writeVarInt(payload.visualId());
                    buffer.writeBoolean(payload.active());
                    buffer.writeDouble(payload.startX());
                    buffer.writeDouble(payload.startY());
                    buffer.writeDouble(payload.startZ());
                    buffer.writeDouble(payload.targetX());
                    buffer.writeDouble(payload.targetY());
                    buffer.writeDouble(payload.targetZ());
                    buffer.writeVarInt(payload.orbCount());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
