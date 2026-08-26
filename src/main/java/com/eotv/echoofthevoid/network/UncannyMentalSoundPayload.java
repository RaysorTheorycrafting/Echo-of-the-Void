package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A private, non-positional sound routed to one player's audio space. */
public record UncannyMentalSoundPayload(
        String soundId,
        String sourceName,
        float volume,
        float pitch,
        int maximumDurationTicks) implements CustomPacketPayload {
    public static final Type<UncannyMentalSoundPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_mental_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyMentalSoundPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyMentalSoundPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyMentalSoundPayload(
                            buffer.readUtf(256),
                            buffer.readUtf(32),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyMentalSoundPayload payload) {
                    buffer.writeUtf(payload.soundId(), 256);
                    buffer.writeUtf(payload.sourceName(), 32);
                    buffer.writeFloat(payload.volume());
                    buffer.writeFloat(payload.pitch());
                    buffer.writeVarInt(payload.maximumDurationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
