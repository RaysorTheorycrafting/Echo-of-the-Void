package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Starts or clears one private presentation made entirely from debug-style bounding boxes. */
public record UncannyDebugBoundsPayload(
        boolean active,
        long seed,
        int durationTicks,
        int presenceCount) implements CustomPacketPayload {
    public static final Type<UncannyDebugBoundsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_debug_bounds"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDebugBoundsPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyDebugBoundsPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyDebugBoundsPayload(
                            buffer.readBoolean(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyDebugBoundsPayload payload) {
                    buffer.writeBoolean(payload.active());
                    buffer.writeLong(payload.seed());
                    buffer.writeVarInt(payload.durationTicks());
                    buffer.writeVarInt(payload.presenceCount());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
