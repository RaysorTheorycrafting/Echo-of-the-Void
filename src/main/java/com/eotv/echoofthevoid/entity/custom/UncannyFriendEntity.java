package com.eotv.echoofthevoid.entity.custom;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import com.eotv.echoofthevoid.event.special.OldFriendLoadout;
import com.eotv.echoofthevoid.event.special.OldFriendRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The old friend, once it finally shows up: a player body with the friend's name and skin, fighting
 * with copies of its target's own sword, shield and armour, moving like a player (sprinting, jump
 * crits, raising the shield between swings), pillaring up and digging down or through with tools of
 * the sword's material. Every block it places or breaks is remembered and put back when it is gone.
 */
public class UncannyFriendEntity extends Monster implements UncannyEntityMarker {
    private static final EntityDataAccessor<Optional<UUID>> PROFILE_ID =
            SynchedEntityData.defineId(UncannyFriendEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final double REACH = 3.0D;
    private static final int PILLAR_LIMIT = 24;
    /** Its blows are a player's: never multiplied by the difficulty, and "slain by <name>" like PvP. */
    public static final ResourceKey<DamageType> ATTACK = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("echoofthevoid", "old_friend_attack"));
    /** A player's movement attribute; sprinting adds Vanilla's +30 % modifier on top. */
    public static final double PLAYER_MOVEMENT_SPEED = 0.1D;
    private static final ItemStack PILLAR_BLOCKS = new ItemStack(Items.COBBLESTONE, 64);
    private static final int LAVA_REACH = 2;

    /** Something other than its weapon in hand (blocks, a bucket) for this many more ticks. */
    private int handOverrideTicks;
    private int lavaCooldown;

    private UUID targetId;
    private ItemStack weapon = ItemStack.EMPTY;
    private int attackCooldown;
    private int shieldCooldown;
    private int stuckTicks;
    private Vec3 lastProgressPosition = Vec3.ZERO;
    private BlockPos miningPos;
    private float miningProgress;
    private int lastCrackStage = -1;
    private BlockPos pillarBase;
    private int pillarBlocks;
    /** Ticks pillaring without gaining a block; it gives the spot up past a limit. */
    private int pillarStallTicks;
    private int pillarCooldown;
    /** A player's hunger: it eats to regenerate, as its target would (user, 2026-10-09). */
    private final OldFriendRules.Hunger hunger = new OldFriendRules.Hunger();
    private ItemStack meal = new ItemStack(Items.COOKED_BEEF);
    private Vec3 lastHungerPosition;
    private final List<BlockEdit> edits = new ArrayList<>();

    private record BlockEdit(BlockPos pos, BlockState original, boolean placed) {
    }

