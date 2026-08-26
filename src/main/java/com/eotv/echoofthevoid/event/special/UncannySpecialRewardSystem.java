package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Applies one balanced reward contract to every real Special combat death. */
public final class UncannySpecialRewardSystem {
    public static final String GRAND_WARDEN_TAG = GrandWardenRules.ENTITY_TAG;

    private UncannySpecialRewardSystem() {
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)
                || !event.isRecentlyHit()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            return;
        }

        if (entity instanceof Warden && entity.getTags().contains(GRAND_WARDEN_TAG)) {
            int range = UncannySpecialRewardRules.GRAND_WARDEN_MAX_SHARDS
                    - UncannySpecialRewardRules.GRAND_WARDEN_MIN_SHARDS + 1;
            int count = UncannySpecialRewardRules.grandWardenShardCount(entity.getRandom().nextInt(range));
            event.getDrops().add(dropAt(
                    entity,
                    new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get(), count)));
            return;
        }

        if (!UncannyEntityRegistry.isSpecialEntity(entity.getType())) {
            return;
        }

        addRewards(
                event,
                UncannySpecialRewardRules.resolve(
                        entity.getRandom().nextFloat(),
                        entity.getRandom().nextFloat()));
    }

    static void addRewards(
            LivingDropsEvent event,
            UncannySpecialRewardRules.RewardRoll rewards) {
        LivingEntity entity = event.getEntity();
        if (rewards.shard()) {
            event.getDrops().add(dropAt(
                    entity,
                    new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get())));
        }
        if (rewards.shardPiece()) {
            event.getDrops().add(dropAt(
                    entity,
                    new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get())));
        }
    }

    private static ItemEntity dropAt(LivingEntity entity, ItemStack stack) {
        return new ItemEntity(
                entity.level(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                stack);
    }
}
