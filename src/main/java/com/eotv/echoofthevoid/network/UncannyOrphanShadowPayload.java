package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared spawn/removal instruction for a true vanilla-textured entity shadow without an entity. */
public record UncannyOrphanShadowPayload(
        int shadowId,
        boolean visible,
        double x,
        double y,
        double z,
        float radius,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyOrphanShadowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_orphan_shadow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyOrphanShadowPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyOrphanShadowPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyOrphanShadowPayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readFloat(),
                            buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyOrphanShadowPayload payload) {
                    buffer.writeVarInt(payload.shadowId());
                    buffer.writeBoolean(payload.visible());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeFloat(payload.radius());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