    public UncannyFriendEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
            navigation.setCanFloat(true);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    @Override
    protected void registerGoals() {
        // No FloatGoal: a mob that always bobs up is helpless against anyone below it. It swims and
        // dives like a player instead (see tickSwim), surfacing only for air or to follow its target.
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROFILE_ID, Optional.empty());
    }

    public Optional<UUID> profileId() {
        return this.entityData.get(PROFILE_ID);
    }

    public Optional<UUID> targetId() {
        return Optional.ofNullable(this.targetId);
    }

    /** Takes the friend's identity and copies (never moves) the target's own gear. */
    public void setup(UUID friendId, String friendName, ServerPlayer target) {
        this.entityData.set(PROFILE_ID, Optional.of(friendId));
        this.setCustomName(net.minecraft.network.chat.Component.literal(friendName));
        this.setCustomNameVisible(true);
        this.targetId = target.getUUID();
        OldFriendLoadout.Loadout loadout = OldFriendLoadout.of(target);
        this.weapon = loadout.weapon().copy();
        this.setItemSlot(EquipmentSlot.MAINHAND, this.weapon.copy());
        this.setItemSlot(EquipmentSlot.OFFHAND, loadout.shield().copy());
        this.setItemSlot(EquipmentSlot.HEAD, loadout.head().copy());
        this.setItemSlot(EquipmentSlot.CHEST, loadout.chest().copy());
        this.setItemSlot(EquipmentSlot.LEGS, loadout.legs().copy());
        this.setItemSlot(EquipmentSlot.FEET, loadout.feet().copy());
        this.meal = OldFriendLoadout.mealOf(target);
        this.setHealth(this.getMaxHealth());
    }

    public OldFriendRules.Hunger hunger() {
        return this.hunger;
    }

    public boolean isEating() {
        return this.isUsingItem() && this.getUsedItemHand() == InteractionHand.MAIN_HAND;
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level) || this.isDeadOrDying()) {
            return;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.shieldCooldown > 0) {
            this.shieldCooldown--;
        }
        if (this.lavaCooldown > 0) {
            this.lavaCooldown--;
        }
        if (this.pillarCooldown > 0) {
            this.pillarCooldown--;
        }
        if (this.handOverrideTicks > 0 && --this.handOverrideTicks == 0 && this.miningPos == null && !isEating()) {
            holdWeapon();
        }
        tickHunger();
        updateSwimmingPose();
        ServerPlayer target = this.targetId == null ? null : level.getServer().getPlayerList().getPlayer(this.targetId);
        if (target == null || !target.isAlive() || target.level() != level || target.isSpectator()) {
            this.getNavigation().stop();
            stopBlocking();
            return;
        }
        this.setTarget(target);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (tickLava(level)) {
            return;
        }
        if (this.miningPos != null) {
            tickMining(level, target);
            return;
        }
        if (this.isInWater()) {
            if (isEating()) {
                this.stopUsingItem();
            }
            holdWeapon();
            tickSwim(target);
            tickShield(target);
            tickAttack(target);
            return;
        }
        double dy = target.getY() - this.getY();
        double horizontal = Math.hypot(target.getX() - this.getX(), target.getZ() - this.getZ());
        boolean inReach = this.distanceToSqr(target) <= REACH * REACH && this.hasLineOfSight(target);
        if (dy >= 2.5D && horizontal <= 2.5D && this.pillarBlocks < PILLAR_LIMIT && !inReach
                && this.pillarCooldown <= 0 && !isEating()) {
            tickPillar(level);
            return;
        }
        this.pillarStallTicks = 0;
        if (tickMeal(target)) {
            // Eating: it keeps walking (slowly, as a player does) but neither strikes nor blocks.
            tickChase(level, target, horizontal);
            return;
        }
        holdWeapon();
        if (dy <= -2.5D && horizontal <= 2.0D) {
            startMining(level, this.blockPosition().below(), target);
            return;
        }
        tickChase(level, target, horizontal);
        if (this.miningPos != null) {
            // It just took out a tool to break through: a player does not swing an axe at a raised
            // shield by accident, and an axe blow would disable it for five seconds.
            return;
        }
        tickShield(target);
        tickAttack(target);
    }

    // ------------------------------------------------------------------ hunger, like a player

    /** Vanilla's natural regeneration from food and saturation, paid for in exhaustion. */
    private void tickHunger() {
        Vec3 now = this.position();
        if (this.lastHungerPosition != null && this.isSprinting()) {
            // Player.checkMovementStatistics: sprinting costs 0.1 exhaustion per metre.
            this.hunger.addExhaustion(0.1F * (float) now.distanceTo(this.lastHungerPosition));
        }
        this.lastHungerPosition = now;
        float heal = this.hunger.tick(this.getMaxHealth() - this.getHealth());
        if (heal > 0.0F) {
            this.heal(heal);
        }
    }

    /**
     * Starts or continues a meal: hurt and hungry, with room to eat, it takes the food out and eats
     * it (the Vanilla item-use path: particles, sounds, slower walk). A target closing in before the
     * meal is half done makes it drop the food and draw its sword again.
     * @return true while eating
     */
    private boolean tickMeal(ServerPlayer target) {
        double distance = this.distanceTo(target);
        if (isEating()) {
            if (distance <= REACH && this.getTicksUsingItem() < OldFriendRules.EAT_TICKS / 2) {
                this.stopUsingItem();
                this.handOverrideTicks = 0;
                holdWeapon();
                return false;
            }
            return true;
        }
        if (!this.onGround() || distance < OldFriendRules.MIN_EAT_DISTANCE
                || !this.hunger.wantsToEat(this.getMaxHealth() - this.getHealth())) {
            return false;
        }
        stopBlocking();
        showInHand(this.meal.copyWithCount(1), OldFriendRules.EAT_TICKS + 6);
        this.startUsingItem(InteractionHand.MAIN_HAND);
        return isEating();
    }

    /** Eating feeds its hunger bar exactly as it would feed a player's. */
    @Override
    public ItemStack eat(Level level, ItemStack food, net.minecraft.world.food.FoodProperties properties) {
        if (!level.isClientSide()) {
            this.hunger.eat(properties.nutrition(), properties.saturation());
        }
        return super.eat(level, food, properties);
    }

    // ------------------------------------------------------------------ moving like a player

    /** Mobs move by speed squared (speed doubles as forward input); a player's input is a full step. */
    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);
        // Holding a shield up slows a player to a fifth, as using any item does.
        this.setZza(speed > 0.0F ? (this.isUsingItem() ? 0.2F : 1.0F) : 0.0F);
    }

    @Override
    protected float getFlyingSpeed() {
        return this.isSprinting() ? 0.025999999F : 0.02F;
    }

    /** A swimming player follows its view up or down; copied from {@code Player.travel}. */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isSwimming() && !this.isPassenger()) {
            double lookY = this.getLookAngle().y;
            double rate = lookY < -0.2D ? 0.085D : 0.06D;
            if (lookY <= 0.0D || this.jumping || !this.level().getBlockState(
                    BlockPos.containing(this.getX(), this.getY() + 1.0D - 0.1D, this.getZ())).getFluidState().isEmpty()) {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.add(0.0D, (lookY - motion.y) * rate, 0.0D));
            }
        }
        super.travel(travelVector);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return pose == Pose.SWIMMING ? SWIMMING_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    private static final EntityDimensions SWIMMING_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F);

    /** Lies flat while sprint-swimming, stands up again only where there is room, as a player does. */
    private void updateSwimmingPose() {
        Pose wanted = this.isSwimming() ? Pose.SWIMMING : Pose.STANDING;
        if (this.getPose() == wanted || this.getPose() == Pose.DYING) {
            return;
        }
        if (wanted == Pose.STANDING && !this.level().noCollision(this,
                super.getDefaultDimensions(Pose.STANDING).makeBoundingBox(this.position()).deflate(1.0E-7D))) {
            return;
        }
        this.setPose(wanted);
    }

    /**
     * In water: sprint-swim (only possible submerged, as for a player) straight at the target, dive
     * after it, come up for air. A target that surfaces to breathe while it still has air is struck
     * from just below instead of followed up (user, 2026-10-09).
     */
    private void tickSwim(ServerPlayer target) {
        this.getNavigation().stop();
        boolean submerged = this.isUnderWater();
        // At the surface a player cannot sprint: swimming there is slow, for it too.
        this.setSprinting(submerged && !this.isUsingItem());
        double dy = target.getY() - this.getY();
        boolean targetBreathing = target.isInWater() && !target.isUnderWater();
        boolean hasAir = this.getAirSupply() >= OldFriendRules.MIN_AIR_TO_STAY_UNDER;
        if (targetBreathing && hasAir) {
            // Hold just under the target and strike up at it.
            double holdY = target.getY() - OldFriendRules.HOLD_DEPTH_UNDER_TARGET;
            this.getMoveControl().setWantedPosition(target.getX(), holdY, target.getZ(), 1.0D);
            this.getLookControl().setLookAt(target.getX(), holdY + 0.4D, target.getZ());
            if (this.getY() < holdY - 0.5D) {
                this.getJumpControl().jump();
            } else if (!submerged) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            }
            if (this.distanceToSqr(target) <= REACH * REACH) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            return;
        }
        this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
        if (this.getAirSupply() < 60) {
            this.getLookControl().setLookAt(this.getX(), this.getEyeY() + 4.0D, this.getZ());
            this.getJumpControl().jump();
        } else if (dy > 0.6D) {
            this.getJumpControl().jump();
        } else if (dy < -0.6D && !this.isSwimming()) {
            // Not yet under the surface: sink as a player holding sneak does, then swim down.
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
    }

    /**
     * Lava poured on it, or lava it stands in: scoop the source with an empty bucket, as a player
     * would. Natural lava it merely walks past is left alone.
     */
    private boolean tickLava(ServerLevel level) {
        if (this.lavaCooldown > 0 || !(this.isInLava() || this.isOnFire() || touchesLava(level))) {
            return false;
        }
        BlockPos source = nearestLavaSource(level);
        if (source == null) {
            return false;
        }
        stopBlocking();
        showInHand(new ItemStack(Items.BUCKET), 2);
        this.getLookControl().setLookAt(Vec3.atCenterOf(source));
        this.swing(InteractionHand.MAIN_HAND);
        level.setBlock(source, Blocks.AIR.defaultBlockState(), 11);
        level.playSound(null, source, SoundEvents.BUCKET_FILL_LAVA, SoundSource.HOSTILE, 1.0F, 1.0F);
        showInHand(new ItemStack(Items.LAVA_BUCKET), 15);
        this.lavaCooldown = 6;
        return true;
    }

    private boolean touchesLava(ServerLevel level) {
        return level.getBlockStates(this.getBoundingBox().inflate(0.6D)).anyMatch(state -> state.getFluidState().is(FluidTags.LAVA));
    }

    private BlockPos nearestLavaSource(ServerLevel level) {
        BlockPos feet = this.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-LAVA_REACH, -1, -LAVA_REACH), feet.offset(LAVA_REACH, 3, LAVA_REACH))) {
            if (level.getFluidState(pos).is(FluidTags.LAVA) && level.getFluidState(pos).isSource()
                    && level.getBlockState(pos).getBlock() instanceof LiquidBlock) {
                double distance = Vec3.atCenterOf(pos).distanceToSqr(this.getEyePosition());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private void showInHand(ItemStack stack, int ticks) {
        this.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        this.handOverrideTicks = ticks;
    }

    /** Back to its sword (or bare hand) whenever it is not placing, scooping or digging. */
    private void holdWeapon() {
        if (this.handOverrideTicks > 0 || this.miningPos != null) {
            return;
        }
        if (!ItemStack.isSameItemSameComponents(this.getMainHandItem(), this.weapon)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, this.weapon.copy());
        }
    }

    private void tickChase(ServerLevel level, ServerPlayer target, double horizontal) {
        if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
            // Speed comes from the player attribute; sprinting adds Vanilla's own modifier.
            this.getNavigation().moveTo(target, 1.0D);
        }
        this.setSprinting(horizontal > 3.5D && !this.isUsingItem());
        // A player sprint-jumps across open ground.
        if (this.isSprinting() && this.onGround() && horizontal > 6.0D && this.random.nextInt(14) == 0) {
            this.getJumpControl().jump();
        }
        if (this.tickCount % 20 == 0) {
            double moved = this.position().distanceTo(this.lastProgressPosition);
            this.lastProgressPosition = this.position();
            // Not getting closer, or right against a wall the target hid behind (blocks placed to keep
            // it out): either way it digs through toward the target.
            boolean walledOff = !this.hasLineOfSight(target);
            this.stuckTicks = moved < 0.6D && (horizontal > 2.0D || walledOff) ? this.stuckTicks + 20 : 0;
        }
        if (this.stuckTicks >= 40) {
            this.stuckTicks = 0;
            breakThrough(level, target);
        }
    }

    /** Stuck behind something: dig the head-level then the foot-level block toward the target. */
    private void breakThrough(ServerLevel level, ServerPlayer target) {
        Direction toward = Direction.getNearest(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        BlockPos feet = this.blockPosition();
        BlockPos front = feet.relative(toward);
        for (BlockPos candidate : List.of(front.above(), front)) {
            if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) {
                startMining(level, candidate, target);
                return;
            }
        }
        if (target.getY() > this.getY() + 0.5D) {
            tickPillar(level);
        }
    }

    private void tickPillar(ServerLevel level) {
        this.getNavigation().stop();
        this.setSprinting(false);
        stopBlocking();
        if (++this.pillarStallTicks > OldFriendRules.PILLAR_STALL_TICKS) {
            // No block gained for a while (a ceiling it may not break, a ledge it keeps sliding off):
            // give the spot up and move around instead of freezing in place (user, 2026-10-09).
            abandonPillar();
            return;
        }
        // Blocks in hand and eyes on the block under its feet, as a player pillaring up.
        if (!this.getMainHandItem().is(PILLAR_BLOCKS.getItem())) {
            this.setItemSlot(EquipmentSlot.MAINHAND, PILLAR_BLOCKS.copy());
        }
        this.getLookControl().setLookAt(this.getX(), this.getY() - 1.5D, this.getZ(), 90.0F, 90.0F);
        Vec3 motion = this.getDeltaMovement();
        if (this.onGround()) {
            // The column it stands on (the block that really carries it, not the one its feet hang
            // over at an edge): it steps to the very middle of it first, as a player lines up before
            // pillaring, otherwise its hitbox straddles two columns and the jump catches on the side
            // (user, 2026-10-09: "il ne se tenait pas au milieu du block").
            BlockPos column = this.getOnPos().above();
            double dx = column.getX() + 0.5D - this.getX();
            double dz = column.getZ() + 0.5D - this.getZ();
            if (!OldFriendRules.isCentered(dx, dz)) {
                this.setDeltaMovement(OldFriendRules.centeringStep(dx), motion.y, OldFriendRules.centeringStep(dz));
                return;
            }
            this.setDeltaMovement(0.0D, motion.y, 0.0D);
            for (BlockPos ceiling : List.of(column.above(), column.above(2))) {
                if (!level.getBlockState(ceiling).getCollisionShape(level, ceiling).isEmpty()) {
                    if (!startMining(level, ceiling, null)) {
                        abandonPillar();
                    }
                    return;
                }
            }
            this.pillarBase = column;
            this.getJumpControl().jump();
            return;
        }
        // In the air a pillaring player stands still: any sideways drift would set the block beside it.
        this.setDeltaMovement(0.0D, motion.y, 0.0D);
        if (this.pillarBase != null && this.getY() >= this.pillarBase.getY() + 1.05D) {
            BlockPos feet = BlockPos.containing(this.getX(), this.pillarBase.getY(), this.getZ());
            if (feet.equals(this.pillarBase)
                    && level.getBlockState(this.pillarBase).canBeReplaced()
                    && level.getFluidState(this.pillarBase).isEmpty()) {
                place(level, this.pillarBase, Blocks.COBBLESTONE.defaultBlockState());
                this.pillarBlocks++;
                this.pillarStallTicks = 0;
            }
            this.pillarBase = null;
        }
    }

    private void abandonPillar() {
        this.pillarStallTicks = 0;
        this.pillarBase = null;
        this.pillarCooldown = OldFriendRules.PILLAR_RETRY_COOLDOWN_TICKS;
        holdWeapon();
        // Step off to the side to look for another way up or around.
        Vec3 aside = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(this, 5, 2);
        if (aside != null) {
            this.getNavigation().moveTo(aside.x, aside.y, aside.z, 1.0D);
        }
    }

    /**
     * What it may break on its way to its target: whatever a player could dig through, so walls
     * and roofs thrown up to keep it out do not stop it (user, 2026-10-09). It takes a player's time
     * and needs the right tool for hard blocks. Every broken block is put back when it is gone.
     * Never: unbreakable blocks, anything holding items or data (chests, furnaces, signs...), the
     * mod's own blocks, liquids, or a block carrying sand or gravel that would fall and be lost.
     */
    private static boolean canBreak(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && !state.hasBlockEntity()
                && level.getBlockEntity(pos) == null
                && !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("echoofthevoid")
                && !(above.getBlock() instanceof net.minecraft.world.level.block.FallingBlock);
    }

    private boolean startMining(ServerLevel level, BlockPos pos, ServerPlayer target) {
        if (!canBreak(level, pos)) {
            return false;
        }
        this.miningPos = pos.immutable();
        this.miningProgress = 0.0F;
        this.lastCrackStage = -1;
        this.getNavigation().stop();
        stopBlocking();
        this.setItemSlot(EquipmentSlot.MAINHAND, OldFriendLoadout.toolFor(level.getBlockState(pos), this.weapon));
        return true;
    }

    private void tickMining(ServerLevel level, ServerPlayer target) {
        BlockPos pos = this.miningPos;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || this.position().distanceTo(Vec3.atCenterOf(pos)) > 5.0D
                || !canBreak(level, pos)) {
            finishMining(level);
            return;
        }
        if (target != null && this.distanceToSqr(target) <= REACH * REACH && this.hasLineOfSight(target)) {
            finishMining(level);
            return;
        }
        this.getLookControl().setLookAt(Vec3.atCenterOf(pos));
        ItemStack tool = this.getMainHandItem();
        this.miningProgress += OldFriendRules.miningProgressPerTick(
                tool.getDestroySpeed(state), state.getDestroySpeed(level, pos),
                // Vanilla "can harvest": the right tool, or a block that needs none (leaves, dirt).
                tool.isCorrectToolForDrops(state) || !state.requiresCorrectToolForDrops());
        if (this.tickCount % 5 == 0) {
            this.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        int stage = Math.min(9, (int) (this.miningProgress * 10.0F));
        if (stage != this.lastCrackStage) {
            level.destroyBlockProgress(this.getId(), pos, stage);
            this.lastCrackStage = stage;
        }
        if (this.miningProgress >= 1.0F) {
            this.edits.add(new BlockEdit(pos, state, false));
            level.destroyBlockProgress(this.getId(), pos, -1);
            level.destroyBlock(pos, false, this);
            finishMining(level);
        }
    }

    private void finishMining(ServerLevel level) {
        if (this.miningPos != null) {
            level.destroyBlockProgress(this.getId(), this.miningPos, -1);
        }
        this.miningPos = null;
        this.miningProgress = 0.0F;
        this.setItemSlot(EquipmentSlot.MAINHAND, this.weapon.copy());
    }

    private void place(ServerLevel level, BlockPos pos, BlockState state) {
        this.edits.add(new BlockEdit(pos.immutable(), level.getBlockState(pos), true));
        level.setBlock(pos, state, 3);
        this.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private void tickShield(ServerPlayer target) {
        if (isEating()) {
            return;
        }
        ItemStack offhand = this.getOffhandItem();
        if (!(offhand.getItem() instanceof ShieldItem) || this.shieldCooldown > 0) {
            stopBlocking();
            return;
        }
        double distance = Math.sqrt(this.distanceToSqr(target));
        ItemStack using = target.getUseItem();
        boolean aimedAt = target.isUsingItem() && (using.getItem() instanceof BowItem || using.getItem() instanceof CrossbowItem);
        boolean betweenSwings = distance <= 5.0D && this.attackCooldown > 3;
        if ((aimedAt && distance > 3.0D) || betweenSwings) {
            if (!this.isUsingItem()) {
                this.startUsingItem(InteractionHand.OFF_HAND);
            }
        } else {
            stopBlocking();
        }
    }

    private void stopBlocking() {
        if (this.isUsingItem() && this.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.stopUsingItem();
        }
    }

    private void tickAttack(ServerPlayer target) {
        if (this.attackCooldown > 0 || this.distanceToSqr(target) > REACH * REACH || !this.hasLineOfSight(target)
                || !ItemStack.isSameItemSameComponents(this.getMainHandItem(), this.weapon)) {
            // Only ever strikes with its weapon in hand (never a tool, a bucket or blocks).
            return;
        }
        boolean falling = !this.onGround() && this.getDeltaMovement().y < 0.0D && !this.isInWater();
        if (!falling && this.onGround() && this.random.nextFloat() < 0.35F) {
            // Jump first and strike on the way down, like a player going for a critical hit.
            this.getJumpControl().jump();
            return;
        }
        stopBlocking();
        this.swing(InteractionHand.MAIN_HAND);
        this.doHurtTarget(target);
        this.attackCooldown = OldFriendRules.attackCooldownTicks(this.getAttributeValue(Attributes.ATTACK_SPEED));
    }

    /**
     * A player's blow (after {@code Player.attack}): weapon damage, ×1.5 on a falling critical with
     * enchantment bonus added after, +1 knockback when sprinting (and the sprint ends), never scaled
     * by difficulty. The mob path multiplied every hit by 1.5 on Hard.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        boolean critical = this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater()
                && !this.isPassenger() && !this.isSprinting();
        boolean sprintHit = this.isSprinting();
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ATTACK), this);
        float base = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float enchantBonus = EnchantmentHelper.modifyDamage(level, this.getWeaponItem(), target, source, base) - base;
        float damage = base * (critical ? 1.5F : 1.0F) + enchantBonus;
        boolean hurt = target.hurt(source, damage);
        // Player.attack: every swing that connects costs 0.1 exhaustion.
        this.hunger.addExhaustion(0.1F);
        if (!hurt) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE, 1.0F, 1.0F);
            return false;
        }
        float knockback = this.getKnockback(target, source) + (sprintHit ? 1.0F : 0.0F);
        if (knockback > 0.0F && target instanceof LivingEntity living) {
            living.knockback(knockback * 0.5F, Mth.sin(this.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(this.getYRot() * Mth.DEG_TO_RAD));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            this.setSprinting(false);
        }
        EnchantmentHelper.doPostAttackEffects(level, target, source);
        this.setLastHurtMob(target);
        if (critical) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.2D);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                critical ? SoundEvents.PLAYER_ATTACK_CRIT : sprintHit ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasBlocking = this.isBlocking();
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.hunger.addExhaustion(source.getFoodExhaustion());
        }
        if (wasBlocking && source.getEntity() instanceof LivingEntity attacker
                && attacker.getMainHandItem().getItem() instanceof AxeItem) {
            // An axe knocks a shield out of use, exactly as it would for a player.
            this.stopUsingItem();
            this.shieldCooldown = 100;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
        }
        return hurt;
    }

    // ------------------------------------------------------------------ world restoration

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel level) {
            restoreEdits(level);
        }
        super.remove(reason);
    }

    /** Placed blocks go, broken blocks come back, unless a player changed the spot since. */
    public void restoreEdits(ServerLevel level) {
        for (int i = this.edits.size() - 1; i >= 0; i--) {
            BlockEdit edit = this.edits.get(i);
            BlockState current = level.getBlockState(edit.pos());
            if (edit.placed() && current.is(Blocks.COBBLESTONE)) {
                level.setBlock(edit.pos(), edit.original(), 3);
            } else if (!edit.placed() && current.isAir()) {
                level.setBlock(edit.pos(), edit.original(), 3);
            }
        }
        this.edits.clear();
        if (this.miningPos != null) {
            level.destroyBlockProgress(this.getId(), this.miningPos, -1);
        }
    }

    public int editCount() {
        return this.edits.size();
    }

    // ------------------------------------------------------------------ vanilla surfaces

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        profileId().ifPresent(id -> tag.putUUID("FriendProfile", id));
        if (this.targetId != null) {
            tag.putUUID("FriendTarget", this.targetId);
        }
        if (!this.weapon.isEmpty()) {
            tag.put("FriendWeapon", this.weapon.save(this.registryAccess()));
        }
        ListTag list = new ListTag();
        for (BlockEdit edit : this.edits) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", edit.pos().asLong());
            entry.put("state", NbtUtils.writeBlockState(edit.original()));
            entry.putBoolean("placed", edit.placed());
            list.add(entry);
        }
        tag.put("FriendEdits", list);
        tag.putInt("FriendFood", this.hunger.food());
        tag.putFloat("FriendSaturation", this.hunger.saturation());
        tag.putFloat("FriendExhaustion", this.hunger.exhaustion());
        if (!this.meal.isEmpty()) {
            tag.put("FriendMeal", this.meal.save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("FriendProfile")) {
            this.entityData.set(PROFILE_ID, Optional.of(tag.getUUID("FriendProfile")));
        }
        this.targetId = tag.hasUUID("FriendTarget") ? tag.getUUID("FriendTarget") : null;
        this.weapon = tag.contains("FriendWeapon")
                ? ItemStack.parseOptional(this.registryAccess(), tag.getCompound("FriendWeapon"))
                : ItemStack.EMPTY;
        this.edits.clear();
        ListTag list = tag.getList("FriendEdits", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            this.edits.add(new BlockEdit(BlockPos.of(entry.getLong("pos")),
                    NbtUtils.readBlockState(this.level().holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                            entry.getCompound("state")),
                    entry.getBoolean("placed")));
        }
        if (tag.contains("FriendFood")) {
            this.hunger.restore(tag.getInt("FriendFood"), tag.getFloat("FriendSaturation"), tag.getFloat("FriendExhaustion"));
        }
        if (tag.contains("FriendMeal")) {
            ItemStack savedMeal = ItemStack.parseOptional(this.registryAccess(), tag.getCompound("FriendMeal"));
            if (!savedMeal.isEmpty()) {
                this.meal = savedMeal;
            }
        }
    }
}
