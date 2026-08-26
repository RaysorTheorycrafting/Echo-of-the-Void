package com.eotv.echoofthevoid.entity.variant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApprovedEntityBehaviorSurfaceTest {
    private static final Path ROOT = Path.of("src", "main", "java", "com", "eotv", "echoofthevoid");

    @Test
    void revisedPassiveV5VariantsDoNotPunishInventoryHealthOrTaming() throws IOException {
        String source = read(ROOT.resolve(Path.of("event", "UncannyPassiveVariantSystem.java")));

        assertFalse(source.contains("player.hurt(player.damageSources().generic(), 2.0F)"));
        assertFalse(source.contains("wolf.setTame(false"));
        assertFalse(source.contains("wolf.setTarget(owner)"));
        assertFalse(source.contains("UncannyParanoiaEventSystem.spawnStalker(player)"));
        assertFalse(source.contains("UncannyParanoiaEventSystem.triggerFlashError(player)"));

        assertTrue(source.contains("cow.discard();"));
        assertTrue(source.contains("TAG_WOLF_FEINT_END"));
        assertTrue(source.contains("TAG_VILLAGER_MIRROR_END"));
    }

    @Test
    void commandAndSpawnEggPassivesStayVanilla() throws IOException {
        String source = read(ROOT.resolve(Path.of("event", "UncannyPassiveVariantSystem.java")));
        assertTrue(source.contains("case SPAWNER, SPAWN_EGG, COMMAND, DISPENSER, TRIAL_SPAWNER"));
    }

    @Test
    void dangerousCreepersAreAudibleAndAbsorberHasAFuseTelegraph() throws IOException {
        String source = read(ROOT.resolve(Path.of("entity", "custom", "UncannyCreeperEntity.java")));
        assertTrue(source.contains("this.setSilent(false);"));
        assertTrue(source.contains("if (variant == CreeperVariant.ABSORBER)"));
        assertTrue(source.contains("SoundEvents.CREEPER_PRIMED"));
        assertFalse(source.contains("this.setSilent(variant == CreeperVariant.SILHOUETTE)"));
    }

    @Test
    void presenceHasReducedReachAndSkipsPassiveAnimals() throws IOException {
        String source = read(ROOT.resolve(Path.of("entity", "custom", "UncannyPulseEntity.java")));
        assertTrue(source.contains("getNearestPlayer(this, 24.0D)"));
        assertTrue(source.contains("living instanceof Player || living instanceof Enemy"));
        assertFalse(source.contains("getNearestPlayer(this, 48.0D)"));
    }

    @Test
    void explicitlyPreservedKeeperAndShadowEffectsRemainConcrete() throws IOException {
        String keeper = read(ROOT.resolve(Path.of("entity", "custom", "UncannyKeeperEntity.java")));
        String shadow = read(ROOT.resolve(Path.of("entity", "custom", "UncannyShadowEntity.java")));
        assertTrue(keeper.contains("container.setItem(source, second)"));
        assertTrue(keeper.contains("container.setItem(destination, first)"));
        assertTrue(shadow.contains("destroyNearbyLights(this.blockPosition())"));
        assertTrue(shadow.contains("this.level().destroyBlock(pos, true, this)"));
        assertTrue(shadow.contains("private static final double FEAR_RADIUS = 7.0D"));
        assertTrue(shadow.contains("wolf.isTame()"));
        assertTrue(shadow.contains("cat.isTame()"));
        assertTrue(shadow.contains("ironGolem.isPlayerCreated()"));
        assertTrue(shadow.contains("startSinking();"));
        assertTrue(shadow.contains("this.setDeltaMovement(0.0D, -0.035D, 0.0D);"));
        assertTrue(shadow.contains("state.is(UncannyBlockRegistry.UNCANNY_ALTAR.get())"));
        assertTrue(shadow.contains("state.is(UncannyBlockRegistry.UNCANNY_ALTAR_PART.get())"));
        assertFalse(shadow.contains("this.setPos(this.getX(), this.getY() - 0.035D, this.getZ());"));
        assertFalse(shadow.contains("this.fleeTicks <= 0 || this.unseenTicks >= 26) {\n                this.discard();"));
    }

    @Test
    void usherNoLongerBreaksOrTeleportsAcrossObstacles() throws IOException {
        String source = read(ROOT.resolve(Path.of("entity", "custom", "UncannyUsherEntity.java")));
        assertFalse(source.contains("destroyBlock("));
        assertFalse(source.contains("clearLeavesInFront"));
        assertFalse(source.contains("tryStepUpObstacle"));
        assertTrue(source.contains("this.getNavigation().moveTo("));
        assertTrue(source.contains("setPathfindingMalus(PathType.LEAVES, 0.0F)"));
        assertTrue(source.contains("shouldPhaseThroughLeaves"));
        assertTrue(source.contains("if (!state.is(BlockTags.LEAVES))"));
        assertTrue(source.contains("this.getNavigation().setMaxVisitedNodesMultiplier(2.0F)"));
        assertTrue(source.contains("Attributes.STEP_HEIGHT).setBaseValue(1.0D)"));
        assertFalse(source.contains("this.jumping = false;"));
        assertTrue(source.contains("super.jumpFromGround();"));
        assertTrue(source.contains("this.getJumpControl().jump();"));
        assertTrue(source.contains("level.playSound(null, this.getX(), this.getEyeY(), this.getZ(), event"));
        assertFalse(source.contains("player.playNotifySound(event"));
    }

    @Test
    void ironGolemVariantsKeepVanillaRetaliationAndSuppressSprintParticles() throws IOException {
        String source = read(ROOT.resolve(Path.of("entity", "custom", "UncannyIronGolemEntity.java")));
        assertTrue(source.contains("public boolean canSpawnSprintParticle()"));
        assertTrue(source.contains("return false;"));
        assertTrue(source.contains("boolean hurt = super.hurt(source, amount);"));
        assertTrue(source.contains("this.frozenByVariant = false;"));
        assertTrue(source.contains("Preserve Vanilla targets"));
        assertTrue(source.contains("allowedVariantForOrigin"));
        assertTrue(source.contains("if (this.getTarget() != null)"));
        assertTrue(source.contains("this.setSilent(false);"));
        assertTrue(source.contains("return SoundEvents.IRON_GOLEM_HURT;"));
        assertFalse(source.contains("this.setSilent(variant =="));
    }

    @Test
    void keeperQaBypassesNaturalPhaseAndBasePrerequisites() throws IOException {
        String source = read(ROOT.resolve(Path.of("event", "UncannyParanoiaEventSystem.java")));
        String signature = "public static boolean spawnKeeperForCommand(ServerPlayer player)";
        int start = source.indexOf(signature);
        assertTrue(start >= 0);
        String method = source.substring(start, Math.min(source.length(), start + 180));
        assertTrue(method.contains("return spawnKeeper(player, true);"));
    }

    @Test
    void tenantUsesARealInteriorRouteAndContextualCommandsCannotBypassSetup() throws IOException {
        String tenant = read(ROOT.resolve(Path.of("entity", "custom", "UncannyTenantEntity.java")));
        String events = read(ROOT.resolve(Path.of("event", "UncannyParanoiaEventSystem.java")));
        String commands = read(ROOT.resolve(Path.of("command", "UncannyCommandRegistry.java")));

        assertTrue(tenant.contains("HomeInteriorX"));
        assertTrue(tenant.contains("hasReachedHome()"));
        assertFalse(tenant.contains("WaterAvoidingRandomStrollGoal"));
        assertTrue(events.contains("findTenantDoorRoute"));
        assertTrue(events.contains("tenant.setupTenant(player, doorPos, route.inside())"));
        assertTrue(commands.contains("UncannyParanoiaEventSystem.spawnTenantForCommand(target)"));
        assertTrue(commands.contains("ApprovedSpecialSystem.spawnForDebug(target, approvedSpecialId)"));
    }

    @Test
    void specialPhysicalSoundsAndPersonalTargetsKeepTheirIntendedAudience() throws IOException {
        String keeper = read(ROOT.resolve(Path.of("entity", "custom", "UncannyKeeperEntity.java")));
        String tenant = read(ROOT.resolve(Path.of("entity", "custom", "UncannyTenantEntity.java")));
        String terror = read(ROOT.resolve(Path.of("entity", "custom", "UncannyTerrorEntity.java")));
        String approved = read(ROOT.resolve(Path.of("entity", "custom", "UncannyApprovedSpecialEntity.java")));
        String mimic = read(ROOT.resolve(Path.of("entity", "custom", "UncannyDoubleDormantEntity.java")));

        assertTrue(keeper.contains("level.playSound(\n                        null,\n                        this,"));
        assertFalse(keeper.contains("owner.playNotifySound"));
        assertTrue(keeper.contains("bound.serverLevel() == level"));
        assertTrue(tenant.contains("bound.serverLevel() == level"));
        assertTrue(terror.contains("bound.serverLevel() == level"));
        assertTrue(approved.contains("bound.serverLevel() == level"));
        assertTrue(mimic.contains("copied.serverLevel() == level"));
        assertTrue(mimic.contains("setDropChance(EquipmentSlot.MAINHAND, 0.0F)"));
        assertTrue(terror.contains("this.discard();"));
        assertFalse(terror.contains("bound != null && bound.isAlive())"));
    }

    @Test
    void uncannyCompassKeepsItsRegisteredItemIdentity() throws IOException {
        String item = read(ROOT.resolve(Path.of("item", "custom", "UncannyCompassItem.java")));
        String events = read(ROOT.resolve(Path.of("event", "UncannyParanoiaEventSystem.java")));
        String client = read(ROOT.resolve("EchoOfTheVoidClient.java"));
        assertFalse(item.contains("new ItemStack(Items.COMPASS"));
        assertFalse(events.contains("normalizeUncannyCompassInHands"));
        assertTrue(events.contains("stack.is(UncannyItemRegistry.UNCANNY_COMPASS.get())"));
        assertTrue(client.contains("ItemProperties.register("));
        assertTrue(client.contains("UncannyItemRegistry.UNCANNY_COMPASS.get()"));
        assertTrue(client.contains("ResourceLocation.withDefaultNamespace(\"angle\")"));
        assertTrue(client.contains("stack.get(DataComponents.LODESTONE_TRACKER)"));
        assertTrue(events.contains("target = findNearestAnyUncannyStructure(player);"));
    }

    @Test
    void vanillaVexAndZoglinAreNotGloballyMuted() throws IOException {
        String source = read(ROOT.resolve(Path.of("event", "UncannySpawnController.java")));
        assertFalse(source.contains("vex.setSilent(true)"));
        assertFalse(source.contains("zoglin.setSilent(true)"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
