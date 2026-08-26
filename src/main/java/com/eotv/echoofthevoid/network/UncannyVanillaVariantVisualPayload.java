package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared, presentation-only cue for a Vanilla-derived variant. */
public record UncannyVanillaVariantVisualPayload(
        int entityId,
        String effectId,
        boolean active,
        double targetX,
        double targetY,
        double targetZ,
        int durationTicks,
        long seed) implements CustomPacketPayload {
    public static final Type<UncannyVanillaVariantVisualPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_vanilla_variant_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyVanillaVariantVisualPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyVanillaVariantVisualPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyVanillaVariantVisualPayload(
                            buffer.readVarInt(),
                            buffer.readUtf(96),
                            buffer.readBoolean(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readVarInt(),
                            buffer.readLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyVanillaVariantVisualPayload payload) {
                    buffer.writeVarInt(payload.entityId());
                    buffer.writeUtf(payload.effectId(), 96);
                    buffer.writeBoolean(payload.active());
                    buffer.writeDouble(payload.targetX());
                    buffer.writeDouble(payload.targetY());
                    buffer.writeDouble(payload.targetZ());
                    buffer.writeVarInt(payload.durationTicks());
                    buffer.writeLong(payload.seed());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
