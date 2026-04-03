package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.Optional;
import java.util.function.Predicate;

public final class UncannyPassiveVariantSystem {
    private static final String TAG_ENABLED = "UncannyPassiveEnabled";
    private static final String TAG_TYPE = "UncannyPassiveType";
    private static final String TAG_VARIANT = "UncannyPassiveVariant";
    private static final String TAG_TIMER = "UncannyPassiveTimer";
    private static final String TAG_TRIGGERED = "UncannyPassiveTriggered";
    private static final String TAG_LAST_EAT = "UncannyPassiveLastEat";
    private static final String TAG_ALARM_END = "UncannyPassiveAlarmEnd";
    private static final String TAG_FAKE_END = "UncannyPassiveFakeEnd";
    private static final String TAG_FAKE_REAWAKE = "UncannyPassiveFakeReawake";
    private static final String TAG_FAKE_USED = "UncannyPassiveFakeUsed";
    private static final String TAG_SHEEP_PANIC_END = "UncannyPassiveSheepPanicEnd";
    private static final String TAG_PIG_CARCASS = "UncannyPassivePigCarcass";
    private static final String TAG_PIG_CARCASS_EXPIRE = "UncannyPassivePigCarcassExpire";
    private static final String TAG_CHICKEN_SLIDE_X = "UncannyPassiveChickenSlideX";
    private static final String TAG_CHICKEN_SLIDE_Z = "UncannyPassiveChickenSlideZ";
    private static final String TAG_CHICKEN_SLIDE_TICK = "UncannyPassiveChickenSlideTick";
    private static final String TAG_CAT_GIFT_DAY = "UncannyPassiveCatGiftDay";
    private static final String TAG_FISH_GAZE = "UncannyPassiveFishGazeTicks";
    private static final String TAG_PARROT_ALARM_END = "UncannyPassiveParrotAlarmEnd";
    private static final String TAG_VILLAGER_OFFER_INIT = "UncannyPassiveVillagerOfferInit";
    private static final String TAG_TRADER_OFFER_INIT = "UncannyPassiveTraderOfferInit";
    private static final String TAG_LLAMA_BLACK_MARKER = "eotv_black_llama";
    private static final String TEAM_LLAMA_BLACK = "eotv_black_llama";
    private static final String TAG_CAT_REVIVES = "UncannyPassiveCatRevives";
    private static final String TAG_CAT_SENSE_THIEF_COOLDOWN = "UncannyPassiveCatSenseThiefCooldown";
    private static final int CAT_MAX_REVIVES = 8;
    private static final int FALSE_EGG_FUSE_TICKS = 20 * 5;

    private static final List<FalseEggState> FALSE_EGGS = new ArrayList<>();
    private static long lastEggProcessTick = Long.MIN_VALUE;

    private UncannyPassiveVariantSystem() {
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!UncannyWorldState.get(serverLevel.getServer()).isSystemEnabled()) {
            return;
        }
        if (mob instanceof IronGolem ironGolem && ironGolem.isPlayerCreated()) {
            return;
        }
        if (!isSupportedPassive(mob.getType())) {
            return;
        }
        if (!isReplacementEligibleSpawnType(event.getSpawnType())) {
            return;
        }

        UncannyPhase phase = UncannyWorldState.get(serverLevel.getServer()).getPhase();
        // Use a local RNG here: FinalizeSpawn can run on chunk worker threads, so serverLevel.random is unsafe.
        RandomSource localRandom = RandomSource.create(
                mob.getUUID().getMostSignificantBits()
                        ^ mob.getUUID().getLeastSignificantBits()
                        ^ serverLevel.getGameTime());

        if (localRandom.nextDouble() > phase.replacementChance()) {
            return;
        }

