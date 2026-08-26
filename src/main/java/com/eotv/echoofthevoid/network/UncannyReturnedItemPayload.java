package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared, non-interactive rendering of an ordinary item already collected by a player. */
public record UncannyReturnedItemPayload(
        int visualId,
        boolean visible,
        ItemStack stack,
        double x,
        double y,
        double z,
        int durationTicks) implements CustomPacketPayload {
    public static final Type<UncannyReturnedItemPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_returned_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyReturnedItemPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyReturnedItemPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyReturnedItemPayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyReturnedItemPayload payload) {
                    buffer.writeVarInt(payload.visualId());
                    buffer.writeBoolean(payload.visible());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
