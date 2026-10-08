package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class UncannyMagmaCubeEntity extends MagmaCube implements UncannyEntityMarker {
    public UncannyMagmaCubeEntity(EntityType<? extends MagmaCube> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Magma Cube?");
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        this.setSize(1, true);
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setSilent(false);

        if (this.level().isClientSide()
                || !ReplacementVariantExpansionSystem.usesHistoricalSpecializedBehavior(this)) {
            return;
        }
        // The former "crawler" replaced every Vanilla jump with an untelegraphed 0.28-0.38
        // block/tick homing glide. Keeping Vanilla's visible jump arc restores counter-play; the
        // specialized variant remains visually distinct through its catalog presentation and
        // still uses the complete Magma Cube combat/splitting contract.
    }
}
