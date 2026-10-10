package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A physical sound attached to an entity: it travels with the body while it moves. Vanilla's own
 * entity sound is dropped for silent entities, and the Specials stay silent to mute their Vanilla
 * voice, so their cries go through this payload. The coordinates are a fallback when the client
 * does not track the entity.
 */
public record UncannyEntitySoundPayload(
        int entityId,
        String soundId,
        String sourceName,
        float volume,
        float pitch,
        long seed,
        double x,
        double y,
        double z) implements CustomPacketPayload {
    public static final Type<UncannyEntitySoundPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_entity_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyEntitySoundPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyEntitySoundPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyEntitySoundPayload(
                            buffer.readVarInt(),
                            buffer.readUtf(256),
                            buffer.readUtf(32),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readLong(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyEntitySoundPayload payload) {
                    buffer.writeVarInt(payload.entityId());
                    buffer.writeUtf(payload.soundId(), 256);
                    buffer.writeUtf(payload.sourceName(), 32);
                    buffer.writeFloat(payload.volume());
                    buffer.writeFloat(payload.pitch());
                    buffer.writeLong(payload.seed());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
