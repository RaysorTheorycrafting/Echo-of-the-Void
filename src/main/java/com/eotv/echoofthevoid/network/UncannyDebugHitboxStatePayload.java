package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client observation only; the server remains authoritative over whether the encounter starts. */
public record UncannyDebugHitboxStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<UncannyDebugHitboxStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_debug_hitbox_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyDebugHitboxStatePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyDebugHitboxStatePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyDebugHitboxStatePayload(buffer.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyDebugHitboxStatePayload payload) {
                    buffer.writeBoolean(payload.enabled());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
