package com.eotv.echoofthevoid.world.lifecycle_mixin;

import com.eotv.echoofthevoid.world.UncannyDimensionLifecyclePolicy;
import com.mojang.serialization.Lifecycle;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Corrects Vanilla's blanket experimental lifecycle for the one supported EOTV
 * dimension set. Genuine experimental datapacks and foreign dimensions remain
 * experimental and continue to display Minecraft's warning.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsLifecycleMixin {
    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private void echoofthevoid$stabilizeElsewhere(
            Registry<LevelStem> ignoredSourceRegistry,
            CallbackInfoReturnable<WorldDimensions.Complete> callback) {
        WorldDimensions.Complete original = callback.getReturnValue();
        Registry<LevelStem> dimensions = original.dimensions();
        if (dimensions.registryLifecycle() == Lifecycle.stable()) {
            return;
        }

        Set<String> dimensionIds = dimensions.registryKeySet().stream()
                .map(key -> key.location().toString())
                .collect(Collectors.toUnmodifiableSet());
        Set<String> unstableDimensionIds = dimensions.registryKeySet().stream()
                .filter(key -> dimensions.registrationInfo(key)
                        .map(RegistrationInfo::lifecycle)
                        .orElse(Lifecycle.experimental()) != Lifecycle.stable())
                .map(key -> key.location().toString())
                .collect(Collectors.toUnmodifiableSet());
        if (!UncannyDimensionLifecyclePolicy.canMarkAsStable(dimensionIds, unstableDimensionIds)) {
            return;
        }

        WritableRegistry<LevelStem> stableDimensions =
                new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
        dimensions.holders().forEach(holder -> {
            RegistrationInfo previous = dimensions.registrationInfo(holder.key())
                    .orElse(RegistrationInfo.BUILT_IN);
            stableDimensions.register(
                    holder.key(),
                    holder.value(),
                    new RegistrationInfo(previous.knownPackInfo(), Lifecycle.stable()));
        });
        callback.setReturnValue(new WorldDimensions.Complete(
                stableDimensions.freeze(), original.specialWorldProperty()));
    }
}
