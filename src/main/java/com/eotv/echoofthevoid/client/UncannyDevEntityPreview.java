package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionCatalog;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog;
import com.eotv.echoofthevoid.event.passive.VanillaVariantBehaviorRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Creates a detached client entity for visual QA only; it is never added to the client level. */
final class UncannyDevEntityPreview {
    private UncannyDevEntityPreview() {
    }

    static LivingEntity create(UncannyDevCatalog.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entry == null || minecraft.level == null) {
            return null;
        }

        EntityType<?> type = resolveType(entry);
        if (type == null) {
            return null;
        }
        Entity created = type.create(minecraft.level);
        if (!(created instanceof LivingEntity living)) {
            return null;
        }
        if (living instanceof Mob mob) {
            mob.setNoAi(true);
        }
        applyVariant(entry, living);
        return living;
    }

    private static EntityType<?> resolveType(UncannyDevCatalog.Entry entry) {
        return switch (entry.actionKind()) {
            case SPAWN_UNCANNY, SPAWN_UNCANNY_FORCED ->
                    UncannyEntityRegistry.byCommandType(firstPart(entry.actionArg()));
            case SPAWN_SPECIAL -> UncannyEntityRegistry.byCommandType(firstPart(entry.actionArg()));
            case FORCE_MIMIC -> UncannyEntityRegistry.byCommandType("uncanny_mimic");
            case SPAWN_PASSIVE_FORCED -> resolvePassiveType(entry.actionArg());
            default -> null;
        };
    }

    private static void applyVariant(UncannyDevCatalog.Entry entry, LivingEntity entity) {
        if (entry.actionKind() == UncannyDevCatalog.ActionKind.SPAWN_UNCANNY_FORCED && entity instanceof Mob mob) {
            String[] parts = entry.actionArg().split("\\|");
            if (parts.length < 3) {
                return;
            }
            if (parts.length >= 4 && "replacement".equalsIgnoreCase(parts[3])) {
                try {
                    ReplacementVariantExpansionCatalog.Variant variant =
                            ReplacementVariantExpansionCatalog.variant(parts[0], Integer.parseInt(parts[2]));
                    applyVisualStyle(entity, variant == null ? null : variant.visualStyle());
                } catch (NumberFormatException ignored) {
                    // Invalid catalog data deliberately produces the undecorated fallback preview.
                }
                return;
            }
            CompoundTag tag = new CompoundTag();
            mob.addAdditionalSaveData(tag);
            if (parts.length >= 4 && "bool".equalsIgnoreCase(parts[3])) {
                tag.putBoolean(parts[1], Boolean.parseBoolean(parts[2]));
            } else {
                try {
                    tag.putInt(parts[1], Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                    return;
                }
            }
            mob.readAdditionalSaveData(tag);
            return;
        }

        if (entry.actionKind() == UncannyDevCatalog.ActionKind.SPAWN_PASSIVE_FORCED) {
            String[] parts = entry.actionArg().split("\\|", 2);
            if (parts.length != 2) {
                return;
            }
            if ("approved".equals(parts[0])) {
                ApprovedVanillaVariantCatalog.Variant variant =
                        ApprovedVanillaVariantCatalog.byId(parts[1]);
                if (variant != null) {
                    entity.getPersistentData().putString("UncannyApprovedVanillaVariant", parts[1]);
                    applyVisualStyle(entity, variant.visualStyle());
                    entity.setSilent(variant.silent());
                }
                return;
            }
            try {
                int variant = Integer.parseInt(parts[1]);
                if (variant > 0) {
                    entity.addTag("eotv_passive_" + parts[0] + "_v" + Math.min(5, variant));
                }
            } catch (NumberFormatException ignored) {
                // Random-variant entries intentionally show the vanilla base model.
            }
        }
    }

    private static void applyVisualStyle(
            LivingEntity entity,
            ApprovedVanillaVariantCatalog.VisualStyle visualStyle) {
        if (visualStyle != null && visualStyle != ApprovedVanillaVariantCatalog.VisualStyle.NORMAL) {
            entity.addTag(VanillaVariantBehaviorRuntime.visualTag(visualStyle));
        }
    }

    private static String firstPart(String value) {
        if (value == null) {
            return "";
        }
        int separator = value.indexOf('|');
        return separator < 0 ? value : value.substring(0, separator);
    }

    private static EntityType<?> resolvePassiveType(String actionArg) {
        String[] parts = actionArg == null ? new String[0] : actionArg.split("\\|", 2);
        if (parts.length == 2) {
            String approvedType = null;
            if ("approved".equals(parts[0])) {
                ApprovedVanillaVariantCatalog.Variant variant = ApprovedVanillaVariantCatalog.byId(parts[1]);
                approvedType = variant == null ? null : variant.typeKey();
            } else if ("approved_random".equals(parts[0])) {
                ApprovedVanillaVariantCatalog.Species species =
                        ApprovedVanillaVariantCatalog.speciesByTypeKey(parts[1]);
                approvedType = species == null ? null : species.typeKey();
            }
            if (approvedType != null) {
                return BuiltInRegistries.ENTITY_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("minecraft", approvedType));
            }
        }
        return BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", firstPart(actionArg)));
    }
}
