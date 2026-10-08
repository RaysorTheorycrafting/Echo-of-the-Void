package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.entity.UncannyEntityUtil;
import com.eotv.echoofthevoid.entity.variant.ReplacementVariantExpansionSystem;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class UncannySlimeEntity extends Slime implements UncannyEntityMarker {
    public UncannySlimeEntity(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
        UncannyEntityUtil.applyDisplayName(this, "Slime?");
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
        // Preserve the visible Vanilla jump telegraph. The old shared ground-glide was the same
        // no-counterplay motion defect as Magma Cube?'s and affected all five catalog variants.
    }

    @Override
    protected float getSoundVolume() {
        return super.getSoundVolume() * 0.6F;
    }
}
