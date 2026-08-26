package com.eotv.echoofthevoid.event.passive;

import com.eotv.echoofthevoid.network.UncannyVanillaVariantVisualPayload;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Runtime for VV-01..VV-20. These are tags on the real Vanilla entities: no replacement entity,
 * attribute, equipment, breeding, taming, sound set, or loot table is changed.
 */
public final class ApprovedVanillaVariantSystem {
    public static final String TAG_VARIANT = "UncannyApprovedVanillaVariant";
    private static final String TAG_NEXT = "UncannyApprovedVariantNext";
    private static final String TAG_MODE = "UncannyApprovedVariantMode";
    private static final String TAG_END = "UncannyApprovedVariantEnd";
    private static final String TAG_X = "UncannyApprovedVariantX";
    private static final String TAG_Y = "UncannyApprovedVariantY";
    private static final String TAG_Z = "UncannyApprovedVariantZ";
    private static final String TAG_MEMORY_TIME = "UncannyApprovedVariantMemoryTime";
    private static final String TAG_DEV = "UncannyApprovedVariantDev";
    private static final String LEGACY_PASSIVE_TAG = "UncannyPassiveEnabled";
    private static final String DEV_SPAWN_TAG = "eotv_dev_spawned";

    private ApprovedVanillaVariantSystem() {
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!UncannyWorldState.get(level.getServer()).isSystemEnabled()
                || !isNaturalSpawn(event.getSpawnType())
                || mob.getPersistentData().getBoolean(LEGACY_PASSIVE_TAG)) {
            return;
        }
        ApprovedVanillaVariantCatalog.Variant variant = variantForType(mob.getType());
        if (variant == null) {
            return;
        }
        int phase = UncannyWorldState.get(level.getServer()).getPhase().index();
        double chance = ApprovedVanillaVariantCatalog.naturalChance(variant, phase);
        RandomSource random = RandomSource.create(
                mob.getUUID().getMostSignificantBits()
                        ^ mob.getUUID().getLeastSignificantBits()
                        ^ level.getSeed()
                        ^ level.getGameTime());
        if (random.nextDouble() < chance) {
            applyVariant(mob, variant, level.getGameTime(), false);
        }
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        String id = mob.getPersistentData().getString(TAG_VARIANT);
        if (id.isEmpty() || !UncannyWorldState.get(level.getServer()).isSystemEnabled()) {
            return;
        }
        ApprovedVanillaVariantCatalog.Variant variant = ApprovedVanillaVariantCatalog.byId(id);
        if (variant == null || variantForType(mob.getType()) != variant) {
            return;
        }
        tickVariant(level, mob, variant.id(), level.getGameTime());
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof Goat goat)
                || !is(goat, "goat_echo_ram")) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = goat.getPersistentData();
        if (now < data.getLong(TAG_NEXT) || data.getInt(TAG_MODE) != 0) {
            return;
        }
        Vec3 impact = event.getEntity().position().add(0.0D, event.getEntity().getBbHeight() * 0.45D, 0.0D);
        data.putInt(TAG_MODE, 2);
        data.putLong(TAG_MEMORY_TIME, now);
        data.putLong(TAG_END, now + 6L);
        data.putDouble(TAG_X, impact.x);
        data.putDouble(TAG_Y, impact.y);
        data.putDouble(TAG_Z, impact.z);
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof BreezeWindCharge charge)
                || !(charge.level() instanceof ServerLevel level)
                || !(charge.getOwner() instanceof Breeze breeze)
                || !is(breeze, "breeze_returned_wind")) {
            return;
        }
        CompoundTag data = breeze.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLong(TAG_NEXT)) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        Vec3 from = hit.getLocation();
        data.putInt(TAG_MODE, 2);
        data.putLong(TAG_MEMORY_TIME, now);
        data.putLong(TAG_END, now + 13L);
        data.putDouble(TAG_X, from.x);
        data.putDouble(TAG_Y, from.y);
        data.putDouble(TAG_Z, from.z);
        level.playSound(null, from.x, from.y, from.z,
                SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.HOSTILE, 0.45F, 0.72F);
    }

    public static boolean forceSpawn(ServerPlayer player, String variantId) {
        ApprovedVanillaVariantCatalog.Variant variant = ApprovedVanillaVariantCatalog.byId(variantId);
        EntityType<? extends Mob> type = variant == null ? null : resolveType(variant.typeKey());
        if (player == null || type == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Mob mob = type.create(level);
        if (mob == null) {
            return false;
        }
        Vec3 position = findDevSpawnPosition(level, player, type);
        mob.moveTo(position.x, position.y, position.z, player.getYRot() + 180.0F, 0.0F);
        applyVariant(mob, variant, level.getGameTime(), true);
        mob.addTag(DEV_SPAWN_TAG);
        return level.addFreshEntity(mob);
    }

    public static String variantId(Entity entity) {
        return entity == null ? "" : entity.getPersistentData().getString(TAG_VARIANT);
    }

    private static void tickVariant(ServerLevel level, Mob mob, String id, long now) {
        switch (id) {
            case "bee_false_hive" -> tickBee(level, (Bee) mob, now);
            case "bat_wrong_roost" -> tickBat(level, (Bat) mob, now);
            case "rabbit_return_to_cover" -> tickRabbit(level, (Rabbit) mob, now);
            case "goat_echo_ram" -> tickGoat(level, (Goat) mob, now);
            case "breeze_returned_wind" -> tickBreeze(level, (Breeze) mob, now);
            case "horse_empty_rider" -> tickHorse(level, (AbstractHorse) mob, now);
            case "allay_wrong_recipient" -> tickAllay(level, (Allay) mob, now);
            case "axolotl_healthy_feign" -> tickAxolotl(level, (Axolotl) mob, now);
            case "dolphin_blindside_escort" -> tickDolphin(level, (Dolphin) mob, now);
            case "frog_empty_tongue" -> tickFrog(level, (Frog) mob, now);
            case "turtle_false_nest" -> tickTurtle(level, (Turtle) mob, now);
            case "sniffer_second_dig" -> tickSniffer(level, (Sniffer) mob, now);
            case "armadillo_empty_threat" -> tickArmadillo(level, (Armadillo) mob, now);
            case "glow_squid_light_lag" -> tickGlowSquid(level, (GlowSquid) mob, now);
            case "cave_spider_ceiling_wait" -> tickCaveSpider(level, (CaveSpider) mob, now);
            case "shulker_empty_aim" -> tickShulker(level, (Shulker) mob, now);
            case "guardian_false_beam" -> tickGuardian(level, (Guardian) mob, now);
            case "vex_caught_between" -> tickVex(level, (Vex) mob, now);
            case "silverfish_wrong_stone" -> tickSilverfish(level, (Silverfish) mob, now);
            case "zombified_piglin_procession" -> tickPiglinProcession(level, (ZombifiedPiglin) mob, now);
            default -> {
            }
        }
    }

    private static void tickBee(ServerLevel level, Bee bee, long now) {
        CompoundTag data = bee.getPersistentData();
        if (bee.isAngry() || bee.hasNectar() || bee.hasHive() || bee.getTarget() != null) {
            cancelMovementCue(bee, data);
            return;
        }
        if (data.getInt(TAG_MODE) == 1) {
            BlockPos target = storedPos(data);
            if (now >= data.getLong(TAG_END) || !isOrdinaryHiveFace(level, target)) {
                finish(bee, data, now, 1000, 2200);
                return;
            }
            bee.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
            bee.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 50.0F, 50.0F);
            return;
        }
        if (ready(data, now)) {
            BlockPos target = findOrdinaryHiveFace(level, bee.blockPosition(), bee.getRandom());
            if (target != null) {
                start(data, target, now + 80L + bee.getRandom().nextInt(50));
            } else {
                data.putLong(TAG_NEXT, now + 300L);
            }
        }
    }

    private static void tickGoat(ServerLevel level, Goat goat, long now) {
        CompoundTag data = goat.getPersistentData();
        if (data.getInt(TAG_MODE) != 2 || now < data.getLong(TAG_END)) {
            return;
        }
        Vec3 impact = new Vec3(data.getDouble(TAG_X), data.getDouble(TAG_Y), data.getDouble(TAG_Z));
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GOAT_RAM_IMPACT, SoundSource.NEUTRAL, 0.65F, 0.82F);
        BlockPos wall = findNearbySolid(level, BlockPos.containing(impact), 3);
        if (wall != null) {
            BlockState state = level.getBlockState(wall);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    wall.getX() + 0.5D, wall.getY() + 0.5D, wall.getZ() + 0.5D,
                    8, 0.24D, 0.24D, 0.24D, 0.02D);
        }
        finish(goat, data, now, 1400, 2800);
    }

    private static void tickBreeze(ServerLevel level, Breeze breeze, long now) {
        CompoundTag data = breeze.getPersistentData();
        if (data.getInt(TAG_MODE) != 2) {
            return;
        }
        long started = data.getLong(TAG_MEMORY_TIME);
        double t = Mth.clamp((now - started) / 12.0D, 0.0D, 1.0D);
        Vec3 from = new Vec3(data.getDouble(TAG_X), data.getDouble(TAG_Y), data.getDouble(TAG_Z));
        Vec3 point = from.lerp(breeze.getEyePosition(), t);
        level.sendParticles(ParticleTypes.GUST,
                point.x, point.y, point.z, 2, 0.04D, 0.04D, 0.04D, 0.0D);
        if (now >= data.getLong(TAG_END)) {
            finish(breeze, data, now, 900, 1900);
        }
    }

    private static void tickBat(ServerLevel level, Bat bat, long now) {
        CompoundTag data = bat.getPersistentData();
        ServerPlayer observer = nearestPlayer(level, bat, 9.0D);
        if (data.getInt(TAG_MODE) == 1) {
            BlockPos roost = storedPos(data);
            if (now >= data.getLong(TAG_END)) {
                bat.setNoGravity(false);
                if (level.getBlockState(roost.above()).isSolidRender(level, roost.above())) {
                    bat.setPos(roost.getX() + 0.5D, roost.getY() + 0.15D, roost.getZ() + 0.5D);
                    bat.setResting(true);
                }
                finish(bat, data, now, 900, 1800);
                return;
            }
            double angle = (now + bat.getId() * 13L) * 0.24D;
            Vec3 center = Vec3.atCenterOf(roost).add(0.0D, -0.2D, 0.0D);
            Vec3 destination = center.add(Math.cos(angle) * 1.25D, 0.35D, Math.sin(angle) * 1.25D);
            bat.setDeltaMovement(destination.subtract(bat.position()).scale(0.24D));
            return;
        }
        if (!bat.isResting() || observer == null || !ready(data, now)) {
            return;
        }
        bat.lookAt(observer, 80.0F, 80.0F);
        if (isLookingAt(observer, bat, 0.965D)) {
            start(data, bat.blockPosition(), now + 42L);
            bat.setResting(false);
            level.playSound(null, bat, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.45F, 0.9F);
        }
    }

    private static void tickRabbit(ServerLevel level, Rabbit rabbit, long now) {
        CompoundTag data = rabbit.getPersistentData();
        ServerPlayer player = nearestPlayer(level, rabbit, 14.0D);
        if (rabbit.getTarget() != null || rabbit.hurtTime > 0) {
            cancelMovementCue(rabbit, data);
            return;
        }
        if (data.getInt(TAG_MODE) == 1) {
            BlockPos cover = storedPos(data);
            if (player != null && rabbit.distanceToSqr(player) < 7.0D * 7.0D) {
                return;
            }
            if (rabbit.blockPosition().closerThan(cover, 1.5D) || now >= data.getLong(TAG_END)) {
                if (player != null) {
                    rabbit.getLookControl().setLookAt(player, 60.0F, 50.0F);
                }
                finish(rabbit, data, now, 1000, 2400);
                return;
            }
            rabbit.getNavigation().moveTo(cover.getX() + 0.5D, cover.getY(), cover.getZ() + 0.5D, 1.05D);
            return;
        }
        if (ready(data, now) && player != null && rabbit.distanceToSqr(player) < 4.5D * 4.5D) {
            start(data, rabbit.blockPosition(), now + 180L);
        }
    }

    private static void tickHorse(ServerLevel level, AbstractHorse horse, long now) {
        CompoundTag data = horse.getPersistentData();
        if (!ready(data, now) || horse.isTamed() || horse.isSaddled() || horse.isVehicle()
                || horse.hasCustomName() || horse.isInLove() || horse.hurtTime > 0) {
            return;
        }
        horse.standIfPossible();
        horse.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, horse, SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 0.32F, 0.83F);
        data.putLong(TAG_NEXT, now + cooldown(horse, 1100, 2400));
    }

    private static void tickAllay(ServerLevel level, Allay allay, long now) {
        CompoundTag data = allay.getPersistentData();
        if (allay.getTarget() != null || allay.hurtTime > 0 || allay.getMainHandItem().isEmpty()) {
            cancelMovementCue(allay, data);
            return;
        }
        if (data.getInt(TAG_MODE) == 1) {
            BlockPos target = storedPos(data);
            if (now >= data.getLong(TAG_END) || allay.position().distanceToSqr(Vec3.atCenterOf(target)) < 1.8D) {
                allay.swing(InteractionHand.MAIN_HAND);
                level.playSound(null, allay, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 0.32F, 0.9F);
                finish(allay, data, now, 1200, 2600);
                return;
            }
            allay.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.1D);
            return;
        }
        if (ready(data, now)) {
            Vec3 side = allay.position().add(rotatedHorizontal(allay, 3.0D));
            start(data, BlockPos.containing(side), now + 36L);
        }
    }

    private static void tickAxolotl(ServerLevel level, Axolotl axolotl, long now) {
        CompoundTag data = axolotl.getPersistentData();
        boolean unsafe = axolotl.getTarget() != null || axolotl.hurtTime > 0
                || axolotl.getHealth() < axolotl.getMaxHealth() || !axolotl.isInWaterOrBubble();
        if (unsafe) {
            if (data.getInt(TAG_MODE) == 1) {
                axolotl.setPlayingDead(false);
                finish(axolotl, data, now, 900, 1900);
            }
            return;
        }
        if (data.getInt(TAG_MODE) == 1 && now >= data.getLong(TAG_END)) {
            axolotl.setPlayingDead(false);
            finish(axolotl, data, now, 900, 1900);
        } else if (ready(data, now)) {
            data.putInt(TAG_MODE, 1);
            data.putLong(TAG_END, now + 45L);
            axolotl.setPlayingDead(true);
        }
    }

    private static void tickDolphin(ServerLevel level, Dolphin dolphin, long now) {
        CompoundTag data = dolphin.getPersistentData();
        ServerPlayer player = nearestPlayer(level, dolphin, 16.0D);
        if (player == null || !player.isInWaterOrBubble() || dolphin.getTarget() != null || dolphin.hurtTime > 0) {
            cancelMovementCue(dolphin, data);
            return;
        }
        if (isLookingAt(player, dolphin, 0.88D)) {
            cancelMovementCue(dolphin, data);
            data.putLong(TAG_NEXT, Math.max(data.getLong(TAG_NEXT), now + 100L));
            return;
        }
        if (data.getInt(TAG_MODE) == 1) {
            if (now >= data.getLong(TAG_END)) {
                finish(dolphin, data, now, 900, 1800);
                return;
            }
            Vec3 behind = player.position().subtract(player.getLookAngle().scale(3.2D));
            dolphin.getNavigation().moveTo(behind.x, behind.y, behind.z, 1.15D);
        } else if (ready(data, now)) {
            data.putInt(TAG_MODE, 1);
            data.putLong(TAG_END, now + 100L + dolphin.getRandom().nextInt(80));
        }
    }

    private static void tickFrog(ServerLevel level, Frog frog, long now) {
        CompoundTag data = frog.getPersistentData();
        if (!ready(data, now) || frog.getTarget() != null || frog.hurtTime > 0 || frog.getTongueTarget().isPresent()) {
            return;
        }
        Vec3 target = frog.position().add(rotatedHorizontal(frog, 1.7D)).add(0.0D, 0.35D, 0.0D);
        sendVisual(level, frog, "frog_empty_tongue", true, target, 12, frog.getRandom().nextLong());
        level.playSound(null, frog, SoundEvents.FROG_TONGUE, SoundSource.NEUTRAL, 0.42F, 0.92F);
        data.putLong(TAG_NEXT, now + cooldown(frog, 1000, 2300));
    }

    private static void tickTurtle(ServerLevel level, Turtle turtle, long now) {
        CompoundTag data = turtle.getPersistentData();
        if (!ready(data, now) || turtle.hasEgg() || turtle.isLayingEgg() || turtle.isInLove()
                || turtle.hurtTime > 0 || !level.getBlockState(turtle.blockPosition().below()).is(BlockTags.SAND)) {
            return;
        }
        BlockPos sand = turtle.blockPosition().below();
        BlockState state = level.getBlockState(sand);
        level.playSound(null, turtle, SoundEvents.TURTLE_LAY_EGG, SoundSource.NEUTRAL, 0.38F, 0.82F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                turtle.getX(), sand.getY() + 1.05D, turtle.getZ(), 18, 0.55D, 0.08D, 0.55D, 0.025D);
        turtle.getNavigation().stop();
        data.putLong(TAG_NEXT, now + cooldown(turtle, 1300, 3000));
    }

    private static void tickSniffer(ServerLevel level, Sniffer sniffer, long now) {
        CompoundTag data = sniffer.getPersistentData();
        if (sniffer.diggingAnimationState.isStarted() && data.getLong(TAG_MEMORY_TIME) <= 0L) {
            storePos(data, sniffer.blockPosition());
            data.putLong(TAG_MEMORY_TIME, now);
            data.putLong(TAG_NEXT, now + 24000L);
            return;
        }
        if (!ready(data, now) || data.getLong(TAG_MEMORY_TIME) <= 0L || sniffer.getTarget() != null || sniffer.hurtTime > 0) {
            return;
        }
        BlockPos memory = storedPos(data);
        if (!sniffer.blockPosition().closerThan(memory, 2.5D)) {
            sniffer.getNavigation().moveTo(memory.getX() + 0.5D, memory.getY(), memory.getZ() + 0.5D, 1.0D);
            return;
        }
        sendVisual(level, sniffer, "sniffer_second_dig", true, Vec3.atCenterOf(memory), 120, sniffer.getRandom().nextLong());
        level.playSound(null, sniffer, SoundEvents.SNIFFER_DIGGING, SoundSource.NEUTRAL, 0.48F, 0.92F);
        BlockState below = level.getBlockState(memory.below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below),
                memory.getX() + 0.5D, memory.getY() + 0.1D, memory.getZ() + 0.5D,
                26, 0.55D, 0.08D, 0.55D, 0.04D);
        data.putLong(TAG_MEMORY_TIME, 0L);
        data.putLong(TAG_NEXT, now + 24000L);
    }

    private static void tickArmadillo(ServerLevel level, Armadillo armadillo, long now) {
        CompoundTag data = armadillo.getPersistentData();
        boolean actualThreat = armadillo.shouldSwitchToScaredState() || armadillo.hurtTime > 0;
        if (actualThreat) {
            if (data.getInt(TAG_MODE) == 1) {
                data.putInt(TAG_MODE, 0);
            }
            return;
        }
        if (data.getInt(TAG_MODE) == 1) {
            armadillo.getLookControl().setLookAt(data.getDouble(TAG_X), data.getDouble(TAG_Y), data.getDouble(TAG_Z));
            if (now >= data.getLong(TAG_END)) {
                armadillo.rollOut();
                finish(armadillo, data, now, 1000, 2200);
            }
        } else if (ready(data, now)) {
            Vec3 target = armadillo.position().add(rotatedHorizontal(armadillo, 3.0D));
            storePos(data, BlockPos.containing(target));
            data.putInt(TAG_MODE, 1);
            data.putLong(TAG_END, now + 55L);
            armadillo.rollUp();
        }
    }

    private static void tickGlowSquid(ServerLevel level, GlowSquid squid, long now) {
        CompoundTag data = squid.getPersistentData();
        Vec3 previous = new Vec3(data.getDouble(TAG_X), data.getDouble(TAG_Y), data.getDouble(TAG_Z));
        Vec3 current = squid.position();
        if (data.getLong(TAG_MEMORY_TIME) > 0L && ready(data, now)) {
            Vec3 oldMotion = current.subtract(previous);
            Vec3 motion = squid.getDeltaMovement();
            if (oldMotion.lengthSqr() > 0.02D && motion.lengthSqr() > 0.02D
                    && oldMotion.normalize().dot(motion.normalize()) < 0.25D) {
                sendVisual(level, squid, "glow_squid_light_lag", true, previous, 11, squid.getRandom().nextLong());
                data.putLong(TAG_NEXT, now + cooldown(squid, 700, 1500));
            }
        }
        data.putDouble(TAG_X, current.x);
        data.putDouble(TAG_Y, current.y);
        data.putDouble(TAG_Z, current.z);
        data.putLong(TAG_MEMORY_TIME, now);
    }

    private static void tickCaveSpider(ServerLevel level, CaveSpider spider, long now) {
        CompoundTag data = spider.getPersistentData();
        if (data.getInt(TAG_MODE) == 1) {
            if (spider.getTarget() == null || spider.hurtTime > 0 || now >= data.getLong(TAG_END)) {
                spider.setNoGravity(false);
                finish(spider, data, now, 900, 1900);
                return;
            }
            spider.getNavigation().stop();
            spider.setDeltaMovement(0.0D, now + 45L < data.getLong(TAG_END) ? 0.08D : 0.0D, 0.0D);
            if (now % 12L == 0L) {
                level.playSound(null, spider, SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 0.34F, 0.65F);
            }
            return;
        }
        if (!ready(data, now) || spider.getTarget() == null || spider.distanceToSqr(spider.getTarget()) < 4.0D * 4.0D
                || !hasWallAndCeiling(level, spider.blockPosition())) {
            return;
        }
        data.putInt(TAG_MODE, 1);
        data.putLong(TAG_END, now + 65L + spider.getRandom().nextInt(35));
        spider.setNoGravity(true);
    }

    private static void tickShulker(ServerLevel level, Shulker shulker, long now) {
        CompoundTag data = shulker.getPersistentData();
        if (shulker.getTarget() != null || shulker.hurtTime > 0) {
            if (data.getInt(TAG_MODE) == 1) {
                sendVisual(level, shulker, "shulker_empty_aim", false, shulker.position(), 0, 0L);
                finish(shulker, data, now, 800, 1600);
            }
            return;
        }
        if (data.getInt(TAG_MODE) == 1 && now >= data.getLong(TAG_END)) {
            sendVisual(level, shulker, "shulker_empty_aim", false, shulker.position(), 0, 0L);
            finish(shulker, data, now, 800, 1600);
        } else if (ready(data, now)) {
            Vec3 target = shulker.position().add(rotatedHorizontal(shulker, 3.0D)).add(0.0D, 0.6D, 0.0D);
            data.putInt(TAG_MODE, 1);
            data.putLong(TAG_END, now + 35L);
            sendVisual(level, shulker, "shulker_empty_aim", true, target, 35, shulker.getRandom().nextLong());
            level.playSound(null, shulker, SoundEvents.SHULKER_OPEN, SoundSource.HOSTILE, 0.35F, 0.75F);
        }
    }

    private static void tickGuardian(ServerLevel level, Guardian guardian, long now) {
        CompoundTag data = guardian.getPersistentData();
        if (guardian.getTarget() != null || guardian.hasActiveAttackTarget() || guardian.hurtTime > 0) {
            if (data.getInt(TAG_MODE) == 1) {
                sendVisual(level, guardian, "guardian_false_beam", false, guardian.position(), 0, 0L);
                finish(guardian, data, now, 900, 1800);
            }
            return;
        }
        if (data.getInt(TAG_MODE) == 1 && now >= data.getLong(TAG_END)) {
            sendVisual(level, guardian, "guardian_false_beam", false, guardian.position(), 0, 0L);
            finish(guardian, data, now, 900, 1800);
        } else if (ready(data, now)) {
            Vec3 target = guardian.position().add(rotatedHorizontal(guardian, 4.0D)).add(0.0D, 0.3D, 0.0D);
            if (level.getFluidState(BlockPos.containing(target)).isEmpty()) {
                data.putLong(TAG_NEXT, now + 200L);
                return;
            }
            data.putInt(TAG_MODE, 1);
            data.putLong(TAG_END, now + 24L);
            sendVisual(level, guardian, "guardian_false_beam", true, target, 24, guardian.getRandom().nextLong());
            level.playSound(null, guardian, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.24F, 0.7F);
        }
    }

    private static void tickVex(ServerLevel level, Vex vex, long now) {
        CompoundTag data = vex.getPersistentData();
        ServerPlayer observer = nearestPlayer(level, vex, 14.0D);
        if (data.getInt(TAG_MODE) == 1) {
            if (now >= data.getLong(TAG_END) || observer == null || !isLookingAt(observer, vex, 0.9D)) {
                finish(vex, data, now, 800, 1600);
                return;
            }
            vex.setIsCharging(false);
            vex.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (ready(data, now) && vex.isInWall() && observer != null && isLookingAt(observer, vex, 0.94D)) {
            data.putInt(TAG_MODE, 1);
            data.putLong(TAG_END, now + 12L);
        }
    }

    private static void tickSilverfish(ServerLevel level, Silverfish silverfish, long now) {
        CompoundTag data = silverfish.getPersistentData();
        if (data.getInt(TAG_MODE) == 1) {
            BlockPos stone = storedPos(data);
            if (now >= data.getLong(TAG_END) || silverfish.position().distanceToSqr(Vec3.atCenterOf(stone)) < 1.3D) {
                BlockState state = level.getBlockState(stone);
                level.playSound(null, silverfish, SoundEvents.SILVERFISH_STEP, SoundSource.HOSTILE, 0.35F, 0.68F);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        stone.getX() + 0.5D, stone.getY() + 0.45D, stone.getZ() + 0.5D,
                        5, 0.15D, 0.15D, 0.15D, 0.015D);
                finish(silverfish, data, now, 750, 1500);
                return;
            }
            silverfish.getNavigation().moveTo(stone.getX() + 0.5D, stone.getY(), stone.getZ() + 0.5D, 1.1D);
            return;
        }
        if (ready(data, now) && (silverfish.getTarget() != null || silverfish.hurtTime > 0)) {
            BlockPos stone = findNormalStone(level, silverfish.blockPosition());
            if (stone != null) {
                start(data, stone, now + 30L);
            }
        }
    }

    private static void tickPiglinProcession(ServerLevel level, ZombifiedPiglin piglin, long now) {
        CompoundTag data = piglin.getPersistentData();
        if (piglin.isAngry() || piglin.getTarget() != null || piglin.hurtTime > 0) {
            cancelMovementCue(piglin, data);
            return;
        }
        if (data.getInt(TAG_MODE) == 1) {
            if (now >= data.getLong(TAG_END)) {
                finish(piglin, data, now, 1500, 3200);
                return;
            }
            List<ZombifiedPiglin> group = level.getEntitiesOfClass(
                    ZombifiedPiglin.class, piglin.getBoundingBox().inflate(10.0D),
                    candidate -> !candidate.isAngry() && candidate.getTarget() == null && candidate.isAlive());
            BlockPos destination = storedPos(data);
            Vec3 forward = Vec3.atCenterOf(destination).subtract(piglin.position());
            if (forward.lengthSqr() < 1.0D) {
                piglin.getNavigation().stop();
                return;
            }
            forward = forward.normalize();
            int limit = Math.min(5, group.size());
            for (int index = 0; index < limit; index++) {
                ZombifiedPiglin member = group.get(index);
                Vec3 slot = Vec3.atCenterOf(destination).subtract(forward.scale(index * 1.25D));
                member.getNavigation().moveTo(slot.x, slot.y, slot.z, 0.86D);
            }
            return;
        }
        if (ready(data, now)) {
            List<ZombifiedPiglin> group = level.getEntitiesOfClass(
                    ZombifiedPiglin.class, piglin.getBoundingBox().inflate(8.0D),
                    candidate -> !candidate.isAngry() && candidate.getTarget() == null && candidate.isAlive());
            if (group.size() >= 3) {
                Vec3 destination = piglin.position().add(rotatedHorizontal(piglin, 6.0D));
                start(data, BlockPos.containing(destination), now + 160L);
            } else {
                data.putLong(TAG_NEXT, now + 300L);
            }
        }
    }

    private static void applyVariant(
            Mob mob,
            ApprovedVanillaVariantCatalog.Variant variant,
            long now,
            boolean dev) {
        CompoundTag data = mob.getPersistentData();
        data.putString(TAG_VARIANT, variant.id());
        data.putInt(TAG_MODE, 0);
        data.putLong(TAG_END, 0L);
        data.putLong(TAG_NEXT, dev ? now + 12L : now + cooldown(mob, 300, 900));
        data.putBoolean(TAG_DEV, dev);
        if (dev && "sniffer_second_dig".equals(variant.id())) {
            storePos(data, mob.blockPosition());
            data.putLong(TAG_MEMORY_TIME, now - 24000L);
            data.putLong(TAG_NEXT, now + 20L);
        }
    }

    private static void sendVisual(
            ServerLevel level,
            Mob entity,
            String effect,
            boolean active,
            Vec3 target,
            int duration,
            long seed) {
        UncannyVanillaVariantVisualPayload payload = new UncannyVanillaVariantVisualPayload(
                entity.getId(), effect, active, target.x, target.y, target.z, duration, seed);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) <= 64.0D * 64.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private static ApprovedVanillaVariantCatalog.Variant variantForType(EntityType<?> type) {
        String key = type.builtInRegistryHolder().key().location().getPath();
        return ApprovedVanillaVariantCatalog.byTypeKey(key);
    }

    private static EntityType<? extends Mob> resolveType(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "bee" -> EntityType.BEE;
            case "bat" -> EntityType.BAT;
            case "rabbit" -> EntityType.RABBIT;
            case "goat" -> EntityType.GOAT;
            case "horse" -> EntityType.HORSE;
            case "allay" -> EntityType.ALLAY;
            case "axolotl" -> EntityType.AXOLOTL;
            case "dolphin" -> EntityType.DOLPHIN;
            case "frog" -> EntityType.FROG;
            case "turtle" -> EntityType.TURTLE;
            case "sniffer" -> EntityType.SNIFFER;
            case "armadillo" -> EntityType.ARMADILLO;
            case "glow_squid" -> EntityType.GLOW_SQUID;
            case "breeze" -> EntityType.BREEZE;
            case "cave_spider" -> EntityType.CAVE_SPIDER;
            case "shulker" -> EntityType.SHULKER;
            case "guardian" -> EntityType.GUARDIAN;
            case "vex" -> EntityType.VEX;
            case "silverfish" -> EntityType.SILVERFISH;
            case "zombified_piglin" -> EntityType.ZOMBIFIED_PIGLIN;
            default -> null;
        };
    }

    private static Vec3 findDevSpawnPosition(ServerLevel level, ServerPlayer player, EntityType<?> type) {
        if (type == EntityType.AXOLOTL || type == EntityType.DOLPHIN || type == EntityType.GLOW_SQUID
                || type == EntityType.GUARDIAN || type == EntityType.TURTLE) {
            BlockPos origin = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -5, -8), origin.offset(8, 5, 8))) {
                if (!level.getFluidState(pos).isEmpty() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                    return Vec3.atCenterOf(pos);
                }
            }
        }
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        horizontal = horizontal.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
        Vec3 desired = player.position().add(horizontal.scale(3.2D));
        BlockPos ground = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(desired));
        return new Vec3(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D);
    }

    private static boolean isNaturalSpawn(MobSpawnType type) {
        return switch (type) {
            case SPAWNER, SPAWN_EGG, COMMAND, DISPENSER, TRIAL_SPAWNER, BUCKET, BREEDING,
                    MOB_SUMMONED, TRIGGERED -> false;
            default -> true;
        };
    }

    private static boolean is(Mob mob, String id) {
        return id.equals(mob.getPersistentData().getString(TAG_VARIANT));
    }

    private static boolean ready(CompoundTag data, long now) {
        return data.getInt(TAG_MODE) == 0 && now >= data.getLong(TAG_NEXT);
    }

    private static void start(CompoundTag data, BlockPos target, long end) {
        data.putInt(TAG_MODE, 1);
        data.putLong(TAG_END, end);
        storePos(data, target);
    }

    private static void finish(Mob mob, CompoundTag data, long now, int minimum, int spread) {
        data.putInt(TAG_MODE, 0);
        data.putLong(TAG_END, 0L);
        mob.getNavigation().stop();
        data.putLong(TAG_NEXT, now + cooldown(mob, minimum, spread));
    }

    private static void cancelMovementCue(Mob mob, CompoundTag data) {
        if (data.getInt(TAG_MODE) != 0) {
            data.putInt(TAG_MODE, 0);
            data.putLong(TAG_END, 0L);
            mob.setNoGravity(false);
        }
    }

    private static long cooldown(Mob mob, int minimum, int spread) {
        if (mob.getPersistentData().getBoolean(TAG_DEV)) {
            return Math.max(80, minimum / 5);
        }
        return minimum + mob.getRandom().nextInt(Math.max(1, spread));
    }

    private static void storePos(CompoundTag data, BlockPos pos) {
        data.putDouble(TAG_X, pos.getX());
        data.putDouble(TAG_Y, pos.getY());
        data.putDouble(TAG_Z, pos.getZ());
    }

    private static BlockPos storedPos(CompoundTag data) {
        return BlockPos.containing(data.getDouble(TAG_X), data.getDouble(TAG_Y), data.getDouble(TAG_Z));
    }

    private static Vec3 rotatedHorizontal(Entity entity, double distance) {
        double angle = (entity.getYRot() + 70.0D + entity.getId() * 17.0D) * Mth.DEG_TO_RAD;
        return new Vec3(-Math.sin(angle) * distance, 0.0D, Math.cos(angle) * distance);
    }

    private static ServerPlayer nearestPlayer(ServerLevel level, Entity entity, double radius) {
        return level.getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), radius, false) instanceof ServerPlayer player
                ? player
                : null;
    }

    private static boolean isLookingAt(ServerPlayer player, Entity entity, double threshold) {
        if (!player.hasLineOfSight(entity)) {
            return false;
        }
        Vec3 toEntity = entity.getEyePosition().subtract(player.getEyePosition());
        return toEntity.lengthSqr() > 0.001D
                && player.getViewVector(1.0F).normalize().dot(toEntity.normalize()) >= threshold;
    }

    private static BlockPos findOrdinaryHiveFace(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos candidate = origin.offset(
                    random.nextInt(9) - 4,
                    random.nextInt(5) - 2,
                    random.nextInt(9) - 4);
            if (isOrdinaryHiveFace(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isOrdinaryHiveFace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && !state.is(Blocks.BEEHIVE)
                && !state.is(Blocks.BEE_NEST)
                && state.isSolidRender(level, pos)
                && Direction.stream().anyMatch(direction -> level.getBlockState(pos.relative(direction)).isAir());
    }

    private static BlockPos findNearbySolid(ServerLevel level, BlockPos origin, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).isSolidRender(level, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static BlockPos findNormalStone(ServerLevel level, BlockPos origin) {
        for (Direction direction : Direction.values()) {
            BlockPos pos = origin.relative(direction);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLESTONE)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean hasWallAndCeiling(ServerLevel level, BlockPos pos) {
        boolean wall = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).isSolidRender(level, pos.relative(direction))) {
                wall = true;
                break;
            }
        }
        if (!wall) {
            return false;
        }
        for (int height = 1; height <= 4; height++) {
            BlockPos ceiling = pos.above(height);
            if (level.getBlockState(ceiling).isSolidRender(level, ceiling)) {
                return true;
            }
        }
        return false;
    }
}
