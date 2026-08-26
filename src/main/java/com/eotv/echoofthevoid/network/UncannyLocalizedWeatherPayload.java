package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Parameters for one shared, presentation-only localized weather anomaly. */
public record UncannyLocalizedWeatherPayload(
        String eventId,
        boolean active,
        double centerX,
        double centerY,
        double centerZ,
        double directionX,
        double directionZ,
        int radius,
        long seed,
        int elapsedTicks,
        int remainingTicks,
        String data) implements CustomPacketPayload {
    public static final Type<UncannyLocalizedWeatherPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_localized_weather"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyLocalizedWeatherPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyLocalizedWeatherPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyLocalizedWeatherPayload(
                            buffer.readUtf(64), buffer.readBoolean(),
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                            buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                            buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(512));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyLocalizedWeatherPayload payload) {
                    buffer.writeUtf(payload.eventId(), 64);
                    buffer.writeBoolean(payload.active());
                    buffer.writeDouble(payload.centerX());
                    buffer.writeDouble(payload.centerY());
                    buffer.writeDouble(payload.centerZ());
                    buffer.writeDouble(payload.directionX());
                    buffer.writeDouble(payload.directionZ());
                    buffer.writeVarInt(payload.radius());
                    buffer.writeLong(payload.seed());
                    buffer.writeVarInt(payload.elapsedTicks());
                    buffer.writeVarInt(payload.remainingTicks());
                    buffer.writeUtf(payload.data(), 512);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
