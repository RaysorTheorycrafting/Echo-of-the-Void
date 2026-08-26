package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyApprovedSpecialEntity;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server integration checks for the common reward event used by every Special type. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class UncannySpecialRewardGameTests {
    private static final String TEMPLATE = "special_test_room";

    private UncannySpecialRewardGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void everySpecialTypeUsesTheSameCombatDropHandler(GameTestHelper helper) {
        List<EntityType<?>> specialTypes = List.of(
                UncannyEntityRegistry.UNCANNY_DOUBLE_DORMANT.get(),
                UncannyEntityRegistry.UNCANNY_WATCHER.get(),
                UncannyEntityRegistry.UNCANNY_STALKER.get(),
                UncannyEntityRegistry.UNCANNY_HURLER.get(),
                UncannyEntityRegistry.UNCANNY_SHADOW.get(),
                UncannyEntityRegistry.UNCANNY_KNOCKER.get(),
                UncannyEntityRegistry.UNCANNY_PULSE.get(),
                UncannyEntityRegistry.UNCANNY_TERROR.get(),
                UncannyEntityRegistry.UNCANNY_USHER.get(),
                UncannyEntityRegistry.UNCANNY_KEEPER.get(),
                UncannyEntityRegistry.UNCANNY_TENANT.get(),
                UncannyEntityRegistry.UNCANNY_FOLLOWER.get(),
                UncannyEntityRegistry.UNCANNY_SURVEYOR.get(),
                UncannyEntityRegistry.UNCANNY_MOURNER.get(),
                UncannyEntityRegistry.UNCANNY_DOUBLER.get(),
                UncannyEntityRegistry.UNCANNY_FERRYMAN.get(),
                UncannyEntityRegistry.UNCANNY_LISTENER.get(),
                UncannyEntityRegistry.UNCANNY_BYSTANDER.get());
        for (EntityType<?> type : specialTypes) {
            helper.assertTrue(UncannyEntityRegistry.isSpecialEntity(type),
                    "The common reward handler must classify " + type + " as a Special");
        }

        UncannyApprovedSpecialEntity special =
                UncannyEntityRegistry.UNCANNY_DOUBLER.get().create(helper.getLevel());
        helper.assertTrue(special != null, "A representative Special must be creatable");

        long rewardingSeed = findSeedProducingBothRewards(special);
        helper.assertTrue(rewardingSeed >= 0L,
                "The configured probabilities must have a deterministic seed producing both rewards");
        special.getRandom().setSeed(rewardingSeed);

        List<ItemEntity> drops = new ArrayList<>();
        LivingDropsEvent event = new LivingDropsEvent(
                special,
                helper.getLevel().damageSources().generic(),
                drops,
                true);
        UncannySpecialRewardSystem.onLivingDrops(event);
        helper.assertTrue(drops.stream().anyMatch(drop ->
                        drop.getItem().is(UncannyItemRegistry.UNCANNY_REALITY_SHARD.get())),
                "A successful full-shard roll must add an Uncanny Reality Shard");
        helper.assertTrue(drops.stream().anyMatch(drop ->
                        drop.getItem().is(UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get())),
                "A successful piece roll must add an Uncanny Reality Shard Piece");

        special.getRandom().setSeed(rewardingSeed);
        List<ItemEntity> environmentalDrops = new ArrayList<>();
        UncannySpecialRewardSystem.onLivingDrops(new LivingDropsEvent(
                special,
                helper.getLevel().damageSources().generic(),
                environmentalDrops,
                false));
        helper.assertTrue(environmentalDrops.isEmpty(),
                "Scripted or environmental disappearance must not create combat rewards");

        var vanillaZombie = EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(vanillaZombie != null, "A Vanilla control entity must be creatable");
        vanillaZombie.getRandom().setSeed(rewardingSeed);
        List<ItemEntity> vanillaDrops = new ArrayList<>();
        UncannySpecialRewardSystem.onLivingDrops(new LivingDropsEvent(
                vanillaZombie,
                helper.getLevel().damageSources().generic(),
                vanillaDrops,
                true));
        helper.assertTrue(vanillaDrops.isEmpty(),
                "The Special reward handler must never add shards to a Vanilla entity");
        helper.succeed();
    }

    private static long findSeedProducingBothRewards(UncannyApprovedSpecialEntity special) {
        for (long seed = 0L; seed < 10_000L; seed++) {
            special.getRandom().setSeed(seed);
            UncannySpecialRewardRules.RewardRoll result = UncannySpecialRewardRules.resolve(
                    special.getRandom().nextFloat(),
                    special.getRandom().nextFloat());
            if (result.shard() && result.shardPiece()) {
                return seed;
            }
        }
        return -1L;
    }
}
