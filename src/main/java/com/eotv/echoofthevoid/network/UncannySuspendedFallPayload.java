package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared block-model presentation for Suspended Fall; the server block never moves. */
public record UncannySuspendedFallPayload(
        int visualId,
        boolean active,
        int blockStateId,
        double x,
        double y,
        double z,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannySuspendedFallPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_suspended_fall"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannySuspendedFallPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannySuspendedFallPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannySuspendedFallPayload(
                            buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannySuspendedFallPayload payload) {
                    buffer.writeVarInt(payload.visualId());
                    buffer.writeBoolean(payload.active());
                    buffer.writeVarInt(payload.blockStateId());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
