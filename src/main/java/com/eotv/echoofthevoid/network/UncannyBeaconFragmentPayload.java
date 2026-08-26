package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared, bounded beacon beam segment with no backing beacon or block mutation. */
public record UncannyBeaconFragmentPayload(
        int visualId,
        boolean active,
        double x,
        double y,
        double z,
        int height,
        int color,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyBeaconFragmentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_beacon_fragment"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyBeaconFragmentPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyBeaconFragmentPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyBeaconFragmentPayload(
                            buffer.readVarInt(), buffer.readBoolean(),
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                            buffer.readVarInt(), buffer.readInt(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyBeaconFragmentPayload payload) {
                    buffer.writeVarInt(payload.visualId());
                    buffer.writeBoolean(payload.active());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeVarInt(payload.height());
                    buffer.writeInt(payload.color());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
