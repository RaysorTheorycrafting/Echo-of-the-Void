package com.eotv.echoofthevoid.entity.variant;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyPhantomEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyWitherSkeletonEntity;
import com.eotv.echoofthevoid.event.passive.VanillaVariantBehaviorRuntime;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Runtime tag layer for the seventeen historical one-concept replacement classes. */
public final class ReplacementVariantExpansionSystem {
    public static final String TAG_VARIANT_INDEX = "UncannyReplacementVariant";
    public static final String TAG_VARIANT_ID = "UncannyReplacementVariantId";

    private ReplacementVariantExpansionSystem() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(event.getLevel() instanceof ServerLevel level)
                || !UncannyWorldState.get(level.getServer()).isSystemEnabled()) {
            return;
        }
        ensureInitialized(mob, level);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !UncannyWorldState.get(level.getServer()).isSystemEnabled()) {
            return;
        }
        ReplacementVariantExpansionCatalog.Species species = species(mob);
        if (species == null) {
            return;
        }
        ensureInitialized(mob, level);
        ReplacementVariantExpansionCatalog.Variant variant = variant(mob);
        if (variant != null) {
            VanillaVariantBehaviorRuntime.tickReplacement(
                    level, mob, variant.behaviorKind(), level.getGameTime());
        }
    }

    public static boolean applyForcedVariant(Mob mob, int index, long now) {
        ReplacementVariantExpansionCatalog.Species species = species(mob);
        ReplacementVariantExpansionCatalog.Variant variant = species == null
                ? null
                : ReplacementVariantExpansionCatalog.variant(species.entityTypePath(), index);
        if (variant == null) {
            return false;
        }
        apply(mob, variant, now, true);
        return true;
    }

    public static String variantId(Entity entity) {
        return entity == null ? "" : entity.getPersistentData().getString(TAG_VARIANT_ID);
    }

    public static int variantIndex(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getInt(TAG_VARIANT_INDEX);
    }

    /**
     * Historical replacement classes predate the five-way catalog and still contain their first
     * concept in the entity class itself. Only index 1 may run that delta; applying it to indices
     * 2-5 made every Magma Cube glide-charge and every Drowned surge regardless of its advertised
     * variant. Index 0 remains compatible with legacy/summoned entities that have not been tagged
     * yet.
     */
    public static boolean usesHistoricalSpecializedBehavior(Entity entity) {
        int index = variantIndex(entity);
        return index <= 1;
    }

    private static void ensureInitialized(Mob mob, ServerLevel level) {
        ReplacementVariantExpansionCatalog.Species species = species(mob);
        if (species == null) {
            return;
        }
        ReplacementVariantExpansionCatalog.Variant current = variant(mob);
        if (current != null && current.entityTypePath().equals(species.entityTypePath())) {
            return;
        }
        int phase = UncannyWorldState.get(level.getServer()).getPhase().index();
        long ticket = mob.getUUID().getMostSignificantBits()
                ^ mob.getUUID().getLeastSignificantBits()
                ^ level.getSeed()
                ^ level.getGameTime();
        ReplacementVariantExpansionCatalog.Variant selected =
                ReplacementVariantExpansionCatalog.selectVariant(species.entityTypePath(), phase, ticket);
        if (selected != null) {
            apply(mob, selected, level.getGameTime(), false);
        }
    }

    private static void apply(
            Mob mob,
            ReplacementVariantExpansionCatalog.Variant variant,
            long now,
            boolean dev) {
        CompoundTag data = mob.getPersistentData();
        data.putInt(TAG_VARIANT_INDEX, variant.index());
        data.putString(TAG_VARIANT_ID, variant.id());
        VanillaVariantBehaviorRuntime.initialize(
                mob,
                variant.id(),
                variant.behaviorKind(),
                variant.visualStyle(),
                false,
                now,
                dev);

        if (mob instanceof UncannyPhantomEntity phantom) {
            phantom.setLanternEaterMode(variant.index() == 2);
        } else if (mob instanceof UncannyWitherSkeletonEntity witherSkeleton) {
            witherSkeleton.setArcherVariantForExpansion((variant.index() & 1) == 1);
        }
    }

    private static ReplacementVariantExpansionCatalog.Variant variant(Mob mob) {
        String id = mob.getPersistentData().getString(TAG_VARIANT_ID);
        ReplacementVariantExpansionCatalog.Variant byId =
                ReplacementVariantExpansionCatalog.byId(id);
        if (byId != null) {
            return byId;
        }
        ReplacementVariantExpansionCatalog.Species species = species(mob);
        return species == null
                ? null
                : ReplacementVariantExpansionCatalog.variant(
                        species.entityTypePath(), mob.getPersistentData().getInt(TAG_VARIANT_INDEX));
    }

    private static ReplacementVariantExpansionCatalog.Species species(Mob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (key == null || !EchoOfTheVoid.MODID.equals(key.getNamespace())) {
            return null;
        }
        return ReplacementVariantExpansionCatalog.byEntityTypePath(key.getPath());
    }
}
