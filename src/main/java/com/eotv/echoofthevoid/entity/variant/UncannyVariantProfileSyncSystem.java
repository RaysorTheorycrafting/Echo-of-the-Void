package com.eotv.echoofthevoid.entity.variant;

import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantSystem;
import com.eotv.echoofthevoid.event.passive.VanillaVariantBehaviorRuntime;
import com.eotv.echoofthevoid.network.UncannyVariantProfilePayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends persistent variant presentation when a client begins tracking the real entity. */
public final class UncannyVariantProfileSyncSystem {
    private static final String LEGACY_ENABLED = "UncannyPassiveEnabled";
    private static final String LEGACY_TYPE = "UncannyPassiveType";
    private static final String LEGACY_VARIANT = "UncannyPassiveVariant";

    private UncannyVariantProfileSyncSystem() {
    }

    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncProfile(player, event.getTarget());
        }
    }

    public static boolean syncProfile(ServerPlayer player, Entity entity) {
        if (player == null || entity == null) {
            return false;
        }
        String presentationTag = presentationTag(entity);
        if (presentationTag == null) {
            return false;
        }
        PacketDistributor.sendToPlayer(
                player, new UncannyVariantProfilePayload(entity.getId(), presentationTag));
        return true;
    }

    /** Null means no EOTV variant; an empty string means a real variant with normal geometry. */
    public static String presentationTag(Entity entity) {
        ApprovedVanillaVariantCatalog.Variant approved = ApprovedVanillaVariantCatalog.byId(
                ApprovedVanillaVariantSystem.variantId(entity));
        if (approved != null) {
            return approved.visualStyle() == ApprovedVanillaVariantCatalog.VisualStyle.NORMAL
                    ? ""
                    : VanillaVariantBehaviorRuntime.visualTag(approved.visualStyle());
        }

        ReplacementVariantExpansionCatalog.Variant replacement =
                ReplacementVariantExpansionCatalog.byId(
                        ReplacementVariantExpansionSystem.variantId(entity));
        if (replacement != null) {
            return replacement.visualStyle() == ApprovedVanillaVariantCatalog.VisualStyle.NORMAL
                    ? ""
                    : VanillaVariantBehaviorRuntime.visualTag(replacement.visualStyle());
        }

        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(LEGACY_ENABLED)) {
            String type = data.getString(LEGACY_TYPE);
            int variant = data.getInt(LEGACY_VARIANT);
            if (!type.isBlank() && variant >= 1 && variant <= 5) {
                return "eotv_passive_" + type + "_v" + variant;
            }
        }
        return null;
    }
}
