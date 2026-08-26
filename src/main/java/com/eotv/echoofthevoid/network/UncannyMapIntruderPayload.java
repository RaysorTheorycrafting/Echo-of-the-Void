package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Private presentation-only marker integrated into one held map renderer. */
public record UncannyMapIntruderPayload(
        int mapId,
        byte x,
        byte y,
        byte moveX,
        byte moveY,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyMapIntruderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_map_intruder"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyMapIntruderPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyMapIntruderPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyMapIntruderPayload(
                            buffer.readVarInt(),
                            buffer.readByte(),
                            buffer.readByte(),
                            buffer.readByte(),
                            buffer.readByte(),
                            buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyMapIntruderPayload payload) {
                    buffer.writeVarInt(payload.mapId());
                    buffer.writeByte(payload.x());
                    buffer.writeByte(payload.y());
                    buffer.writeByte(payload.moveX());
                    buffer.writeByte(payload.moveY());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