        applyPassiveVariantTag(
                mob,
                rollVariantForPhase(phase, localRandom.nextInt(100)),
                serverLevel.getServer().getTickCount());
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LlamaSpit spit
                && spit.getOwner() instanceof Llama owner
                && isVariant(owner, "llama", 1)) {
            spit.setInvisible(true);
            spit.setSilent(true);
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!UncannyWorldState.get(serverLevel.getServer()).isSystemEnabled()) {
            return;
        }

        processFalseEggs(serverLevel);

        if (UncannyEntityRegistry.isSpecialEntity(mob.getType())) {
            preventSpecialPowderedSnowSink(mob);
        }

        if (!isUncannyPassive(mob)) {
            return;
        }

        String type = getTypeKey(mob);
        int variant = getVariant(mob);
        if (type.equals("chicken")) {
            tickChicken(serverLevel, (Chicken) mob, variant);
        } else if (type.equals("pig")) {
            tickPig(serverLevel, (Pig) mob, variant);
        } else if (type.equals("cow")) {
            tickCow(serverLevel, (Cow) mob, variant);
        } else if (type.equals("sheep")) {
            tickSheep(serverLevel, (Sheep) mob, variant);
        } else if (type.equals("wolf")) {
            tickWolf(serverLevel, (Wolf) mob, variant);
        } else if (type.equals("cat")) {
            tickCat(serverLevel, (Cat) mob, variant);
        } else if (type.equals("fox")) {
            tickFox(serverLevel, (Fox) mob, variant);
        } else if (type.equals("squid")) {
            tickSquid(serverLevel, (Squid) mob, variant);
        } else if (type.equals("cod") || type.equals("salmon")) {
            tickFish(serverLevel, mob, variant);
        } else if (type.equals("parrot")) {
            tickParrot(serverLevel, (Parrot) mob, variant);
        } else if (type.equals("llama")) {
            tickLlama(serverLevel, (Llama) mob, variant);
        } else if (type.equals("villager")) {
            tickVillager(serverLevel, (Villager) mob, variant);
        } else if (type.equals("wandering_trader")) {
            tickWanderingTrader(serverLevel, (WanderingTrader) mob, variant);
        }
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !(living.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (UncannyEntityRegistry.isSpecialEntity(living.getType()) && event.getSource().is(DamageTypes.FALL)) {
            event.setCanceled(true);
            return;
        }
        if (!UncannyWorldState.get(serverLevel.getServer()).isSystemEnabled()) {
            return;
        }
        long now = serverLevel.getServer().getTickCount();

        if (living instanceof Chicken chicken && isVariant(chicken, "chicken", 5)) {
            if (chicken.getPersistentData().getLong(TAG_ALARM_END) <= now) {
                playSound(serverLevel, chicken, SoundEvents.GHAST_SCREAM, 1.7F, 0.45F);
            }
            chicken.getPersistentData().putLong(TAG_ALARM_END, now + 20L * 10L);
        }

        if (living instanceof Pig pig && isVariant(pig, "pig", 5) && !pig.getPersistentData().getBoolean(TAG_TRIGGERED)) {
            pig.getPersistentData().putBoolean(TAG_TRIGGERED, true);
            playSound(serverLevel, pig, SoundEvents.GHAST_SCREAM, 1.8F, 0.45F);
            teleportAround(pig, 10.0D);
        }

        if (living instanceof Cow cow && isVariant(cow, "cow", 4)) {
            tryFakeDeath(cow, event, SoundEvents.COW_DEATH);
        }

        if (living instanceof Sheep sheep && isVariant(sheep, "sheep", 4)) {
            sheep.getPersistentData().putLong(TAG_SHEEP_PANIC_END, serverLevel.getServer().getTickCount() + 20L * 4L);
        }

        if (living instanceof Cat cat && isVariant(cat, "cat", 4)) {
            // Not invincible anymore: handled by capped revive logic in onLivingDeath.
        }

        if (living instanceof Fox fox) {
            if (isVariant(fox, "fox", 4)) {
                tryFoxFakeDeath(fox, event);
            } else if (isVariant(fox, "fox", 5) && event.getSource().getEntity() instanceof Player player) {
                swapPositions(serverLevel, fox, player);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false, true));
                playSound(serverLevel, player, SoundEvents.GLASS_BREAK, 1.35F, 0.9F);
            }
        }

        if (living instanceof WanderingTrader trader && isVariant(trader, "wandering_trader", 4)
                && event.getSource().getEntity() instanceof ServerPlayer) {
            // Death trigger only (handled in onLivingDeath) to avoid blackout spam on each hit.
        }

        if (living instanceof Player player) {
            Entity direct = event.getSource().getDirectEntity();
            Entity attacker = event.getSource().getEntity();

            if (attacker instanceof Cat cat && isVariant(cat, "cat", 5)) {
                event.setAmount(Math.max(event.getAmount(), 1.0F));
                triggerCatSenseThief(serverLevel, cat, player, now);
            }

            if (attacker instanceof Squid squid && isVariant(squid, "squid", 5)) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
            }

            Llama llamaDamager = resolveLlamaDamager(attacker, direct);
            if (llamaDamager != null && isVariant(llamaDamager, "llama", 5)) {
                event.setAmount(1.0F);
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                if (player instanceof ServerPlayer serverPlayer) {
                    UncannyParanoiaEventSystem.applyTemporaryDeafness(serverPlayer, 60);
                }
            } else if (llamaDamager != null && isVariant(llamaDamager, "llama", 1)) {
                event.setAmount(Math.max(event.getAmount(), 1.0F));
            }
        }

        if (living instanceof Parrot parrot && isVariant(parrot, "parrot", 5)) {
            parrot.getPersistentData().putLong(TAG_PARROT_ALARM_END, now + 20L * 6L);
        }

        if (living instanceof Squid squid) {
            int variant = getVariant(squid);
            if (variant == 1 || variant == 5) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, squid.getX(), squid.getY() + 0.5D, squid.getZ(), 20, 0.35D, 0.25D, 0.35D, 0.01D);
            }
            if (variant == 4 && event.getSource().getEntity() instanceof ServerPlayer player) {
                pullPlayerDown(serverLevel, squid, player);
            }
            if (variant == 5 && event.getSource().getEntity() instanceof ServerPlayer player) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
            }
        }
    }

    private static void preventSpecialPowderedSnowSink(Mob mob) {
        if (!mob.isAlive()) {
            return;
        }
        if (!mob.isInPowderSnow && !mob.level().getBlockState(mob.blockPosition()).is(Blocks.POWDER_SNOW)) {
            return;
        }

        mob.setTicksFrozen(0);
        Vec3 motion = mob.getDeltaMovement();
        double boostedUpward = Math.max(0.16D, motion.y);
        mob.setDeltaMovement(motion.x * 0.82D, boostedUpward, motion.z * 0.82D);
        mob.hasImpulse = true;

        BlockPos head = mob.blockPosition().above();
        if (mob.level().getBlockState(head).isAir()) {
            mob.setPos(mob.getX(), Math.max(mob.getY(), head.getY() + 0.02D), mob.getZ());
        } else {
            mob.setPos(mob.getX(), mob.getY() + 0.14D, mob.getZ());
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!UncannyWorldState.get(serverLevel.getServer()).isSystemEnabled()) {
            return;
        }

        if (event.getEntity() instanceof Chicken chicken && isVariant(chicken, "chicken", 3)) {
            dropFalseEgg(serverLevel, chicken.blockPosition(), chicken.getUUID());
        }

        if (event.getEntity() instanceof Cod cod
                && isVariant(cod, "cod", 4)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            int danger = Mth.clamp(UncannyConfig.EVENT_DANGER_LEVEL.get(), 0, 5);
            if (serverLevel.random.nextDouble() < drownedSpawnChanceForDanger(danger)) {
                spawnDrownedBehind(player);
            }
            return;
        }
        if (event.getEntity() instanceof Salmon salmon
                && isVariant(salmon, "salmon", 4)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            int danger = Mth.clamp(UncannyConfig.EVENT_DANGER_LEVEL.get(), 0, 5);
            if (serverLevel.random.nextDouble() < drownedSpawnChanceForDanger(danger)) {
                spawnDrownedBehind(player);
            }
            return;
        }

        if (event.getEntity() instanceof Fox fox && isVariant(fox, "fox", 3)) {
            playCustomSound(serverLevel, fox, UncannySoundRegistry.UNCANNY_FOX_SCREAM.get(), 1.28F, 1.0F);
        }

        if (event.getEntity() instanceof Cat cat && isVariant(cat, "cat", 4)) {
            int revives = Math.max(0, cat.getPersistentData().getInt(TAG_CAT_REVIVES));
            if (revives < CAT_MAX_REVIVES) {
                event.setCanceled(true);
                cat.getPersistentData().putInt(TAG_CAT_REVIVES, revives + 1);
                cat.setHealth(cat.getMaxHealth());
                cat.removeAllEffects();
                serverLevel.sendParticles(ParticleTypes.SMOKE, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 18, 0.22D, 0.25D, 0.22D, 0.01D);
                playSound(serverLevel, cat, SoundEvents.ENDERMAN_TELEPORT, 0.65F, 1.55F);
                return;
            }
        }

        if (event.getEntity() instanceof WanderingTrader trader
                && isVariant(trader, "wandering_trader", 4)
                && event.getSource().getEntity() instanceof ServerPlayer killer) {
            UncannyParanoiaEventSystem.triggerTotalBlackout(killer);
        }

        if (!(event.getEntity() instanceof Pig pig) || !(pig.level() instanceof ServerLevel level)) {
            return;
        }
        if (!isVariant(pig, "pig", 3) || pig.getPersistentData().getBoolean(TAG_PIG_CARCASS)) {
            return;
        }

        Pig carcass = EntityType.PIG.create(level);
        if (carcass == null) {
            return;
        }

        carcass.moveTo(pig.getX(), pig.getY(), pig.getZ(), pig.getYRot(), pig.getXRot());
        applyPassiveVariantTag(carcass, 3, level.getServer().getTickCount());
        carcass.getPersistentData().putBoolean(TAG_PIG_CARCASS, true);
        carcass.getPersistentData().putLong(TAG_PIG_CARCASS_EXPIRE, level.getServer().getTickCount() + 20L * 90L);
        carcass.setNoAi(true);
        carcass.setSilent(true);
        level.addFreshEntity(carcass);
    }

    public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) {
            return;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        if (handlePlayerEntityInteract(player, event.getTarget(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static void onPlayerEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) {
            return;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        if (handlePlayerEntityInteract(player, event.getTarget(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean handlePlayerEntityInteract(ServerPlayer player, Entity target, InteractionHand hand) {
        if (!(target instanceof Mob mob) || !isUncannyPassive(mob)) {
            return false;
        }

        ItemStack held = player.getItemInHand(hand);

        if (mob instanceof Cow cow && isVariant(cow, "cow", 5) && held.is(Items.BUCKET)) {
            player.hurt(player.damageSources().generic(), 2.0F);
            player.level().playSound(null, player, SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.1F, 0.65F);
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.SMOKE, cow.getX(), cow.getY() + 0.7D, cow.getZ(), 20, 0.35D, 0.4D, 0.35D, 0.02D);
            cow.discard();
            return true;
        }

        if (mob instanceof Sheep sheep && isVariant(sheep, "sheep", 5) && held.is(Items.SHEARS) && !sheep.isSheared()) {
            sheep.setSheared(true);
            int amount = 1 + sheep.getRandom().nextInt(3);
            sheep.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, amount));
            sheep.playSound(SoundEvents.SHEEP_HURT, 1.0F, 1.0F);
            sheep.hurt(player.damageSources().playerAttack(player), 2.0F);
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, sheep.getX(), sheep.getY() + 0.8D, sheep.getZ(), 12, 0.35D, 0.2D, 0.35D, 0.01D);
            ((ServerLevel) player.level()).sendParticles(DustParticleOptions.REDSTONE, sheep.getX(), sheep.getY() + 0.8D, sheep.getZ(), 18, 0.3D, 0.2D, 0.3D, 0.0D);
            held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            return true;
        }

        if (mob instanceof Wolf wolf && isVariant(wolf, "wolf", 2)) {
            wolf.playSound(SoundEvents.WOLF_GROWL, 0.95F, 0.8F);
            Vec3 away = wolf.position().subtract(player.position()).normalize().scale(0.5D);
            wolf.setDeltaMovement(away.x, 0.16D, away.z);
            return true;
        }

        if ((mob instanceof Cod || mob instanceof Salmon) && getVariant(mob) == 3 && held.is(Items.WATER_BUCKET)) {
            playSound((ServerLevel) player.level(), mob, SoundEvents.GLASS_BREAK, 1.1F, 1.0F);
            mob.discard();
            player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
            return true;
        }

        if (mob instanceof Villager villager) {
            int variant = getVariant(villager);
            if (variant == 2) {
                playSound((ServerLevel) player.level(), villager, SoundEvents.VILLAGER_AMBIENT, 1.0F, 0.45F);
                player.closeContainer();
                return true;
            }
            if (variant == 4 && held.is(Items.DIRT) && held.getCount() > 0) {
                held.shrink(1);
                ItemStack help = new ItemStack(Items.ROTTEN_FLESH);
                help.set(DataComponents.CUSTOM_NAME, Component.literal("Help me"));
                if (!player.addItem(help)) {
                    player.drop(help, false);
                }
                return true;
            }
        }

        if (mob instanceof WanderingTrader trader) {
            int variant = getVariant(trader);
            if (variant == 1) {
                trader.setInvisible(true);
            }
        }
        return false;
    }

    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        if (!(event.getAbstractVillager() instanceof Mob mob) || !isUncannyPassive(mob)) {
            return;
        }

        if (mob instanceof Villager villager && isVariant(villager, "villager", 5)) {
            UncannyParanoiaEventSystem.triggerFlashError(player);
            if (player.level().random.nextBoolean()) {
                UncannyParanoiaEventSystem.spawnStalker(player);
            }
            villager.discard();
            player.closeContainer();
            return;
        }

        if (mob instanceof WanderingTrader trader) {
            int variant = getVariant(trader);
            if (variant == 2) {
                ServerLevel level = (ServerLevel) player.level();
                level.sendParticles(ParticleTypes.SMOKE, trader.getX(), trader.getY() + 0.8D, trader.getZ(), 20, 0.35D, 0.35D, 0.35D, 0.02D);
                trader.discard();
                player.closeContainer();
                return;
            }
            if (variant == 5) {
                playSound((ServerLevel) player.level(), trader, SoundEvents.ZOMBIE_VILLAGER_CURE, 2.0F, 0.45F);
                trader.kill();
                player.closeContainer();
            }
        }
    }

    public static void onAnimalTame(AnimalTameEvent event) {
        // Reserved for future tame-state hooks.
    }

    public static int forcePassiveVariantsAround(ServerPlayer centerPlayer, int radius, int variant) {
        ServerLevel level = centerPlayer.serverLevel();
        AABB box = centerPlayer.getBoundingBox().inflate(radius);
        int count = 0;

        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> isSupportedPassive(mob.getType()))) {
            if (applyPassiveVariantTag(mob, variant, level.getServer().getTickCount())) {
                count++;
            }
        }
        return count;
    }

    public static boolean spawnPassiveVariantForCommand(ServerPlayer centerPlayer, String typeKey, int variant) {
        if (centerPlayer == null || centerPlayer.getServer() == null || typeKey == null) {
            return false;
        }

        EntityType<? extends Mob> entityType = resolvePassiveEntityType(typeKey);
        if (entityType == null) {
            return false;
        }

        ServerLevel level = centerPlayer.serverLevel();
        Mob mob = entityType.create(level);
        if (mob == null) {
            return false;
        }

        int chosenVariant = variant;
        if (chosenVariant <= 0) {
            UncannyPhase phase = UncannyWorldState.get(level.getServer()).getPhase();
            chosenVariant = rollVariantForPhase(phase, level.random.nextInt(100));
        }
        chosenVariant = Mth.clamp(chosenVariant, 1, 5);

        if (!applyPassiveVariantTag(mob, chosenVariant, level.getServer().getTickCount())) {
            return false;
        }

        Vec3 look = centerPlayer.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 spawnPos = centerPlayer.position().add(horizontal.scale(2.2D)).add(0.0D, 0.8D, 0.0D);
        if (entityType == EntityType.COD || entityType == EntityType.SALMON || entityType == EntityType.SQUID) {
            BlockPos waterPos = findNearbyWaterPos(level, centerPlayer.blockPosition(), 12);
            if (waterPos != null) {
                spawnPos = new Vec3(waterPos.getX() + 0.5D, waterPos.getY() + 0.1D, waterPos.getZ() + 0.5D);
            }
        }

        mob.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, centerPlayer.getYRot(), 0.0F);
        level.addFreshEntity(mob);
        return true;
    }

    public static boolean forceFoxCry(ServerPlayer centerPlayer) {
        ServerLevel level = centerPlayer.serverLevel();
        AABB box = centerPlayer.getBoundingBox().inflate(48.0D);
        Fox fox = level.getEntitiesOfClass(Fox.class, box, LivingEntity::isAlive)
                .stream()
                .filter(candidate -> isVariant(candidate, "fox", 3))
                .min((a, b) -> Double.compare(a.distanceToSqr(centerPlayer), b.distanceToSqr(centerPlayer)))
                .orElse(null);
        if (fox == null) {
            fox = level.getEntitiesOfClass(Fox.class, box, LivingEntity::isAlive)
                    .stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(centerPlayer), b.distanceToSqr(centerPlayer)))
                    .orElse(null);
            if (fox == null) {
                return false;
            }
            applyPassiveVariantTag(fox, 3, level.getServer().getTickCount());
        }
        playCustomSound(level, fox, UncannySoundRegistry.UNCANNY_FOX_SCREAM.get(), 1.45F, 1.0F);
        return true;
    }

    private static void tickChicken(ServerLevel level, Chicken chicken, int variant) {
        long now = level.getServer().getTickCount();
        if (variant != 4 && chicken.isNoAi()) {
            chicken.setNoAi(false);
        }
        switch (variant) {
            case 1 -> {
                if (chicken.onGround() && chicken.getDeltaMovement().horizontalDistanceSqr() > 0.001D && now % 8L == 0L) {
                    BlockState below = level.getBlockState(chicken.blockPosition().below());
                    playAtPos(level, chicken.position(), below.getSoundType().getStepSound(), 0.12F, 1.0F);
                }
            }
            case 2 -> {
                Player nearest = level.getNearestPlayer(chicken, 24.0D);
                if (nearest != null) {
                    forceLookAt(chicken, nearest);
                }
            }
            case 3 -> {
                chicken.setSilent(true);
            }
            case 4 -> {
                if (!chicken.isNoAi()) {
                    chicken.setNoAi(true);
                }
                long nextSlide = chicken.getPersistentData().getLong(TAG_CHICKEN_SLIDE_TICK);
                if (now >= nextSlide) {
                    double vx = (chicken.getRandom().nextDouble() - 0.5D) * 0.24D;
                    double vz = (chicken.getRandom().nextDouble() - 0.5D) * 0.24D;
                    chicken.getPersistentData().putDouble(TAG_CHICKEN_SLIDE_X, vx);
                    chicken.getPersistentData().putDouble(TAG_CHICKEN_SLIDE_Z, vz);
                    chicken.getPersistentData().putLong(TAG_CHICKEN_SLIDE_TICK, now + 8L + chicken.getRandom().nextInt(8));
                }
                double slideX = chicken.getPersistentData().getDouble(TAG_CHICKEN_SLIDE_X);
                double slideZ = chicken.getPersistentData().getDouble(TAG_CHICKEN_SLIDE_Z);
                chicken.setPos(chicken.getX() + slideX, chicken.getY(), chicken.getZ() + slideZ);
                chicken.setDeltaMovement(0.0D, Math.min(0.0D, chicken.getDeltaMovement().y), 0.0D);
                chicken.walkAnimation.setSpeed(0.0F);
            }
            case 5 -> {
                long end = chicken.getPersistentData().getLong(TAG_ALARM_END);
                if (end > now) {
                    chicken.getNavigation().stop();
                    chicken.setDeltaMovement(0.0D, Math.min(0.0D, chicken.getDeltaMovement().y), 0.0D);
                }
            }
            default -> {
            }
        }
    }

    private static void tickPig(ServerLevel level, Pig pig, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 3 -> {
                if (pig.getPersistentData().getBoolean(TAG_PIG_CARCASS)) {
                    pig.setNoAi(true);
                    pig.setSilent(true);
                    Player nearby = level.getNearestPlayer(pig, 36.0D);
                    long expire = pig.getPersistentData().getLong(TAG_PIG_CARCASS_EXPIRE);
                    if (nearby == null || now >= expire) {
                        pig.discard();
                    }
                }
            }
            case 1 -> {
                pig.setSilent(true);
                if (shouldPlay(now, pig, 85, 130)) {
                    playSound(level, pig, SoundEvents.HOGLIN_AMBIENT, 0.7F, 0.72F + pig.getRandom().nextFloat() * 0.12F);
                }
            }
            case 2 -> {
                Vec3 delta = pig.getDeltaMovement();
                if (delta.horizontalDistanceSqr() > 0.0008D) {
                    float moveYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
                    float reversed = moveYaw + 180.0F;
                    pig.setYRot(reversed);
                    pig.setYHeadRot(reversed);
                }
            }
            case 4 -> {
                long lastEat = pig.getPersistentData().getLong(TAG_LAST_EAT);
                if (lastEat == Long.MIN_VALUE || now < lastEat || now - lastEat >= 20L * 60L) {
                    BlockPos below = pig.blockPosition().below();
                    BlockPos eatPos = isEdibleSurface(level, below) ? below : findNearbyEdibleSurface(level, pig.blockPosition(), 5);
                    if (eatPos != null) {
                        BlockState state = level.getBlockState(eatPos);
                        if (state.is(Blocks.GRASS_BLOCK)) {
                            level.setBlockAndUpdate(eatPos, Blocks.DIRT.defaultBlockState());
                        } else if (state.is(Blocks.FARMLAND)) {
                            level.setBlockAndUpdate(eatPos, Blocks.DIRT.defaultBlockState());
                        } else {
                            level.destroyBlock(eatPos, false);
                        }
                    }
                    pig.getPersistentData().putLong(TAG_LAST_EAT, now);
                    playSound(level, pig, SoundEvents.HOGLIN_RETREAT, 1.35F, 0.40F);
                }
            }
            default -> {
            }
        }
    }

    private static void tickCow(ServerLevel level, Cow cow, int variant) {
        long now = level.getServer().getTickCount();
        long fakeEnd = cow.getPersistentData().getLong(TAG_FAKE_END);
        if (fakeEnd > now) {
            cow.setNoAi(true);
            Player nearest = level.getNearestPlayer(cow, 2.0D);
            if (nearest != null) {
                cow.getPersistentData().putLong(TAG_FAKE_END, now);
            }
            return;
        } else if (cow.isNoAi()) {
            cow.setNoAi(false);
            if (cow.getPersistentData().getBoolean(TAG_FAKE_REAWAKE)) {
                cow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 6, 1, false, false, true));
                playSound(level, cow, SoundEvents.COW_HURT, 1.2F, 0.8F);
                cow.getPersistentData().putBoolean(TAG_FAKE_REAWAKE, false);
            }
        }

        switch (variant) {
            case 1 -> {
                cow.setSilent(true);
                if (shouldPlay(now, cow, 100, 170)) {
                    Player nearest = level.getNearestPlayer(cow, 20.0D);
                    Vec3 origin = nearest != null ? behindPlayer(nearest, 5.0D) : cow.position();
                    playAtPos(level, origin, SoundEvents.COW_AMBIENT, 0.95F, 0.9F);
                }
            }
            case 2 -> {
                Player nearest = level.getNearestPlayer(cow, 20.0D);
                if (nearest != null) {
                    cow.getNavigation().stop();
                    cow.setDeltaMovement(0.0D, Math.min(0.0D, cow.getDeltaMovement().y), 0.0D);
                    forceLookAt(cow, nearest);
                }
            }
            case 3 -> {
                cow.setSilent(true);
                if (shouldPlay(now, cow, 90, 150)) {
                    playSound(level, cow, SoundEvents.COW_AMBIENT, 1.1F, 0.33F);
                }
            }
            default -> {
            }
        }
    }

    private static void tickSheep(ServerLevel level, Sheep sheep, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 1 -> {
                Player nearest = level.getNearestPlayer(sheep, 20.0D);
                // Variant should only shift color when the sheep is not visible to the player at all,
                // not only when the player is not directly looking at it.
                if (nearest != null && !isBroadlySeenBy(nearest, sheep) && now % 20L == 0L) {
                    DyeColor[] colors = DyeColor.values();
                    sheep.setColor(colors[sheep.getRandom().nextInt(colors.length)]);
                }
            }
            case 2 -> {
                if (sheep.getRandom().nextInt(160) == 0) {
                    playSound(level, sheep, SoundEvents.GLASS_BREAK, 0.8F, 1.35F);
                }
            }
            case 3 -> {
                Player nearest = level.getNearestPlayer(sheep, 24.0D);
                if (nearest != null && isDirectlyWatchedBy(nearest, sheep, 0.962D)) {
                    sheep.getNavigation().stop();
                    sheep.setDeltaMovement(0.0D, Math.min(0.0D, sheep.getDeltaMovement().y), 0.0D);
                } else {
                    sheep.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
                }
            }
            case 4 -> {
                long panicEnd = sheep.getPersistentData().getLong(TAG_SHEEP_PANIC_END);
                if (panicEnd > now) {
                    sheep.getNavigation().stop();
                    sheep.setDeltaMovement(0.0D, Math.min(0.0D, sheep.getDeltaMovement().y), 0.0D);
                    sheep.setSprinting(true);
                    if (now % 8L == 0L) {
                        playSound(level, sheep, SoundEvents.SHEEP_HURT, 1.2F, 1.35F);
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void tickWolf(ServerLevel level, Wolf wolf, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 1 -> wolf.setSilent(true);
            case 2 -> {
            }
            case 3 -> {
                if (shouldPlay(now, wolf, 1100, 1400)) {
                    Player nearest = level.getNearestPlayer(wolf, 28.0D);
                    if (nearest != null) {
                        playAtPos(level, nearest.getEyePosition(), SoundEvents.WOLF_GROWL, 0.85F, 0.9F);
                    }
                }
            }
            case 4 -> {
                wolf.setSilent(true);
                if (shouldPlay(now, wolf, 1100, 1400)) {
                    playSound(level, wolf, wolf.getRandom().nextBoolean() ? SoundEvents.CREEPER_PRIMED : SoundEvents.ARROW_SHOOT, 1.0F, 1.0F);
                }
            }
            case 5 -> {
                if (wolf.isTame() && wolf.getOwner() instanceof Player owner && owner.isAlive() && owner.getHealth() <= 6.0F) {
                    wolf.setTame(false, true);
                    wolf.setTarget(owner);
                }
            }
            default -> {
            }
        }
    }

    private static void tickCat(ServerLevel level, Cat cat, int variant) {
        long now = level.getServer().getTickCount();
        if (variant != 4) {
            cat.noPhysics = false;
            cat.setInvulnerable(false);
            cat.setSilent(false);
        }
        switch (variant) {
            case 1 -> {
                if (shouldPlay(now, cat, 60, 110)) {
                    Player nearest = level.getNearestPlayer(cat, 15.0D);
                    if (nearest != null) {
                        playAtPos(level, nearest.getEyePosition(), SoundEvents.CAT_PURR, 0.95F, 0.95F);
                    }
                }
            }
            case 2 -> {
                Player nearest = level.getNearestPlayer(cat, 22.0D);
                BlockPos chest = findNearbyBlock(level, cat.blockPosition(), 4, state -> state.is(Blocks.CHEST));
                BlockPos bed = findNearbyBlock(level, cat.blockPosition(), 4, state -> state.is(Blocks.RED_BED)
                        || state.is(Blocks.WHITE_BED) || state.is(Blocks.BLACK_BED) || state.is(Blocks.BLUE_BED)
                        || state.is(Blocks.BROWN_BED) || state.is(Blocks.CYAN_BED) || state.is(Blocks.GRAY_BED)
                        || state.is(Blocks.GREEN_BED) || state.is(Blocks.LIGHT_BLUE_BED) || state.is(Blocks.LIGHT_GRAY_BED)
                        || state.is(Blocks.LIME_BED) || state.is(Blocks.MAGENTA_BED) || state.is(Blocks.ORANGE_BED)
                        || state.is(Blocks.PINK_BED) || state.is(Blocks.PURPLE_BED) || state.is(Blocks.YELLOW_BED));
                BlockPos anchor = chest != null ? chest : bed;
                if (nearest != null && anchor != null) {
                    double distToAnchor = cat.position().distanceToSqr(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D);
                    if (distToAnchor > 1.35D * 1.35D) {
                        cat.setInSittingPose(false);
                        cat.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D, 1.1D);
                    } else {
                        cat.setInSittingPose(true);
                        cat.getNavigation().stop();
                        forceLookAtTwisted(cat, nearest);
                    }
                } else {
                    cat.setInSittingPose(false);
                }
            }
            case 3 -> {
                if (cat.isTame() && cat.getOwner() instanceof Player owner && owner.isSleeping()) {
                    long day = level.getDayTime() / 24000L;
                    if (cat.getPersistentData().getLong(TAG_CAT_GIFT_DAY) != day) {
                        ItemStack gift = level.random.nextBoolean() ? new ItemStack(Items.BONE) : new ItemStack(Items.ROTTEN_FLESH);
                        if (!owner.addItem(gift)) {
                            owner.drop(gift, false);
                        }
                        cat.getPersistentData().putLong(TAG_CAT_GIFT_DAY, day);
                    }
                }
            }
            case 4 -> {
                cat.setSilent(true);
                cat.setInvulnerable(false);
            }
            case 5 -> {
                Player nearest = level.getNearestPlayer(cat, 1.35D);
                if (nearest != null && nearest.isAlive() && !nearest.isSpectator()) {
                    triggerCatSenseThief(level, cat, nearest, now);
                }
            }
            default -> {
            }
        }
    }

    private static void tickFox(ServerLevel level, Fox fox, int variant) {
        long now = level.getServer().getTickCount();
        long fakeEnd = fox.getPersistentData().getLong(TAG_FAKE_END);
        if (fakeEnd > now) {
            fox.setNoAi(true);
            fox.setSitting(true);
            Player nearest = level.getNearestPlayer(fox, 2.0D);
            if (nearest != null) {
                fox.getPersistentData().putLong(TAG_FAKE_END, now);
            }
            return;
        } else if (fox.isNoAi()) {
            fox.setNoAi(false);
            fox.setSitting(false);
            if (fox.getPersistentData().getBoolean(TAG_FAKE_REAWAKE)) {
                fox.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 6, 1, false, false, true));
                fox.getPersistentData().putBoolean(TAG_FAKE_REAWAKE, false);
            }
        }

        switch (variant) {
            case 1 -> {
                fox.setSilent(true);
                if (level.isDay()) {
                    Player nearest = level.getNearestPlayer(fox, 24.0D);
                    if (nearest != null) {
                        fox.setSitting(true);
                        forceLookAt(fox, nearest);
                    }
                }
            }
            case 2 -> fox.setSilent(true);
            case 3 -> {
                // Death trigger only (handled in onLivingDeath).
            }
            case 5 -> {
                // handled on hit.
            }
            default -> {
            }
        }
    }

    private static void tickSquid(ServerLevel level, Squid squid, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 1 -> squid.setSilent(true);
            case 2 -> {
                squid.setDeltaMovement(Vec3.ZERO);
                squid.setXRot(-90.0F);
            }
            case 3 -> {
                Player nearest = level.getNearestPlayer(squid, 16.0D);
                if (nearest != null && nearest.isUnderWater() && shouldPlay(now, squid, 70, 120)) {
                    Vec3 behind = behindPlayer(nearest, 0.8D);
                    playAtPos(level, behind, SoundEvents.SQUID_AMBIENT, 0.65F, 0.82F);
                }
            }
            default -> {
            }
        }
    }

    private static void tickFish(ServerLevel level, Mob fish, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 1 -> {
                if (fish.getCustomName() == null || !"Dinnerbone".equals(fish.getCustomName().getString())) {
                    fish.setCustomName(Component.literal("Dinnerbone"));
                }
                fish.setCustomNameVisible(false);
            }
            case 2 -> {
                fish.setDeltaMovement(fish.getDeltaMovement().scale(0.15D));
                Player nearest = level.getNearestPlayer(fish, 5.0D);
                if (nearest != null) {
                    fish.discard();
                }
            }
            case 3 -> fish.setSilent(true);
            case 4 -> {
                Vec3 motion = fish.getDeltaMovement();
                if (motion.lengthSqr() < 0.0005D || fish.horizontalCollision) {
                    double angle = (now % 360L) * (Math.PI / 180.0D);
                    fish.setDeltaMovement(Math.cos(angle) * 0.06D, motion.y, Math.sin(angle) * 0.06D);
                }
            }
            case 5 -> {
                Player nearest = level.getNearestPlayer(fish, 14.0D);
                if (nearest == null) {
                    fish.getPersistentData().putInt(TAG_FISH_GAZE, 0);
                    return;
                }
                if (isDirectlyWatchedBy(nearest, fish, 0.97D)) {
                    int looked = fish.getPersistentData().getInt(TAG_FISH_GAZE) + 1;
                    fish.getPersistentData().putInt(TAG_FISH_GAZE, looked);
                    if (looked == 40) {
                        nearest.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                        playAtPos(level, nearest.getEyePosition(), UncannySoundRegistry.UNCANNY_HEARTBEAT.get(), 2.15F, 1.0F);
                    }
                } else {
                    fish.getPersistentData().putInt(TAG_FISH_GAZE, 0);
                }
            }
            default -> {
            }
        }
    }

    private static void tickParrot(ServerLevel level, Parrot parrot, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 1 -> {
                if (shouldPlay(now, parrot, 70, 130)) {
                    playSound(level, parrot, level.random.nextBoolean() ? SoundEvents.CREEPER_PRIMED : SoundEvents.ZOMBIE_AMBIENT, 0.9F, 1.0F);
                }
            }
            case 2 -> {
                if (shouldPlay(now, parrot, 80, 140)) {
                    Player nearest = level.getNearestPlayer(parrot, 24.0D);
                    if (nearest != null) {
                        SoundEvent sound = level.random.nextBoolean() ? SoundEvents.CREEPER_PRIMED : SoundEvents.ZOMBIE_AMBIENT;
                        playAtPos(level, nearest.getEyePosition(), sound, 0.9F, 1.0F);
                    }
                }
            }
            case 3 -> {
                if (shouldPlay(now, parrot, 70, 120)) {
                    SoundEvent sound = switch (level.random.nextInt(3)) {
                        case 0 -> SoundEvents.PLAYER_HURT;
                        case 1 -> SoundEvents.GENERIC_EAT;
                        default -> SoundEvents.STONE_STEP;
                    };
                    playSound(level, parrot, sound, 0.75F, 1.0F);
                }
            }
            case 4 -> {
                Player nearest = level.getNearestPlayer(parrot, 20.0D);
                if (nearest != null && isDirectlyWatchedBy(nearest, parrot, 0.96D)) {
                    parrot.getNavigation().stop();
                    parrot.setDeltaMovement(Vec3.ZERO);
                }
            }
            case 5 -> {
                long end = parrot.getPersistentData().getLong(TAG_PARROT_ALARM_END);
                if (end > now) {
                    parrot.setDeltaMovement(Vec3.ZERO);
                    parrot.setNoGravity(true);
                    if (now % 8L == 0L) {
                        playSound(level, parrot, SoundEvents.GHAST_HURT, 1.3F, 0.8F);
                    }
                } else {
                    parrot.setNoGravity(false);
                }
            }
            default -> {
            }
        }
    }

    private static void tickLlama(ServerLevel level, Llama llama, int variant) {
        long now = level.getServer().getTickCount();
        if (variant != 3 && llama.getTags().contains(TAG_LLAMA_BLACK_MARKER)) {
            llama.removeTag(TAG_LLAMA_BLACK_MARKER);
            removeLlamaFromBlackTeam(level, llama);
        }
        switch (variant) {
            case 1 -> {
                llama.setSilent(true);
                Player nearest = level.getNearestPlayer(llama, 12.0D);
                if (nearest != null && llama.hasLineOfSight(nearest) && shouldPlay(now, llama, 50, 90)) {
                    spawnGhostSpit(level, llama, nearest);
                }
            }
            case 2 -> {
                Player nearest = level.getNearestPlayer(llama, 48.0D);
                if (nearest != null) {
                    forceLookAtTwisted(llama, nearest);
                }
            }
            case 3 -> {
                llama.setSilent(true);
                llama.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
                if (llama.getCustomName() != null && "[eotv_black]".equals(llama.getCustomName().getString())) {
                    llama.setCustomName(null);
                }
                llama.setCustomNameVisible(false);
                if (!llama.getTags().contains(TAG_LLAMA_BLACK_MARKER)) {
                    llama.addTag(TAG_LLAMA_BLACK_MARKER);
                }
                assignLlamaToBlackTeam(level, llama);
            }
            case 4 -> {
                llama.setSilent(true);
                if (shouldPlay(now, llama, 60, 110)) {
                    playCustomSound(level, llama, UncannySoundRegistry.UNCANNY_MONSTER_BREATH.get(), 1.65F, 0.92F);
                }
            }
            default -> {
            }
        }
    }

    private static void tickVillager(ServerLevel level, Villager villager, int variant) {
        switch (variant) {
            case 1 -> {
                if (isNight(level)) {
                    villager.getNavigation().stop();
                    villager.setDeltaMovement(0.0D, Math.min(0.0D, villager.getDeltaMovement().y), 0.0D);
                }
            }
            case 3 -> {
                villager.setSilent(true);
                Player nearest = level.getNearestPlayer(villager, 24.0D);
                if (nearest != null) {
                    double dist = Math.sqrt(villager.distanceToSqr(nearest));
                    if (dist > 5.5D) {
                        villager.getNavigation().moveTo(nearest, 1.1D);
                    } else if (dist < 4.2D) {
                        Vec3 away = villager.position().subtract(nearest.position()).normalize().scale(0.22D);
                        villager.setDeltaMovement(away.x, villager.getDeltaMovement().y, away.z);
                    } else {
                        villager.getNavigation().stop();
                    }
                }
            }
            case 4 -> {
                if (level.getServer().getTickCount() % 20L == 0L || !villager.getPersistentData().getBoolean(TAG_VILLAGER_OFFER_INIT)) {
                    ensureVillagerMacabreOffer(villager);
                }
            }
            default -> {
            }
        }
    }

    private static void tickWanderingTrader(ServerLevel level, WanderingTrader trader, int variant) {
        long now = level.getServer().getTickCount();
        switch (variant) {
            case 1 -> trader.setInvisible(true);
            case 3 -> {
                if (shouldPlay(now, trader, 90, 170)) {
                    Player nearest = level.getNearestPlayer(trader, 28.0D);
                    if (nearest != null) {
                        playAtPos(level, nearest.getEyePosition(), SoundEvents.WANDERING_TRADER_AMBIENT, 0.85F, 1.0F);
                    }
                }
            }
            case 5 -> ensureTraderMessengerOffer(trader);
            default -> {
            }
        }
    }

    private static void tryFakeDeath(Cow cow, LivingIncomingDamageEvent event, SoundEvent deathSound) {
        if (cow.getPersistentData().getBoolean(TAG_FAKE_USED)) {
            return;
        }
        if (cow.getPersistentData().getLong(TAG_FAKE_END) > cow.level().getGameTime()) {
            return;
        }
        float threshold = cow.getMaxHealth() * 0.5F;
        if (cow.getHealth() - event.getAmount() > threshold) {
            return;
        }
        float newDamage = Math.max(0.0F, cow.getHealth() - threshold);
        event.setAmount(newDamage);
        cow.getPersistentData().putLong(TAG_FAKE_END, cow.level().getGameTime() + 60L);
        cow.getPersistentData().putBoolean(TAG_FAKE_REAWAKE, true);
        cow.getPersistentData().putBoolean(TAG_FAKE_USED, true);
        playSound((ServerLevel) cow.level(), cow, deathSound, 1.0F, 1.0F);
    }

    private static void dropFalseEgg(ServerLevel level, BlockPos near, UUID owner) {
        BlockPos pos = null;
        for (int r = 0; r <= 2 && pos == null; r++) {
            for (int dx = -r; dx <= r && pos == null; dx++) {
                for (int dz = -r; dz <= r && pos == null; dz++) {
                    BlockPos candidate = near.offset(dx, 0, dz);
                    if (level.getBlockState(candidate).isAir()) {
                        pos = candidate;
                    } else if (level.getBlockState(candidate.above()).isAir()) {
                        pos = candidate.above();
                    }
                }
            }
        }
        if (pos == null) {
            return;
        }
        level.setBlockAndUpdate(pos, UncannyBlockRegistry.UNCANNY_EGG.get().defaultBlockState());
        FALSE_EGGS.add(new FalseEggState(level.dimension(), pos.immutable(), owner, level.getServer().getTickCount() + FALSE_EGG_FUSE_TICKS));
    }

    private static void processFalseEggs(ServerLevel level) {
        long now = level.getServer().getTickCount();
        if (lastEggProcessTick == now) {
            return;
        }
        lastEggProcessTick = now;
        Iterator<FalseEggState> iterator = FALSE_EGGS.iterator();
        while (iterator.hasNext()) {
            FalseEggState egg = iterator.next();
            if (!Objects.equals(egg.dimension, level.dimension()) || now < egg.explodeTick) {
                continue;
            }
            if (level.getBlockState(egg.pos).is(UncannyBlockRegistry.UNCANNY_EGG.get())) {
                level.removeBlock(egg.pos, false);
                level.playSound(null, egg.pos, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 0.7F, 1.3F);
                AABB box = new AABB(egg.pos).inflate(3.0D);
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                }
            }
            iterator.remove();
        }
    }

    private static void forceLookAt(Mob mob, LivingEntity target) {
        Vec3 d = target.getEyePosition().subtract(mob.getEyePosition());
        float yaw = (float) (Mth.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
    }

    private static Vec3 behindPlayer(Player player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        return player.position().subtract(horizontal.scale(distance)).add(0.0D, 1.0D, 0.0D);
    }

    private static void teleportAround(Mob mob, double radius) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = radius * (0.6D + mob.getRandom().nextDouble() * 0.4D);
            double x = mob.getX() + Math.cos(angle) * distance;
            double z = mob.getZ() + Math.sin(angle) * distance;
            BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, mob.getY(), z));
            if (!level.getBlockState(top).isSolid()) {
                mob.teleportTo(x, top.getY(), z);
                return;
            }
        }
    }

    private static void tryFoxFakeDeath(Fox fox, LivingIncomingDamageEvent event) {
        if (fox.getPersistentData().getBoolean(TAG_FAKE_USED)) {
            return;
        }
        if (fox.getPersistentData().getLong(TAG_FAKE_END) > fox.level().getGameTime()) {
            return;
        }
        float threshold = fox.getMaxHealth() * 0.5F;
        if (fox.getHealth() - event.getAmount() > threshold) {
            return;
        }
        float newDamage = Math.max(0.0F, fox.getHealth() - threshold);
        event.setAmount(newDamage);
        fox.getPersistentData().putLong(TAG_FAKE_END, fox.level().getGameTime() + 60L);
        fox.getPersistentData().putBoolean(TAG_FAKE_REAWAKE, true);
        fox.getPersistentData().putBoolean(TAG_FAKE_USED, true);
        if (fox.level() instanceof ServerLevel level) {
            playSound(level, fox, SoundEvents.FOX_DEATH, 1.0F, 1.0F);
        }
    }

    private static void pullPlayerDown(ServerLevel level, Squid squid, ServerPlayer player) {
        BlockPos target = findWaterDestinationBelow(level, player.blockPosition(), 10);
        if (target == null) {
            return;
        }
        player.teleportTo(level, target.getX() + 0.5D, target.getY() + 0.1D, target.getZ() + 0.5D, player.getYRot(), player.getXRot());
        squid.teleportTo(squid.getX(), Math.max(level.getMinBuildHeight() + 1, target.getY()), squid.getZ());
    }

    private static BlockPos findWaterDestinationBelow(ServerLevel level, BlockPos start, int maxDepth) {
        for (int depth = 1; depth <= maxDepth; depth++) {
            BlockPos candidate = start.below(depth);
            if (!level.isInWorldBounds(candidate)) {
                break;
            }
            if (level.getFluidState(candidate).is(FluidTags.WATER) && !level.getBlockState(candidate).isSolid()) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static void swapPositions(ServerLevel level, Entity first, Entity second) {
        Vec3 firstPos = first.position();
        Vec3 secondPos = second.position();
        first.teleportTo(secondPos.x, secondPos.y, secondPos.z);
        second.teleportTo(firstPos.x, firstPos.y, firstPos.z);
        playSound(level, second, SoundEvents.GLASS_BREAK, 1.1F, 0.95F);
    }

    private static void spawnDrownedBehind(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 behind = behindPlayer(player, 2.0D);
        Mob drowned = UncannyEntityRegistry.UNCANNY_DROWNED.get().create(level);
        if (drowned == null) {
            return;
        }
        drowned.moveTo(behind.x, player.getY(), behind.z, player.getYRot() + 180.0F, 0.0F);
        drowned.setSilent(true);
        level.addFreshEntity(drowned);
    }

    private static Llama resolveLlamaDamager(Entity attacker, Entity direct) {
        if (attacker instanceof Llama llama) {
            return llama;
        }
        if (direct instanceof LlamaSpit spit && spit.getOwner() instanceof Llama llamaOwner) {
            return llamaOwner;
        }
        return null;
    }

    private static void assignLlamaToBlackTeam(ServerLevel level, Llama llama) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_LLAMA_BLACK);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_LLAMA_BLACK);
        }
        String key = llama.getStringUUID();
        if (scoreboard.getPlayersTeam(key) != team) {
            scoreboard.addPlayerToTeam(key, team);
        }
    }

    private static void removeLlamaFromBlackTeam(ServerLevel level, Llama llama) {
        Scoreboard scoreboard = level.getScoreboard();
        String key = llama.getStringUUID();
        PlayerTeam team = scoreboard.getPlayersTeam(key);
        if (team != null && TEAM_LLAMA_BLACK.equals(team.getName())) {
            scoreboard.removePlayerFromTeam(key, team);
        }
    }

    private static void ensureVillagerMacabreOffer(Villager villager) {
        villager.getOffers().clear();
        ItemStack result = new ItemStack(Items.ROTTEN_FLESH, 1);
        result.set(DataComponents.CUSTOM_NAME, Component.literal("Help me"));
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.DIRT, 1), Optional.empty(), result, 0, 9999, 1, 0.0F);
        villager.getOffers().add(offer);
        villager.getPersistentData().putBoolean(TAG_VILLAGER_OFFER_INIT, true);
    }

    private static void ensureTraderMessengerOffer(WanderingTrader trader) {
        if (trader.getPersistentData().getBoolean(TAG_TRADER_OFFER_INIT)) {
            return;
        }
        trader.getOffers().clear();
        ItemStack result = new ItemStack(UncannyItemRegistry.UNCANNY_REALITY_SHARD_PIECE.get(), 1);
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.DIRT, 1), Optional.empty(), result, 0, 9999, 1, 0.0F);
        trader.getOffers().add(offer);
        trader.getPersistentData().putBoolean(TAG_TRADER_OFFER_INIT, true);
    }

    private static void triggerCatSenseThief(ServerLevel level, Cat cat, Player player, long now) {
        long cooldownEnd = cat.getPersistentData().getLong(TAG_CAT_SENSE_THIEF_COOLDOWN);
        if (cooldownEnd > now) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
        if (player instanceof ServerPlayer serverPlayer) {
            UncannyParanoiaEventSystem.applyTemporaryDeafness(serverPlayer, 40);
        }

        cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false, true));
        Vec3 away = cat.position().subtract(player.position());
        if (away.lengthSqr() < 0.0001D) {
            away = player.getLookAngle().scale(-1.0D);
        }
        away = away.normalize().scale(0.55D);
        cat.setDeltaMovement(away.x, 0.2D, away.z);
        cat.getNavigation().stop();
        cat.getPersistentData().putLong(TAG_CAT_SENSE_THIEF_COOLDOWN, now + 70L);
    }

    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    private static BlockPos findNearbyBlock(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> predicate) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (predicate.test(level.getBlockState(pos))) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findNearbyWaterPos(ServerLevel level, BlockPos center, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getBlockState(pos).isSolid()) {
                            return pos.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findNearbyEdibleSurface(ServerLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.offset(dx, -1, dz);
                if (isEdibleSurface(level, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static boolean isEdibleSurface(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.FARMLAND)
                || above.is(Blocks.SHORT_GRASS)
                || above.is(Blocks.TALL_GRASS)
                || above.is(Blocks.FERN)
                || above.is(Blocks.LARGE_FERN);
    }

    private static void forceLookAtTwisted(Mob mob, LivingEntity target) {
        Vec3 d = target.getEyePosition().subtract(mob.getEyePosition());
        float yawToTarget = (float) (Mth.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        float bodyYaw = yawToTarget + 180.0F;
        float oscillation = (float) Math.sin((mob.tickCount + mob.getId()) * 0.55F) * 36.0F;
        float headYaw = yawToTarget - 180.0F + oscillation;
        float pitch = (float) Math.sin((mob.tickCount + mob.getId()) * 0.72F) * 32.0F;

        mob.setYRot(bodyYaw);
        mob.setYBodyRot(bodyYaw);
        mob.yBodyRotO = bodyYaw;
        mob.setYHeadRot(headYaw);
        mob.yHeadRotO = headYaw;
        mob.setXRot(pitch);
        mob.xRotO = pitch;
    }

    private static void playCustomSound(ServerLevel level, Entity source, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, source.getX(), source.getY(), source.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static boolean shouldPlay(long now, LivingEntity entity, int minDelay, int maxDelay) {
        long next = entity.getPersistentData().getLong(TAG_TIMER);
        if (now < next) {
            return false;
        }
        int delay = minDelay + entity.getRandom().nextInt(Math.max(1, maxDelay - minDelay + 1));
        entity.getPersistentData().putLong(TAG_TIMER, now + delay);
        return true;
    }

    private static boolean isDirectlyWatchedBy(Player observer, Entity observed, double dotThreshold) {
        if (!observer.hasLineOfSight(observed)) {
            return false;
        }
        Vec3 look = observer.getViewVector(1.0F).normalize();
        Vec3 toEntity = observed.getEyePosition().subtract(observer.getEyePosition()).normalize();
        return look.dot(toEntity) > dotThreshold;
    }

    private static boolean isBroadlySeenBy(Player observer, Entity observed) {
        if (!observer.hasLineOfSight(observed)) {
            return false;
        }
        Vec3 look = observer.getViewVector(1.0F).normalize();
        Vec3 toEntity = observed.getEyePosition().subtract(observer.getEyePosition());
        if (toEntity.lengthSqr() < 0.0001D) {
            return true;
        }
        return look.dot(toEntity.normalize()) > 0.0D;
    }

    private static void playSound(ServerLevel level, Entity entity, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void playAtPos(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void spawnGhostSpit(ServerLevel level, Llama llama, Player target) {
        if (!target.isAlive()) {
            return;
        }
        target.hurt(level.damageSources().mobAttack(llama), 1.0F);
        Vec3 push = target.position().subtract(llama.position());
        if (push.lengthSqr() > 0.0001D) {
            Vec3 knockback = push.normalize().scale(0.12D);
            target.push(knockback.x, 0.04D, knockback.z);
        }
    }

    private static boolean applyPassiveVariantTag(Mob mob, int variant, long nowTick) {
        if (!isSupportedPassive(mob.getType())) {
            return false;
        }

        CompoundTag tag = mob.getPersistentData();
        int clampedVariant = Mth.clamp(variant, 1, 5);
        tag.putBoolean(TAG_ENABLED, true);
        tag.putString(TAG_TYPE, passiveTypeKey(mob.getType()));
        tag.putInt(TAG_VARIANT, clampedVariant);
        long nextTimer = nowTick + 40L + mob.getRandom().nextInt(120);
        if (mob.getType() == EntityType.CHICKEN && clampedVariant == 3) {
            nextTimer = nowTick + 20L + mob.getRandom().nextInt(20);
        }
        if (mob.getType() == EntityType.PIG && clampedVariant == 4) {
            nextTimer = nowTick + 10L;
        }
        tag.putLong(TAG_TIMER, nextTimer);
        tag.putLong(TAG_LAST_EAT, Long.MIN_VALUE);
        tag.putBoolean(TAG_FAKE_USED, false);
        tag.putBoolean(TAG_PIG_CARCASS, false);
        tag.putLong(TAG_ALARM_END, Long.MIN_VALUE);
        tag.putLong(TAG_PARROT_ALARM_END, Long.MIN_VALUE);
        tag.putInt(TAG_FISH_GAZE, 0);
        tag.putBoolean(TAG_VILLAGER_OFFER_INIT, false);
        tag.putBoolean(TAG_TRADER_OFFER_INIT, false);
        tag.putInt(TAG_CAT_REVIVES, 0);
        mob.getTags().removeIf(existing -> existing.startsWith("eotv_passive_"));
        mob.addTag(passiveRenderTag(getTypeKey(mob), clampedVariant));
        return true;
    }

    private static double drownedSpawnChanceForDanger(int danger) {
        return switch (danger) {
            case 0 -> 0.04D;
            case 1 -> 0.08D;
            case 2 -> 0.12D;
            case 3 -> 0.18D;
            case 4 -> 0.26D;
            default -> 0.35D;
        };
    }

    private static String passiveRenderTag(String type, int variant) {
        return "eotv_passive_" + type + "_v" + variant;
    }

    private static EntityType<? extends Mob> resolvePassiveEntityType(String typeKey) {
        return switch (typeKey.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "chicken" -> EntityType.CHICKEN;
            case "pig" -> EntityType.PIG;
            case "cow" -> EntityType.COW;
            case "sheep" -> EntityType.SHEEP;
            case "wolf" -> EntityType.WOLF;
            case "cat" -> EntityType.CAT;
            case "fox" -> EntityType.FOX;
            case "squid" -> EntityType.SQUID;
            case "cod", "fish_cod" -> EntityType.COD;
            case "salmon", "fish_salmon" -> EntityType.SALMON;
            case "parrot" -> EntityType.PARROT;
            case "llama" -> EntityType.LLAMA;
            case "villager" -> EntityType.VILLAGER;
            case "wandering_trader", "trader" -> EntityType.WANDERING_TRADER;
            default -> null;
        };
    }

    private static boolean isUncannyPassive(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        CompoundTag tag = mob.getPersistentData();
        return tag.getBoolean(TAG_ENABLED) && tag.contains(TAG_TYPE) && tag.contains(TAG_VARIANT);
    }

    private static String getTypeKey(Mob mob) {
        return mob.getPersistentData().getString(TAG_TYPE);
    }

    private static int getVariant(Mob mob) {
        return Mth.clamp(mob.getPersistentData().getInt(TAG_VARIANT), 1, 5);
    }

    private static boolean isVariant(Mob mob, String expectedType, int expectedVariant) {
        return isUncannyPassive(mob) && expectedType.equals(getTypeKey(mob)) && getVariant(mob) == expectedVariant;
    }

    private static boolean isSupportedPassive(EntityType<?> type) {
        return type == EntityType.CHICKEN
                || type == EntityType.PIG
                || type == EntityType.COW
                || type == EntityType.SHEEP
                || type == EntityType.WOLF
                || type == EntityType.CAT
                || type == EntityType.FOX
                || type == EntityType.SQUID
                || type == EntityType.COD
                || type == EntityType.SALMON
                || type == EntityType.PARROT
                || type == EntityType.LLAMA
                || type == EntityType.VILLAGER
                || type == EntityType.WANDERING_TRADER;
    }

    private static String passiveTypeKey(EntityType<?> type) {
        if (type == EntityType.WANDERING_TRADER) {
            return "wandering_trader";
        }
        if (type == EntityType.COD) {
            return "cod";
        }
        if (type == EntityType.SALMON) {
            return "salmon";
        }
        return type.builtInRegistryHolder().key().location().getPath();
    }

    private static int rollVariantForPhase(UncannyPhase phase, int roll) {
        return switch (phase) {
            case PHASE_1 -> 1;
            case PHASE_2 -> roll < 68 ? 2 : 1;
            case PHASE_3 -> roll < 58 ? 3 : (roll < 86 ? 2 : 1);
            case PHASE_4 -> roll < 32 ? 5 : (roll < 64 ? 4 : (roll < 86 ? 3 : 2));
        };
    }

    private static boolean isReplacementEligibleSpawnType(MobSpawnType spawnType) {
        return switch (spawnType) {
            case SPAWNER, DISPENSER, TRIAL_SPAWNER, BUCKET, BREEDING, MOB_SUMMONED, TRIGGERED -> false;
            default -> true;
        };
    }

    private static final class FalseEggState {
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final UUID owner;
        private final long explodeTick;

        private FalseEggState(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos, UUID owner, long explodeTick) {
            this.dimension = dimension;
            this.pos = pos;
            this.owner = owner;
            this.explodeTick = explodeTick;
        }
    }
}
