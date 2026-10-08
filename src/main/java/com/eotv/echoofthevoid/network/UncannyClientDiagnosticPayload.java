package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded, diagnostic-only client report. It is never used as gameplay authority. */
public record UncannyClientDiagnosticPayload(
        String severity,
        String code,
        String message,
        String context) implements CustomPacketPayload {
    public static final Type<UncannyClientDiagnosticPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_client_diagnostic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyClientDiagnosticPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(16),
                    UncannyClientDiagnosticPayload::severity,
                    ByteBufCodecs.stringUtf8(96),
                    UncannyClientDiagnosticPayload::code,
                    ByteBufCodecs.stringUtf8(8192),
                    UncannyClientDiagnosticPayload::message,
                    ByteBufCodecs.stringUtf8(2048),
                    UncannyClientDiagnosticPayload::context,
                    UncannyClientDiagnosticPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
