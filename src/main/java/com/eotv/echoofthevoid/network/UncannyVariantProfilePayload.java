package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Synchronizes one validated, presentation-only variant tag to a tracking client. */
public record UncannyVariantProfilePayload(int entityId, String presentationTag)
        implements CustomPacketPayload {
    public static final Type<UncannyVariantProfilePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_variant_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyVariantProfilePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UncannyVariantProfilePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new UncannyVariantProfilePayload(buffer.readVarInt(), buffer.readUtf(96));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UncannyVariantProfilePayload payload) {
                    buffer.writeVarInt(payload.entityId());
                    buffer.writeUtf(payload.presentationTag(), 96);
                }
            };

    public UncannyVariantProfilePayload {
        presentationTag = presentationTag == null ? "" : presentationTag;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
