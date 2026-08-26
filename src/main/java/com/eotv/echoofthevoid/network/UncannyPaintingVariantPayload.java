package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;

/** Shared temporary painting motive; the authoritative painting entity is never modified. */
public record UncannyPaintingVariantPayload(
        int entityId,
        Holder<PaintingVariant> variant,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyPaintingVariantPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_painting_variant"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyPaintingVariantPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyPaintingVariantPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyPaintingVariantPayload(
                            buffer.readVarInt(),
                            PaintingVariant.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyPaintingVariantPayload payload) {
                    buffer.writeVarInt(payload.entityId());
                    PaintingVariant.STREAM_CODEC.encode(buffer, payload.variant());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
