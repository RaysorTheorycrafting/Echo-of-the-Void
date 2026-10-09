package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyAshwalkerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDredgerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDrifterEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDrownedEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyEchoerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyFlankerEntity;
import com.eotv.echoofthevoid.event.special.HuntingSpecialRules;
import com.eotv.echoofthevoid.event.special.UncannyHuntingSpecialSystem;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Server integration checks for the five persistent hunting Specials. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class HuntingSpecialGameTests {
    private static final String TEMPLATE = "special_test_room";

    private HuntingSpecialGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void echoerCapsEveryPlayerAttackBeforeAggroAndPersistsFlee(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        UncannyEchoerEntity echoer = UncannyEntityRegistry.UNCANNY_ECHOER.get().create(helper.getLevel());
        helper.assertTrue(echoer != null, "Echoer? must be registered and creatable");
        echoer.moveTo(helper.absoluteVec(new Vec3(5.5D, 1.0D, 8.5D)));
        echoer.setupTarget(player, 6_000);
        helper.assertTrue(helper.getLevel().addFreshEntity(echoer), "Echoer? must enter the ServerLevel");

        float before = echoer.getHealth();
        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        helper.assertTrue(echoer.hurt(helper.getLevel().damageSources().arrow(arrow, player), 100.0F),
                "A player projectile must remain a valid pre-aggro attack");
        helper.assertTrue(before - echoer.getHealth() <= 4.001F,
                "Every player-owned attack must be capped at four raw damage before aggro");
        helper.assertTrue(echoer.echoerState() == UncannyEchoerEntity.State.FLEEING,
                "A pre-aggro player attack must start a real retreat");

        CompoundTag saved = new CompoundTag();
        echoer.saveWithoutId(saved);
        UncannyEchoerEntity reloaded = UncannyEntityRegistry.UNCANNY_ECHOER.get().create(helper.getLevel());
        helper.assertTrue(reloaded != null, "Echoer? must be creatable for reload validation");
        reloaded.load(saved);
        helper.assertTrue(reloaded.echoerState() == UncannyEchoerEntity.State.FLEEING,
                "Echoer?'s retreat state must survive entity reload");
        helper.assertTrue(saved.contains("HuntingRemainingLifetime"),
                "The remaining encounter lifetime must be persisted instead of resetting after reload");
        echoer.discard();
        reloaded.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void echoerKeepsClosingDuringItsAttackTelegraph(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(12.5D, 1.0D, 8.5D));
        player.setInvulnerable(true);
        UncannyEchoerEntity echoer = UncannyEntityRegistry.UNCANNY_ECHOER.get().create(helper.getLevel());
        helper.assertTrue(echoer != null, "Echoer? must be registered and creatable");
        echoer.moveTo(helper.absoluteVec(new Vec3(5.5D, 1.0D, 8.5D)));
        echoer.setupTarget(player, 6_000);
        echoer.forceAggroForDebug();
        double initialDistance = echoer.distanceTo(player);
        helper.assertTrue(helper.getLevel().addFreshEntity(echoer), "Echoer? must enter the ServerLevel");

        helper.runAtTickTime(12, () -> {
            helper.assertTrue(echoer.echoerState() == UncannyEchoerEntity.State.AGGRO,
                    "The reaction window must not cancel the definitive aggro state");
            helper.assertTrue(echoer.distanceTo(player) < initialDistance - 0.35D,
                    "Echoer? must keep advancing while its twenty-tick cry protects the player from damage");
            echoer.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 235)
    public static void drifterHasARealFiniteLandReserve(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(8, y, 8, Blocks.STONE);
        }
        ServerPlayer player = playerAt(helper, new Vec3(12.5D, 1.0D, 8.5D));
        UncannyDrifterEntity drifter = UncannyEntityRegistry.UNCANNY_DRIFTER.get().create(helper.getLevel());
        helper.assertTrue(drifter != null, "Drifter? must be registered and creatable");
        drifter.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.0D, 8.5D)));
        drifter.setupTarget(player, 6_000);
        drifter.forceDryForDebug();
        float initialHealth = drifter.getHealth();
        helper.assertTrue(helper.getLevel().addFreshEntity(drifter), "Drifter? must enter the ServerLevel");

        helper.runAtTickTime(25, () -> helper.assertTrue(drifter.getHealth() < initialHealth,
                "The QA dry state starts one second before the ten-second limit and must expose dry damage"));
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(drifter.isAlive(), "One dry pulse must not arbitrarily kill Drifter?");
            CompoundTag saved = new CompoundTag();
            drifter.saveWithoutId(saved);
            helper.assertTrue(saved.getInt("DrifterDryTicks") >= 200,
                    "The depleted land reserve must be persisted");
            drifter.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 110)
    public static void drifterPhysicallyClosesAndAttacksUnderwater(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        fillDeepWater(helper, 2, 13, 2, 13, 1, 10);
        ServerPlayer player = playerAt(helper, new Vec3(9.5D, 6.0D, 8.5D));
        float initialHealth = player.getHealth();
        UncannyDrifterEntity drifter = UncannyEntityRegistry.UNCANNY_DRIFTER.get().create(helper.getLevel());
        helper.assertTrue(drifter != null, "Drifter? must be registered and creatable");
        drifter.moveTo(helper.absoluteVec(new Vec3(4.5D, 6.0D, 8.5D)));
        drifter.setupTarget(player, 6_000);
        drifter.forceLungeForDebug();
        double initialDistance = drifter.distanceTo(player);
        helper.assertTrue(helper.getLevel().addFreshEntity(drifter),
                "Drifter? must enter the deep-water fixture");

        // Embedded GameTest players retain Vanilla's sixty-tick join protection. Assert after it
        // expires so the test observes real survival damage instead of a rejected correct hit.
        helper.runAtTickTime(75, () -> {
            helper.assertTrue(drifter.isAlive()
                            && drifter.getAirSupply() == drifter.getMaxAirSupply(),
                    "Drifter? must remain alive with full air in its required habitat");
            helper.assertTrue(drifter.distanceTo(player) < initialDistance - 1.5D,
                    "Drifter? must physically close the underwater distance");
            helper.assertTrue(player.getHealth() < initialHealth,
                    "A forced offensive lunge must reach and attack a stationary underwater player");
            drifter.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void ashwalkerProjectileDamageTriggersBoundedPersistentSubmersion(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        helper.setBlock(6, 1, 8, Blocks.LAVA);
        ServerPlayer player = playerAt(helper, new Vec3(10.5D, 1.0D, 8.5D));
        UncannyAshwalkerEntity ashwalker = UncannyEntityRegistry.UNCANNY_ASHWALKER.get().create(helper.getLevel());
        helper.assertTrue(ashwalker != null, "Ashwalker? must be registered and creatable");
        ashwalker.moveTo(helper.absoluteVec(new Vec3(6.5D, 1.2D, 8.5D)));
        ashwalker.setupTarget(player, 6_000);
        helper.assertTrue(helper.getLevel().addFreshEntity(ashwalker), "Ashwalker? must enter the ServerLevel");
        float before = ashwalker.getHealth();
        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        helper.assertTrue(ashwalker.hurt(helper.getLevel().damageSources().arrow(arrow, player), 2.0F),
                "Ashwalker? must take projectile damage normally");
        helper.assertTrue(ashwalker.getHealth() < before && ashwalker.isSubmerged(),
                "The same projectile must both deal damage and make Ashwalker? dive");
        CompoundTag saved = new CompoundTag();
        ashwalker.saveWithoutId(saved);
        helper.assertTrue(saved.getInt("AshwalkerSubmergedTicks") >= 60
                        && saved.getInt("AshwalkerSubmergedTicks") <= 100,
                "Projectile submersion must remain between three and five seconds and persist");
        ashwalker.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void ashwalkerPhysicallyLeavesLavaForAOneBlockShore(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        for (int x = 4; x <= 8; x++) {
            helper.setBlock(x, 1, 8, Blocks.LAVA);
        }
        helper.setBlock(9, 1, 8, Blocks.STONE);
        ServerPlayer player = playerAt(helper, new Vec3(9.5D, 2.0D, 8.5D));
        player.setInvulnerable(true);
        UncannyAshwalkerEntity ashwalker = UncannyEntityRegistry.UNCANNY_ASHWALKER.get().create(helper.getLevel());
        helper.assertTrue(ashwalker != null, "Ashwalker? must be registered and constructible");
        Vec3 start = helper.absoluteVec(new Vec3(5.5D, 1.12D, 8.5D));
        ashwalker.moveTo(start.x, start.y, start.z, 270.0F, 0.0F);
        ashwalker.setupTarget(player, 6_000);
        helper.assertTrue(helper.getLevel().addFreshEntity(ashwalker),
                "Ashwalker? must enter the connected lava fixture");

        boolean[] excursionObserved = {false};
        boolean[] fullHitboxObserved = {false};
        double[] maximumY = {start.y};
        for (int tick = 1; tick <= 100; tick++) {
            helper.runAtTickTime(tick, () -> {
                maximumY[0] = Math.max(maximumY[0], ashwalker.getY());
                boolean excursion = ashwalker.motionState() == UncannyAshwalkerEntity.MotionState.LUNGING
                        || ashwalker.motionState() == UncannyAshwalkerEntity.MotionState.RETURNING;
                excursionObserved[0] |= excursion;
                fullHitboxObserved[0] |= excursion && ashwalker.getBbHeight() >= 2.19F;
            });
        }
        helper.runAtTickTime(105, () -> {
            helper.assertTrue(excursionObserved[0],
                    "A reachable one-block shore must start a real Ashwalker? excursion");
            helper.assertTrue(fullHitboxObserved[0],
                    "The complete visible body must own a matching hitbox outside lava");
            helper.assertTrue(maximumY[0] >= start.y + 0.30D,
                    "The excursion must contain a physical upward jump rather than a render-only pose");
            ashwalker.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void ashwalkerUsesOnlyAConnectedLoadedLavaGraphInTheRealNether(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "The GameTest server must expose a real Nether level");

        BlockPos origin = new BlockPos(
                nether.getSharedSpawnPos().getX() & ~15,
                Math.max(nether.getMinBuildHeight() + 40, 64),
                nether.getSharedSpawnPos().getZ() & ~15).offset(2, 0, 2);
        nether.getChunkAt(origin); // Test fixture setup; the runtime search itself must never do this.
        List<BlockState> originals = new java.util.ArrayList<>();
        for (int step = 0; step < 10; step++) {
            BlockPos lava = origin.offset(step, 0, 0);
            originals.add(nether.getBlockState(lava));
            originals.add(nether.getBlockState(lava.above()));
            nether.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
            nether.setBlockAndUpdate(lava.above(), Blocks.AIR.defaultBlockState());
        }

        List<BlockPos> route = UncannyHuntingSpecialSystem.findConnectedLavaRoute(
                nether, origin, origin.offset(20, 0, 0), 512);
        boolean validRoute = route != null
                && route.size() == 10
                && route.stream().allMatch(pos -> nether.hasChunkAt(pos)
                        && nether.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA));

        int originalIndex = 0;
        for (int step = 0; step < 10; step++) {
            BlockPos lava = origin.offset(step, 0, 0);
            nether.setBlockAndUpdate(lava, originals.get(originalIndex++));
            nether.setBlockAndUpdate(lava.above(), originals.get(originalIndex++));
        }

        BlockPos deliberatelyUnloaded = new BlockPos(20_000_000, 64, 20_000_000);
        boolean unloadedBefore = !nether.hasChunkAt(deliberatelyUnloaded);
        List<BlockPos> absentRoute = UncannyHuntingSpecialSystem.findConnectedLavaRoute(
                nether, deliberatelyUnloaded, deliberatelyUnloaded.offset(8, 0, 0), 512);
        helper.assertTrue(validRoute,
                "Ashwalker? must traverse a bounded connected surface graph in the actual Nether");
        helper.assertTrue(unloadedBefore && absentRoute == null && !nether.hasChunkAt(deliberatelyUnloaded),
                "Ashwalker?'s path search must not load an absent Nether chunk");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void dredgerGrabIsReleasedByThreeAcceptedPlayerHits(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        fillDeepWater(helper, 5, 11, 5, 11, 1, 10);
        ServerPlayer victim = playerAt(helper, new Vec3(8.5D, 8.0D, 8.5D));
        ServerPlayer ally = playerAt(helper, new Vec3(9.5D, 8.0D, 8.5D));
        UncannyDredgerEntity dredger = UncannyEntityRegistry.UNCANNY_DREDGER.get().create(helper.getLevel());
        helper.assertTrue(dredger != null, "Dredger? must be registered and creatable");
        dredger.moveTo(helper.absoluteVec(new Vec3(8.5D, 2.0D, 8.5D)));
        dredger.setupTarget(victim, 6_000);
        helper.assertTrue(dredger.forceGrabForDebug(helper.getLevel(), victim),
                "A ten-block water column must provide a valid seabed pull anchor");
        Vec3 beforeRejectedKnockback = dredger.getDeltaMovement();
        dredger.knockback(2.0D, 1.0D, 0.0D);
        helper.assertTrue(dredger.getDeltaMovement().equals(beforeRejectedKnockback),
                "Hits must count toward release without detaching the Dredger? from its visible grip");
        helper.assertTrue(helper.getLevel().addFreshEntity(dredger), "Dredger? must enter the ServerLevel");

        helper.runAtTickTime(1, () -> helper.assertTrue(
                dredger.hurt(helper.getLevel().damageSources().playerAttack(victim), 1.0F),
                "The victim's first hit must count"));
        helper.runAtTickTime(13, () -> helper.assertTrue(
                dredger.hurt(helper.getLevel().damageSources().playerAttack(ally), 1.0F),
                "An ally's second hit must count"));
        helper.runAtTickTime(25, () -> helper.assertTrue(
                dredger.hurt(helper.getLevel().damageSources().playerAttack(victim), 1.0F),
                "The victim's third hit must count"));
        helper.runAtTickTime(28, () -> {
            helper.assertTrue(dredger.grabbedPlayerId().isEmpty()
                            && dredger.dredgerState() == UncannyDredgerEntity.State.CHASING,
                    "Exactly three accepted player hits must release the victim");
            CompoundTag saved = new CompoundTag();
            dredger.saveWithoutId(saved);
            helper.assertTrue(saved.getInt("DredgerRegrabCooldown") > 0,
                    "The six-second anti-regrab window must persist");
            dredger.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void dredgerPullRemainsAttachedThroughItsDamagePulse(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        fillDeepWater(helper, 5, 11, 5, 11, 1, 10);
        ServerPlayer victim = playerAt(helper, new Vec3(8.5D, 7.0D, 8.5D));
        float initialHealth = victim.getHealth();
        UncannyDredgerEntity dredger = UncannyEntityRegistry.UNCANNY_DREDGER.get().create(helper.getLevel());
        helper.assertTrue(dredger != null, "Dredger? must be registered and creatable");
        dredger.moveTo(helper.absoluteVec(new Vec3(8.5D, 2.0D, 8.5D)));
        dredger.setupTarget(victim, 6_000);
        helper.assertTrue(dredger.forceGrabForDebug(helper.getLevel(), victim),
                "The real water fixture must admit a debug grapple");
        helper.assertTrue(helper.getLevel().addFreshEntity(dredger),
                "Dredger? must enter the deep-water fixture");

        // The first forty-tick pulse is correctly rejected by Vanilla join protection. The second
        // pulse is the first one that can damage a freshly created mock survival player.
        helper.runAtTickTime(85, () -> {
            Vec3 towardVisibleSource = dredger.position().add(0.0D, 0.55D, 0.0D)
                    .subtract(victim.position());
            helper.assertTrue(dredger.grabbedPlayerId().filter(victim.getUUID()::equals).isPresent(),
                    "The grapple must remain attached to the same visible Dredger?");
            helper.assertTrue(dredger.distanceTo(victim) <= HuntingSpecialRules.DREDGER_GRAB_BREAK_DISTANCE + 0.01D,
                    "Dredger? may not leave a remotely drowning victim behind");
            helper.assertTrue(victim.getDeltaMovement().dot(towardVisibleSource) >= -1.0E-5D,
                    "The periodic grapple damage must not knock the victim away from Dredger?");
            helper.assertTrue(victim.getHealth() < initialHealth,
                    "The bounded grapple damage pulse must actually occur");
            dredger.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void aquaticSpecialsRemainVerticallyBoundedForOneHundredTicks(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        fillDeepWater(helper, 2, 13, 2, 13, 1, 10);
        ServerPlayer player = playerAt(helper, new Vec3(11.5D, 6.0D, 8.5D));
        player.setInvulnerable(true);
        UncannyDrifterEntity drifter = UncannyEntityRegistry.UNCANNY_DRIFTER.get().create(helper.getLevel());
        UncannyDredgerEntity dredger = UncannyEntityRegistry.UNCANNY_DREDGER.get().create(helper.getLevel());
        helper.assertTrue(drifter != null && dredger != null,
                "Both aquatic Specials must be constructible");
        drifter.moveTo(helper.absoluteVec(new Vec3(4.5D, 6.0D, 6.5D)));
        dredger.moveTo(helper.absoluteVec(new Vec3(4.5D, 6.0D, 10.5D)));
        drifter.setupTarget(player, 6_000);
        dredger.setupTarget(player, 6_000);
        CompoundTag dredgerState = new CompoundTag();
        dredger.saveWithoutId(dredgerState);
        dredgerState.putInt("DredgerRegrabCooldown", 1_000);
        dredger.load(dredgerState);
        helper.assertTrue(helper.getLevel().addFreshEntity(drifter)
                        && helper.getLevel().addFreshEntity(dredger),
                "Both aquatic Specials must enter the deep-water fixture");

        double[] minimumY = {drifter.getY(), dredger.getY()};
        double[] maximumY = {drifter.getY(), dredger.getY()};
        for (int tick = 5; tick <= 100; tick += 5) {
            helper.runAtTickTime(tick, () -> {
                minimumY[0] = Math.min(minimumY[0], drifter.getY());
                minimumY[1] = Math.min(minimumY[1], dredger.getY());
                maximumY[0] = Math.max(maximumY[0], drifter.getY());
                maximumY[1] = Math.max(maximumY[1], dredger.getY());
                helper.assertTrue(Math.abs(drifter.getDeltaMovement().y) <= 0.101D
                                && Math.abs(dredger.getDeltaMovement().y) <= 0.101D,
                        "Neither aquatic controller may accumulate a vertical launch velocity");
            });
        }
        helper.runAtTickTime(105, () -> {
            helper.assertTrue(maximumY[0] - minimumY[0] <= 7.0D
                            && maximumY[1] - minimumY[1] <= 4.0D,
                    "One hundred ticks of swimming must remain inside the authored depth envelope");
            drifter.discard();
            dredger.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 660)
    public static void drownedDerivedEntitiesCannotDrownInTheirRequiredHabitat(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        fillDeepWater(helper, 2, 13, 2, 13, 1, 10);
        ServerPlayer player = playerAt(helper, new Vec3(11.5D, 6.0D, 8.5D));
        player.setInvulnerable(true);
        UncannyDrifterEntity drifter = UncannyEntityRegistry.UNCANNY_DRIFTER.get().create(helper.getLevel());
        UncannyDredgerEntity dredger = UncannyEntityRegistry.UNCANNY_DREDGER.get().create(helper.getLevel());
        UncannyDrownedEntity drowned = UncannyEntityRegistry.UNCANNY_DROWNED.get().create(helper.getLevel());
        helper.assertTrue(drifter != null && dredger != null && drowned != null,
                "Every Drowned-derived entity type must be constructible");
        drifter.moveTo(helper.absoluteVec(new Vec3(4.5D, 6.0D, 5.5D)));
        dredger.moveTo(helper.absoluteVec(new Vec3(4.5D, 6.0D, 8.5D)));
        drowned.moveTo(helper.absoluteVec(new Vec3(4.5D, 6.0D, 11.5D)));
        drifter.setupTarget(player, 6_000);
        dredger.setupTarget(player, 6_000);
        CompoundTag dredgerState = new CompoundTag();
        dredger.saveWithoutId(dredgerState);
        dredgerState.putInt("DredgerRegrabCooldown", 2_000);
        dredger.load(dredgerState);
        drowned.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(drifter)
                        && helper.getLevel().addFreshEntity(dredger)
                        && helper.getLevel().addFreshEntity(drowned),
                "All three breathing-tag users must enter the deep-water fixture");

        helper.runAtTickTime(620, () -> {
            helper.assertTrue(drifter.isAlive() && dredger.isAlive() && drowned.isAlive(),
                    "No Drowned-derived entity may die from drowning after thirty-one seconds underwater");
            helper.assertTrue(drifter.getAirSupply() == drifter.getMaxAirSupply()
                            && dredger.getAirSupply() == dredger.getMaxAirSupply()
                            && drowned.getAirSupply() == drowned.getMaxAirSupply(),
                    "The engine breathing tag and migration guard must preserve full air");
            drifter.discard();
            dredger.discard();
            drowned.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 35)
    public static void flankersCannotCompleteAnEncirclementAtSpawn(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        player.setInvulnerable(true);
        helper.assertTrue(UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A wide open floor must admit a Flanker? pair");
        helper.runAtTickTime(20, () -> {
            List<UncannyFlankerEntity> pair = helper.getLevel().getEntitiesOfClass(
                    UncannyFlankerEntity.class,
                    new AABB(player.blockPosition()).inflate(32.0D),
                    entity -> entity.isAlive());
            helper.assertTrue(pair.size() == 2, "The complete pair must still exist");
            for (UncannyFlankerEntity member : pair) {
                CompoundTag state = new CompoundTag();
                member.saveWithoutId(state);
                helper.assertTrue(!state.getBoolean("FlankerEncirclement")
                                && state.getInt("FlankerStableFormation") < 20,
                        "No Flanker? may emit its encirclement call during the first twenty ticks");
                member.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 70)
    public static void observedFlankerPhysicallyEscapesMeleeRange(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        player.setInvulnerable(true);
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        helper.assertTrue(UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A wide open floor must admit a Flanker? pair");
        List<UncannyFlankerEntity> pair = helper.getLevel().getEntitiesOfClass(
                UncannyFlankerEntity.class,
                new AABB(player.blockPosition()).inflate(32.0D),
                Entity::isAlive);
        helper.assertTrue(pair.size() == 2, "The complete pair must exist before the evasion check");
        UncannyFlankerEntity watched = pair.getFirst();
        UncannyFlankerEntity opposite = pair.getLast();
        watched.moveTo(player.getX(), player.getY(), player.getZ() + 4.0D, 180.0F, 0.0F);
        opposite.moveTo(player.getX(), player.getY(), player.getZ() - 12.0D, 0.0F, 0.0F);

        helper.runAtTickTime(35, () -> {
            // The member looked at is the bait: it never stays within reach of the target's sword.
            helper.assertTrue(watched.distanceTo(player) >= 5.0D,
                    "A directly watched close Flanker? must back out of melee range: " + watched.distanceTo(player));
            helper.assertTrue(watched.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
                            >= HuntingSpecialRules.FLANKER_FOLLOW_RANGE,
                    "Flanker? must retain the long focus range used by its coordinated hunt");
            pair.forEach(Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260, batch = "flanker_strike_isolated")
    public static void flankerKeepsClosingAndStrikingAfterKnockback(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        // Mock players of finished batches linger; their gaze would swap the pair's roles at random.
        for (ServerPlayer other : helper.getLevel().players()) {
            if (other != player) {
                other.setGameMode(GameType.SPECTATOR);
            }
        }
        helper.assertTrue(UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A wide open floor must admit a Flanker? pair");
        // Face the room diagonal so both the staging point behind and the watched ring ahead fit.
        // World-space offsets: the GameTest structure may be rotated, the square room is not.
        Vec3 corner = player.position().add(4.0D, 0.0D, 4.0D);
        player.moveTo(corner.x, corner.y, corner.z, 135.0F, 0.0F);
        player.setYHeadRot(135.0F);
        List<UncannyFlankerEntity> pair = helper.getLevel().getEntitiesOfClass(
                UncannyFlankerEntity.class,
                new AABB(player.blockPosition()).inflate(32.0D),
                Entity::isAlive);
        helper.assertTrue(pair.size() == 2, "The complete pair must exist before the strike check");
        UncannyFlankerEntity advancing = pair.stream().filter(member -> member.memberIndex() == 0).findFirst().orElseThrow();
        UncannyFlankerEntity watched = pair.stream().filter(member -> member.memberIndex() == 1).findFirst().orElseThrow();
        advancing.moveTo(player.getX() + 2.4D, player.getY(), player.getZ() + 2.4D, -45.0F, 0.0F);
        watched.moveTo(player.getX() - 8.5D, player.getY(), player.getZ() - 8.5D, 135.0F, 0.0F);
        Vec3 shove = player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.6D);
        float[] lastHealth = {player.getHealth()};
        int[] hits = {0};
        helper.onEachTick(() -> {
            float health = player.getHealth();
            if (health < lastHealth[0] - 0.01F) {
                hits[0]++;
                if (hits[0] == 1) {
                    // Mock players never apply knockback; reproduce the in-game shove that left the
                    // attacker 3.7 blocks away, inside its staging slack yet beyond melee reach.
                    player.teleportTo(player.getX() + shove.x, player.getY(), player.getZ() + shove.z);
                }
                player.setHealth(player.getMaxHealth());
                health = player.getHealth();
            }
            lastHealth[0] = health;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(hits[0] >= 2,
                    "The staged Flanker? must close back in and keep striking after knockback");
            pair.forEach(Entity::discard);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void flankerRefusesAnEncirclementAreaCutByWater(GameTestHelper helper) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(x, 0, z, Blocks.WATER);
            }
        }
        helper.setBlock(8, 0, 8, Blocks.STONE);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        helper.assertTrue(!UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A pair whose rings would lie over water must not spawn only to sink");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void unresolvedFlankerPairsNeverBlockFuturePairs(GameTestHelper helper) {
        UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
        java.util.UUID oldest = java.util.UUID.randomUUID();
        helper.assertTrue(state.initializeFlankerPair(oldest, java.util.UUID.randomUUID(), java.util.UUID.randomUUID()),
                "A fresh pair must be recorded");
        // Pairs discarded without death or sink stay unresolved; fill the table well past its bound.
        for (int index = 0; index < 80; index++) {
            helper.assertTrue(state.initializeFlankerPair(
                            java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), java.util.UUID.randomUUID()),
                    "A saturated table of abandoned pairs must never refuse a new Flanker? pair");
        }
        helper.assertTrue(state.getFlankerPairReward(oldest) == null,
                "The oldest unresolved pair is the one forgotten");
        helper.assertTrue(state.getActiveFlankerPairRewardCount() <= 64, "The table stays bounded");
        CompoundTag saved = state.save(new CompoundTag(), helper.getLevel().registryAccess());
        UncannyWorldState restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.initializeFlankerPair(
                        java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), java.util.UUID.randomUUID()),
                "A reloaded full table must still admit a new pair");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void flankerPairSpawnsTransactionallyAndRewardsOnlyAfterBothPlayerKills(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        helper.assertTrue(UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A wide open loaded floor must admit both opposite Flanker? paths");
        List<UncannyFlankerEntity> pair = helper.getLevel().getEntitiesOfClass(
                UncannyFlankerEntity.class,
                new AABB(player.blockPosition()).inflate(32.0D),
                entity -> entity.isAlive() && entity.focusPlayerId().filter(player.getUUID()::equals).isPresent());
        helper.assertTrue(pair.size() == 2, "The transaction must expose exactly two linked members");
        UncannyFlankerEntity first = pair.get(0);
        UncannyFlankerEntity second = pair.get(1);
        helper.assertTrue(first.pairId() != null && first.pairId().equals(second.pairId()),
                "Both members must share one persistent pair id");
        helper.assertTrue(first.partnerId().filter(second.getUUID()::equals).isPresent()
                        && second.partnerId().filter(first.getUUID()::equals).isPresent(),
                "Partner identities must be reciprocal");

        first.hurt(helper.getLevel().damageSources().playerAttack(player), 1_000.0F);
        second.hurt(helper.getLevel().damageSources().playerAttack(player), 1_000.0F);
        UncannyWorldState state = UncannyWorldState.get(helper.getLevel().getServer());
        UncannyWorldState.FlankerPairRewardState reward = state.getFlankerPairReward(first.pairId());
        helper.assertTrue(reward != null && reward.resolved() && reward.rewardEligible() && reward.rewardClaimed(),
                "The second player-attributed death must perform and claim exactly one shared reward roll");

        CompoundTag saved = state.save(new CompoundTag(), helper.getLevel().registryAccess());
        UncannyWorldState restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess());
        UncannyWorldState.FlankerPairRewardState restoredReward = restored.getFlankerPairReward(first.pairId());
        helper.assertTrue(restoredReward != null && restoredReward.rewardClaimed(),
                "A completed pair cannot gain a second reward after reload");
        helper.succeed();
    }

    /**
     * Flanker? rework (2026-10-09): while the target watches the bait in front, the blade circles round
     * and every blow it lands comes from behind; the pair never lands more than one blow per Attacker?
     * cadence, a pincer (two blows together) then waiting twice as long.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 420, batch = "flanker_pincer_isolated")
    public static void flankerBladeStrikesFromBehindWhileTheBaitIsWatched(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        for (ServerPlayer other : helper.getLevel().players()) {
            if (other != player) {
                other.setGameMode(GameType.SPECTATOR);
            }
        }
        helper.assertTrue(UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A wide open floor must admit a Flanker? pair");
        Vec3 spot = player.position().add(-3.0D, 0.0D, -3.0D);
        player.moveTo(spot.x, spot.y, spot.z, -45.0F, 0.0F);
        player.setYHeadRot(-45.0F);
        Vec3 view = player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 side = new Vec3(-view.z, 0.0D, view.x);
        List<UncannyFlankerEntity> pair = helper.getLevel().getEntitiesOfClass(
                UncannyFlankerEntity.class, new AABB(player.blockPosition()).inflate(32.0D), Entity::isAlive);
        helper.assertTrue(pair.size() == 2, "The complete pair must exist");
        UncannyFlankerEntity blade = pair.stream().filter(member -> member.memberIndex() == 0).findFirst().orElseThrow();
        UncannyFlankerEntity bait = pair.stream().filter(member -> member.memberIndex() == 1).findFirst().orElseThrow();
        Vec3 ahead = player.position().add(view.scale(7.0D));
        Vec3 beside = player.position().add(side.scale(6.0D));
        bait.moveTo(ahead.x, ahead.y, ahead.z, 0.0F, 0.0F);
        blade.moveTo(beside.x, beside.y, beside.z, 0.0F, 0.0F);

        java.util.List<Integer> hitTicks = new java.util.ArrayList<>();
        int[] fromBehind = {0};
        float[] lastHealth = {player.getHealth()};
        int[] tick = {0};
        helper.onEachTick(() -> {
            tick[0]++;
            // Mock players are never ticked: no invulnerability frames run out, no knockback moves them.
            player.invulnerableTime = 0;
            player.moveTo(spot.x, spot.y, spot.z, -45.0F, 0.0F);
            player.setYHeadRot(-45.0F);
            float health = player.getHealth();
            if (health < lastHealth[0] - 0.01F) {
                hitTicks.add(tick[0]);
                if (player.getLastHurtByMob() instanceof UncannyFlankerEntity hitter) {
                    double angle = com.eotv.echoofthevoid.event.special.FlankerRules.signedAngleFromView(
                            view.x, view.z, hitter.getX() - player.getX(), hitter.getZ() - player.getZ());
                    if (Math.abs(angle) >= 90.0D) {
                        fromBehind[0]++;
                    }
                }
                player.setHealth(player.getMaxHealth());
                health = player.getHealth();
            }
            lastHealth[0] = health;
        });
        helper.runAtTickTime(400, () -> {
            helper.assertTrue(fromBehind[0] >= 2, "The blade must get round and strike from behind: "
                    + fromBehind[0] + " of " + hitTicks.size());
            int gap = com.eotv.echoofthevoid.event.special.FlankerRules.pairStrikeGapTicks();
            for (int i = 1; i < hitTicks.size(); i++) {
                int between = hitTicks.get(i) - hitTicks.get(i - 1);
                boolean pincer = between <= com.eotv.echoofthevoid.event.special.FlankerRules.PINCER_WINDOW_TICKS + 1;
                helper.assertTrue(pincer || between >= gap - 1,
                        "The pair shares one strike rhythm: blows " + between + " ticks apart at " + hitTicks);
                if (pincer && i + 1 < hitTicks.size()) {
                    helper.assertTrue(hitTicks.get(i + 1) - hitTicks.get(i) >= 2 * gap - 2,
                            "After a pincer the pair waits twice as long: " + hitTicks);
                }
            }
            pair.forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** Alone, a Flanker? keeps its own half of the pair's toughness: losing the partner halves the danger. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aLoneFlankerKeepsHalfThePairsToughness(GameTestHelper helper) {
        fillFloor(helper, 0, 15);
        ServerPlayer player = playerAt(helper, new Vec3(8.5D, 1.0D, 8.5D));
        player.setInvulnerable(true);
        helper.assertTrue(UncannyHuntingSpecialSystem.spawnForDebug(player, "flanker", "normal"),
                "A wide open floor must admit a Flanker? pair");
        List<UncannyFlankerEntity> pair = helper.getLevel().getEntitiesOfClass(
                UncannyFlankerEntity.class, new AABB(player.blockPosition()).inflate(32.0D), Entity::isAlive);
        helper.assertTrue(pair.size() == 2, "The complete pair must exist");
        UncannyFlankerEntity first = pair.get(0);
        UncannyFlankerEntity second = pair.get(1);
        float before = second.getMaxHealth();
        first.hurt(helper.getLevel().damageSources().playerAttack(player), 1_000.0F);
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(second.isAlive() && second.flankerRole() == UncannyFlankerEntity.Role.SURVIVOR,
                    "The other one hunts on alone");
            helper.assertTrue(Math.abs(second.getMaxHealth() - before) < 0.5F,
                    "It keeps half an Attacker?'s toughness, not a whole one: " + before + " -> " + second.getMaxHealth());
            second.discard();
            helper.succeed();
        });
    }

    private static ServerPlayer playerAt(GameTestHelper helper, Vec3 relativePosition) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        // GameTest mock players default to creative. Combat assertions must exercise the same
        // damage path as a survival encounter; otherwise a real melee hit/pull pulse is
        // indistinguishable from an attack that never happened.
        player.setGameMode(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(relativePosition);
        player.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        return player;
    }

    private static void fillFloor(GameTestHelper helper, int minimum, int maximum) {
        for (int x = minimum; x <= maximum; x++) {
            for (int z = minimum; z <= maximum; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static void fillDeepWater(
            GameTestHelper helper,
            int minimumX,
            int maximumX,
            int minimumZ,
            int maximumZ,
            int minimumY,
            int maximumY) {
        for (int x = minimumX; x <= maximumX; x++) {
            for (int z = minimumZ; z <= maximumZ; z++) {
                for (int y = minimumY; y <= maximumY; y++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }
    }
}
