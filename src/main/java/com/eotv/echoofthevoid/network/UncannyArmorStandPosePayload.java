package com.eotv.echoofthevoid.network;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Shared, reversible client illusion for one real armor stand. Variant -1 restores its captured pose. */
public record UncannyArmorStandPosePayload(int entityId, int variant, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<UncannyArmorStandPosePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EchoOfTheVoid.MODID, "uncanny_armor_stand_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UncannyArmorStandPosePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UncannyArmorStandPosePayload::entityId,
                    ByteBufCodecs.VAR_INT,
                    UncannyArmorStandPosePayload::variant,
                    ByteBufCodecs.VAR_INT,
                    UncannyArmorStandPosePayload::durationTicks,
                    UncannyArmorStandPosePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
