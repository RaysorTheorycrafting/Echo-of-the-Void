package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.block.UncannyBlockRegistry;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyFollowerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyHurlerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyKeeperEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyKnockerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyPhantomEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyPulseEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyShadowEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyTenantEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyUsherEntity;
import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import com.eotv.echoofthevoid.network.UncannyFalseRecipeToastPayload;
import com.eotv.echoofthevoid.network.UncannyHotbarWrongCountPayload;
import com.eotv.echoofthevoid.network.UncannyPetRefusalVisualPayload;
import com.eotv.echoofthevoid.phase.UncannyPhase;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.CRC32;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UncannyParanoiaEventSystem {
    private static final int AUTO_CHECK_INTERVAL_TICKS = 20;
    private static final int MIN_AUTO_CHECK_INTERVAL_TICKS = 10;
    private static final int MAX_AUTO_CHECK_INTERVAL_TICKS = 34;
    private static final double[] PROFILE_TRIGGER_MULTIPLIER = {0.34D, 0.58D, 0.92D, 1.48D, 2.20D};
    private static final int[] PROFILE_BASE_COOLDOWN_SECONDS = {210, 150, 105, 72, 48};
    private static final double[] PROFILE_AMBIENT_TRIGGER_MULTIPLIER = {0.55D, 0.72D, 0.92D, 1.20D, 1.55D};
    private static final int[] PROFILE_AMBIENT_BASE_COOLDOWN_SECONDS = {190, 145, 105, 78, 58};
    private static final double[] PROFILE_BELL_WAVE_CHANCE = {0.70D, 0.78D, 0.86D, 0.92D, 0.96D};
    private static final double[] PROFILE_BLACKOUT_SPECIAL_CHANCE = {0.13D, 0.18D, 0.24D, 0.32D, 0.42D};
    private static final int[] PROFILE_MAX_SILENCE_SECONDS = {360, 240, 150, 80, 55};
    private static final double[] DANGER_TRIGGER_MULTIPLIER = {0.42D, 0.62D, 0.82D, 1.00D, 1.28D, 1.62D};
    private static final double[] DANGER_GLOBAL_COOLDOWN_MULTIPLIER = {1.90D, 1.45D, 1.15D, 1.00D, 0.82D, 0.64D};
    private static final double[] DANGER_AMBIENT_TRIGGER_MULTIPLIER = {0.78D, 0.88D, 0.95D, 1.00D, 1.08D, 1.18D};
    private static final double[] DANGER_AMBIENT_COOLDOWN_MULTIPLIER = {1.25D, 1.12D, 1.05D, 1.00D, 0.92D, 0.84D};
    private static final double[] DANGER_EVENT_COOLDOWN_MULTIPLIER = {2.05D, 1.55D, 1.20D, 1.00D, 0.78D, 0.58D};
    private static final double[] DANGER_MAX_SILENCE_MULTIPLIER = {1.60D, 1.30D, 1.10D, 1.00D, 0.85D, 0.70D};
    private static final double[] DANGER_HIGH_EVENT_MULTIPLIER = {0.00D, 0.20D, 0.50D, 1.00D, 2.00D, 3.40D};
    private static final double[] DANGER_MEDIUM_EVENT_MULTIPLIER = {0.65D, 0.78D, 0.90D, 1.00D, 1.30D, 1.75D};
    private static final double[] DANGER_LIGHT_EVENT_MULTIPLIER = {2.60D, 2.00D, 1.35D, 1.00D, 0.55D, 0.20D};
    private static final double[] DANGER_FLASH_MONSTER_CHANCE = {0.00D, 0.15D, 0.30D, 0.50D, 0.70D, 0.90D};
    private static final int[] DANGER_HURLER_ATTACK_PERCENT = {0, 3, 6, 10, 18, 28};
    private static final int[] DANGER_KNOCKER_OPEN_ATTACK_PERCENT = {0, 8, 14, 20, 30, 40};
    private static final int[] PROFILE_SPECIAL_ENTITY_BASE_COOLDOWN_SECONDS = {720, 520, 360, 250, 175};
    private static final int[] PROFILE_SPECIAL_ENTITY_CHECK_INTERVAL_SECONDS = {12, 10, 8, 6, 5};
    private static final double[] PROFILE_SPECIAL_ENTITY_TRIGGER_CHANCE = {0.05D, 0.09D, 0.15D, 0.24D, 0.34D};
    private static final double[] DANGER_SPECIAL_ENTITY_COOLDOWN_MULTIPLIER = {1.35D, 1.22D, 1.10D, 1.00D, 0.92D, 0.84D};
    private static final double[] DANGER_SPECIAL_ENTITY_TRIGGER_MULTIPLIER = {0.70D, 0.82D, 0.92D, 1.00D, 1.08D, 1.16D};
    private static final String FLASH_RED_OVERLAY_TAG = "eotv_event_flash_red";
    private static final String HUNTER_FOG_TAG = "eotv_event_hunter_fog";
    private static final String GIANT_SUN_TAG = "eotv_event_giant_sun";
    private static final String TEAM_PET_REFUSAL_BLACK = "eotv_pet_refusal_black";
    private static final int FLASH_RED_MARKER_AMPLIFIER = 7;
    private static final String DONT_TURN_AROUND_MESSAGE = "Don't turn around.";
    private static final int DOOR_LOCK_SECONDS = 5;
    private static final double[] SLEEP_DISTURB_PHASE_CHANCE = {0.0D, 0.055D, 0.078D, 0.105D};
    private static final double[] SLEEP_DISTURB_PROFILE_MULTIPLIER = {0.72D, 0.88D, 1.00D, 1.14D, 1.30D};
    private static final int SLEEP_DISTURB_COOLDOWN_MIN_SECONDS = 16 * 60;
    private static final int SLEEP_DISTURB_COOLDOWN_MAX_SECONDS = 28 * 60;
    private static final int SLEEP_DISTURB_REQUIRED_CLICKS = 3;
    private static final int INITIAL_EVENT_JOIN_GRACE_TICKS = 20 * 18;
    private static final int INITIAL_SPECIAL_JOIN_GRACE_TICKS = 20 * 24;
    private static final int TENSION_BUILDER_MIN_SECONDS = 5 * 60;
    private static final int TENSION_BUILDER_MAX_SECONDS = 10 * 60;
    private static final int TENSION_BREAK_MIN_SECONDS = 25 * 60;
    private static final int TENSION_BREAK_MAX_SECONDS = 50 * 60;
    private static final int GRAND_EVENT_BOOST_MIN_SECONDS = 45;
    private static final int GRAND_EVENT_BOOST_MAX_SECONDS = 110;
    private static final int GRAND_EVENT_ROLL_MIN_SECONDS = 10;
    private static final int GRAND_EVENT_ROLL_MAX_SECONDS = 24;
    private static final int GRAND_EVENT_BASE_COOLDOWN_SECONDS = 35 * 60;
    private static final double GRAND_EVENT_BASE_CHANCE = 0.0D;
    private static final double GRAND_EVENT_POST_TENSION_CHANCE = 0.22D;
    private static final String GRAND_WARDEN_TAG = "eotv_grand_warden";
    private static final int GRAND_WARDEN_ZONE_RADIUS = 64;
    private static final int GRAND_WARDEN_SPAWN_MIN_DISTANCE = 28;
    private static final int GRAND_WARDEN_SPAWN_MAX_DISTANCE = 52;
    private static final int GRAND_WARDEN_COVERED_SPAWN_MIN_DISTANCE = 10;
    private static final int GRAND_WARDEN_COVERED_SPAWN_MAX_DISTANCE = 20;
    private static final int GRAND_WARDEN_COVERED_PRIMARY_MAX_Y_DELTA = 2;
    private static final int GRAND_WARDEN_COVERED_FALLBACK_MAX_Y_DELTA = 4;
    private static final int GRAND_EVENT_MESSAGE_INTERVAL_TICKS = 44;
    private static final int GRAND_EVENT_HEAVY_SOUND_MIN_TICKS = 24;
    private static final int GRAND_EVENT_HEAVY_SOUND_MAX_TICKS = 54;
    private static final double GRAND_EVENT_MOVEMENT_MIN_STEP_BLOCKS = 0.03D;
    private static final double GRAND_EVENT_MOVEMENT_TRIGGER_BLOCKS = 0.65D;
    private static final double GRAND_EVENT_MOVEMENT_IDLE_DECAY_BLOCKS = 0.03D;
    private static final double GRAND_EVENT_MOVEMENT_IGNORED_DECAY_BLOCKS = 0.12D;
    private static final int GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS = 20;
    private static final int GRAND_EVENT_KNOCKBACK_GRACE_TICKS = 5;
    private static final long GRAND_EVENT_AUDIBLE_ACTION_WINDOW_TICKS = 40L;
    private static final int GRAND_EVENT_INTENT_REISSUE_MIN_TICKS = 16;
    private static final int GRAND_EVENT_INTENT_NONCONSUMED_TIMEOUT_TICKS = 36;
    private static final int GRAND_EVENT_PENDING_MIN_LIFETIME_TICKS = 20;
    private static final int GRAND_EVENT_PENDING_PROGRESS_STALL_TICKS = 18;
    private static final int GRAND_EVENT_PENDING_NAV_IDLE_STALL_TICKS = 12;
    private static final int GRAND_EVENT_PENDING_CANT_REACH_STALL_TICKS = 12;
    private static final double GRAND_EVENT_PENDING_PROGRESS_EPSILON_BLOCKS = 1.10D;
    private static final double GRAND_EVENT_PENDING_PROGRESS_EPSILON_SQR =
            GRAND_EVENT_PENDING_PROGRESS_EPSILON_BLOCKS * GRAND_EVENT_PENDING_PROGRESS_EPSILON_BLOCKS;
    private static final int GRAND_EVENT_REPLAN_WINDOW_TICKS = 60;
    private static final int GRAND_EVENT_REPLAN_WINDOW_MAX = 7;
    private static final int GRAND_EVENT_REPLAN_COOLDOWN_TICKS = 22;
    private static final double GRAND_EVENT_NODE_CHURN_NEAR_REPEAT_SQR = 6.0D * 6.0D;
    private static final double GRAND_EVENT_SPIN_IN_PLACE_SPEED_MAX = 0.045D;
    private static final double GRAND_EVENT_SPIN_IN_PLACE_YAW_DELTA_MIN = 24.0D;
    private static final long GRAND_EVENT_SPIN_IN_PLACE_DETECT_TICKS = 16L;
    private static final int GRAND_EVENT_LATCH_DISTANCE_SQR = 9 * 9;
    private static final double GRAND_EVENT_ANCHOR_APPROACH_FAR_DISTANCE_SQR = 24.0D * 24.0D;
    private static final double GRAND_EVENT_ANCHOR_APPROACH_VERY_FAR_DISTANCE_SQR = 34.0D * 34.0D;
    private static final double GRAND_EVENT_ANCHOR_LATCH_DISTANCE_SQR = 14.0D * 14.0D;
    private static final double GRAND_EVENT_CLOSE_CONTACT_DISTANCE_SQR = 4.0D * 4.0D;
    private static final int GRAND_EVENT_POST_CLOSE_FOLLOWUP_TICKS = 12 * 20;
    private static final double GRAND_EVENT_ANCHOR_SEARCH_MIN_PLAYER_DISTANCE_SQR = 16.0D * 16.0D;
    private static final double GRAND_EVENT_NON_AGGRO_HARD_CONTACT_DISTANCE_SQR = 4.25D * 4.25D;
    private static final double GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_DISTANCE_SQR = 12.0D * 12.0D;
    private static final int GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_MIN_RADIUS = 16;
    private static final int GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_MAX_RADIUS = 30;
    private static final long GRAND_EVENT_NON_AGGRO_GUARD_LOCKOUT_TICKS = 20L;
    private static final long GRAND_EVENT_NON_AGGRO_STAGNATION_SAMPLE_5S_TICKS = 5L * 20L;
    private static final long GRAND_EVENT_NON_AGGRO_STAGNATION_SAMPLE_10S_TICKS = 10L * 20L;
    private static final double GRAND_EVENT_NON_AGGRO_STAGNATION_5S_MIN_MOVEMENT = 2.5D;
    private static final double GRAND_EVENT_NON_AGGRO_STAGNATION_10S_MIN_MOVEMENT = 5.0D;
    private static final long GRAND_EVENT_NON_AGGRO_STAGNATION_FORCE_REISSUE_TICKS = 80L;
    private static final double GRAND_EVENT_NON_AGGRO_SIGNIFICANT_PROGRESS_BLOCKS = 1.20D;
    private static final long GRAND_EVENT_NON_AGGRO_NODE_WINDOW_TICKS = 60L * 20L;
    private static final int GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_BLOCKS = 12;
    private static final double GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_SQR =
            GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_BLOCKS * GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_BLOCKS;
    private static final int GRAND_EVENT_INTENT_CONSUME_MIN_AGE_TICKS = 10;
    private static final double GRAND_EVENT_INTENT_CONSUME_MIN_MOVE_BLOCKS = 5.5D;
    private static final double GRAND_EVENT_INTENT_CONSUME_MIN_MOVE_SQR =
            GRAND_EVENT_INTENT_CONSUME_MIN_MOVE_BLOCKS * GRAND_EVENT_INTENT_CONSUME_MIN_MOVE_BLOCKS;
    private static final int GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_UNIQUE_SUBZONES = 6;
    private static final double GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_RADIUS_60S = 22.0D;
    private static final int GRAND_EVENT_LOCAL_ORBITING_MIN_WINDOW_NODES = 6;
    private static final double GRAND_EVENT_LOCAL_ORBITING_MAX_SPAN_BLOCKS = 10.0D;
    private static final int GRAND_EVENT_LOCAL_ORBITING_MAX_UNIQUE_SUBZONES = 3;
    private static final int GRAND_EVENT_UNSEEN_REQUIRED_TICKS = 22;
    private static final int GRAND_EVENT_SINK_DURATION_TICKS = 82;
    private static final int GRAND_EVENT_EMPTY_SCOPE_SINK_TICKS = 40;
    private static final int GRAND_EVENT_MAX_DURATION_TICKS = 5 * 60 * 20;
    private static final int GRAND_EVENT_MIN_RUNTIME_TICKS = 60 * 20;
    private static final int GRAND_EVENT_NON_AGGRO_MIN_RUNTIME_TICKS = 72 * 20;
    private static final int GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_CONSUMED_NODES = 8;
    private static final int GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_VISITED_SECTORS = 5;
    private static final int GRAND_EVENT_NON_AGGRO_ACTIVITY_TIMEOUT_TICKS = 50 * 20;
    private static final double GRAND_EVENT_ATTACK_RELEASE_DISTANCE_SQR = 256.0D * 256.0D;
    private static final int GRAND_EVENT_MAX_RECOVERIES = 12;
    private static final int GRAND_EVENT_SEARCH_MIN_TICKS = 100;
    private static final int GRAND_EVENT_SEARCH_MAX_TICKS = 220;
    private static final int GRAND_EVENT_CROSS_MIN_TICKS = 18;
    private static final int GRAND_EVENT_CROSS_MAX_TICKS = 34;
    private static final int GRAND_EVENT_NEXT_CROSS_MIN_TICKS = 24;
    private static final int GRAND_EVENT_NEXT_CROSS_MAX_TICKS = 95;
    private static final double GRAND_EVENT_SEARCH_SPEED = 0.17D;
    private static final double GRAND_EVENT_CROSS_SPEED = 0.30D;
    private static final double GRAND_EVENT_SEARCH_GROUND_SPEED = 0.11D;
    private static final double GRAND_EVENT_CROSS_GROUND_SPEED = 0.18D;
    private static final double GRAND_EVENT_APPROACH_SPEED = 0.20D;
    private static final double GRAND_EVENT_EXIT_SPEED = 0.17D;
    private static final double GRAND_EVENT_SINK_STEP = 0.055D;
    private static final long GRAND_EVENT_ATTACK_PATH_REFRESH_TICKS = 2L;
    private static final long GRAND_EVENT_AGGRO_ANGER_REFRESH_TICKS = 10L;
    private static final double GRAND_EVENT_ATTACK_CHASE_SPEED_NEAR = 1.22D;
    private static final double GRAND_EVENT_ATTACK_CHASE_SPEED_MEDIUM = 1.32D;
    private static final double GRAND_EVENT_ATTACK_CHASE_SPEED_FAR = 1.40D;
    private static final double GRAND_EVENT_ATTACK_MEDIUM_DISTANCE_SQR = 18.0D * 18.0D;
    private static final double GRAND_EVENT_ATTACK_FAR_DISTANCE_SQR = 34.0D * 34.0D;
    private static final double GRAND_EVENT_SONIC_ASSIST_MIN_DISTANCE_SQR = 11.0D * 11.0D;
    private static final long GRAND_EVENT_SONIC_ASSIST_MIN_INTERVAL_TICKS = 70L;
    private static final int GRAND_EVENT_WATER_SPEED_EFFECT_TICKS = 40;
    private static final int GRAND_EVENT_WATER_SPEED_EFFECT_AMPLIFIER = 2;
    private static final double GRAND_EVENT_WATER_SPEED_SCALE = 0.60D;
    private static final float GRAND_EVENT_WATER_PATH_MALUS = 0.0F;
    private static final double GRAND_EVENT_ATTACK_WATER_CHASE_BONUS = 0.78D;
    private static final double GRAND_EVENT_ATTACK_WATER_CHASE_MAX = 2.10D;
    private static final double GRAND_EVENT_NO_BLOCK_WATER_HORIZONTAL_MULTIPLIER = 1.28D;
    private static final long GRAND_EVENT_CAVE_BREAK_INTERVAL_TICKS = 1L;
    private static final int GRAND_EVENT_CAVE_BREAK_MAX_BLOCKS_PER_TICK = 4;
    private static final int GRAND_EVENT_CAVE_BREAK_STUCK_MAX_BLOCKS_PER_TICK = 6;
    private static final long GRAND_EVENT_CAVE_BREAK_STUCK_THRESHOLD_TICKS = 40L;
    private static final long GRAND_EVENT_ROAR_SNIFF_STUCK_THRESHOLD_TICKS = 20L;
    private static final int GRAND_EVENT_SEARCH_SNIFF_MIN_INTERVAL_TICKS = 18 * 20;
    private static final int GRAND_EVENT_SEARCH_SNIFF_MAX_INTERVAL_TICKS = 32 * 20;
    private static final int GRAND_EVENT_SEARCH_SNIFF_POSE_TICKS = 10;
    private static final double GRAND_EVENT_SEARCH_SNIFF_MIN_PLAYER_DISTANCE_SQR = 8.0D * 8.0D;
    private static final int GRAND_EVENT_SEARCH_SNIFF_POST_HARD_CONTACT_COOLDOWN_TICKS = 4 * 20;
    private static final int GRAND_EVENT_SNIFF_PASS_MIN_RADIUS = 2;
    private static final int GRAND_EVENT_SNIFF_PASS_MAX_RADIUS = 3;
    private static final double GRAND_EVENT_SNIFF_TRIGGER_DISTANCE_SQR = 2.5D * 2.5D;
    private static final int GRAND_EVENT_SNIFF_PENDING_TIMEOUT_TICKS = 140;
    private static final int GRAND_EVENT_SNIFF_POSE_TICKS = 22;
    private static final int GRAND_EVENT_SNIFF_RETREAT_MIN_RADIUS = 8;
    private static final int GRAND_EVENT_SNIFF_RETREAT_MAX_RADIUS = 14;
    private static final long GRAND_EVENT_SNIFF_SOUND_COOLDOWN_TICKS = 40L;
    private static final int GRAND_EVENT_EMERGE_DURATION_TICKS = 44;
    private static final int GRAND_EVENT_RECENT_SEARCH_HISTORY_LIMIT = 10;
    private static final double GRAND_EVENT_RECENT_SEARCH_MIN_DISTANCE_SQR = 16.0D * 16.0D;
    private static final int GRAND_EVENT_RECENT_OVERLAP_STALL_RESET_THRESHOLD = 3;
    private static final long GRAND_EVENT_RECENT_OVERLAP_RESET_COOLDOWN_TICKS = 60L;
    private static final int GRAND_EVENT_SEARCH_POOL_ATTEMPTS = 28;
    private static final int GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS = 36;
    private static final int GRAND_EVENT_NO_PATH_RECOVERY_STREAK_THRESHOLD = 8;
    private static final double GRAND_EVENT_DIRECT_FOCUS_FALLBACK_MIN_DISTANCE_SQR = 56.0D * 56.0D;
    private static final int GRAND_EVENT_SEARCH_SECTOR_COUNT = 8;
    private static final int GRAND_EVENT_SEARCH_MICROZONE_RING_COUNT = 3;
    private static final double GRAND_EVENT_SEARCH_MICROZONE_RING_NEAR_MAX = 14.0D;
    private static final double GRAND_EVENT_SEARCH_MICROZONE_RING_MID_MAX = 22.0D;
    private static final double GRAND_EVENT_SEARCH_SAME_SECTOR_PENALTY = 18.0D;
    private static final double GRAND_EVENT_SEARCH_ADJACENT_SECTOR_PENALTY = 9.0D;
    private static final double GRAND_EVENT_SEARCH_RECENT_SECTOR_PENALTY_STEP = 3.5D;
    private static final double GRAND_EVENT_SEARCH_SECTOR_COVERAGE_BONUS_STEP = 6.0D;
    private static final double GRAND_EVENT_SEARCH_DIRECTION_WINDOW_DEGREES = 48.0D;
    private static final double GRAND_EVENT_SEARCH_DIRECTION_REPEAT_PENALTY = 8.0D;
    private static final double GRAND_EVENT_SEARCH_DIRECTION_COVERAGE_BONUS_SCALE = 0.22D;
    private static final double GRAND_EVENT_SPECIAL_PAUSE_RADIUS = 96.0D;
    private static final String GRAND_EVENT_PAUSED_SPECIAL_TAG = "eotv_grand_pause_special";
    private static final float GRAND_WARDEN_STEP_UP = 3.0F;
    private static final double GRAND_EVENT_CAVE_EXIT_RETREAT_MIN_PLAYER_DISTANCE_SQR = 8.0D * 8.0D;
    private static final int GRAND_EVENT_CAVE_EXIT_RETREAT_MIN_RADIUS = 10;
    private static final int GRAND_EVENT_CAVE_EXIT_RETREAT_MAX_RADIUS = 22;
    private static final long GRAND_EVENT_CAVE_EXIT_RETREAT_RETARGET_TICKS = 20L;
    private static final long GRAND_EVENT_CAVE_EXIT_RETREAT_TIMEOUT_TICKS = 120L;
    private static final int GRAND_EVENT_CAVE_EXIT_RETREAT_MAX_ATTEMPTS = 8;
    private static final String GRAND_WARDEN_DISPLAY_NAME = "Warden?";
    private static final Pose WARDEN_SNIFF_POSE = resolvePoseByName("SNIFFING");
    private static final Pose WARDEN_EMERGE_POSE = resolvePoseByName("EMERGING");
    private static final Pose WARDEN_DIG_POSE = resolvePoseByName("DIGGING");
    private static final String GRAND_EVENT_RUNTIME_CLASS = UncannyParanoiaEventSystem.class.getName();
    private static final String GRAND_EVENT_RUNTIME_BUILD_SIGNATURE = resolveGrandEventBuildSignature();
    private static final List<String> GRAND_EVENT_WARNING_LINES = List.of(
            "DON'T MOVE",
            "DON'T MAKE A SOUND",
            "IT IS HERE",
            "IT IS HERE",
            "IT IS HERE");
    private static Method WARDEN_INCREASE_ANGER_METHOD;
    private static boolean WARDEN_INCREASE_ANGER_LOOKUP_DONE;
    private static boolean WARDEN_KEEPALIVE_INVOKE_FAILURE_LOGGED;
    private static Method ENTITY_SET_MAX_UP_STEP_METHOD;
    private static boolean ENTITY_SET_MAX_UP_STEP_LOOKUP_DONE;
    private static boolean ENTITY_SET_MAX_UP_STEP_INVOKE_FAILURE_LOGGED;
    private static MemoryModuleType<?> WARDEN_SONIC_COOLDOWN_MEMORY;
    private static boolean WARDEN_SONIC_COOLDOWN_LOOKUP_DONE;
    private static boolean WARDEN_SONIC_COOLDOWN_LOOKUP_LOGGED;
    private static final Component SLEEP_DISTURB_MESSAGE = Component.literal("There is something in your bed.");
    private static final Component WORKBENCH_REJECT_MESSAGE = Component.literal("Not this one.");
    private static final Component COMPASS_LIAR_MESSAGE =
            Component.literal("You feel like your compass is pointing towards an anomaly for now.");

    private static final int COOLDOWN_ANIMAL_STARE_LOCK_SECONDS = 900;
    private static final int COOLDOWN_COMPASS_LIAR_SECONDS = 1200;
    private static final int COOLDOWN_FURNACE_BREATH_SECONDS = 480;
    private static final int COOLDOWN_MISPLACED_LIGHT_SECONDS = 900;
    private static final int COOLDOWN_PET_REFUSAL_SECONDS = 1500;
    private static final int COOLDOWN_WORKBENCH_REJECT_SECONDS = 1800;
    private static final int COOLDOWN_FALSE_CONTAINER_OPEN_SECONDS = 300;
    private static final int COOLDOWN_LEVER_ANSWER_SECONDS = 300;
    private static final int COOLDOWN_PRESSURE_PLATE_REPLY_SECONDS = 300;
    private static final int COOLDOWN_CAMPFIRE_COUGH_SECONDS = 480;
    private static final int COOLDOWN_BUCKET_DRIP_SECONDS = 360;
    private static final int COOLDOWN_HOTBAR_WRONG_COUNT_SECONDS = 480;
    private static final int COOLDOWN_FALSE_RECIPE_TOAST_SECONDS = 1200;
    private static final int COOLDOWN_TOOL_ANSWER_SECONDS = 600;
    private static final int COOLDOWN_BEDSIDE_OPEN_SECONDS = 600;
    private static final List<String> CORRUPT_MESSAGE_PHASE1_POOL = parseMessageLines("""
            Something feels wrong.
            It noticed.
            You were seen.
            Too quiet.
            You missed it.
            It is near.
            Keep the lights on.
            You should leave.
            You are not alone.
            That was not wind.
            It heard you.
            Be careful now.
            Do not stay long.
            This is not safe.
            It is still here.
            You took too long.
            There is a reason for the silence.
            You should not be here yet.
            Something changed.
            You were followed.
            The dark moved first.
            It is closer at night.
            It is watching you.
            Something is watching you.
            You are being watched.
            Eyes on you.
            It can see you.
            Do not face it.
            It waits for you to notice.
            It likes when you look.
            It is staring.
            Do not meet its eyes.
            It knows where you are.
            You are easy to find.
            It has not blinked once.
            Stand still and listen.
            It is behind the next glance.
            You looked too long.
            It was already there.
            You were supposed to miss it.
            It moved when you blinked.
            It watches from the edge.
            It only moves for you.
            It knows your face.
            It knows your steps.
            You are easier to track now.
            It has your rhythm.
            Do not let it learn you.
            It recognizes you now.
            You have been marked.
            It can follow your breathing.
            It wants you to turn around.
            It knows your name.
            It knows your home.
            It knows your bed.
            It knows where you return.
            It remembers your door.
            It remembers your base.
            It remembers your path.
            It knows what you carry.
            It knows what you lost.
            It knows where you hide.
            It knows where you sleep.
            It knows when you leave.
            It counts your returns.
            It remembers your last death.
            It remembers your last mistake.
            It knows which chest is yours.
            It knows your favorite room.
            It knows which torch you placed first.
            It knows the sound of your tools.
            It remembers your footsteps better than you do.
            Something entered your house.
            Something waits inside.
            Something touched your storage.
            Something stood by your bed.
            Something passed through your walls.
            You left it inside.
            It learned your home.
            Your home does not feel empty anymore.
            It was here before you returned.
            Do not trust the quiet in your base.
            Your walls heard that.
            It likes your house.
            Your door opened for a reason.
            Something is using your home.
            You are sharing this place now.
            Your hands are not steady.
            Something is wrong with your pulse.
            Your body noticed first.
            Something touched your shoulder.
            That cold is not natural.
            It is close enough to hear you swallow.
            Your breathing is louder now.
            It can hear your heart.
            It is learning your fear.
            Reality slipped.
            That should not happen.
            The world twitched.
            Something desynced.
            This is not how it was.
            That was moved.
            You remember this differently.
            Something changed while you were not looking.
            The world is misaligned.
            One thing is out of place.
            You are not where you think you are.
            This part is wrong.
            The world blinked.
            Something is using old shapes.
            It is rewriting small things first.
            The room is almost the same.
            There is a mistake in this place.
            Do not trust what is familiar.
            It is changing the safe parts first.
            The world is making room.
            """);
    private static final List<String> CORRUPT_MESSAGE_CAVE_POOL = parseMessageLines("""
            The stone is not empty.
            Do not dig deeper.
            There is something in the wall.
            The cave heard that.
            Something below answered.
            You woke something up.
            The mine remembers you.
            Stop digging.
            That sound came from inside.
            The wall hit back.
            You are opening the wrong place.
            It is under the stone.
            Something is below this layer.
            The rock is listening.
            The deep is not asleep.
            It waits under your feet.
            Do not mine that vein.
            Something moved in the ore.
            The cave is breathing.
            There is no bottom for it.
            """);
    private static final List<String> CORRUPT_MESSAGE_PHASE3_POOL = parseMessageLines("""
            Turn around.
            Look behind you.
            Don't turn around.
            Run.
            Hide now.
            Too late to hide.
            It is already here.
            It is already inside.
            You let it get close.
            It is in front of you.
            Do not let it touch you.
            It wants you still.
            It wants you to freeze.
            Do not answer it.
            Do not follow it.
            Leave the light on.
            It wants the dark.
            It wants you alone.
            It chose you.
            You were selected.
            You are the nearest.
            It is coming for you.
            It is moving now.
            It has waited long enough.
            It will not stay hidden now.
            It does not need to pretend anymore.
            You are out of time.
            It is done watching.
            It is done waiting.
            It has started.
            There is no safe room now.
            It found the way in.
            It crossed the line.
            It is using your path.
            It is faster than before.
            It knows where to cut you off.
            You should have left earlier.
            It wants to be seen.
            It wants to be followed.
            It wants your attention.
            It wants you curious.
            It wants you close.
            It wants you to notice the wrong thing.
            It wants you facing away.
            It wants you near the door.
            It wants you under the dark.
            It wants you in the open.
            It wants you underground.
            It wants you home.
            It wants you tired.
            It wants you late.
            It wants you to think it left.
            It wants you to trust the silence.
            It wants one mistake.
            It wants one open door.
            It wants one missed sound.
            Something sat on your bed.
            Something waited by your chest.
            Something opened the right door.
            Something knows this room.
            Something stood where you stand.
            Something touched your things.
            Something watched your door.
            Something crossed your floor.
            Something stopped in your hallway.
            Something listened at your wall.
            Something waited for you to leave.
            Something stayed after you did.
            Something learned your routine.
            Something knows your storage.
            Something paused at your bed.
            Something likes this house.
            Something remembers this room.
            Something is comfortable here.
            Something is learning the way in.
            Something no longer feels like a guest.
            Behind you.
            Still there.
            Not empty.
            Too close.
            Wrong one.
            Don't blink.
            Not safe.
            It stayed.
            It moved.
            Not gone.
            Don't follow.
            Don't answer.
            Don't stop.
            Not yours.
            It saw.
            It heard.
            It knows.
            Too late.
            Look away.
            Run now.
            You have seen this before.
            This already happened.
            You were warned already.
            You ignored the first sign.
            You are learning too slowly.
            The pattern is for you.
            You keep surviving the wrong things.
            It noticed your habits.
            It noticed what scares you.
            You are easier to scare now.
            You made this easier.
            It only needs one opening.
            It is patient with you.
            It can wait longer than you can.
            You keep coming back. So does it.
            It remembers every return.
            You were never the first one here.
            You will not be the last.
            It is closing the distance.
            It is choosing an angle.
            It is waiting for the turn.
            It wants the blind spot.
            It wants the doorway.
            It wants the moment after silence.
            It wants the second look.
            It wants the room you trust most.
            It wants you facing the wrong way.
            It wants you occupied.
            It wants your hands full.
            It wants your torch first.
            It wants your breath shallow.
            It wants your door open.
            It wants you listening elsewhere.
            It wants your routine unchanged.
            It wants one peaceful second.
            It wants you home before it moves.
            It wants to be noticed late.
            It wants the familiar path.
            It only appears when you feel safe.
            It waits for habits, not mistakes.
            It learns from what you ignore.
            It prefers the things you trust.
            It hides in routines.
            It follows the comfortable paths.
            It uses ordinary sounds first.
            It changes small things before large ones.
            It starts where you will doubt yourself.
            It wants confusion before fear.
            It wants you uncertain, not screaming.
            It wants to be mistaken for normal.
            It borrows familiar shapes.
            It enters through recognition.
            It waits where memory is weak.
            It benefits from hesitation.
            It hides in second guesses.
            It gets closer each time you dismiss it.
            It survives by feeling explainable.
            It is safer when you call it nothing.
            """);
    private static final List<String> CORRUPT_MESSAGE_PHASE4_POOL = parseMessageLines("""
            It has access now.
            The boundary failed.
            Containment was never real.
            There is no separation now.
            It is inside the safe parts.
            It can reach you here.
            The quiet belongs to it now.
            It no longer needs distance.
            It no longer needs dark.
            It no longer needs you alone.
            It crossed over cleanly.
            This world leaves openings.
            It found yours.
            It is settled in.
            It will return to this place.
            It will use what you built.
            You made a place for it.
            It knows how to come back.
            It knows how to wait inside.
            It is part of the pattern now.
            Don't.
            Wait.
            Leave.
            Listen.
            Quiet.
            Again.
            Closer.
            Below.
            Inside.
            Wrong.
            Open.
            Still.
            There.
            Home.
            Late.
            Near.
            Mine.
            Watch.
            Stay.
            Gone?
            """);
    private static final List<String> CORRUPT_MESSAGE_EN_POOL = CORRUPT_MESSAGE_PHASE1_POOL;

    private static final Map<UUID, BlackoutState> ACTIVE_BLACKOUTS = new HashMap<>();
    private static final Map<UUID, FootstepsState> ACTIVE_FOOTSTEPS = new HashMap<>();
    private static final Map<UUID, FlashErrorState> ACTIVE_FLASH_EVENTS = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_DEAFNESS = new HashMap<>();
    private static final Map<UUID, VoidSilenceState> ACTIVE_VOID_SILENCE = new HashMap<>();
    private static final Map<UUID, GhostMinerState> ACTIVE_GHOST_MINERS = new HashMap<>();
    private static final Map<UUID, AsphyxiaState> ACTIVE_ASPHYXIA = new HashMap<>();
    private static final Map<UUID, HunterFogState> ACTIVE_HUNTER_FOG = new HashMap<>();
    private static final Map<UUID, GiantSunState> ACTIVE_GIANT_SUN = new HashMap<>();
    private static final Map<UUID, CompassLiarState> ACTIVE_COMPASS_LIARS = new HashMap<>();
    private static final Map<UUID, AnimalStareLockState> ACTIVE_ANIMAL_STARE_LOCKS = new HashMap<>();
    private static final Map<UUID, FurnaceBreathState> ACTIVE_FURNACE_BREATHS = new HashMap<>();
    private static final Map<UUID, MisplacedLightState> ACTIVE_MISPLACED_LIGHTS = new HashMap<>();
    private static final Map<UUID, PetRefusalState> ACTIVE_PET_REFUSALS = new HashMap<>();
    private static final Map<UUID, HotbarWrongCountState> ACTIVE_HOTBAR_WRONG_COUNTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGERED_PRESSURE_PLATE_TICKS = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_TRIGGERED_PRESSURE_PLATE_POS = new HashMap<>();
    private static final Map<UUID, Long> LAST_CONTAINER_OPEN_TICKS = new HashMap<>();
    private static final Map<UUID, ContainerEchoContext> LAST_CONTAINER_CONTEXTS = new HashMap<>();
    private static final Map<UUID, ToolAnswerContext> LAST_TOOL_ANSWER_CONTEXT = new HashMap<>();
    private static final Map<UUID, TurnAroundTrapState> ACTIVE_TURN_AROUND_TRAPS = new HashMap<>();
    private static final Map<UUID, LivingOreState> LIVING_ORE_PRIMED = new HashMap<>();
    private static final Map<UUID, AquaticBiteState> ACTIVE_AQUATIC_BITE = new HashMap<>();
    private static final Map<UUID, Long> FLASH_RED_OVERLAY_END_TICKS = new HashMap<>();
    private static final Map<UUID, SleepDisturbanceState> ACTIVE_SLEEP_DISTURBANCES = new HashMap<>();
    private static final Map<UUID, Long> PENDING_SLEEP_MESSAGE_TICKS = new HashMap<>();
    private static final Set<UUID> SKIP_NEXT_SLEEP_DISTURB = new HashSet<>();
    private static final Set<UUID> REQUIRE_NORMAL_SLEEP_BEFORE_NEXT_DISTURB = new HashSet<>();
    private static final Map<UUID, Long> NEXT_SLEEP_DISTURB_ALLOWED_TICKS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_AUTO_CHECK_TICKS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SPECIAL_ENTITY_CHECK_TICKS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> EVENT_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> AMBIENT_EVENT_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> SPECIAL_ENTITY_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SPECIAL_ENTITY_EVENT_TICKS = new HashMap<>();
    private static final Map<UUID, Long> LAST_AMBIENT_EVENT_TICKS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_LIVING_ORE_CHECK_TICKS = new HashMap<>();
    private static final Map<UUID, Long> LIVING_ORE_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> TENANT_AWAY_SINCE = new HashMap<>();
    private static final Map<UUID, Long> GRAND_EVENT_RECENT_AUDIBLE_ACTION_TICKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, GrandEventState> ACTIVE_GRAND_EVENTS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, PausedSpecialSnapshot>> ACTIVE_GRAND_PAUSED_SPECIALS = new HashMap<>();
    private static final List<ChestCloseTask> CHEST_CLOSE_TASKS = new ArrayList<>();
    private static final List<ChestPanicTask> CHEST_PANIC_TASKS = new ArrayList<>();
    private static final List<FurnaceResetTask> FURNACE_RESET_TASKS = new ArrayList<>();
    private static final List<WaterRestoreTask> WATER_RESTORE_TASKS = new ArrayList<>();
    private static final List<LeverReplyTask> LEVER_REPLY_TASKS = new ArrayList<>();
    private static final List<DoorCascadeTask> DOOR_CASCADE_TASKS = new ArrayList<>();
    private static final List<PressurePlateReplyTask> PRESSURE_PLATE_REPLY_TASKS = new ArrayList<>();
    private static final List<ToolAnswerEchoTask> TOOL_ANSWER_ECHO_TASKS = new ArrayList<>();
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Map<BlockPos, Long>> LOCKED_DOORS = new HashMap<>();
    private static long lastChestTaskTick = Long.MIN_VALUE;

    private UncannyParanoiaEventSystem() {
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Event] " + message, args);
        }
    }

    private static String resolveGrandEventBuildSignature() {
        String version = "unknown";
        try {
            version = ModList.get()
                    .getModContainerById(EchoOfTheVoid.MODID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
        } catch (Throwable ignored) {
        }

        String crcHex = "nocrc";
        try (InputStream stream = UncannyParanoiaEventSystem.class.getResourceAsStream("UncannyParanoiaEventSystem.class")) {
            if (stream != null) {
                CRC32 crc32 = new CRC32();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = stream.read(buffer)) > 0) {
                    crc32.update(buffer, 0, read);
                }
                crcHex = Long.toHexString(crc32.getValue());
            }
        } catch (Throwable ignored) {
        }

        String jarPath = "unknown";
        try {
            CodeSource source = UncannyParanoiaEventSystem.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                jarPath = source.getLocation().toString();
            }
        } catch (Throwable ignored) {
        }
        return "v=" + version + "|crc=" + crcHex + "|jar=" + jarPath;
    }

    private static String playerLabel(ServerPlayer player) {
        return player.getGameProfile().getName() + "@" + player.blockPosition();
    }

    private static Pose resolvePoseByName(String poseName) {
        if (poseName == null || poseName.isBlank()) {
            return null;
        }
        for (Pose pose : Pose.values()) {
            if (pose.name().equalsIgnoreCase(poseName)) {
                return pose;
            }
        }
        return null;
    }

    private static void applyGrandWardenRenderMarkerName(Warden warden) {
        if (warden == null) {
            return;
        }
        warden.setCustomName(Component.literal(GRAND_WARDEN_DISPLAY_NAME));
        warden.setCustomNameVisible(false);
    }

    public static String getFlashRedOverlayTag() {
        return FLASH_RED_OVERLAY_TAG;
    }

    public static int getFlashRedMarkerAmplifier() {
        return FLASH_RED_MARKER_AMPLIFIER;
    }

    public static String getHunterFogTag() {
        return HUNTER_FOG_TAG;
    }

    public static String getGiantSunTag() {
        return GIANT_SUN_TAG;
    }

    public static String getGrandPauseSpecialTag() {
        return GRAND_EVENT_PAUSED_SPECIAL_TAG;
    }

    public static boolean isGrandEventAutoPauseActive(ServerLevel level) {
        if (level == null) {
            return false;
        }
        GrandEventState state = ACTIVE_GRAND_EVENTS.get(level.dimension());
        return state != null && !state.ended();
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator()) {
            return;
        }

        long now = server.getTickCount();
        UncannyWorldState worldState = UncannyWorldState.get(server);
        if (!worldState.isSystemEnabled()) {
            clearPlayerEventState(player);
            return;
        }
        UncannyPhase phase = worldState.getPhase();
        if (phase.index() >= UncannyPhase.PHASE_2.index() && (now % 20L) == 0L) {
            player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        }
        int profile = getIntensityProfile();
        ServerLevel level = player.serverLevel();
        tickTensionBuilder(level, worldState, now, phase);
        tickActiveGrandEvent(level, now);
        if (isGrandEventAutoPauseActive(level)) {
            suppressNonGrandEventEffectsDuringGrandPause(player);
            return;
        }
        if (isTensionBuilderEventPauseActive(level, worldState, now)) {
            if ((now % 20L) == 0L) {
                debugLog(
                        "TENSION pause_all_events=true source=on_player_tick remaining={}s",
                        ticksToSeconds(worldState.getTensionBuilderEndTick() - now));
            }
            suppressNonGrandEventEffectsDuringGrandPause(player);
            return;
        }

        UUID playerId = player.getUUID();
        if (!NEXT_AUTO_CHECK_TICKS.containsKey(playerId) || !NEXT_SPECIAL_ENTITY_CHECK_TICKS.containsKey(playerId)) {
            clearPlayerEventState(player);
            long nextAuto = now + INITIAL_EVENT_JOIN_GRACE_TICKS + rollAutoCheckIntervalTicks(phase, profile, player.serverLevel());
            long nextSpecial = now + INITIAL_SPECIAL_JOIN_GRACE_TICKS + rollSpecialEntityCheckIntervalTicks(phase, profile, player.serverLevel());
            NEXT_AUTO_CHECK_TICKS.put(playerId, nextAuto);
            NEXT_SPECIAL_ENTITY_CHECK_TICKS.put(playerId, nextSpecial);
            debugLog(
                    "EVENT init-session player={} phase={} nextAuto={}t nextSpecial={}t",
                    playerLabel(player),
                    phase.index(),
                    Math.max(0L, nextAuto - now),
                    Math.max(0L, nextSpecial - now));
            return;
        }

        if (phase.index() < UncannyPhase.PHASE_2.index()) {
            // Keep phase-1 ambiance stable: no lingering silence/deafness carryover.
            ACTIVE_DEAFNESS.remove(playerId);
            ACTIVE_VOID_SILENCE.remove(playerId);
        }

        tickFlashRedOverlay(player, now);
        tickPendingSleepMessage(player, now);
        tickBlackout(player, now);
        tickFootsteps(player, now);
        tickFlashError(player, now);
        tickDeafness(player, now);
        tickVoidSilence(player, now);
        tickGhostMiner(player, now);
        tickAsphyxia(player, now);
        tickHunterFog(player, now);
        tickGiantSun(player, now);
        tickCompassLiar(player, now);
        tickAnimalStareLock(player, now);
        tickFurnaceBreath(player, now);
        tickMisplacedLight(player, now);
        tickPetRefusal(player, now);
        tickHotbarWrongCount(player, now);
        tickPressurePlateReply(player, now);
        tickTurnAroundTrap(player, now);
        tickAquaticBite(player, now);
        tickLivingOre(player, now);
        tickChestCloseTasks(server, now);
        UncannyClientStateSync.syncParanoiaState(
                player,
                ACTIVE_HUNTER_FOG.containsKey(playerId),
                ACTIVE_GIANT_SUN.containsKey(playerId));

        if (player.isSleeping()) {
            ACTIVE_SLEEP_DISTURBANCES.remove(playerId);
            PENDING_SLEEP_MESSAGE_TICKS.remove(playerId);
            REQUIRE_NORMAL_SLEEP_BEFORE_NEXT_DISTURB.remove(playerId);
        }

        maybeTriggerIndependentLivingOre(player, now, phase);
        updateTenantAwayTracking(player, now, server);

        maybeTriggerSpecialEntityEncounter(player, now);

        long nextAutoCheck = NEXT_AUTO_CHECK_TICKS.getOrDefault(playerId, Long.MIN_VALUE);
        if (now < nextAutoCheck) {
            return;
        }

        NEXT_AUTO_CHECK_TICKS.put(playerId, now + rollAutoCheckIntervalTicks(phase, profile, player.serverLevel()));
        maybeTriggerRandomEvent(player, now);
        maybeTriggerAmbientSoundEvent(player, now);
    }

    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator()) {
            return;
        }

        UncannyWorldState worldState = UncannyWorldState.get(server);
        if (!worldState.isSystemEnabled()) {
            return;
        }
        if (isGrandEventAutoPauseActive(player.serverLevel())
                || isTensionBuilderEventPauseActive(player.serverLevel(), worldState, server.getTickCount())) {
            return;
        }

        tryTriggerBedsideOpen(player, server, event.getPos());

        UUID playerId = player.getUUID();
        SleepDisturbanceState state = ACTIVE_SLEEP_DISTURBANCES.get(playerId);
        if (state == null) {
            if (SKIP_NEXT_SLEEP_DISTURB.remove(playerId)) {
                return;
            }
            if (REQUIRE_NORMAL_SLEEP_BEFORE_NEXT_DISTURB.contains(playerId)) {
                return;
            }
            UncannyPhase phase = UncannyWorldState.get(server).getPhase();
            if (phase.index() < UncannyPhase.PHASE_2.index()) {
                return;
            }
            if (event.getVanillaProblem() != null) {
                return;
            }
            long now = server.getTickCount();
            long nextAllowed = NEXT_SLEEP_DISTURB_ALLOWED_TICKS.getOrDefault(playerId, Long.MIN_VALUE);
            if (now < nextAllowed) {
                return;
            }
            double chance = getSleepDisturbChance(phase, getIntensityProfile());
            if (player.getRandom().nextDouble() >= chance) {
                return;
            }

            state = new SleepDisturbanceState(event.getPos(), 1);
            ACTIVE_SLEEP_DISTURBANCES.put(playerId, state);
            NEXT_SLEEP_DISTURB_ALLOWED_TICKS.put(playerId, now + rollSleepDisturbCooldownTicks(player.serverLevel(), phase, getIntensityProfile()));
            queueSleepDisturbMessage(player, now);
            event.setProblem(BedSleepingProblem.OTHER_PROBLEM);
            return;
        }

        state.setBedPos(event.getPos());
        state.incrementAttempts();
        queueSleepDisturbMessage(player, server.getTickCount());
        event.setProblem(BedSleepingProblem.OTHER_PROBLEM);

        if (state.attempts() >= SLEEP_DISTURB_REQUIRED_CLICKS) {
            spawnPulseInBed(player, state.bedPos());
            ACTIVE_SLEEP_DISTURBANCES.remove(playerId);
            PENDING_SLEEP_MESSAGE_TICKS.remove(playerId);
            SKIP_NEXT_SLEEP_DISTURB.add(playerId);
            REQUIRE_NORMAL_SLEEP_BEFORE_NEXT_DISTURB.add(playerId);
            long now = server.getTickCount();
            UncannyPhase phase = UncannyWorldState.get(server).getPhase();
            long extendedCooldown = now + rollSleepDisturbCooldownTicks(player.serverLevel(), phase, getIntensityProfile());
            NEXT_SLEEP_DISTURB_ALLOWED_TICKS.merge(playerId, extendedCooldown, Math::max);
        }
    }

    public static boolean triggerBedDisturbance(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        ACTIVE_SLEEP_DISTURBANCES.put(player.getUUID(), new SleepDisturbanceState(player.blockPosition(), 0));
        return true;
    }

    public static boolean triggerBedsideOpen(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        BlockPos nearestDoor = findNearestDoor(player.serverLevel(), player.blockPosition(), 16);
        if (nearestDoor == null) {
            debugLog("EVENT bedside_open manual-skip player={} reason=no-door", playerLabel(player));
            return false;
        }
        DOOR_CASCADE_TASKS.removeIf(task -> task.playerId().equals(player.getUUID()));
        setDoorOpen(player, player.serverLevel(), nearestDoor, true);
        playLocalSoundAt(player, nearestDoor, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.96F + player.getRandom().nextFloat() * 0.08F);
        return true;
    }

    private static void tryTriggerBedsideOpen(ServerPlayer player, MinecraftServer server, BlockPos bedPos) {
        UncannyPhase phase = UncannyWorldState.get(server).getPhase();
        if (phase.index() < UncannyPhase.PHASE_2.index()) {
            return;
        }

        long now = server.getTickCount();
        if (!isManualEventCooldownReady(player, "bedside_open", now, COOLDOWN_BEDSIDE_OPEN_SECONDS)) {
            return;
        }

        if (player.getRandom().nextDouble() > 0.14D) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = bedPos != null ? bedPos : player.blockPosition();
        BlockPos nearestDoor = findNearestDoor(level, center, 16);
        if (nearestDoor == null) {
            debugLog("EVENT bedside_open skip player={} reason=no-door", playerLabel(player));
            return;
        }

        DOOR_CASCADE_TASKS.removeIf(task -> task.playerId().equals(player.getUUID()));
        setDoorOpen(player, level, nearestDoor, true);
        playLocalSoundAt(player, nearestDoor, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.96F + level.random.nextFloat() * 0.08F);
        applyManualEventCooldown(player, "bedside_open", now, COOLDOWN_BEDSIDE_OPEN_SECONDS);
        debugLog("EVENT bedside_open player={} door={}", playerLabel(player), nearestDoor);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null) {
            return;
        }
        UncannyWorldState worldState = UncannyWorldState.get(player.getServer());
        if (!worldState.isSystemEnabled()) {
            return;
        }
        if (isGrandEventAutoPauseActive(player.serverLevel())
                || isTensionBuilderEventPauseActive(player.serverLevel(), worldState, player.getServer().getTickCount())) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        long now = player.getServer().getTickCount();
        BlockState state = level.getBlockState(pos);

        if (isDoorLocked(level, pos, now)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (state.is(Blocks.CRAFTING_TABLE)) {
            if (tryTriggerWorkbenchRejectOnInteract(player, now)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
        }

        if (isContainerInteractionBlock(state)) {
            tryTriggerFalseContainerOpenOnInteract(player, state, pos, now);
        }

        if (state.getBlock() instanceof LeverBlock) {
            tryTriggerLeverAnswerOnInteract(player, pos, now, true);
        }

    }

    public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        Entity target = event.getTarget();
        if (target != null && target.getTags().contains("eotv_pet_refusal")) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onPlayerEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        Entity target = event.getTarget();
        if (target != null && target.getTags().contains("eotv_pet_refusal")) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        ItemStack used = player.getItemInHand(event.getHand());
        if (isGrandEventAudibleRightClickItem(used)) {
            markGrandEventAudibleAction(player, "right_click_item");
        }
    }

    public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        if (isGrandEventAudibleUseFinish(event.getItem())) {
            markGrandEventAudibleAction(player, "use_item_finish");
        }
    }

    private static boolean isGrandEventAudibleRightClickItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG)
                || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.EXPERIENCE_BOTTLE)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.FIREWORK_ROCKET)
                || stack.is(Items.FISHING_ROD)
                || stack.is(Items.GOAT_HORN);
    }

    private static boolean isGrandEventAudibleUseFinish(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.has(DataComponents.FOOD)
                || stack.is(Items.POTION)
                || stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.MILK_BUCKET);
    }

    private static void markGrandEventAudibleAction(ServerPlayer player, String reason) {
        if (player == null || player.getServer() == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        GrandEventState state = ACTIVE_GRAND_EVENTS.get(level.dimension());
        if (state == null || state.ended()) {
            return;
        }
        if (getGrandEventScopeRejectReason(level, state, player) != null) {
            return;
        }
        long now = player.getServer().getTickCount();
        GRAND_EVENT_RECENT_AUDIBLE_ACTION_TICKS.put(player.getUUID(), now);
        if (shouldSampleGrandEventRuntime(now)) {
            debugLog(
                    "GRAND_EVENT audible_action player={} reason={} tick={} runtime={}",
                    playerLabel(player),
                    reason,
                    now,
                    state.runtimeId());
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getServer() == null) {
            return;
        }
        UncannyWorldState worldState = UncannyWorldState.get(player.getServer());
        if (!worldState.isSystemEnabled()) {
            return;
        }
        if (isGrandEventAutoPauseActive(player.serverLevel())
                || isTensionBuilderEventPauseActive(player.serverLevel(), worldState, player.getServer().getTickCount())) {
            return;
        }

        long now = player.getServer().getTickCount();
        BlockPos pos = event.getPos();
        BlockState block = player.serverLevel().getBlockState(pos);
        LAST_TOOL_ANSWER_CONTEXT.put(
                player.getUUID(),
                new ToolAnswerContext(pos.immutable(), block, player.getMainHandItem().copy(), now, player.serverLevel().dimension()));

        LivingOreState state = LIVING_ORE_PRIMED.get(player.getUUID());
        if (state != null) {
            if (now >= state.endTick()) {
                LIVING_ORE_PRIMED.remove(player.getUUID());
            } else if (isLivingOreTriggerBlock(block)) {
                triggerLivingOreOnBreak(player, pos, state.variant(), now);
                LIVING_ORE_PRIMED.remove(player.getUUID());
            }
        }

        if (!player.getMainHandItem().isEmpty() && !player.getMainHandItem().is(Items.AIR)) {
            tryTriggerToolAnswerOnBreak(player, pos, block, now);
        }
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof Warden attackerWarden) {
            if (attackerWarden.getTags().contains(GRAND_WARDEN_TAG)) {
                GrandEventState state = ACTIVE_GRAND_EVENTS.get(player.serverLevel().dimension());
                boolean authorizedAttack = state != null
                        && !state.ended()
                        && attackerWarden.getUUID().equals(state.wardenUuid())
                        && state.attackTarget() != null
                        && state.attackTarget().equals(player.getUUID());
                if (!authorizedAttack) {
                    event.setCanceled(true);
                    debugLog(
                            "GRAND_EVENT damage_blocked reason=unauthorized_attack attacker={} victim={} runtime={} dim={}",
                            attackerWarden.getUUID(),
                            playerLabel(player),
                            state == null ? "none" : state.runtimeId(),
                            player.serverLevel().dimension().location());
                    return;
                }
            }
        }

        if (!(event.getEntity() instanceof Warden warden)) {
            return;
        }
        if (!warden.getTags().contains(GRAND_WARDEN_TAG)) {
            return;
        }
        if (event.getSource().is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Mob mob && UncannyEntityRegistry.isSpecialEntity(mob.getType())) {
            Map<UUID, PausedSpecialSnapshot> paused = ACTIVE_GRAND_PAUSED_SPECIALS.get(event.getLevel().dimension());
            if (paused != null) {
                paused.remove(entity.getUUID());
                if (paused.isEmpty()) {
                    ACTIVE_GRAND_PAUSED_SPECIALS.remove(event.getLevel().dimension());
                }
            }
        }
        if (!(entity instanceof Warden warden)) {
            return;
        }
        boolean isGrand = warden.getTags().contains(GRAND_WARDEN_TAG);
        if (!isGrand) {
            return;
        }
        debugLog(
                "GRAND_EVENT warden-leave dim={} pos={} removed={} reason={} alive={}",
                event.getLevel().dimension().location(),
                warden.blockPosition(),
                warden.isRemoved(),
                String.valueOf(warden.getRemovalReason()),
                warden.isAlive());
    }

    private static boolean tryTriggerWorkbenchRejectOnInteract(ServerPlayer player, long now) {
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        if (!isManualEventCooldownReady(player, "workbench_reject", now, COOLDOWN_WORKBENCH_REJECT_SECONDS)) {
            return false;
        }
        int profile = getIntensityProfile();
        double chance = switch (profile) {
            case 1 -> 0.004D;
            case 2 -> 0.006D;
            case 3 -> 0.008D;
            case 4 -> 0.012D;
            default -> 0.017D;
        };
        if (player.getRandom().nextDouble() > chance) {
            return false;
        }
        player.displayClientMessage(WORKBENCH_REJECT_MESSAGE, true);
        applyManualEventCooldown(player, "workbench_reject", now, COOLDOWN_WORKBENCH_REJECT_SECONDS);
        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    private static boolean isContainerInteractionBlock(BlockState state) {
        return state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof AbstractFurnaceBlock;
    }

    private static void tryTriggerFalseContainerOpenOnInteract(ServerPlayer player, BlockState state, BlockPos sourcePos, long now) {
        SoundEvent sourceSound = resolveContainerEchoSound(state);
        LAST_CONTAINER_CONTEXTS.put(
                player.getUUID(),
                new ContainerEchoContext(player.serverLevel().dimension(), sourcePos.immutable(), sourceSound, now));

        long minGap = 12L;
        Long last = LAST_CONTAINER_OPEN_TICKS.get(player.getUUID());
        if (last != null && now - last < minGap) {
            return;
        }
        LAST_CONTAINER_OPEN_TICKS.put(player.getUUID(), now);

        if (!isManualEventCooldownReady(player, "false_container_open", now, COOLDOWN_FALSE_CONTAINER_OPEN_SECONDS)) {
            return;
        }

        int profile = getIntensityProfile();
        double chance = switch (profile) {
            case 1 -> 0.12D;
            case 2 -> 0.15D;
            case 3 -> 0.18D;
            case 4 -> 0.24D;
            default -> 0.30D;
        };
        if (player.getRandom().nextDouble() > chance) {
            return;
        }

        Vec3 look = player.getViewVector(1.0F);
        Vec3 dir = new Vec3(-look.x, 0.0D, -look.z);
        if (dir.lengthSqr() < 0.0001D) {
            dir = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            dir = dir.normalize();
        }
        Vec3 at = player.position().add(dir.scale(2.2D));
        playLocalSoundAt(player, at.x, player.getEyeY() - 0.5D, at.z, sourceSound, SoundSource.BLOCKS, 0.95F, 0.88F + player.getRandom().nextFloat() * 0.2F);
        applyManualEventCooldown(player, "false_container_open", now, COOLDOWN_FALSE_CONTAINER_OPEN_SECONDS);
        debugLog("EVENT false_container_open player={} source={}", playerLabel(player), sourcePos);
    }

    private static boolean tryTriggerLeverAnswerOnInteract(ServerPlayer player, BlockPos sourceLeverPos, long now, boolean enforceCooldown) {
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        if (enforceCooldown && !tryAcquireManualEventCooldown(player, "lever_answer", now, COOLDOWN_LEVER_ANSWER_SECONDS)) {
            return false;
        }
        List<BlockPos> levers = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    BlockPos candidate = sourceLeverPos.offset(dx, dy, dz);
                    if (candidate.equals(sourceLeverPos)) {
                        continue;
                    }
                    if (level.getBlockState(candidate).getBlock() instanceof LeverBlock) {
                        levers.add(candidate.immutable());
                    }
                }
            }
        }
        if (levers.isEmpty()) {
            LEVER_REPLY_TASKS.add(new LeverReplyTask(player.getUUID(), level.dimension(), sourceLeverPos, now + 20L, true));
            debugLog("EVENT lever_answer player={} mode=audio-only source={}", playerLabel(player), sourceLeverPos);
            return true;
        }

        BlockPos target = levers.get(level.random.nextInt(levers.size()));
        LEVER_REPLY_TASKS.add(new LeverReplyTask(player.getUUID(), level.dimension(), target, now + 20L, false));
        debugLog("EVENT lever_answer player={} mode=toggle source={} target={} delay=20t", playerLabel(player), sourceLeverPos, target);
        return true;
    }

    private static void tryTriggerToolAnswerOnBreak(ServerPlayer player, BlockPos minedPos, BlockState minedState, long now) {
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return;
        }
        if (!isManualEventCooldownReady(player, "tool_answer", now, COOLDOWN_TOOL_ANSWER_SECONDS)) {
            return;
        }

        double chance = isStoneFamily(minedState) ? 0.18D : 0.11D;
        chance += (getIntensityProfile() - 1) * 0.02D;
        if (player.getRandom().nextDouble() > chance) {
            return;
        }
        scheduleToolAnswerEchoSequence(player, minedPos, minedState, player.getMainHandItem(), now);
        applyManualEventCooldown(player, "tool_answer", now, COOLDOWN_TOOL_ANSWER_SECONDS);
    }

    private static boolean tryAcquireManualEventCooldown(ServerPlayer player, String eventKey, long now, int seconds) {
        if (!isManualEventCooldownReady(player, eventKey, now, seconds)) {
            return false;
        }
        applyManualEventCooldown(player, eventKey, now, seconds);
        return true;
    }

    private static boolean isManualEventCooldownReady(ServerPlayer player, String eventKey, long now, int seconds) {
        if (seconds <= 0 || eventKey == null || eventKey.isBlank()) {
            return true;
        }
        Map<String, Long> perPlayer = EVENT_COOLDOWNS.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        Long until = perPlayer.get(eventKey);
        if (until != null && now < until) {
            debugLog("EVENT {} skip player={} reason=cooldown remaining={}t", eventKey, playerLabel(player), until - now);
            return false;
        }
        return true;
    }

    private static void applyManualEventCooldown(ServerPlayer player, String eventKey, long now, int seconds) {
        if (seconds <= 0 || eventKey == null || eventKey.isBlank()) {
            return;
        }
        EVENT_COOLDOWNS.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                .put(eventKey, now + seconds * 20L);
    }

    private static String pickCorruptMessageForPlayer(ServerPlayer player) {
        if (player.getServer() == null) {
            return DONT_TURN_AROUND_MESSAGE;
        }

        UncannyPhase phase = UncannyWorldState.get(player.getServer()).getPhase();
        List<String> pool = new ArrayList<>(CORRUPT_MESSAGE_PHASE1_POOL);
        if (isDeepCaveContext(player.serverLevel(), player.blockPosition())) {
            pool.addAll(CORRUPT_MESSAGE_CAVE_POOL);
        }
        if (phase.index() >= UncannyPhase.PHASE_3.index()) {
            pool.addAll(CORRUPT_MESSAGE_PHASE3_POOL);
        }
        if (phase.index() >= UncannyPhase.PHASE_4.index()) {
            pool.addAll(CORRUPT_MESSAGE_PHASE4_POOL);
        }
        if (pool.isEmpty()) {
            return DONT_TURN_AROUND_MESSAGE;
        }
        return pool.get(player.getRandom().nextInt(pool.size()));
    }

    private static boolean isDeepCaveContext(ServerLevel level, BlockPos pos) {
        if (level.canSeeSky(pos)) {
            return false;
        }
        int minY = level.getMinBuildHeight();
        int maxYForDeepCave = Math.min(level.getSeaLevel() - 20, minY + 56);
        return pos.getY() <= maxYForDeepCave;
    }

    private static List<String> parseMessageLines(String block) {
        List<String> lines = new ArrayList<>();
        if (block == null || block.isBlank()) {
            return lines;
        }
        for (String raw : block.split("\\R")) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return List.copyOf(lines);
    }

    private static boolean holdsCompass(ServerPlayer player) {
        return player.getMainHandItem().is(Items.COMPASS)
                || player.getOffhandItem().is(Items.COMPASS)
                || player.getMainHandItem().is(UncannyItemRegistry.UNCANNY_COMPASS.get())
                || player.getOffhandItem().is(UncannyItemRegistry.UNCANNY_COMPASS.get());
    }

    private static boolean holdsEmptyBucket(ServerPlayer player) {
        return player.getMainHandItem().is(Items.BUCKET) || player.getOffhandItem().is(Items.BUCKET);
    }

    private static boolean isPlayerUsingContainerMenu(ServerPlayer player) {
        return player.containerMenu != null && player.containerMenu != player.inventoryMenu;
    }

    private static boolean isPlayerUsingWorkbenchMenu(ServerPlayer player) {
        return player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu;
    }

    private static SoundEvent resolveContainerEchoSound(BlockState state) {
        if (state.getBlock() instanceof AbstractFurnaceBlock) {
            return SoundEvents.FURNACE_FIRE_CRACKLE;
        }
        if (state.is(Blocks.BARREL)) {
            return SoundEvents.BARREL_OPEN;
        }
        return SoundEvents.CHEST_OPEN;
    }

    private static void playToolAnswerEcho(ServerPlayer player, BlockPos minedPos, BlockState minedState, ItemStack toolStack) {
        ServerLevel level = player.serverLevel();
        BlockPos adjacent = minedPos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(player.getRandom()));
        BlockPos soundPos = level.getBlockState(adjacent).isAir() ? minedPos : adjacent;
        SoundEvent sound = minedState.getSoundType().getHitSound();
        float basePitch = 0.82F + player.getRandom().nextFloat() * 0.14F;
        float toolPitch = computeToolAnswerPitch(toolStack);
        playLocalSoundAt(player, soundPos, sound, SoundSource.BLOCKS, 0.95F, Mth.clamp(basePitch * toolPitch, 0.45F, 1.35F));
    }

    private static void scheduleToolAnswerEchoSequence(ServerPlayer player, BlockPos minedPos, BlockState minedState, ItemStack toolStack, long now) {
        ItemStack copiedTool = toolStack == null ? ItemStack.EMPTY : toolStack.copy();
        TOOL_ANSWER_ECHO_TASKS.add(new ToolAnswerEchoTask(player.getUUID(), player.serverLevel().dimension(), minedPos, minedState, copiedTool, now));
        TOOL_ANSWER_ECHO_TASKS.add(new ToolAnswerEchoTask(player.getUUID(), player.serverLevel().dimension(), minedPos, minedState, copiedTool, now + 20L));
        TOOL_ANSWER_ECHO_TASKS.add(new ToolAnswerEchoTask(player.getUUID(), player.serverLevel().dimension(), minedPos, minedState, copiedTool, now + 40L));
    }

    private static float computeToolAnswerPitch(ItemStack toolStack) {
        if (toolStack == null || toolStack.isEmpty()) {
            return 1.0F;
        }
        if (toolStack.is(Items.NETHERITE_PICKAXE) || toolStack.is(Items.NETHERITE_AXE) || toolStack.is(Items.NETHERITE_SHOVEL)) {
            return 0.86F;
        }
        if (toolStack.is(Items.DIAMOND_PICKAXE) || toolStack.is(Items.DIAMOND_AXE) || toolStack.is(Items.DIAMOND_SHOVEL)) {
            return 0.90F;
        }
        if (toolStack.is(Items.IRON_PICKAXE) || toolStack.is(Items.IRON_AXE) || toolStack.is(Items.IRON_SHOVEL)) {
            return 0.95F;
        }
        if (toolStack.is(Items.GOLDEN_PICKAXE) || toolStack.is(Items.GOLDEN_AXE) || toolStack.is(Items.GOLDEN_SHOVEL)) {
            return 1.10F;
        }
        if (toolStack.is(Items.STONE_PICKAXE) || toolStack.is(Items.STONE_AXE) || toolStack.is(Items.STONE_SHOVEL)) {
            return 0.98F;
        }
        if (toolStack.is(Items.WOODEN_PICKAXE) || toolStack.is(Items.WOODEN_AXE) || toolStack.is(Items.WOODEN_SHOVEL)) {
            return 1.03F;
        }
        return 1.0F;
    }

    private static int findRandomNonEmptyHotbarSlot(ServerPlayer player) {
        int selected = Mth.clamp(player.getInventory().selected, 0, 8);
        ItemStack selectedStack = player.getInventory().getItem(selected);
        if (!selectedStack.isEmpty()) {
            return selected;
        }

        List<Integer> visibleCountSlots = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                slots.add(slot);
                if (stack.getCount() > 1) {
                    visibleCountSlots.add(slot);
                }
            }
        }
        if (!visibleCountSlots.isEmpty()) {
            return visibleCountSlots.get(player.getRandom().nextInt(visibleCountSlots.size()));
        }
        if (slots.isEmpty()) {
            return -1;
        }
        return slots.get(player.getRandom().nextInt(slots.size()));
    }

    private static boolean isStoneFamily(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT);
    }

    private static BlockPos findNearestDoor(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> doors = findNearbyDoors(level, center, radius);
        if (doors.isEmpty()) {
            return null;
        }
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos door : doors) {
            double dist = door.distSqr(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = door;
            }
        }
        return nearest;
    }

    private static List<MobSnapshot> collectAnimalStareTargets(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB box = new AABB(player.blockPosition()).inflate(64.0D, 24.0D, 64.0D);
        List<MobSnapshot> snapshots = new ArrayList<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.isAlive() || mob.isRemoved()) {
                continue;
            }
            if (mob.isPassenger() || mob.isVehicle()) {
                continue;
            }
            if (UncannyEntityRegistry.isSpecialEntity(mob.getType())) {
                continue;
            }
            if (mob instanceof Monster monster) {
                if (monster.getTarget() != null || monster.getLastHurtByMob() != null) {
                    continue;
                }
                continue;
            }
            boolean allowed = mob instanceof Animal || (mob instanceof TamableAnimal tamable && tamable.isTame());
            if (!allowed) {
                continue;
            }
            snapshots.add(new MobSnapshot(mob.getUUID(), mob.isNoAi()));
        }
        return snapshots;
    }

    private static BlockPos findNearestLorePriorityStructure(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        BlockPos from = player.blockPosition();
        String dimensionId = normalizeDimensionId(dimension);
        if (dimensionId == null) {
            return null;
        }
        List<String> priorities = List.of("false_ascent_house", "false_descent_house", "secret_house");
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (UncannyWorldState.StructureMarker marker : state.getStructureMarkers()) {
            if (!dimensionId.equals(marker.dimension())) {
                continue;
            }
            if (!priorities.contains(marker.type())) {
                continue;
            }
            BlockPos pos = BlockPos.of(marker.posLong());
            double dist = pos.distSqr(from);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pos;
            }
        }
        if (nearest != null) {
            return nearest;
        }
        ServerLevel level = player.serverLevel();
        for (String id : priorities) {
            BlockPos candidate = UncannyStructureFeatureSystem.findNearestPlannedStructure(level, id, from);
            if (candidate == null) {
                continue;
            }
            double dist = candidate.distSqr(from);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = candidate.immutable();
            }
        }
        return nearest;
    }

    private static BlockPos findNearestLorePriorityMarker(ServerPlayer player, int maxRadius) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        String dimensionId = normalizeDimensionId(player.serverLevel().dimension());
        if (dimensionId == null) {
            return null;
        }
        BlockPos from = player.blockPosition();
        long maxDistSqr = (long) maxRadius * maxRadius;
        List<String> priorities = List.of("false_ascent_house", "false_descent_house", "secret_house");
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (UncannyWorldState.StructureMarker marker : state.getStructureMarkers()) {
            if (!dimensionId.equals(marker.dimension()) || !priorities.contains(marker.type())) {
                continue;
            }
            BlockPos pos = BlockPos.of(marker.posLong());
            double dist = pos.distSqr(from);
            if (dist > maxDistSqr || dist >= nearestDist) {
                continue;
            }
            nearestDist = dist;
            nearest = pos;
        }
        return nearest;
    }

    private static BlockPos findNearestAnyUncannyStructure(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        BlockPos from = player.blockPosition();
        String dimensionId = normalizeDimensionId(dimension);
        if (dimensionId == null) {
            return null;
        }
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (UncannyWorldState.StructureMarker marker : state.getStructureMarkers()) {
            if (!dimensionId.equals(marker.dimension())) {
                continue;
            }
            BlockPos pos = BlockPos.of(marker.posLong());
            double dist = pos.distSqr(from);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pos;
            }
        }
        if (nearest != null) {
            return nearest;
        }
        ServerLevel level = player.serverLevel();
        List<String> features = List.of(
                "mimic_shelter",
                "glitched_shelter",
                "anechoic_cube",
                "isolation_cube",
                "false_descent",
                "false_ascent",
                "bell_shrine",
                "watching_tower",
                "false_camp",
                "wrong_village_house",
                "wrong_village_utility",
                "sinkhole",
                "observation_platform",
                "wrong_road_segment",
                "false_entrance",
                "storage_shed",
                "secret_house");
        for (String featureId : features) {
            BlockPos candidate = UncannyStructureFeatureSystem.findNearestPlannedStructure(level, featureId, from);
            if (candidate == null) {
                continue;
            }
            double dist = candidate.distSqr(from);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = candidate.immutable();
            }
        }
        return nearest;
    }

    private static String normalizeDimensionId(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return null;
        }
        return dimension.location().toString();
    }

    private static void applyUncannyCompassTarget(ItemStack stack, ServerLevel level, BlockPos target) {
        if (stack == null || stack.isEmpty() || !isTrackableCompassItem(stack) || target == null) {
            return;
        }
        LodestoneTracker tracker = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), target.immutable())), false);
        stack.set(DataComponents.LODESTONE_TRACKER, tracker);
    }

    private static void clearUncannyCompassTarget(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isTrackableCompassItem(stack)) {
            return;
        }
        stack.remove(DataComponents.LODESTONE_TRACKER);
    }

    private static boolean isTrackableCompassItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(UncannyItemRegistry.UNCANNY_COMPASS.get())) {
            return true;
        }
        if (!stack.is(Items.COMPASS) || !stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
        }
        return "Uncanny Compass".equals(stack.getHoverName().getString());
    }

    private static boolean isConsumableGuideCompass(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(UncannyItemRegistry.UNCANNY_COMPASS.get())) {
            return true;
        }
        if (!stack.is(Items.COMPASS) || !stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
        }
        return "Uncanny Compass".equals(stack.getHoverName().getString());
    }

    private static BlockPos findBestFurnaceBreathTarget(ServerLevel level, ServerPlayer player) {
        List<BlockPos> hidden = new ArrayList<>();
        List<BlockPos> visible = new ArrayList<>();
        BlockPos center = player.blockPosition();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (!(state.getBlock() instanceof AbstractFurnaceBlock)) {
                        continue;
                    }
                    if (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT)) {
                        continue;
                    }
                    if (isBlockInPlayerView(level, player, candidate)) {
                        visible.add(candidate.immutable());
                    } else {
                        hidden.add(candidate.immutable());
                    }
                }
            }
        }
        List<BlockPos> pool = hidden.isEmpty() ? visible : hidden;
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(level.random.nextInt(pool.size()));
    }

    private static MisplacedLightState tryCreateMisplacedLightState(ServerLevel level, ServerPlayer player) {
        List<BlockPos> torches = new ArrayList<>();
        List<BlockPos> wallTorches = new ArrayList<>();
        BlockPos center = player.blockPosition();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.REDSTONE_TORCH)) {
                        torches.add(pos.immutable());
                    } else if (state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) {
                        wallTorches.add(pos.immutable());
                    }
                }
            }
        }

        List<BlockPos> ordered = new ArrayList<>(torches);
        ordered.addAll(wallTorches);
        if (ordered.isEmpty()) {
            return null;
        }

        Collections.shuffle(ordered, new java.util.Random(level.random.nextLong()));
        for (BlockPos original : ordered) {
            BlockState originalState = level.getBlockState(original);
            List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
            Collections.shuffle(directions, new java.util.Random(level.random.nextLong()));
            for (Direction direction : directions) {
                if (direction == Direction.DOWN) {
                    continue;
                }
                BlockPos movedPos = original.relative(direction);
                List<BlockState> candidates = toMovableTorchStates(originalState);
                for (BlockState movedState : candidates) {
                    if (!canMoveLight(level, movedPos, movedState)) {
                        continue;
                    }
                    level.setBlock(original, Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(movedPos, movedState, 3);
                    long autoRevertTick = player.getServer().getTickCount() + 15L * 20L;
                    return new MisplacedLightState(original, originalState, movedPos, movedState, autoRevertTick);
                }
            }
        }
        return null;
    }

    private static List<BlockState> toMovableTorchStates(BlockState source) {
        List<BlockState> result = new ArrayList<>();
        if (source.is(Blocks.TORCH) || source.is(Blocks.WALL_TORCH)) {
            result.add(Blocks.TORCH.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                result.add(Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction));
            }
            return result;
        }
        if (source.is(Blocks.SOUL_TORCH) || source.is(Blocks.SOUL_WALL_TORCH)) {
            result.add(Blocks.SOUL_TORCH.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                result.add(Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction));
            }
            return result;
        }
        if (source.is(Blocks.REDSTONE_TORCH) || source.is(Blocks.REDSTONE_WALL_TORCH)) {
            result.add(Blocks.REDSTONE_TORCH.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                result.add(Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction));
            }
        }
        return result;
    }

    private static boolean canMoveLight(ServerLevel level, BlockPos movedPos, BlockState movedState) {
        if (movedPos.getY() <= level.getMinBuildHeight() + 1 || movedPos.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockState targetState = level.getBlockState(movedPos);
        if (!targetState.isAir() || !targetState.getFluidState().isEmpty()) {
            return false;
        }
        if (level.getFluidState(movedPos).isSource()) {
            return false;
        }
        return movedState.canSurvive(level, movedPos);
    }

    private static Mob findNearestTamedPet(ServerLevel level, BlockPos origin, int radius) {
        AABB box = new AABB(origin).inflate(radius, 6.0D, radius);
        Mob nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            if (!(mob instanceof TamableAnimal tamable) || !tamable.isTame()) {
                continue;
            }
            if (!isSupportedPetRefusalTarget(mob)) {
                continue;
            }
            if (!mob.isAlive()) {
                continue;
            }
            double dist = mob.blockPosition().distSqr(origin);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = mob;
            }
        }
        return nearest;
    }

    private static boolean isSupportedPetRefusalTarget(Mob mob) {
        return mob instanceof Wolf || mob instanceof Cat;
    }

    private static boolean triggerPressurePlateReplyNow(ServerPlayer player, BlockPos source, long now, boolean applyCooldown) {
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        if (applyCooldown && !tryAcquireManualEventCooldown(player, "pressure_plate_reply", now, COOLDOWN_PRESSURE_PLATE_REPLY_SECONDS)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        List<BlockPos> plates = new ArrayList<>();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos candidate = source.offset(dx, dy, dz);
                    if (candidate.equals(source)) {
                        continue;
                    }
                    if (level.getBlockState(candidate).getBlock() instanceof PressurePlateBlock) {
                        plates.add(candidate.immutable());
                    }
                }
            }
        }
        if (plates.isEmpty()) {
            Vec3 look = player.getViewVector(1.0F).normalize();
            Vec3 at = player.position().add(new Vec3(-look.x, 0.0D, -look.z).normalize().scale(2.2D));
            playLocalSoundAt(player, at.x, player.getEyeY() - 0.6D, at.z, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.9F, 1.0F);
            if (applyCooldown) {
                markGlobalCooldown(player, now, EventSeverity.LIGHT);
            }
            return true;
        }

        BlockPos target = plates.get(level.random.nextInt(plates.size()));
        PRESSURE_PLATE_REPLY_TASKS.add(new PressurePlateReplyTask(player.getUUID(), level.dimension(), target, now + 12L, false));
        if (applyCooldown) {
            markGlobalCooldown(player, now, EventSeverity.LIGHT);
        }
        return true;
    }

    private static boolean isBlockInPlayerView(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 to = center.subtract(eye);
        if (to.lengthSqr() < 0.0001D) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        double dot = look.dot(to.normalize());
        if (dot < 0.45D) {
            return false;
        }
        BlockHitResult hit = level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    public static boolean triggerTotalBlackout(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        int duration = 260 + player.getRandom().nextInt(121);
        ACTIVE_BLACKOUTS.put(player.getUUID(), new BlackoutState(now, duration));

        playLocalSound(player, SoundEvents.MUSIC_DISC_11.value(), SoundSource.RECORDS, 1.25F, 1.0F);
        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static void applyTemporaryDeafness(ServerPlayer player, int durationTicks) {
        if (player.getServer() == null) {
            return;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return;
        }
        long endTick = player.getServer().getTickCount() + Math.max(1, durationTicks);
        ACTIVE_DEAFNESS.merge(player.getUUID(), endTick, Math::max);
    }

    public static boolean triggerFootstepsBehind(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        UncannyPhase phase = UncannyWorldState.get(player.getServer()).getPhase();
        FootstepPattern pattern = rollFootstepPattern(player.serverLevel(), phase, getIntensityProfile());
        if (findNearbyBlock(player.serverLevel(), player.blockPosition(), 12, state -> state.getBlock() instanceof LadderBlock) != null
                && player.getRandom().nextDouble() < 0.14D) {
            pattern = FootstepPattern.LADDER_STEPS;
        }
        return triggerFootstepsPattern(player, pattern, now, true);
    }

    private static FootstepPattern rollFootstepPattern(ServerLevel level, UncannyPhase phase, int profile) {
        int roll = level.random.nextInt(100);
        int heavyWeight = switch (phase) {
            case PHASE_1 -> 8;
            case PHASE_2 -> 12;
            case PHASE_3 -> 16;
            case PHASE_4 -> 20;
        } + (profile - 1) * 3;
        int sprintWeight = switch (phase) {
            case PHASE_1 -> 16;
            case PHASE_2 -> 21;
            case PHASE_3 -> 25;
            case PHASE_4 -> 29;
        } + (profile - 1) * 2;

        if (roll < heavyWeight) {
            return FootstepPattern.HEAVY;
        }
        if (roll < heavyWeight + sprintWeight) {
            return FootstepPattern.SPRINT;
        }
        if (roll < heavyWeight + sprintWeight + 24) {
            return FootstepPattern.ECHO;
        }
        return FootstepPattern.BASIC;
    }

    private static boolean triggerFootstepsPattern(ServerPlayer player, FootstepPattern pattern, long now, boolean applyCooldown) {
        FootstepsState state;
        switch (pattern) {
            case ECHO -> {
                BlockPos samplePos = findGroundBlock(player.serverLevel(), player.blockPosition().below());
                state = new FootstepsState(pattern, now + 40L, now + 10L + player.getRandom().nextInt(11), 0.0D, samplePos.immutable());
            }
            case SPRINT -> state = new FootstepsState(pattern, now + 140L, now + 6L, 15.0D, BlockPos.ZERO);
            case HEAVY -> state = new FootstepsState(pattern, now + 140L, now + 8L, 0.0D, BlockPos.ZERO);
            case LADDER_STEPS -> {
                BlockPos ladder = findNearbyBlock(player.serverLevel(), player.blockPosition(), 12, s -> s.getBlock() instanceof LadderBlock);
                if (ladder == null) {
                    return false;
                }
                state = new FootstepsState(pattern, now + 100L, now + 6L, 0.0D, ladder.immutable());
            }
            default -> state = new FootstepsState(pattern, now + 120L, now, 0.0D, BlockPos.ZERO);
        }

        ACTIVE_FOOTSTEPS.put(player.getUUID(), state);
        if (applyCooldown) {
            markGlobalCooldown(player, now, pattern == FootstepPattern.HEAVY ? EventSeverity.MEDIUM : EventSeverity.LIGHT);
        }
        return true;
    }

    public static boolean triggerFlashError(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        int danger = getDangerLevel();
        long now = player.getServer().getTickCount();
        playLocalSound(player, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 2.25F, 0.55F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, true));
        boolean shouldSpawnMonster = player.serverLevel().random.nextDouble() < DANGER_FLASH_MONSTER_CHANCE[danger];
        ACTIVE_FLASH_EVENTS.put(player.getUUID(), new FlashErrorState(now + 10L, shouldSpawnMonster));
        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerBaseReplay(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos baseCenter = resolveBaseCenter(player, player.getServer());
        long now = player.getServer().getTickCount();

        List<String> options = new ArrayList<>();

        List<BlockPos> chests = findNearbyChests(level, baseCenter, 10);
        BlockPos craftingPos = findNearbyBlock(level, baseCenter, 10, state -> state.is(Blocks.CRAFTING_TABLE));
        BlockPos anvilPos = findNearbyBlock(level, baseCenter, 10, state ->
                state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL));
        BlockPos enchantingPos = findNearbyBlock(level, baseCenter, 10, state -> state.is(Blocks.ENCHANTING_TABLE));
        BlockPos emptyFurnace = findNearbyEmptyFurnace(level, baseCenter, 10);
        List<BlockPos> torches = findNearbyTorches(level, baseCenter, 10);

        if (!chests.isEmpty()) {
            options.add("chest");
        }
        if (craftingPos != null) {
            options.add("crafting");
        }
        if (anvilPos != null) {
            options.add("anvil");
        }
        if (enchantingPos != null) {
            options.add("enchant");
        }
        if (emptyFurnace != null) {
            options.add("furnace");
        }
        if (!chests.isEmpty()) {
            options.add("chest_panic");
        }
        if (craftingPos != null || anvilPos != null) {
            options.add("artisan_fail");
        }
        if (!torches.isEmpty()) {
            options.add("torch_dying");
        }
        if (options.isEmpty()) {
            return false;
        }

        String selected = options.get(level.random.nextInt(options.size()));
        switch (selected) {
            case "crafting" -> playEventSoundAt(player, level, craftingPos, Blocks.CRAFTING_TABLE.defaultBlockState().getSoundType().getHitSound(), 0.95F, 0.8F, 1.1F);
            case "anvil" -> playLocalSoundAt(player, anvilPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.12F, 0.92F);
            case "enchant" -> playLocalSoundAt(player, enchantingPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.95F, 1.0F);
            case "furnace" -> lightFurnaceTemporarily(player, level, emptyFurnace, now + 20L);
            case "chest_panic" -> triggerPanickedChest(player, level, chests.get(level.random.nextInt(chests.size())), now);
            case "artisan_fail" -> triggerArtisanFail(player, level, craftingPos, anvilPos);
            case "torch_dying" -> triggerTorchDying(player, level, torches);
            default -> triggerChestReplay(player, level, baseCenter, now);
        }

        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerBell(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        playLocalSound(player, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.65F, 0.88F);

        double waveChance = PROFILE_BELL_WAVE_CHANCE[profile - 1];
        if (danger <= 1) {
            waveChance *= danger == 0 ? 0.12D : 0.32D;
        } else if (danger == 2) {
            waveChance *= 0.65D;
        } else if (danger == 4) {
            waveChance *= 1.20D;
        } else if (danger == 5) {
            waveChance *= 1.40D;
        }
        waveChance = Mth.clamp(waveChance, 0.0D, 0.98D);

        boolean spawnWave = level.random.nextDouble() < waveChance;
        if (spawnWave) {
            int count = switch (profile) {
                case 1, 2 -> 1 + level.random.nextInt(2);
                case 3 -> 2 + level.random.nextInt(2);
                case 4 -> 2 + level.random.nextInt(3);
                default -> 3 + level.random.nextInt(2);
            };
            if (danger <= 1) {
                count = Math.max(1, count - (danger == 0 ? 1 : 0));
            } else if (danger == 4) {
                count += 1;
            } else if (danger == 5) {
                count += 2;
            }
            for (int i = 0; i < count; i++) {
                spawnBellUncannyAttacker(player);
            }
        }

        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerFlashRed(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        player.addTag(FLASH_RED_OVERLAY_TAG);
        FLASH_RED_OVERLAY_END_TICKS.merge(player.getUUID(), now + 4L, Math::max);
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6, FLASH_RED_MARKER_AMPLIFIER, false, false, false));
        applyGuaranteedDamageFlash(player);
        player.serverLevel().broadcastDamageEvent(player, player.damageSources().generic());
        player.connection.send(new ClientboundHurtAnimationPacket(player));
        playLocalSound(player, UncannySoundRegistry.UNCANNY_HEARTBEAT.get(), SoundSource.HOSTILE, 2.2F, 0.82F);
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerVoidSilence(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        int durationTicks = 300 + player.getRandom().nextInt(301);
        applyTemporaryDeafness(player, durationTicks);
        ACTIVE_VOID_SILENCE.put(player.getUUID(), new VoidSilenceState(now + durationTicks, now + 30L + player.getRandom().nextInt(50)));
        stopAllSoundsForPlayer(player);
        playCustomEventSound(player, player.serverLevel(), player.blockPosition(), UncannySoundRegistry.UNCANNY_TINNITUS.get(), 0.20F, 0.95F, 1.05F);
        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerFalseFall(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 5, false, false, true));
        playLocalSound(player, SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 1.45F, 0.8F);
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerFalseInjury(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        player.serverLevel().broadcastDamageEvent(player, player.damageSources().generic());
        player.connection.send(new ClientboundHurtAnimationPacket(player));
        playLocalSound(player, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.25F, 0.9F);
        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerForcedDrop(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        ItemStack dropped = held.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.drop(dropped, false);
        playCustomEventSound(player, player.serverLevel(), player.blockPosition(), UncannySoundRegistry.UNCANNY_SCARY_LAUGH.get(), 0.35F, 0.88F, 1.0F);
        markGlobalCooldown(player, now, EventSeverity.EXTREME);
        return true;
    }

    public static boolean triggerCorruptedMessage(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        long now = player.getServer().getTickCount();
        List<? extends String> configuredColors = UncannyConfig.CORRUPT_MESSAGE_COLORS.get();

        String text = pickCorruptMessageForPlayer(player);

        String colorName = configuredColors.isEmpty()
                ? "dark_red"
                : configuredColors.get(player.getRandom().nextInt(configuredColors.size()));

        ChatFormatting color = parseChatFormatting(colorName);
        MutableComponent message = Component.literal(text).withStyle(color);

        double glitchChance = Math.min(UncannyConfig.CORRUPT_MESSAGE_GLITCH_CHANCE.get(), 1.0D / 500.0D);
        if (player.getRandom().nextDouble() < glitchChance) {
            message = Component.literal(text).withStyle(style -> style.withColor(color).withObfuscated(true));
        }

        player.sendSystemMessage(message);
        if (DONT_TURN_AROUND_MESSAGE.equalsIgnoreCase(text) || "Turn around.".equalsIgnoreCase(text)) {
            Vec3 look = player.getLookAngle().normalize();
            ACTIVE_TURN_AROUND_TRAPS.put(player.getUUID(), new TurnAroundTrapState(now + 40L, look));
        }
        markGlobalCooldown(player, now, EventSeverity.LIGHT);
        return true;
    }

    public static boolean triggerGhostMiner(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (player.serverLevel().canSeeSky(player.blockPosition())) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos start = findGhostMinerStartPos(level, player);
        if (start == null) {
            start = findGhostMinerStrikePos(level, player);
        }
        if (start == null) {
            return false;
        }

        long now = player.getServer().getTickCount();
        int duration = 300 + player.getRandom().nextInt(401);
        double startRadius = horizontalDistance(start, player.blockPosition());
        ACTIVE_GHOST_MINERS.put(
                player.getUUID(),
                new GhostMinerState(
                        start,
                        now + duration,
                        now + 6L,
                        -1L,
                        start,
                        player.getRandom().nextFloat() * 360.0F,
                        Math.max(5.0D, Math.min(18.0D, startRadius))));
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerCaveCollapse(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        if (!UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (player.serverLevel().canSeeSky(player.blockPosition())) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        BlockPos start = player.blockPosition().above(2);
        BlockPos ceiling = null;
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos candidate = start.above(dy);
            if (!level.getBlockState(candidate).isAir()) {
                ceiling = candidate.immutable();
                break;
            }
        }

        BlockPos source = ceiling != null ? ceiling : player.blockPosition().above(4);
        playLocalSoundAt(player, source, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.55F, 0.85F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()),
                source.getX() + 0.5D, source.getY() - 0.1D, source.getZ() + 0.5D,
                26, 0.8D, 0.2D, 0.8D, 0.02D);
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerAsphyxia(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        int danger = getDangerLevel();
        AsphyxiaVariant variant;
        int roll = player.getRandom().nextInt(100);
        if (danger <= 1) {
            variant = AsphyxiaVariant.FALSE_ALERT;
        } else if (danger >= 4 && roll < 35) {
            variant = AsphyxiaVariant.TERRAIN_DROWNING;
        } else if (danger >= 3 && roll < 70) {
            variant = AsphyxiaVariant.HEAVY_LUNGS;
        } else {
            variant = AsphyxiaVariant.FALSE_ALERT;
        }
        return triggerAsphyxia(player, variant, true);
    }

    public static boolean triggerAsphyxiaVariant(ServerPlayer player, AsphyxiaVariant variant) {
        if (variant == null) {
            return false;
        }
        return triggerAsphyxia(player, variant, true);
    }

    private static boolean triggerAsphyxia(ServerPlayer player, AsphyxiaVariant variant, boolean applyCooldown) {
        long now = player.getServer().getTickCount();
        int duration = 80 + player.getRandom().nextInt(50);
        ACTIVE_ASPHYXIA.put(player.getUUID(), new AsphyxiaState(now, now + duration, variant, false));
        playLocalSound(player, SoundEvents.DROWNED_AMBIENT_WATER, SoundSource.HOSTILE, 0.9F, 0.75F);
        if (applyCooldown) {
            markGlobalCooldown(player, now, variant == AsphyxiaVariant.FALSE_ALERT ? EventSeverity.MEDIUM : EventSeverity.HIGH);
        }
        return true;
    }

    public static boolean triggerArmorBreak(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (findBreakableArmorSlot(player) == null) {
            return false;
        }
        int danger = getDangerLevel();
        int roll = player.getRandom().nextInt(100);
        ArmorBreakVariant variant;
        if (danger <= 1) {
            variant = ArmorBreakVariant.GHOST_SOUND;
        } else if (roll < 35) {
            variant = ArmorBreakVariant.DROP_GEAR;
        } else if (roll < 70) {
            variant = ArmorBreakVariant.CRACKED_DEFENSE;
        } else {
            variant = ArmorBreakVariant.GHOST_SOUND;
        }
        return triggerArmorBreak(player, variant, true);
    }

    public static boolean triggerArmorBreakVariant(ServerPlayer player, ArmorBreakVariant variant) {
        if (variant == null) {
            return false;
        }
        if (findBreakableArmorSlot(player) == null) {
            return false;
        }
        return triggerArmorBreak(player, variant, true);
    }

    private static boolean triggerArmorBreak(ServerPlayer player, ArmorBreakVariant variant, boolean applyCooldown) {
        long now = player.getServer().getTickCount();
        EquipmentSlot armorSlot = findBreakableArmorSlot(player);
        if (armorSlot == null) {
            return false;
        }

        playLocalSound(player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.25F, 0.7F);
        if (variant == ArmorBreakVariant.DROP_GEAR) {
            ItemStack stack = player.getItemBySlot(armorSlot);
            if (!stack.isEmpty()) {
                ItemStack dropped = stack.copy();
                player.setItemSlot(armorSlot, ItemStack.EMPTY);
                player.drop(dropped, false);
            }
        } else if (variant == ArmorBreakVariant.CRACKED_DEFENSE) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10, 0, false, true, true));
        }

        if (applyCooldown) {
            markGlobalCooldown(player, now, variant == ArmorBreakVariant.GHOST_SOUND ? EventSeverity.LIGHT : EventSeverity.HIGH);
        }
        return true;
    }

    private static EquipmentSlot findBreakableArmorSlot(ServerPlayer player) {
        EquipmentSlot[] priority = {
                EquipmentSlot.CHEST,
                EquipmentSlot.HEAD,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : priority) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ArmorItem && stack.isDamageableItem()) {
                return slot;
            }
        }
        return null;
    }

    public static boolean triggerAquaticSteps(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        int danger = getDangerLevel();
        int roll = player.getRandom().nextInt(100);
        AquaticStepsVariant variant;
        if (danger <= 1) {
            variant = AquaticStepsVariant.FOLLOWER;
        } else if (roll < 30) {
            variant = AquaticStepsVariant.SLIPPERY_AMBUSH;
        } else if (roll < 60) {
            variant = AquaticStepsVariant.INVISIBLE_BITE;
        } else {
            variant = AquaticStepsVariant.FOLLOWER;
        }
        return triggerAquaticSteps(player, variant, true);
    }

    public static boolean triggerAquaticStepsVariant(ServerPlayer player, AquaticStepsVariant variant) {
        if (variant == null) {
            return false;
        }
        return triggerAquaticSteps(player, variant, true);
    }

    private static boolean triggerAquaticSteps(ServerPlayer player, AquaticStepsVariant variant, boolean applyCooldown) {
        long now = player.getServer().getTickCount();
        ServerLevel level = player.serverLevel();
        if (variant == AquaticStepsVariant.FOLLOWER) {
            ACTIVE_FOOTSTEPS.put(player.getUUID(),
                    new FootstepsState(FootstepPattern.BASIC, now + 42L, now, 0.0D, player.blockPosition()));
            playLocalSound(player, SoundEvents.DROWNED_SWIM, SoundSource.HOSTILE, 0.85F, 0.9F);
        } else if (variant == AquaticStepsVariant.SLIPPERY_AMBUSH) {
            BlockPos target = player.blockPosition().below();
            BlockState original = level.getBlockState(target);
            if (!canApplySlipperyAmbush(level, target, original)) {
                debugLog("AQUATIC_STEPS slippery-ambush reroll player={} pos={} block={}", playerLabel(player), target, original.getBlock());
                return false;
            }
            level.setBlock(target, Blocks.WATER.defaultBlockState(), 3);
            WATER_RESTORE_TASKS.add(new WaterRestoreTask(level.dimension(), target.immutable(), original, now + 20L));
            playLocalSound(player, SoundEvents.DROWNED_SWIM, SoundSource.HOSTILE, 1.1F, 0.8F);
        } else {
            Vec3 look = player.getViewVector(1.0F);
            Vec3 behind = new Vec3(-look.x, 0.0D, -look.z);
            if (behind.lengthSqr() < 0.0001D) {
                behind = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                behind = behind.normalize();
            }
            BlockPos source = BlockPos.containing(player.getX() + behind.x * 2.0D, player.getY(), player.getZ() + behind.z * 2.0D);
            ACTIVE_AQUATIC_BITE.put(player.getUUID(), new AquaticBiteState(now, now + 45L, source, now + 10L));
            playLocalSoundAt(player, source, SoundEvents.DROWNED_SWIM, SoundSource.HOSTILE, 1.05F, 0.75F);
        }

        if (applyCooldown) {
            markGlobalCooldown(player, now, variant == AquaticStepsVariant.FOLLOWER ? EventSeverity.MEDIUM : EventSeverity.HIGH);
        }
        return true;
    }

    private static boolean canApplySlipperyAmbush(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getFluidState().isSource()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.ENDER_CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(UncannyBlockRegistry.UNCANNY_ALTAR.get())
                || state.is(BlockTags.BEDS)
                || state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof AbstractFurnaceBlock) {
            return false;
        }
        return true;
    }

    public static boolean triggerDoorInversion(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        List<BlockPos> doors = findNearbyDoors(level, player.blockPosition(), 10);
        if (doors.isEmpty()) {
            return false;
        }

        int danger = getDangerLevel();
        int roll = player.getRandom().nextInt(100);
        DoorInversionVariant variant;
        if (danger <= 1) {
            variant = DoorInversionVariant.POLTERGEIST;
        } else if (roll < 30) {
            variant = DoorInversionVariant.LOCKDOWN;
        } else if (roll < 56) {
            variant = DoorInversionVariant.INTRUSION;
        } else if (roll < 73 && doors.size() >= 2) {
            variant = DoorInversionVariant.DOOR_TRAP_CASCADE;
        } else {
            variant = DoorInversionVariant.POLTERGEIST;
        }
        return triggerDoorInversion(player, doors, variant, true);
    }

    public static boolean triggerDoorInversionVariant(ServerPlayer player, DoorInversionVariant variant) {
        if (variant == null || player.getServer() == null) {
            return false;
        }
        List<BlockPos> doors = findNearbyDoors(player.serverLevel(), player.blockPosition(), 10);
        if (doors.isEmpty()) {
            return false;
        }
        return triggerDoorInversion(player, doors, variant, true);
    }

    private static boolean triggerDoorInversion(
            ServerPlayer player,
            List<BlockPos> doors,
            DoorInversionVariant variant,
            boolean applyCooldown) {
        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        switch (variant) {
            case POLTERGEIST -> {
                for (BlockPos doorPos : doors) {
                    toggleDoorOrTrap(player, level, doorPos);
                }
            }
            case LOCKDOWN -> {
                for (BlockPos doorPos : doors) {
                    closeDoor(player, level, doorPos);
                    lockDoor(level.dimension(), doorPos, now + DOOR_LOCK_SECONDS * 20L);
                }
            }
            case INTRUSION -> {
                BlockPos doorPos = doors.get(level.random.nextInt(doors.size()));
                level.destroyBlock(doorPos, true);
                playLocalSoundAt(player, doorPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.35F, 0.9F);
            }
            case DOOR_TRAP_CASCADE -> {
                List<BlockPos> sequence = new ArrayList<>(doors.size());
                sequence.addAll(doors);
                sequence.sort((a, b) -> Double.compare(a.distSqr(player.blockPosition()), b.distSqr(player.blockPosition())));
                DOOR_CASCADE_TASKS.add(new DoorCascadeTask(player.getUUID(), level.dimension(), sequence, now + 6L, 0));
            }
        }

        if (applyCooldown) {
            markGlobalCooldown(
                    player,
                    now,
                    (variant == DoorInversionVariant.POLTERGEIST || variant == DoorInversionVariant.DOOR_TRAP_CASCADE)
                            ? EventSeverity.MEDIUM
                            : EventSeverity.HIGH);
        }
        return true;
    }

    public static boolean triggerPhantomHarvest(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        List<BlockPos> crops = findNearbyCrops(level, player.blockPosition(), 8);
        if (crops.isEmpty()) {
            return false;
        }

        int danger = getDangerLevel();
        int roll = player.getRandom().nextInt(100);
        PhantomHarvestVariant variant;
        if (danger <= 1) {
            variant = PhantomHarvestVariant.BLACK_HARVEST;
        } else if (roll < 35) {
            variant = PhantomHarvestVariant.ROTTEN_SOIL;
        } else if (roll < 60) {
            variant = PhantomHarvestVariant.INFESTATION;
        } else {
            variant = PhantomHarvestVariant.BLACK_HARVEST;
        }
        return triggerPhantomHarvest(player, crops, variant, true);
    }

    public static boolean triggerPhantomHarvestVariant(ServerPlayer player, PhantomHarvestVariant variant) {
        if (variant == null || player.getServer() == null) {
            return false;
        }
        List<BlockPos> crops = findNearbyCrops(player.serverLevel(), player.blockPosition(), 8);
        if (crops.isEmpty()) {
            return false;
        }
        return triggerPhantomHarvest(player, crops, variant, true);
    }

    private static boolean triggerPhantomHarvest(
            ServerPlayer player,
            List<BlockPos> crops,
            PhantomHarvestVariant variant,
            boolean applyCooldown) {
        ServerLevel level = player.serverLevel();
        long now = player.getServer().getTickCount();
        int count = Math.min(crops.size(), 3 + level.random.nextInt(6));
        for (int i = 0; i < count; i++) {
            BlockPos cropPos = crops.get(level.random.nextInt(crops.size()));
            level.destroyBlock(cropPos, true);
            if (variant == PhantomHarvestVariant.ROTTEN_SOIL) {
                BlockPos below = cropPos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(Blocks.FARMLAND)) {
                    level.setBlock(below, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                }
            } else if (variant == PhantomHarvestVariant.INFESTATION && level.random.nextBoolean()) {
                if (level.random.nextBoolean()) {
                    Silverfish silverfish = EntityType.SILVERFISH.create(level);
                    if (silverfish != null) {
                        silverfish.moveTo(cropPos.getX() + 0.5D, cropPos.getY() + 0.1D, cropPos.getZ() + 0.5D, 0.0F, 0.0F);
                        silverfish.setTarget(player);
                        level.addFreshEntity(silverfish);
                    }
                } else {
                    Mob spiderling = UncannyEntityRegistry.UNCANNY_SPIDERLING.get().create(level);
                    if (spiderling != null) {
                        spiderling.moveTo(cropPos.getX() + 0.5D, cropPos.getY() + 0.1D, cropPos.getZ() + 0.5D, 0.0F, 0.0F);
                        if (spiderling instanceof Monster monster) {
                            monster.setTarget(player);
                        }
                        level.addFreshEntity(spiderling);
                    }
                }
            }
        }

        if (applyCooldown) {
            markGlobalCooldown(player, now, variant == PhantomHarvestVariant.BLACK_HARVEST ? EventSeverity.MEDIUM : EventSeverity.HIGH);
        }
        return true;
    }

    public static boolean triggerLivingOre(ServerPlayer player) {
        return triggerLivingOre(player, false);
    }

    public static boolean triggerLivingOreVariant(ServerPlayer player, LivingOreVariant variant) {
        if (variant == null) {
            return false;
        }
        return triggerLivingOre(player, variant, false);
    }

    private static boolean triggerLivingOre(ServerPlayer player, boolean independent) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        int danger = getDangerLevel();
        int roll = player.getRandom().nextInt(100);
        LivingOreVariant variant;
        if (danger <= 1) {
            variant = LivingOreVariant.BLEEDING;
        } else if (roll < 34) {
            variant = LivingOreVariant.TOXIC_BLOOD;
        } else if (roll < 56) {
            variant = LivingOreVariant.VICIOUS_FALL;
        } else if (roll < 62) {
            variant = LivingOreVariant.VEIN_RETREAT;
        } else if (roll < 70) {
            variant = LivingOreVariant.INSIDE_KNOCK;
        } else {
            variant = LivingOreVariant.BLEEDING;
        }
        return triggerLivingOre(player, variant, independent);
    }

    private static boolean triggerLivingOre(ServerPlayer player, LivingOreVariant variant, boolean independent) {
        long now = player.getServer().getTickCount();
        LIVING_ORE_PRIMED.put(player.getUUID(), new LivingOreState(now + 20L * 25L, variant));
        if (!independent) {
            markGlobalCooldown(player, now, variant == LivingOreVariant.BLEEDING ? EventSeverity.MEDIUM : EventSeverity.HIGH);
        }
        return true;
    }

    public static boolean triggerProjectedShadow(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }

        int danger = getDangerLevel();
        int roll = player.getRandom().nextInt(100);
        ProjectedShadowVariant variant;
        if (danger <= 1) {
            variant = ProjectedShadowVariant.MIME;
        } else if (roll < 36) {
            variant = ProjectedShadowVariant.SHADOW_ASSAULT;
        } else if (roll < 62) {
            variant = ProjectedShadowVariant.GHOST_SHOT;
        } else {
            variant = ProjectedShadowVariant.MIME;
        }
        return triggerProjectedShadow(player, variant, true);
    }

    public static boolean triggerProjectedShadowVariant(ServerPlayer player, ProjectedShadowVariant variant) {
        if (variant == null) {
            return false;
        }
        return triggerProjectedShadow(player, variant, true);
    }

    private static boolean triggerProjectedShadow(ServerPlayer player, ProjectedShadowVariant variant, boolean applyCooldown) {
        long now = player.getServer().getTickCount();
        ServerLevel level = player.serverLevel();
        BlockPos wall = findNearbySolidWall(level, player);
        switch (variant) {
            case MIME -> {
                spawnWallShadowSilhouette(level, player, wall, 1.0F);
                playLocalSound(player, UncannySoundRegistry.UNCANNY_WHISPER.get(), SoundSource.AMBIENT, 0.6F, 0.9F);
            }
            case SHADOW_ASSAULT -> {
                spawnWallShadowSilhouette(level, player, wall, 1.35F);
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                playLocalSound(player, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.3F, 0.6F);
            }
            case GHOST_SHOT -> {
                spawnWallShadowSilhouette(level, player, wall, 1.2F);
                player.hurt(player.damageSources().magic(), 2.0F);
                playLocalSound(player, SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.1F, 0.5F);
            }
        }

        if (applyCooldown) {
            markGlobalCooldown(player, now, variant == ProjectedShadowVariant.MIME ? EventSeverity.MEDIUM : EventSeverity.HIGH);
        }
        return true;
    }

    public static boolean triggerGiantSun(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        long now = player.getServer().getTickCount();
        int durationTicks = 20 * (20 + player.getRandom().nextInt(26));
        ACTIVE_GIANT_SUN.put(player.getUUID(), new GiantSunState(now + durationTicks, now + 20L, 0, false, 0));
        player.addTag(GIANT_SUN_TAG);
        playLocalSound(player, SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, 1.05F, 0.42F);
        playLocalSound(player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.AMBIENT, 0.16F, 0.72F);

        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerHunterFog(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        long now = player.getServer().getTickCount();
        int duration = 20 * 30;
        ACTIVE_HUNTER_FOG.put(player.getUUID(), new HunterFogState(now + duration, 0));
        player.addTag(HUNTER_FOG_TAG);
        playLocalSound(player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.AMBIENT, 0.35F, 0.75F);
        markGlobalCooldown(player, now, EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerGrandEventWarden(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return false;
        }
        return startGrandEventWarden(player.serverLevel(), player.getServer().getTickCount(), true);
    }

    public static boolean triggerGrandEventStop(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        GrandEventState state = ACTIVE_GRAND_EVENTS.get(player.serverLevel().dimension());
        if (state == null || state.ended()) {
            return false;
        }
        debugLog(
                "GRAND_EVENT cleanup_reason=forced_stop runtime={} dim={}",
                state.runtimeId(),
                player.serverLevel().dimension().location());
        finishGrandEvent(player.serverLevel(), state, true, player.getServer().getTickCount(), "forced_stop");
        return true;
    }

    public static boolean triggerTensionBuilderStart(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        MinecraftServer server = player.getServer();
        UncannyWorldState state = UncannyWorldState.get(server);
        if (state.getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        long now = server.getTickCount();
        int durationSeconds = rollRangeInclusive(player.serverLevel(), TENSION_BUILDER_MIN_SECONDS, TENSION_BUILDER_MAX_SECONDS);
        state.setTensionBuilderEndTick(now + durationSeconds * 20L);
        state.setTensionBuilderNextStartTick(Long.MIN_VALUE);
        state.setTensionBuilderGrandEventBoostUntilTick(Long.MIN_VALUE);
        state.setTensionBuilderNextGrandEventRollTick(now + rollGrandEventRollDelayTicks(player.serverLevel()));
        debugLog("TENSION command-start by={} duration={}s", playerLabel(player), durationSeconds);
        return true;
    }

    public static boolean triggerTensionBuilderStop(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        MinecraftServer server = player.getServer();
        UncannyWorldState state = UncannyWorldState.get(server);
        if (state.getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        long now = server.getTickCount();
        int breakSeconds = rollRangeInclusive(player.serverLevel(), TENSION_BREAK_MIN_SECONDS, TENSION_BREAK_MAX_SECONDS);
        int boostSeconds = rollRangeInclusive(player.serverLevel(), GRAND_EVENT_BOOST_MIN_SECONDS, GRAND_EVENT_BOOST_MAX_SECONDS);
        state.setTensionBuilderEndTick(Long.MIN_VALUE);
        state.setTensionBuilderNextStartTick(now + breakSeconds * 20L);
        state.setTensionBuilderGrandEventBoostUntilTick(now + boostSeconds * 20L);
        state.setTensionBuilderNextGrandEventRollTick(now + rollGrandEventRollDelayTicks(player.serverLevel()));
        debugLog(
                "TENSION command-stop by={} nextStartIn={}s grandBoost={}s",
                playerLabel(player),
                breakSeconds,
                boostSeconds);
        return true;
    }

    public static String getTensionBuilderStatus(ServerPlayer player) {
        if (player.getServer() == null) {
            return "TensionBuilder status unavailable: no server.";
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        long now = player.getServer().getTickCount();
        boolean active = isTensionBuilderActive(state, now);
        long remaining = safeFutureRemainingTicks(state.getTensionBuilderEndTick(), now, 20L * 60L * 60L);
        long nextStart = safeFutureRemainingTicks(state.getTensionBuilderNextStartTick(), now, 20L * 60L * 60L);
        long boost = safeFutureRemainingTicks(state.getTensionBuilderGrandEventBoostUntilTick(), now, 20L * 60L * 60L);
        long nextRoll = safeFutureRemainingTicks(state.getTensionBuilderNextGrandEventRollTick(), now, 20L * 60L * 60L);
        return "TensionBuilder | active=" + active
                + " | remaining=" + ticksToSeconds(remaining) + "s"
                + " | nextStart=" + ticksToSeconds(nextStart) + "s"
                + " | grandBoost=" + ticksToSeconds(boost) + "s"
                + " | nextGrandRoll=" + ticksToSeconds(nextRoll) + "s"
                + " | grandBaseChance=" + String.format(Locale.ROOT, "%.3f%%", GRAND_EVENT_BASE_CHANCE * 100.0D)
                + " | grandBoostChance=" + String.format(Locale.ROOT, "%.1f%%", GRAND_EVENT_POST_TENSION_CHANCE * 100.0D);
    }

    public static String getGrandEventStatus(ServerPlayer player) {
        if (player.getServer() == null) {
            return "GrandEvent status unavailable: no server.";
        }
        GrandEventState state = ACTIVE_GRAND_EVENTS.get(player.serverLevel().dimension());
        if (state == null || state.ended()) {
            return "GrandEvent | active=none";
        }
        long now = player.getServer().getTickCount();
        return "GrandEvent | active=warden"
                + " | tracked=" + state.trackedPlayers().size()
                + " | latched=" + state.latchedPlayers().size()
                + " | attack=" + (state.attackTarget() != null)
                + " | exiting=" + state.exiting()
                + " | sinking=" + state.sinking()
                + " | elapsed=" + ticksToSeconds(Math.max(0L, now - state.startedTick())) + "s";
    }

    public static boolean activateUncannyCompassGuide(ServerPlayer player, boolean fromItemUse) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        normalizeUncannyCompassInHands(player);
        long now = player.getServer().getTickCount();
        BlockPos target = findNearestLorePriorityStructure(player);
        if (target == null) {
            debugLog("EVENT compass_liar skip player={} reason=no-lore-structure-target", playerLabel(player));
            return false;
        }
        ACTIVE_COMPASS_LIARS.put(player.getUUID(), new CompassLiarState(now + 20L * 30L, target));
        if (fromItemUse) {
            player.displayClientMessage(COMPASS_LIAR_MESSAGE, true);
        }
        return true;
    }

    private static void normalizeUncannyCompassInHands(ServerPlayer player) {
        convertUncannyCompassInHand(player, InteractionHand.MAIN_HAND);
        convertUncannyCompassInHand(player, InteractionHand.OFF_HAND);
    }

    private static void convertUncannyCompassInHand(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        if (!stack.is(UncannyItemRegistry.UNCANNY_COMPASS.get())
                && !(stack.is(Items.COMPASS) && !isTrackableCompassItem(stack))) {
            return;
        }
        ItemStack vanillaCompass = new ItemStack(Items.COMPASS, stack.getCount());
        vanillaCompass.set(DataComponents.CUSTOM_NAME, Component.literal("Uncanny Compass"));
        player.setItemInHand(hand, vanillaCompass);
    }

    public static boolean triggerAnimalStareLock(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        long now = player.getServer().getTickCount();
        List<MobSnapshot> affected = collectAnimalStareTargets(player);
        if (affected.size() < 3) {
            return false;
        }
        long endTick = now + 15L * 20L;
        ACTIVE_ANIMAL_STARE_LOCKS.put(player.getUUID(), new AnimalStareLockState(endTick, affected));
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerCompassLiar(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return false;
        }
        if (!holdsCompass(player)) {
            debugLog("EVENT compass_liar skip player={} reason=no-compass", playerLabel(player));
            player.sendSystemMessage(Component.literal("Debug: compass_liar requires a compass in main/off hand."));
            return false;
        }
        long now = player.getServer().getTickCount();
        if (!isManualEventCooldownReady(player, "compass_liar", now, COOLDOWN_COMPASS_LIAR_SECONDS)) {
            return false;
        }
        if (!activateUncannyCompassGuide(player, false)) {
            return false;
        }
        applyManualEventCooldown(player, "compass_liar", now, COOLDOWN_COMPASS_LIAR_SECONDS);
        player.displayClientMessage(COMPASS_LIAR_MESSAGE, true);
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerFurnaceBreath(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos source = findBestFurnaceBreathTarget(level, player);
        if (source == null) {
            return false;
        }
        long now = player.getServer().getTickCount();
        int repetitions = 1 + level.random.nextInt(3);
        ACTIVE_FURNACE_BREATHS.put(player.getUUID(), new FurnaceBreathState(source, repetitions, now + 6L + level.random.nextInt(12)));
        markGlobalCooldown(player, now, EventSeverity.LIGHT);
        return true;
    }

    public static boolean triggerMisplacedLight(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return false;
        }
        if (ACTIVE_MISPLACED_LIGHTS.containsKey(player.getUUID())) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        MisplacedLightState state = tryCreateMisplacedLightState(level, player);
        if (state == null) {
            return false;
        }
        ACTIVE_MISPLACED_LIGHTS.put(player.getUUID(), state);
        markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerPetRefusal(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return false;
        }
        Mob pet = findNearestTamedPet(player.serverLevel(), player.blockPosition(), 12);
        if (pet == null) {
            debugLog("EVENT pet_refusal skip player={} reason=no-supported-tamed-pet", playerLabel(player));
            return false;
        }
        long now = player.getServer().getTickCount();
        long duration = (8L + player.getRandom().nextInt(8)) * 20L;
        ACTIVE_PET_REFUSALS.put(player.getUUID(), new PetRefusalState(pet.getUUID(), pet.isNoAi(), now + duration));
        pet.addTag("eotv_pet_refusal");
        pet.addTag("eotv_pet_refusal_black");
        assignPetRefusalBlackTeam(player.serverLevel(), pet);
        sendPetRefusalVisual(player, pet, true, (int) duration);
        debugLog(
                "EVENT pet_refusal start player={} pet={} type={} team={} tags={} noAi={} duration={}t",
                playerLabel(player),
                pet.getStringUUID(),
                pet.getType().toShortString(),
                pet.getTeam() != null ? pet.getTeam().getName() : "none",
                pet.getTags(),
                pet.isNoAi(),
                duration);
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerWorkbenchReject(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (!isPlayerUsingWorkbenchMenu(player)) {
            debugLog("EVENT workbench_reject skip player={} reason=not-in-workbench-menu", playerLabel(player));
            return false;
        }
        player.closeContainer();
        player.sendSystemMessage(WORKBENCH_REJECT_MESSAGE);
        markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.HIGH);
        return true;
    }

    public static boolean triggerFalseContainerOpen(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (!isPlayerUsingContainerMenu(player)) {
            debugLog("EVENT false_container_open manual-skip player={} reason=no-open-container-ui", playerLabel(player));
            return false;
        }
        long now = player.getServer().getTickCount();
        ContainerEchoContext context = LAST_CONTAINER_CONTEXTS.get(player.getUUID());
        if (context == null
                || context.dimension() != player.serverLevel().dimension()
                || (now - context.tick()) > 100L
                || context.sourcePos().distSqr(player.blockPosition()) > 64.0D) {
            debugLog("EVENT false_container_open manual-skip player={} reason=no-recent-context", playerLabel(player));
            return false;
        }

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 dir = new Vec3(-look.x, 0.0D, -look.z);
        if (dir.lengthSqr() < 0.0001D) {
            dir = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            dir = dir.normalize();
        }
        Vec3 at = player.position().add(dir.scale(2.0D));
        playLocalSoundAt(
                player,
                at.x,
                player.getEyeY() - 0.5D,
                at.z,
                context.sound(),
                SoundSource.BLOCKS,
                0.95F,
                0.9F + player.getRandom().nextFloat() * 0.14F);
        markGlobalCooldown(player, now, EventSeverity.LIGHT);
        return true;
    }

    public static boolean triggerLeverAnswer(ServerPlayer player) {
        return triggerLeverAnswer(player, true);
    }

    private static boolean triggerLeverAnswer(ServerPlayer player, boolean allowFallbackIfNoLever) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        BlockPos source = findNearbyBlock(player.serverLevel(), player.blockPosition(), 16, s -> s.getBlock() instanceof LeverBlock);
        if (source == null) {
            if (!allowFallbackIfNoLever) {
                return false;
            }
            Vec3 look = player.getViewVector(1.0F);
            Vec3 dir = new Vec3(-look.x, 0.0D, -look.z);
            if (dir.lengthSqr() < 0.0001D) {
                dir = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                dir = dir.normalize();
            }
            Vec3 at = player.position().add(dir.scale(2.0D));
            playLocalSoundAt(player, at.x, player.getEyeY() - 0.5D, at.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.05F, 0.6F);
            markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.LIGHT);
            debugLog("EVENT lever_answer fallback player={} reason=no-nearby-lever", playerLabel(player));
            return true;
        }
        return tryTriggerLeverAnswerOnInteract(player, source, player.getServer().getTickCount(), false);
    }

    public static boolean triggerPressurePlateReply(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        BlockPos source = findNearbyBlock(player.serverLevel(), player.blockPosition(), 16, s -> s.getBlock() instanceof PressurePlateBlock);
        if (source == null) {
            return false;
        }
        return triggerPressurePlateReplyNow(player, source, player.getServer().getTickCount(), true);
    }

    public static boolean triggerCampfireCough(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }
        BlockPos campfirePos = findNearbyBlock(player.serverLevel(), player.blockPosition(), 12,
                state -> state.getBlock() instanceof CampfireBlock);
        if (campfirePos == null) {
            return false;
        }
        playLocalSoundAt(player, campfirePos, UncannySoundRegistry.CAMPFIRE_COUGH_CREEPY.get(), SoundSource.HOSTILE, 0.92F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerBucketDrip(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (!holdsEmptyBucket(player)) {
            return false;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 0.0001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 at = player.position().add(side.scale(1.8D));
        playLocalSoundAt(player, at.x, player.getEyeY() - 0.45D, at.z, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.28F, 1.5F);
        markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.LIGHT);
        return true;
    }

    public static boolean triggerHotbarWrongCount(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        int slot = findRandomNonEmptyHotbarSlot(player);
        if (slot < 0) {
            return false;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            return false;
        }
        int actualCount = stack.getCount();
        int fakeCount;
        if (actualCount <= 1) {
            fakeCount = 2 + player.getRandom().nextInt(3);
        } else {
            int deviation = Math.max(2, Math.min(16, Math.max(2, actualCount / 3)));
            fakeCount = player.getRandom().nextBoolean()
                    ? Math.min(99, actualCount + deviation)
                    : Math.max(1, actualCount - deviation);
        }
        if (fakeCount == actualCount) {
            fakeCount = actualCount <= 1 ? 2 : actualCount - 1;
        }
        int durationTicks = 34 + player.getRandom().nextInt(25);
        long endTick = player.getServer().getTickCount() + durationTicks;
        ACTIVE_HOTBAR_WRONG_COUNTS.put(player.getUUID(), new HotbarWrongCountState(slot, fakeCount, endTick));
        PacketDistributor.sendToPlayer(player, new UncannyHotbarWrongCountPayload(slot, fakeCount, durationTicks));
        debugLog(
                "EVENT hotbar_wrong_count player={} slot={} selected={} actual={} fake={} duration={}t",
                playerLabel(player),
                slot,
                player.getInventory().selected,
                actualCount,
                fakeCount,
                durationTicks);
        markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerFalseRecipeToast(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        String message = pickCorruptMessageForPlayer(player);
        PacketDistributor.sendToPlayer(player, new UncannyFalseRecipeToastPayload("System Message", message));
        markGlobalCooldown(player, player.getServer().getTickCount(), EventSeverity.MEDIUM);
        return true;
    }

    public static boolean triggerToolAnswer(ServerPlayer player) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        ToolAnswerContext context = LAST_TOOL_ANSWER_CONTEXT.get(player.getUUID());
        if (!hasRecentToolAnswerContext(player, context, 20L * 12L)) {
            debugLog("EVENT tool_answer skip player={} reason=no-recent-mining-context", playerLabel(player));
            return false;
        }
        long now = player.getServer().getTickCount();
        scheduleToolAnswerEchoSequence(player, context.minedPos(), context.minedState(), context.toolStack(), now);
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
        return true;
    }

    private static boolean hasRecentToolAnswerContext(ServerPlayer player, ToolAnswerContext context, long maxAgeTicks) {
        if (player.getServer() == null || context == null) {
            return false;
        }
        if (context.dimension() != player.serverLevel().dimension()) {
            return false;
        }
        long age = player.getServer().getTickCount() - context.tick();
        return age >= 0L && age <= maxAgeTicks;
    }

    public static boolean spawnShadow(ServerPlayer player) {
        return spawnShadow(player, false, false);
    }

    private static boolean spawnShadow(ServerPlayer player, boolean strictDark) {
        return spawnShadow(player, strictDark, false);
    }

    public static boolean spawnShadowForCommand(ServerPlayer player) {
        return spawnShadow(player, false, true);
    }

    private static boolean spawnShadow(ServerPlayer player, boolean strictDark, boolean preferCloseBehind) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-shadow blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        if (!preferCloseBehind && getDangerLevel() <= 0) {
            debugLog("SPECIAL spawn-shadow blocked danger0 player={}", playerLabel(player));
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos;

        if (preferCloseBehind) {
            pos = findCommandSpawnBehindPlayer(level, player, 9, 22, true);
            if (pos == null) {
                pos = findCommandSpawnAroundPlayer(level, player, 8, 20, true);
            }
        } else {
            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 26, 58, 0.52D);
            pos = findSpawnAroundPlayer(level, player, window.minDistance(), window.maxDistance(), true);
        }

        if (pos == null && !strictDark) {
            if (preferCloseBehind) {
                pos = findCommandSpawnBehindPlayer(level, player, 8, 20, false);
                if (pos == null) {
                    pos = findCommandSpawnAroundPlayer(level, player, 8, 18, false);
                }
            } else {
                SpawnDistanceWindow window = resolveFarSpawnWindow(player, 26, 58, 0.52D);
                int relaxedMin = Math.max(26, window.minDistance() - 16);
                pos = findSpawnAroundPlayer(level, player, relaxedMin, window.maxDistance(), false);
                if (pos == null) {
                    pos = findSpawnBehindPlayer(level, player, 14, 34, false);
                }
                if (pos == null) {
                    pos = findSpawnAroundPlayer(level, player, 12, 30, false);
                }
            }
        }

        pos = refineSpecialSpawnPos(level, player, pos, !preferCloseBehind && strictDark, preferCloseBehind, 12, 40);
        if (pos == null) {
            debugLog("SPECIAL spawn-shadow no-position player={} strictDark={} close={}", playerLabel(player), strictDark, preferCloseBehind);
            return false;
        }

        UncannyShadowEntity shadow = UncannyEntityRegistry.UNCANNY_SHADOW.get().create(level);
        if (shadow == null) {
            debugLog("SPECIAL spawn-shadow create-null player={}", playerLabel(player));
            return false;
        }

        shadow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(shadow);
        debugLog("SPECIAL spawn-shadow success player={} at={}", playerLabel(player), pos);
        return true;
    }

    public static boolean spawnHurler(ServerPlayer player) {
        return spawnHurler(player, false, false);
    }

    public static boolean spawnHurlerForCommand(ServerPlayer player) {
        return spawnHurler(player, true, true);
    }

    private static boolean spawnHurler(ServerPlayer player, boolean preferCloseBehind, boolean forceAggression) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-hurler blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos;

        if (preferCloseBehind) {
            pos = findCommandSpawnBehindPlayer(level, player, 9, 22, false);
            if (pos == null) {
                pos = findCommandSpawnAroundPlayer(level, player, 8, 20, false);
            }
            if (pos == null && forceAggression) {
                pos = findSpawnBehindPlayer(level, player, 8, 18, false);
                if (pos == null) {
                    pos = findSpawnAroundPlayer(level, player, 7, 16, false);
                }
                if (pos == null) {
                    pos = fallbackSpawnBehindOffset(level, player, 7, false);
                }
            }
        } else {
            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 24, 54, 0.50D);
            pos = findSpawnAroundPlayer(level, player, window.minDistance(), window.maxDistance(), false);
            if (pos == null) {
                pos = findSpawnBehindPlayer(level, player, 14, 36, false);
            }
            if (pos == null) {
                pos = findSpawnAroundPlayer(level, player, 12, 32, false);
            }
        }

        pos = refineSpecialSpawnPos(level, player, pos, false, preferCloseBehind, 10, 36);
        if (pos == null) {
            debugLog("SPECIAL spawn-hurler no-position player={} close={}", playerLabel(player), preferCloseBehind);
            return false;
        }

        UncannyHurlerEntity hurler = UncannyEntityRegistry.UNCANNY_HURLER.get().create(level);
        if (hurler == null) {
            debugLog("SPECIAL spawn-hurler create-null player={}", playerLabel(player));
            return false;
        }

        hurler.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        hurler.setWatchedPlayer(player);
        int danger = getDangerLevel();
        int attackChance = forceAggression ? 10 : DANGER_HURLER_ATTACK_PERCENT[danger];
        hurler.setAttackChancePercent(attackChance);
        level.addFreshEntity(hurler);
        debugLog("SPECIAL spawn-hurler success player={} at={} attackChance={}", playerLabel(player), pos, attackChance);
        return true;
    }

    public static boolean spawnStalker(ServerPlayer player) {
        return spawnStalker(player, false, false);
    }

    public static boolean spawnStalkerForCommand(ServerPlayer player) {
        return spawnStalker(player, true, true);
    }

    private static boolean spawnStalker(ServerPlayer player, boolean preferCloseBehind, boolean ignoreDangerGate) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-stalker blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!ignoreDangerGate && getDangerLevel() <= 0) {
            debugLog("SPECIAL spawn-stalker blocked danger0 player={}", playerLabel(player));
            return false;
        }
        if (preferCloseBehind) {
            boolean requireObserverStealth = !ignoreDangerGate;
            UncannyStalkerEntity spawned = spawnStalkerEntity(player, 10, 24, true, requireObserverStealth);
            if (spawned != null) {
                debugLog("SPECIAL spawn-stalker success(close) player={} at={}", playerLabel(player), spawned.blockPosition());
                return true;
            }
            if (ignoreDangerGate) {
                spawned = spawnStalkerEntity(player, 8, 18, true, false);
                if (spawned == null) {
                    spawned = spawnStalkerEntity(player, 7, 16, false, false);
                }
                if (spawned == null) {
                    BlockPos fallback = fallbackSpawnBehindOffset(level, player, 7, false);
                    if (fallback != null) {
                        UncannyStalkerEntity manual = UncannyEntityRegistry.UNCANNY_STALKER.get().create(level);
                        if (manual != null) {
                            manual.moveTo(fallback.getX() + 0.5D, fallback.getY(), fallback.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
                            manual.setHuntTarget(player);
                            level.addFreshEntity(manual);
                            spawned = manual;
                        }
                    }
                }
                if (spawned != null) {
                    debugLog("SPECIAL spawn-stalker success(close-relaxed) player={} at={}", playerLabel(player), spawned.blockPosition());
                    return true;
                }
            }
            debugLog("SPECIAL spawn-stalker fail(close) player={}", playerLabel(player));
            return false;
        }
        SpawnDistanceWindow window = resolveFarSpawnWindow(player, 30, 62, 0.55D);
        UncannyStalkerEntity stalker = spawnStalkerEntity(player, window.minDistance(), window.maxDistance(), true, false);
        if (stalker != null) {
            debugLog("SPECIAL spawn-stalker success(far) player={} at={}", playerLabel(player), stalker.blockPosition());
            return true;
        }
        stalker = spawnStalkerEntity(player, 16, 40, true, false);
        if (stalker != null) {
            debugLog("SPECIAL spawn-stalker success(mid-behind) player={} at={}", playerLabel(player), stalker.blockPosition());
            return true;
        }
        stalker = spawnStalkerEntity(player, 12, 30, false, false);
        if (stalker != null) {
            debugLog("SPECIAL spawn-stalker success(mid-around) player={} at={}", playerLabel(player), stalker.blockPosition());
            return true;
        }
        debugLog("SPECIAL spawn-stalker fail player={}", playerLabel(player));
        return false;
    }

    public static boolean spawnKnocker(ServerPlayer player) {
        return spawnKnocker(player, false, false);
    }

    public static boolean spawnKnockerForCommand(ServerPlayer player) {
        return spawnKnocker(player, true, true);
    }

    private static boolean spawnKnocker(ServerPlayer player, boolean preferCloseBehind, boolean forceAggression) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-knocker blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        if (!preferCloseBehind && player.getServer() != null && !isNearBase(player, player.getServer())) {
            debugLog("SPECIAL spawn-knocker blocked not-near-base player={}", playerLabel(player));
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (level.canSeeSky(player.blockPosition())) {
            debugLog("SPECIAL spawn-knocker blocked sky-visible player={}", playerLabel(player));
            return false;
        }

        BlockPos doorPos = findNearbyBlock(level, player.blockPosition(), 10, state -> state.getBlock() instanceof DoorBlock);
        if (doorPos == null) {
            debugLog("SPECIAL spawn-knocker no-door player={}", playerLabel(player));
            return false;
        }

        BlockPos spawnPos;
        if (preferCloseBehind) {
            spawnPos = findCommandSpawnBehindPlayer(level, player, 7, 18, false);
            if (spawnPos == null) {
                spawnPos = findCommandSpawnAroundPlayer(level, player, 6, 16, false);
            }
        } else {
            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 18, 42, 0.45D);
            spawnPos = findSpawnAroundPlayer(level, player, window.minDistance(), window.maxDistance(), false);
        }

        spawnPos = refineSpecialSpawnPos(level, player, spawnPos, false, preferCloseBehind, 8, 26);
        if (spawnPos == null) {
            spawnPos = findSpawnNearDoorOutside(level, doorPos, player);
        }
        if (spawnPos == null) {
            debugLog("SPECIAL spawn-knocker no-position player={} door={}", playerLabel(player), doorPos);
            return false;
        }

        UncannyKnockerEntity knocker = UncannyEntityRegistry.UNCANNY_KNOCKER.get().create(level);
        if (knocker == null) {
            debugLog("SPECIAL spawn-knocker create-null player={}", playerLabel(player));
            return false;
        }

        knocker.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        knocker.setupKnockingTarget(player, doorPos);
        int danger = getDangerLevel();
        boolean canAttack = forceAggression || danger > 0;
        knocker.setCanAttack(canAttack);
        knocker.setOpenDoorAttackChancePercent(forceAggression ? 20 : DANGER_KNOCKER_OPEN_ATTACK_PERCENT[danger]);
        level.addFreshEntity(knocker);
        debugLog("SPECIAL spawn-knocker success player={} at={} door={} canAttack={}", playerLabel(player), spawnPos, doorPos, canAttack);
        return true;
    }

    public static boolean spawnPulse(ServerPlayer player) {
        return spawnPulse(player, false);
    }

    public static boolean spawnPulseForCommand(ServerPlayer player) {
        return spawnPulse(player, true);
    }

    private static boolean spawnPulse(ServerPlayer player, boolean preferCloseBehind) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-pulse blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        if (!preferCloseBehind && getDangerLevel() <= 0) {
            debugLog("SPECIAL spawn-pulse blocked danger0 player={}", playerLabel(player));
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = null;

        if (preferCloseBehind) {
            for (int attempt = 0; attempt < 14 && pos == null; attempt++) {
                BlockPos candidate = findCommandSpawnBehindPlayer(level, player, 6, 14, false);
                if (candidate == null) {
                    candidate = findCommandSpawnAroundPlayer(level, player, 4, 12, false);
                }
                if (candidate != null) {
                    pos = candidate.immutable();
                }
            }
            if (pos == null) {
                pos = findSpawnAroundPlayer(level, player, 4, 10, false);
            }
            if (pos == null) {
                pos = fallbackSpawnBehindOffset(level, player, 6, false);
            }
        } else {
            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 28, 72, 0.56D);
            pos = findBrightSpawnAroundPlayer(level, player, window.minDistance(), window.maxDistance(), 13);
            if (pos == null) {
                pos = findBrightSpawnAroundPlayer(level, player, 24, 56, 12);
            }
            if (pos == null) {
                pos = findSpawnBehindPlayer(level, player, 20, 46, false);
            }
        }

        pos = refineSpecialSpawnPos(level, player, pos, false, preferCloseBehind, preferCloseBehind ? 6 : 20, preferCloseBehind ? 24 : 52);
        if (pos == null) {
            debugLog("SPECIAL spawn-pulse no-position player={} close={}", playerLabel(player), preferCloseBehind);
            return false;
        }

        UncannyPulseEntity pulse = UncannyEntityRegistry.UNCANNY_PULSE.get().create(level);
        if (pulse == null) {
            debugLog("SPECIAL spawn-pulse create-null player={}", playerLabel(player));
            return false;
        }

        pulse.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        pulse.setTarget(player);
        level.addFreshEntity(pulse);

        if (preferCloseBehind) {
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D,
                    14, 0.18D, 0.45D, 0.18D, 0.02D);
            playLocalSound(player, UncannySoundRegistry.UNCANNY_HEARTBEAT.get(), SoundSource.HOSTILE, 1.4F, 0.92F);
        }
        debugLog("SPECIAL spawn-pulse success player={} at={} close={}", playerLabel(player), pos, preferCloseBehind);
        return true;
    }

    public static boolean spawnUsher(ServerPlayer player) {
        return spawnUsher(player, false);
    }

    public static boolean spawnUsherForCommand(ServerPlayer player) {
        return spawnUsher(player, true);
    }

    private static boolean spawnUsher(ServerPlayer player, boolean preferCloseBehind) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-usher blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server != null && hasActiveUsher(server)) {
            debugLog("SPECIAL spawn-usher blocked existing-active player={}", playerLabel(player));
            return false;
        }
        if (!preferCloseBehind && player.getServer() != null
                && UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos target;
        if (preferCloseBehind) {
            target = findNearestLorePriorityStructure(player);
            if (target == null) {
                target = findNearestAnyUncannyStructure(player);
            }
        } else {
            target = findNearestLorePriorityMarker(player, 360);
        }
        if (target == null) {
            debugLog("SPECIAL spawn-usher skip player={} reason=no-near-generated-lore-target", playerLabel(player));
            return false;
        }
        if (!preferCloseBehind && target.distSqr(player.blockPosition()) > 360.0D * 360.0D) {
            debugLog("SPECIAL spawn-usher skip player={} reason=target-too-far target={}", playerLabel(player), target);
            return false;
        }

        BlockPos spawnPos;
        if (preferCloseBehind) {
            spawnPos = findCommandSpawnBehindPlayer(level, player, 8, 20, false);
            if (spawnPos == null) {
                spawnPos = findCommandSpawnAroundPlayer(level, player, 8, 18, false);
            }
            if (spawnPos == null) {
                spawnPos = findSpawnBehindPlayer(level, player, 8, 18, false);
                if (spawnPos == null) {
                    spawnPos = findSpawnAroundPlayer(level, player, 7, 16, false);
                }
                if (spawnPos == null) {
                    spawnPos = fallbackSpawnBehindOffset(level, player, 7, false);
                }
            }
        } else {
            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 10, 22, 0.56D);
            spawnPos = findSpawnBehindPlayer(level, player, window.minDistance(), window.maxDistance(), false);
            if (spawnPos == null) {
                spawnPos = findSpawnAroundPlayer(level, player, window.minDistance(), window.maxDistance(), false);
            }
        }
        spawnPos = refineSpecialSpawnPos(level, player, spawnPos, false, preferCloseBehind, 8, 22);
        if (spawnPos == null) {
            debugLog("SPECIAL spawn-usher no-position player={} close={}", playerLabel(player), preferCloseBehind);
            return false;
        }

        UncannyUsherEntity usher = UncannyEntityRegistry.UNCANNY_USHER.get().create(level);
        if (usher == null) {
            return false;
        }
        usher.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        usher.setupUsher(player, target);
        level.addFreshEntity(usher);
        debugLog("SPECIAL spawn-usher success player={} at={} target={}", playerLabel(player), spawnPos, target);
        return true;
    }

    public static boolean spawnKeeper(ServerPlayer player) {
        return spawnKeeper(player, false);
    }

    public static boolean spawnKeeperForCommand(ServerPlayer player) {
        return spawnKeeper(player, false);
    }

    private static boolean spawnKeeper(ServerPlayer player, boolean preferCloseBehind) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-keeper blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        MinecraftServer server = player.getServer();
        if (!preferCloseBehind && server != null) {
            UncannyPhase phase = UncannyWorldState.get(server).getPhase();
            if (phase.index() < UncannyPhase.PHASE_3.index()) {
                return false;
            }
            if (!isNearBase(player, server) || isInsideBase(player, server)) {
                debugLog("SPECIAL spawn-keeper skip player={} reason=not-near-or-inside-base", playerLabel(player));
                return false;
            }
        }
        ServerLevel level = player.serverLevel();
        BlockPos container = preferCloseBehind
                ? findKeeperContainerForPlayer(player, 18, 12)
                : findKeeperContainerNearBase(player, 18);
        if (container == null) {
            debugLog("SPECIAL spawn-keeper skip player={} reason=no-container", playerLabel(player));
            return false;
        }

        BlockPos spawnPos;
        if (preferCloseBehind) {
            spawnPos = findCommandSpawnBehindPlayer(level, player, 7, 18, false);
            if (spawnPos == null) {
                spawnPos = findCommandSpawnAroundPlayer(level, player, 7, 16, false);
            }
        } else {
            spawnPos = findSpawnAroundBaseOutsideView(level, player, container);
            if (spawnPos == null) {
                spawnPos = findSpawnNearDoorOutside(level, container, player);
                if (spawnPos != null) {
                    Vec3 center = Vec3.atCenterOf(spawnPos);
                    if (spawnPos.distSqr(player.blockPosition()) < 22.0D * 22.0D
                            || !isOutsideViewCone(player, center, 0.24D)
                            || !isOutOfSightOfOtherPlayers(level, player, center)) {
                        spawnPos = null;
                    }
                }
            }
        }
        if (preferCloseBehind) {
            spawnPos = refineSpecialSpawnPos(level, player, spawnPos, false, true, 7, 20);
        }
        if (spawnPos == null && preferCloseBehind) {
            spawnPos = container.above();
        } else if (spawnPos == null) {
            debugLog("SPECIAL spawn-keeper skip player={} reason=no-valid-base-spawn container={}", playerLabel(player), container);
            return false;
        }

        UncannyKeeperEntity keeper = UncannyEntityRegistry.UNCANNY_KEEPER.get().create(level);
        if (keeper == null) {
            return false;
        }
        keeper.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        keeper.setupKeeper(player, container);
        level.addFreshEntity(keeper);
        debugLog("SPECIAL spawn-keeper success player={} at={} container={}", playerLabel(player), spawnPos, container);
        return true;
    }

    private static BlockPos findKeeperContainerForPlayer(ServerPlayer player, int baseRadius, int localRadius) {
        BlockPos nearBase = findKeeperContainerNearBase(player, baseRadius);
        if (nearBase != null) {
            return nearBase;
        }
        ServerLevel level = player.serverLevel();
        return findNearbyBlock(level, player.blockPosition(), localRadius, UncannyParanoiaEventSystem::isContainerInteractionBlock);
    }

    private static BlockPos findKeeperContainerNearBase(ServerPlayer player, int baseRadius) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        BlockPos baseCenter = resolveBaseCenter(player, server);
        return findNearbyBlock(level, baseCenter, baseRadius, UncannyParanoiaEventSystem::isContainerInteractionBlock);
    }

    private static BlockPos findSpawnAroundBaseOutsideView(ServerLevel level, ServerPlayer player, BlockPos container) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        BlockPos baseCenter = resolveBaseCenter(player, server);
        int baseY = container != null ? container.getY() : baseCenter.getY();
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 6 + level.random.nextInt(15);
            int x = Mth.floor(baseCenter.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(baseCenter.getZ() + Math.sin(angle) * distance);
            BlockPos candidate = findSpawnAtOrAbovePlayerY(level, x, z, baseY, false);
            if (candidate == null) {
                continue;
            }
            if (candidate.distSqr(player.blockPosition()) < 22.0D * 22.0D) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(candidate);
            if (!isOutsideViewCone(player, center, 0.26D)) {
                continue;
            }
            if (!isOutOfSightOfOtherPlayers(level, player, center)) {
                continue;
            }
            return candidate.immutable();
        }
        return null;
    }

    public static boolean spawnTenant(ServerPlayer player) {
        return spawnTenant(player, false);
    }

    public static boolean spawnTenantForCommand(ServerPlayer player) {
        return spawnTenant(player, true);
    }

    private static boolean spawnTenant(ServerPlayer player, boolean preferCloseBehind) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-tenant blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        if (!preferCloseBehind && UncannyWorldState.get(server).getPhase().index() < UncannyPhase.PHASE_3.index()) {
            return false;
        }
        if (!preferCloseBehind && !isNearBase(player, server)) {
            return false;
        }

        long now = server.getTickCount();
        UUID playerId = player.getUUID();
        if (!preferCloseBehind) {
            Long awaySince = TENANT_AWAY_SINCE.get(playerId);
            if (awaySince == null || now - awaySince < 180L * 20L) {
                return false;
            }
        }

        ServerLevel level = player.serverLevel();
        BlockPos baseCenter = resolveBaseCenter(player, server);
        BlockPos doorPos = findNearestDoor(level, baseCenter, 24);
        if (doorPos == null) {
            if (preferCloseBehind) {
                doorPos = findNearestDoor(level, player.blockPosition(), 16);
            }
            if (doorPos == null) {
                debugLog("SPECIAL spawn-tenant skip player={} reason=no-door", playerLabel(player));
                return false;
            }
        }

        BlockPos spawnPos = findSpawnNearDoorOutside(level, doorPos, player);
        if (spawnPos == null && preferCloseBehind) {
            spawnPos = findCommandSpawnBehindPlayer(level, player, 8, 20, false);
        }
        if (preferCloseBehind) {
            spawnPos = refineSpecialSpawnPos(level, player, spawnPos, false, true, 8, 22);
        }
        if (spawnPos == null) {
            debugLog("SPECIAL spawn-tenant no-position player={} door={}", playerLabel(player), doorPos);
            return false;
        }

        UncannyTenantEntity tenant = UncannyEntityRegistry.UNCANNY_TENANT.get().create(level);
        if (tenant == null) {
            return false;
        }
        tenant.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        tenant.setupTenant(player, doorPos);
        level.addFreshEntity(tenant);
        TENANT_AWAY_SINCE.remove(playerId);
        debugLog("SPECIAL spawn-tenant success player={} at={} door={}", playerLabel(player), spawnPos, doorPos);
        return true;
    }

    public static boolean spawnFollower(ServerPlayer player) {
        return spawnFollower(player, false);
    }

    public static boolean spawnFollowerForCommand(ServerPlayer player) {
        return spawnFollower(player, true);
    }

    private static boolean spawnFollower(ServerPlayer player, boolean preferCloseBehind) {
        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL spawn-follower blocked water-or-boat player={}", playerLabel(player));
            return false;
        }
        if (!preferCloseBehind && player.getServer() != null
                && UncannyWorldState.get(player.getServer()).getPhase().index() < UncannyPhase.PHASE_2.index()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos spawnPos;
        if (preferCloseBehind) {
            spawnPos = findCommandSpawnBehindPlayer(level, player, 8, 20, false);
            if (spawnPos == null) {
                spawnPos = findCommandSpawnAroundPlayer(level, player, 8, 18, false);
            }
        } else {
            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 20, 46, 0.52D);
            spawnPos = findSpawnBehindPlayer(level, player, window.minDistance(), window.maxDistance(), false);
            if (spawnPos == null) {
                spawnPos = findSpawnAroundPlayer(level, player, window.minDistance(), window.maxDistance(), false);
            }
        }
        spawnPos = refineSpecialSpawnPos(level, player, spawnPos, false, preferCloseBehind, 8, 30);
        if (spawnPos == null) {
            debugLog("SPECIAL spawn-follower no-position player={} close={}", playerLabel(player), preferCloseBehind);
            return false;
        }

        UncannyFollowerEntity follower = UncannyEntityRegistry.UNCANNY_FOLLOWER.get().create(level);
        if (follower == null) {
            return false;
        }
        long durationTicks = (5L + level.random.nextInt(6)) * 60L * 20L;
        follower.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        follower.setupFollower(player, durationTicks);
        level.addFreshEntity(follower);
        debugLog("SPECIAL spawn-follower success player={} at={} duration={}s", playerLabel(player), spawnPos, durationTicks / 20L);
        return true;
    }

    public static boolean spawnPhantomLanternEaterForCommand(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UncannyPhantomEntity phantom = UncannyEntityRegistry.UNCANNY_PHANTOM.get().create(level);
        if (phantom == null) {
            return false;
        }
        BlockPos spawnPos = findCommandSpawnBehindPlayer(level, player, 8, 20, false);
        if (spawnPos == null) {
            spawnPos = findCommandSpawnAroundPlayer(level, player, 6, 16, false);
        }
        if (spawnPos == null) {
            spawnPos = player.blockPosition().above(2);
        }
        phantom.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.8D, spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        phantom.setTarget(player);
        phantom.setLanternEaterMode(true);
        level.addFreshEntity(phantom);
        debugLog("SPECIAL spawn-phantom-lantern-eater success player={} at={}", playerLabel(player), spawnPos);
        return true;
    }

    public static String getAutoEventDebugReport(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return "Auto-event debug unavailable: server is null.";
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        UncannyPhase phase = state.getPhase();
        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        long now = server.getTickCount();

        long globalCooldownTicks = computeEffectiveGlobalCooldownTicks(phase, profile, danger);
        long globalRemainingTicks = remainingCooldownTicks(state.getLastGlobalEventTick(), now, globalCooldownTicks);
        long nextCheckTick = NEXT_AUTO_CHECK_TICKS.getOrDefault(player.getUUID(), now);
        long nextCheckRemaining = Math.max(0L, nextCheckTick - now);

        long respawnGraceTicks = UncannyConfig.RESPAWN_GRACE_SECONDS.get() * 20L;
        long respawnRemainingTicks = remainingCooldownTicks(state.getLastRespawnTick(player.getUUID()), now, respawnGraceTicks);

        long specialNextCheck = Math.max(0L, NEXT_SPECIAL_ENTITY_CHECK_TICKS.getOrDefault(player.getUUID(), now) - now);
        Long lastSpecialTick = LAST_SPECIAL_ENTITY_EVENT_TICKS.get(player.getUUID());
        long specialCooldownTicks = computeSpecialEntityGlobalCooldownTicks(phase, profile, danger);
        long specialRemainingTicks = remainingCooldownTicks(lastSpecialTick, now, specialCooldownTicks);
        long ambientGlobalCooldownTicks = computeAmbientGlobalCooldownTicks(phase, profile, danger);
        long ambientRemainingTicks = remainingCooldownTicks(LAST_AMBIENT_EVENT_TICKS.get(player.getUUID()), now, ambientGlobalCooldownTicks);
        long weatherNextCheckRemaining = safeFutureRemainingTicks(state.getWeatherNextCheckTick(), now, 20L * 60L * 20L);
        long weatherCooldownRemaining = safeFutureRemainingTicks(state.getWeatherCooldownUntilTick(), now, 20L * 60L * 20L);
        long weatherActiveRemaining = safeFutureRemainingTicks(state.getWeatherEventEndTick(), now, 20L * 60L * 20L);
        long tensionRemaining = safeFutureRemainingTicks(state.getTensionBuilderEndTick(), now, 20L * 60L * 60L);
        long tensionNextRemaining = safeFutureRemainingTicks(state.getTensionBuilderNextStartTick(), now, 20L * 60L * 60L);
        long grandBoostRemaining = safeFutureRemainingTicks(state.getTensionBuilderGrandEventBoostUntilTick(), now, 20L * 60L * 60L);
        long grandNextRollRemaining = safeFutureRemainingTicks(state.getTensionBuilderNextGrandEventRollTick(), now, 20L * 60L * 60L);
        boolean tensionActive = isTensionBuilderActive(state, now);
        GrandEventState grandEvent = ACTIVE_GRAND_EVENTS.get(player.serverLevel().dimension());
        String grandActive = grandEvent == null || grandEvent.ended()
                ? "none"
                : "warden(tracked=" + grandEvent.trackedPlayers().size()
                        + ",latched=" + grandEvent.latchedPlayers().size()
                        + ",attack=" + (grandEvent.attackTarget() != null)
                        + ",sinking=" + grandEvent.sinking()
                        + ")";
        String activeWeatherId = state.getActiveWeatherEventId();
        String mainPoolCooldowns = summarizeCooldownPool(EVENT_COOLDOWNS.get(player.getUUID()), now, 5);
        String ambientPoolCooldowns = summarizeCooldownPool(AMBIENT_EVENT_COOLDOWNS.get(player.getUUID()), now, 5);
        String specialPoolCooldowns = summarizeCooldownPool(SPECIAL_ENTITY_COOLDOWNS.get(player.getUUID()), now, 5);

        boolean forcedBySilence = isForcedBySilence(state, phase, profile, danger, now);
        double autoChance = getAutoTriggerChance(phase, profile, danger);
        double ambientChance = getAmbientTriggerChance(phase, profile, danger);

        return "Auto-event debug | phase=" + state.getCurrentPhaseIndex()
                + " | systemEnabled=" + state.isSystemEnabled()
                + " | profile=" + profile
                + " | danger=" + danger
                + " | chancePerCheck=" + String.format(Locale.ROOT, "%.2f%%", autoChance * 100.0D)
                + " | ambientChance=" + String.format(Locale.ROOT, "%.2f%%", ambientChance * 100.0D)
                + " | checkInterval=random(" + MIN_AUTO_CHECK_INTERVAL_TICKS + "-" + MAX_AUTO_CHECK_INTERVAL_TICKS + "t)"
                + " | nextCheck=" + nextCheckRemaining + "t"
                + " | globalCd=" + ticksToSeconds(globalRemainingTicks) + "s"
                + " | ambientCd=" + ticksToSeconds(ambientRemainingTicks) + "s"
                + " | respawnGrace=" + ticksToSeconds(respawnRemainingTicks) + "s"
                + " | specialNextCheck=" + ticksToSeconds(specialNextCheck) + "s"
                + " | specialCd=" + ticksToSeconds(specialRemainingTicks) + "s"
                + " | weatherActive=" + (activeWeatherId == null || activeWeatherId.isBlank() ? "none" : activeWeatherId)
                + " | weatherActiveCd=" + ticksToSeconds(weatherActiveRemaining) + "s"
                + " | weatherNextCheck=" + ticksToSeconds(weatherNextCheckRemaining) + "s"
                + " | weatherCd=" + ticksToSeconds(weatherCooldownRemaining) + "s"
                + " | tensionActive=" + tensionActive
                + " | tensionCd=" + ticksToSeconds(tensionRemaining) + "s"
                + " | tensionNext=" + ticksToSeconds(tensionNextRemaining) + "s"
                + " | grandBoostCd=" + ticksToSeconds(grandBoostRemaining) + "s"
                + " | grandNextRoll=" + ticksToSeconds(grandNextRollRemaining) + "s"
                + " | grandActive=" + grandActive
                + " | mainPoolCdKeys=" + mainPoolCooldowns
                + " | ambientPoolCdKeys=" + ambientPoolCooldowns
                + " | specialPoolCdKeys=" + specialPoolCooldowns
                + " | forcedBySilence=" + forcedBySilence
                + " | activeBlackout=" + ACTIVE_BLACKOUTS.containsKey(player.getUUID())
                + " | activeFootsteps=" + ACTIVE_FOOTSTEPS.containsKey(player.getUUID())
                + " | activeFlash=" + ACTIVE_FLASH_EVENTS.containsKey(player.getUUID());
    }

    public static String debugForceRandomSpecialRoll(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return "Special debug roll unavailable: server is null.";
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled()) {
            return "Special debug roll blocked: system disabled.";
        }

        if (shouldBlockSpecialSpawn(player)) {
            return "Special debug roll blocked: player is in water/bubble or in a boat.";
        }

        UncannyPhase phase = state.getPhase();
        if (phase.index() < UncannyPhase.PHASE_2.index()) {
            return "Special debug roll blocked: phase " + phase.index() + " (requires phase 2+).";
        }

        long now = server.getTickCount();
        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        long specialGlobalCooldownTicks = computeSpecialEntityGlobalCooldownTicks(phase, profile, danger);
        List<EventChoice> choices = buildSpecialEntityChoices(player, phase, profile, danger, now, true);
        if (choices.isEmpty()) {
            return "Special debug roll found no eligible candidates (phase/profile/danger/location constraints).";
        }

        String triggeredKey = triggerSpecialChoicePool(
                player,
                choices,
                now,
                phase,
                profile,
                danger,
                specialGlobalCooldownTicks,
                true);

        if (triggeredKey != null) {
            return "Special debug roll success: " + triggeredKey
                    + " | phase=" + phase.index()
                    + " | profile=" + profile
                    + " | danger=" + danger
                    + " | cooldown=" + ticksToSeconds(specialGlobalCooldownTicks) + "s";
        }

        return "Special debug roll failed: candidates were rolled but all spawns failed. Check debug logs for spawn-position diagnostics.";
    }

    private static void maybeTriggerIndependentLivingOre(ServerPlayer player, long now, UncannyPhase phase) {
        if (phase.index() < UncannyPhase.PHASE_3.index()) {
            return;
        }
        if (player.getServer() == null) {
            return;
        }
        UncannyWorldState worldState = UncannyWorldState.get(player.getServer());
        if (isTensionBuilderEventPauseActive(player.serverLevel(), worldState, now)) {
            debugLog(
                    "LIVING_ORE suppressed reason=tension_builder player={} remaining={}s",
                    playerLabel(player),
                    ticksToSeconds(worldState.getTensionBuilderEndTick() - now));
            return;
        }
        if (LIVING_ORE_PRIMED.containsKey(player.getUUID())) {
            return;
        }
        if (player.serverLevel().canSeeSky(player.blockPosition())) {
            return;
        }

        long nextCheck = NEXT_LIVING_ORE_CHECK_TICKS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now < nextCheck) {
            return;
        }
        NEXT_LIVING_ORE_CHECK_TICKS.put(player.getUUID(), now + 80L + player.getRandom().nextInt(81));

        long cooldownUntil = LIVING_ORE_COOLDOWN_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now < cooldownUntil) {
            return;
        }

        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        double phaseFactor = switch (phase) {
            case PHASE_1, PHASE_2 -> 0.0D;
            case PHASE_3 -> 1.0D;
            case PHASE_4 -> 1.25D;
        };
        double chance = Mth.clamp((0.12D + (profile - 1) * 0.06D + danger * 0.025D) * phaseFactor, 0.08D, 0.62D);
        if (player.serverLevel().random.nextDouble() > chance) {
            return;
        }

        if (!triggerLivingOre(player, true)) {
            return;
        }

        int cooldownSeconds = Math.max(30, 95 - (profile - 1) * 12 - danger * 6);
        LIVING_ORE_COOLDOWN_UNTIL.put(player.getUUID(), now + cooldownSeconds * 20L);
        debugLog(
                "LIVING_ORE independent-trigger player={} phase={} profile={} danger={} chance={} cooldown={}s",
                playerLabel(player),
                phase.index(),
                profile,
                danger,
                String.format(Locale.ROOT, "%.3f", chance),
                cooldownSeconds);
    }

    private static void tickTensionBuilder(ServerLevel level, UncannyWorldState state, long now, UncannyPhase phase) {
        if (state.getTensionBuilderLastUpdateTick() == now) {
            return;
        }
        state.setTensionBuilderLastUpdateTick(now);

        if (phase.index() < UncannyPhase.PHASE_2.index()) {
            if (state.getTensionBuilderEndTick() != Long.MIN_VALUE
                    || state.getTensionBuilderNextStartTick() != Long.MIN_VALUE
                    || state.getTensionBuilderGrandEventBoostUntilTick() != Long.MIN_VALUE
                    || state.getTensionBuilderNextGrandEventRollTick() != Long.MIN_VALUE) {
                state.setTensionBuilderEndTick(Long.MIN_VALUE);
                state.setTensionBuilderNextStartTick(Long.MIN_VALUE);
                state.setTensionBuilderGrandEventBoostUntilTick(Long.MIN_VALUE);
                state.setTensionBuilderNextGrandEventRollTick(Long.MIN_VALUE);
            }
            return;
        }

        long activeUntil = state.getTensionBuilderEndTick();
        if (activeUntil != Long.MIN_VALUE && now >= activeUntil) {
            int breakSeconds = rollRangeInclusive(level, TENSION_BREAK_MIN_SECONDS, TENSION_BREAK_MAX_SECONDS);
            int boostSeconds = rollRangeInclusive(level, GRAND_EVENT_BOOST_MIN_SECONDS, GRAND_EVENT_BOOST_MAX_SECONDS);
            state.setTensionBuilderEndTick(Long.MIN_VALUE);
            state.setTensionBuilderNextStartTick(now + breakSeconds * 20L);
            state.setTensionBuilderGrandEventBoostUntilTick(now + boostSeconds * 20L);
            state.setTensionBuilderNextGrandEventRollTick(now + rollGrandEventRollDelayTicks(level));
            debugLog(
                    "TENSION end now={} nextStartIn={}s grandBoostIn={}s",
                    now,
                    breakSeconds,
                    boostSeconds);
        }

        if (!isTensionBuilderActive(state, now)) {
            long nextStart = state.getTensionBuilderNextStartTick();
            if (nextStart == Long.MIN_VALUE) {
                int firstDelay = rollRangeInclusive(level, TENSION_BREAK_MIN_SECONDS, TENSION_BREAK_MAX_SECONDS);
                state.setTensionBuilderNextStartTick(now + firstDelay * 20L);
                debugLog("TENSION scheduled nextStartIn={}s", firstDelay);
            } else if (now >= nextStart) {
                int durationSeconds = rollRangeInclusive(level, TENSION_BUILDER_MIN_SECONDS, TENSION_BUILDER_MAX_SECONDS);
                state.setTensionBuilderEndTick(now + durationSeconds * 20L);
                state.setTensionBuilderNextStartTick(Long.MIN_VALUE);
                debugLog("TENSION start now={} duration={}s", now, durationSeconds);
            }
        }

        maybeRollGrandEvent(level, state, now, phase);
    }

    private static void maybeRollGrandEvent(ServerLevel level, UncannyWorldState state, long now, UncannyPhase phase) {
        if (phase.index() < UncannyPhase.PHASE_3.index() || isTensionBuilderActive(state, now)) {
            return;
        }

        long boostUntil = state.getTensionBuilderGrandEventBoostUntilTick();
        boolean boosted = boostUntil != Long.MIN_VALUE && now <= boostUntil;
        if (boostUntil != Long.MIN_VALUE && now > boostUntil) {
            state.setTensionBuilderGrandEventBoostUntilTick(Long.MIN_VALUE);
        }

        long nextRoll = state.getTensionBuilderNextGrandEventRollTick();
        if (nextRoll == Long.MIN_VALUE) {
            state.setTensionBuilderNextGrandEventRollTick(now + rollGrandEventRollDelayTicks(level));
            return;
        }
        if (now < nextRoll) {
            return;
        }

        long cooldownTicks = GRAND_EVENT_BASE_COOLDOWN_SECONDS * 20L;
        long lastGrandEvent = state.getTensionBuilderLastGrandEventTick();
        if (lastGrandEvent != Long.MIN_VALUE && now < lastGrandEvent + cooldownTicks) {
            long nextDelay = rollGrandEventRollDelayTicks(level);
            state.setTensionBuilderNextGrandEventRollTick(now + nextDelay);
            return;
        }

        double chance = boosted ? GRAND_EVENT_POST_TENSION_CHANCE : GRAND_EVENT_BASE_CHANCE;
        double roll = level.random.nextDouble();
        if (roll <= chance) {
            if (startGrandEventWarden(level, now, false)) {
                state.setTensionBuilderLastGrandEventTick(now);
                state.setTensionBuilderGrandEventBoostUntilTick(Long.MIN_VALUE);
                state.setTensionBuilderNextGrandEventRollTick(now + cooldownTicks);
                debugLog(
                        "GRAND_EVENT started now={} boosted={} roll={} chance={} nextRollIn={}s",
                        now,
                        boosted,
                        String.format(Locale.ROOT, "%.5f", roll),
                        String.format(Locale.ROOT, "%.5f", chance),
                        ticksToSeconds(cooldownTicks));
            } else {
                long nextDelay = 60L * 20L;
                state.setTensionBuilderNextGrandEventRollTick(now + nextDelay);
                debugLog(
                        "GRAND_EVENT rolled-but-failed now={} boosted={} roll={} chance={} nextRollIn={}s",
                        now,
                        boosted,
                        String.format(Locale.ROOT, "%.5f", roll),
                        String.format(Locale.ROOT, "%.5f", chance),
                        ticksToSeconds(nextDelay));
            }
            return;
        }

        long nextDelay = rollGrandEventRollDelayTicks(level);
        state.setTensionBuilderNextGrandEventRollTick(now + nextDelay);
        debugLog(
                "GRAND_EVENT roll boosted={} chance={} roll={} nextRollIn={}s",
                boosted,
                String.format(Locale.ROOT, "%.5f", chance),
                String.format(Locale.ROOT, "%.5f", roll),
                ticksToSeconds(nextDelay));
    }

    private static boolean startGrandEventWarden(ServerLevel level, long now, boolean forcedByCommand) {
        ResourceKey<Level> dimension = level.dimension();
        GrandEventState existing = ACTIVE_GRAND_EVENTS.get(dimension);
        if (existing != null && !existing.ended()) {
            return false;
        }

        List<ServerPlayer> eligiblePlayers = new ArrayList<>();
        for (ServerPlayer candidate : level.players()) {
            if (!candidate.isSpectator() && candidate.isAlive()) {
                eligiblePlayers.add(candidate);
            }
        }
        if (eligiblePlayers.isEmpty()) {
            debugLog("GRAND_EVENT start-fail reason=no-eligible-players dim={}", dimension.location());
            return false;
        }

        ServerPlayer anchor = eligiblePlayers.get(level.random.nextInt(eligiblePlayers.size()));
        BlockPos spawnPos = findGrandWardenSpawnPos(level, anchor, forcedByCommand);
        if (spawnPos == null) {
            debugLog("GRAND_EVENT start-fail reason=no-spawn-pos anchor={}", playerLabel(anchor));
            return false;
        }

        Warden warden = EntityType.WARDEN.create(level);
        if (warden == null) {
            debugLog("GRAND_EVENT start-fail reason=warden-create-null anchor={}", playerLabel(anchor));
            return false;
        }

        warden.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, anchor.getYRot() + 180.0F, 0.0F);
        warden.setNoAi(false);
        warden.setTarget(null);
        warden.setInvulnerable(false);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        warden.setPersistenceRequired();
        warden.addTag(GRAND_WARDEN_TAG);
        applyGrandWardenRenderMarkerName(warden);
        applyGrandWardenStepUp(warden, now);

        BlockPos anchorPos = anchor.blockPosition().immutable();
        if (!level.addFreshEntity(warden)) {
            debugLog("GRAND_EVENT start-fail reason=add-entity-failed anchor={} pos={}", playerLabel(anchor), spawnPos);
            return false;
        }
        // Root fix: prime vanilla DIG cooldown immediately on spawn to prevent
        // the DIGGING activity from discarding the Warden before the event flow.
        preventGrandWardenDigBeforeEnd(warden, now);
        // Best-effort only: never reject start on this first navigation hint.
        // Non-aggro invariant: priming must be anchor-driven (never focus/player-position driven).
        primeGrandEventWardenNavigation(level, warden, anchorPos, anchor);

        Set<UUID> trackedPlayers = new HashSet<>();
        boolean coveredAtStart = hasAnyNonAirAbove(level, anchorPos);
        long zoneSqr = (long) GRAND_WARDEN_ZONE_RADIUS * GRAND_WARDEN_ZONE_RADIUS;
        for (ServerPlayer candidate : level.players()) {
            if (!candidate.isAlive() || candidate.isSpectator()) {
                continue;
            }
            if (candidate.blockPosition().distSqr(anchorPos) <= zoneSqr) {
                trackedPlayers.add(candidate.getUUID());
            }
        }
        if (trackedPlayers.isEmpty()) {
            trackedPlayers.add(anchor.getUUID());
        }

        GrandEventState state = new GrandEventState(now, warden.getUUID(), anchorPos, trackedPlayers, coveredAtStart);
        for (UUID playerId : trackedPlayers) {
            ServerPlayer tracked = level.getServer().getPlayerList().getPlayer(playerId);
            if (tracked != null && tracked.serverLevel() == level) {
                state.setLastKnownPosition(playerId, tracked.position());
            }
        }
        ACTIVE_GRAND_EVENTS.put(dimension, state);
        debugLog("GRAND_EVENT pause_auto on dim={}", dimension.location());
        beginGrandEventEmerging(level, state, warden, now);
        playGrandEventWarningPulse(level, state, warden, now, true);
        debugLog(
                "GRAND_EVENT_RUNTIME start id={} build={} class={} startTick={} dim={} warden={} spawn={} anchor={} trackedPlayers={}",
                state.runtimeId(),
                state.buildSignature(),
                GRAND_EVENT_RUNTIME_CLASS,
                state.startedTick(),
                dimension.location(),
                warden.getStringUUID(),
                spawnPos,
                playerLabel(anchor),
                trackedPlayers.size());
        debugLog(
                "GRAND_EVENT start dim={} anchor={} spawn={} trackedPlayers={} coveredAtStart={}",
                dimension.location(),
                playerLabel(anchor),
                spawnPos,
                trackedPlayers.size(),
                coveredAtStart);
        return true;
    }

    private static BlockPos findGrandWardenSpawnPos(ServerLevel level, ServerPlayer anchor, boolean forcedByCommand) {
        boolean coveredAnchor = hasAnyNonAirAbove(level, anchor.blockPosition());
        debugLog(
                "GRAND_EVENT spawn-profile context=initial anchor={} covered={} forced={}",
                playerLabel(anchor),
                coveredAnchor,
                forcedByCommand);
        if (coveredAnchor) {
            return findGrandWardenCoveredSpawnPos(level, anchor, forcedByCommand);
        }
        return findGrandWardenOpenSkySpawnPos(
                level,
                anchor,
                forcedByCommand,
                GRAND_WARDEN_SPAWN_MIN_DISTANCE,
                GRAND_WARDEN_SPAWN_MAX_DISTANCE,
                8,
                -4);
    }

    private static BlockPos findGrandWardenRecoverySpawnPos(ServerLevel level, GrandEventState state) {
        ServerPlayer pivot = resolveGrandEventFallbackTarget(level, state);
        if (pivot == null) {
            return null;
        }
        boolean coveredPivot = hasAnyNonAirAbove(level, pivot.blockPosition());
        debugLog(
                "GRAND_EVENT spawn-profile context=recovery anchor={} covered={} forced=false",
                playerLabel(pivot),
                coveredPivot);
        if (coveredPivot) {
            return findGrandWardenCoveredSpawnPos(level, pivot, false);
        }
        return findGrandWardenOpenSkySpawnPos(
                level,
                pivot,
                false,
                Math.max(24, GRAND_WARDEN_SPAWN_MIN_DISTANCE - 4),
                GRAND_WARDEN_SPAWN_MAX_DISTANCE + 8,
                10,
                -6);
    }

    private static BlockPos findGrandWardenCoveredSpawnPos(ServerLevel level, ServerPlayer pivot, boolean forcedByCommand) {
        int[] yTolerances = {GRAND_WARDEN_COVERED_PRIMARY_MAX_Y_DELTA, GRAND_WARDEN_COVERED_FALLBACK_MAX_Y_DELTA};
        for (int maxYDelta : yTolerances) {
            for (int attempt = 0; attempt < 42; attempt++) {
                BlockPos candidate = findSpawnBehindPlayer(
                        level,
                        pivot,
                        GRAND_WARDEN_COVERED_SPAWN_MIN_DISTANCE,
                        GRAND_WARDEN_COVERED_SPAWN_MAX_DISTANCE,
                        false);
                if (isGrandWardenSpawnCandidateValid(level, pivot, candidate, forcedByCommand, true, maxYDelta, 0, 0)) {
                    return candidate.immutable();
                }
            }
            for (int attempt = 0; attempt < 26; attempt++) {
                BlockPos candidate = findSpawnAroundPlayer(
                        level,
                        pivot,
                        GRAND_WARDEN_COVERED_SPAWN_MIN_DISTANCE,
                        GRAND_WARDEN_COVERED_SPAWN_MAX_DISTANCE,
                        false);
                if (isGrandWardenSpawnCandidateValid(level, pivot, candidate, forcedByCommand, true, maxYDelta, 0, 0)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    private static BlockPos findGrandWardenOpenSkySpawnPos(
            ServerLevel level,
            ServerPlayer pivot,
            boolean forcedByCommand,
            int minDistance,
            int maxDistance,
            int maxUpDelta,
            int maxDownDelta) {
        for (int attempt = 0; attempt < 48; attempt++) {
            BlockPos candidate = findSpawnAroundPlayer(level, pivot, minDistance, maxDistance, false);
            if (isGrandWardenSpawnCandidateValid(level, pivot, candidate, forcedByCommand, false, 0, maxUpDelta, maxDownDelta)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static boolean isGrandWardenSpawnCandidateValid(
            ServerLevel level,
            ServerPlayer pivot,
            BlockPos candidate,
            boolean forcedByCommand,
            boolean requireCovered,
            int maxAbsYDelta,
            int maxUpDelta,
            int maxDownDelta) {
        if (candidate == null || !isSpecialSpawnContextValid(level, pivot, candidate)) {
            return false;
        }

        boolean candidateCovered = hasAnyNonAirAbove(level, candidate);
        if (requireCovered && !candidateCovered) {
            return false;
        }
        if (!requireCovered && candidateCovered) {
            return false;
        }

        int yDelta = candidate.getY() - pivot.blockPosition().getY();
        if (requireCovered) {
            if (Math.abs(yDelta) > maxAbsYDelta) {
                return false;
            }
        } else if (yDelta > maxUpDelta || yDelta < maxDownDelta) {
            return false;
        }

        Vec3 center = Vec3.atCenterOf(candidate);
        if (!forcedByCommand && !isOutsideViewCone(pivot, center, 0.10D)) {
            return false;
        }
        if (!forcedByCommand && !isOutOfSightOfOtherPlayers(level, pivot, center)) {
            return false;
        }
        return true;
    }

    private static void tickActiveGrandEvent(ServerLevel level, long now) {
        GrandEventState state = ACTIVE_GRAND_EVENTS.get(level.dimension());
        if (state == null || state.lastProcessedTick() == now || state.ended()) {
            return;
        }
        state.setLastProcessedTick(now);

        Entity raw = level.getEntity(state.wardenUuid());
        Warden warden = null;
        if (raw instanceof Warden existing && existing.isAlive()) {
            warden = existing;
        } else {
            boolean inProtectionWindow = now < state.startedTick() + GRAND_EVENT_NON_AGGRO_MIN_RUNTIME_TICKS;
            if (inProtectionWindow && state.recoveryCount() < GRAND_EVENT_MAX_RECOVERIES) {
                Warden recovered = recoverGrandEventWarden(level, state, now);
                if (recovered != null) {
                    warden = recovered;
                }
            }
            if (warden == null) {
                debugLog(
                        "GRAND_EVENT cleanup_reason=warden_missing runtime={} dim={}",
                        state.runtimeId(),
                        level.dimension().location());
                finishGrandEvent(level, state, false, now, "warden_missing");
                return;
            }
        }
        applyGrandWardenStepUp(warden, now);

        ServerPlayer runtimeFocus = resolveGrandEventFocus(level, state);
        if (runtimeFocus == null) {
            runtimeFocus = resolveGrandEventFallbackTarget(level, state);
        }
        logGrandEventRuntimeEntered("tickActiveGrandEvent", state, level, warden, runtimeFocus, now);

        List<ServerPlayer> zonePlayers = gatherGrandEventZonePlayers(level, state.anchorPos());
        tickGrandEventPausedSpecials(level, state, now);
        pruneTrackedGrandEventPlayers(level, state, zonePlayers);
        ServerPlayer lockedAttackTarget = resolveGrandEventCurrentAttackTarget(level, state);
        boolean attackLockActive = isGrandEventAttackLockActive(level, state, warden, lockedAttackTarget);
        if (attackLockActive) {
            ensureGrandEventAttackTargetTracked(state, zonePlayers, lockedAttackTarget);
        }
        if (state.trackedPlayers().isEmpty() && !attackLockActive) {
            if (!state.exiting()) {
                debugLog(
                        "GRAND_EVENT cleanup_reason=no_valid_players runtime={} dim={} anchor={}",
                        state.runtimeId(),
                        level.dimension().location(),
                        state.anchorPos());
            }
            enterGrandEventExitAuthority(level, state, warden, now, "no_valid_players");
            if (!state.sinking()) {
                startGrandEventSinking(state, now, GRAND_EVENT_EMPTY_SCOPE_SINK_TICKS);
            }
        }

        tickGrandEventWarnings(level, state, zonePlayers, warden, now);
        applyGrandEventDarkness(zonePlayers);
        if (state.emerging()) {
            tickGrandEventEmerging(level, state, warden, zonePlayers, now);
            return;
        }

        if (!attackLockActive && now >= state.startedTick() + GRAND_EVENT_MAX_DURATION_TICKS) {
            debugLog(
                    "GRAND_EVENT cleanup_reason=event_finished runtime={} dim={} duration={}s",
                    state.runtimeId(),
                    level.dimension().location(),
                    ticksToSeconds(now - state.startedTick()));
            enterGrandEventExitAuthority(level, state, warden, now, "event_finished");
            if (!state.sinking()) {
                startGrandEventSinking(state, now, GRAND_EVENT_SINK_DURATION_TICKS);
            }
        }

        if (state.sinking()) {
            clearGrandEventAggroTuning(state, warden, now);
            if (state.sinking()) {
                tickGrandEventSinking(level, state, zonePlayers, warden, now);
            }
            return;
        }

        if (state.exiting()) {
            if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                debugLog(
                        "GRAND_EVENT search_suppressed_in_exit=true runtime={} source=tick_active reason=exit_authority_lock",
                        state.runtimeId());
                debugLog(
                        "GRAND_EVENT exit_nav_owner=retreat_only runtime={} sinking={}",
                        state.runtimeId(),
                        state.sinking());
            }
            tickGrandEventExit(level, state, zonePlayers, warden, now);
            return;
        }

        stabilizeGrandWardenState(warden, resolveGrandEventFallbackTarget(level, state), now);
        if (state.attackTarget() == null) {
            clearGrandEventAggroTuning(state, warden, now);
            ServerPlayer vanillaSoundTrigger = pickGrandEventVanillaSoundTrigger(level, state, warden, now);
            if (vanillaSoundTrigger != null) {
                beginGrandEventAttack(level, state, zonePlayers, warden, vanillaSoundTrigger, now, "sound");
                return;
            }

            Entity unsolicited = warden.getTarget();
            Optional<LivingEntity> attackMemory = warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
            if (unsolicited != null || attackMemory.isPresent()) {
                debugLog(
                        "GRAND_EVENT combat_intent_cleared reason=no_admitted_trigger runtime={} hardTarget={} attackMemory={} dim={}",
                        state.runtimeId(),
                        unsolicited == null ? "none" : unsolicited.getStringUUID(),
                        attackMemory.map(living -> living.getStringUUID()).orElse("none"),
                        level.dimension().location());
                warden.setTarget(null);
                warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                Optional<LivingEntity> angryAt = warden.getEntityAngryAt();
                if (angryAt.isPresent()) {
                    warden.clearAnger(angryAt.get());
                }
            }
        }

        if (state.attackTarget() != null) {
            tickGrandEventAttack(level, state, zonePlayers, warden, now);
            return;
        }

        ServerPlayer triggerPlayer = pickGrandEventTriggeringPlayer(level, state, now);
        if (triggerPlayer != null) {
            beginGrandEventAttack(level, state, zonePlayers, warden, triggerPlayer, now, "movement");
            return;
        }

        ServerPlayer focus = resolveGrandEventFocus(level, state);
        boolean allowExit = now >= state.startedTick() + GRAND_EVENT_NON_AGGRO_MIN_RUNTIME_TICKS;
        if (focus != null) {
            boolean finishedFocusSearch = tickGrandEventApproach(level, state, warden, focus, now, true);
            if (finishedFocusSearch) {
                state.markLatched(focus.getUUID());
            }
        } else {
            ServerPlayer fallback = resolveGrandEventFallbackTarget(level, state);
            if (fallback != null) {
                tickGrandEventApproach(level, state, warden, fallback, now, false);
            } else if (allowExit) {
                debugLog(
                        "GRAND_EVENT cleanup_reason=no_focus_in_scope runtime={} dim={}",
                        state.runtimeId(),
                        level.dimension().location());
                enterGrandEventExitAuthority(level, state, warden, now, "no_focus_in_scope");
                if (!state.sinking()) {
                    startGrandEventSinking(state, now, GRAND_EVENT_EMPTY_SCOPE_SINK_TICKS);
                }
            }
        }

        if (allowExit && !state.exiting() && state.allTrackedPlayersLatched(level)) {
            int validNodes = state.spatiallyValidConsumedSearchNodes();
            int visitedSectors = state.visitedSearchSectorsCount();
            int uniqueSubzones = state.uniqueSearchSubzonesLast60s(now);
            double coverageRadius = state.maxConsumedRadiusLast60s(now);
            boolean activityBudgetMet = validNodes >= GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_CONSUMED_NODES
                    && visitedSectors >= GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_VISITED_SECTORS
                    && uniqueSubzones >= GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_UNIQUE_SUBZONES
                    && coverageRadius >= GRAND_EVENT_NON_AGGRO_ACTIVITY_MIN_RADIUS_60S;
            boolean timeoutBypass = now >= state.startedTick()
                    + GRAND_EVENT_NON_AGGRO_MIN_RUNTIME_TICKS
                    + GRAND_EVENT_NON_AGGRO_ACTIVITY_TIMEOUT_TICKS;
            debugLog(
                    "GRAND_EVENT progress_gate runtime={} minTime={} nodesValid={} sectors={} subzones={} maxRadius60s={} budgetMet={} timeoutBypass={} tracked={} latched={}",
                    state.runtimeId(),
                    now >= state.startedTick() + GRAND_EVENT_NON_AGGRO_MIN_RUNTIME_TICKS,
                    validNodes,
                    visitedSectors,
                    uniqueSubzones,
                    String.format(Locale.ROOT, "%.2f", coverageRadius),
                    activityBudgetMet,
                    timeoutBypass,
                    state.trackedPlayers().size(),
                    state.latchedPlayers().size());
            if (activityBudgetMet || timeoutBypass) {
                enterGrandEventExitAuthority(level, state, warden, now, activityBudgetMet ? "progress_gate_met" : "progress_gate_timeout");
            }
        }

        if (state.exiting()) {
            tickGrandEventExit(level, state, zonePlayers, warden, now);
        }
    }

    private static boolean enterGrandEventExitAuthority(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            long now,
            String reason) {
        if (state == null || warden == null || state.exiting()) {
            return false;
        }

        boolean hadPending = state.hasPendingIssuedNode();
        boolean hadDisturbance = warden.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION);
        boolean hadWalkTarget = warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        boolean hadPathMemory = warden.getBrain().hasMemoryValue(MemoryModuleType.PATH);
        boolean hadCantReach = warden.getBrain().hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);

        state.setExiting(true);
        state.clearIssuedIntent();
        state.clearSearchFocus();

        warden.setTarget(null);
        warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        warden.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.PATH);
        warden.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        Optional<LivingEntity> angryAt = warden.getEntityAngryAt();
        if (angryAt.isPresent()) {
            warden.clearAnger(angryAt.get());
        }
        warden.getNavigation().stop();

        debugLog(
                "GRAND_EVENT pending_cleared_on_exit=true runtime={} source={} hadPending={} hadDisturbance={} hadWalkTarget={} hadPathMemory={} hadCantReach={} dim={}",
                state.runtimeId(),
                reason == null ? "unspecified" : reason,
                hadPending,
                hadDisturbance,
                hadWalkTarget,
                hadPathMemory,
                hadCantReach,
                level.dimension().location());
        debugLog(
                "GRAND_EVENT search_suppressed_in_exit=true runtime={} source={} reason=exit_authority_lock",
                state.runtimeId(),
                reason == null ? "unspecified" : reason);
        debugLog(
                "GRAND_EVENT exit_nav_owner=retreat_only runtime={} reason={}",
                state.runtimeId(),
                reason == null ? "unspecified" : reason);
        return true;
    }

    private static Warden recoverGrandEventWarden(ServerLevel level, GrandEventState state, long now) {
        BlockPos spawnPos = findGrandWardenRecoverySpawnPos(level, state);
        if (spawnPos == null) {
            return null;
        }

        Warden warden = EntityType.WARDEN.create(level);
        if (warden == null) {
            return null;
        }

        warden.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        warden.setNoAi(false);
        warden.setTarget(null);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        warden.setInvulnerable(false);
        warden.setPersistenceRequired();
        warden.addTag(GRAND_WARDEN_TAG);
        applyGrandWardenRenderMarkerName(warden);
        applyGrandWardenStepUp(warden, now);
        if (!level.addFreshEntity(warden)) {
            return null;
        }
        preventGrandWardenDigBeforeEnd(warden, now);

        ServerPlayer pivot = resolveGrandEventFallbackTarget(level, state);
        if (pivot != null) {
            // Best-effort only on recovery; keep anchor-driven priming.
            primeGrandEventWardenNavigation(level, warden, state.anchorPos(), pivot);
        }

        state.incrementRecoveryCount();
        state.setWardenUuid(warden.getUUID());
        beginGrandEventEmerging(level, state, warden, now);
        debugLog(
                "GRAND_EVENT recovered dim={} count={} spawn={} now={}",
                level.dimension().location(),
                state.recoveryCount(),
                spawnPos,
                now);
        return warden;
    }

    private static void beginGrandEventEmerging(ServerLevel level, GrandEventState state, Warden warden, long now) {
        if (state == null || warden == null) {
            return;
        }
        state.startEmerging(now + GRAND_EVENT_EMERGE_DURATION_TICKS);
        warden.setNoAi(true);
        warden.setTarget(null);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        if (WARDEN_EMERGE_POSE != null) {
            try {
                warden.setPose(WARDEN_EMERGE_POSE);
            } catch (Throwable ignored) {
                warden.setPose(Pose.STANDING);
            }
        }
        List<ServerPlayer> zonePlayers = gatherGrandEventZonePlayers(level, state.anchorPos());
        for (ServerPlayer player : zonePlayers) {
            playLocalSoundAt(
                    player,
                    warden.blockPosition(),
                    SoundEvents.WARDEN_AGITATED,
                    SoundSource.HOSTILE,
                    2.2F,
                    0.78F + level.random.nextFloat() * 0.08F);
        }
        debugLog(
                "GRAND_EVENT emerge-start runtime={} warden={} endTick={} dim={}",
                state.runtimeId(),
                warden.getStringUUID(),
                state.emergeEndTick(),
                level.dimension().location());
    }

    private static void tickGrandEventEmerging(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            List<ServerPlayer> zonePlayers,
            long now) {
        if (state == null || warden == null) {
            return;
        }
        preventGrandWardenDigBeforeEnd(warden, now);
        warden.setNoAi(true);
        warden.setTarget(null);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        if (now < state.emergeEndTick()) {
            return;
        }
        state.stopEmerging();
        if (WARDEN_EMERGE_POSE != null && warden.getPose() == WARDEN_EMERGE_POSE) {
            warden.setPose(Pose.STANDING);
        }
        warden.setNoAi(false);
        for (ServerPlayer player : zonePlayers) {
            playLocalSoundAt(
                    player,
                    warden.blockPosition(),
                    SoundEvents.WARDEN_NEARBY_CLOSE,
                    SoundSource.HOSTILE,
                    1.7F,
                    0.92F + level.random.nextFloat() * 0.08F);
        }
        debugLog(
                "GRAND_EVENT emerge-finish runtime={} warden={} tick={} dim={}",
                state.runtimeId(),
                warden.getStringUUID(),
                now,
                level.dimension().location());
    }

    private static boolean primeGrandEventWardenNavigation(
            ServerLevel level,
            Warden warden,
            BlockPos anchorPos,
            ServerPlayer scopePlayer) {
        if (warden == null || anchorPos == null) {
            return false;
        }
        ServerPlayer context = scopePlayer;
        if (context == null) {
            Player nearest = level.getNearestPlayer(
                    anchorPos.getX() + 0.5D,
                    anchorPos.getY() + 0.5D,
                    anchorPos.getZ() + 0.5D,
                    GRAND_WARDEN_ZONE_RADIUS,
                    false);
            if (nearest instanceof ServerPlayer serverPlayer) {
                context = serverPlayer;
            }
        }
        if (context == null) {
            return false;
        }
        int baseY = Mth.clamp(anchorPos.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = level.random.nextDouble() * (Math.PI * 2.0D);
            double distance = 10.0D + (level.random.nextDouble() * 16.0D);
            int x = Mth.floor(anchorPos.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(anchorPos.getZ() + Math.sin(angle) * distance);
            BlockPos candidate = findGrandEventSearchCandidateWithTolerance(level, x, z, baseY, true);
            if (candidate == null) {
                continue;
            }
            if (issueGrandEventSearchNode(
                    level,
                    null,
                    warden,
                    context,
                    candidate,
                    "prime_anchor",
                    warden.distanceToSqr(Vec3.atCenterOf(anchorPos)),
                    true)) {
                return true;
            }
        }
        return false;
    }

    private static List<ServerPlayer> gatherGrandEventZonePlayers(ServerLevel level, BlockPos anchorPos) {
        List<ServerPlayer> players = new ArrayList<>();
        if (anchorPos == null) {
            return players;
        }
        long radiusSqr = (long) GRAND_WARDEN_ZONE_RADIUS * GRAND_WARDEN_ZONE_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (player.blockPosition().distSqr(anchorPos) <= radiusSqr) {
                players.add(player);
            }
        }
        return players;
    }

    private static void pruneTrackedGrandEventPlayers(ServerLevel level, GrandEventState state, List<ServerPlayer> zonePlayers) {
        Set<UUID> onlineZone = new HashSet<>();
        for (ServerPlayer player : zonePlayers) {
            onlineZone.add(player.getUUID());
        }
        List<UUID> removed = new ArrayList<>();
        state.trackedPlayers().removeIf(playerId -> {
            if (onlineZone.contains(playerId)) {
                return false;
            }
            ServerPlayer tracked = level.getServer().getPlayerList().getPlayer(playerId);
            if (playerId.equals(state.attackTarget())
                    && tracked != null
                    && tracked.serverLevel() == level
                    && tracked.isAlive()
                    && !tracked.isSpectator()) {
                return false;
            }
            String rejectReason = getGrandEventScopeRejectReason(level, state, tracked);
            boolean shouldRemove = tracked == null || rejectReason != null;
            if (shouldRemove) {
                removed.add(playerId);
                if (tracked != null) {
                    logGrandEventScopeDecision(
                            "prune_tracked",
                            state,
                            tracked,
                            "rejected",
                            rejectReason == null ? "offline_or_missing" : rejectReason,
                            tracked.blockPosition().distSqr(state.anchorPos()));
                }
            }
            return shouldRemove;
        });
        for (UUID removedId : removed) {
            state.removeLastKnownPosition(removedId);
        }
    }

    private static void tickGrandEventWarnings(
            ServerLevel level,
            GrandEventState state,
            List<ServerPlayer> zonePlayers,
            Warden warden,
            long now) {
        if (zonePlayers.isEmpty()) {
            return;
        }
        if (state.nextMessageTick() <= now && state.messageIndex() < GRAND_EVENT_WARNING_LINES.size()) {
            String line = GRAND_EVENT_WARNING_LINES.get(state.messageIndex());
            state.setMessageIndex(state.messageIndex() + 1);
            state.setNextMessageTick(now + GRAND_EVENT_MESSAGE_INTERVAL_TICKS);
            broadcastGrandEventTitle(zonePlayers, line);

            SoundEvent voice = resolveGrandEventVoiceSound(line);
            for (ServerPlayer player : zonePlayers) {
                playLocalSoundAt(
                        player,
                        player.blockPosition(),
                        voice,
                        SoundSource.AMBIENT,
                        2.8F,
                        1.0F);
            }
        }

        if (now >= state.nextHeavySoundTick()) {
            if (state.attackTarget() == null) {
                playGrandEventWarningPulse(level, state, warden, now, false);
            } else {
                state.setNextHeavySoundTick(now + rollRangeInclusive(level, GRAND_EVENT_HEAVY_SOUND_MIN_TICKS, GRAND_EVENT_HEAVY_SOUND_MAX_TICKS));
            }
        }
    }

    private static void playGrandEventWarningPulse(ServerLevel level, GrandEventState state, Warden warden, long now, boolean immediate) {
        List<ServerPlayer> zonePlayers = gatherGrandEventZonePlayers(level, state.anchorPos());
        if (!zonePlayers.isEmpty()) {
            for (ServerPlayer player : zonePlayers) {
                playLocalSoundAt(
                        player,
                        warden.blockPosition(),
                        SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE,
                        immediate ? 2.8F : 2.2F,
                        0.70F + level.random.nextFloat() * 0.10F);
                if (!immediate && level.random.nextDouble() < 0.35D) {
                    playLocalSoundAt(
                            player,
                            warden.blockPosition(),
                            SoundEvents.WARDEN_NEARBY_CLOSE,
                            SoundSource.HOSTILE,
                            1.8F,
                            0.85F + level.random.nextFloat() * 0.10F);
                }
            }
        }
        state.setNextHeavySoundTick(now + rollRangeInclusive(level, GRAND_EVENT_HEAVY_SOUND_MIN_TICKS, GRAND_EVENT_HEAVY_SOUND_MAX_TICKS));
    }

    private static SoundEvent resolveGrandEventVoiceSound(String line) {
        if (line == null) {
            return UncannySoundRegistry.UNCANNY_GRANDEVENT_IT_IS_HERE.get();
        }
        if (line.startsWith("DON'T MOVE")) {
            return UncannySoundRegistry.UNCANNY_GRANDEVENT_DONT_MOVE.get();
        }
        if (line.startsWith("DON'T MAKE A SOUND")) {
            return UncannySoundRegistry.UNCANNY_GRANDEVENT_DONT_MAKE_A_SOUND.get();
        }
        return UncannySoundRegistry.UNCANNY_GRANDEVENT_IT_IS_HERE.get();
    }

    private static void broadcastGrandEventTitle(List<ServerPlayer> players, String line) {
        Component text = Component.literal(line).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer player : players) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(4, 24, 8));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            player.connection.send(new ClientboundSetTitleTextPacket(text));
        }
    }

    private static void applyGrandEventDarkness(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, true, false, false));
        }
    }

    private static ServerPlayer resolveGrandEventCurrentAttackTarget(ServerLevel level, GrandEventState state) {
        if (level == null || state == null || state.attackTarget() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(state.attackTarget());
    }

    private static boolean isGrandEventAttackLockActive(ServerLevel level, GrandEventState state, Warden warden, ServerPlayer target) {
        if (level == null || state == null || warden == null || target == null || state.attackTarget() == null) {
            return false;
        }
        if (target.serverLevel() != level || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        return warden.distanceToSqr(target) <= GRAND_EVENT_ATTACK_RELEASE_DISTANCE_SQR;
    }

    private static void ensureGrandEventAttackTargetTracked(GrandEventState state, List<ServerPlayer> zonePlayers, ServerPlayer target) {
        if (state == null || target == null) {
            return;
        }
        UUID targetId = target.getUUID();
        state.trackedPlayers().add(targetId);
        boolean alreadyInZoneList = false;
        for (ServerPlayer candidate : zonePlayers) {
            if (candidate.getUUID().equals(targetId)) {
                alreadyInZoneList = true;
                break;
            }
        }
        if (!alreadyInZoneList) {
            zonePlayers.add(target);
        }
    }

    private static boolean isPlayerWithinGrandEventScope(ServerLevel level, GrandEventState state, ServerPlayer player) {
        return getGrandEventScopeRejectReason(level, state, player) == null;
    }

    private static String getGrandEventScopeRejectReason(ServerLevel level, GrandEventState state, ServerPlayer player) {
        if (player == null) {
            return "null_player";
        }
        if (level == null || state == null) {
            return "null_state";
        }
        if (player.serverLevel() != level) {
            return "wrong_dimension";
        }
        if (!player.isAlive() || player.isSpectator()) {
            return "invalid_player_state";
        }
        long radiusSqr = (long) GRAND_WARDEN_ZONE_RADIUS * GRAND_WARDEN_ZONE_RADIUS;
        double distToAnchorSqr = player.blockPosition().distSqr(state.anchorPos());
        if (distToAnchorSqr > radiusSqr) {
            return "out_of_scope";
        }
        return null;
    }

    private static void logGrandEventScopeDecision(
            String context,
            GrandEventState state,
            ServerPlayer player,
            String decision,
            String reason,
            double distToAnchorSqr) {
        debugLog(
                "GRAND_EVENT scope context={} runtime={} player={} decision={} reason={} distToAnchor={} radius={}",
                context,
                state == null ? "none" : state.runtimeId(),
                player == null ? "none" : playerLabel(player),
                decision,
                reason == null ? "none" : reason,
                String.format(Locale.ROOT, "%.2f", Math.sqrt(Math.max(0.0D, distToAnchorSqr))),
                GRAND_WARDEN_ZONE_RADIUS);
    }

    private static boolean hasAnyValidGrandEventTrackedPlayer(ServerLevel level, GrandEventState state) {
        if (level == null || state == null) {
            return false;
        }
        for (UUID playerId : state.trackedPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (getGrandEventScopeRejectReason(level, state, player) == null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGrandEventTrackedPlayer(ServerLevel level, GrandEventState state, ServerPlayer player) {
        if (player == null || level == null || state == null) {
            return false;
        }
        if (!state.trackedPlayers().contains(player.getUUID())) {
            return false;
        }
        String rejectReason = getGrandEventScopeRejectReason(level, state, player);
        if (rejectReason != null) {
            logGrandEventScopeDecision(
                    "tracked_check",
                    state,
                    player,
                    "rejected",
                    rejectReason,
                    player.blockPosition().distSqr(state.anchorPos()));
            return false;
        }
        return true;
    }

    private static ServerPlayer pickGrandEventVanillaSoundTrigger(ServerLevel level, GrandEventState state, Warden warden, long now) {
        if (warden == null) {
            return null;
        }

        Entity target = warden.getTarget();
        if (target instanceof ServerPlayer player
                && isGrandEventTrackedPlayer(level, state, player)
                && isGrandEventVanillaSoundAdmissible(level, state, player, now, "target")) {
            debugLog("GRAND_EVENT trigger reason=vanilla_sound target={}", playerLabel(player));
            return player;
        }

        Optional<LivingEntity> memoryTarget = warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (memoryTarget.isPresent()
                && memoryTarget.get() instanceof ServerPlayer player
                && isGrandEventTrackedPlayer(level, state, player)
                && isGrandEventVanillaSoundAdmissible(level, state, player, now, "memory")) {
            debugLog("GRAND_EVENT trigger reason=vanilla_sound_memory target={}", playerLabel(player));
            return player;
        }
        return null;
    }

    private static boolean isGrandEventVanillaSoundAdmissible(
            ServerLevel level,
            GrandEventState state,
            ServerPlayer player,
            long now,
            String source) {
        if (level == null || state == null || player == null) {
            return false;
        }
        Long lastAudibleTick = GRAND_EVENT_RECENT_AUDIBLE_ACTION_TICKS.get(player.getUUID());
        long delta = lastAudibleTick == null ? Long.MAX_VALUE : now - lastAudibleTick;
        double distToAnchor = Math.sqrt(player.blockPosition().distSqr(state.anchorPos()));
        if (lastAudibleTick != null && delta >= 0L && delta <= GRAND_EVENT_AUDIBLE_ACTION_WINDOW_TICKS) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog(
                        "GRAND_EVENT trigger admit reason=recent_player_audible source={} target={} delta={}t distToAnchor={}",
                        source,
                        playerLabel(player),
                        delta,
                        String.format(Locale.ROOT, "%.2f", distToAnchor));
            }
            return true;
        }
        debugLog(
                "GRAND_EVENT trigger rejected reason=no_recent_player_sound source={} target={} sinceLastAudible={}t distToAnchor={}",
                source,
                playerLabel(player),
                lastAudibleTick == null ? -1L : delta,
                String.format(Locale.ROOT, "%.2f", distToAnchor));
        return false;
    }

    private static ServerPlayer pickGrandEventTriggeringPlayer(ServerLevel level, GrandEventState state, long now) {
        for (UUID playerId : state.trackedPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            String scopeRejectReason = getGrandEventScopeRejectReason(level, state, player);
            if (scopeRejectReason != null) {
                if (shouldSampleGrandEventRuntime(now) && player != null) {
                    logGrandEventScopeDecision(
                            "movement_trigger",
                            state,
                            player,
                            "rejected",
                            scopeRejectReason,
                            player.blockPosition().distSqr(state.anchorPos()));
                }
                continue;
            }

            Vec3 previous = state.lastKnownPosition(playerId);
            Vec3 current = player.position();
            state.setLastKnownPosition(playerId, current);
            if (previous == null) {
                state.setLastMovementSampleTick(playerId, now);
                continue;
            }

            double movementDelta = Math.sqrt(horizontalDistanceSqr(previous, current));
            long lastSampleTick = state.lastMovementSampleTick(playerId);
            long sampleDeltaTicks = lastSampleTick == Long.MIN_VALUE ? 1L : Math.max(1L, now - lastSampleTick);
            state.setLastMovementSampleTick(playerId, now);

            double accumulator = state.movementAccumulator(playerId);
            if (player.isShiftKeyDown()) {
                if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                    debugLog(
                            "GRAND_EVENT movement-sample player={} crouching=true delta={} accumulator={} decision=ignored reason=sneak distToAnchor={}",
                            playerLabel(player),
                            String.format(Locale.ROOT, "%.4f", movementDelta),
                            String.format(Locale.ROOT, "%.4f", accumulator),
                            String.format(Locale.ROOT, "%.2f", Math.sqrt(player.blockPosition().distSqr(state.anchorPos()))));
                }
                state.clearMovementTracking(playerId);
                continue;
            }

            boolean ignoredByEnvironment = player.isInWaterOrBubble() || player.isPassenger();
            boolean ignoredByDamage = player.hurtTime > 0 && player.hurtTime <= GRAND_EVENT_KNOCKBACK_GRACE_TICKS;
            if (ignoredByEnvironment || ignoredByDamage) {
                accumulator = Math.max(0.0D, accumulator - GRAND_EVENT_MOVEMENT_IGNORED_DECAY_BLOCKS * sampleDeltaTicks);
                state.setMovementAccumulator(playerId, accumulator);
                if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                    String ignoredReason = ignoredByEnvironment ? "environment" : "knockback_guard";
                    debugLog(
                            "GRAND_EVENT movement-sample player={} crouching=false delta={} accumulator={} decision=ignored reason={} hurtTime={} invul={} distToAnchor={}",
                            playerLabel(player),
                            String.format(Locale.ROOT, "%.4f", movementDelta),
                            String.format(Locale.ROOT, "%.4f", accumulator),
                            ignoredReason,
                            player.hurtTime,
                            player.invulnerableTime,
                            String.format(Locale.ROOT, "%.2f", Math.sqrt(player.blockPosition().distSqr(state.anchorPos()))));
                }
                continue;
            }

            if (movementDelta >= GRAND_EVENT_MOVEMENT_MIN_STEP_BLOCKS) {
                accumulator += movementDelta;
            } else {
                accumulator = Math.max(0.0D, accumulator - GRAND_EVENT_MOVEMENT_IDLE_DECAY_BLOCKS * sampleDeltaTicks);
            }
            state.setMovementAccumulator(playerId, accumulator);

            if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                String decision = movementDelta >= GRAND_EVENT_MOVEMENT_MIN_STEP_BLOCKS ? "accepted" : "ignored";
                String reason = movementDelta >= GRAND_EVENT_MOVEMENT_MIN_STEP_BLOCKS ? "movement" : "below_step";
                debugLog(
                        "GRAND_EVENT movement-sample player={} crouching=false delta={} accumulator={} decision={} reason={} hurtTime={} invul={} distToAnchor={}",
                        playerLabel(player),
                        String.format(Locale.ROOT, "%.4f", movementDelta),
                        String.format(Locale.ROOT, "%.4f", accumulator),
                        decision,
                        reason,
                        player.hurtTime,
                        player.invulnerableTime,
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(player.blockPosition().distSqr(state.anchorPos()))));
            }

            if (accumulator >= GRAND_EVENT_MOVEMENT_TRIGGER_BLOCKS) {
                debugLog(
                        "GRAND_EVENT movement-trigger player={} delta={} accumulator={} threshold={}",
                        playerLabel(player),
                        String.format(Locale.ROOT, "%.4f", movementDelta),
                        String.format(Locale.ROOT, "%.4f", accumulator),
                        String.format(Locale.ROOT, "%.4f", GRAND_EVENT_MOVEMENT_TRIGGER_BLOCKS));
                state.clearMovementTracking(playerId);
                return player;
            }
        }
        return null;
    }

    private static ServerPlayer resolveGrandEventFocus(ServerLevel level, GrandEventState state) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (UUID playerId : state.trackedPlayers()) {
            if (state.latchedPlayers().contains(playerId)) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            String scopeRejectReason = getGrandEventScopeRejectReason(level, state, player);
            if (scopeRejectReason != null) {
                if (player != null) {
                    logGrandEventScopeDecision(
                            "focus",
                            state,
                            player,
                            "rejected",
                            scopeRejectReason,
                            player.blockPosition().distSqr(state.anchorPos()));
                }
                continue;
            }
            double distance = player.position().distanceToSqr(Vec3.atCenterOf(state.anchorPos()));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        if (best != null) {
            logGrandEventScopeDecision(
                    "focus",
                    state,
                    best,
                    "accepted",
                    "in_scope",
                    best.blockPosition().distSqr(state.anchorPos()));
        }
        return best;
    }

    private static ServerPlayer resolveGrandEventFallbackTarget(ServerLevel level, GrandEventState state) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 wardenAnchor = Vec3.atCenterOf(state.anchorPos());
        for (UUID playerId : state.trackedPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            String scopeRejectReason = getGrandEventScopeRejectReason(level, state, player);
            if (scopeRejectReason != null) {
                if (player != null) {
                    logGrandEventScopeDecision(
                            "fallback",
                            state,
                            player,
                            "rejected",
                            scopeRejectReason,
                            player.blockPosition().distSqr(state.anchorPos()));
                }
                continue;
            }
            double distance = player.position().distanceToSqr(wardenAnchor);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        if (best != null) {
            logGrandEventScopeDecision(
                    "fallback",
                    state,
                    best,
                    "accepted",
                    "in_scope",
                    best.blockPosition().distSqr(state.anchorPos()));
        }
        return best;
    }

    private static ServerPlayer resolveNearestGrandEventInScopePlayer(ServerLevel level, GrandEventState state, Warden warden) {
        if (level == null || state == null || warden == null) {
            return null;
        }
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (UUID playerId : state.trackedPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (getGrandEventScopeRejectReason(level, state, player) != null) {
                continue;
            }
            double dist = warden.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    private static boolean tickGrandEventApproach(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            long now,
            boolean allowCompletion) {
        logGrandEventRuntimeEntered("tickGrandEventApproach", state, level, warden, focus, now);
        if (state.exiting()) {
            if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                debugLog(
                        "GRAND_EVENT search_suppressed_in_exit=true runtime={} source=approach reason=exit_authority_lock",
                        state.runtimeId());
                debugLog(
                        "GRAND_EVENT exit_nav_owner=retreat_only runtime={} source=approach",
                        state.runtimeId());
            }
            return false;
        }
        warden.setNoAi(false);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        tickGrandEventCavePathBreak(level, state, warden, null, now);

        UUID focusId = focus.getUUID();
        if (state.searchFocusId() == null) {
            state.setSearchFocusId(focusId);
            state.setSearchEndTick(now + rollRangeInclusive(level, GRAND_EVENT_SEARCH_MIN_TICKS, GRAND_EVENT_SEARCH_MAX_TICKS));
            state.setNextCrossTick(now);
            state.setCrossEndTick(Long.MIN_VALUE);
            state.setSearchAngleDegrees(level.random.nextFloat() * 360.0F);
            state.resetSearchNoPathStreak();
            if (state.searchRecoveryModeActive()) {
                state.setSearchRecoveryModeActive(false);
            }
            state.resetSearchSectorCoverage();
            state.startNewSniffCycle();
            state.scheduleNextSearchSniffTick(level, now);
            debugLog("GRAND_EVENT search-start target={} dim={}", playerLabel(focus), level.dimension().location());
            debugLog(
                    "GRAND_EVENT approach_mode=anchor_only runtime={} target={} dim={}",
                    state.runtimeId(),
                    playerLabel(focus),
                    level.dimension().location());
            debugLog("GRAND_EVENT sniff_pass=disabled_contact runtime={} target={}", state.runtimeId(), playerLabel(focus));
            debugLog("GRAND_EVENT search_sniff=enabled_anchor runtime={} target={}", state.runtimeId(), playerLabel(focus));
            debugLog("GRAND_EVENT close_pass=disabled runtime={} target={}", state.runtimeId(), playerLabel(focus));
        }

        double distToFocusSqr = warden.distanceToSqr(focus); // monitor-only
        double distToAnchorSqr = warden.distanceToSqr(Vec3.atCenterOf(state.anchorPos()));
        ServerPlayer nearestInScopePlayer = resolveNearestGrandEventInScopePlayer(level, state, warden);
        double nearestInScopeDistSqr = nearestInScopePlayer == null ? Double.MAX_VALUE : warden.distanceToSqr(nearestInScopePlayer);
        state.sampleNonAggroMobility(warden.position(), now);
        double moved5s = state.lastCompletedNonAggroMoved5s();
        double moved10s = state.lastCompletedNonAggroMoved10s();
        long stagnationTicks = state.nonAggroStagnationTicks(now);
        boolean nonAggroStagnating = state.isNonAggroStagnating(now);
        boolean localOrbiting = state.isLocalOrbiting(now);
        Vec3 delta = warden.getDeltaMovement();
        Vec3 horizontalDelta = new Vec3(delta.x, 0.0D, delta.z);
        double horizontalSpeed = horizontalDelta.length();
        Vec3 lookHorizontal = new Vec3(warden.getLookAngle().x, 0.0D, warden.getLookAngle().z);
        double yawDeltaMove = (horizontalSpeed > 0.001D && lookHorizontal.lengthSqr() > 0.001D)
                ? angleBetweenHorizontalDegrees(horizontalDelta, lookHorizontal)
                : -1.0D;
        state.sampleSpinInPlace(warden.getYRot(), horizontalSpeed);
        boolean spinInPlaceDetected = state.isSpinInPlaceDetected();
        boolean inCloseContact = nearestInScopePlayer != null && nearestInScopeDistSqr <= GRAND_EVENT_CLOSE_CONTACT_DISTANCE_SQR;
        boolean hardContactGuard = state.attackTarget() == null
                && nearestInScopePlayer != null
                && nearestInScopeDistSqr <= GRAND_EVENT_NON_AGGRO_HARD_CONTACT_DISTANCE_SQR;
        boolean softSeparationGuard = state.attackTarget() == null
                && nearestInScopePlayer != null
                && nearestInScopeDistSqr <= GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_DISTANCE_SQR;
        boolean guardLockoutActive = state.isNonAggroGuardLockoutActive(now);
        boolean separationRequested = hardContactGuard || (softSeparationGuard && !guardLockoutActive);
        if (inCloseContact && !state.closeContactActive()) {
            state.setCloseContactActive(true);
            state.setLastCloseEncounterTick(now);
            debugLog(
                    "GRAND_EVENT close_contact edge_enter runtime={} target={} dist={}",
                    state.runtimeId(),
                    nearestInScopePlayer == null ? "none" : playerLabel(nearestInScopePlayer),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(nearestInScopeDistSqr)));
        } else if (!inCloseContact && state.closeContactActive()) {
            state.setCloseContactActive(false);
            debugLog(
                    "GRAND_EVENT close_contact edge_exit runtime={} target={} dist={}",
                    state.runtimeId(),
                    nearestInScopePlayer == null ? "none" : playerLabel(nearestInScopePlayer),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(nearestInScopeDistSqr)));
        }
        if (hardContactGuard) {
            state.setLastHardContactGuardTick(now);
            Optional<LivingEntity> angryAt = warden.getEntityAngryAt();
            if (angryAt.isPresent()) {
                warden.clearAnger(angryAt.get());
            }
            warden.setTarget(null);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            if (WARDEN_SNIFF_POSE != null && warden.getPose() == WARDEN_SNIFF_POSE) {
                warden.setPose(Pose.STANDING);
            }
            BlockPos anchor = state.anchorPos();
            warden.getLookControl().setLookAt(anchor.getX() + 0.5D, anchor.getY() + 0.5D, anchor.getZ() + 0.5D);
        }
        if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
            String guardType = hardContactGuard ? "hard" : (softSeparationGuard ? "soft" : "none");
            String guardAction;
            if (hardContactGuard) {
                guardAction = guardLockoutActive ? "scrub" : "separate";
            } else if (softSeparationGuard) {
                guardAction = guardLockoutActive ? "none" : "separate";
            } else {
                guardAction = "none";
            }
            debugLog(
                    "GRAND_EVENT guard.non_aggro type={} action={} distNearest={} lockout={} nearest={}",
                    guardType,
                    guardAction,
                    nearestInScopePlayer == null ? "n/a" : String.format(Locale.ROOT, "%.2f", Math.sqrt(nearestInScopeDistSqr)),
                    guardLockoutActive,
                    nearestInScopePlayer == null ? "none" : playerLabel(nearestInScopePlayer));
            debugLog(
                    "GRAND_EVENT mobility.progress runtime={} moved5s={} moved10s={} stagnationTicks={} localOrbiting={}",
                    state.runtimeId(),
                    String.format(Locale.ROOT, "%.2f", moved5s),
                    String.format(Locale.ROOT, "%.2f", moved10s),
                    stagnationTicks,
                    localOrbiting);
            debugLog(
                    "GRAND_EVENT coverage60s sectorsVisited={} maxRadius={} uniqueSubzones={} nodeChanges60s={} runtime={}",
                    state.visitedSearchSectorsCount(),
                    String.format(Locale.ROOT, "%.2f", state.maxConsumedRadiusLast60s(now)),
                    state.uniqueSearchSubzonesLast60s(now),
                    state.searchNodeChangesLast60s(now),
                    state.runtimeId());
            if (localOrbiting) {
                debugLog(
                        "GRAND_EVENT orbiting_detected=true reason=low_extension_high_node_churn runtime={} subzones={} maxRadius={} consecutiveMicro={}",
                        state.runtimeId(),
                        state.uniqueSearchSubzonesLast60s(now),
                        String.format(Locale.ROOT, "%.2f", state.maxConsumedRadiusLast60s(now)),
                        state.consecutiveMicroLoopNodes());
            }
            if (spinInPlaceDetected) {
                debugLog(
                        "GRAND_EVENT spin_in_place detected=true yawDeltaMove={} speed={} ticks={} runtime={}",
                        yawDeltaMove < 0.0D ? "n/a" : String.format(Locale.ROOT, "%.2f", yawDeltaMove),
                        String.format(Locale.ROOT, "%.3f", horizontalSpeed),
                        state.spinInPlaceTicks(),
                        state.runtimeId());
            }
        }
        maybeTickGrandEventSearchSniff(
                level,
                state,
                warden,
                focus,
                nearestInScopePlayer,
                nearestInScopeDistSqr,
                hardContactGuard,
                softSeparationGuard,
                guardLockoutActive,
                nonAggroStagnating || localOrbiting,
                now);
        boolean veryFarFromFocus = distToAnchorSqr > GRAND_EVENT_ANCHOR_APPROACH_VERY_FAR_DISTANCE_SQR;
        boolean farFromFocus = distToAnchorSqr > GRAND_EVENT_ANCHOR_APPROACH_FAR_DISTANCE_SQR;
        boolean hasDisturbance = warden.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION);
        boolean hasWalkTarget = warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        boolean hasPathMemory = warden.getBrain().hasMemoryValue(MemoryModuleType.PATH);
        boolean hasCantReachSince = warden.getBrain().hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        boolean navDone = warden.getNavigation().isDone();
        String activeActivityName = warden.getBrain().getActiveNonCoreActivity().map(Object::toString).orElse("none");
        String activeActivityLower = activeActivityName.toLowerCase(Locale.ROOT);
        boolean roarSniffLoopPattern = state.attackTarget() == null
                && navDone
                && !hasWalkTarget
                && !hasPathMemory
                && !hasDisturbance
                && (activeActivityLower.contains("roar") || activeActivityLower.contains("sniff"));
        if (roarSniffLoopPattern) {
            if (state.roarSniffStuckSinceTick() == Long.MIN_VALUE) {
                state.setRoarSniffStuckSinceTick(now);
            } else if (now - state.roarSniffStuckSinceTick() >= GRAND_EVENT_ROAR_SNIFF_STUCK_THRESHOLD_TICKS) {
                Optional<LivingEntity> angryAt = warden.getEntityAngryAt();
                if (angryAt.isPresent()) {
                    warden.clearAnger(angryAt.get());
                }
                warden.setTarget(null);
                warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
                warden.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.PATH);
                warden.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                warden.getNavigation().stop();
                state.clearIssuedIntent();
                state.setNextCrossTick(now);
                state.startNewSniffCycle();
                state.setSniffPoseUntilTick(Long.MIN_VALUE);
                state.setNextSniffSoundTick(Long.MIN_VALUE);
                if (WARDEN_SNIFF_POSE != null && warden.getPose() == WARDEN_SNIFF_POSE) {
                    warden.setPose(Pose.STANDING);
                }
                debugLog(
                        "GRAND_EVENT roar_sniff_recovery runtime={} target={} active={} dist={} navDone={} hasWalkTarget={} hasPathMemory={} disturbance=none",
                        state.runtimeId(),
                        playerLabel(focus),
                        activeActivityName,
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)),
                        navDone,
                        hasWalkTarget,
                        hasPathMemory);
                state.setRoarSniffStuckSinceTick(now);
            }
        } else {
            state.setRoarSniffStuckSinceTick(Long.MIN_VALUE);
        }
        boolean intentConsumed = hasWalkTarget || hasPathMemory || !navDone;
        if (intentConsumed) {
            state.setLastIntentConsumedTick(now);
        }
        boolean consumedThisTick = false;
        boolean pendingTimedOut = false;
        boolean pendingProgressStalled = false;
        boolean pendingNavIdleStalled = false;
        boolean pendingCantReachStalled = false;
        boolean pendingEmergencyReissue = false;
        String pendingEmergencyReason = "none";
        if (state.hasPendingIssuedNode()) {
            BlockPos pendingNode = state.pendingIssuedNode();
            long pendingIssuedTick = state.pendingIssuedTick();
            Vec3 pendingIssuedOrigin = state.pendingIssuedOriginPos();
            state.samplePendingPathProgress(
                    pendingNode,
                    warden.position(),
                    hasWalkTarget,
                    hasPathMemory,
                    hasCantReachSince,
                    navDone,
                    now);
            boolean reachedPendingNode = pendingNode != null
                    && warden.distanceToSqr(Vec3.atCenterOf(pendingNode)) <= GRAND_EVENT_LATCH_DISTANCE_SQR;
            double movedFromIssueSqr = pendingIssuedOrigin == null
                    ? 0.0D
                    : horizontalDistanceSqr(pendingIssuedOrigin, warden.position());
            boolean pendingOldEnough = state.pendingAgeTicks(now) >= GRAND_EVENT_PENDING_MIN_LIFETIME_TICKS;
            pendingProgressStalled = pendingOldEnough && state.isPendingProgressStalled(now);
            pendingNavIdleStalled = pendingOldEnough && state.isPendingNavIdleStalled(now);
            pendingCantReachStalled = pendingOldEnough && state.isPendingCantReachStalled(now);
            boolean pendingSpinStalled = pendingOldEnough && spinInPlaceDetected;
            pendingEmergencyReissue = pendingProgressStalled || pendingNavIdleStalled || pendingCantReachStalled || pendingSpinStalled;
            if (pendingCantReachStalled) {
                pendingEmergencyReason = "cant_reach";
            } else if (pendingNavIdleStalled) {
                pendingEmergencyReason = "nav_idle";
            } else if (pendingSpinStalled) {
                pendingEmergencyReason = "spin_in_place";
            } else if (pendingProgressStalled) {
                pendingEmergencyReason = "stalled_path";
            }
            boolean authorityConsumed = intentConsumed
                    && pendingIssuedTick != Long.MIN_VALUE
                    && (now - pendingIssuedTick) >= GRAND_EVENT_INTENT_CONSUME_MIN_AGE_TICKS
                    && movedFromIssueSqr >= GRAND_EVENT_INTENT_CONSUME_MIN_MOVE_SQR;
            if (reachedPendingNode || authorityConsumed) {
                double stepFromLastConsumed = state.markIntentConsumed(now, pendingNode, reachedPendingNode, Math.sqrt(movedFromIssueSqr));
                consumedThisTick = true;
                int consumedSector = resolveGrandEventSearchSector(state.anchorPos(), pendingNode);
                int consumedMicroZone = resolveGrandEventSearchMicroZone(state.anchorPos(), pendingNode);
                debugLog(
                        "GRAND_EVENT search.lifecycle state=consumed reason={} node={} runtime={} tick={} distToAnchor={}",
                        reachedPendingNode ? "reached_node" : "authority_progressed",
                        pendingNode,
                        state.runtimeId(),
                        now,
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToAnchorSqr)));
                debugLog(
                        "GRAND_EVENT node_step distPrevConsumed={} sector={} microzone={} runtime={} node={}",
                        String.format(Locale.ROOT, "%.2f", stepFromLastConsumed),
                        consumedSector,
                        consumedMicroZone,
                        state.runtimeId(),
                        pendingNode);
            } else if (pendingIssuedTick != Long.MIN_VALUE
                    && (now - pendingIssuedTick) >= GRAND_EVENT_INTENT_NONCONSUMED_TIMEOUT_TICKS) {
                pendingTimedOut = true;
                debugLog(
                        "GRAND_EVENT search.lifecycle state=timed_out reason=non_consumed_timeout node={} runtime={} issuedTick={} now={}",
                        pendingNode,
                        state.runtimeId(),
                        pendingIssuedTick,
                        now);
                debugLog(
                        "GRAND_EVENT node_abandon reason=timeout node={} runtime={} age={} distNow={} distBest={} delta={}",
                        pendingNode,
                        state.runtimeId(),
                        state.pendingAgeTicks(now),
                        String.format(Locale.ROOT, "%.2f", state.pendingCurrentDistance()),
                        String.format(Locale.ROOT, "%.2f", state.pendingBestDistance()),
                        String.format(Locale.ROOT, "%.2f", state.pendingDistanceDelta()));
                debugLog(
                        "GRAND_EVENT path_churn state=dropped reason=timeout node={} runtime={}",
                        pendingNode,
                        state.runtimeId());
                state.clearPendingIssuedIntent();
            }
            if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                debugLog(
                        "GRAND_EVENT path_progress pendingNode={} distNow={} distBest={} delta={} stalledTicks={} cantReachStalled={} navIdleStalled={} runtime={}",
                        pendingNode,
                        String.format(Locale.ROOT, "%.2f", state.pendingCurrentDistance()),
                        String.format(Locale.ROOT, "%.2f", state.pendingBestDistance()),
                        String.format(Locale.ROOT, "%.2f", state.pendingDistanceDelta()),
                        state.pendingNoProgressTicks(now),
                        pendingCantReachStalled,
                        pendingNavIdleStalled,
                        state.runtimeId());
            }
        }
        long lastIssuedTick = state.lastIssuedTick();
        long ticksSinceIssue = lastIssuedTick == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0L, now - lastIssuedTick);
        boolean cadenceReady = ticksSinceIssue >= GRAND_EVENT_INTENT_REISSUE_MIN_TICKS;
        boolean scheduledRefresh = now >= state.nextCrossTick();
        boolean noIntentActive = !hasDisturbance || (!hasWalkTarget && !hasPathMemory && navDone);
        boolean pendingNotConsumed = state.hasPendingIssuedNode();
        boolean issueGateAllow = true;
        String issueGateReason = "open";
        if (pendingNotConsumed) {
            if (hardContactGuard) {
                issueGateAllow = true;
                issueGateReason = "hard_contact_escape";
            } else if (pendingEmergencyReissue) {
                issueGateAllow = true;
                issueGateReason = pendingEmergencyReason;
            } else {
                issueGateAllow = false;
                issueGateReason = "pending_hysteresis";
            }
        } else if (separationRequested && guardLockoutActive) {
            issueGateAllow = false;
            issueGateReason = "lockout";
        } else if (pendingTimedOut) {
            issueGateReason = "timeout";
        } else if (consumedThisTick) {
            issueGateReason = "consumed";
        }
        boolean replanCooldownActive = state.isReplanCooldownActive(now);
        boolean replanEmergency = hardContactGuard || pendingTimedOut || pendingEmergencyReissue;
        if (issueGateAllow && replanCooldownActive && !replanEmergency) {
            issueGateAllow = false;
            issueGateReason = "replan_cooldown";
        }

        if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
            debugLog(
                    "GRAND_EVENT search.issue_gate allow={} reason={} pending={} lockout={} stagnation={} localOrbiting={} cadenceReady={} scheduledRefresh={} noIntentActive={} pendingStall={} pendingNavIdle={} pendingCantReach={} replanCooldown={}",
                    issueGateAllow,
                    issueGateReason,
                    pendingNotConsumed,
                    guardLockoutActive,
                    nonAggroStagnating,
                    localOrbiting,
                    cadenceReady,
                    scheduledRefresh,
                    noIntentActive,
                    pendingProgressStalled,
                    pendingNavIdleStalled,
                    pendingCantReachStalled,
                    replanCooldownActive);
            debugLog(
                    "GRAND_EVENT replan_window count={} nearRepeat={} cooldownActive={} cooldownUntil={} allowReissue={} reason={} runtime={}",
                    state.replansLastWindow(now),
                    state.nearRepeatReplansLastWindow(now),
                    replanCooldownActive,
                    state.replanCooldownUntilTick(),
                    issueGateAllow,
                    issueGateReason,
                    state.runtimeId());
            if (state.nearRepeatReplansLastWindow(now) >= 3) {
                debugLog(
                        "GRAND_EVENT node_churn detected=true countWindow={} sameOrNearNode={} runtime={}",
                        state.replansLastWindow(now),
                        state.nearRepeatReplansLastWindow(now),
                        state.runtimeId());
            }
            if (navDone && !hasWalkTarget && !hasPathMemory) {
                String idleReason = guardLockoutActive && softSeparationGuard
                        ? "guard_hold"
                        : hasCantReachSince
                                ? "cant_reach"
                                : hasDisturbance
                                        ? "path_null"
                                        : "no_walktarget";
                debugLog(
                        "GRAND_EVENT authority.nav_idle reason={} runtime={} disturbance={} hasCantReach={} attackTarget={} nearest={}",
                        idleReason,
                        state.runtimeId(),
                        warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION).map(Object::toString).orElse("none"),
                        hasCantReachSince,
                        state.attackTarget() == null ? "none" : state.attackTarget(),
                        nearestInScopePlayer == null ? "none" : playerLabel(nearestInScopePlayer));
            }
        }
        boolean needsNewSearchNode = issueGateAllow
                && (lastIssuedTick == Long.MIN_VALUE
                        || pendingTimedOut
                        || (pendingEmergencyReissue && ticksSinceIssue >= 4L)
                        || (!pendingNotConsumed && localOrbiting && ticksSinceIssue >= 6L)
                        || (!pendingNotConsumed && nonAggroStagnating && ticksSinceIssue >= 8L)
                        || (cadenceReady
                                && (scheduledRefresh
                                        || noIntentActive
                                        || separationRequested
                                        || consumedThisTick
                                        || distToAnchorSqr <= GRAND_EVENT_ANCHOR_LATCH_DISTANCE_SQR)));

        if (needsNewSearchNode) {
            boolean noPathRecoveryMode = state.searchNoPathStreak() >= GRAND_EVENT_NO_PATH_RECOVERY_STREAK_THRESHOLD;
            boolean stagnationRecoveryMode = nonAggroStagnating;
            boolean coverageRecoveryMode = localOrbiting;
            boolean recoveryMode = noPathRecoveryMode || stagnationRecoveryMode || coverageRecoveryMode;
            if (recoveryMode && !state.searchRecoveryModeActive()) {
                state.setSearchRecoveryModeActive(true);
                debugLog(
                        "GRAND_EVENT search.stagnation_recovery on reason={} streak={} target={} dim={} stagnationTicks={} moved5s={} moved10s={}",
                        noPathRecoveryMode
                                ? "no_path_streak"
                                : (stagnationRecoveryMode ? "mobility_stagnation" : "local_orbiting"),
                        state.searchNoPathStreak(),
                        playerLabel(focus),
                        level.dimension().location(),
                        stagnationTicks,
                        String.format(Locale.ROOT, "%.2f", moved5s),
                        String.format(Locale.ROOT, "%.2f", moved10s));
            } else if (!recoveryMode && state.searchRecoveryModeActive()) {
                state.setSearchRecoveryModeActive(false);
                debugLog(
                        "GRAND_EVENT search.stagnation_recovery off reason=path_recovered streak={} target={} dim={}",
                        state.searchNoPathStreak(),
                        playerLabel(focus),
                        level.dimension().location());
            }

            boolean forceApproach = separationRequested
                    || recoveryMode
                    || veryFarFromFocus
                    || (farFromFocus && level.random.nextDouble() < 0.70D);
            boolean sniffPass = false;
            boolean closePass = false;
            boolean crossingSweep = !forceApproach
                    && !sniffPass
                    && now < state.searchEndTick()
                    && level.random.nextDouble() < 0.26D;

            int searchMinRadius;
            int searchMaxRadius;
            String phaseReason;
            boolean aroundAnchor = true;
            boolean oppositeBias = false;
            boolean preferOuterRing = false;
            if (separationRequested) {
                searchMinRadius = GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_MIN_RADIUS;
                searchMaxRadius = GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_MAX_RADIUS;
                phaseReason = "player_separation";
                preferOuterRing = true;
            } else if (coverageRecoveryMode) {
                searchMinRadius = 18;
                searchMaxRadius = 34;
                phaseReason = "coverage_recovery";
                preferOuterRing = true;
            } else if (forceApproach) {
                searchMinRadius = 12;
                searchMaxRadius = 26;
                phaseReason = recoveryMode ? "search_recovery" : "approach";
            } else if (crossingSweep) {
                searchMinRadius = 14;
                searchMaxRadius = 28;
                phaseReason = "cross_sweep";
                preferOuterRing = true;
            } else {
                searchMinRadius = 12;
                searchMaxRadius = 26;
                phaseReason = "search";
            }

            debugLog(
                    "GRAND_EVENT search-radius profile reason={} min={} max={} target={} distToAnchor={} distToFocus={} recovery={}",
                    phaseReason,
                    searchMinRadius,
                    searchMaxRadius,
                    playerLabel(focus),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(distToAnchorSqr)),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)),
                    recoveryMode);
            debugLog(
                    "GRAND_EVENT search.reissue reason={} pending={} ticksSinceIssue={} localOrbiting={} stagnation={} noPathStreak={} runtime={}",
                    issueGateReason,
                    pendingNotConsumed,
                    ticksSinceIssue,
                    localOrbiting,
                    nonAggroStagnating,
                    state.searchNoPathStreak(),
                    state.runtimeId());

            GrandEventSearchResolution primaryResolution = resolveGrandEventSearchTargetPathFirst(
                    level,
                    state,
                    warden,
                    focus,
                    searchMinRadius,
                    searchMaxRadius,
                    phaseReason,
                    aroundAnchor,
                    oppositeBias,
                    preferOuterRing,
                    recoveryMode,
                    false,
                    recoveryMode ? GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS : GRAND_EVENT_SEARCH_POOL_ATTEMPTS);
            GrandEventSearchResolution anchorResolution = null;
            GrandEventSearchResolution relaxedRecentResolution = null;

            BlockPos issuedNode = null;
            String issuedReason = phaseReason;
            int issuedSector = -1;
            double issuedScore = Double.NaN;
            if (primaryResolution.selectedNode() == null) {
                debugLog("GRAND_EVENT search-node-rejected target={} reason=no_node_generated source={}", playerLabel(focus), phaseReason);
            } else if (issueGrandEventSearchNode(
                    level,
                    state,
                    warden,
                    focus,
                    primaryResolution.selectedNode(),
                    phaseReason,
                    distToAnchorSqr,
                    false)) {
                debugLog(
                        "GRAND_EVENT search.lifecycle state=chosen reason={} node={} runtime={} score={}",
                        phaseReason,
                        primaryResolution.selectedNode(),
                        state.runtimeId(),
                        String.format(Locale.ROOT, "%.2f", primaryResolution.selectedScore()));
                issuedNode = primaryResolution.selectedNode().immutable();
                issuedSector = primaryResolution.selectedSector();
                issuedScore = primaryResolution.selectedScore();
            }

            if (issuedNode == null) {
                anchorResolution = resolveGrandEventSearchTargetPathFirst(
                        level,
                        state,
                        warden,
                        focus,
                        14,
                        30,
                        "wide_focus_fallback",
                        true,
                        false,
                        recoveryMode,
                        recoveryMode,
                        false,
                        recoveryMode ? GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS : GRAND_EVENT_SEARCH_POOL_ATTEMPTS);
                if (anchorResolution.selectedNode() != null
                        && issueGrandEventSearchNode(
                                level,
                                state,
                                warden,
                                focus,
                                anchorResolution.selectedNode(),
                                "wide_focus_fallback",
                                distToAnchorSqr,
                                false)) {
                    issuedNode = anchorResolution.selectedNode().immutable();
                    issuedReason = "wide_focus_fallback";
                    issuedSector = anchorResolution.selectedSector();
                    issuedScore = anchorResolution.selectedScore();
                }
            }
            GrandEventSearchResolution emergencyResolution = null;
            if (issuedNode == null && recoveryMode) {
                emergencyResolution = resolveGrandEventSearchTargetPathFirst(
                        level,
                        state,
                        warden,
                        focus,
                        12,
                        30,
                        "anchor_recovery_emergency",
                        true,
                        false,
                        false,
                        true,
                        false,
                        Math.min(64, GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS + 16));
                if (emergencyResolution.selectedNode() != null
                        && issueGrandEventSearchNode(
                                level,
                                state,
                                warden,
                                focus,
                                emergencyResolution.selectedNode(),
                                "anchor_recovery_emergency",
                                distToAnchorSqr,
                                true)) {
                    issuedNode = emergencyResolution.selectedNode().immutable();
                    issuedReason = "anchor_recovery_emergency";
                    issuedSector = emergencyResolution.selectedSector();
                    issuedScore = emergencyResolution.selectedScore();
                }
            }

            int totalPool = primaryResolution.poolSize()
                    + (anchorResolution == null ? 0 : anchorResolution.poolSize())
                    + (emergencyResolution == null ? 0 : emergencyResolution.poolSize());
            int totalReachable = primaryResolution.reachableCount()
                    + (anchorResolution == null ? 0 : anchorResolution.reachableCount())
                    + (emergencyResolution == null ? 0 : emergencyResolution.reachableCount());
            int totalNoPath = primaryResolution.noPathCount()
                    + (anchorResolution == null ? 0 : anchorResolution.noPathCount())
                    + (emergencyResolution == null ? 0 : emergencyResolution.noPathCount());
            int totalContextReject = primaryResolution.contextRejectCount()
                    + (anchorResolution == null ? 0 : anchorResolution.contextRejectCount())
                    + (emergencyResolution == null ? 0 : emergencyResolution.contextRejectCount());
            int totalRecentReject = primaryResolution.recentRejectCount()
                    + (anchorResolution == null ? 0 : anchorResolution.recentRejectCount())
                    + (emergencyResolution == null ? 0 : emergencyResolution.recentRejectCount());
            int totalPlayerReject = primaryResolution.playerDistanceRejectCount()
                    + (anchorResolution == null ? 0 : anchorResolution.playerDistanceRejectCount())
                    + (emergencyResolution == null ? 0 : emergencyResolution.playerDistanceRejectCount());
            boolean noReachableCandidate = totalReachable <= 0;
            boolean recentOverlapOnlyFailure = issuedNode == null
                    && totalPool > 0
                    && totalNoPath == 0
                    && totalContextReject == 0
                    && totalPlayerReject == 0
                    && totalRecentReject > 0;

            if (issuedNode == null && recentOverlapOnlyFailure) {
                if (!state.relaxedRecentModeActive()) {
                    state.setRelaxedRecentModeActive(true);
                    debugLog(
                            "GRAND_EVENT search-relaxed-recent on runtime={} target={} reason=recent_overlap_only pool={} recentReject={}",
                            state.runtimeId(),
                            playerLabel(focus),
                            totalPool,
                            totalRecentReject);
                }
                relaxedRecentResolution = resolveGrandEventSearchTargetPathFirst(
                        level,
                        state,
                        warden,
                        focus,
                        Math.max(searchMinRadius, GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_MIN_RADIUS),
                        Math.max(searchMaxRadius, GRAND_EVENT_NON_AGGRO_FORCE_SEPARATION_MAX_RADIUS),
                        phaseReason + "_relaxed_recent",
                        true,
                        oppositeBias,
                        true,
                        true,
                        true,
                        Math.min(64, (recoveryMode ? GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS : GRAND_EVENT_SEARCH_POOL_ATTEMPTS) + 12));
                if (relaxedRecentResolution.selectedNode() != null
                        && issueGrandEventSearchNode(
                                level,
                                state,
                                warden,
                                focus,
                                relaxedRecentResolution.selectedNode(),
                                phaseReason + "_relaxed_recent",
                                distToAnchorSqr,
                                true)) {
                    issuedNode = relaxedRecentResolution.selectedNode().immutable();
                    issuedReason = phaseReason + "_relaxed_recent";
                    issuedSector = relaxedRecentResolution.selectedSector();
                    issuedScore = relaxedRecentResolution.selectedScore();
                    totalReachable += relaxedRecentResolution.reachableCount();
                }
            } else if (state.relaxedRecentModeActive()) {
                state.setRelaxedRecentModeActive(false);
                debugLog(
                        "GRAND_EVENT search-relaxed-recent off runtime={} target={} reason=normal_path",
                        state.runtimeId(),
                        playerLabel(focus));
            }

            if (crossingSweep) {
                state.setCrossEndTick(now + rollRangeInclusive(level, GRAND_EVENT_CROSS_MIN_TICKS, GRAND_EVENT_CROSS_MAX_TICKS));
            } else {
                state.setCrossEndTick(Long.MIN_VALUE);
            }
            if (issuedNode != null) {
                BlockPos replacedPendingNode = state.hasPendingIssuedNode() ? state.pendingIssuedNode() : null;
                state.resetSearchNoPathStreak();
                state.resetRecentOverlapStallStreak();
                if (state.relaxedRecentModeActive()) {
                    state.setRelaxedRecentModeActive(false);
                    debugLog(
                            "GRAND_EVENT search-relaxed-recent off runtime={} target={} reason=node_issued",
                            state.runtimeId(),
                            playerLabel(focus));
                }
                if (state.searchRecoveryModeActive()) {
                    state.setSearchRecoveryModeActive(false);
                    debugLog(
                            "GRAND_EVENT search.stagnation_recovery off reason=node_issued streak={} target={} dim={}",
                            state.searchNoPathStreak(),
                            playerLabel(focus),
                            level.dimension().location());
                }
                if (replacedPendingNode != null) {
                    debugLog(
                            "GRAND_EVENT path_churn state=replaced reason={} oldNode={} newNode={} runtime={}",
                            issuedReason,
                            replacedPendingNode,
                            issuedNode,
                            state.runtimeId());
                } else {
                    debugLog(
                            "GRAND_EVENT path_churn state=created reason={} node={} runtime={}",
                            issuedReason,
                            issuedNode,
                            state.runtimeId());
                }
                state.markIssuedIntent(issuedNode, now, issuedReason, warden.position());
                state.markSearchNodeIssuedAt(now);
                if (separationRequested) {
                    state.setNonAggroGuardLockoutUntilTick(now + GRAND_EVENT_NON_AGGRO_GUARD_LOCKOUT_TICKS);
                }
                if (issuedSector < 0) {
                    BlockPos sectorOrigin = state.anchorPos();
                    issuedSector = resolveGrandEventSearchSector(sectorOrigin, issuedNode);
                }
                if (issuedSector >= 0) {
                    state.rememberSearchSector(issuedSector);
                    int issuedMicroZone = resolveGrandEventSearchMicroZone(state.anchorPos(), issuedNode);
                    debugLog(
                            "GRAND_EVENT search-sector selected={} coverage={} node={} reason={} target={}",
                            issuedSector,
                            state.searchSectorCoverageString(),
                            issuedNode,
                            issuedReason,
                            playerLabel(focus));
                    debugLog(
                            "GRAND_EVENT search_choice reason={} sector={} microzone={} underVisited={} score={} runtime={}",
                            issuedReason.contains("recovery") ? "recovery_sweep"
                                    : (state.searchSectorVisitCount(issuedSector) <= 1 ? "under_visited_forced" : "balanced"),
                            issuedSector,
                            issuedMicroZone,
                            state.searchSectorVisitCount(issuedSector),
                            String.format(Locale.ROOT, "%.2f", issuedScore),
                            state.runtimeId());
                }
                debugLog(
                        "GRAND_EVENT search.lifecycle state=issued reason={} node={} runtime={} tick={} lockoutUntil={}",
                        issuedReason,
                        issuedNode,
                        state.runtimeId(),
                        now,
                        state.nonAggroGuardLockoutUntilTick());
                debugLog(
                        "GRAND_EVENT search-node-issued target={} node={} reason={} distToAnchor={} distToFocus={} dim={}",
                        playerLabel(focus),
                        issuedNode,
                        issuedReason,
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToAnchorSqr)),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)),
                        level.dimension().location());
                debugLog(
                        "GRAND_EVENT search-radius profile reason={} node={} distToAnchor={} distToFocus={}",
                        issuedReason,
                        issuedNode,
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToAnchorSqr)),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)));
                debugLog(
                        "GRAND_EVENT sweep.coverage sectors={} nodeChanges60s={} runtime={}",
                        state.searchSectorCoverageString(),
                        state.searchNodeChangesLast60s(now),
                        state.runtimeId());
                debugLog(
                        "GRAND_EVENT coverage60s sectorsVisited={} maxRadius={} uniqueSubzones={} runtime={}",
                        state.visitedSearchSectorsCount(),
                        String.format(Locale.ROOT, "%.2f", state.maxConsumedRadiusLast60s(now)),
                        state.uniqueSearchSubzonesLast60s(now),
                        state.runtimeId());
                debugLog(
                        "GRAND_EVENT_RUNTIME intent-issued id={} target={} node={} reason={} tick={} disturbance={} hasWalkTarget={} hasPathMemory={} navDone={} distToAnchor={} distToFocus={}",
                        state.runtimeId(),
                        playerLabel(focus),
                        issuedNode,
                        issuedReason,
                        now,
                        warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION).map(Object::toString).orElse("none"),
                        warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET),
                        warden.getBrain().hasMemoryValue(MemoryModuleType.PATH),
                        warden.getNavigation().isDone(),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToAnchorSqr)),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)));
                if (veryFarFromFocus) {
                    state.setNextCrossTick(now + 12L);
                } else {
                    state.setNextCrossTick(now + rollRangeInclusive(level, GRAND_EVENT_NEXT_CROSS_MIN_TICKS, GRAND_EVENT_NEXT_CROSS_MAX_TICKS));
                }
            } else {
                if (recentOverlapOnlyFailure) {
                    state.incrementRecentOverlapStallStreak();
                    state.resetSearchNoPathStreak();
                    long lastResetTick = state.lastSearchHistoryResetTick();
                    boolean resetCooldownReady = lastResetTick == Long.MIN_VALUE
                            || (now - lastResetTick) >= GRAND_EVENT_RECENT_OVERLAP_RESET_COOLDOWN_TICKS;
                    if (state.recentOverlapStallStreak() >= GRAND_EVENT_RECENT_OVERLAP_STALL_RESET_THRESHOLD && resetCooldownReady) {
                        state.clearRecentSearchHistory();
                        state.setLastSearchHistoryResetTick(now);
                        state.resetRecentOverlapStallStreak();
                        state.setNextCrossTick(now);
                        debugLog(
                                "GRAND_EVENT search-history-reset reason=recent_overlap_stall runtime={} target={} tick={}",
                                state.runtimeId(),
                                playerLabel(focus),
                                now);
                    }
                } else {
                    state.resetRecentOverlapStallStreak();
                    if (state.relaxedRecentModeActive()) {
                        state.setRelaxedRecentModeActive(false);
                        debugLog(
                                "GRAND_EVENT search-relaxed-recent off runtime={} target={} reason=hard_reject",
                                state.runtimeId(),
                                playerLabel(focus));
                    }
                }
                if (!recentOverlapOnlyFailure && noReachableCandidate) {
                    state.incrementSearchNoPathStreak();
                } else {
                    state.resetSearchNoPathStreak();
                }
                state.markIssueFailure(now);
                state.setNextCrossTick(now + 12L);
                debugLog(
                        "GRAND_EVENT search-node-fail target={} wardenPos={} distToAnchor={} distToFocus={} nearestInScope={} dim={}",
                        playerLabel(focus),
                        warden.blockPosition(),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToAnchorSqr)),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)),
                        nearestInScopePlayer == null
                                ? "none"
                                : String.format(Locale.ROOT, "%.2f", Math.sqrt(nearestInScopeDistSqr)),
                        level.dimension().location());
            }
        }

        if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
            logGrandEventInvestigateState(level, warden, focus, now);
            logGrandEventIntentAuthority(level, state, warden, focus, now);
            logGrandEventAuthoritySnapshot(level, state, warden, focus, nearestInScopePlayer, now, hardContactGuard || softSeparationGuard);
        }

        if (!allowCompletion) {
            return false;
        }
        if (now < state.searchEndTick()) {
            return false;
        }

        if (distToAnchorSqr > GRAND_EVENT_ANCHOR_LATCH_DISTANCE_SQR) {
            state.setSearchEndTick(now + rollRangeInclusive(level, GRAND_EVENT_SEARCH_MIN_TICKS / 2, GRAND_EVENT_SEARCH_MAX_TICKS / 2));
            state.setNextCrossTick(now);
            return false;
        }

        boolean canContinueSearching = now < state.startedTick() + (GRAND_EVENT_MIN_RUNTIME_TICKS / 2L);
        if (canContinueSearching || level.random.nextDouble() < 0.35D) {
            state.setSearchEndTick(now + rollRangeInclusive(level, GRAND_EVENT_SEARCH_MIN_TICKS / 2, GRAND_EVENT_SEARCH_MAX_TICKS / 2));
            state.setNextCrossTick(now + rollRangeInclusive(level, GRAND_EVENT_NEXT_CROSS_MIN_TICKS, GRAND_EVENT_NEXT_CROSS_MAX_TICKS));
            return false;
        }

        state.clearSearchFocus();
        debugLog("GRAND_EVENT search-finish target={} dim={}", playerLabel(focus), level.dimension().location());
        return true;
    }

    private static void maybeTickGrandEventSearchSniff(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            ServerPlayer nearestInScopePlayer,
            double nearestInScopeDistSqr,
            boolean hardContactGuard,
            boolean softSeparationGuard,
            boolean guardLockoutActive,
            boolean nonAggroStagnating,
            long now) {
        if (level == null || state == null || warden == null || focus == null || state.attackTarget() != null) {
            return;
        }

        if (state.sniffPoseUntilTick() != Long.MIN_VALUE && now >= state.sniffPoseUntilTick()) {
            if (WARDEN_SNIFF_POSE != null && warden.getPose() == WARDEN_SNIFF_POSE) {
                try {
                    warden.setPose(Pose.STANDING);
                } catch (Throwable ignored) {
                    // Keep runtime-safe for versions where pose transitions are constrained.
                }
            }
            state.setSniffPoseUntilTick(Long.MIN_VALUE);
        }

        long nextSniffTick = state.nextSearchSniffTick();
        if (nextSniffTick == Long.MIN_VALUE) {
            state.scheduleNextSearchSniffTick(level, now);
            nextSniffTick = state.nextSearchSniffTick();
        }
        if (now < nextSniffTick) {
            return;
        }

        String gateReason = "ok";
        boolean allow = true;
        if (hardContactGuard || softSeparationGuard) {
            allow = false;
            gateReason = "guard_active";
        } else if (state.lastHardContactGuardTick() != Long.MIN_VALUE
                && (now - state.lastHardContactGuardTick()) < GRAND_EVENT_SEARCH_SNIFF_POST_HARD_CONTACT_COOLDOWN_TICKS) {
            allow = false;
            gateReason = "recent_hard_contact";
        } else if (guardLockoutActive) {
            allow = false;
            gateReason = "guard_lockout";
        } else if (nonAggroStagnating) {
            allow = false;
            gateReason = "stagnation";
        } else if (nearestInScopePlayer != null && nearestInScopeDistSqr < GRAND_EVENT_SEARCH_SNIFF_MIN_PLAYER_DISTANCE_SQR) {
            allow = false;
            gateReason = "player_too_close";
        }

        debugLog(
                "GRAND_EVENT search_sniff gate allow={} reason={} distNearest={} lockout={} stagnation={}",
                allow,
                gateReason,
                nearestInScopePlayer == null ? "n/a" : String.format(Locale.ROOT, "%.2f", Math.sqrt(nearestInScopeDistSqr)),
                guardLockoutActive,
                nonAggroStagnating);

        if (!allow) {
            debugLog("GRAND_EVENT search_sniff skipped reason={}", gateReason);
            state.setNextSearchSniffTick(now + 40L);
            return;
        }

        BlockPos sniffNode = state.pendingIssuedNode() != null ? state.pendingIssuedNode() : state.lastIssuedNode();
        String source = sniffNode != null ? "sector" : "anchor_sweep";
        Vec3 lookAtPos;
        if (sniffNode != null) {
            lookAtPos = Vec3.atCenterOf(sniffNode);
        } else {
            double angle = Math.toRadians(state.searchAngleDegrees());
            double radius = 9.0D + level.random.nextDouble() * 7.0D;
            BlockPos anchor = state.anchorPos();
            lookAtPos = new Vec3(
                    anchor.getX() + 0.5D + (Math.cos(angle) * radius),
                    anchor.getY() + 0.5D,
                    anchor.getZ() + 0.5D + (Math.sin(angle) * radius));
        }
        warden.getLookControl().setLookAt(lookAtPos.x, lookAtPos.y, lookAtPos.z);

        double facingDeltaNearest = Double.NaN;
        if (nearestInScopePlayer != null) {
            Vec3 toNearest = nearestInScopePlayer.position().subtract(warden.position());
            facingDeltaNearest = angleBetweenHorizontalDegrees(warden.getLookAngle(), toNearest);
        }

        level.playSound(
                null,
                warden.blockPosition(),
                SoundEvents.WARDEN_SNIFF,
                SoundSource.HOSTILE,
                1.35F,
                0.92F + level.random.nextFloat() * 0.10F);

        if (WARDEN_SNIFF_POSE != null && warden.getNavigation().isDone()) {
            try {
                warden.setPose(WARDEN_SNIFF_POSE);
                state.setSniffPoseUntilTick(now + GRAND_EVENT_SEARCH_SNIFF_POSE_TICKS);
            } catch (Throwable ignored) {
                state.setSniffPoseUntilTick(Long.MIN_VALUE);
            }
        }

        state.scheduleNextSearchSniffTick(level, now);
        debugLog(
                "GRAND_EVENT search_sniff fired source={} nearestDist={} facingDeltaNearest={}",
                source,
                nearestInScopePlayer == null ? "n/a" : String.format(Locale.ROOT, "%.2f", Math.sqrt(nearestInScopeDistSqr)),
                Double.isNaN(facingDeltaNearest) ? "n/a" : String.format(Locale.ROOT, "%.2f", facingDeltaNearest));
    }

    private static void tickGrandEventSniffPass(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            long now,
            double distToFocusSqr) {
        if (state == null || warden == null || focus == null) {
            return;
        }

        if (state.sniffPoseUntilTick() != Long.MIN_VALUE && now >= state.sniffPoseUntilTick()) {
            if (WARDEN_SNIFF_POSE != null && warden.getPose() == WARDEN_SNIFF_POSE) {
                warden.setPose(Pose.STANDING);
            }
            state.setSniffPoseUntilTick(Long.MIN_VALUE);
        }

        UUID focusId = focus.getUUID();
        if (!state.isSniffPending(focusId, now)) {
            return;
        }
        if (distToFocusSqr > GRAND_EVENT_SNIFF_TRIGGER_DISTANCE_SQR) {
            return;
        }
        if (!warden.hasLineOfSight(focus)) {
            return;
        }
        if (state.nextSniffSoundTick() != Long.MIN_VALUE && now < state.nextSniffSoundTick()) {
            return;
        }

        List<ServerPlayer> zonePlayers = gatherGrandEventZonePlayers(level, state.anchorPos());
        for (ServerPlayer player : zonePlayers) {
            playLocalSoundAt(
                    player,
                    warden.blockPosition(),
                    SoundEvents.WARDEN_SNIFF,
                    SoundSource.HOSTILE,
                    2.1F,
                    0.92F + level.random.nextFloat() * 0.10F);
        }

        if (WARDEN_SNIFF_POSE != null) {
            try {
                warden.setPose(WARDEN_SNIFF_POSE);
                state.setSniffPoseUntilTick(now + GRAND_EVENT_SNIFF_POSE_TICKS);
            } catch (Throwable ignored) {
                state.setSniffPoseUntilTick(Long.MIN_VALUE);
            }
        }
        warden.getLookControl().setLookAt(focus, 35.0F, 35.0F);
        state.setNextSniffSoundTick(now + GRAND_EVENT_SNIFF_SOUND_COOLDOWN_TICKS);
        state.markSniffedThisCycle(focusId);
        state.clearSniffPending();
        debugLog(
                "GRAND_EVENT sniff-pass triggered target={} dist={} dim={}",
                playerLabel(focus),
                String.format(Locale.ROOT, "%.2f", Math.sqrt(distToFocusSqr)),
                level.dimension().location());

        BlockPos retreat = findGrandEventSearchTargetOpposite(
                level,
                state,
                focus,
                warden,
                14,
                24);
        if (retreat == null) {
            retreat = findGrandEventSearchTarget(
                    level,
                    state,
                    focus,
                    14,
                    24);
        }
        if (retreat != null && issueGrandEventSearchNode(level, state, warden, focus, retreat, "sniff_retreat", distToFocusSqr, false)) {
            state.markIssuedIntent(retreat.immutable(), now, "sniff_retreat", warden.position());
            state.setNextCrossTick(now + rollRangeInclusive(level, GRAND_EVENT_NEXT_CROSS_MIN_TICKS, GRAND_EVENT_NEXT_CROSS_MAX_TICKS));
        }
    }

    private static GrandEventSearchResolution resolveGrandEventSearchTargetPathFirst(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            int minDistance,
            int maxDistance,
            String reason,
            boolean aroundAnchor,
            boolean oppositeBias,
            boolean preferOuterRing,
            boolean relaxedY,
            boolean allowRecentOverlap,
            int poolAttempts) {
        if (level == null || state == null || warden == null || focus == null) {
            return GrandEventSearchResolution.empty();
        }
        int clampedMin = Math.max(1, minDistance);
        int clampedMax = Math.max(clampedMin + 1, maxDistance);
        int attempts = Mth.clamp(poolAttempts, 10, 64);
        float baseAngle = advanceGrandEventSearchAngleForPool(level, state);
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int baseY = aroundAnchor
                ? Mth.clamp(state.anchorPos().getY(), minY, maxY)
                : Mth.clamp(focus.blockPosition().getY(), minY, maxY);
        BlockPos center = aroundAnchor ? state.anchorPos() : focus.blockPosition();
        Set<Long> visited = new HashSet<>();

        int poolSize = 0;
        int reachableCount = 0;
        int noPathCount = 0;
        int contextRejectCount = 0;
        int recentRejectCount = 0;
        int playerDistanceRejectCount = 0;
        int localOrbitRejectCount = 0;
        BlockPos selected = null;
        double selectedScore = Double.NEGATIVE_INFINITY;
        int selectedSector = -1;
        double sectorWidth = 360.0D / GRAND_EVENT_SEARCH_SECTOR_COUNT;
        double sectorJitter = sectorWidth * 0.42D;
        int sectorSeed = Math.floorMod((int) Math.floor(baseAngle / sectorWidth), GRAND_EVENT_SEARCH_SECTOR_COUNT);

        for (int attempt = 0; attempt < attempts; attempt++) {
            int sector = Math.floorMod(sectorSeed + attempt, GRAND_EVENT_SEARCH_SECTOR_COUNT);
            double sectorCenter = baseAngle + (sector * sectorWidth);
            double jitter = (level.random.nextDouble() * (sectorJitter * 2.0D)) - sectorJitter;
            double angle = Math.toRadians(sectorCenter + jitter);
            double distance = sampleGrandEventSearchDistance(level, clampedMin, clampedMax, preferOuterRing);
            int x = Mth.floor(center.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(center.getZ() + Math.sin(angle) * distance);
            BlockPos candidate = findGrandEventSearchCandidateWithTolerance(level, x, z, baseY, relaxedY);
            if (candidate == null) {
                continue;
            }
            if (!visited.add(candidate.asLong())) {
                continue;
            }

            poolSize++;
            String searchContextRejectReason = getGrandEventSearchContextRejectReason(level, state, focus, candidate);
            if (searchContextRejectReason != null) {
                contextRejectCount++;
                debugLog(
                        "GRAND_EVENT search_context rejected reason={} sector={} node={} target={}",
                        searchContextRejectReason,
                        sector,
                        candidate,
                        playerLabel(focus));
                continue;
            }
            if (!allowRecentOverlap && state.isRecentSearchNodeTooClose(candidate, GRAND_EVENT_RECENT_SEARCH_MIN_DISTANCE_SQR)) {
                recentRejectCount++;
                debugLog("GRAND_EVENT search-sector rejected reason=recent_overlap sector={} node={} target={}", sector, candidate, playerLabel(focus));
                continue;
            }
            BlockPos lastConsumedNode = state.lastConsumedSearchNode();
            if (lastConsumedNode != null
                    && candidate.distSqr(lastConsumedNode) < GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_SQR
                    && !allowRecentOverlap) {
                localOrbitRejectCount++;
                debugLog(
                        "GRAND_EVENT search-sector rejected reason=local_orbiting_step sector={} node={} target={} minStep={}",
                        sector,
                        candidate,
                        playerLabel(focus),
                        GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_BLOCKS);
                continue;
            }
            if (aroundAnchor
                    && isGrandEventSearchNodeTooCloseToTrackedPlayers(level, state, candidate, GRAND_EVENT_ANCHOR_SEARCH_MIN_PLAYER_DISTANCE_SQR)) {
                playerDistanceRejectCount++;
                debugLog("GRAND_EVENT search-sector rejected reason=too_close_player sector={} node={} target={}", sector, candidate, playerLabel(focus));
                continue;
            }
            if (warden.getNavigation().createPath(candidate, 0) == null) {
                noPathCount++;
                debugLog("GRAND_EVENT search-sector rejected reason=no_path sector={} node={} target={}", sector, candidate, playerLabel(focus));
                continue;
            }

            reachableCount++;
            double score = scoreGrandEventSearchCandidate(
                    state,
                    warden,
                    focus,
                    candidate,
                    clampedMin,
                    clampedMax,
                    aroundAnchor,
                    oppositeBias,
                    preferOuterRing,
                    sector);
            if (selected == null || score > selectedScore) {
                selected = candidate.immutable();
                selectedScore = score;
                selectedSector = sector;
            }
        }

        debugLog(
                "GRAND_EVENT search-candidate-pool size={} reachable={} target={} reason={} noPath={} contextReject={} recentReject={} orbitReject={} playerReject={} recovery={} relaxedRecent={}",
                poolSize,
                reachableCount,
                playerLabel(focus),
                reason,
                noPathCount,
                contextRejectCount,
                recentRejectCount,
                localOrbitRejectCount,
                playerDistanceRejectCount,
                relaxedY,
                allowRecentOverlap);
        if (selected != null) {
            debugLog(
                    "GRAND_EVENT search-selected node={} score={} ring={} target={}",
                    selected,
                    String.format(Locale.ROOT, "%.2f", selectedScore),
                    reason,
                    playerLabel(focus));
        }
        return new GrandEventSearchResolution(
                selected,
                selectedScore,
                selectedSector,
                poolSize,
                reachableCount,
                noPathCount,
                contextRejectCount,
                recentRejectCount,
                playerDistanceRejectCount);
    }

    private static float advanceGrandEventSearchAngleForPool(ServerLevel level, GrandEventState state) {
        float current = state == null ? level.random.nextFloat() * 360.0F : state.searchAngleDegrees();
        float rotation = 95.0F + ((level.random.nextFloat() * 30.0F) - 15.0F);
        float next = (current + rotation) % 360.0F;
        if (next < 0.0F) {
            next += 360.0F;
        }
        if (state != null) {
            state.setSearchAngleDegrees(next);
        }
        return next;
    }

    private static double sampleGrandEventSearchDistance(ServerLevel level, int minDistance, int maxDistance, boolean preferOuterRing) {
        double span = Math.max(1.0D, maxDistance - minDistance);
        double sample = level.random.nextDouble();
        double weighted = preferOuterRing
                ? 1.0D - Math.pow(1.0D - sample, 2.15D)
                : Math.pow(sample, 1.25D);
        return minDistance + (weighted * span);
    }

    private static BlockPos findGrandEventSearchCandidateWithTolerance(
            ServerLevel level,
            int x,
            int z,
            int baseY,
            boolean relaxedY) {
        if (level == null) {
            return null;
        }
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int[] yOffsets = relaxedY
                ? new int[] {0, 2, -2, 4, -4, 6, -6, 8, -8, 10, -10, 12, -12}
                : new int[] {0, 2, -2, 4, -4, 6, -6};
        int[][] xzOffsets = relaxedY
                ? new int[][] {
                    {0, 0},
                    {1, 0},
                    {-1, 0},
                    {0, 1},
                    {0, -1},
                    {1, 1},
                    {-1, 1},
                    {1, -1},
                    {-1, -1},
                    {2, 0},
                    {-2, 0},
                    {0, 2},
                    {0, -2}
                }
                : new int[][] {
                    {0, 0},
                    {1, 0},
                    {-1, 0},
                    {0, 1},
                    {0, -1},
                    {1, 1},
                    {-1, 1},
                    {1, -1},
                    {-1, -1}
                };
        for (int yOffset : yOffsets) {
            int y = Mth.clamp(baseY + yOffset, minY, maxY);
            for (int[] xzOffset : xzOffsets) {
                BlockPos candidate = new BlockPos(x + xzOffset[0], y, z + xzOffset[1]);
                if (canSpawnAt(level, candidate, false)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    private static boolean isGrandEventSearchNodeTooCloseToTrackedPlayers(
            ServerLevel level,
            GrandEventState state,
            BlockPos candidate,
            double minDistanceSqr) {
        if (level == null || state == null || candidate == null) {
            return false;
        }
        for (UUID playerId : state.trackedPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (getGrandEventScopeRejectReason(level, state, player) != null) {
                continue;
            }
            if (player.blockPosition().distSqr(candidate) < minDistanceSqr) {
                return true;
            }
        }
        return false;
    }

    private static String getGrandEventSearchContextRejectReason(
            ServerLevel level,
            GrandEventState state,
            ServerPlayer focus,
            BlockPos candidate) {
        if (candidate == null) {
            return "null_candidate";
        }
        if (state == null || level == null || focus == null) {
            return isSpecialSpawnContextValid(level, focus, candidate) ? null : "spawn_context";
        }

        boolean candidateCovered = hasAnyNonAirAbove(level, candidate);
        if (state.coveredAtStart()) {
            if (!candidateCovered) {
                return "open_sky_in_covered_event";
            }
        } else if (candidateCovered) {
            return "covered_in_open_event";
        }
        return null;
    }

    private static double scoreGrandEventSearchCandidate(
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            BlockPos candidate,
            int minDistance,
            int maxDistance,
            boolean aroundAnchor,
            boolean oppositeBias,
            boolean preferOuterRing,
            int sector) {
        if (state == null || warden == null || focus == null || candidate == null) {
            return Double.NEGATIVE_INFINITY;
        }
        BlockPos scoringOrigin = aroundAnchor ? state.anchorPos() : focus.blockPosition();
        double fromOrigin = Math.sqrt(candidate.distSqr(scoringOrigin));
        double ringSpan = Math.max(1.0D, maxDistance - minDistance);
        double ringRatio = Mth.clamp((fromOrigin - minDistance) / ringSpan, 0.0D, 1.0D);

        BlockPos lastIssued = state.lastIssuedNode();
        double fromLast = lastIssued == null ? fromOrigin : Math.sqrt(candidate.distSqr(lastIssued));

        double minRecentSqr = state.minDistanceToRecentSearchNodesSqr(candidate);
        double minRecent = Double.isFinite(minRecentSqr) ? Math.sqrt(minRecentSqr) : fromLast;

        Vec3 candidateDir = Vec3.atCenterOf(candidate).subtract(Vec3.atCenterOf(scoringOrigin));
        candidateDir = new Vec3(candidateDir.x, 0.0D, candidateDir.z);
        Vec3 previousDir = resolveGrandEventPreviousDirection(state, scoringOrigin);
        double angleDelta = angleBetweenHorizontalDegrees(previousDir, candidateDir);
        double minRecentAngularGap = minAngleDistanceToRecentSearchNodes(state, scoringOrigin, candidate);
        int directionalRepeat = countRecentSearchNodesInAngleWindow(
                state,
                scoringOrigin,
                candidate,
                GRAND_EVENT_SEARCH_DIRECTION_WINDOW_DEGREES);

        int sectorVisits = state.searchSectorVisitCount(sector);
        int maxSectorVisits = state.maxSearchSectorVisitCount();
        double sectorCoverageBonus = Math.max(0, (maxSectorVisits + 2) - sectorVisits) * (GRAND_EVENT_SEARCH_SECTOR_COVERAGE_BONUS_STEP + 4.0D);
        double sectorRepeatPenalty = state.isSameSearchSectorAsLast(sector)
                ? (GRAND_EVENT_SEARCH_SAME_SECTOR_PENALTY + 8.0D)
                : state.isAdjacentSearchSectorToLast(sector)
                        ? (GRAND_EVENT_SEARCH_ADJACENT_SECTOR_PENALTY + 5.0D)
                        : 0.0D;
        sectorRepeatPenalty += state.countRecentSearchSectorAdjacencyHits(sector) * (GRAND_EVENT_SEARCH_RECENT_SECTOR_PENALTY_STEP + 1.5D);
        int microZone = resolveGrandEventSearchMicroZone(scoringOrigin, candidate);
        int microZoneRecentHits = state.countRecentSearchMicroZoneHits(microZone);
        int microZoneVisitCount = state.searchMicroZoneVisitCount(microZone);

        double score = (fromLast * 0.56D)
                + (Math.min(44.0D, minRecent) * 0.72D)
                + (angleDelta * 0.07D)
                + (ringRatio * 11.0D)
                + (minRecentAngularGap * GRAND_EVENT_SEARCH_DIRECTION_COVERAGE_BONUS_SCALE)
                - (directionalRepeat * GRAND_EVENT_SEARCH_DIRECTION_REPEAT_PENALTY)
                - (microZoneRecentHits * 6.5D)
                - (Math.max(0, microZoneVisitCount - 1) * 4.0D)
                + sectorCoverageBonus
                - sectorRepeatPenalty;
        if (preferOuterRing) {
            score += ringRatio * 5.5D;
        }
        if (aroundAnchor) {
            score += Math.min(10.0D, Math.sqrt(candidate.distSqr(state.anchorPos())) * 0.18D);
        }
        if (oppositeBias) {
            Vec3 focusToWarden = warden.position().subtract(focus.position());
            focusToWarden = new Vec3(focusToWarden.x, 0.0D, focusToWarden.z);
            if (focusToWarden.lengthSqr() > 0.001D && candidateDir.lengthSqr() > 0.001D) {
                double dot = Mth.clamp(focusToWarden.normalize().dot(candidateDir.normalize()), -1.0D, 1.0D);
                score += (1.0D - dot) * 8.0D;
            }
        }
        return score;
    }

    private static double minAngleDistanceToRecentSearchNodes(
            GrandEventState state,
            BlockPos origin,
            BlockPos candidate) {
        if (state == null || origin == null || candidate == null || state.recentSearchNodes.isEmpty()) {
            return 180.0D;
        }
        double candidateAngle = bearingDegrees(origin, candidate);
        double best = 180.0D;
        for (BlockPos recent : state.recentSearchNodes) {
            if (recent == null) {
                continue;
            }
            double recentAngle = bearingDegrees(origin, recent);
            double delta = Math.abs(wrapAngleDegrees(candidateAngle - recentAngle));
            if (delta < best) {
                best = delta;
            }
        }
        return best;
    }

    private static int countRecentSearchNodesInAngleWindow(
            GrandEventState state,
            BlockPos origin,
            BlockPos candidate,
            double halfWindowDegrees) {
        if (state == null || origin == null || candidate == null || state.recentSearchNodes.isEmpty()) {
            return 0;
        }
        double candidateAngle = bearingDegrees(origin, candidate);
        int count = 0;
        for (BlockPos recent : state.recentSearchNodes) {
            if (recent == null) {
                continue;
            }
            double recentAngle = bearingDegrees(origin, recent);
            double delta = Math.abs(wrapAngleDegrees(candidateAngle - recentAngle));
            if (delta <= halfWindowDegrees) {
                count++;
            }
        }
        return count;
    }

    private static double bearingDegrees(BlockPos origin, BlockPos target) {
        if (origin == null || target == null) {
            return 0.0D;
        }
        double dx = target.getX() - origin.getX();
        double dz = target.getZ() - origin.getZ();
        if (Math.abs(dx) < 1.0E-4D && Math.abs(dz) < 1.0E-4D) {
            return 0.0D;
        }
        return Math.toDegrees(Math.atan2(dz, dx));
    }

    private static int resolveGrandEventSearchSector(BlockPos origin, BlockPos target) {
        if (origin == null || target == null) {
            return -1;
        }
        double angle = bearingDegrees(origin, target);
        if (Double.isNaN(angle)) {
            return -1;
        }
        double normalized = (angle + 360.0D) % 360.0D;
        double sectorWidth = 360.0D / GRAND_EVENT_SEARCH_SECTOR_COUNT;
        int sector = (int) Math.floor(normalized / sectorWidth);
        return normalizeSearchSector(sector);
    }

    private static int resolveGrandEventSearchMicroZone(BlockPos origin, BlockPos target) {
        int sector = resolveGrandEventSearchSector(origin, target);
        if (sector < 0 || origin == null || target == null) {
            return -1;
        }
        double distance = Math.sqrt(origin.distSqr(target));
        int ring;
        if (distance < GRAND_EVENT_SEARCH_MICROZONE_RING_NEAR_MAX) {
            ring = 0;
        } else if (distance < GRAND_EVENT_SEARCH_MICROZONE_RING_MID_MAX) {
            ring = 1;
        } else {
            ring = 2;
        }
        return (ring * GRAND_EVENT_SEARCH_SECTOR_COUNT) + sector;
    }

    private static double wrapAngleDegrees(double angleDegrees) {
        double wrapped = angleDegrees % 360.0D;
        if (wrapped > 180.0D) {
            wrapped -= 360.0D;
        } else if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    private static int normalizeSearchSector(int sector) {
        if (GRAND_EVENT_SEARCH_SECTOR_COUNT <= 0 || sector < 0) {
            return -1;
        }
        return Math.floorMod(sector, GRAND_EVENT_SEARCH_SECTOR_COUNT);
    }

    private static boolean areAdjacentSearchSectors(int a, int b) {
        int aNorm = normalizeSearchSector(a);
        int bNorm = normalizeSearchSector(b);
        if (aNorm < 0 || bNorm < 0) {
            return false;
        }
        int diff = Math.abs(aNorm - bNorm);
        int wrapDiff = GRAND_EVENT_SEARCH_SECTOR_COUNT - diff;
        return Math.min(diff, wrapDiff) == 1;
    }

    private static Vec3 resolveGrandEventPreviousDirection(GrandEventState state, BlockPos origin) {
        if (state == null || origin == null) {
            return Vec3.ZERO;
        }
        BlockPos lastIssued = state.lastIssuedNode();
        if (lastIssued != null) {
            Vec3 dir = Vec3.atCenterOf(lastIssued).subtract(Vec3.atCenterOf(origin));
            dir = new Vec3(dir.x, 0.0D, dir.z);
            if (dir.lengthSqr() > 0.001D) {
                return dir.normalize();
            }
        }
        float radians = (float) Math.toRadians(state.searchAngleDegrees());
        return new Vec3(Math.cos(radians), 0.0D, Math.sin(radians)).normalize();
    }

    private static double angleBetweenHorizontalDegrees(Vec3 a, Vec3 b) {
        if (a == null || b == null || a.lengthSqr() < 0.001D || b.lengthSqr() < 0.001D) {
            return 0.0D;
        }
        Vec3 aNorm = new Vec3(a.x, 0.0D, a.z).normalize();
        Vec3 bNorm = new Vec3(b.x, 0.0D, b.z).normalize();
        double dot = Mth.clamp(aNorm.dot(bNorm), -1.0D, 1.0D);
        return Math.toDegrees(Math.acos(dot));
    }

    private static BlockPos findGrandEventSearchTarget(
            ServerLevel level,
            GrandEventState state,
            ServerPlayer focus,
            int minDistance,
            int maxDistance) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos candidate = findGrandEventSearchCandidate(level, state, focus, minDistance, maxDistance);
            if (candidate == null) {
                continue;
            }
            if (getGrandEventSearchContextRejectReason(level, state, focus, candidate) == null) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findGrandEventSearchTargetOpposite(
            ServerLevel level,
        GrandEventState state,
        ServerPlayer focus,
        Warden warden,
        int minDistance,
        int maxDistance) {
        BlockPos firstValid = null;
        Vec3 fromFocusToWarden = warden.position().subtract(focus.position());
        Vec3 baseDir = fromFocusToWarden.lengthSqr() > 0.001D
                ? fromFocusToWarden.normalize()
                : focus.getLookAngle().normalize();
        for (int attempt = 0; attempt < 14; attempt++) {
            BlockPos candidate = findGrandEventSearchCandidate(level, state, focus, minDistance, maxDistance);
            if (candidate == null) {
                continue;
            }
            Vec3 toCandidate = Vec3.atCenterOf(candidate).subtract(focus.position());
            if (toCandidate.lengthSqr() < 0.001D) {
                continue;
            }
            if (getGrandEventSearchContextRejectReason(level, state, focus, candidate) != null) {
                continue;
            }
            if (firstValid == null) {
                firstValid = candidate.immutable();
            }
            double dot = baseDir.dot(toCandidate.normalize());
            if (dot < -0.10D) {
                return candidate.immutable();
            }
        }
        return firstValid;
    }

    private static BlockPos findGrandEventSearchTargetAroundAnchor(
            ServerLevel level,
            GrandEventState state,
            ServerPlayer focus,
            int minDistance,
            int maxDistance) {
        if (state == null || focus == null) {
            return null;
        }
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int baseY = Mth.clamp(focus.blockPosition().getY(), minY, maxY);
        BlockPos anchor = state.anchorPos();
        int clampedMin = Math.max(1, minDistance);
        int clampedMax = Math.max(clampedMin + 1, maxDistance);
        float baseAngle = state.searchAngleDegrees();
        state.setSearchAngleDegrees((baseAngle + 67.0F + level.random.nextFloat() * 21.0F) % 360.0F);

        for (int attempt = 0; attempt < 16; attempt++) {
            float jitter = (level.random.nextFloat() * 26.0F) - 13.0F;
            float attemptSpread = attempt * 29.0F;
            double radians = Math.toRadians(baseAngle + jitter + attemptSpread);
            double distance = clampedMin + level.random.nextDouble() * (clampedMax - clampedMin);
            int x = Mth.floor(anchor.getX() + Math.cos(radians) * distance);
            int z = Mth.floor(anchor.getZ() + Math.sin(radians) * distance);
            BlockPos candidate = findGrandEventSearchCandidateWithTolerance(level, x, z, baseY, false);
            if (candidate == null) {
                continue;
            }
            if (getGrandEventSearchContextRejectReason(level, state, focus, candidate) == null) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findGrandEventSearchCandidate(
            ServerLevel level,
            GrandEventState state,
            ServerPlayer focus,
            int minDistance,
            int maxDistance) {
        int clampedMin = Math.max(1, minDistance);
        int clampedMax = Math.max(clampedMin + 1, maxDistance);
        int baseY = Mth.floor(focus.getY());
        float baseAngle = state == null ? level.random.nextFloat() * 360.0F : state.searchAngleDegrees();
        if (state != null) {
            state.setSearchAngleDegrees((baseAngle + 73.0F + level.random.nextFloat() * 25.0F) % 360.0F);
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            float jitter = (level.random.nextFloat() * 30.0F) - 15.0F;
            float attemptSpread = attempt * 36.0F;
            double angle = Math.toRadians(baseAngle + jitter + attemptSpread);
            double distance = clampedMin + level.random.nextDouble() * (clampedMax - clampedMin);
            int x = Mth.floor(focus.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(focus.getZ() + Math.sin(angle) * distance);
            BlockPos candidate = findGrandEventSearchCandidateWithTolerance(level, x, z, baseY, false);
            if (candidate != null) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findGrandEventBridgeTarget(ServerLevel level, Warden warden, ServerPlayer focus) {
        Vec3 from = warden.position();
        Vec3 to = focus.position();
        Vec3 delta = to.subtract(from);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 0.001D) {
            return null;
        }

        Vec3 direction = new Vec3(delta.x / horizontal, 0.0D, delta.z / horizontal);
        int baseY = Mth.floor(from.y);
        int focusY = focus.blockPosition().getY();

        for (double step = 8.0D; step <= 18.0D; step += 2.5D) {
            int x = Mth.floor(from.x + direction.x * step);
            int z = Mth.floor(from.z + direction.z * step);
            BlockPos candidate = findSpawnAtOrAbovePlayerY(level, x, z, baseY, false);
            if (candidate == null) {
                continue;
            }
            if (Math.abs(candidate.getY() - focusY) > 10) {
                continue;
            }
            return candidate.immutable();
        }
        return null;
    }

    private static boolean issueGrandEventSearchNode(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            BlockPos node,
            String reason,
            double distContextSqr,
            boolean allowRecentOverlapAtIssue) {
        if (state != null && state.exiting()) {
            debugLog(
                    "GRAND_EVENT search_suppressed_in_exit=true runtime={} source=issue_node reason=exit_authority_lock",
                    state.runtimeId());
            debugLog(
                    "GRAND_EVENT exit_nav_owner=retreat_only runtime={} source=issue_node",
                    state.runtimeId());
            return false;
        }
        if (warden == null || focus == null || node == null) {
            return false;
        }
        if (state != null
                && !allowRecentOverlapAtIssue
                && state.isRecentSearchNodeTooClose(node, GRAND_EVENT_RECENT_SEARCH_MIN_DISTANCE_SQR)) {
            debugLog(
                    "GRAND_EVENT search-node-rejected target={} node={} reason=too_close_recent_node",
                    playerLabel(focus),
                    node);
            debugLog(
                    "GRAND_EVENT search.contract resolverRelaxed={} issueReject={} node={} reason={}",
                    false,
                    true,
                    node,
                    "too_close_recent_node");
            debugLog(
                    "GRAND_EVENT search.lifecycle state=blocked reason=too_close_recent_node node={} runtime={}",
                    node,
                    state.runtimeId());
            return false;
        }
        String searchContextRejectReason = getGrandEventSearchContextRejectReason(level, state, focus, node);
        if (searchContextRejectReason != null) {
            debugLog(
                    "GRAND_EVENT search_context rejected reason={} target={} node={}",
                    searchContextRejectReason,
                    playerLabel(focus),
                    node);
            debugLog(
                    "GRAND_EVENT search.lifecycle state=blocked reason={} node={} runtime={}",
                    searchContextRejectReason,
                    node,
                    state == null ? "none" : state.runtimeId());
            return false;
        }
        if (warden.getNavigation().createPath(node, 0) == null) {
            debugLog("GRAND_EVENT search-node-rejected target={} node={} reason=no_path", playerLabel(focus), node);
            debugLog(
                    "GRAND_EVENT search.lifecycle state=blocked reason=no_path node={} runtime={}",
                    node,
                    state == null ? "none" : state.runtimeId());
            return false;
        }

        Optional<LivingEntity> angryAt = warden.getEntityAngryAt();
        if (angryAt.isPresent()) {
            warden.clearAnger(angryAt.get());
            debugLog(
                    "GRAND_EVENT search-node-clear-anger target={} node={} angryAt={}",
                    playerLabel(focus),
                    node,
                    angryAt.get().getStringUUID());
        }
        if (warden.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            debugLog("GRAND_EVENT search-node-rejected target={} node={} reason=attack_target_present", playerLabel(focus), node);
            debugLog(
                    "GRAND_EVENT search.lifecycle state=blocked reason=attack_target_present node={} runtime={}",
                    node,
                    state == null ? "none" : state.runtimeId());
            return false;
        }

        WardenAi.setDisturbanceLocation(warden, node);
        Optional<BlockPos> disturbance = warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        boolean disturbanceSet = disturbance.isPresent() && disturbance.get().equals(node);
        if (!disturbanceSet) {
            debugLog(
                    "GRAND_EVENT search-node-rejected target={} node={} reason=disturbance_not_set activeDisturbance={} distContext={} source={}",
                    playerLabel(focus),
                    node,
                    disturbance.map(Object::toString).orElse("none"),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(distContextSqr)),
                    reason);
            debugLog(
                    "GRAND_EVENT search.contract resolverRelaxed={} issueReject={} node={} reason={}",
                    allowRecentOverlapAtIssue,
                    true,
                    node,
                    "disturbance_not_set");
            return false;
        }
        if (state != null) {
            debugLog("GRAND_EVENT search_context accepted reason=ok target={} node={}", playerLabel(focus), node);
            debugLog(
                    "GRAND_EVENT search.contract resolverRelaxed={} issueReject={} node={} reason={}",
                    allowRecentOverlapAtIssue,
                    false,
                    node,
                    "accepted");
        }
        return true;
    }

    private static void logGrandEventInvestigateState(ServerLevel level, Warden warden, ServerPlayer focus, long now) {
        if (warden == null) {
            return;
        }
        Optional<?> activeActivity = warden.getBrain().getActiveNonCoreActivity();
        Optional<BlockPos> disturbance = warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        boolean hasWalkTarget = warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        boolean hasPathMemory = warden.getBrain().hasMemoryValue(MemoryModuleType.PATH);
        boolean hasCantReachSince = warden.getBrain().hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        Optional<?> cantReachSince = warden.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        boolean navDone = warden.getNavigation().isDone();
        debugLog(
                "GRAND_EVENT investigate-state tick={} focus={} active={} disturbance={} hasWalkTarget={} hasPathMemory={} hasCantReach={} cantReachSince={} navDone={} distToFocus={}",
                now,
                playerLabel(focus),
                activeActivity.map(Object::toString).orElse("none"),
                disturbance.map(Object::toString).orElse("none"),
                hasWalkTarget,
                hasPathMemory,
                hasCantReachSince,
                cantReachSince.map(Object::toString).orElse("none"),
                navDone,
                String.format(Locale.ROOT, "%.2f", Math.sqrt(warden.distanceToSqr(focus))));
    }

    private static boolean shouldSampleGrandEventRuntime(long now) {
        return (now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L;
    }

    private static void logGrandEventRuntimeEntered(
            String methodName,
            GrandEventState state,
            ServerLevel level,
            Warden warden,
            ServerPlayer focus,
            long now) {
        if (state == null || !shouldSampleGrandEventRuntime(now)) {
            return;
        }
        String focusLabel = focus == null ? "none" : playerLabel(focus);
        String wardenPos = warden == null ? "none" : String.valueOf(warden.blockPosition());
        String wardenId = warden == null ? String.valueOf(state.wardenUuid()) : warden.getStringUUID();
        String distance = (warden == null || focus == null)
                ? "n/a"
                : String.format(Locale.ROOT, "%.2f", Math.sqrt(warden.distanceToSqr(focus)));
        String active = "none";
        boolean navDone = true;
        if (warden != null) {
            active = warden.getBrain().getActiveNonCoreActivity().map(Object::toString).orElse("none");
            navDone = warden.getNavigation().isDone();
        }
        debugLog(
                "GRAND_EVENT_RUNTIME entered {} id={} build={} class={} startTick={} tick={} dim={} warden={} wardenPos={} focus={} dist={} active={} navDone={}",
                methodName,
                state.runtimeId(),
                state.buildSignature(),
                GRAND_EVENT_RUNTIME_CLASS,
                state.startedTick(),
                now,
                level.dimension().location(),
                wardenId,
                wardenPos,
                focusLabel,
                distance,
                active,
                navDone);
    }

    private static void logGrandEventIntentAuthority(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            long now) {
        if (state == null || warden == null || !shouldSampleGrandEventRuntime(now) || !state.lastIssuedByFlow()) {
            return;
        }

        BlockPos lastIssuedNode = state.lastIssuedNode();
        if (lastIssuedNode == null) {
            return;
        }

        long lastIssuedTick = state.lastIssuedTick();
        Optional<BlockPos> disturbance = warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        boolean hasWalkTarget = warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        boolean hasPathMemory = warden.getBrain().hasMemoryValue(MemoryModuleType.PATH);
        boolean hasCantReachSince = warden.getBrain().hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        Optional<?> cantReachSince = warden.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        boolean navDone = warden.getNavigation().isDone();
        boolean consumed = hasWalkTarget || hasPathMemory || !navDone;

        Entity hardTarget = warden.getTarget();
        Optional<LivingEntity> attackMemory = warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        String focusLabel = focus == null ? "none" : playerLabel(focus);
        String distToFocus = (focus == null)
                ? "n/a"
                : String.format(Locale.ROOT, "%.2f", Math.sqrt(warden.distanceToSqr(focus)));

        if (consumed && state.lastIntentConsumedTick() < lastIssuedTick) {
            state.setLastIntentConsumedTick(now);
            debugLog(
                    "GRAND_EVENT_RUNTIME intent-consumed id={} target={} issuedNode={} issuedReason={} issuedTick={} disturbance={} hasWalkTarget={} hasPathMemory={} hasCantReach={} cantReachSince={} navDone={} distToFocus={}",
                    state.runtimeId(),
                    focusLabel,
                    lastIssuedNode,
                    state.lastIssuedReason(),
                    lastIssuedTick,
                    disturbance.map(Object::toString).orElse("none"),
                    hasWalkTarget,
                    hasPathMemory,
                    hasCantReachSince,
                    cantReachSince.map(Object::toString).orElse("none"),
                    navDone,
                    distToFocus);
            return;
        }

        if (!consumed && now - lastIssuedTick >= GRAND_EVENT_INTENT_NONCONSUMED_TIMEOUT_TICKS) {
            boolean disturbanceMatches = disturbance.isPresent() && disturbance.get().equals(lastIssuedNode);
            if (disturbanceMatches && state.lastIntentNotConsumedLogTick() < lastIssuedTick) {
                state.setLastIntentNotConsumedLogTick(now);
                debugLog(
                        "GRAND_EVENT_RUNTIME intent-not-consumed id={} target={} issuedNode={} issuedReason={} issuedTick={} currentDisturbance={} hasWalkTarget={} hasPathMemory={} hasCantReach={} cantReachSince={} navDone={} distToFocus={}",
                        state.runtimeId(),
                        focusLabel,
                        lastIssuedNode,
                        state.lastIssuedReason(),
                        lastIssuedTick,
                        disturbance.map(Object::toString).orElse("none"),
                        hasWalkTarget,
                        hasPathMemory,
                        hasCantReachSince,
                        cantReachSince.map(Object::toString).orElse("none"),
                        navDone,
                        distToFocus);
                return;
            }
        }

        boolean disturbanceChanged = disturbance.isPresent() && !disturbance.get().equals(lastIssuedNode);
        boolean attackInjectedOutsideEventState = state.attackTarget() == null
                && (attackMemory.isPresent() || (hardTarget instanceof ServerPlayer tracked && isGrandEventTrackedPlayer(level, state, tracked)));
        if ((disturbanceChanged || attackInjectedOutsideEventState) && state.lastIntentOverwrittenLogTick() < lastIssuedTick) {
            state.setLastIntentOverwrittenLogTick(now);
            String writerContext;
            if (attackMemory.isPresent()) {
                writerContext = "vanilla_brain_attack_memory";
            } else if (hardTarget != null) {
                writerContext = "entity_target=" + hardTarget.getStringUUID();
            } else if (disturbanceChanged) {
                writerContext = "disturbance_changed";
            } else {
                writerContext = "unknown";
            }
            debugLog(
                    "GRAND_EVENT_RUNTIME intent-overwritten id={} target={} issuedNode={} issuedReason={} issuedTick={} currentDisturbance={} writerContext={} attackMemory={} hardTarget={} hasWalkTarget={} hasPathMemory={} hasCantReach={} cantReachSince={} navDone={} distToFocus={}",
                    state.runtimeId(),
                    focusLabel,
                    lastIssuedNode,
                    state.lastIssuedReason(),
                    lastIssuedTick,
                    disturbance.map(Object::toString).orElse("none"),
                    writerContext,
                    attackMemory.map(living -> living.getStringUUID()).orElse("none"),
                    hardTarget == null ? "none" : hardTarget.getStringUUID(),
                    hasWalkTarget,
                    hasPathMemory,
                    hasCantReachSince,
                    cantReachSince.map(Object::toString).orElse("none"),
                    navDone,
                    distToFocus);
        }
    }

    private static void logGrandEventAuthoritySnapshot(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            ServerPlayer focus,
            ServerPlayer nearestInScope,
            long now,
            boolean perceptualGuardActive) {
        if (level == null || state == null || warden == null || !shouldSampleGrandEventRuntime(now)) {
            return;
        }
        Optional<BlockPos> disturbance = warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        boolean hasPath = warden.getBrain().hasMemoryValue(MemoryModuleType.PATH);
        boolean hasWalkTarget = warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        boolean navDone = warden.getNavigation().isDone();
        Optional<?> active = warden.getBrain().getActiveNonCoreActivity();
        Optional<LivingEntity> attackMemory = warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        Entity hardTarget = warden.getTarget();
        BlockPos lastIssuedNode = state.lastIssuedNode();
        String walkSource = "none";
        if (disturbance.isPresent()) {
            if (state.lastIssuedByFlow() && lastIssuedNode != null && disturbance.get().equals(lastIssuedNode)) {
                walkSource = "custom_issue";
            } else {
                walkSource = "vanilla";
            }
        }

        String lookMode = "none";
        String yawToNearest = "n/a";
        if (nearestInScope != null) {
            Vec3 toNearest = nearestInScope.position().subtract(warden.position());
            double yawDelta = angleBetweenHorizontalDegrees(warden.getLookAngle(), toNearest);
            yawToNearest = String.format(Locale.ROOT, "%.2f", yawDelta);
            lookMode = yawDelta <= 20.0D ? "player_like" : "path";
        }
        debugLog(
                "GRAND_EVENT authority.walk runtime={} source={} disturbance={} hasWalkTarget={} hasPath={} navDone={}",
                state.runtimeId(),
                walkSource,
                disturbance.map(Object::toString).orElse("none"),
                hasWalkTarget,
                hasPath,
                navDone);
        debugLog(
                "GRAND_EVENT authority.walk_target effective={} disturbance={} walk={} path={} navDone={}",
                walkSource,
                disturbance.map(Object::toString).orElse("none"),
                hasWalkTarget,
                hasPath,
                navDone);
        debugLog(
                "GRAND_EVENT authority.path runtime={} navDone={} hasPath={} hasWalkTarget={} pathIsNull={}",
                state.runtimeId(),
                navDone,
                hasPath,
                hasWalkTarget,
                warden.getNavigation().getPath() == null);
        debugLog(
                "GRAND_EVENT authority.look runtime={} mode={} yawToNearestPlayer={} nearest={}",
                state.runtimeId(),
                lookMode,
                yawToNearest,
                nearestInScope == null ? "none" : playerLabel(nearestInScope));
        debugLog(
                "GRAND_EVENT authority.attack runtime={} stateTarget={} hardTarget={} memoryTarget={} admittedTrigger={}",
                state.runtimeId(),
                state.attackTarget() == null ? "none" : state.attackTarget(),
                hardTarget == null ? "none" : hardTarget.getStringUUID(),
                attackMemory.map(living -> living.getStringUUID()).orElse("none"),
                state.lastAdmittedTrigger());
        debugLog(
                "GRAND_EVENT authority.activity runtime={} active={} attackMemory={} hardTarget={} admittedTrigger={} guard={}",
                state.runtimeId(),
                active.map(Object::toString).orElse("none"),
                attackMemory.map(living -> living.getStringUUID()).orElse("none"),
                hardTarget == null ? "none" : hardTarget.getStringUUID(),
                state.lastAdmittedTrigger(),
                perceptualGuardActive);
        debugLog(
                "GRAND_EVENT authority.cave_break runtime={} dirSource={} lastTick={} focus={} distToAnchor={}",
                state.runtimeId(),
                state.lastCaveBreakDirectionSource(),
                state.lastCaveBreakDirectionTick(),
                focus == null ? "none" : playerLabel(focus),
                String.format(Locale.ROOT, "%.2f", Math.sqrt(warden.distanceToSqr(Vec3.atCenterOf(state.anchorPos())))));
    }

    private static void keepGrandWardenEngaged(Warden warden, ServerPlayer focus, long now) {
        if (warden == null || focus == null) {
            return;
        }
        if (!WARDEN_INCREASE_ANGER_LOOKUP_DONE) {
            WARDEN_INCREASE_ANGER_LOOKUP_DONE = true;
            WARDEN_INCREASE_ANGER_METHOD = resolveWardenIncreaseAngerMethod();
            if (WARDEN_INCREASE_ANGER_METHOD == null) {
                debugLog("GRAND_EVENT keep-alive warning: Warden increaseAngerAt signature not found.");
            } else {
                debugLog("GRAND_EVENT keep-alive method resolved: {}", WARDEN_INCREASE_ANGER_METHOD.toGenericString());
            }
        }
        if (WARDEN_INCREASE_ANGER_METHOD == null) {
            return;
        }
        try {
            invokeWardenIncreaseAnger(warden, focus);
        } catch (Throwable t) {
            if (!WARDEN_KEEPALIVE_INVOKE_FAILURE_LOGGED) {
                WARDEN_KEEPALIVE_INVOKE_FAILURE_LOGGED = true;
                debugLog("GRAND_EVENT keep-alive invoke failed: {}", t.toString());
            }
        }
    }

    private static void stabilizeGrandWardenState(Warden warden, ServerPlayer focus, long now) {
        if (warden == null) {
            return;
        }
        if (!warden.getTags().contains(GRAND_WARDEN_TAG)) {
            warden.addTag(GRAND_WARDEN_TAG);
        }
        if (!isGrandWardenDisplayNameSet(warden)) {
            applyGrandWardenRenderMarkerName(warden);
        }
        warden.setCustomNameVisible(false);
        if (WARDEN_DIG_POSE != null && warden.getPose() == WARDEN_DIG_POSE) {
            warden.setPose(Pose.STANDING);
        }
        applyGrandWardenStepUp(warden, now);
        preventGrandWardenDigBeforeEnd(warden, now);
    }

    private static boolean isGrandWardenDisplayNameSet(Warden warden) {
        if (warden == null || !warden.hasCustomName() || warden.getCustomName() == null) {
            return false;
        }
        return GRAND_WARDEN_DISPLAY_NAME.equals(warden.getCustomName().getString());
    }

    private static void applyGrandWardenStepUp(Warden warden, long now) {
        if (warden == null) {
            return;
        }

        if (!ENTITY_SET_MAX_UP_STEP_LOOKUP_DONE) {
            ENTITY_SET_MAX_UP_STEP_LOOKUP_DONE = true;
            try {
                ENTITY_SET_MAX_UP_STEP_METHOD = Entity.class.getMethod("setMaxUpStep", float.class);
            } catch (Throwable ignored) {
                ENTITY_SET_MAX_UP_STEP_METHOD = null;
                debugLog("GRAND_EVENT warden_stepup lookup-missing");
            }
        }

        if (ENTITY_SET_MAX_UP_STEP_METHOD == null) {
            return;
        }

        try {
            ENTITY_SET_MAX_UP_STEP_METHOD.invoke(warden, GRAND_WARDEN_STEP_UP);
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog("GRAND_EVENT warden_stepup={} applied warden={}", GRAND_WARDEN_STEP_UP, warden.getStringUUID());
            }
        } catch (Throwable t) {
            if (!ENTITY_SET_MAX_UP_STEP_INVOKE_FAILURE_LOGGED) {
                ENTITY_SET_MAX_UP_STEP_INVOKE_FAILURE_LOGGED = true;
                debugLog("GRAND_EVENT warden_stepup invoke-failed: {}", t.toString());
            }
        }
    }

    private static double resolveGrandEventChaseSpeed(Warden warden, ServerPlayer target) {
        if (warden == null || target == null) {
            return GRAND_EVENT_ATTACK_CHASE_SPEED_NEAR;
        }
        double distSqr = warden.distanceToSqr(target);
        boolean waterPursuit = warden.isInWaterOrBubble() || target.isInWaterOrBubble();
        if (distSqr >= GRAND_EVENT_ATTACK_FAR_DISTANCE_SQR) {
            double base = GRAND_EVENT_ATTACK_CHASE_SPEED_FAR;
            if (waterPursuit) {
                double waterSpeed = Math.min(GRAND_EVENT_ATTACK_WATER_CHASE_MAX, base + GRAND_EVENT_ATTACK_WATER_CHASE_BONUS);
                return waterSpeed * GRAND_EVENT_WATER_SPEED_SCALE;
            }
            return base;
        }
        if (distSqr >= GRAND_EVENT_ATTACK_MEDIUM_DISTANCE_SQR) {
            double base = GRAND_EVENT_ATTACK_CHASE_SPEED_MEDIUM;
            if (waterPursuit) {
                double waterSpeed = Math.min(GRAND_EVENT_ATTACK_WATER_CHASE_MAX, base + GRAND_EVENT_ATTACK_WATER_CHASE_BONUS);
                return waterSpeed * GRAND_EVENT_WATER_SPEED_SCALE;
            }
            return base;
        }
        double base = GRAND_EVENT_ATTACK_CHASE_SPEED_NEAR;
        if (waterPursuit) {
            double waterSpeed = Math.min(GRAND_EVENT_ATTACK_WATER_CHASE_MAX, base + GRAND_EVENT_ATTACK_WATER_CHASE_BONUS);
            return waterSpeed * GRAND_EVENT_WATER_SPEED_SCALE;
        }
        return base;
    }

    private static void applyGrandEventAggroTuning(GrandEventState state, Warden warden, ServerPlayer target, long now) {
        if (state == null || warden == null || target == null) {
            return;
        }
        state.captureGrandWardenAggroDefaults(warden);
        state.setAggroTuningActive(true);

        if (warden.getNavigation() instanceof GroundPathNavigation groundNavigation) {
            groundNavigation.setCanFloat(true);
        }
        warden.setPathfindingMalus(PathType.WATER, GRAND_EVENT_WATER_PATH_MALUS);

        boolean waterMode = warden.isInWaterOrBubble() || target.isInWaterOrBubble();
        if (waterMode) {
            warden.addEffect(new MobEffectInstance(
                    MobEffects.DOLPHINS_GRACE,
                    GRAND_EVENT_WATER_SPEED_EFFECT_TICKS,
                    GRAND_EVENT_WATER_SPEED_EFFECT_AMPLIFIER,
                    false,
                    false,
                    false));
        }
        applyGrandEventNoBlockWaterRun(warden);

        if (shouldSampleGrandEventRuntime(now)) {
            debugLog(
                    "GRAND_EVENT water_speed scale={} speed={} cap=2.10 waterMode={}",
                    String.format(Locale.ROOT, "%.2f", GRAND_EVENT_WATER_SPEED_SCALE),
                    String.format(Locale.ROOT, "%.2f", resolveGrandEventChaseSpeed(warden, target)),
                    waterMode);
            debugLog(
                    "GRAND_EVENT aggro-tuning on speed={} dist={} waterMode={}",
                    String.format(Locale.ROOT, "%.2f", resolveGrandEventChaseSpeed(warden, target)),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(warden.distanceToSqr(target))),
                    waterMode);
        }
    }

    private static void clearGrandEventAggroTuning(GrandEventState state, Warden warden, long now) {
        if (state == null || warden == null || !state.isAggroTuningActive()) {
            return;
        }
        state.restoreGrandWardenAggroDefaults(warden);
        state.setAggroTuningActive(false);
        if (shouldSampleGrandEventRuntime(now)) {
            debugLog("GRAND_EVENT aggro-tuning off");
        }
    }

    private static void applyGrandEventNoBlockWaterRun(Warden warden) {
        if (warden == null || !(warden.level() instanceof ServerLevel level) || !warden.isInWaterOrBubble()) {
            return;
        }
        double surfaceY = resolveFluidSurfaceY(level, warden.blockPosition());
        if (Double.isNaN(surfaceY)) {
            return;
        }

        double currentY = warden.getY();
        double targetY = surfaceY + 0.02D;
        double verticalDelta = targetY - currentY;
        Vec3 velocity = warden.getDeltaMovement();
        double verticalBoost = Math.max(velocity.y, Math.min(0.22D, (verticalDelta * 0.42D) + 0.04D));
        if (verticalDelta < -0.30D) {
            verticalBoost = Math.max(-0.06D, velocity.y);
        }
        warden.setDeltaMovement(
                velocity.x * GRAND_EVENT_NO_BLOCK_WATER_HORIZONTAL_MULTIPLIER * GRAND_EVENT_WATER_SPEED_SCALE,
                verticalBoost,
                velocity.z * GRAND_EVENT_NO_BLOCK_WATER_HORIZONTAL_MULTIPLIER * GRAND_EVENT_WATER_SPEED_SCALE);
        warden.fallDistance = 0.0F;
    }

    private static void tickGrandEventCavePathBreak(
            ServerLevel level,
            GrandEventState state,
            Warden warden,
            LivingEntity priorityTarget,
            long now) {
        if (level == null || state == null || warden == null || !warden.isAlive()) {
            return;
        }
        if (!state.coveredAtStart()) {
            return;
        }
        if ((now % GRAND_EVENT_CAVE_BREAK_INTERVAL_TICKS) != 0L) {
            return;
        }

        String[] directionSource = new String[] {"none"};
        Vec3 forward = resolveGrandEventCaveBreakDirection(warden, priorityTarget, state, directionSource);
        state.setLastCaveBreakDirectionSource(directionSource[0], now);
        if (forward == null) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog(
                        "GRAND_EVENT cave_break skip reason=no_direction runtime={} warden={} dirSource={}",
                        state.runtimeId(),
                        warden.blockPosition(),
                        directionSource[0]);
            }
            return;
        }

        int broken = 0;
        String firstSkipReason = null;
        BlockPos firstSkipPos = null;
        Vec3 wardenPos = warden.position();
        int footY = Mth.floor(warden.getY());
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double[] lateralOffsets = {0.0D, -0.85D, 0.85D, -1.35D, 1.35D};

        boolean hasWalkTarget = warden.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        boolean hasPathMemory = warden.getBrain().hasMemoryValue(MemoryModuleType.PATH);
        boolean navDone = warden.getNavigation().isDone();
        boolean noIntentStall = state.attackTarget() == null && navDone && !hasWalkTarget && !hasPathMemory;
        if (noIntentStall) {
            if (state.caveBreakNoIntentSinceTick() == Long.MIN_VALUE) {
                state.setCaveBreakNoIntentSinceTick(now);
            }
        } else {
            state.setCaveBreakNoIntentSinceTick(Long.MIN_VALUE);
        }

        boolean stuckBoost = noIntentStall
                && state.caveBreakNoIntentSinceTick() != Long.MIN_VALUE
                && (now - state.caveBreakNoIntentSinceTick()) >= GRAND_EVENT_CAVE_BREAK_STUCK_THRESHOLD_TICKS;
        if (stuckBoost != state.caveBreakStuckBoostActive()) {
            state.setCaveBreakStuckBoostActive(stuckBoost);
            debugLog(
                    "GRAND_EVENT cave_break_stuck_boost {} runtime={} warden={} noIntentStall={} navDone={} hasWalkTarget={} hasPathMemory={}",
                    stuckBoost ? "on" : "off",
                    state.runtimeId(),
                    warden.blockPosition(),
                    noIntentStall,
                    navDone,
                    hasWalkTarget,
                    hasPathMemory);
        }

        int breakCap = stuckBoost
                ? GRAND_EVENT_CAVE_BREAK_STUCK_MAX_BLOCKS_PER_TICK
                : GRAND_EVENT_CAVE_BREAK_MAX_BLOCKS_PER_TICK;
        double[] forwardDistances = stuckBoost ? new double[] {1.0D, 2.0D, 2.5D} : new double[] {1.0D, 2.0D};

        for (double forwardDistance : forwardDistances) {
            if (broken >= breakCap) {
                break;
            }
            Vec3 forwardProbe = wardenPos.add(forward.scale(forwardDistance));
            for (double lateral : lateralOffsets) {
                if (broken >= breakCap) {
                    break;
                }
                Vec3 columnProbe = forwardProbe.add(right.scale(lateral));
                BlockPos columnBase = BlockPos.containing(columnProbe.x, footY, columnProbe.z);
                for (int yOffset = 1; yOffset <= 4; yOffset++) {
                    if (broken >= breakCap) {
                        break;
                    }
                    BlockPos candidate = columnBase.above(yOffset);
                    BlockState candidateState = level.getBlockState(candidate);
                    if (candidateState.isAir()) {
                        if (firstSkipReason == null) {
                            firstSkipReason = "air";
                            firstSkipPos = candidate.immutable();
                        }
                        continue;
                    }
                    if (!level.getFluidState(candidate).isEmpty()) {
                        if (firstSkipReason == null) {
                            firstSkipReason = "fluid";
                            firstSkipPos = candidate.immutable();
                        }
                        continue;
                    }
                    if (level.getBlockEntity(candidate) != null) {
                        if (firstSkipReason == null) {
                            firstSkipReason = "block_entity";
                            firstSkipPos = candidate.immutable();
                        }
                        continue;
                    }
                    if (candidateState.getDestroySpeed(level, candidate) < 0.0F) {
                        if (firstSkipReason == null) {
                            firstSkipReason = "unbreakable";
                            firstSkipPos = candidate.immutable();
                        }
                        continue;
                    }
                    if (!isGrandEventCaveBreakWhitelist(candidateState)) {
                        if (firstSkipReason == null) {
                            firstSkipReason = "not_whitelist";
                            firstSkipPos = candidate.immutable();
                        }
                        continue;
                    }
                    if (!level.destroyBlock(candidate, false, warden)) {
                        if (firstSkipReason == null) {
                            firstSkipReason = "destroy_failed";
                            firstSkipPos = candidate.immutable();
                        }
                        continue;
                    }
                    broken++;
                    if (UncannyConfig.DEBUG_LOGS.get()) {
                        debugLog(
                                "GRAND_EVENT cave_break broke pos={} block={} runtime={} count={}",
                                candidate,
                                candidateState.getBlock().getDescriptionId(),
                                state.runtimeId(),
                                broken);
                    }
                }
            }
        }

        if (broken <= 0 && shouldSampleGrandEventRuntime(now)) {
            debugLog(
                    "GRAND_EVENT cave_break skip reason={} pos={} runtime={}",
                    firstSkipReason == null ? "no_candidate" : firstSkipReason,
                    firstSkipPos == null ? "none" : firstSkipPos,
                    state.runtimeId());
        }
    }

    private static Vec3 resolveGrandEventCaveBreakDirection(
            Warden warden,
            LivingEntity priorityTarget,
            GrandEventState state,
            String[] sourceOut) {
        if (sourceOut != null && sourceOut.length > 0) {
            sourceOut[0] = "none";
        }
        Vec3 fromTarget = horizontalDirection(
                priorityTarget != null && priorityTarget.isAlive() ? priorityTarget.position().subtract(warden.position()) : null);
        if (fromTarget != null) {
            if (sourceOut != null && sourceOut.length > 0) {
                sourceOut[0] = "priority_target";
            }
            return fromTarget;
        }

        Path path = warden.getNavigation() != null ? warden.getNavigation().getPath() : null;
        if (path != null && !path.isDone()) {
            Vec3 fromPathNode = horizontalDirection(Vec3.atCenterOf(path.getNextNodePos()).subtract(warden.position()));
            if (fromPathNode != null) {
                if (sourceOut != null && sourceOut.length > 0) {
                    sourceOut[0] = "path";
                }
                return fromPathNode;
            }
        }

        BlockPos lastIssuedNode = state.lastIssuedNode();
        if (lastIssuedNode != null) {
            Vec3 fromIssuedNode = horizontalDirection(Vec3.atCenterOf(lastIssuedNode).subtract(warden.position()));
            if (fromIssuedNode != null) {
                if (sourceOut != null && sourceOut.length > 0) {
                    sourceOut[0] = "issued_node";
                }
                return fromIssuedNode;
            }
        }

        Vec3 lookDirection = horizontalDirection(warden.getLookAngle());
        if (lookDirection != null && sourceOut != null && sourceOut.length > 0) {
            sourceOut[0] = "look";
        }
        return lookDirection;
    }

    private static Vec3 horizontalDirection(Vec3 direction) {
        if (direction == null) {
            return null;
        }
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return null;
        }
        return flat.normalize();
    }

    private static boolean isGrandEventCaveBreakWhitelist(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.POINTED_DRIPSTONE)
                || state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.REDSTONE_ORE)
                || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.LAPIS_ORE)
                || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE)
                || state.is(Blocks.DEEPSLATE_EMERALD_ORE);
    }

    private static double resolveFluidSurfaceY(ServerLevel level, BlockPos origin) {
        if (level == null || origin == null) {
            return Double.NaN;
        }
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                origin.getX(),
                Mth.clamp(origin.getY(), minY, maxY),
                origin.getZ());

        if (!level.getBlockState(cursor).getFluidState().is(FluidTags.WATER)) {
            if (!level.getBlockState(cursor.below()).getFluidState().is(FluidTags.WATER)) {
                return Double.NaN;
            }
            cursor.move(Direction.DOWN);
        }
        while (cursor.getY() < maxY && level.getBlockState(cursor.above()).getFluidState().is(FluidTags.WATER)) {
            cursor.move(Direction.UP);
        }
        if (!level.getBlockState(cursor).getFluidState().is(FluidTags.WATER)) {
            return Double.NaN;
        }
        return cursor.getY() + 1.0D;
    }

    private static void maybeApplyGrandEventSonicAssist(GrandEventState state, Warden warden, ServerPlayer target, long now) {
        if (state == null || warden == null || target == null) {
            return;
        }
        if (state.attackTarget() == null || !target.getUUID().equals(state.attackTarget())) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog("GRAND_EVENT shriek_assist skipped reason=not_aggro target={}", playerLabel(target));
            }
            return;
        }
        double distSqr = warden.distanceToSqr(target);
        if (distSqr < GRAND_EVENT_SONIC_ASSIST_MIN_DISTANCE_SQR) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog(
                        "GRAND_EVENT shriek_assist skipped reason=too_close target={} dist={}",
                        playerLabel(target),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distSqr)));
            }
            return;
        }
        if (!warden.hasLineOfSight(target)) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog(
                        "GRAND_EVENT shriek_assist skipped reason=no_los target={} dist={}",
                        playerLabel(target),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distSqr)));
            }
            return;
        }
        long lastAssistTick = state.lastSonicAssistTick();
        if (lastAssistTick != Long.MIN_VALUE && now - lastAssistTick < GRAND_EVENT_SONIC_ASSIST_MIN_INTERVAL_TICKS) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog(
                        "GRAND_EVENT shriek_assist skipped reason=cooldown target={} dist={} remainingTicks={}",
                        playerLabel(target),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distSqr)),
                        Math.max(0L, GRAND_EVENT_SONIC_ASSIST_MIN_INTERVAL_TICKS - (now - lastAssistTick)));
            }
            return;
        }
        MemoryModuleType<?> sonicCooldownMemory = resolveWardenSonicCooldownMemory();
        if (sonicCooldownMemory == null) {
            if (shouldSampleGrandEventRuntime(now)) {
                debugLog(
                        "GRAND_EVENT shriek_assist skipped reason=memory_unavailable target={} dist={}",
                        playerLabel(target),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(distSqr)));
            }
            return;
        }

        eraseWardenMemory(warden, sonicCooldownMemory);
        state.setLastSonicAssistTick(now);
        debugLog(
                "GRAND_EVENT shriek_assist fired reason=valid_window target={} dist={} cooldownTicks={}",
                playerLabel(target),
                String.format(Locale.ROOT, "%.2f", Math.sqrt(distSqr)),
                GRAND_EVENT_SONIC_ASSIST_MIN_INTERVAL_TICKS);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void eraseWardenMemory(Warden warden, MemoryModuleType<?> memory) {
        warden.getBrain().eraseMemory((MemoryModuleType) memory);
    }

    private static MemoryModuleType<?> resolveWardenSonicCooldownMemory() {
        if (WARDEN_SONIC_COOLDOWN_LOOKUP_DONE) {
            return WARDEN_SONIC_COOLDOWN_MEMORY;
        }
        WARDEN_SONIC_COOLDOWN_LOOKUP_DONE = true;
        try {
            for (java.lang.reflect.Field field : MemoryModuleType.class.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!MemoryModuleType.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                String normalized = field.getName().toLowerCase(Locale.ROOT);
                if (!normalized.contains("sonic") || !normalized.contains("cooldown")) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof MemoryModuleType<?> memory) {
                    WARDEN_SONIC_COOLDOWN_MEMORY = memory;
                    break;
                }
            }
            if (!WARDEN_SONIC_COOLDOWN_LOOKUP_LOGGED) {
                WARDEN_SONIC_COOLDOWN_LOOKUP_LOGGED = true;
                debugLog(
                        "GRAND_EVENT sonic-memory resolve result={}",
                        WARDEN_SONIC_COOLDOWN_MEMORY == null ? "not_found" : "ok");
            }
        } catch (Throwable t) {
            if (!WARDEN_SONIC_COOLDOWN_LOOKUP_LOGGED) {
                WARDEN_SONIC_COOLDOWN_LOOKUP_LOGGED = true;
                debugLog("GRAND_EVENT sonic-memory resolve failed: {}", t.toString());
            }
            WARDEN_SONIC_COOLDOWN_MEMORY = null;
        }
        return WARDEN_SONIC_COOLDOWN_MEMORY;
    }

    private static void preventGrandWardenDigBeforeEnd(Warden warden, long now) {
        try {
            // Root fix: force the DIG_COOLDOWN memory value itself.
            // Using setDigCooldown(...) only refreshes when VALUE_PRESENT, which
            // allows DIGGING to start if the memory is empty on first ticks.
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        } catch (Throwable t) {
            if (!WARDEN_KEEPALIVE_INVOKE_FAILURE_LOGGED) {
                WARDEN_KEEPALIVE_INVOKE_FAILURE_LOGGED = true;
                debugLog("GRAND_EVENT no-dig guard invoke failed: {}", t.toString());
            }
        }
    }

    private static Method resolveWardenIncreaseAngerMethod() {
        Method method = findNamedWardenAngerMethod("increaseAngerAt", false);
        if (method != null) {
            return method;
        }
        method = findNamedWardenAngerMethod("increaseAngerAt", true);
        if (method != null) {
            return method;
        }
        method = findNamedWardenAngerMethod("increaseAnger", false);
        if (method != null) {
            return method;
        }
        method = findNamedWardenAngerMethod("increaseAnger", true);
        if (method != null) {
            return method;
        }
        method = findWardenAngerMethod(false);
        if (method != null) {
            return method;
        }
        return findWardenAngerMethod(true);
    }

    private static Method findNamedWardenAngerMethod(String expectedName, boolean declaredOnly) {
        Method[] methods = declaredOnly ? Warden.class.getDeclaredMethods() : Warden.class.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(expectedName)) {
                continue;
            }
            if (!isWardenAngerMethodSignature(method)) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }

    private static boolean isWardenAngerMethodSignature(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 3) {
            return Entity.class.isAssignableFrom(params[0])
                    && params[1] == int.class
                    && params[2] == boolean.class;
        }
        if (params.length == 2) {
            return Entity.class.isAssignableFrom(params[0]) && params[1] == int.class;
        }
        if (params.length == 1) {
            return Entity.class.isAssignableFrom(params[0]);
        }
        return false;
    }

    private static Method findWardenAngerMethod(boolean declaredOnly) {
        Method[] methods = declaredOnly ? Warden.class.getDeclaredMethods() : Warden.class.getMethods();
        for (Method method : methods) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("anger") || !name.contains("increase") || name.contains("clear")) {
                continue;
            }
            if (!isWardenAngerMethodSignature(method)) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }

    private static void invokeWardenIncreaseAnger(Warden warden, LivingEntity focus) throws Exception {
        Class<?>[] params = WARDEN_INCREASE_ANGER_METHOD.getParameterTypes();
        if (params.length == 3) {
            WARDEN_INCREASE_ANGER_METHOD.invoke(warden, focus, 6, false);
            return;
        }
        if (params.length == 2) {
            WARDEN_INCREASE_ANGER_METHOD.invoke(warden, focus, 6);
            return;
        }
        if (params.length == 1) {
            WARDEN_INCREASE_ANGER_METHOD.invoke(warden, focus);
        }
    }

    private static void beginGrandEventAttack(
            ServerLevel level,
            GrandEventState state,
            List<ServerPlayer> zonePlayers,
            Warden warden,
            ServerPlayer target,
            long now,
            String admittedTrigger) {
        state.clearIssuedIntent();
        state.setAttackTarget(target.getUUID(), now);
        state.setLastAdmittedTrigger(admittedTrigger);
        state.markLatched(target.getUUID());
        state.clearSearchFocus();
        warden.setNoAi(false);
        warden.setTarget(target);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        applyGrandEventAggroTuning(state, warden, target, now);
        double chaseSpeed = resolveGrandEventChaseSpeed(warden, target);
        warden.getNavigation().moveTo(target, chaseSpeed);
        for (ServerPlayer player : zonePlayers) {
            playLocalSoundAt(
                    player,
                    target.blockPosition(),
                    SoundEvents.WARDEN_ROAR,
                    SoundSource.HOSTILE,
                    2.8F,
                    0.95F + level.random.nextFloat() * 0.08F);
        }
        debugLog(
                "GRAND_EVENT attack-start target={} dim={} admittedTrigger={}",
                playerLabel(target),
                level.dimension().location(),
                state.lastAdmittedTrigger());
    }

    private static void tickGrandEventAttack(
            ServerLevel level,
            GrandEventState state,
            List<ServerPlayer> zonePlayers,
            Warden warden,
            long now) {
        UUID targetId = state.attackTarget();
        if (targetId == null) {
            return;
        }

        ServerPlayer target = level.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || target.serverLevel() != level) {
            debugLog("GRAND_EVENT attack-release reason=target_missing runtime={} target={} dim={}", state.runtimeId(), targetId, level.dimension().location());
            state.clearAttack();
            clearGrandEventAggroTuning(state, warden, now);
            warden.setTarget(null);
            warden.setNoAi(true);
            enterGrandEventExitAuthority(level, state, warden, now, "attack_release_target_missing");
            if (!state.sinking()) {
                startGrandEventSinking(state, now, GRAND_EVENT_EMPTY_SCOPE_SINK_TICKS);
            }
            return;
        }

        if (!target.isAlive() || target.isDeadOrDying()) {
            for (ServerPlayer player : zonePlayers) {
                playLocalSoundAt(
                        player,
                        target.blockPosition(),
                        SoundEvents.WARDEN_LISTENING,
                        SoundSource.HOSTILE,
                        1.9F,
                        0.88F + level.random.nextFloat() * 0.08F);
            }
            state.clearAttack();
            clearGrandEventAggroTuning(state, warden, now);
            warden.setTarget(null);
            warden.setNoAi(true);
            enterGrandEventExitAuthority(level, state, warden, now, "attack_release_target_dead");
            if (!state.sinking()) {
                startGrandEventSinking(state, now, GRAND_EVENT_EMPTY_SCOPE_SINK_TICKS);
            }
            return;
        }

        double targetDistanceSqr = warden.distanceToSqr(target);
        if (targetDistanceSqr > GRAND_EVENT_ATTACK_RELEASE_DISTANCE_SQR) {
            debugLog(
                    "GRAND_EVENT attack-release reason=target_too_far runtime={} target={} dist={} maxDist=256.00",
                    state.runtimeId(),
                    playerLabel(target),
                    String.format(Locale.ROOT, "%.2f", Math.sqrt(targetDistanceSqr)));
            state.clearAttack();
            clearGrandEventAggroTuning(state, warden, now);
            warden.setTarget(null);
            warden.setNoAi(true);
            enterGrandEventExitAuthority(level, state, warden, now, "attack_release_target_too_far");
            if (!state.sinking()) {
                startGrandEventSinking(state, now, GRAND_EVENT_EMPTY_SCOPE_SINK_TICKS);
            }
            return;
        }

        warden.setNoAi(false);
        warden.setTarget(target);
        warden.setNoGravity(false);
        warden.noPhysics = false;
        applyGrandEventAggroTuning(state, warden, target, now);
        tickGrandEventCavePathBreak(level, state, warden, target, now);
        maybeApplyGrandEventSonicAssist(state, warden, target, now);
        if ((now % GRAND_EVENT_AGGRO_ANGER_REFRESH_TICKS) == 0L) {
            keepGrandWardenEngaged(warden, target, now);
        }
        boolean waterPursuit = warden.isInWaterOrBubble() || target.isInWaterOrBubble();
        if (waterPursuit || (now % GRAND_EVENT_ATTACK_PATH_REFRESH_TICKS) == 0L || warden.getNavigation().isDone()) {
            double chaseSpeed = resolveGrandEventChaseSpeed(warden, target);
            warden.getNavigation().moveTo(target, chaseSpeed);
        }
    }

    private static void tickGrandEventExit(
            ServerLevel level,
            GrandEventState state,
            List<ServerPlayer> zonePlayers,
            Warden warden,
            long now) {
        clearGrandEventAggroTuning(state, warden, now);
        if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
            debugLog(
                    "GRAND_EVENT search_suppressed_in_exit=true runtime={} source=tick_exit reason=exit_authority_lock",
                    state.runtimeId());
            debugLog(
                    "GRAND_EVENT exit_nav_owner=retreat_only runtime={} mode={} sinking={}",
                    state.runtimeId(),
                    state.coveredAtStart() ? "cave_retreat_then_sink" : "surface_retreat_then_sink",
                    state.sinking());
        }
        if (state.coveredAtStart()) {
            if (!state.sinking()) {
                warden.setNoAi(false);
                warden.setTarget(null);
                warden.setNoGravity(false);
                warden.noPhysics = false;

                if (state.caveExitStartTick() == Long.MIN_VALUE) {
                    state.setCaveExitStartTick(now);
                }

                ServerPlayer nearest = null;
                double nearestDistSqr = Double.MAX_VALUE;
                for (ServerPlayer player : zonePlayers) {
                    double distSqr = player.distanceToSqr(warden);
                    if (distSqr < nearestDistSqr) {
                        nearestDistSqr = distSqr;
                        nearest = player;
                    }
                }

                double nearestDist = nearest == null ? Double.POSITIVE_INFINITY : Math.sqrt(nearestDistSqr);
                if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                    debugLog(
                            "GRAND_EVENT cave_sink_guard runtime={} nearestPlayerDist={} min=8.00",
                            state.runtimeId(),
                            String.format(Locale.ROOT, "%.2f", nearestDist));
                }

                boolean canSink = nearest == null || nearestDistSqr >= GRAND_EVENT_CAVE_EXIT_RETREAT_MIN_PLAYER_DISTANCE_SQR;
                if ((now % GRAND_EVENT_MOVEMENT_DEBUG_SAMPLE_INTERVAL_TICKS) == 0L) {
                    debugLog(
                            "GRAND_EVENT authority.exit runtime={} mode=cave_retreat_then_sink nearestPlayerDist={} canSink={} attempts={}",
                            state.runtimeId(),
                            String.format(Locale.ROOT, "%.2f", nearestDist),
                            canSink,
                            state.caveExitRetreatAttempts());
                }

                if (canSink) {
                    state.resetCaveExitRetreatAttempts();
                    debugLog(
                            "GRAND_EVENT exit_mode=cave_retreat_then_sink runtime={} dim={} anchor={} reason=distance_reached",
                            state.runtimeId(),
                            level.dimension().location(),
                            state.anchorPos());
                    startGrandEventSinking(state, now, GRAND_EVENT_SINK_DURATION_TICKS);
                } else {
                    if (state.caveExitRetreatNode() == null
                            || now >= state.caveExitRetargetTick()
                            || warden.getNavigation().isDone()) {
                        int attempts = state.caveExitRetreatAttempts();
                        int minRadius = attempts >= 4
                                ? GRAND_EVENT_CAVE_EXIT_RETREAT_MIN_RADIUS + 4
                                : GRAND_EVENT_CAVE_EXIT_RETREAT_MIN_RADIUS;
                        int maxRadius = attempts >= 4
                                ? GRAND_EVENT_CAVE_EXIT_RETREAT_MAX_RADIUS + 8
                                : GRAND_EVENT_CAVE_EXIT_RETREAT_MAX_RADIUS;
                        boolean allowRecentOverlap = attempts >= 2;
                        GrandEventSearchResolution retreatResolution = resolveGrandEventSearchTargetPathFirst(
                                level,
                                state,
                                warden,
                                nearest,
                                minRadius,
                                maxRadius,
                                "cave_exit_retreat",
                                true,
                                false,
                                true,
                                true,
                                allowRecentOverlap,
                                Math.min(64, GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS));
                        if (retreatResolution.selectedNode() == null) {
                            retreatResolution = resolveGrandEventSearchTargetPathFirst(
                                    level,
                                    state,
                                    warden,
                                    nearest,
                                    GRAND_EVENT_CAVE_EXIT_RETREAT_MIN_RADIUS + 6,
                                    GRAND_EVENT_CAVE_EXIT_RETREAT_MAX_RADIUS + 14,
                                    "cave_exit_retreat_emergency",
                                    true,
                                    false,
                                    true,
                                    true,
                                    true,
                                    Math.min(64, GRAND_EVENT_SEARCH_RECOVERY_POOL_ATTEMPTS + 12));
                        }
                        if (retreatResolution.selectedNode() != null) {
                            BlockPos retreatNode = retreatResolution.selectedNode().immutable();
                            state.setCaveExitRetreatNode(retreatNode);
                            state.setCaveExitRetargetTick(now + GRAND_EVENT_CAVE_EXIT_RETREAT_RETARGET_TICKS);
                            warden.getNavigation().moveTo(
                                    retreatNode.getX() + 0.5D,
                                    retreatNode.getY(),
                                    retreatNode.getZ() + 0.5D,
                                    GRAND_EVENT_EXIT_SPEED);
                            state.incrementCaveExitRetreatAttempts();
                            debugLog(
                                    "GRAND_EVENT exit_mode=cave_retreat_then_sink runtime={} node={} nearestDist={} attempts={}",
                                    state.runtimeId(),
                                    retreatNode,
                                    String.format(Locale.ROOT, "%.2f", nearestDist),
                                    state.caveExitRetreatAttempts());
                        } else {
                            state.setCaveExitRetargetTick(now + GRAND_EVENT_CAVE_EXIT_RETREAT_RETARGET_TICKS);
                            debugLog(
                                    "GRAND_EVENT exit_mode=cave_retreat_then_sink runtime={} nearestDist={} attempts={} reason=no_reachable_retreat_node",
                                    state.runtimeId(),
                                    String.format(Locale.ROOT, "%.2f", nearestDist),
                                    state.caveExitRetreatAttempts());
                        }
                    }

                    if ((now - state.caveExitStartTick()) >= GRAND_EVENT_CAVE_EXIT_RETREAT_TIMEOUT_TICKS) {
                        if (state.caveExitRetreatAttempts() < GRAND_EVENT_CAVE_EXIT_RETREAT_MAX_ATTEMPTS) {
                            state.incrementCaveExitRetreatAttempts();
                        }
                        state.clearRecentSearchHistory();
                        state.setCaveExitStartTick(now);
                        state.setCaveExitRetargetTick(now);
                        state.setCaveExitRetreatNode(null);
                        debugLog(
                                "GRAND_EVENT exit_mode=cave_retreat_then_sink runtime={} timeout={}t nearestDist={} fallback=retarget_only attempts={}",
                                state.runtimeId(),
                                GRAND_EVENT_CAVE_EXIT_RETREAT_TIMEOUT_TICKS,
                                String.format(Locale.ROOT, "%.2f", nearestDist),
                                state.caveExitRetreatAttempts());
                    }
                }
            }
            tickGrandEventSinking(level, state, zonePlayers, warden, now);
            return;
        }

        if (!state.sinking()) {
            if ((now % 40L) == 0L) {
                debugLog(
                        "GRAND_EVENT exit_mode=surface_retreat_then_sink runtime={} dim={} unseenTicks={} nearestDist={}",
                        state.runtimeId(),
                        level.dimension().location(),
                        state.unseenTicks(),
                        zonePlayers.isEmpty()
                                ? "none"
                                : String.format(
                                        Locale.ROOT,
                                        "%.2f",
                                        Math.sqrt(zonePlayers.stream().mapToDouble(player -> player.distanceToSqr(warden)).min().orElse(0.0D))));
            }
            warden.setNoAi(false);
            warden.setTarget(null);
            warden.setNoGravity(false);
            warden.noPhysics = false;

            Vec3 current = warden.position();
            Vec3 anchorCenter = Vec3.atCenterOf(state.anchorPos());
            Vec3 away = current.subtract(anchorCenter);
            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            away = away.normalize().scale(18.0D);
            Vec3 target = current.add(away);
            if ((now % 10L) == 0L || warden.getNavigation().isDone()) {
                warden.getNavigation().moveTo(target.x, target.y, target.z, 0.90D);
            }
            float yaw = (float) (Mth.atan2(away.z, away.x) * (180.0D / Math.PI)) - 90.0F;
            warden.setYRot(yaw);
            warden.yBodyRot = yaw;
            warden.yHeadRot = yaw;

            if (isGrandEventWardenSeen(level, warden, zonePlayers)) {
                state.setUnseenTicks(0);
            } else {
                state.setUnseenTicks(state.unseenTicks() + 1);
            }

            if (state.unseenTicks() >= GRAND_EVENT_UNSEEN_REQUIRED_TICKS) {
                startGrandEventSinking(state, now, GRAND_EVENT_SINK_DURATION_TICKS);
            }
        }

        if (state.sinking()) {
            tickGrandEventSinking(level, state, zonePlayers, warden, now);
        }
    }

    private static boolean isGrandEventWardenSeen(ServerLevel level, Warden warden, List<ServerPlayer> zonePlayers) {
        Vec3 wardenEyes = warden.getEyePosition();
        for (ServerPlayer player : zonePlayers) {
            if (player.distanceToSqr(wardenEyes) > (double) GRAND_WARDEN_ZONE_RADIUS * GRAND_WARDEN_ZONE_RADIUS) {
                continue;
            }
            if (isOutsideViewCone(player, wardenEyes, 0.18D)) {
                continue;
            }
            if (player.hasLineOfSight(warden)) {
                return true;
            }
        }
        return false;
    }

    private static void startGrandEventSinking(GrandEventState state, long now, int durationTicks) {
        if (state.sinking()) {
            return;
        }
        state.resetCaveExitRetreat();
        state.setSinking(true);
        state.setSinkDigSoundPlayed(false);
        state.setSinkEndTick(now + Math.max(10, durationTicks));
        debugLog(
                "GRAND_EVENT sink_dig_pose {} runtime={} sinkDuration={}t",
                WARDEN_DIG_POSE == null ? "off" : "on",
                state.runtimeId(),
                Math.max(10, durationTicks));
    }

    private static void tickGrandEventSinking(
            ServerLevel level,
            GrandEventState state,
            List<ServerPlayer> zonePlayers,
            Warden warden,
            long now) {
        clearGrandEventAggroTuning(state, warden, now);
        warden.setNoAi(true);
        warden.setTarget(null);
        warden.setNoGravity(true);
        warden.noPhysics = true;
        if (WARDEN_DIG_POSE != null) {
            if (warden.getPose() != WARDEN_DIG_POSE) {
                warden.setPose(WARDEN_DIG_POSE);
            }
            if (!state.sinkDigSoundPlayed()) {
                level.playSound(
                        null,
                        warden.blockPosition(),
                        SoundEvents.WARDEN_DIG,
                        SoundSource.HOSTILE,
                        1.0F,
                        0.95F + level.random.nextFloat() * 0.12F);
                state.setSinkDigSoundPlayed(true);
            }
        }
        Vec3 pos = warden.position();
        double minY = level.getMinBuildHeight() - 4.0D;
        warden.setPos(pos.x, Math.max(minY, pos.y - GRAND_EVENT_SINK_STEP), pos.z);

        if (now >= state.sinkEndTick()) {
            finishGrandEvent(level, state, true, now, "completed");
            for (ServerPlayer player : zonePlayers) {
                playLocalSoundAt(
                        player,
                        warden.blockPosition(),
                        SoundEvents.WARDEN_AGITATED,
                        SoundSource.HOSTILE,
                        1.6F,
                        0.70F + level.random.nextFloat() * 0.08F);
            }
        }
    }

    private static void tickGrandEventPausedSpecials(ServerLevel level, GrandEventState state, long now) {
        if (level == null || state == null || state.ended()) {
            return;
        }

        Map<UUID, PausedSpecialSnapshot> pausedByEntity = ACTIVE_GRAND_PAUSED_SPECIALS.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        pausedByEntity.entrySet().removeIf(entry -> {
            Entity raw = level.getEntity(entry.getKey());
            return !(raw instanceof Mob mob) || !mob.isAlive() || !UncannyEntityRegistry.isSpecialEntity(mob.getType());
        });

        Vec3 anchorCenter = Vec3.atCenterOf(state.anchorPos());
        AABB bounds = new AABB(
                anchorCenter.x - GRAND_EVENT_SPECIAL_PAUSE_RADIUS,
                level.getMinBuildHeight(),
                anchorCenter.z - GRAND_EVENT_SPECIAL_PAUSE_RADIUS,
                anchorCenter.x + GRAND_EVENT_SPECIAL_PAUSE_RADIUS,
                level.getMaxBuildHeight(),
                anchorCenter.z + GRAND_EVENT_SPECIAL_PAUSE_RADIUS);
        List<Mob> specials = level.getEntitiesOfClass(
                Mob.class,
                bounds,
                mob -> mob != null
                        && mob.isAlive()
                        && UncannyEntityRegistry.isSpecialEntity(mob.getType())
                        && !mob.getUUID().equals(state.wardenUuid()));

        int pausedCount = 0;
        for (Mob special : specials) {
            pausedCount++;
            UUID specialId = special.getUUID();
            PausedSpecialSnapshot existing = pausedByEntity.get(specialId);
            if (existing == null) {
                UUID targetId = special.getTarget() != null ? special.getTarget().getUUID() : null;
                pausedByEntity.put(specialId, new PausedSpecialSnapshot(specialId, special.isNoAi(), targetId));
                debugLog(
                        "GRAND_EVENT special_pause apply uuid={} type={} dist={}",
                        specialId,
                        special.getType().toShortString(),
                        String.format(Locale.ROOT, "%.2f", Math.sqrt(special.blockPosition().distSqr(state.anchorPos()))));
            }
            if (special.getNavigation() != null) {
                special.getNavigation().stop();
            }
            special.setDeltaMovement(Vec3.ZERO);
            special.setNoAi(true);
            special.addTag(GRAND_EVENT_PAUSED_SPECIAL_TAG);
        }

        if (shouldSampleGrandEventRuntime(now)) {
            debugLog(
                    "GRAND_EVENT special_pause count={} radius={} runtime={}",
                    pausedCount,
                    String.format(Locale.ROOT, "%.1f", GRAND_EVENT_SPECIAL_PAUSE_RADIUS),
                    state.runtimeId());
        }
    }

    private static void restoreGrandEventPausedSpecials(ServerLevel level, GrandEventState state, long now, String reason) {
        Map<UUID, PausedSpecialSnapshot> pausedByEntity = ACTIVE_GRAND_PAUSED_SPECIALS.remove(level.dimension());
        if (pausedByEntity == null || pausedByEntity.isEmpty()) {
            return;
        }

        for (PausedSpecialSnapshot snapshot : pausedByEntity.values()) {
            Entity raw = level.getEntity(snapshot.entityId());
            if (!(raw instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }

            mob.removeTag(GRAND_EVENT_PAUSED_SPECIAL_TAG);
            mob.setNoAi(snapshot.hadNoAi());
            if (!snapshot.hadNoAi() && snapshot.targetId() != null) {
                Entity targetRaw = level.getEntity(snapshot.targetId());
                if (targetRaw instanceof LivingEntity living && living.isAlive()) {
                    mob.setTarget(living);
                }
            }

            debugLog(
                    "GRAND_EVENT special_pause resume uuid={} type={} reason={}",
                    snapshot.entityId(),
                    mob.getType().toShortString(),
                    reason);
        }

        if (shouldSampleGrandEventRuntime(now) || UncannyConfig.DEBUG_LOGS.get()) {
            debugLog(
                    "GRAND_EVENT special_pause cleared count={} dim={} reason={}",
                    pausedByEntity.size(),
                    level.dimension().location(),
                    reason);
        }
    }

    private static void finishGrandEvent(
            ServerLevel level,
            GrandEventState state,
            boolean discardWarden,
            long now,
            String reason) {
        GrandEventState active = ACTIVE_GRAND_EVENTS.get(level.dimension());
        if (active != state) {
            return;
        }
        ACTIVE_GRAND_EVENTS.remove(level.dimension());
        state.setEnded(true);
        restoreGrandEventPausedSpecials(level, state, now, reason);
        for (UUID trackedPlayerId : state.trackedPlayers()) {
            GRAND_EVENT_RECENT_AUDIBLE_ACTION_TICKS.remove(trackedPlayerId);
        }

        Entity raw = level.getEntity(state.wardenUuid());
        if (discardWarden && raw != null && raw.isAlive()) {
            raw.discard();
            if (raw.isAlive()) {
                raw.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        List<ServerPlayer> players = gatherGrandEventZonePlayers(level, state.anchorPos());
        for (ServerPlayer player : players) {
            player.connection.send(new ClientboundClearTitlesPacket(true));
        }
        debugLog(
                "GRAND_EVENT end dim={} reason={} duration={}s discardWarden={} remainingPlayers={}",
                level.dimension().location(),
                reason,
                ticksToSeconds(now - state.startedTick()),
                discardWarden,
                players.size());
        debugLog("GRAND_EVENT pause_auto off dim={}", level.dimension().location());
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static boolean isTensionBuilderActive(UncannyWorldState state, long now) {
        long activeUntil = state.getTensionBuilderEndTick();
        return activeUntil != Long.MIN_VALUE && now < activeUntil;
    }

    private static boolean isTensionBuilderEventPauseActive(ServerLevel level, UncannyWorldState state, long now) {
        return level != null
                && state != null
                && isTensionBuilderActive(state, now)
                && !isGrandEventAutoPauseActive(level);
    }

    private static long rollGrandEventRollDelayTicks(ServerLevel level) {
        int seconds = rollRangeInclusive(level, GRAND_EVENT_ROLL_MIN_SECONDS, GRAND_EVENT_ROLL_MAX_SECONDS);
        return seconds * 20L;
    }

    private static int rollRangeInclusive(ServerLevel level, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + level.random.nextInt(max - min + 1);
    }

    private static void maybeTriggerRandomEvent(ServerPlayer player, long now) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        UncannyPhase phase = state.getPhase();
        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        boolean tensionActive = isTensionBuilderActive(state, now);
        if (tensionActive) {
            debugLog(
                    "AUTO_EVENT suppressed reason=tension_builder player={} remaining={}s",
                    playerLabel(player),
                    ticksToSeconds(state.getTensionBuilderEndTick() - now));
            return;
        }

        if (!passesGlobalAndRespawnChecks(state, player, phase, profile, danger, now)) {
            debugLog("AUTO_EVENT skip checks player={} phase={} profile={} danger={}", playerLabel(player), phase.index(), profile, danger);
            return;
        }

        boolean forcedBySilence = isForcedBySilence(state, phase, profile, danger, now);
        double roll = player.serverLevel().random.nextDouble();
        double triggerChance = getAutoTriggerChance(phase, profile, danger);
        if (!forcedBySilence && roll > triggerChance) {
            debugLog(
                    "AUTO_EVENT no-trigger player={} phase={} profile={} danger={} roll={} chance={}",
                    playerLabel(player),
                    phase.index(),
                    profile,
                    danger,
                    String.format(Locale.ROOT, "%.4f", roll),
                    String.format(Locale.ROOT, "%.4f", triggerChance));
            return;
        }

        List<EventChoice> choices = new ArrayList<>();
        addEventChoiceIfReady(choices, player, "footsteps", profileScaledWeight("footsteps", 16, profile, danger), now);
        addEventChoiceIfReady(choices, player, "corrupt_message", profileScaledWeight("corrupt_message", 18, profile, danger), now);
        addEventChoiceIfReady(choices, player, "flash_red", profileScaledWeight("flash_red", 8, profile, danger), now);
        addEventChoiceIfReady(choices, player, "false_fall", profileScaledWeight("false_fall", 9, profile, danger), now);

        if (findBreakableArmorSlot(player) != null) {
            addEventChoiceIfReady(choices, player, "armor_break", profileScaledWeight("armor_break", 8, profile, danger), now);
        }
        if (isNearBase(player, server)) {
            addEventChoiceIfReady(choices, player, "base_replay", profileScaledWeight("base_replay", 16, profile, danger), now);
        }
        if (!player.serverLevel().canSeeSky(player.blockPosition())) {
            addEventChoiceIfReady(choices, player, "ghost_miner", profileScaledWeight("ghost_miner", 11, profile, danger), now);
            addEventChoiceIfReady(choices, player, "cave_collapse", profileScaledWeight("cave_collapse", 8, profile, danger), now);
        }

        if (phase.index() >= UncannyPhase.PHASE_2.index()) {
            addEventChoiceIfReady(choices, player, "bell", profileScaledWeight("bell", 14, profile, danger), now);
            addEventChoiceIfReady(choices, player, "void_silence", profileScaledWeight("void_silence", 7, profile, danger), now);
            addEventChoiceIfReady(choices, player, "aquatic_steps", profileScaledWeight("aquatic_steps", 10, profile, danger), now);
            addEventChoiceIfReady(choices, player, "animal_stare_lock", profileScaledWeight("animal_stare_lock", 3, profile, danger), now);
            addEventChoiceIfReady(choices, player, "workbench_reject", profileScaledWeight("workbench_reject", 1, profile, danger), now);
        }

        if (phase.index() >= UncannyPhase.PHASE_3.index()) {
            addEventChoiceIfReady(choices, player, "blackout", profileScaledWeight("blackout", 7, profile, danger), now);
            addEventChoiceIfReady(choices, player, "flash", profileScaledWeight("flash", 4, profile, danger), now);
            addEventChoiceIfReady(choices, player, "false_injury", profileScaledWeight("false_injury", 4, profile, danger), now);
            addEventChoiceIfReady(choices, player, "forced_drop", profileScaledWeight("forced_drop", 2, profile, danger), now);
            addEventChoiceIfReady(choices, player, "door_inversion", profileScaledWeight("door_inversion", 9, profile, danger), now);
            addEventChoiceIfReady(choices, player, "living_ore", profileScaledWeight("living_ore", 8, profile, danger), now);
            addEventChoiceIfReady(choices, player, "giant_sun", profileScaledWeight("giant_sun", 5, profile, danger), now);
            addEventChoiceIfReady(choices, player, "compass_liar", profileScaledWeight("compass_liar", 2, profile, danger), now);
            addEventChoiceIfReady(choices, player, "misplaced_light", profileScaledWeight("misplaced_light", 3, profile, danger), now);
            addEventChoiceIfReady(choices, player, "pet_refusal", profileScaledWeight("pet_refusal", 2, profile, danger), now);
            addEventChoiceIfReady(choices, player, "hotbar_wrong_count", profileScaledWeight("hotbar_wrong_count", 4, profile, danger), now);
            addEventChoiceIfReady(choices, player, "corrupt_toast", profileScaledWeight("corrupt_toast", 2, profile, danger), now);
        }

        if (phase.index() >= UncannyPhase.PHASE_4.index()) {
            addEventChoiceIfReady(choices, player, "asphyxia", profileScaledWeight("asphyxia", 6, profile, danger), now);
            addEventChoiceIfReady(choices, player, "phantom_harvest", profileScaledWeight("phantom_harvest", 8, profile, danger), now);
            addEventChoiceIfReady(choices, player, "projected_shadow", profileScaledWeight("projected_shadow", 8, profile, danger), now);
            addEventChoiceIfReady(choices, player, "hunter_fog", profileScaledWeight("hunter_fog", 7, profile, danger), now);
        }

        if (choices.isEmpty()) {
            debugLog("AUTO_EVENT no-choices player={} phase={} profile={} danger={}", playerLabel(player), phase.index(), profile, danger);
            return;
        }

        String triggeredKey = triggerWeightedChoiceWithFallback(player, choices);
        boolean triggered = triggeredKey != null;

        if (!triggered && forcedBySilence && !tensionActive) {
            triggered = triggerForcedFallback(player, phase);
            if (triggered) {
                triggeredKey = "forced_fallback";
            }
        }

        if (triggered && triggeredKey != null) {
            onAutoEventTriggered(player, triggeredKey, now, phase, profile, danger);
            debugLog("AUTO_EVENT triggered key={} player={} phase={} profile={} danger={}", triggeredKey, playerLabel(player), phase.index(), profile, danger);
        } else {
            debugLog("AUTO_EVENT failed-all player={} phase={} profile={} danger={}", playerLabel(player), phase.index(), profile, danger);
        }
    }

    private static void maybeTriggerAmbientSoundEvent(ServerPlayer player, long now) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        if (!state.isSystemEnabled()) {
            return;
        }
        if (isTensionBuilderEventPauseActive(player.serverLevel(), state, now)) {
            debugLog(
                    "AUTO_AMBIENT suppressed reason=tension_builder player={} remaining={}s",
                    playerLabel(player),
                    ticksToSeconds(state.getTensionBuilderEndTick() - now));
            return;
        }

        UncannyPhase phase = state.getPhase();
        int profile = getIntensityProfile();
        int danger = getDangerLevel();

        long ambientGlobalCooldownTicks = computeAmbientGlobalCooldownTicks(phase, profile, danger);
        long lastAmbientTick = LAST_AMBIENT_EVENT_TICKS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (isCooldownActive(lastAmbientTick, now, ambientGlobalCooldownTicks)) {
            return;
        }

        double triggerChance = getAmbientTriggerChance(phase, profile, danger);
        double roll = player.serverLevel().random.nextDouble();
        if (roll > triggerChance) {
            debugLog(
                    "AUTO_AMBIENT no-trigger player={} phase={} profile={} danger={} roll={} chance={}",
                    playerLabel(player),
                    phase.index(),
                    profile,
                    danger,
                    String.format(Locale.ROOT, "%.4f", roll),
                    String.format(Locale.ROOT, "%.4f", triggerChance));
            return;
        }

        List<EventChoice> choices = new ArrayList<>();
        addAmbientEventChoiceIfReady(choices, player, "false_container_open", profileScaledWeight("false_container_open", 7, profile, danger), now);
        addAmbientEventChoiceIfReady(choices, player, "bucket_drip", profileScaledWeight("bucket_drip", 6, profile, danger), now);

        ToolAnswerContext recentToolAnswer = LAST_TOOL_ANSWER_CONTEXT.get(player.getUUID());
        if (phase.index() >= UncannyPhase.PHASE_2.index()) {
            addAmbientEventChoiceIfReady(choices, player, "furnace_breath", profileScaledWeight("furnace_breath", 6, profile, danger), now);
            addAmbientEventChoiceIfReady(choices, player, "lever_answer", profileScaledWeight("lever_answer", 6, profile, danger), now);
            addAmbientEventChoiceIfReady(choices, player, "pressure_plate_reply", profileScaledWeight("pressure_plate_reply", 6, profile, danger), now);
            addAmbientEventChoiceIfReady(choices, player, "campfire_cough", profileScaledWeight("campfire_cough", 5, profile, danger), now);
            if (hasRecentToolAnswerContext(player, recentToolAnswer, 20L * 20L)) {
                addAmbientEventChoiceIfReady(choices, player, "tool_answer", profileScaledWeight("tool_answer", 6, profile, danger), now);
            }
        }

        if (choices.isEmpty()) {
            debugLog("AUTO_AMBIENT no-choices player={} phase={} profile={} danger={}", playerLabel(player), phase.index(), profile, danger);
            return;
        }

        String triggeredKey = triggerWeightedChoiceWithFallback(player, choices);
        if (triggeredKey == null) {
            debugLog("AUTO_AMBIENT failed-all player={} phase={} profile={} danger={}", playerLabel(player), phase.index(), profile, danger);
            return;
        }

        markAmbientEventCooldown(player, triggeredKey, now, phase, profile, danger);
        LAST_AMBIENT_EVENT_TICKS.put(player.getUUID(), now);
        debugLog("AUTO_AMBIENT triggered key={} player={} phase={} profile={} danger={}", triggeredKey, playerLabel(player), phase.index(), profile, danger);
    }

    private static void addEventChoiceIfReady(List<EventChoice> choices, ServerPlayer player, String key, int weight, long now) {
        if (isTensionBuilderActiveForPlayer(player, now)) {
            return;
        }
        if (weight <= 0 || isEventOnCooldown(player, key, now)) {
            return;
        }
        choices.add(new EventChoice(key, weight));
    }

    private static boolean isTensionBuilderActiveForPlayer(ServerPlayer player, long now) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        return isTensionBuilderActive(UncannyWorldState.get(server), now);
    }

    private static boolean isEventOnCooldown(ServerPlayer player, String eventKey, long now) {
        Map<String, Long> perPlayer = EVENT_COOLDOWNS.get(player.getUUID());
        if (perPlayer == null) {
            return false;
        }
        Long until = perPlayer.get(eventKey);
        return until != null && now < until;
    }

    private static void addAmbientEventChoiceIfReady(List<EventChoice> choices, ServerPlayer player, String key, int weight, long now) {
        if (weight <= 0 || isAmbientEventOnCooldown(player, key, now)) {
            return;
        }
        choices.add(new EventChoice(key, weight));
    }

    private static boolean isAmbientEventOnCooldown(ServerPlayer player, String eventKey, long now) {
        Map<String, Long> perPlayer = AMBIENT_EVENT_COOLDOWNS.get(player.getUUID());
        if (perPlayer == null) {
            return false;
        }
        Long until = perPlayer.get(eventKey);
        return until != null && now < until;
    }

    private static void onAutoEventTriggered(
            ServerPlayer player,
            String eventKey,
            long now,
            UncannyPhase phase,
            int profile,
            int danger) {
        if (player.getServer() == null) {
            return;
        }
        UncannyWorldState.get(player.getServer()).setLastGlobalEventTick(now);
        markEventCooldown(player, eventKey, now, phase, profile, danger);
        if (isSpecialEntityEventKey(eventKey)) {
            LAST_SPECIAL_ENTITY_EVENT_TICKS.put(player.getUUID(), now);
        }
    }

    private static void markEventCooldown(ServerPlayer player, String eventKey, long now, UncannyPhase phase, int profile, int danger) {
        EventSeverity severity = getEventSeverity(eventKey);
        long cooldownTicks = rollEventCooldownTicks(player.serverLevel(), phase, profile, danger, severity);
        long explicitCooldownTicks = explicitEventCooldownTicks(eventKey);
        if (explicitCooldownTicks > 0L) {
            cooldownTicks = explicitCooldownTicks;
        }
        if ("blackout".equals(eventKey)) {
            cooldownTicks = (long) Math.round(cooldownTicks * 2.35D);
        } else if ("footsteps".equals(eventKey)) {
            cooldownTicks = (long) Math.round(cooldownTicks * 1.95D);
        }
        EVENT_COOLDOWNS.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>()).put(eventKey, now + cooldownTicks);
    }

    private static void markAmbientEventCooldown(ServerPlayer player, String eventKey, long now, UncannyPhase phase, int profile, int danger) {
        long cooldownTicks = explicitAmbientEventCooldownTicks(eventKey);
        if (cooldownTicks <= 0L) {
            cooldownTicks = Math.max(20L, (long) Math.round(rollEventCooldownTicks(player.serverLevel(), phase, profile, danger, EventSeverity.LIGHT) * 0.65D));
        }
        AMBIENT_EVENT_COOLDOWNS.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>()).put(eventKey, now + cooldownTicks);
    }

    private static long explicitAmbientEventCooldownTicks(String eventKey) {
        int seconds = switch (eventKey) {
            case "false_container_open" -> 190;
            case "bucket_drip" -> 210;
            case "furnace_breath" -> 250;
            case "lever_answer" -> 190;
            case "pressure_plate_reply" -> 190;
            case "campfire_cough" -> 250;
            case "tool_answer" -> 300;
            default -> 0;
        };
        return seconds <= 0 ? 0L : seconds * 20L;
    }

    private static long explicitEventCooldownTicks(String eventKey) {
        int seconds = switch (eventKey) {
            case "animal_stare_lock" -> COOLDOWN_ANIMAL_STARE_LOCK_SECONDS;
            case "compass_liar" -> COOLDOWN_COMPASS_LIAR_SECONDS;
            case "furnace_breath" -> COOLDOWN_FURNACE_BREATH_SECONDS;
            case "misplaced_light" -> COOLDOWN_MISPLACED_LIGHT_SECONDS;
            case "pet_refusal" -> COOLDOWN_PET_REFUSAL_SECONDS;
            case "workbench_reject" -> COOLDOWN_WORKBENCH_REJECT_SECONDS;
            case "false_container_open" -> COOLDOWN_FALSE_CONTAINER_OPEN_SECONDS;
            case "lever_answer" -> COOLDOWN_LEVER_ANSWER_SECONDS;
            case "pressure_plate_reply" -> COOLDOWN_PRESSURE_PLATE_REPLY_SECONDS;
            case "campfire_cough" -> COOLDOWN_CAMPFIRE_COUGH_SECONDS;
            case "bucket_drip" -> COOLDOWN_BUCKET_DRIP_SECONDS;
            case "hotbar_wrong_count" -> COOLDOWN_HOTBAR_WRONG_COUNT_SECONDS;
            case "false_recipe_toast", "corrupt_toast" -> COOLDOWN_FALSE_RECIPE_TOAST_SECONDS;
            case "tool_answer" -> COOLDOWN_TOOL_ANSWER_SECONDS;
            default -> 0;
        };
        return seconds <= 0 ? 0L : seconds * 20L;
    }

    private static String triggerWeightedChoiceWithFallback(ServerPlayer player, List<EventChoice> initialChoices) {
        List<EventChoice> remaining = new ArrayList<>(initialChoices);
        while (!remaining.isEmpty()) {
            int totalWeight = remaining.stream().mapToInt(EventChoice::weight).sum();
            if (totalWeight <= 0) {
                debugLog("AUTO_EVENT weighted-fallback exhausted-weight player={}", playerLabel(player));
                return null;
            }

            EventChoice selected = pickWeightedChoice(remaining, player.serverLevel().random.nextInt(totalWeight));
            if (selected == null) {
                debugLog("AUTO_EVENT weighted-fallback selection-null player={}", playerLabel(player));
                return null;
            }

            debugLog("AUTO_EVENT attempt key={} player={} remainingChoices={}", selected.key(), playerLabel(player), remaining.size());
            if (triggerEventByKey(player, selected.key())) {
                debugLog("AUTO_EVENT success key={} player={}", selected.key(), playerLabel(player));
                return selected.key();
            }
            debugLog("AUTO_EVENT fail key={} player={}", selected.key(), playerLabel(player));
            remaining.remove(selected);
        }
        debugLog("AUTO_EVENT no-success player={}", playerLabel(player));
        return null;
    }

    private static boolean triggerEventByKey(ServerPlayer player, String eventKey) {
        return switch (eventKey) {
            case "blackout" -> triggerTotalBlackout(player);
            case "footsteps" -> triggerFootstepsBehind(player);
            case "flash" -> triggerFlashError(player);
            case "base_replay" -> triggerBaseReplay(player);
            case "bell" -> triggerBell(player);
            case "watcher" -> UncannyWatcherSystem.spawnWatcherFromEvents(player);
            case "shadow" -> spawnShadow(player, true);
            case "hurler" -> spawnHurler(player);
            case "knocker" -> spawnKnocker(player);
            case "stalker" -> spawnStalker(player);
            case "pulse" -> spawnPulse(player);
            case "flash_red" -> triggerFlashRed(player);
            case "void_silence" -> triggerVoidSilence(player);
            case "false_fall" -> triggerFalseFall(player);
            case "ghost_miner" -> triggerGhostMiner(player);
            case "cave_collapse" -> triggerCaveCollapse(player);
            case "false_injury" -> triggerFalseInjury(player);
            case "forced_drop" -> triggerForcedDrop(player);
            case "corrupt_message" -> triggerCorruptedMessage(player);
            case "animal_stare_lock" -> triggerAnimalStareLock(player);
            case "bedside_open" -> triggerBedsideOpen(player);
            case "compass_liar" -> triggerCompassLiar(player);
            case "furnace_breath" -> triggerFurnaceBreath(player);
            case "misplaced_light" -> triggerMisplacedLight(player);
            case "pet_refusal" -> triggerPetRefusal(player);
            case "workbench_reject" -> triggerWorkbenchReject(player);
            case "false_container_open" -> triggerFalseContainerOpen(player);
            case "lever_answer" -> triggerLeverAnswer(player, false);
            case "pressure_plate_reply" -> triggerPressurePlateReply(player);
            case "campfire_cough" -> triggerCampfireCough(player);
            case "bucket_drip" -> triggerBucketDrip(player);
            case "hotbar_wrong_count" -> triggerHotbarWrongCount(player);
            case "false_recipe_toast", "corrupt_toast" -> triggerFalseRecipeToast(player);
            case "tool_answer" -> triggerToolAnswer(player);
            case "asphyxia" -> triggerAsphyxia(player);
            case "armor_break" -> triggerArmorBreak(player);
            case "aquatic_steps" -> triggerAquaticSteps(player);
            case "door_inversion" -> triggerDoorInversion(player);
            case "phantom_harvest" -> triggerPhantomHarvest(player);
            case "living_ore" -> triggerLivingOre(player);
            case "projected_shadow" -> triggerProjectedShadow(player);
            case "giant_sun" -> triggerGiantSun(player);
            case "hunter_fog" -> triggerHunterFog(player);
            case "grand_event", "grand_event_warden" -> triggerGrandEventWarden(player);
            case "grand_event_stop" -> triggerGrandEventStop(player);
            case "tension_builder_start" -> triggerTensionBuilderStart(player);
            case "tension_builder_stop" -> triggerTensionBuilderStop(player);
            default -> false;
        };
    }

    public static boolean triggerEventVariant(ServerPlayer player, String rawEventKey, String rawVariantKey) {
        if (player.getServer() == null || !UncannyWorldState.get(player.getServer()).isSystemEnabled()) {
            return false;
        }
        if (rawEventKey == null || rawVariantKey == null) {
            return false;
        }

        String eventKey = rawEventKey.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        String variantKey = rawVariantKey.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (eventKey) {
            case "footsteps" -> {
                FootstepPattern variant = parseEnumVariant(FootstepPattern.class, variantKey);
                if (variant == null) {
                    yield false;
                }
                long now = player.getServer().getTickCount();
                yield triggerFootstepsPattern(player, variant, now, true);
            }
            case "asphyxia" -> {
                AsphyxiaVariant variant = parseEnumVariant(AsphyxiaVariant.class, variantKey);
                if (variant == null && "heavy_lung".equals(variantKey)) {
                    variant = AsphyxiaVariant.HEAVY_LUNGS;
                }
                yield triggerAsphyxiaVariant(player, variant);
            }
            case "armor_break", "armorbreak" -> {
                ArmorBreakVariant variant = parseEnumVariant(ArmorBreakVariant.class, variantKey);
                yield triggerArmorBreakVariant(player, variant);
            }
            case "aquatic_steps", "aquaticsteps" -> {
                AquaticStepsVariant variant = parseEnumVariant(AquaticStepsVariant.class, variantKey);
                yield triggerAquaticStepsVariant(player, variant);
            }
            case "door_inversion", "doorinversion" -> {
                DoorInversionVariant variant = parseEnumVariant(DoorInversionVariant.class, variantKey);
                yield triggerDoorInversionVariant(player, variant);
            }
            case "phantom_harvest", "phantomharvest" -> {
                PhantomHarvestVariant variant = parseEnumVariant(PhantomHarvestVariant.class, variantKey);
                yield triggerPhantomHarvestVariant(player, variant);
            }
            case "phantom_mode", "phantommode", "phantom" -> {
                if ("lantern_eater".equals(variantKey) || "lanterneater".equals(variantKey)) {
                    yield spawnPhantomLanternEaterForCommand(player);
                }
                yield false;
            }
            case "living_ore", "livingore" -> {
                LivingOreVariant variant = parseEnumVariant(LivingOreVariant.class, variantKey);
                yield triggerLivingOreVariant(player, variant);
            }
            case "projected_shadow", "projectedshadow" -> {
                ProjectedShadowVariant variant = parseEnumVariant(ProjectedShadowVariant.class, variantKey);
                yield triggerProjectedShadowVariant(player, variant);
            }
            default -> false;
        };
    }

    private static <E extends Enum<E>> E parseEnumVariant(Class<E> enumClass, String rawVariantKey) {
        if (rawVariantKey == null) {
            return null;
        }
        String normalized = rawVariantKey.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException ignored) {
            String compact = normalized.replace("_", "");
            for (E value : enumClass.getEnumConstants()) {
                if (value.name().replace("_", "").equalsIgnoreCase(compact)) {
                    return value;
                }
            }
            return null;
        }
    }

    private static void maybeTriggerSpecialEntityEncounter(ServerPlayer player, long now) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        UncannyPhase phase = state.getPhase();
        if (phase.index() < UncannyPhase.PHASE_2.index()) {
            debugLog("SPECIAL skip low-phase player={} phase={}", playerLabel(player), phase.index());
            return;
        }
        if (isTensionBuilderActive(state, now)) {
            debugLog("SPECIAL skip tension-builder player={} remaining={}s", playerLabel(player), ticksToSeconds(state.getTensionBuilderEndTick() - now));
            return;
        }

        if (shouldBlockSpecialSpawn(player)) {
            debugLog("SPECIAL skip water-or-boat player={}", playerLabel(player));
            return;
        }

        int profile = getIntensityProfile();
        int danger = getDangerLevel();

        long nextCheck = NEXT_SPECIAL_ENTITY_CHECK_TICKS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now < nextCheck) {
            return;
        }
        NEXT_SPECIAL_ENTITY_CHECK_TICKS.put(
                player.getUUID(),
                now + rollSpecialEntityCheckIntervalTicks(phase, profile, player.serverLevel()));

        Long lastRespawnTick = state.getLastRespawnTick(player.getUUID());
        long respawnGraceTicks = UncannyConfig.RESPAWN_GRACE_SECONDS.get() * 20L;
        if (isCooldownActive(lastRespawnTick, now, respawnGraceTicks)) {
            debugLog("SPECIAL skip respawn-grace player={}", playerLabel(player));
            return;
        }

        long specialGlobalCooldownTicks = computeSpecialEntityGlobalCooldownTicks(phase, profile, danger);
        Long lastSpecialTick = LAST_SPECIAL_ENTITY_EVENT_TICKS.get(player.getUUID());
        if (isCooldownActive(lastSpecialTick, now, specialGlobalCooldownTicks)) {
            debugLog("SPECIAL skip global-cooldown player={} remaining={}t", playerLabel(player), remainingCooldownTicks(lastSpecialTick, now, specialGlobalCooldownTicks));
            return;
        }

        double triggerChance = getSpecialEntityTriggerChance(phase, profile, danger);
        double roll = player.serverLevel().random.nextDouble();
        if (roll > triggerChance) {
            debugLog(
                    "SPECIAL no-trigger player={} phase={} profile={} danger={} roll={} chance={}",
                    playerLabel(player),
                    phase.index(),
                    profile,
                    danger,
                    String.format(Locale.ROOT, "%.4f", roll),
                    String.format(Locale.ROOT, "%.4f", triggerChance));
            return;
        }

        List<EventChoice> specialChoices = buildSpecialEntityChoices(player, phase, profile, danger, now, false);

        if (specialChoices.isEmpty()) {
            debugLog("SPECIAL no-choices player={} phase={} profile={} danger={}", playerLabel(player), phase.index(), profile, danger);
            return;
        }

        String triggeredKey = triggerSpecialChoicePool(player, specialChoices, now, phase, profile, danger, specialGlobalCooldownTicks, true);
        if (triggeredKey != null) {
            return;
        }
        String guaranteedFallback = tryGuaranteedSpecialSpawn(player, phase, danger);
        if (guaranteedFallback != null) {
            markSpecialEntityTriggered(player, guaranteedFallback, now, phase, profile, danger, specialGlobalCooldownTicks);
            debugLog("SPECIAL guaranteed-fallback-success key={} player={}", guaranteedFallback, playerLabel(player));
            return;
        }
        debugLog("SPECIAL no-success player={}", playerLabel(player));
    }

    private static List<EventChoice> buildSpecialEntityChoices(
            ServerPlayer player,
            UncannyPhase phase,
            int profile,
            int danger,
            long now,
            boolean ignoreCooldowns) {
        List<EventChoice> specialChoices = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return specialChoices;
        }

        boolean hasSky = level.canSeeSky(player.blockPosition());
        boolean isOverworld = level.dimension() == Level.OVERWORLD;
        boolean nearBase = isNearBase(player, server);
        boolean insideBase = isInsideBase(player, server);

        if (isOverworld && hasSky && isNightOrTwilight(level)) {
            addSpecialEntityChoiceIfReady(specialChoices, player, "watcher", profileScaledWeight("watcher", 18, profile, danger), now, ignoreCooldowns);
        }

        addSpecialEntityChoiceIfReady(specialChoices, player, "pulse", profileScaledWeight("pulse", 4, profile, danger), now, ignoreCooldowns);
        if (phase.index() >= UncannyPhase.PHASE_2.index()) {
            addSpecialEntityChoiceIfReady(specialChoices, player, "follower", profileScaledWeight("follower", 8, profile, danger), now, ignoreCooldowns);
        }

        if (!hasSky
                && nearBase
                && findNearbyBlock(level, player.blockPosition(), 10, blockState -> blockState.getBlock() instanceof DoorBlock) != null) {
            addSpecialEntityChoiceIfReady(specialChoices, player, "knocker", profileScaledWeight("knocker", 11, profile, danger), now, ignoreCooldowns);
        }

        if (phase.index() >= UncannyPhase.PHASE_3.index()) {
            addSpecialEntityChoiceIfReady(specialChoices, player, "hurler", profileScaledWeight("hurler", 12, profile, danger), now, ignoreCooldowns);
            if (findNearestLorePriorityMarker(player, 360) != null && !hasActiveUsher(server)) {
                addSpecialEntityChoiceIfReady(specialChoices, player, "usher", profileScaledWeight("usher", 1, profile, danger), now, ignoreCooldowns);
            }
            if (nearBase && !insideBase && findKeeperContainerNearBase(player, 18) != null) {
                addSpecialEntityChoiceIfReady(specialChoices, player, "keeper", profileScaledWeight("keeper", 4, profile, danger), now, ignoreCooldowns);
            }
            Long awaySince = TENANT_AWAY_SINCE.get(player.getUUID());
            if (nearBase && awaySince != null && (now - awaySince) >= 180L * 20L) {
                addSpecialEntityChoiceIfReady(specialChoices, player, "tenant", profileScaledWeight("tenant", 5, profile, danger), now, ignoreCooldowns);
            }
            if (danger > 0) {
                addSpecialEntityChoiceIfReady(specialChoices, player, "stalker", profileScaledWeight("stalker", 11, profile, danger), now, ignoreCooldowns);
            }
            if (level.getRawBrightness(player.blockPosition(), 0) <= 8) {
                addSpecialEntityChoiceIfReady(specialChoices, player, "shadow", profileScaledWeight("shadow", 12, profile, danger), now, ignoreCooldowns);
            }
        }

        return specialChoices;
    }

    private static boolean hasActiveUsher(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            AABB loadedBounds = new AABB(-30000000.0D, level.getMinBuildHeight(), -30000000.0D, 30000000.0D, level.getMaxBuildHeight(), 30000000.0D);
            if (!level.getEntitiesOfClass(UncannyUsherEntity.class, loadedBounds, entity -> entity != null && entity.isAlive()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String triggerSpecialChoicePool(
            ServerPlayer player,
            List<EventChoice> specialChoices,
            long now,
            UncannyPhase phase,
            int profile,
            int danger,
            long specialGlobalCooldownTicks,
            boolean tryCloseFallback) {
        ServerLevel level = player.serverLevel();
        List<EventChoice> remaining = new ArrayList<>(specialChoices);
        while (!remaining.isEmpty()) {
            int totalWeight = remaining.stream().mapToInt(EventChoice::weight).sum();
            if (totalWeight <= 0) {
                break;
            }

            EventChoice selected = pickWeightedChoice(remaining, level.random.nextInt(totalWeight));
            if (selected == null) {
                debugLog("SPECIAL selection-null player={}", playerLabel(player));
                break;
            }

            debugLog("SPECIAL attempt key={} player={} remainingChoices={}", selected.key(), playerLabel(player), remaining.size());
            if (triggerSpecialEntityByKey(player, selected.key())) {
                markSpecialEntityTriggered(player, selected.key(), now, phase, profile, danger, specialGlobalCooldownTicks);
                debugLog("SPECIAL success key={} player={} cooldown={}t", selected.key(), playerLabel(player), specialGlobalCooldownTicks);
                return selected.key();
            }
            debugLog("SPECIAL fail key={} player={}", selected.key(), playerLabel(player));
            remaining.remove(selected);
        }

        if (tryCloseFallback) {
            String fallbackKey = tryCloseFallbackSpecialSpawn(player, phase, danger);
            if (fallbackKey != null) {
                markSpecialEntityTriggered(player, fallbackKey, now, phase, profile, danger, specialGlobalCooldownTicks);
                debugLog("SPECIAL fallback-success key={} player={}", fallbackKey, playerLabel(player));
                return fallbackKey;
            }
        }

        return null;
    }

    private static String tryCloseFallbackSpecialSpawn(ServerPlayer player, UncannyPhase phase, int danger) {
        if (phase.index() >= UncannyPhase.PHASE_3.index() && danger > 0 && spawnHurler(player, true, false)) {
            return "hurler";
        }
        if (phase.index() >= UncannyPhase.PHASE_3.index() && danger > 0 && spawnStalker(player, true, false)) {
            return "stalker";
        }
        if (phase.index() >= UncannyPhase.PHASE_2.index() && spawnFollower(player, true)) {
            return "follower";
        }
        if (phase.index() >= UncannyPhase.PHASE_2.index() && spawnKnocker(player, true, false)) {
            return "knocker";
        }
        if (phase.index() >= UncannyPhase.PHASE_2.index()
                && (UncannyWatcherSystem.spawnWatcherFromEvents(player) || UncannyWatcherSystem.forceSpawnWatcher(player))) {
            return "watcher";
        }
        return null;
    }

    private static String tryGuaranteedSpecialSpawn(ServerPlayer player, UncannyPhase phase, int danger) {
        if (phase.index() >= UncannyPhase.PHASE_3.index() && danger > 0 && spawnStalker(player, true, true)) {
            return "stalker";
        }
        if (phase.index() >= UncannyPhase.PHASE_3.index() && danger > 0 && spawnHurler(player, true, true)) {
            return "hurler";
        }
        if (phase.index() >= UncannyPhase.PHASE_2.index() && spawnFollower(player, true)) {
            return "follower";
        }
        if (phase.index() >= UncannyPhase.PHASE_2.index() && spawnKnocker(player, true, false)) {
            return "knocker";
        }
        if (phase.index() >= UncannyPhase.PHASE_2.index() && (UncannyWatcherSystem.forceSpawnWatcher(player) || UncannyWatcherSystem.spawnWatcherFromEvents(player))) {
            return "watcher";
        }
        if (phase.index() >= UncannyPhase.PHASE_3.index() && spawnShadow(player, false, true)) {
            return "shadow";
        }
        if (phase.index() >= UncannyPhase.PHASE_3.index() && spawnUsher(player, true)) {
            return "usher";
        }
        return null;
    }

    private static void addSpecialEntityChoiceIfReady(
            List<EventChoice> choices,
            ServerPlayer player,
            String key,
            int weight,
            long now) {
        addSpecialEntityChoiceIfReady(choices, player, key, weight, now, false);
    }

    private static void addSpecialEntityChoiceIfReady(
            List<EventChoice> choices,
            ServerPlayer player,
            String key,
            int weight,
            long now,
            boolean ignoreCooldown) {
        if (weight <= 0 || (!ignoreCooldown && isSpecialEntityOnCooldown(player, key, now))) {
            return;
        }
        choices.add(new EventChoice(key, weight));
    }

    private static boolean isSpecialEntityOnCooldown(ServerPlayer player, String key, long now) {
        Map<String, Long> perPlayer = SPECIAL_ENTITY_COOLDOWNS.get(player.getUUID());
        if (perPlayer == null) {
            return false;
        }
        Long until = perPlayer.get(key);
        return until != null && now < until;
    }

    private static boolean triggerSpecialEntityByKey(ServerPlayer player, String key) {
        return switch (key) {
            case "watcher" -> UncannyWatcherSystem.spawnWatcherFromEvents(player) || UncannyWatcherSystem.forceSpawnWatcher(player);
            case "shadow" -> spawnShadow(player, true) || spawnShadow(player, false, true) || spawnShadow(player, false);
            case "hurler" -> spawnHurler(player, true, false) || spawnHurler(player, false, false);
            case "knocker" -> spawnKnocker(player, true, false) || spawnKnocker(player, false, false);
            case "stalker" -> spawnStalker(player, true, false) || spawnStalker(player, false, false);
            case "pulse" -> spawnPulse(player, false);
            case "usher" -> spawnUsher(player, true) || spawnUsher(player, false);
            case "keeper" -> spawnKeeper(player, true) || spawnKeeper(player, false);
            case "tenant" -> spawnTenant(player, true) || spawnTenant(player, false);
            case "follower" -> spawnFollower(player, true) || spawnFollower(player, false);
            default -> false;
        };
    }

    private static void markSpecialEntityTriggered(
            ServerPlayer player,
            String key,
            long now,
            UncannyPhase phase,
            int profile,
            int danger,
            long globalCooldownTicks) {
        LAST_SPECIAL_ENTITY_EVENT_TICKS.put(player.getUUID(), now);
        long perEntityCooldownTicks = computeSpecialEntityPerKeyCooldownTicks(key, phase, profile, danger);
        SPECIAL_ENTITY_COOLDOWNS
                .computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                .put(key, now + Math.max(globalCooldownTicks / 2L, perEntityCooldownTicks));
    }

    private static int rollSpecialEntityCheckIntervalTicks(UncannyPhase phase, int profile, ServerLevel level) {
        int base = PROFILE_SPECIAL_ENTITY_CHECK_INTERVAL_SECONDS[profile - 1];
        double phaseMultiplier = switch (phase) {
            case PHASE_1 -> 1.35D;
            case PHASE_2 -> 1.15D;
            case PHASE_3 -> 1.00D;
            case PHASE_4 -> 0.85D;
        };
        int minSeconds = Math.max(1, (int) Math.floor(base * phaseMultiplier * 0.65D));
        int maxSeconds = Math.max(minSeconds, (int) Math.ceil(base * phaseMultiplier * 1.35D));
        return (minSeconds + level.random.nextInt(maxSeconds - minSeconds + 1)) * 20;
    }

    private static long computeSpecialEntityGlobalCooldownTicks(
            UncannyPhase phase,
            int profile,
            int danger) {
        int baseSeconds = PROFILE_SPECIAL_ENTITY_BASE_COOLDOWN_SECONDS[profile - 1];
        double phaseMultiplier = switch (phase) {
            case PHASE_1 -> 1.30D;
            case PHASE_2 -> 1.02D;
            case PHASE_3 -> 0.82D;
            case PHASE_4 -> 0.68D;
        };
        double dangerMultiplier = DANGER_SPECIAL_ENTITY_COOLDOWN_MULTIPLIER[danger];
        int seconds = Math.max(35, (int) Math.round(baseSeconds * phaseMultiplier * dangerMultiplier));
        return seconds * 20L;
    }

    private static long computeSpecialEntityPerKeyCooldownTicks(
            String key,
            UncannyPhase phase,
            int profile,
            int danger) {
        long base = computeSpecialEntityGlobalCooldownTicks(phase, profile, danger);
        double keyMultiplier = switch (key) {
            case "watcher" -> 0.65D;
            case "pulse" -> 2.10D;
            case "follower" -> 720.0D / Math.max(1.0D, base / 20.0D);
            case "usher" -> 3600.0D / Math.max(1.0D, base / 20.0D);
            case "tenant" -> 1800.0D / Math.max(1.0D, base / 20.0D);
            case "keeper" -> 2400.0D / Math.max(1.0D, base / 20.0D);
            case "knocker", "hurler", "shadow" -> 0.85D;
            case "stalker" -> 1.10D;
            default -> 1.00D;
        };
        return Math.max(30L * 20L, (long) Math.round(base * keyMultiplier));
    }

    private static double getSpecialEntityTriggerChance(UncannyPhase phase, int profile, int danger) {
        if (phase.index() < UncannyPhase.PHASE_2.index()) {
            return 0.0D;
        }
        double phaseMultiplier = switch (phase) {
            case PHASE_1 -> 0.0D;
            case PHASE_2 -> 1.00D;
            case PHASE_3 -> 1.15D;
            case PHASE_4 -> 1.30D;
        };
        double chance = PROFILE_SPECIAL_ENTITY_TRIGGER_CHANCE[profile - 1]
                * DANGER_SPECIAL_ENTITY_TRIGGER_MULTIPLIER[danger]
                * phaseMultiplier;
        return Mth.clamp(chance, 0.03D, 0.84D);
    }

    private static int rollAutoCheckIntervalTicks(UncannyPhase phase, int profile, ServerLevel level) {
        int profileReduction = profile * 2;
        int phaseReduction = switch (phase) {
            case PHASE_1 -> 0;
            case PHASE_2 -> 2;
            case PHASE_3 -> 4;
            case PHASE_4 -> 6;
        };
        int min = Math.max(6, MIN_AUTO_CHECK_INTERVAL_TICKS - profileReduction - phaseReduction / 2);
        int max = Math.max(min + 4, MAX_AUTO_CHECK_INTERVAL_TICKS - profileReduction - phaseReduction);
        return min + level.random.nextInt(max - min + 1);
    }

    private static long rollEventCooldownTicks(ServerLevel level, UncannyPhase phase, int profile, int danger, EventSeverity severity) {
        int baseMinSeconds;
        int baseMaxSeconds;
        switch (severity) {
            case LIGHT -> {
                baseMinSeconds = 16;
                baseMaxSeconds = 45;
            }
            case MEDIUM -> {
                baseMinSeconds = 35;
                baseMaxSeconds = 95;
            }
            case HIGH -> {
                baseMinSeconds = 70;
                baseMaxSeconds = 180;
            }
            case EXTREME -> {
                baseMinSeconds = 120;
                baseMaxSeconds = 300;
            }
            default -> {
                baseMinSeconds = 60;
                baseMaxSeconds = 150;
            }
        }

        double profileScale = switch (profile) {
            case 1 -> 1.45D;
            case 2 -> 1.00D;
            case 3 -> 0.72D;
            case 4 -> 0.52D;
            default -> 0.38D;
        };
        double dangerScale = DANGER_EVENT_COOLDOWN_MULTIPLIER[danger];
        double phaseScale = switch (phase) {
            case PHASE_1 -> 1.20D;
            case PHASE_2 -> 1.02D;
            case PHASE_3 -> 0.86D;
            case PHASE_4 -> 0.72D;
        };
        double jitter = 0.78D + level.random.nextDouble() * 0.54D;

        int seconds = baseMinSeconds + level.random.nextInt(Math.max(1, baseMaxSeconds - baseMinSeconds + 1));
        int finalSeconds = Math.max(8, (int) Math.round(seconds * profileScale * dangerScale * phaseScale * jitter));
        return finalSeconds * 20L;
    }

    private static boolean passesGlobalAndRespawnChecks(
            UncannyWorldState state,
            ServerPlayer player,
            UncannyPhase phase,
            int profile,
            int danger,
            long now) {
        long globalCooldownTicks = computeEffectiveGlobalCooldownTicks(phase, profile, danger);
        long lastGlobal = state.getLastGlobalEventTick();
        if (isCooldownActive(lastGlobal, now, globalCooldownTicks)) {
            return false;
        }

        Long lastRespawnTick = state.getLastRespawnTick(player.getUUID());
        long graceTicks = UncannyConfig.RESPAWN_GRACE_SECONDS.get() * 20L;
        return !isCooldownActive(lastRespawnTick, now, graceTicks);
    }

    private static boolean isForcedBySilence(UncannyWorldState state, UncannyPhase phase, int profile, int danger, long now) {
        long lastGlobal = state.getLastGlobalEventTick();
        long referenceTick = (lastGlobal == Long.MIN_VALUE || now < lastGlobal) ? 0L : lastGlobal;
        return now - referenceTick >= getMaxSilenceTicks(phase, profile, danger);
    }

    private static EventChoice pickWeightedChoice(List<EventChoice> choices, int roll) {
        int running = roll;
        for (EventChoice choice : choices) {
            running -= choice.weight();
            if (running < 0) {
                return choice;
            }
        }
        return null;
    }

    private static int getIntensityProfile() {
        return Mth.clamp(UncannyConfig.EVENT_INTENSITY_PROFILE.get(), 1, 5);
    }

    private static double getSleepDisturbChance(UncannyPhase phase, int profile) {
        int phaseIndex = Mth.clamp(phase.index(), 1, 4);
        double base = SLEEP_DISTURB_PHASE_CHANCE[phaseIndex - 1];
        double chance = base * SLEEP_DISTURB_PROFILE_MULTIPLIER[profile - 1];
        return Mth.clamp(chance, 0.0D, 0.24D);
    }

    private static long rollSleepDisturbCooldownTicks(ServerLevel level, UncannyPhase phase, int profile) {
        int baseSeconds = SLEEP_DISTURB_COOLDOWN_MIN_SECONDS
                + level.random.nextInt(Math.max(1, SLEEP_DISTURB_COOLDOWN_MAX_SECONDS - SLEEP_DISTURB_COOLDOWN_MIN_SECONDS + 1));

        double phaseScale = switch (phase) {
            case PHASE_1 -> 1.35D;
            case PHASE_2 -> 1.12D;
            case PHASE_3 -> 1.00D;
            case PHASE_4 -> 0.92D;
        };
        double profileScale = switch (profile) {
            case 1 -> 1.35D;
            case 2 -> 1.15D;
            case 3 -> 1.00D;
            case 4 -> 0.90D;
            default -> 0.82D;
        };
        int cooldownSeconds = Math.max(9 * 60, (int) Math.round(baseSeconds * phaseScale * profileScale));
        return cooldownSeconds * 20L;
    }

    private static double getAutoTriggerChance(UncannyPhase phase, int profile, int danger) {
        double base = switch (phase) {
            case PHASE_1 -> 0.010D;
            case PHASE_2 -> 0.016D;
            case PHASE_3 -> 0.022D;
            case PHASE_4 -> 0.030D;
        };
        return Mth.clamp(base * PROFILE_TRIGGER_MULTIPLIER[profile - 1] * DANGER_TRIGGER_MULTIPLIER[danger], 0.0030D, 0.30D);
    }

    private static double getAmbientTriggerChance(UncannyPhase phase, int profile, int danger) {
        double base = switch (phase) {
            case PHASE_1 -> 0.10D;
            case PHASE_2 -> 0.14D;
            case PHASE_3 -> 0.18D;
            case PHASE_4 -> 0.22D;
        };
        return Mth.clamp(base * PROFILE_AMBIENT_TRIGGER_MULTIPLIER[profile - 1] * DANGER_AMBIENT_TRIGGER_MULTIPLIER[danger], 0.06D, 0.82D);
    }

    public static long getEffectiveGlobalCooldownTicks(UncannyPhase phase) {
        return computeEffectiveGlobalCooldownTicks(phase, getIntensityProfile(), getDangerLevel());
    }

    private static long computeEffectiveGlobalCooldownTicks(UncannyPhase phase, int profile, int danger) {
        int configured = Math.max(20, UncannyConfig.EVENT_GLOBAL_COOLDOWN_SECONDS.get());
        int profileSeconds = PROFILE_BASE_COOLDOWN_SECONDS[profile - 1];
        int chosenSeconds = Math.min(configured, profileSeconds);

        double phaseMultiplier = switch (phase) {
            case PHASE_1 -> 1.15D;
            case PHASE_2 -> 0.92D;
            case PHASE_3 -> 0.78D;
            case PHASE_4 -> 0.66D;
        };
        double dangerMultiplier = DANGER_GLOBAL_COOLDOWN_MULTIPLIER[danger];

        int finalSeconds = Math.max(10, (int) Math.round(chosenSeconds * phaseMultiplier * dangerMultiplier));
        return finalSeconds * 20L;
    }

    private static long computeAmbientGlobalCooldownTicks(UncannyPhase phase, int profile, int danger) {
        int baseSeconds = PROFILE_AMBIENT_BASE_COOLDOWN_SECONDS[profile - 1];
        double phaseMultiplier = switch (phase) {
            case PHASE_1 -> 1.12D;
            case PHASE_2 -> 1.00D;
            case PHASE_3 -> 0.85D;
            case PHASE_4 -> 0.72D;
        };
        double dangerMultiplier = DANGER_AMBIENT_COOLDOWN_MULTIPLIER[danger];
        int seconds = Math.max(10, (int) Math.round(baseSeconds * phaseMultiplier * dangerMultiplier));
        return seconds * 20L;
    }

    private static String summarizeCooldownPool(Map<String, Long> pool, long now, int maxEntries) {
        if (pool == null || pool.isEmpty()) {
            return "none";
        }
        List<Map.Entry<String, Long>> active = new ArrayList<>();
        for (Map.Entry<String, Long> entry : pool.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > now) {
                active.add(entry);
            }
        }
        if (active.isEmpty()) {
            return "none";
        }
        active.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        int limit = Math.max(1, Math.min(maxEntries, active.size()));
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = active.get(i);
            if (i > 0) {
                summary.append(",");
            }
            summary.append(entry.getKey()).append(":").append(ticksToSeconds(entry.getValue() - now)).append("s");
        }
        if (active.size() > limit) {
            summary.append(",+").append(active.size() - limit);
        }
        return summary.toString();
    }

    private static long getMaxSilenceTicks(UncannyPhase phase, int profile, int danger) {
        int baseSeconds = PROFILE_MAX_SILENCE_SECONDS[profile - 1];
        double phaseMultiplier = switch (phase) {
            case PHASE_1 -> 1.20D;
            case PHASE_2 -> 1.00D;
            case PHASE_3 -> 0.80D;
            case PHASE_4 -> 0.65D;
        };
        int seconds = Math.max(20, (int) Math.round(baseSeconds * phaseMultiplier * DANGER_MAX_SILENCE_MULTIPLIER[danger]));
        return seconds * 20L;
    }

    private static int profileScaledWeight(String eventKey, int baseWeight, int profile, int danger) {
        double multiplier = switch (eventKey) {
            case "footsteps" -> 0.90D - (profile - 1) * 0.08D;
            case "base_replay" -> 1.90D - (profile - 1) * 0.10D;
            case "blackout" -> 0.22D + (profile - 1) * 0.12D;
            case "bell" -> 0.62D + (profile - 1) * 0.24D;
            case "watcher" -> 0.92D + (profile - 1) * 0.18D;
            case "knocker" -> 0.45D + (profile - 1) * 0.22D;
            case "flash" -> 0.12D + (profile - 1) * 0.28D;
            case "stalker" -> 0.22D + (profile - 1) * 0.34D;
            case "shadow" -> 0.32D + (profile - 1) * 0.34D;
            case "hurler" -> 0.38D + (profile - 1) * 0.33D;
            case "pulse" -> 0.08D + (profile - 1) * 0.14D;
            case "usher" -> 0.10D + (profile - 1) * 0.08D;
            case "keeper" -> 0.30D + (profile - 1) * 0.22D;
            case "tenant" -> 0.28D + (profile - 1) * 0.24D;
            case "follower" -> 0.56D + (profile - 1) * 0.22D;
            case "animal_stare_lock", "misplaced_light", "hotbar_wrong_count" -> 0.42D + (profile - 1) * 0.22D;
            case "compass_liar", "pet_refusal", "corrupt_toast", "false_recipe_toast" -> 0.26D + (profile - 1) * 0.20D;
            case "furnace_breath", "false_container_open", "lever_answer", "pressure_plate_reply", "campfire_cough",
                    "bucket_drip", "tool_answer" -> 0.62D + (profile - 1) * 0.18D;
            case "workbench_reject" -> 0.12D + (profile - 1) * 0.10D;
            case "flash_red", "void_silence", "false_fall", "ghost_miner", "cave_collapse" -> 0.65D + (profile - 1) * 0.22D;
            case "armor_break", "aquatic_steps", "living_ore" -> 0.50D + (profile - 1) * 0.28D;
            case "door_inversion", "phantom_harvest", "projected_shadow", "giant_sun", "hunter_fog" -> 0.24D + (profile - 1) * 0.30D;
            case "asphyxia" -> 0.14D + (profile - 1) * 0.26D;
            case "false_injury", "forced_drop" -> 0.08D + (profile - 1) * 0.40D;
            case "corrupt_message" -> 1.32D - (profile - 1) * 0.12D;
            default -> 1.0D;
        };
        double weighted = baseWeight * multiplier * dangerWeightMultiplier(eventKey, danger);
        if (weighted <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(weighted));
    }

    private static EventSeverity getEventSeverity(String eventKey) {
        return switch (eventKey) {
            case "corrupt_message", "footsteps", "watcher", "base_replay", "false_container_open", "lever_answer",
                    "pressure_plate_reply", "bucket_drip", "tool_answer", "furnace_breath", "campfire_cough" -> EventSeverity.LIGHT;
            case "bell", "false_fall", "flash_red", "ghost_miner", "cave_collapse", "knocker",
                    "armor_break", "aquatic_steps", "living_ore", "follower", "usher", "animal_stare_lock",
                    "misplaced_light", "pet_refusal", "hotbar_wrong_count", "compass_liar", "corrupt_toast", "false_recipe_toast",
                    "bedside_open", "tenant", "keeper" -> EventSeverity.MEDIUM;
            case "flash", "shadow", "hurler", "stalker", "pulse", "void_silence", "false_injury",
                    "door_inversion", "phantom_harvest", "projected_shadow", "giant_sun", "hunter_fog",
                    "workbench_reject" -> EventSeverity.HIGH;
            case "blackout", "forced_drop", "asphyxia" -> EventSeverity.EXTREME;
            default -> EventSeverity.MEDIUM;
        };
    }

    private static boolean isSpecialEntityEventKey(String eventKey) {
        return switch (eventKey) {
            case "watcher", "shadow", "hurler", "knocker", "stalker", "pulse", "usher", "keeper", "tenant", "follower" -> true;
            default -> false;
        };
    }

    private static boolean triggerForcedFallback(ServerPlayer player, UncannyPhase phase) {
        int danger = getDangerLevel();
        if (phase.index() >= UncannyPhase.PHASE_2.index() && UncannyWatcherSystem.spawnWatcherFromEvents(player)) {
            return true;
        }
        if (danger > 0 && phase.index() >= UncannyPhase.PHASE_3.index() && triggerFlashError(player)) {
            return true;
        }
        if (danger > 1 && phase.index() >= UncannyPhase.PHASE_3.index() && triggerBlackoutSafe(player)) {
            return true;
        }
        if (danger > 1 && phase.index() >= UncannyPhase.PHASE_2.index() && triggerBell(player)) {
            return true;
        }
        if (phase.index() >= UncannyPhase.PHASE_3.index() && spawnHurler(player)) {
            return true;
        }
        return triggerFootstepsBehind(player);
    }

    private static boolean triggerBlackoutSafe(ServerPlayer player) {
        // Prevent nested blackout restarts while an active blackout is already running.
        if (ACTIVE_BLACKOUTS.containsKey(player.getUUID())) {
            return false;
        }
        return triggerTotalBlackout(player);
    }

    private static boolean isCooldownActive(long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != Long.MIN_VALUE && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static boolean isCooldownActive(Long lastTick, long now, long cooldownTicks) {
        return cooldownTicks > 0 && lastTick != null && now >= lastTick && (now - lastTick) < cooldownTicks;
    }

    private static long remainingCooldownTicks(long lastTick, long now, long cooldownTicks) {
        if (cooldownTicks <= 0 || lastTick == Long.MIN_VALUE || now < lastTick) {
            return 0L;
        }
        return Math.max(0L, cooldownTicks - (now - lastTick));
    }

    private static long remainingCooldownTicks(Long lastTick, long now, long cooldownTicks) {
        if (cooldownTicks <= 0 || lastTick == null || now < lastTick) {
            return 0L;
        }
        return Math.max(0L, cooldownTicks - (now - lastTick));
    }

    private static long ticksToSeconds(long ticks) {
        return ticks / 20L;
    }

    private static long safeFutureRemainingTicks(long targetTick, long now, long maxReasonableTicks) {
        if (targetTick == Long.MIN_VALUE || targetTick <= now) {
            return 0L;
        }
        long delta = targetTick - now;
        if (delta < 0L) {
            return 0L;
        }
        if (maxReasonableTicks > 0L && delta > maxReasonableTicks) {
            return maxReasonableTicks;
        }
        return delta;
    }

    private static int getDangerLevel() {
        return Mth.clamp(UncannyConfig.EVENT_DANGER_LEVEL.get(), 0, 5);
    }

    private static double dangerWeightMultiplier(String eventKey, int danger) {
        if (danger <= 0) {
            return switch (eventKey) {
                case "watcher", "hurler", "knocker", "footsteps", "corrupt_message", "base_replay", "ghost_miner", "cave_collapse",
                        "flash_red", "void_silence", "false_fall", "armor_break", "aquatic_steps", "door_inversion", "living_ore" ->
                        DANGER_LIGHT_EVENT_MULTIPLIER[0];
                case "blackout", "bell", "flash", "stalker", "shadow", "pulse", "false_injury", "forced_drop",
                        "asphyxia", "phantom_harvest", "projected_shadow", "giant_sun", "hunter_fog" -> 0.0D;
                default -> DANGER_MEDIUM_EVENT_MULTIPLIER[0];
            };
        }

        return switch (eventKey) {
            case "stalker", "shadow", "flash", "blackout", "bell", "pulse", "false_injury", "forced_drop",
                    "asphyxia", "phantom_harvest", "projected_shadow", "giant_sun", "hunter_fog" ->
                    DANGER_HIGH_EVENT_MULTIPLIER[danger];
            case "hurler", "knocker", "void_silence", "false_fall", "door_inversion", "living_ore" ->
                    DANGER_MEDIUM_EVENT_MULTIPLIER[danger];
            default -> DANGER_LIGHT_EVENT_MULTIPLIER[danger];
        };
    }

    private static void tickDeafness(ServerPlayer player, long now) {
        Long endTick = ACTIVE_DEAFNESS.get(player.getUUID());
        if (endTick == null) {
            return;
        }
        if (now >= endTick) {
            ACTIVE_DEAFNESS.remove(player.getUUID());
            return;
        }
        stopAllSoundsForPlayer(player);
    }

    private static void tickVoidSilence(ServerPlayer player, long now) {
        VoidSilenceState state = ACTIVE_VOID_SILENCE.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (now >= state.endTick()) {
            ACTIVE_VOID_SILENCE.remove(player.getUUID());
            return;
        }

        if (now >= state.nextRingTick()) {
            playCustomEventSound(player, player.serverLevel(), player.blockPosition(), UncannySoundRegistry.UNCANNY_TINNITUS.get(), 0.08F, 0.95F, 1.05F);
            state.setNextRingTick(now + 70L + player.getRandom().nextInt(90));
        }
    }

    private static void tickGhostMiner(ServerPlayer player, long now) {
        GhostMinerState state = ACTIVE_GHOST_MINERS.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (now >= state.endTick()) {
            ACTIVE_GHOST_MINERS.remove(player.getUUID());
            return;
        }

        ServerLevel level = player.serverLevel();
        if (state.pendingPickupTick() >= 0L && now >= state.pendingPickupTick()) {
            BlockPos pickupPos = state.pendingPickupPos();
            float pickupPitch = ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F;
            playLocalSoundAt(player, pickupPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, pickupPitch);
            state.clearPendingPickup();
        }

        if (now < state.nextHitTick()) {
            return;
        }

        BlockPos strikePos = advanceGhostMinerStrikePos(level, player, state);
        if (strikePos == null) {
            strikePos = state.wallPos();
            if (strikePos == null || level.getBlockState(strikePos).isAir()) {
                ACTIVE_GHOST_MINERS.remove(player.getUUID());
                return;
            }
        }

        BlockState strikeState = level.getBlockState(strikePos);
        if (strikeState.isAir()) {
            state.setNextHitTick(now + 5L);
            return;
        }

        state.setWallPos(strikePos);
        SoundType soundType = strikeState.getSoundType();
        playLocalSoundAt(
                player,
                strikePos,
                soundType.getBreakSound(),
                SoundSource.BLOCKS,
                Math.min(1.15F, 0.65F + soundType.getVolume() * 0.35F),
                0.84F + player.getRandom().nextFloat() * 0.26F);
        state.setPendingPickup(strikePos, now + 5L);
        state.setNextHitTick(now + 6L + player.getRandom().nextInt(6));
    }

    private static void tickAsphyxia(ServerPlayer player, long now) {
        AsphyxiaState state = ACTIVE_ASPHYXIA.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (!state.damageApplied() && state.variant() == AsphyxiaVariant.TERRAIN_DROWNING && now >= state.endTick()) {
            player.hurt(player.damageSources().drown(), 2.0F + player.getRandom().nextInt(3));
            player.knockback(0.22D, player.getLookAngle().x, player.getLookAngle().z);
            playLocalSound(player, SoundEvents.DROWNED_HURT_WATER, SoundSource.HOSTILE, 1.0F, 0.72F);
            state.setDamageApplied(true);
        }

        if (now >= state.endTick()) {
            ACTIVE_ASPHYXIA.remove(player.getUUID());
            player.setAirSupply(player.getMaxAirSupply());
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
            return;
        }

        long total = Math.max(1L, state.endTick() - state.startTick());
        long elapsed = Math.max(0L, now - state.startTick());
        float progress = Math.min(1.0F, elapsed / (float) total);
        if (state.variant() == AsphyxiaVariant.HEAVY_LUNGS) {
            progress = Math.min(1.0F, progress * 1.35F);
        }
        int targetAir = Math.max(0, player.getMaxAirSupply() - (int) (player.getMaxAirSupply() * progress));
        player.setAirSupply(targetAir);

        if (state.variant() == AsphyxiaVariant.HEAVY_LUNGS) {
            int amp = progress > 0.6F ? 2 : 1;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, amp, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, amp, false, false, true));
            if ((now % 24L) == 0L) {
                playLocalSound(player, SoundEvents.DROWNED_AMBIENT_WATER, SoundSource.HOSTILE, 0.55F, 0.58F);
            }
        }

        if (!state.damageApplied() && state.variant() == AsphyxiaVariant.TERRAIN_DROWNING && progress >= 0.94F) {
            player.hurt(player.damageSources().drown(), 2.0F + player.getRandom().nextInt(3));
            player.knockback(0.22D, player.getLookAngle().x, player.getLookAngle().z);
            playLocalSound(player, SoundEvents.DROWNED_HURT_WATER, SoundSource.HOSTILE, 1.0F, 0.72F);
            state.setDamageApplied(true);
        }
    }

    private static void tickHunterFog(ServerPlayer player, long now) {
        HunterFogState state = ACTIVE_HUNTER_FOG.get(player.getUUID());
        if (state == null) {
            if (player.getTags().contains(HUNTER_FOG_TAG)) {
                player.removeTag(HUNTER_FOG_TAG);
            }
            return;
        }

        player.addTag(HUNTER_FOG_TAG);

        if (now >= state.endTick()) {
            ACTIVE_HUNTER_FOG.remove(player.getUUID());
            player.removeEffect(MobEffects.WITHER);
            player.removeTag(HUNTER_FOG_TAG);
            return;
        }

        boolean sprinting = player.isSprinting() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01D;
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.003D;

        if (sprinting && (now % 20L) == 0L) {
            player.hurt(player.damageSources().cactus(), 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0, false, false, true));
            if ((now % 40L) == 0L) {
                playLocalSound(player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.AMBIENT, 0.14F, 0.88F);
            }
            return;
        }

        player.removeEffect(MobEffects.WITHER);
        if (moving && (now % 50L) == 0L) {
            playLocalSound(player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.AMBIENT, 0.08F, 0.92F);
        }
    }

    private static void tickGiantSun(ServerPlayer player, long now) {
        GiantSunState state = ACTIVE_GIANT_SUN.get(player.getUUID());
        if (state == null) {
            if (player.getTags().contains(GIANT_SUN_TAG)) {
                player.removeTag(GIANT_SUN_TAG);
            }
            return;
        }

        player.addTag(GIANT_SUN_TAG);

        if (now >= state.endTick()) {
            ACTIVE_GIANT_SUN.remove(player.getUUID());
            player.removeTag(GIANT_SUN_TAG);
            return;
        }

        int danger = getDangerLevel();
        if (player.getXRot() <= -33.0F) {
            state.setLookTicks(state.lookTicks() + 1);
        } else {
            state.setLookTicks(Math.max(0, state.lookTicks() - 2));
        }

        if (!state.burnApplied() && danger > 0 && state.lookTicks() >= 40) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 60));
            state.setBurnApplied(true);
            playLocalSound(player, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 0.6F);
        }

        if (danger >= 2 && state.levitationBursts() < 3 && now >= state.nextPulseTick()) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 0, false, false, true));
            state.setLevitationBursts(state.levitationBursts() + 1);
            state.setNextPulseTick(now + 100L + player.getRandom().nextInt(81));
        }
    }

    private static void tickCompassLiar(ServerPlayer player, long now) {
        CompassLiarState state = ACTIVE_COMPASS_LIARS.get(player.getUUID());
        if (state == null) {
            clearUncannyCompassTarget(player.getMainHandItem());
            clearUncannyCompassTarget(player.getOffhandItem());
            return;
        }
        if (state.target() != null) {
            BlockPos target = state.target();
            applyUncannyCompassTarget(player.getMainHandItem(), player.serverLevel(), target);
            applyUncannyCompassTarget(player.getOffhandItem(), player.serverLevel(), target);

            if (player.blockPosition().distSqr(target) <= 25.0D) {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();
                if (isConsumableGuideCompass(main)) {
                    main.shrink(1);
                } else if (isConsumableGuideCompass(off)) {
                    off.shrink(1);
                }
                ACTIVE_COMPASS_LIARS.remove(player.getUUID());
                clearUncannyCompassTarget(main);
                clearUncannyCompassTarget(off);
                return;
            }
        }

        if (now >= state.endTick()) {
            ACTIVE_COMPASS_LIARS.remove(player.getUUID());
            clearUncannyCompassTarget(player.getMainHandItem());
            clearUncannyCompassTarget(player.getOffhandItem());
        }
    }

    private static void tickAnimalStareLock(ServerPlayer player, long now) {
        AnimalStareLockState state = ACTIVE_ANIMAL_STARE_LOCKS.get(player.getUUID());
        if (state == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (now >= state.endTick()) {
            for (MobSnapshot snapshot : state.affected()) {
                Entity entity = level.getEntity(snapshot.entityId());
                if (entity instanceof Mob mob && mob.isAlive()) {
                    mob.setNoAi(snapshot.hadNoAi());
                    mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
            }
            ACTIVE_ANIMAL_STARE_LOCKS.remove(player.getUUID());
            return;
        }

        for (MobSnapshot snapshot : state.affected()) {
            Entity entity = level.getEntity(snapshot.entityId());
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (mob.getNavigation() != null) {
                mob.getNavigation().stop();
            }
            mob.setDeltaMovement(Vec3.ZERO);
            mob.setNoAi(snapshot.hadNoAi());
            if (!snapshot.hadNoAi()) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 14, false, false, false));
            }
            mob.lookAt(player, 80.0F, 80.0F);
            mob.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ(), 80.0F, 80.0F);
        }
    }

    private static void tickFurnaceBreath(ServerPlayer player, long now) {
        FurnaceBreathState state = ACTIVE_FURNACE_BREATHS.get(player.getUUID());
        if (state == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (now < state.nextTick()) {
            return;
        }
        if (isBlockInPlayerView(level, player, state.source())) {
            ACTIVE_FURNACE_BREATHS.remove(player.getUUID());
            return;
        }

        playLocalSoundAt(
                player,
                state.source(),
                UncannySoundRegistry.UNCANNY_PSSS.get(),
                SoundSource.HOSTILE,
                0.55F,
                0.92F + level.random.nextFloat() * 0.15F);
        state.decrementRepetitions();
        if (state.remaining() <= 0) {
            ACTIVE_FURNACE_BREATHS.remove(player.getUUID());
        } else {
            state.setNextTick(now + 8L + level.random.nextInt(14));
        }
    }

    private static void tickMisplacedLight(ServerPlayer player, long now) {
        MisplacedLightState state = ACTIVE_MISPLACED_LIGHTS.get(player.getUUID());
        if (state == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        boolean visibleNow = isBlockInLoosePlayerView(player, state.tempPos());
        if (!state.seenByPlayer() && visibleNow) {
            state.markSeenByPlayer();
        }
        if (visibleNow) {
            state.resetOutOfViewTicks();
        } else if (state.seenByPlayer()) {
            state.incrementOutOfViewTicks();
        }
        if (now >= state.autoRevertTick() || (state.seenByPlayer() && state.outOfViewTicks() >= 20)) {
            revertMisplacedLight(level, state);
            ACTIVE_MISPLACED_LIGHTS.remove(player.getUUID());
        }
    }

    private static boolean isBlockInLoosePlayerView(ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 to = center.subtract(eye);
        if (to.lengthSqr() < 0.0001D) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        double dot = look.dot(to.normalize());
        return dot >= 0.30D;
    }

    private static void revertMisplacedLight(ServerLevel level, MisplacedLightState state) {
        BlockState tempCurrent = level.getBlockState(state.tempPos());
        if (tempCurrent.getBlock() == state.tempState().getBlock()) {
            level.setBlock(state.tempPos(), Blocks.AIR.defaultBlockState(), 3);
        }
        if (level.getBlockState(state.originalPos()).isAir()) {
            level.setBlock(state.originalPos(), state.originalState(), 3);
        }
    }

    private static void tickPetRefusal(ServerPlayer player, long now) {
        PetRefusalState state = ACTIVE_PET_REFUSALS.get(player.getUUID());
        if (state == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Entity raw = level.getEntity(state.petUuid());
        if (!(raw instanceof Mob pet) || !pet.isAlive()) {
            debugLog("EVENT pet_refusal stop player={} reason=pet-missing-or-dead pet={}", playerLabel(player), state.petUuid());
            PacketDistributor.sendToPlayer(player, new UncannyPetRefusalVisualPayload(-1, false, 0));
            ACTIVE_PET_REFUSALS.remove(player.getUUID());
            return;
        }

        if (now >= state.endTick()) {
            pet.setNoAi(state.hadNoAi());
            pet.removeTag("eotv_pet_refusal");
            pet.removeTag("eotv_pet_refusal_black");
            removePetRefusalBlackTeam(level, pet);
            sendPetRefusalVisual(player, pet, false, 0);
            debugLog(
                    "EVENT pet_refusal end player={} pet={} type={} team={} tags={} restoreNoAi={}",
                    playerLabel(player),
                    pet.getStringUUID(),
                    pet.getType().toShortString(),
                    pet.getTeam() != null ? pet.getTeam().getName() : "none",
                    pet.getTags(),
                    state.hadNoAi());
            ACTIVE_PET_REFUSALS.remove(player.getUUID());
            return;
        }

        pet.setNoAi(true);
        pet.setDeltaMovement(Vec3.ZERO);
        pet.addTag("eotv_pet_refusal");
        pet.addTag("eotv_pet_refusal_black");
        assignPetRefusalBlackTeam(level, pet);

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 oddPoint = player.position().add(look.scale(12.0D)).add(2.0D, 0.6D, -2.0D);
        pet.getLookControl().setLookAt(oddPoint.x, oddPoint.y, oddPoint.z, 60.0F, 60.0F);
        if ((now % 20L) == 0L) {
            int remaining = (int) Math.max(1L, state.endTick() - now);
            sendPetRefusalVisual(player, pet, true, remaining);
            debugLog(
                    "EVENT pet_refusal tick player={} pet={} type={} team={} tags={} noAi={} dist={}",
                    playerLabel(player),
                    pet.getStringUUID(),
                    pet.getType().toShortString(),
                    pet.getTeam() != null ? pet.getTeam().getName() : "none",
                    pet.getTags(),
                    pet.isNoAi(),
                    String.format(Locale.ROOT, "%.2f", pet.distanceTo(player)));
        }
    }

    private static void assignPetRefusalBlackTeam(ServerLevel level, Mob pet) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_PET_REFUSAL_BLACK);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_PET_REFUSAL_BLACK);
            debugLog("EVENT pet_refusal team-create name={}", TEAM_PET_REFUSAL_BLACK);
        }
        String key = pet.getStringUUID();
        if (scoreboard.getPlayersTeam(key) != team) {
            scoreboard.addPlayerToTeam(key, team);
            debugLog("EVENT pet_refusal team-assign pet={} team={}", key, team.getName());
        }
    }

    private static void removePetRefusalBlackTeam(ServerLevel level, Mob pet) {
        Scoreboard scoreboard = level.getScoreboard();
        String key = pet.getStringUUID();
        PlayerTeam team = scoreboard.getPlayersTeam(key);
        if (team != null && TEAM_PET_REFUSAL_BLACK.equals(team.getName())) {
            scoreboard.removePlayerFromTeam(key, team);
            debugLog("EVENT pet_refusal team-remove pet={} team={}", key, team.getName());
        }
    }

    private static void sendPetRefusalVisual(ServerPlayer player, Mob pet, boolean active, int durationTicks) {
        if (pet == null || !pet.isAlive()) {
            PacketDistributor.sendToPlayer(player, new UncannyPetRefusalVisualPayload(-1, false, 0));
            return;
        }
        PacketDistributor.sendToPlayer(player, new UncannyPetRefusalVisualPayload(pet.getId(), active, Math.max(0, durationTicks)));
    }

    private static void tickHotbarWrongCount(ServerPlayer player, long now) {
        HotbarWrongCountState state = ACTIVE_HOTBAR_WRONG_COUNTS.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (now >= state.endTick()) {
            ACTIVE_HOTBAR_WRONG_COUNTS.remove(player.getUUID());
        }
    }

    private static void tickPressurePlateReply(ServerPlayer player, long now) {
        ServerLevel level = player.serverLevel();

        Iterator<LeverReplyTask> leverIterator = LEVER_REPLY_TASKS.iterator();
        while (leverIterator.hasNext()) {
            LeverReplyTask task = leverIterator.next();
            if (!task.playerId().equals(player.getUUID()) || task.fireTick() > now || task.dimension() != level.dimension()) {
                continue;
            }
            leverIterator.remove();
            BlockState state = level.getBlockState(task.pos());
            if (!task.audioOnly() && state.getBlock() instanceof LeverBlock && state.hasProperty(BlockStateProperties.POWERED)) {
                boolean next = !state.getValue(BlockStateProperties.POWERED);
                level.setBlock(task.pos(), state.setValue(BlockStateProperties.POWERED, next), 3);
                playLocalSoundAt(player, task.pos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.05F, next ? 0.62F : 0.56F);
                debugLog("EVENT lever_answer execute player={} target={} toggled={} audioOnly=false", playerLabel(player), task.pos(), next);
            } else {
                playLocalSoundAt(player, task.pos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.6F);
                debugLog("EVENT lever_answer execute player={} target={} toggled=none audioOnly={}", playerLabel(player), task.pos(), task.audioOnly());
            }
        }

        Iterator<DoorCascadeTask> cascadeIterator = DOOR_CASCADE_TASKS.iterator();
        while (cascadeIterator.hasNext()) {
            DoorCascadeTask task = cascadeIterator.next();
            if (!task.playerId().equals(player.getUUID()) || task.dimension() != level.dimension()) {
                continue;
            }
            if (now < task.nextTick()) {
                continue;
            }
            if (task.index() >= task.positions().size()) {
                cascadeIterator.remove();
                continue;
            }
            BlockPos pos = task.positions().get(task.index());
            setDoorOpen(player, level, pos, true);
            task.advance(now + 10L + level.random.nextInt(8));
            if (task.index() >= task.positions().size()) {
                cascadeIterator.remove();
            }
        }

        Iterator<PressurePlateReplyTask> pressureIterator = PRESSURE_PLATE_REPLY_TASKS.iterator();
        while (pressureIterator.hasNext()) {
            PressurePlateReplyTask task = pressureIterator.next();
            if (!task.playerId().equals(player.getUUID()) || task.dimension() != level.dimension()) {
                continue;
            }
            if (now < task.fireTick()) {
                continue;
            }

            BlockState plate = level.getBlockState(task.pos());
            if (!(plate.getBlock() instanceof PressurePlateBlock) || !plate.hasProperty(BlockStateProperties.POWERED)) {
                playLocalSoundAt(player, task.pos(), SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.9F, 1.0F);
                pressureIterator.remove();
                continue;
            }

            if (!task.resetPending()) {
                level.setBlock(task.pos(), plate.setValue(BlockStateProperties.POWERED, true), 3);
                playLocalSoundAt(player, task.pos(), SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.9F, 1.0F);
                task.scheduleReset(now + 4L);
            } else {
                BlockState current = level.getBlockState(task.pos());
                if (current.hasProperty(BlockStateProperties.POWERED)) {
                    level.setBlock(task.pos(), current.setValue(BlockStateProperties.POWERED, false), 3);
                }
                playLocalSoundAt(player, task.pos(), SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 0.8F, 0.95F);
                pressureIterator.remove();
            }
        }

        Iterator<ToolAnswerEchoTask> toolAnswerIterator = TOOL_ANSWER_ECHO_TASKS.iterator();
        while (toolAnswerIterator.hasNext()) {
            ToolAnswerEchoTask task = toolAnswerIterator.next();
            if (!task.playerId().equals(player.getUUID()) || task.dimension() != level.dimension()) {
                continue;
            }
            if (task.fireTick() > now) {
                continue;
            }
            toolAnswerIterator.remove();
            playToolAnswerEcho(player, task.minedPos(), task.minedState(), task.toolStack());
        }

        BlockPos currentPlatePos = player.blockPosition().below();
        BlockState currentPlateState = level.getBlockState(currentPlatePos);
        if (!(currentPlateState.getBlock() instanceof PressurePlateBlock)) {
            return;
        }
        Long lastTick = LAST_TRIGGERED_PRESSURE_PLATE_TICKS.get(player.getUUID());
        if (lastTick != null && now - lastTick < 12L) {
            return;
        }
        BlockPos lastPos = LAST_TRIGGERED_PRESSURE_PLATE_POS.get(player.getUUID());
        if (lastPos != null && lastPos.equals(currentPlatePos) && lastTick != null && now - lastTick < 80L) {
            return;
        }
        LAST_TRIGGERED_PRESSURE_PLATE_TICKS.put(player.getUUID(), now);
        LAST_TRIGGERED_PRESSURE_PLATE_POS.put(player.getUUID(), currentPlatePos.immutable());

        if (player.getRandom().nextDouble() <= 0.24D) {
            triggerPressurePlateReplyNow(player, currentPlatePos, now, false);
        }
    }

    private static void tickAquaticBite(ServerPlayer player, long now) {
        AquaticBiteState state = ACTIVE_AQUATIC_BITE.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (now >= state.nextCueTick()) {
            float progress = (float) Mth.clamp((now - state.startTick()) / (double) Math.max(1L, state.deadlineTick() - state.startTick()), 0.0D, 1.0D);
            Vec3 source = Vec3.atCenterOf(state.source());
            Vec3 towardPlayer = player.getEyePosition().subtract(source);
            if (towardPlayer.lengthSqr() > 0.0001D) {
                Vec3 cuePos = source.add(towardPlayer.normalize().scale(1.5D + progress * 3.0D));
                playLocalSoundAt(
                        player,
                        cuePos.x,
                        cuePos.y,
                        cuePos.z,
                        SoundEvents.DROWNED_SWIM,
                        SoundSource.HOSTILE,
                        0.88F + progress * 0.4F,
                        0.72F + progress * 0.18F);
            }
            state.setNextCueTick(now + 8L);
        }

        Vec3 toSource = Vec3.atCenterOf(state.source()).subtract(player.getEyePosition());
        if (toSource.lengthSqr() > 0.0001D) {
            Vec3 look = player.getViewVector(1.0F).normalize();
            if (look.dot(toSource.normalize()) > 0.88D) {
                ACTIVE_AQUATIC_BITE.remove(player.getUUID());
                playLocalSound(player, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.85F, 1.22F);
                return;
            }
        }

        if (now < state.deadlineTick()) {
            return;
        }

        player.hurt(player.damageSources().mobAttack(player), 2.0F);
        Vec3 source = Vec3.atCenterOf(state.source());
        player.knockback(0.24D, player.getX() - source.x, player.getZ() - source.z);
        playLocalSound(player, SoundEvents.DROWNED_HURT_WATER, SoundSource.HOSTILE, 1.15F, 0.82F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, true));
        ACTIVE_AQUATIC_BITE.remove(player.getUUID());
    }

    private static void tickLivingOre(ServerPlayer player, long now) {
        LivingOreState state = LIVING_ORE_PRIMED.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (now >= state.endTick()) {
            LIVING_ORE_PRIMED.remove(player.getUUID());
        }
    }

    private static void triggerLivingOreOnBreak(ServerPlayer player, BlockPos pos, LivingOreVariant variant, long now) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                20,
                0.25D,
                0.25D,
                0.25D,
                0.02D);
        playLocalSoundAt(player, pos, SoundEvents.SLIME_HURT, SoundSource.BLOCKS, 1.0F, 0.55F);

        if (variant == LivingOreVariant.TOXIC_BLOOD) {
            AreaEffectCloud cloud = EntityType.AREA_EFFECT_CLOUD.create(level);
            if (cloud != null) {
                cloud.setPos(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D);
                cloud.setRadius(1.2F);
                cloud.setDuration(60);
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, true, true));
                level.addFreshEntity(cloud);
            }
        } else if (variant == LivingOreVariant.VICIOUS_FALL) {
            BlockPos aboveHead = player.blockPosition().above(2);
            BlockState stateAbove = level.getBlockState(aboveHead);
            if (!stateAbove.isAir() && stateAbove.getDestroySpeed(level, aboveHead) >= 0.0F) {
                level.setBlock(aboveHead, Blocks.GRAVEL.defaultBlockState(), 3);
            }
        } else if (variant == LivingOreVariant.INSIDE_KNOCK) {
            playLocalSoundAt(
                    player,
                    pos,
                    UncannySoundRegistry.ORE_INSIDE_KNOCK.get(),
                    SoundSource.BLOCKS,
                    0.95F,
                    0.88F + level.random.nextFloat() * 0.18F);
        } else if (variant == LivingOreVariant.VEIN_RETREAT) {
            BlockState broken = level.getBlockState(pos);
            if (isRetreatEligibleOre(broken)) {
                Direction away = Direction.getNearest(
                        Mth.clamp((float) (pos.getX() - player.getX()), -1.0F, 1.0F),
                        0.0F,
                        Mth.clamp((float) (pos.getZ() - player.getZ()), -1.0F, 1.0F));
                if (away.getAxis().isHorizontal()) {
                    BlockPos retreat = pos.relative(away);
                    if (level.getBlockState(retreat).isAir() && broken.canSurvive(level, retreat)) {
                        level.setBlock(retreat, broken, 3);
                        playLocalSoundAt(player, retreat, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 0.72F);
                    }
                }
            }
        }
    }

    private static boolean isRetreatEligibleOre(BlockState state) {
        return state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE);
    }

    private static boolean isLivingOreTriggerBlock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.COAL_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.REDSTONE_ORE)
                || state.is(Blocks.LAPIS_ORE)
                || state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE);
    }

    private static void tickFlashRedOverlay(ServerPlayer player, long now) {
        Long endTick = FLASH_RED_OVERLAY_END_TICKS.get(player.getUUID());
        if (endTick == null) {
            if (player.getTags().contains(FLASH_RED_OVERLAY_TAG)) {
                player.removeTag(FLASH_RED_OVERLAY_TAG);
            }
            return;
        }
        if (now < endTick) {
            return;
        }

        FLASH_RED_OVERLAY_END_TICKS.remove(player.getUUID());
        player.removeTag(FLASH_RED_OVERLAY_TAG);
    }

    private static void applyGuaranteedDamageFlash(ServerPlayer player) {
        float previousHealth = player.getHealth();
        int previousInvulnerableTime = player.invulnerableTime;

        boolean hurt = player.hurt(player.damageSources().magic(), 0.01F);
        if (!hurt || !player.isAlive()) {
            player.invulnerableTime = previousInvulnerableTime;
            return;
        }

        if (player.getHealth() < previousHealth) {
            player.setHealth(previousHealth);
        }
        player.invulnerableTime = previousInvulnerableTime;
    }

    private static void queueSleepDisturbMessage(ServerPlayer player, long now) {
        PENDING_SLEEP_MESSAGE_TICKS.put(player.getUUID(), now + 1L);
    }

    private static void tickPendingSleepMessage(ServerPlayer player, long now) {
        Long displayTick = PENDING_SLEEP_MESSAGE_TICKS.get(player.getUUID());
        if (displayTick == null || now < displayTick) {
            return;
        }

        player.displayClientMessage(SLEEP_DISTURB_MESSAGE, true);
        PENDING_SLEEP_MESSAGE_TICKS.remove(player.getUUID());
    }

    private static void spawnPulseInBed(ServerPlayer player, BlockPos bedPos) {
        ServerLevel level = player.serverLevel();
        UncannyPulseEntity pulse = UncannyEntityRegistry.UNCANNY_PULSE.get().create(level);
        if (pulse == null) {
            return;
        }

        BlockPos spawnPos = findPulseSpawnPosNearBed(level, bedPos);
        if (spawnPos == null) {
            debugLog("BED_DISTURB spawn failed player={} bedPos={}", playerLabel(player), bedPos);
            return;
        }

        pulse.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        pulse.setTarget(player);
        level.addFreshEntity(pulse);
        debugLog("BED_DISTURB spawn success player={} bedPos={} spawnPos={}", playerLabel(player), bedPos, spawnPos);
    }

    private static BlockPos findPulseSpawnPosNearBed(ServerLevel level, BlockPos bedPos) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(bedPos.above());

        BlockState bedState = level.getBlockState(bedPos);
        if (bedState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && bedState.hasProperty(BlockStateProperties.BED_PART)) {
            Direction facing = bedState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BedPart part = bedState.getValue(BlockStateProperties.BED_PART);
            BlockPos otherPart = part == BedPart.FOOT ? bedPos.relative(facing) : bedPos.relative(facing.getOpposite());
            candidates.add(otherPart.above());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                candidates.add(otherPart.relative(direction).above());
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            candidates.add(bedPos.relative(direction).above());
        }

        for (BlockPos candidate : candidates) {
            if (hasTwoBlockAirSpace(level, candidate)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static boolean hasTwoBlockAirSpace(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        return feet.isAir()
                && head.isAir()
                && feet.getFluidState().isEmpty()
                && head.getFluidState().isEmpty();
    }

    private static void tickBlackout(ServerPlayer player, long now) {
        BlackoutState state = ACTIVE_BLACKOUTS.get(player.getUUID());
        if (state == null) {
            return;
        }

        long elapsed = now - state.startTick();
        if (elapsed >= state.durationTicks()) {
            ACTIVE_BLACKOUTS.remove(player.getUUID());
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.RECORDS));
            return;
        }

        int slownessAmp = Math.min(4, (int) ((elapsed * 5L) / Math.max(1L, state.durationTicks())));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slownessAmp, false, false, true));

        if (!state.specialChecked() && elapsed >= 20L) {
            state.setSpecialChecked(true);
            int danger = getDangerLevel();
            int profile = getIntensityProfile();
            double specialChance = PROFILE_BLACKOUT_SPECIAL_CHANCE[profile - 1];
            if (danger == 0) {
                specialChance *= 0.45D;
            } else if (danger == 1) {
                specialChance *= 0.70D;
            } else if (danger == 2) {
                specialChance *= 0.88D;
            } else if (danger == 4) {
                specialChance *= 1.18D;
            } else if (danger == 5) {
                specialChance *= 1.42D;
            }
            specialChance = Mth.clamp(specialChance, 0.0D, 0.95D);

            if (player.serverLevel().random.nextDouble() < specialChance) {
                int roll = player.serverLevel().random.nextInt(100);
                boolean spawned;
                if (danger > 0 && roll < 10) {
                    spawned = spawnStalkerEntity(player, 14, 28, true, false) != null;
                } else if (danger > 0 && roll < 45) {
                    spawned = spawnShadow(player);
                } else {
                    spawned = spawnHurler(player);
                }

                // Ensure the blackout attack chance is effectively 1/2 even if a chosen spawn fails.
                if (!spawned) {
                    if (danger <= 0) {
                        spawnHurler(player);
                    } else {
                        boolean fallbackSpawned = spawnHurler(player) || spawnShadow(player) || spawnStalker(player);
                        if (!fallbackSpawned) {
                            debugLog("BLACKOUT special fallback failed player={}", playerLabel(player));
                        }
                    }
                }
            }
        }

        if (elapsed % 70L == 0L) {
            playLocalSound(player, SoundEvents.MUSIC_DISC_11.value(), SoundSource.RECORDS, 1.1F, 1.0F);
        }
    }

    private static void tickFootsteps(ServerPlayer player, long now) {
        FootstepsState state = ACTIVE_FOOTSTEPS.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (now >= state.endTick()) {
            ACTIVE_FOOTSTEPS.remove(player.getUUID());
            return;
        }

        if (now < state.nextStepTick()) {
            return;
        }

        switch (state.pattern()) {
            case ECHO -> {
                playEchoFootstep(player, state.anchorPos());
                ACTIVE_FOOTSTEPS.remove(player.getUUID());
            }
            case SPRINT -> {
                boolean done = playSprintMacabreFootstep(player, state);
                if (done) {
                    ACTIVE_FOOTSTEPS.remove(player.getUUID());
                } else {
                    state.setNextStepTick(now + 4L + player.serverLevel().random.nextInt(3));
                }
            }
            case HEAVY -> {
                playHeavyFootstep(player);
                state.setNextStepTick(now + 12L + player.serverLevel().random.nextInt(8));
            }
            case LADDER_STEPS -> {
                playLadderStep(player, state.anchorPos());
                state.setNextStepTick(now + 7L + player.serverLevel().random.nextInt(5));
            }
            default -> {
                playFootstepBehind(player);
                state.setNextStepTick(now + 4L + player.serverLevel().random.nextInt(4));
            }
        }
    }

    private static void tickFlashError(ServerPlayer player, long now) {
        FlashErrorState state = ACTIVE_FLASH_EVENTS.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (player.isDeadOrDying()) {
            despawnFlashStalker(player.serverLevel(), state);
            ACTIVE_FLASH_EVENTS.remove(player.getUUID());
            return;
        }

        if (!state.shouldSpawnMonster()) {
            if (now >= state.nextSpawnTick()) {
                ACTIVE_FLASH_EVENTS.remove(player.getUUID());
            }
            return;
        }

        UncannyStalkerEntity stalker = resolveFlashStalker(player.serverLevel(), state);
        if (stalker == null) {
            if (now < state.nextSpawnTick()) {
                return;
            }

            SpawnDistanceWindow window = resolveFarSpawnWindow(player, 54, 132, 0.68D);
            UncannyStalkerEntity spawned = spawnStalkerEntity(player, window.minDistance(), window.maxDistance(), true, false);
            if (spawned == null) {
                state.setNextSpawnTick(now + 30L);
                return;
            }

            state.setStalkerUuid(spawned.getUUID());
            state.setSpawnedTick(now);
            return;
        }

        if (stalker.distanceToSqr(player) <= 4.0D) {
            ACTIVE_FLASH_EVENTS.remove(player.getUUID());
            return;
        }

        if (now - state.spawnedTick() >= 20L * 60L) {
            stalker.discard();
            state.clearStalker();
            state.setNextSpawnTick(now + 20L);
        }
    }

    private static void tickTurnAroundTrap(ServerPlayer player, long now) {
        TurnAroundTrapState state = ACTIVE_TURN_AROUND_TRAPS.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (player.isDeadOrDying() || now > state.checkUntilTick()) {
            ACTIVE_TURN_AROUND_TRAPS.remove(player.getUUID());
            return;
        }

        Vec3 currentLook = player.getLookAngle().normalize();
        if (currentLook.dot(state.initialLook()) <= -0.80D) {
            triggerTotalBlackout(player);
            ACTIVE_TURN_AROUND_TRAPS.remove(player.getUUID());
        }
    }

    private static void playFootstepBehind(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
        if (backward.lengthSqr() < 0.001D) {
            backward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            backward = backward.normalize();
        }

        float yawOffset = (-65.0F + level.random.nextFloat() * 130.0F) * ((float) Math.PI / 180.0F);
        Vec3 direction = backward.yRot(yawOffset).normalize();
        double distance = 1.8D + level.random.nextDouble() * 2.8D;

        double x = player.getX() + direction.x * distance;
        double z = player.getZ() + direction.z * distance;
        int y = Mth.floor(player.getY());

        BlockPos basePos = new BlockPos(Mth.floor(x), y, Mth.floor(z));
        BlockPos stepPos = findGroundBlock(level, basePos);
        BlockState groundState = level.getBlockState(stepPos);

        SoundEvent stepSound = groundState.getSoundType().getStepSound();
        playLocalSoundAt(
                player,
                x,
                stepPos.getY() + 1.0D,
                z,
                stepSound,
                SoundSource.HOSTILE,
                0.92F,
                0.85F + level.random.nextFloat() * 0.25F);
    }

    private static void playEchoFootstep(ServerPlayer player, BlockPos samplePos) {
        ServerLevel level = player.serverLevel();
        BlockPos ground = findGroundBlock(level, samplePos);
        BlockState groundState = level.getBlockState(ground);
        SoundEvent stepSound = groundState.getSoundType().getStepSound();
        playLocalSoundAt(player, player.getX(), player.getY(), player.getZ(), stepSound, SoundSource.HOSTILE, 1.0F, 0.84F + level.random.nextFloat() * 0.16F);
    }

    private static boolean playSprintMacabreFootstep(ServerPlayer player, FootstepsState state) {
        ServerLevel level = player.serverLevel();
        double distance = state.sprintDistance();
        if (distance <= 0.5D) {
            BlockPos under = findGroundBlock(level, player.blockPosition().below());
            SoundEvent step = level.getBlockState(under).getSoundType().getStepSound();
            playLocalSoundAt(player, player.getX(), player.getY(), player.getZ(), step, SoundSource.HOSTILE, 1.15F, 0.8F);
            return true;
        }

        Vec3 look = player.getLookAngle();
        Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
        if (backward.lengthSqr() < 0.001D) {
            backward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            backward = backward.normalize();
        }

        double x = player.getX() + backward.x * distance;
        double z = player.getZ() + backward.z * distance;
        int y = Mth.floor(player.getY());
        BlockPos basePos = new BlockPos(Mth.floor(x), y, Mth.floor(z));
        BlockPos ground = findGroundBlock(level, basePos);
        SoundEvent stepSound = level.getBlockState(ground).getSoundType().getStepSound();
        playLocalSoundAt(player, x, ground.getY() + 1.0D, z, stepSound, SoundSource.HOSTILE, 1.05F, 0.88F + level.random.nextFloat() * 0.20F);

        state.setSprintDistance(distance - (1.1D + level.random.nextDouble() * 0.6D));
        return false;
    }

    private static void playHeavyFootstep(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
        if (backward.lengthSqr() < 0.001D) {
            backward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            backward = backward.normalize();
        }

        double distance = 2.0D + level.random.nextDouble() * 2.6D;
        double x = player.getX() + backward.x * distance;
        double z = player.getZ() + backward.z * distance;
        BlockPos source = new BlockPos(Mth.floor(x), Mth.floor(player.getY()), Mth.floor(z));
        playLocalSoundAt(player, source, level.random.nextBoolean() ? SoundEvents.IRON_GOLEM_STEP : SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.25F, 0.62F);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 12, 0, false, false, true));
    }

    private static void playLadderStep(ServerPlayer player, BlockPos ladderPos) {
        if (ladderPos == null || ladderPos == BlockPos.ZERO) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double yOffset = (level.random.nextBoolean() ? 0.1D : 0.8D) + level.random.nextDouble() * 0.6D;
        playLocalSoundAt(
                player,
                ladderPos.getX() + 0.5D,
                ladderPos.getY() + yOffset,
                ladderPos.getZ() + 0.5D,
                SoundEvents.LADDER_STEP,
                SoundSource.HOSTILE,
                0.92F,
                0.90F + level.random.nextFloat() * 0.18F);
    }

    private static BlockPos findGroundBlock(ServerLevel level, BlockPos around) {
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos candidate = around.offset(0, dy, 0);
            if (!level.getBlockState(candidate).isAir()) {
                return candidate;
            }
        }
        return around.below();
    }

    private static void triggerChestReplay(ServerPlayer player, ServerLevel level, BlockPos baseCenter, long now) {
        List<BlockPos> chests = findNearbyChests(level, baseCenter, 8);
        if (!chests.isEmpty()) {
            BlockPos chestPos = chests.get(level.random.nextInt(chests.size()));
            BlockState state = level.getBlockState(chestPos);
            if (state.getBlock() instanceof ChestBlock) {
                level.blockEvent(chestPos, state.getBlock(), 1, 1);
                level.sendBlockUpdated(chestPos, state, state, 3);
                CHEST_CLOSE_TASKS.add(new ChestCloseTask(player.getUUID(), level.dimension(), chestPos.immutable(), now + 40L + level.random.nextInt(81)));
                playLocalSoundAt(player, chestPos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
                return;
            }
        }

        playEventSound(player, level, baseCenter, SoundEvents.CHEST_OPEN, 0.95F, 0.85F, 1.05F);
    }

    private static void triggerPanickedChest(ServerPlayer player, ServerLevel level, BlockPos chestPos, long now) {
        BlockState state = level.getBlockState(chestPos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return;
        }

        int cycles = 4 + level.random.nextInt(2);
        for (int i = 0; i < cycles; i++) {
            long openTick = now + i * 6L;
            long closeTick = openTick + 3L;
            CHEST_PANIC_TASKS.add(new ChestPanicTask(player.getUUID(), level.dimension(), chestPos.immutable(), openTick, true));
            CHEST_PANIC_TASKS.add(new ChestPanicTask(player.getUUID(), level.dimension(), chestPos.immutable(), closeTick, false));
        }
    }

    private static void triggerArtisanFail(ServerPlayer player, ServerLevel level, BlockPos craftingPos, BlockPos anvilPos) {
        List<BlockPos> options = new ArrayList<>();
        if (craftingPos != null) {
            options.add(craftingPos.immutable());
        }
        if (anvilPos != null) {
            options.add(anvilPos.immutable());
        }
        if (options.isEmpty()) {
            return;
        }

        BlockPos source = options.get(level.random.nextInt(options.size()));
        playLocalSoundAt(player, source, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.7F, 0.70F);
        playCustomEventSound(player, level, source, UncannySoundRegistry.UNCANNY_WHISPER.get(), 0.55F, 0.9F, 1.1F);
        playLocalSoundAt(player, source, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.45F, 0.9F);
    }

    private static void triggerTorchDying(ServerPlayer player, ServerLevel level, List<BlockPos> torches) {
        if (torches.isEmpty()) {
            return;
        }

        int count = Math.min(torches.size(), 3 + level.random.nextInt(4));
        for (int i = 0; i < count; i++) {
            BlockPos pos = torches.get(level.random.nextInt(torches.size()));
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.72D,
                    pos.getZ() + 0.5D,
                    18,
                    0.28D,
                    0.28D,
                    0.28D,
                    0.01D);
            playLocalSoundAt(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.95F, 0.8F + level.random.nextFloat() * 0.12F);
        }
    }

    private static List<BlockPos> findNearbyChests(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof ChestBlock) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
    }

    private static List<BlockPos> findNearbyTorches(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.TORCH)
                            || state.is(Blocks.WALL_TORCH)
                            || state.is(Blocks.SOUL_TORCH)
                            || state.is(Blocks.SOUL_WALL_TORCH)
                            || state.is(Blocks.REDSTONE_TORCH)
                            || state.is(Blocks.REDSTONE_WALL_TORCH)) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
    }

    private static List<BlockPos> findNearbyDoors(ServerLevel level, BlockPos center, int radius) {
        Set<BlockPos> unique = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof DoorBlock) {
                        BlockPos basePos = normalizeDoorPos(level, pos);
                        if (basePos != null) {
                            unique.add(basePos.immutable());
                        }
                    } else if (state.getBlock() instanceof TrapDoorBlock) {
                        unique.add(pos.immutable());
                    }
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static List<BlockPos> findNearbyCrops(ServerLevel level, BlockPos center, int radius) {
        Set<BlockPos> unique = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock) {
                        unique.add(pos.immutable());
                    }
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static BlockPos findNearbyBlock(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> matcher) {
        List<BlockPos> matches = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (matcher.test(state)) {
                        matches.add(pos.immutable());
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(level.random.nextInt(matches.size()));
    }

    private static BlockPos findNearbyEmptyFurnace(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> matches = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof AbstractFurnaceBlock) || !state.hasProperty(BlockStateProperties.LIT)) {
                        continue;
                    }
                    if (state.getValue(BlockStateProperties.LIT)) {
                        continue;
                    }
                    if (!isFurnaceInventoryEmpty(level.getBlockEntity(pos))) {
                        continue;
                    }
                    matches.add(pos.immutable());
                }
            }
        }

        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(level.random.nextInt(matches.size()));
    }

    private static BlockPos normalizeDoorPos(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                return pos.below().immutable();
            }
            return pos.immutable();
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            return pos.immutable();
        }
        return null;
    }

    private static void toggleDoorOrTrap(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return;
        }

        boolean currentlyOpen = state.getValue(BlockStateProperties.OPEN);
        boolean nextOpen = !currentlyOpen;
        setDoorOpen(player, level, pos, nextOpen);
    }

    private static void closeDoor(ServerPlayer player, ServerLevel level, BlockPos pos) {
        setDoorOpen(player, level, pos, false);
    }

    private static void setDoorOpen(ServerPlayer player, ServerLevel level, BlockPos rawPos, boolean open) {
        BlockPos normalized = normalizeDoorPos(level, rawPos);
        if (normalized == null) {
            return;
        }

        BlockState base = level.getBlockState(normalized);
        if (!base.hasProperty(BlockStateProperties.OPEN)) {
            return;
        }
        if (base.getValue(BlockStateProperties.OPEN) != open) {
            level.setBlock(normalized, base.setValue(BlockStateProperties.OPEN, open), 10);
        }

        if (base.getBlock() instanceof DoorBlock) {
            BlockPos upperPos = normalized.above();
            BlockState upper = level.getBlockState(upperPos);
            if (upper.hasProperty(BlockStateProperties.OPEN) && upper.getValue(BlockStateProperties.OPEN) != open) {
                level.setBlock(upperPos, upper.setValue(BlockStateProperties.OPEN, open), 10);
            }
            playLocalSoundAt(
                    player,
                    normalized,
                    open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.95F + level.random.nextFloat() * 0.1F);
        } else if (base.getBlock() instanceof TrapDoorBlock) {
            playLocalSoundAt(
                    player,
                    normalized,
                    open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.95F + level.random.nextFloat() * 0.1F);
        }
    }

    private static void lockDoor(ResourceKey<Level> dimension, BlockPos doorPos, long unlockTick) {
        LOCKED_DOORS.computeIfAbsent(dimension, key -> new HashMap<>()).put(doorPos.immutable(), unlockTick);
    }

    private static boolean isDoorLocked(ServerLevel level, BlockPos rawPos, long now) {
        BlockPos normalized = normalizeDoorPos(level, rawPos);
        if (normalized == null) {
            return false;
        }

        Map<BlockPos, Long> perDimension = LOCKED_DOORS.get(level.dimension());
        if (perDimension == null) {
            return false;
        }
        Long unlockTick = perDimension.get(normalized);
        if (unlockTick == null) {
            return false;
        }

        if (now >= unlockTick || normalizeDoorPos(level, normalized) == null) {
            perDimension.remove(normalized);
            if (perDimension.isEmpty()) {
                LOCKED_DOORS.remove(level.dimension());
            }
            return false;
        }
        return true;
    }

    private static boolean isFurnaceInventoryEmpty(BlockEntity blockEntity) {
        if (!(blockEntity instanceof AbstractFurnaceBlockEntity furnace)) {
            return false;
        }

        for (int slot = 0; slot < furnace.getContainerSize(); slot++) {
            if (!furnace.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void lightFurnaceTemporarily(ServerPlayer player, ServerLevel level, BlockPos pos, long resetTick) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractFurnaceBlock) || !state.hasProperty(BlockStateProperties.LIT)) {
            return;
        }

        if (!state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
            playLocalSoundAt(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.85F, 1.05F);
            FURNACE_RESET_TASKS.add(new FurnaceResetTask(player.getUUID(), level.dimension(), pos.immutable(), resetTick));
        }
    }

    private static void playEventSound(ServerPlayer player, ServerLevel level, BlockPos center, SoundEvent sound, float volume, float minPitch, float maxPitch) {
        double x = center.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 6.0D;
        double y = center.getY() + 0.5D + level.random.nextDouble() * 2.0D;
        double z = center.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 6.0D;
        playLocalSoundAt(player, x, y, z, sound, SoundSource.BLOCKS, volume, minPitch + level.random.nextFloat() * (maxPitch - minPitch));
    }

    private static void playEventSoundAt(ServerPlayer player, ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float minPitch, float maxPitch) {
        if (pos == null) {
            return;
        }
        playLocalSoundAt(player, pos, sound, SoundSource.BLOCKS, volume, minPitch + level.random.nextFloat() * (maxPitch - minPitch));
    }

    private static void playCustomEventSound(ServerPlayer player, ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float minPitch, float maxPitch) {
        if (pos == null) {
            return;
        }
        playLocalSoundAt(player,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                sound,
                SoundSource.HOSTILE,
                volume,
                minPitch + level.random.nextFloat() * (maxPitch - minPitch));
    }

    private static void playLocalSound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        playLocalSoundAt(player, player.getX(), player.getEyeY(), player.getZ(), sound, source, volume, pitch);
    }

    private static void playLocalSoundAt(ServerPlayer player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (pos == null) {
            return;
        }
        playLocalSoundAt(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, sound, source, volume, pitch);
    }

    private static void playLocalSoundAt(ServerPlayer player, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                source,
                x,
                y,
                z,
                volume,
                pitch,
                player.level().random.nextLong()));
    }

    private static void stopAllSoundsForPlayer(ServerPlayer player) {
        // Keep HOSTILE/VOICE alive so custom mod sounds are never muted by paranoia systems.
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.RECORDS));
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.WEATHER));
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.BLOCKS));
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.NEUTRAL));
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.PLAYERS));
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
    }

    private static ChatFormatting parseChatFormatting(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return ChatFormatting.DARK_RED;
        }
        String normalized = rawName.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        ChatFormatting parsed = ChatFormatting.getByName(normalized);
        if (parsed != null) {
            return parsed;
        }
        return switch (normalized) {
            case "DARKGREY", "DARKGRAY" -> ChatFormatting.DARK_GRAY;
            case "LIGHTGREY", "LIGHTGRAY" -> ChatFormatting.GRAY;
            case "MAGENTA" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.DARK_RED;
        };
    }

    private static void tickChestCloseTasks(MinecraftServer server, long now) {
        if (lastChestTaskTick == now) {
            return;
        }
        lastChestTaskTick = now;

        Iterator<ChestPanicTask> panicIterator = CHEST_PANIC_TASKS.iterator();
        while (panicIterator.hasNext()) {
            ChestPanicTask task = panicIterator.next();
            if (task.toggleTick() > now) {
                continue;
            }

            ServerLevel level = server.getLevel(task.dimension());
            if (level != null) {
                BlockState state = level.getBlockState(task.pos());
                if (state.getBlock() instanceof ChestBlock) {
                    int openData = task.open() ? 1 : 0;
                    level.blockEvent(task.pos(), state.getBlock(), 1, openData);
                    level.sendBlockUpdated(task.pos(), state, state, 3);
                    ServerPlayer owner = server.getPlayerList().getPlayer(task.playerId());
                    if (owner != null && owner.serverLevel() == level) {
                        playLocalSoundAt(owner, task.pos(), task.open() ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.95F, 0.9F + level.random.nextFloat() * 0.15F);
                    }
                }
            }
            panicIterator.remove();
        }

        Iterator<ChestCloseTask> iterator = CHEST_CLOSE_TASKS.iterator();
        while (iterator.hasNext()) {
            ChestCloseTask task = iterator.next();
            if (task.closeTick() > now) {
                continue;
            }

            ServerLevel level = server.getLevel(task.dimension());
            if (level != null) {
                BlockState state = level.getBlockState(task.pos());
                if (state.getBlock() instanceof ChestBlock) {
                    level.blockEvent(task.pos(), state.getBlock(), 1, 0);
                    level.sendBlockUpdated(task.pos(), state, state, 3);
                    ServerPlayer owner = server.getPlayerList().getPlayer(task.playerId());
                    if (owner != null && owner.serverLevel() == level) {
                        playLocalSoundAt(owner, task.pos(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
                    }
                }
            }
            iterator.remove();
        }

        Iterator<FurnaceResetTask> furnaceIterator = FURNACE_RESET_TASKS.iterator();
        while (furnaceIterator.hasNext()) {
            FurnaceResetTask task = furnaceIterator.next();
            if (task.resetTick() > now) {
                continue;
            }

            ServerLevel level = server.getLevel(task.dimension());
            if (level != null) {
                BlockState state = level.getBlockState(task.pos());
                if (state.getBlock() instanceof AbstractFurnaceBlock && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
                    level.setBlock(task.pos(), state.setValue(BlockStateProperties.LIT, false), 3);
                }
            }
            furnaceIterator.remove();
        }

        Iterator<WaterRestoreTask> waterIterator = WATER_RESTORE_TASKS.iterator();
        while (waterIterator.hasNext()) {
            WaterRestoreTask task = waterIterator.next();
            if (task.restoreTick() > now) {
                continue;
            }

            ServerLevel level = server.getLevel(task.dimension());
            if (level != null) {
                BlockState current = level.getBlockState(task.pos());
                if (current.is(Blocks.WATER)) {
                    level.setBlock(task.pos(), task.originalState(), 3);
                }
            }
            waterIterator.remove();
        }

        Iterator<Map.Entry<ResourceKey<Level>, Map<BlockPos, Long>>> dimensionIterator = LOCKED_DOORS.entrySet().iterator();
        while (dimensionIterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, Map<BlockPos, Long>> dimEntry = dimensionIterator.next();
            Map<BlockPos, Long> locked = dimEntry.getValue();
            locked.entrySet().removeIf(entry -> entry.getValue() <= now);
            if (locked.isEmpty()) {
                dimensionIterator.remove();
            }
        }
    }

    private static boolean spawnBellUncannyAttacker(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = findSpawnAroundPlayer(level, player, 8, 18, false);
        if (pos == null) {
            return false;
        }

        EntityType<? extends Mob>[] pool = new EntityType[]{
                UncannyEntityRegistry.UNCANNY_ZOMBIE.get(),
                UncannyEntityRegistry.UNCANNY_HUSK.get(),
                UncannyEntityRegistry.UNCANNY_ZOMBIE_VILLAGER.get(),
                UncannyEntityRegistry.UNCANNY_SKELETON.get(),
                UncannyEntityRegistry.UNCANNY_STRAY.get()
        };

        EntityType<? extends Mob> type = pool[level.random.nextInt(pool.length)];
        Mob mob = type.create(level);
        if (mob == null) {
            return false;
        }

        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (mob instanceof Monster monster) {
            monster.setTarget(player);
        }
        level.addFreshEntity(mob);
        return true;
    }

    private static UncannyStalkerEntity spawnStalkerEntity(
            ServerPlayer player,
            int minDistance,
            int maxDistance,
            boolean preferBehind,
            boolean requireObserverStealth) {
        if (shouldBlockSpecialSpawn(player)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = null;
        if (preferBehind) {
            pos = requireObserverStealth
                    ? findCommandSpawnBehindPlayer(level, player, minDistance, maxDistance, false)
                    : findSpawnBehindPlayer(level, player, minDistance, maxDistance, false);
        }
        if (pos == null) {
            pos = requireObserverStealth
                    ? findCommandSpawnAroundPlayer(level, player, minDistance, maxDistance, false)
                    : findSpawnAroundPlayer(level, player, minDistance, maxDistance, false);
        }
        pos = refineSpecialSpawnPos(level, player, pos, false, preferBehind, Math.max(8, minDistance / 2), maxDistance);
        if (pos == null) {
            return null;
        }

        UncannyStalkerEntity stalker = UncannyEntityRegistry.UNCANNY_STALKER.get().create(level);
        if (stalker == null) {
            return null;
        }

        stalker.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        stalker.setHuntTarget(player);
        level.addFreshEntity(stalker);
        return stalker;
    }

    private static UncannyStalkerEntity resolveFlashStalker(ServerLevel level, FlashErrorState state) {
        UUID stalkerId = state.stalkerUuid();
        if (stalkerId == null) {
            return null;
        }

        Entity entity = level.getEntity(stalkerId);
        if (entity instanceof UncannyStalkerEntity stalker && stalker.isAlive()) {
            return stalker;
        }
        return null;
    }

    private static void despawnFlashStalker(ServerLevel level, FlashErrorState state) {
        UncannyStalkerEntity stalker = resolveFlashStalker(level, state);
        if (stalker != null) {
            stalker.discard();
        }
        state.clearStalker();
    }

    private static SpawnDistanceWindow resolveFarSpawnWindow(
            ServerPlayer player,
            int fallbackMinDistance,
            int fallbackMaxDistance,
            double minDistanceRatio) {
        int maxDistance = resolveRuntimeMaxSpawnDistance(player, fallbackMaxDistance);
        maxDistance = Math.min(maxDistance, Math.max(fallbackMaxDistance, fallbackMaxDistance + 28));
        if (!player.serverLevel().canSeeSky(player.blockPosition())) {
            int caveCap = Math.max(fallbackMinDistance + 8, Math.min(fallbackMaxDistance, 36));
            maxDistance = Math.min(maxDistance, caveCap);
        }
        int minDistance = Math.max(fallbackMinDistance, (int) Math.floor(maxDistance * minDistanceRatio));
        if (minDistance >= maxDistance - 6) {
            minDistance = Math.max(fallbackMinDistance, maxDistance - 8);
        }
        if (minDistance <= 0) {
            minDistance = Math.max(8, fallbackMinDistance);
        }
        return new SpawnDistanceWindow(minDistance, maxDistance);
    }

    private static int resolveRuntimeMaxSpawnDistance(ServerPlayer player, int fallbackMaxDistance) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return fallbackMaxDistance;
        }

        int viewDistanceChunks = Math.max(2, server.getPlayerList().getViewDistance());
        int simulationDistanceChunks = Math.max(2, server.getPlayerList().getSimulationDistance());
        int effectiveChunks = Math.max(2, Math.min(viewDistanceChunks, simulationDistanceChunks));

        int chunkLimitedMax = Math.max(24, effectiveChunks * 16 - 20);
        return Math.max(24, Math.min(208, chunkLimitedMax));
    }

    private static BlockPos findSpawnNearDoorOutside(ServerLevel level, BlockPos doorPos, ServerPlayer player) {
        BlockPos best = null;
        double bestDistance = -1.0D;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = doorPos.relative(direction);
            if (!canSpawnAt(level, candidate, false)) {
                continue;
            }

            double distanceToPlayer = candidate.distSqr(player.blockPosition());
            if (distanceToPlayer > bestDistance) {
                bestDistance = distanceToPlayer;
                best = candidate.immutable();
            }
        }

        return best;
    }

    private static BlockPos findNearbySolidWall(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (level.getBlockState(pos.relative(Direction.NORTH)).isAir()
                            || level.getBlockState(pos.relative(Direction.SOUTH)).isAir()
                            || level.getBlockState(pos.relative(Direction.EAST)).isAir()
                            || level.getBlockState(pos.relative(Direction.WEST)).isAir()) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private static BlockPos findGhostMinerStrikePos(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();

        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (Math.abs(dx) + Math.abs(dz) < 3) {
                    continue;
                }
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!isGhostMinerStrikeable(level, pos)) {
                        continue;
                    }
                    candidates.add(pos.immutable());
                }
            }
        }

        if (candidates.isEmpty()) {
            return findNearbySolidWall(level, player);
        }
        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private static BlockPos findGhostMinerStartPos(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq < 100 || distSq > 324) {
                    continue;
                }
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (isGhostMinerStrikeable(level, pos)) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private static BlockPos advanceGhostMinerStrikePos(ServerLevel level, ServerPlayer player, GhostMinerState state) {
        BlockPos current = state.wallPos();
        if (current == null) {
            return findGhostMinerStrikePos(level, player);
        }

        BlockPos playerPos = player.blockPosition();
        double currentDistance = horizontalDistance(current, playerPos);
        BlockPos anchor;

        if (currentDistance > 5.0D) {
            int stepX = Integer.compare(playerPos.getX(), current.getX());
            int stepZ = Integer.compare(playerPos.getZ(), current.getZ());
            anchor = current.offset(stepX, 0, stepZ);
        } else {
            double radius = Math.max(2.0D, state.orbitRadius() - 0.25D);
            state.setOrbitRadius(radius);
            float angle = state.orbitAngleDegrees() + 26.0F + player.getRandom().nextFloat() * 24.0F;
            state.setOrbitAngleDegrees(angle);
            double radians = Math.toRadians(angle);
            anchor = new BlockPos(
                    Mth.floor(player.getX() + Math.cos(radians) * radius),
                    playerPos.getY(),
                    Mth.floor(player.getZ() + Math.sin(radians) * radius));
        }

        BlockPos strike = findGhostMinerStrikeableNear(level, anchor, 2);
        if (strike == null) {
            strike = findGhostMinerStrikeableNear(level, anchor, 4);
        }
        if (strike == null) {
            strike = findGhostMinerStrikePos(level, player);
        }
        if (strike == null) {
            return null;
        }

        double strikeDistance = horizontalDistance(strike, playerPos);
        if (strikeDistance < 2.0D) {
            int awayX = Integer.compare(strike.getX(), playerPos.getX());
            int awayZ = Integer.compare(strike.getZ(), playerPos.getZ());
            BlockPos shifted = findGhostMinerStrikeableNear(level, strike.offset(awayX * 2, 0, awayZ * 2), 3);
            if (shifted != null) {
                strike = shifted;
            }
        }

        return strike;
    }

    private static BlockPos findGhostMinerStrikeableNear(ServerLevel level, BlockPos anchor, int radius) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = anchor.offset(dx, dy, dz);
                    if (isGhostMinerStrikeable(level, pos)) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = (a.getX() + 0.5D) - (b.getX() + 0.5D);
        double dz = (a.getZ() + 0.5D) - (b.getZ() + 0.5D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean isGhostMinerStrikeable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return level.getBlockState(pos.relative(Direction.NORTH)).isAir()
                || level.getBlockState(pos.relative(Direction.SOUTH)).isAir()
                || level.getBlockState(pos.relative(Direction.EAST)).isAir()
                || level.getBlockState(pos.relative(Direction.WEST)).isAir()
                || level.getBlockState(pos.above()).isAir();
    }

    private static void spawnWallShadowSilhouette(ServerLevel level, ServerPlayer player, BlockPos wall, float intensity) {
        if (wall == null) {
            return;
        }

        Direction face = Direction.NORTH;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos airPos = wall.relative(direction);
            if (!level.getBlockState(airPos).isAir()) {
                continue;
            }
            double distance = airPos.distSqr(player.blockPosition());
            if (distance < bestDistance) {
                bestDistance = distance;
                face = direction;
            }
        }

        Vec3 anchor = Vec3.atCenterOf(wall).add(face.getStepX() * 0.51D, 0.0D, face.getStepZ() * 0.51D);
        double[][] offsets = {
                {0.00D, 0.15D}, {0.00D, 0.45D}, {0.00D, 0.75D}, {0.00D, 1.05D}, {0.00D, 1.35D}, {0.00D, 1.65D},
                {-0.30D, 0.75D}, {0.30D, 0.75D},
                {-0.26D, 1.25D}, {0.26D, 1.25D}
        };
        double sideX = face.getClockWise().getStepX();
        double sideZ = face.getClockWise().getStepZ();
        int burstCount = Math.max(1, Math.round(7F * intensity));

        for (double[] offset : offsets) {
            double x = anchor.x + sideX * offset[0];
            double y = anchor.y + offset[1];
            double z = anchor.z + sideZ * offset[0];
            level.sendParticles(
                    ParticleTypes.SQUID_INK,
                    x,
                    y,
                    z,
                    burstCount,
                    0.03D,
                    0.03D,
                    0.03D,
                    0.001D);
        }
    }

    private static BlockPos findSpawnBehindPlayer(ServerLevel level, ServerPlayer player, int minDistance, int maxDistance, boolean requireDark) {
        Vec3 look = player.getLookAngle();
        Vec3 behind = new Vec3(-look.x, 0.0D, -look.z);
        if (behind.lengthSqr() < 0.001D) {
            behind = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            behind = behind.normalize();
        }

        int baseY = Mth.floor(player.getY());
        for (int attempt = 0; attempt < 28; attempt++) {
            float yawOffset = (-28.0F + level.random.nextFloat() * 56.0F) * ((float) Math.PI / 180.0F);
            Vec3 direction = behind.yRot(yawOffset).normalize();
            double distance = minDistance + level.random.nextDouble() * (maxDistance - minDistance);

            int x = Mth.floor(player.getX() + direction.x * distance);
            int z = Mth.floor(player.getZ() + direction.z * distance);
            BlockPos candidate = findSpawnAtOrAbovePlayerY(level, x, z, baseY, requireDark);
            if (candidate != null) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos fallbackSpawnBehindOffset(
            ServerLevel level,
            ServerPlayer player,
            int baseDistance,
            boolean requireDark) {
        Vec3 look = player.getLookAngle();
        Vec3 behind = new Vec3(-look.x, 0.0D, -look.z);
        if (behind.lengthSqr() < 0.0001D) {
            behind = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            behind = behind.normalize();
        }

        int baseY = Mth.floor(player.getY());
        for (int distance = Math.max(4, baseDistance); distance <= baseDistance + 10; distance += 2) {
            int x = Mth.floor(player.getX() + behind.x * distance);
            int z = Mth.floor(player.getZ() + behind.z * distance);
            BlockPos candidate = findSpawnAtOrAbovePlayerY(level, x, z, baseY, requireDark);
            if (isSpecialSpawnContextValid(level, player, candidate)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findCommandSpawnBehindPlayer(
            ServerLevel level,
            ServerPlayer targetPlayer,
            int minDistance,
            int maxDistance,
            boolean requireDark) {
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos candidate = findSpawnBehindPlayer(level, targetPlayer, minDistance, maxDistance, requireDark);
            if (candidate == null) {
                continue;
            }

            Vec3 center = Vec3.atCenterOf(candidate);
            if (!isOutsideViewCone(targetPlayer, center, 0.12D)) {
                continue;
            }
            if (!isOutOfSightOfOtherPlayers(level, targetPlayer, center)) {
                continue;
            }
            return candidate.immutable();
        }
        return null;
    }

    private static BlockPos findCommandSpawnAroundPlayer(
            ServerLevel level,
            ServerPlayer targetPlayer,
            int minDistance,
            int maxDistance,
            boolean requireDark) {
        for (int attempt = 0; attempt < 18; attempt++) {
            BlockPos candidate = findSpawnAroundPlayer(level, targetPlayer, minDistance, maxDistance, requireDark);
            if (candidate == null) {
                continue;
            }

            Vec3 center = Vec3.atCenterOf(candidate);
            if (!isOutsideViewCone(targetPlayer, center, 0.22D)) {
                continue;
            }
            if (!isOutOfSightOfOtherPlayers(level, targetPlayer, center)) {
                continue;
            }
            return candidate.immutable();
        }
        return null;
    }

    private static BlockPos findSpawnAroundPlayer(ServerLevel level, ServerPlayer player, int minDistance, int maxDistance, boolean requireDark) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontal = horizontal.normalize();
        }

        int baseY = Mth.floor(player.getY());

        for (int attempt = 0; attempt < 30; attempt++) {
            float yawOffset = (-120.0F + level.random.nextFloat() * 240.0F) * ((float) Math.PI / 180.0F);
            Vec3 direction = horizontal.yRot(yawOffset).normalize();
            double distance = minDistance + level.random.nextDouble() * (maxDistance - minDistance);

            int x = Mth.floor(player.getX() + direction.x * distance);
            int z = Mth.floor(player.getZ() + direction.z * distance);
            BlockPos candidate = findSpawnAtOrAbovePlayerY(level, x, z, baseY, requireDark);
            if (candidate != null) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findBrightSpawnAroundPlayer(
            ServerLevel level,
            ServerPlayer player,
            int minDistance,
            int maxDistance,
            int minBrightness) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontal = horizontal.normalize();
        }

        int baseY = Mth.floor(player.getY());
        for (int attempt = 0; attempt < 36; attempt++) {
            float yawOffset = (-120.0F + level.random.nextFloat() * 240.0F) * ((float) Math.PI / 180.0F);
            Vec3 direction = horizontal.yRot(yawOffset).normalize();
            double distance = minDistance + level.random.nextDouble() * (maxDistance - minDistance);

            int x = Mth.floor(player.getX() + direction.x * distance);
            int z = Mth.floor(player.getZ() + direction.z * distance);
            BlockPos candidate = findBrightSpawnAtOrAbovePlayerY(level, x, z, baseY, minBrightness);
            if (candidate != null) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findSpawnAtOrAbovePlayerY(
            ServerLevel level,
            int x,
            int z,
            int baseY,
            boolean requireDark) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int startY = Mth.clamp(baseY, minY, maxY);

        BlockPos candidate = new BlockPos(x, startY, z);
        if (canSpawnAt(level, candidate, requireDark)) {
            return candidate.immutable();
        }

        int downwardLimit = Math.max(minY, startY - 24);
        for (int y = startY - 1; y >= downwardLimit; y--) {
            candidate = new BlockPos(x, y, z);
            if (canSpawnAt(level, candidate, requireDark)) {
                return candidate.immutable();
            }
        }

        int upwardLimit = Math.min(maxY, startY + 32);
        for (int y = startY + 1; y <= upwardLimit; y++) {
            candidate = new BlockPos(x, y, z);
            if (canSpawnAt(level, candidate, requireDark)) {
                return candidate.immutable();
            }
        }

        int surfaceY = Mth.clamp(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), minY, maxY);
        if (surfaceY >= startY) {
            candidate = new BlockPos(x, surfaceY, z);
            if (canSpawnAt(level, candidate, requireDark)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static BlockPos findBrightSpawnAtOrAbovePlayerY(
            ServerLevel level,
            int x,
            int z,
            int baseY,
            int minBrightness) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int startY = Mth.clamp(baseY, minY, maxY);

        BlockPos candidate = new BlockPos(x, startY, z);
        if (canSpawnAt(level, candidate, false) && level.getRawBrightness(candidate, 0) >= minBrightness) {
            return candidate.immutable();
        }

        int downwardLimit = Math.max(minY, startY - 24);
        for (int y = startY - 1; y >= downwardLimit; y--) {
            candidate = new BlockPos(x, y, z);
            if (canSpawnAt(level, candidate, false) && level.getRawBrightness(candidate, 0) >= minBrightness) {
                return candidate.immutable();
            }
        }

        int upwardLimit = Math.min(maxY, startY + 32);
        for (int y = startY + 1; y <= upwardLimit; y++) {
            candidate = new BlockPos(x, y, z);
            if (canSpawnAt(level, candidate, false) && level.getRawBrightness(candidate, 0) >= minBrightness) {
                return candidate.immutable();
            }
        }

        int surfaceY = Mth.clamp(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), minY, maxY);
        if (surfaceY >= startY) {
            candidate = new BlockPos(x, surfaceY, z);
            if (canSpawnAt(level, candidate, false) && level.getRawBrightness(candidate, 0) >= minBrightness) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static boolean canSpawnAt(ServerLevel level, BlockPos pos, boolean requireDark) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        if ((!feet.isAir() && !feet.canBeReplaced()) || (!head.isAir() && !head.canBeReplaced())) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }

        if (!below.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }

        if (requireDark && level.getRawBrightness(pos, 0) > 4) {
            return false;
        }

        return true;
    }

    private static BlockPos refineSpecialSpawnPos(
            ServerLevel level,
            ServerPlayer player,
            BlockPos initial,
            boolean requireDark,
            boolean preferBehind,
            int minDistance,
            int maxDistance) {
        if (isSpecialSpawnContextValid(level, player, initial)) {
            return initial.immutable();
        }

        for (int attempt = 0; attempt < 34; attempt++) {
            BlockPos candidate = preferBehind
                    ? findSpawnBehindPlayer(level, player, minDistance, maxDistance, requireDark)
                    : findSpawnAroundPlayer(level, player, minDistance, maxDistance, requireDark);
            if (isSpecialSpawnContextValid(level, player, candidate)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private static boolean isSpecialSpawnContextValid(ServerLevel level, ServerPlayer player, BlockPos candidate) {
        if (candidate == null) {
            return false;
        }

        if (candidate.getY() < player.blockPosition().getY() - 3) {
            return false;
        }

        if (level.canSeeSky(player.blockPosition()) && !level.canSeeSky(candidate)) {
            return false;
        }
        if (!level.canSeeSky(player.blockPosition()) && level.canSeeSky(candidate)) {
            return false;
        }

        return true;
    }

    private static boolean hasAnyNonAirAbove(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        int maxY = level.getMaxBuildHeight() - 1;
        if (pos.getY() >= maxY) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
        for (int y = pos.getY() + 1; y <= maxY; y++) {
            cursor.setY(y);
            if (!level.getBlockState(cursor).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldBlockSpecialSpawn(ServerPlayer player) {
        return player.isInWaterOrBubble() || player.getVehicle() instanceof Boat;
    }

    private static boolean isOutOfSightOfOtherPlayers(ServerLevel level, ServerPlayer targetPlayer, Vec3 spawnPos) {
        for (ServerPlayer otherPlayer : level.players()) {
            if (otherPlayer == targetPlayer || !otherPlayer.isAlive() || otherPlayer.isSpectator()) {
                continue;
            }
            if (otherPlayer.distanceToSqr(spawnPos) <= 7.0D * 7.0D) {
                return false;
            }
            if (otherPlayer.distanceToSqr(spawnPos) > 64.0D * 64.0D) {
                continue;
            }
            if (!isOutsideViewCone(otherPlayer, spawnPos, 0.20D)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOutsideViewCone(ServerPlayer observer, Vec3 observedPos, double dotThreshold) {
        Vec3 toObserved = observedPos.subtract(observer.getEyePosition());
        if (toObserved.lengthSqr() < 0.0001D) {
            return false;
        }
        Vec3 look = observer.getViewVector(1.0F).normalize();
        Vec3 observedDirection = toObserved.normalize();
        return look.dot(observedDirection) < dotThreshold;
    }

    private static void suppressNonGrandEventEffectsDuringGrandPause(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ServerLevel level = player.serverLevel();
        MinecraftServer server = player.getServer();
        if (server != null) {
            UncannyPhase phase = UncannyWorldState.get(server).getPhase();
            if (phase.index() >= UncannyPhase.PHASE_2.index() && (server.getTickCount() % 20L) == 0L) {
                player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
            }
        }

        ACTIVE_BLACKOUTS.remove(playerId);
        ACTIVE_FOOTSTEPS.remove(playerId);
        ACTIVE_FLASH_EVENTS.remove(playerId);
        ACTIVE_DEAFNESS.remove(playerId);
        ACTIVE_VOID_SILENCE.remove(playerId);
        ACTIVE_GHOST_MINERS.remove(playerId);
        ACTIVE_ASPHYXIA.remove(playerId);
        ACTIVE_HUNTER_FOG.remove(playerId);
        ACTIVE_GIANT_SUN.remove(playerId);
        ACTIVE_FURNACE_BREATHS.remove(playerId);
        ACTIVE_HOTBAR_WRONG_COUNTS.remove(playerId);
        ACTIVE_TURN_AROUND_TRAPS.remove(playerId);
        LIVING_ORE_PRIMED.remove(playerId);
        ACTIVE_AQUATIC_BITE.remove(playerId);

        AnimalStareLockState animalStare = ACTIVE_ANIMAL_STARE_LOCKS.remove(playerId);
        if (animalStare != null) {
            for (MobSnapshot snapshot : animalStare.affected()) {
                Entity entity = level.getEntity(snapshot.entityId());
                if (entity instanceof Mob mob && mob.isAlive()) {
                    mob.setNoAi(snapshot.hadNoAi());
                    mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
            }
        }

        MisplacedLightState movedLight = ACTIVE_MISPLACED_LIGHTS.remove(playerId);
        if (movedLight != null) {
            revertMisplacedLight(level, movedLight);
        }

        PetRefusalState petRefusal = ACTIVE_PET_REFUSALS.remove(playerId);
        if (petRefusal != null) {
            Entity raw = level.getEntity(petRefusal.petUuid());
            if (raw instanceof Mob pet) {
                pet.setNoAi(petRefusal.hadNoAi());
                pet.removeTag("eotv_pet_refusal");
                pet.removeTag("eotv_pet_refusal_black");
                removePetRefusalBlackTeam(level, pet);
                sendPetRefusalVisual(player, pet, false, 0);
            } else {
                PacketDistributor.sendToPlayer(player, new UncannyPetRefusalVisualPayload(-1, false, 0));
            }
        }

        CompassLiarState compassLiar = ACTIVE_COMPASS_LIARS.remove(playerId);
        if (compassLiar != null) {
            clearUncannyCompassTarget(player.getMainHandItem());
            clearUncannyCompassTarget(player.getOffhandItem());
        }

        LEVER_REPLY_TASKS.removeIf(task -> task.playerId().equals(playerId));
        DOOR_CASCADE_TASKS.removeIf(task -> task.playerId().equals(playerId));
        PRESSURE_PLATE_REPLY_TASKS.removeIf(task -> task.playerId().equals(playerId));
        TOOL_ANSWER_ECHO_TASKS.removeIf(task -> task.playerId().equals(playerId));

        player.removeTag(FLASH_RED_OVERLAY_TAG);
        player.removeTag(HUNTER_FOG_TAG);
        player.removeTag(GIANT_SUN_TAG);
        UncannyClientStateSync.syncParanoiaState(player, false, false);
    }

    private static void clearPlayerEventState(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ServerLevel level = player.serverLevel();
        ACTIVE_BLACKOUTS.remove(playerId);
        ACTIVE_FOOTSTEPS.remove(playerId);
        ACTIVE_FLASH_EVENTS.remove(playerId);
        ACTIVE_DEAFNESS.remove(playerId);
        ACTIVE_VOID_SILENCE.remove(playerId);
        ACTIVE_GHOST_MINERS.remove(playerId);
        ACTIVE_ASPHYXIA.remove(playerId);
        ACTIVE_HUNTER_FOG.remove(playerId);
        ACTIVE_GIANT_SUN.remove(playerId);
        ACTIVE_COMPASS_LIARS.remove(playerId);
        ACTIVE_ANIMAL_STARE_LOCKS.remove(playerId);
        ACTIVE_FURNACE_BREATHS.remove(playerId);
        MisplacedLightState movedLight = ACTIVE_MISPLACED_LIGHTS.remove(playerId);
        if (movedLight != null) {
            revertMisplacedLight(level, movedLight);
        }
        ACTIVE_HOTBAR_WRONG_COUNTS.remove(playerId);
        PetRefusalState petRefusal = ACTIVE_PET_REFUSALS.remove(playerId);
        if (petRefusal != null) {
            Entity raw = level.getEntity(petRefusal.petUuid());
            if (raw instanceof Mob pet) {
                pet.setNoAi(petRefusal.hadNoAi());
                pet.removeTag("eotv_pet_refusal");
                pet.removeTag("eotv_pet_refusal_black");
                removePetRefusalBlackTeam(level, pet);
                sendPetRefusalVisual(player, pet, false, 0);
            } else {
                PacketDistributor.sendToPlayer(player, new UncannyPetRefusalVisualPayload(-1, false, 0));
            }
        }
        LEVER_REPLY_TASKS.removeIf(task -> task.playerId().equals(playerId));
        DOOR_CASCADE_TASKS.removeIf(task -> task.playerId().equals(playerId));
        PRESSURE_PLATE_REPLY_TASKS.removeIf(task -> task.playerId().equals(playerId));
        TOOL_ANSWER_ECHO_TASKS.removeIf(task -> task.playerId().equals(playerId));
        LAST_TRIGGERED_PRESSURE_PLATE_TICKS.remove(playerId);
        LAST_TRIGGERED_PRESSURE_PLATE_POS.remove(playerId);
        LAST_CONTAINER_OPEN_TICKS.remove(playerId);
        LAST_CONTAINER_CONTEXTS.remove(playerId);
        LAST_TOOL_ANSWER_CONTEXT.remove(playerId);
        ACTIVE_TURN_AROUND_TRAPS.remove(playerId);
        LIVING_ORE_PRIMED.remove(playerId);
        ACTIVE_AQUATIC_BITE.remove(playerId);
        ACTIVE_SLEEP_DISTURBANCES.remove(playerId);
        PENDING_SLEEP_MESSAGE_TICKS.remove(playerId);
        SKIP_NEXT_SLEEP_DISTURB.remove(playerId);
        REQUIRE_NORMAL_SLEEP_BEFORE_NEXT_DISTURB.remove(playerId);
        NEXT_SLEEP_DISTURB_ALLOWED_TICKS.remove(playerId);
        NEXT_AUTO_CHECK_TICKS.remove(playerId);
        NEXT_SPECIAL_ENTITY_CHECK_TICKS.remove(playerId);
        FLASH_RED_OVERLAY_END_TICKS.remove(playerId);
        EVENT_COOLDOWNS.remove(playerId);
        AMBIENT_EVENT_COOLDOWNS.remove(playerId);
        SPECIAL_ENTITY_COOLDOWNS.remove(playerId);
        LAST_SPECIAL_ENTITY_EVENT_TICKS.remove(playerId);
        LAST_AMBIENT_EVENT_TICKS.remove(playerId);
        NEXT_LIVING_ORE_CHECK_TICKS.remove(playerId);
        LIVING_ORE_COOLDOWN_UNTIL.remove(playerId);
        TENANT_AWAY_SINCE.remove(playerId);
        GRAND_EVENT_RECENT_AUDIBLE_ACTION_TICKS.remove(playerId);
        for (GrandEventState state : ACTIVE_GRAND_EVENTS.values()) {
            state.trackedPlayers().remove(playerId);
            state.latchedPlayers().remove(playerId);
            state.removeLastKnownPosition(playerId);
            if (playerId.equals(state.attackTarget())) {
                state.clearAttack();
            }
        }
        player.removeTag(FLASH_RED_OVERLAY_TAG);
        player.removeTag(HUNTER_FOG_TAG);
        player.removeTag(GIANT_SUN_TAG);
        UncannyClientStateSync.syncParanoiaState(player, false, false);
    }

    private static void markGlobalCooldown(ServerPlayer player, long now) {
        markGlobalCooldown(player, now, EventSeverity.MEDIUM);
    }

    private static void markGlobalCooldown(ServerPlayer player, long now, EventSeverity severity) {
        if (player.getServer() == null) {
            return;
        }

        int profile = getIntensityProfile();
        int danger = getDangerLevel();
        UncannyPhase phase = UncannyWorldState.get(player.getServer()).getPhase();
        long softBlockTicks = rollEventCooldownTicks(player.serverLevel(), phase, profile, danger, severity);
        long nextTick = now + Math.max(20L, softBlockTicks / 2L);
        long existing = NEXT_AUTO_CHECK_TICKS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        NEXT_AUTO_CHECK_TICKS.put(player.getUUID(), Math.max(existing, nextTick));
    }

    private static boolean isNearBase(ServerPlayer player, MinecraftServer server) {
        BlockPos baseCenter = resolveBaseCenter(player, server);
        int radius = UncannyConfig.BASE_RADIUS_BLOCKS.get() + 8;
        return player.blockPosition().distSqr(baseCenter) <= (long) radius * radius;
    }

    private static boolean isInsideBase(ServerPlayer player, MinecraftServer server) {
        BlockPos baseCenter = resolveBaseCenter(player, server);
        int radius = Math.max(4, UncannyConfig.BASE_RADIUS_BLOCKS.get());
        return player.blockPosition().distSqr(baseCenter) <= (long) radius * radius;
    }

    private static void updateTenantAwayTracking(ServerPlayer player, long now, MinecraftServer server) {
        if (server == null) {
            return;
        }
        UUID playerId = player.getUUID();
        if (isNearBase(player, server)) {
            return;
        }
        TENANT_AWAY_SINCE.putIfAbsent(playerId, now);
    }

    private static boolean isNightOrTwilight(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= 12000L || dayTime <= 1300L;
    }

    private static BlockPos resolveBaseCenter(ServerPlayer player, MinecraftServer server) {
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null) {
            return respawn;
        }
        return server.overworld().getSharedSpawnPos();
    }

    private static final class CompassLiarState {
        private final long endTick;
        private final BlockPos target;

        private CompassLiarState(long endTick, BlockPos target) {
            this.endTick = endTick;
            this.target = target == null ? null : target.immutable();
        }

        private long endTick() {
            return endTick;
        }

        private BlockPos target() {
            return target;
        }
    }

    private record ContainerEchoContext(ResourceKey<Level> dimension, BlockPos sourcePos, SoundEvent sound, long tick) {
    }

    private record ToolAnswerContext(BlockPos minedPos, BlockState minedState, ItemStack toolStack, long tick, ResourceKey<Level> dimension) {
    }

    private record PausedSpecialSnapshot(UUID entityId, boolean hadNoAi, UUID targetId) {
    }

    private record MobSnapshot(UUID entityId, boolean hadNoAi) {
    }

    private static final class AnimalStareLockState {
        private final long endTick;
        private final List<MobSnapshot> affected;

        private AnimalStareLockState(long endTick, List<MobSnapshot> affected) {
            this.endTick = endTick;
            this.affected = List.copyOf(affected);
        }

        private long endTick() {
            return endTick;
        }

        private List<MobSnapshot> affected() {
            return affected;
        }
    }

    private static final class FurnaceBreathState {
        private final BlockPos source;
        private int remaining;
        private long nextTick;

        private FurnaceBreathState(BlockPos source, int remaining, long nextTick) {
            this.source = source.immutable();
            this.remaining = remaining;
            this.nextTick = nextTick;
        }

        private BlockPos source() {
            return source;
        }

        private int remaining() {
            return remaining;
        }

        private void decrementRepetitions() {
            this.remaining = Math.max(0, this.remaining - 1);
        }

        private long nextTick() {
            return nextTick;
        }

        private void setNextTick(long nextTick) {
            this.nextTick = nextTick;
        }
    }

    private static final class MisplacedLightState {
        private final BlockPos originalPos;
        private final BlockState originalState;
        private final BlockPos tempPos;
        private final BlockState tempState;
        private final long autoRevertTick;
        private boolean seenByPlayer;
        private int outOfViewTicks;

        private MisplacedLightState(
                BlockPos originalPos,
                BlockState originalState,
                BlockPos tempPos,
                BlockState tempState,
                long autoRevertTick) {
            this.originalPos = originalPos.immutable();
            this.originalState = originalState;
            this.tempPos = tempPos.immutable();
            this.tempState = tempState;
            this.autoRevertTick = autoRevertTick;
            this.seenByPlayer = false;
            this.outOfViewTicks = 0;
        }

        private BlockPos originalPos() {
            return originalPos;
        }

        private BlockState originalState() {
            return originalState;
        }

        private BlockPos tempPos() {
            return tempPos;
        }

        private BlockState tempState() {
            return tempState;
        }

        private long autoRevertTick() {
            return autoRevertTick;
        }

        private boolean seenByPlayer() {
            return seenByPlayer;
        }

        private void markSeenByPlayer() {
            this.seenByPlayer = true;
        }

        private int outOfViewTicks() {
            return outOfViewTicks;
        }

        private void resetOutOfViewTicks() {
            this.outOfViewTicks = 0;
        }

        private void incrementOutOfViewTicks() {
            this.outOfViewTicks++;
        }
    }

    private static final class PetRefusalState {
        private final UUID petUuid;
        private final boolean hadNoAi;
        private final long endTick;

        private PetRefusalState(UUID petUuid, boolean hadNoAi, long endTick) {
            this.petUuid = petUuid;
            this.hadNoAi = hadNoAi;
            this.endTick = endTick;
        }

        private UUID petUuid() {
            return petUuid;
        }

        private boolean hadNoAi() {
            return hadNoAi;
        }

        private long endTick() {
            return endTick;
        }
    }

    private static final class HotbarWrongCountState {
        private final int slot;
        private final int fakeCount;
        private final long endTick;
        private long nextEchoTick;

        private HotbarWrongCountState(int slot, int fakeCount, long endTick) {
            this.slot = slot;
            this.fakeCount = fakeCount;
            this.endTick = endTick;
            this.nextEchoTick = 0L;
        }

        private int slot() {
            return slot;
        }

        private int fakeCount() {
            return fakeCount;
        }

        private long endTick() {
            return endTick;
        }

        private long nextEchoTick() {
            return nextEchoTick;
        }

        private void setNextEchoTick(long nextEchoTick) {
            this.nextEchoTick = nextEchoTick;
        }
    }

    private static final class LeverReplyTask {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final long fireTick;
        private final boolean audioOnly;

        private LeverReplyTask(UUID playerId, ResourceKey<Level> dimension, BlockPos pos, long fireTick, boolean audioOnly) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.fireTick = fireTick;
            this.audioOnly = audioOnly;
        }

        private UUID playerId() {
            return playerId;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private BlockPos pos() {
            return pos;
        }

        private long fireTick() {
            return fireTick;
        }

        private boolean audioOnly() {
            return audioOnly;
        }
    }

    private static final class ToolAnswerEchoTask {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos minedPos;
        private final BlockState minedState;
        private final ItemStack toolStack;
        private final long fireTick;

        private ToolAnswerEchoTask(
                UUID playerId,
                ResourceKey<Level> dimension,
                BlockPos minedPos,
                BlockState minedState,
                ItemStack toolStack,
                long fireTick) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.minedPos = minedPos.immutable();
            this.minedState = minedState;
            this.toolStack = toolStack == null ? ItemStack.EMPTY : toolStack.copy();
            this.fireTick = fireTick;
        }

        private UUID playerId() {
            return playerId;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private BlockPos minedPos() {
            return minedPos;
        }

        private BlockState minedState() {
            return minedState;
        }

        private ItemStack toolStack() {
            return toolStack;
        }

        private long fireTick() {
            return fireTick;
        }
    }

    private static final class DoorCascadeTask {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final List<BlockPos> positions;
        private long nextTick;
        private int index;

        private DoorCascadeTask(UUID playerId, ResourceKey<Level> dimension, List<BlockPos> positions, long nextTick, int index) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.positions = positions;
            this.nextTick = nextTick;
            this.index = index;
        }

        private UUID playerId() {
            return playerId;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private List<BlockPos> positions() {
            return positions;
        }

        private long nextTick() {
            return nextTick;
        }

        private int index() {
            return index;
        }

        private void advance(long nextTick) {
            this.index++;
            this.nextTick = nextTick;
        }
    }

    private static final class PressurePlateReplyTask {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private long fireTick;
        private boolean resetPending;

        private PressurePlateReplyTask(UUID playerId, ResourceKey<Level> dimension, BlockPos pos, long fireTick, boolean resetPending) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.fireTick = fireTick;
            this.resetPending = resetPending;
        }

        private UUID playerId() {
            return playerId;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private BlockPos pos() {
            return pos;
        }

        private long fireTick() {
            return fireTick;
        }

        private boolean resetPending() {
            return resetPending;
        }

        private void scheduleReset(long tick) {
            this.resetPending = true;
            this.fireTick = tick;
        }
    }

    private record SpawnDistanceWindow(int minDistance, int maxDistance) {
    }

    private record EventChoice(String key, int weight) {
    }

    private enum EventSeverity {
        LIGHT,
        MEDIUM,
        HIGH,
        EXTREME
    }

    private enum FootstepPattern {
        BASIC,
        ECHO,
        SPRINT,
        HEAVY,
        LADDER_STEPS
    }

    private static final class BlackoutState {
        private final long startTick;
        private final long durationTicks;
        private boolean specialChecked;

        private BlackoutState(long startTick, long durationTicks) {
            this.startTick = startTick;
            this.durationTicks = durationTicks;
        }

        private long startTick() {
            return this.startTick;
        }

        private long durationTicks() {
            return this.durationTicks;
        }

        private boolean specialChecked() {
            return this.specialChecked;
        }

        private void setSpecialChecked(boolean specialChecked) {
            this.specialChecked = specialChecked;
        }
    }

    private static final class FootstepsState {
        private final FootstepPattern pattern;
        private final long endTick;
        private long nextStepTick;
        private double sprintDistance;
        private final BlockPos anchorPos;

        private FootstepsState(FootstepPattern pattern, long endTick, long nextStepTick, double sprintDistance, BlockPos anchorPos) {
            this.pattern = pattern;
            this.endTick = endTick;
            this.nextStepTick = nextStepTick;
            this.sprintDistance = sprintDistance;
            this.anchorPos = anchorPos;
        }

        private FootstepPattern pattern() {
            return pattern;
        }

        private long endTick() {
            return endTick;
        }

        private long nextStepTick() {
            return nextStepTick;
        }

        private void setNextStepTick(long nextStepTick) {
            this.nextStepTick = nextStepTick;
        }

        private double sprintDistance() {
            return sprintDistance;
        }

        private void setSprintDistance(double sprintDistance) {
            this.sprintDistance = sprintDistance;
        }

        private BlockPos anchorPos() {
            return anchorPos;
        }
    }

    private static final class VoidSilenceState {
        private final long endTick;
        private long nextRingTick;

        private VoidSilenceState(long endTick, long nextRingTick) {
            this.endTick = endTick;
            this.nextRingTick = nextRingTick;
        }

        private long endTick() {
            return endTick;
        }

        private long nextRingTick() {
            return nextRingTick;
        }

        private void setNextRingTick(long nextRingTick) {
            this.nextRingTick = nextRingTick;
        }
    }

    private static final class GhostMinerState {
        private BlockPos wallPos;
        private final long endTick;
        private long nextHitTick;
        private long pendingPickupTick;
        private BlockPos pendingPickupPos;
        private float orbitAngleDegrees;
        private double orbitRadius;

        private GhostMinerState(
                BlockPos wallPos,
                long endTick,
                long nextHitTick,
                long pendingPickupTick,
                BlockPos pendingPickupPos,
                float orbitAngleDegrees,
                double orbitRadius) {
            this.wallPos = wallPos.immutable();
            this.endTick = endTick;
            this.nextHitTick = nextHitTick;
            this.pendingPickupTick = pendingPickupTick;
            this.pendingPickupPos = pendingPickupPos.immutable();
            this.orbitAngleDegrees = orbitAngleDegrees;
            this.orbitRadius = orbitRadius;
        }

        private BlockPos wallPos() {
            return wallPos;
        }

        private void setWallPos(BlockPos wallPos) {
            this.wallPos = wallPos.immutable();
        }

        private long endTick() {
            return endTick;
        }

        private long nextHitTick() {
            return nextHitTick;
        }

        private void setNextHitTick(long nextHitTick) {
            this.nextHitTick = nextHitTick;
        }

        private long pendingPickupTick() {
            return pendingPickupTick;
        }

        private BlockPos pendingPickupPos() {
            return pendingPickupPos;
        }

        private void setPendingPickup(BlockPos pendingPickupPos, long pendingPickupTick) {
            this.pendingPickupPos = pendingPickupPos.immutable();
            this.pendingPickupTick = pendingPickupTick;
        }

        private void clearPendingPickup() {
            this.pendingPickupTick = -1L;
        }

        private float orbitAngleDegrees() {
            return orbitAngleDegrees;
        }

        private void setOrbitAngleDegrees(float orbitAngleDegrees) {
            this.orbitAngleDegrees = orbitAngleDegrees;
        }

        private double orbitRadius() {
            return orbitRadius;
        }

        private void setOrbitRadius(double orbitRadius) {
            this.orbitRadius = orbitRadius;
        }
    }

    private static final class FlashErrorState {
        private long nextSpawnTick;
        private UUID stalkerUuid;
        private long spawnedTick;
        private final boolean shouldSpawnMonster;

        private FlashErrorState(long nextSpawnTick, boolean shouldSpawnMonster) {
            this.nextSpawnTick = nextSpawnTick;
            this.shouldSpawnMonster = shouldSpawnMonster;
        }

        private long nextSpawnTick() {
            return this.nextSpawnTick;
        }

        private void setNextSpawnTick(long nextSpawnTick) {
            this.nextSpawnTick = nextSpawnTick;
        }

        private UUID stalkerUuid() {
            return this.stalkerUuid;
        }

        private void setStalkerUuid(UUID stalkerUuid) {
            this.stalkerUuid = stalkerUuid;
        }

        private long spawnedTick() {
            return this.spawnedTick;
        }

        private void setSpawnedTick(long spawnedTick) {
            this.spawnedTick = spawnedTick;
        }

        private boolean shouldSpawnMonster() {
            return this.shouldSpawnMonster;
        }

        private void clearStalker() {
            this.stalkerUuid = null;
            this.spawnedTick = 0L;
        }
    }

    private static final class SleepDisturbanceState {
        private BlockPos bedPos;
        private int attempts;

        private SleepDisturbanceState(BlockPos bedPos, int attempts) {
            this.bedPos = bedPos.immutable();
            this.attempts = attempts;
        }

        private BlockPos bedPos() {
            return this.bedPos;
        }

        private void setBedPos(BlockPos bedPos) {
            this.bedPos = bedPos.immutable();
        }

        private int attempts() {
            return this.attempts;
        }

        private void incrementAttempts() {
            this.attempts++;
        }
    }

    private record ChestCloseTask(UUID playerId, net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos, long closeTick) {
    }

    private record ChestPanicTask(UUID playerId, net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos, long toggleTick, boolean open) {
    }

    private record FurnaceResetTask(UUID playerId, net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos, long resetTick) {
    }

    private enum AsphyxiaVariant {
        FALSE_ALERT,
        TERRAIN_DROWNING,
        HEAVY_LUNGS
    }

    private enum ArmorBreakVariant {
        GHOST_SOUND,
        DROP_GEAR,
        CRACKED_DEFENSE
    }

    private enum AquaticStepsVariant {
        FOLLOWER,
        SLIPPERY_AMBUSH,
        INVISIBLE_BITE
    }

    private enum DoorInversionVariant {
        POLTERGEIST,
        LOCKDOWN,
        INTRUSION,
        DOOR_TRAP_CASCADE
    }

    private enum PhantomHarvestVariant {
        BLACK_HARVEST,
        ROTTEN_SOIL,
        INFESTATION
    }

    private enum LivingOreVariant {
        BLEEDING,
        TOXIC_BLOOD,
        VICIOUS_FALL,
        VEIN_RETREAT,
        INSIDE_KNOCK
    }

    private enum ProjectedShadowVariant {
        MIME,
        SHADOW_ASSAULT,
        GHOST_SHOT
    }

    private static final class GrandEventSearchResolution {
        private final BlockPos selectedNode;
        private final double selectedScore;
        private final int selectedSector;
        private final int poolSize;
        private final int reachableCount;
        private final int noPathCount;
        private final int contextRejectCount;
        private final int recentRejectCount;
        private final int playerDistanceRejectCount;

        private GrandEventSearchResolution(
                BlockPos selectedNode,
                double selectedScore,
                int selectedSector,
                int poolSize,
                int reachableCount,
                int noPathCount,
                int contextRejectCount,
                int recentRejectCount,
                int playerDistanceRejectCount) {
            this.selectedNode = selectedNode;
            this.selectedScore = selectedScore;
            this.selectedSector = selectedSector;
            this.poolSize = poolSize;
            this.reachableCount = reachableCount;
            this.noPathCount = noPathCount;
            this.contextRejectCount = contextRejectCount;
            this.recentRejectCount = recentRejectCount;
            this.playerDistanceRejectCount = playerDistanceRejectCount;
        }

        private static GrandEventSearchResolution empty() {
            return new GrandEventSearchResolution(null, Double.NEGATIVE_INFINITY, -1, 0, 0, 0, 0, 0, 0);
        }

        private BlockPos selectedNode() {
            return this.selectedNode;
        }

        private double selectedScore() {
            return this.selectedScore;
        }

        private int selectedSector() {
            return this.selectedSector;
        }

        private int poolSize() {
            return this.poolSize;
        }

        private int reachableCount() {
            return this.reachableCount;
        }

        private int noPathCount() {
            return this.noPathCount;
        }

        private int contextRejectCount() {
            return this.contextRejectCount;
        }

        private int recentRejectCount() {
            return this.recentRejectCount;
        }

        private int playerDistanceRejectCount() {
            return this.playerDistanceRejectCount;
        }
    }

    private static final class GrandEventState {
        private final String runtimeId;
        private final String buildSignature;
        private final long startedTick;
        private UUID wardenUuid;
        private final BlockPos anchorPos;
        private final boolean coveredAtStart;
        private final Set<UUID> trackedPlayers;
        private final Set<UUID> latchedPlayers;
        private final Map<UUID, Vec3> lastKnownPositions;
        private final Map<UUID, Double> movementAccumulators;
        private final Map<UUID, Long> movementLastSampleTicks;
        private long lastProcessedTick;
        private int messageIndex;
        private long nextMessageTick;
        private long nextHeavySoundTick;
        private final Set<UUID> sniffedPlayersThisCycle;
        private UUID sniffPendingPlayerId;
        private long sniffPendingUntilTick;
        private long sniffPoseUntilTick;
        private long nextSniffSoundTick;
        private boolean emerging;
        private long emergeEndTick;
        private UUID attackTarget;
        private String lastAdmittedTrigger;
        private long attackStartTick;
        private long lastCloseEncounterTick;
        private boolean closeContactActive;
        private long lastHardContactGuardTick;
        private boolean exiting;
        private boolean sinking;
        private long sinkEndTick;
        private boolean sinkDigSoundPlayed;
        private int unseenTicks;
        private UUID searchFocusId;
        private long searchEndTick;
        private long nextCrossTick;
        private long crossEndTick;
        private float searchAngleDegrees;
        private BlockPos lastIssuedNode;
        private long lastIssuedTick;
        private String lastIssuedReason;
        private boolean lastIssuedByFlow;
        private BlockPos pendingIssuedNode;
        private long pendingIssuedTick;
        private String pendingIssuedReason;
        private Vec3 pendingIssuedOriginPos;
        private double pendingInitialDistanceSqr;
        private double pendingBestDistanceSqr;
        private double pendingCurrentDistanceSqr;
        private long pendingBestProgressTick;
        private long pendingNavIdleSinceTick;
        private long pendingCantReachSinceTick;
        private float lastObservedYaw;
        private long spinInPlaceTicks;
        private long nonAggroGuardLockoutUntilTick;
        private long lastIssueFailureTick;
        private long lastIntentConsumedTick;
        private int totalConsumedSearchNodes;
        private int spatiallyValidConsumedSearchNodes;
        private BlockPos lastConsumedSearchNode;
        private int lastConsumedSearchMicroZone;
        private int consecutiveMicroLoopNodes;
        private long lastIntentNotConsumedLogTick;
        private long lastIntentOverwrittenLogTick;
        private Vec3 nonAggroLastMobilityPos;
        private Vec3 nonAggro5sSamplePos;
        private long nonAggro5sSampleTick;
        private Vec3 nonAggro10sSamplePos;
        private long nonAggro10sSampleTick;
        private double nonAggroLastCompletedMoved5s;
        private double nonAggroLastCompletedMoved10s;
        private long nonAggroLastCompletedMoved5sTick;
        private long nonAggroLastCompletedMoved10sTick;
        private long nonAggroLastSignificantProgressTick;
        private final Deque<Long> searchNodeIssuedTicks;
        private final Deque<Long> replanIssueTicks;
        private final Deque<Long> nearRepeatIssueTicks;
        private long replanCooldownUntilTick;
        private final Deque<BlockPos> recentSearchNodes;
        private final Deque<Long> consumedSearchNodeTicks;
        private final Deque<BlockPos> consumedSearchNodes;
        private final Deque<Integer> consumedSearchMicroZones;
        private final int[] searchSectorVisitCounts;
        private final Map<Integer, Integer> searchMicroZoneVisitCounts;
        private final Deque<Integer> recentSearchSectors;
        private int lastSearchSector;
        private int searchNoPathStreak;
        private boolean searchRecoveryModeActive;
        private boolean relaxedRecentModeActive;
        private int recentOverlapStallStreak;
        private long lastSearchHistoryResetTick;
        private long caveBreakNoIntentSinceTick;
        private boolean caveBreakStuckBoostActive;
        private String lastCaveBreakDirectionSource;
        private long lastCaveBreakDirectionTick;
        private BlockPos caveExitRetreatNode;
        private long caveExitRetargetTick;
        private long caveExitStartTick;
        private int caveExitRetreatAttempts;
        private long roarSniffStuckSinceTick;
        private boolean aggroTuningActive;
        private Float waterPathMalusBeforeAggro;
        private Boolean canFloatBeforeAggro;
        private long lastSonicAssistTick;
        private int recoveryCount;
        private boolean ended;

        private GrandEventState(long startedTick, UUID wardenUuid, BlockPos anchorPos, Set<UUID> trackedPlayers, boolean coveredAtStart) {
            String fullRuntimeId = UUID.randomUUID().toString();
            this.runtimeId = fullRuntimeId.length() > 8 ? fullRuntimeId.substring(0, 8) : fullRuntimeId;
            this.buildSignature = GRAND_EVENT_RUNTIME_BUILD_SIGNATURE;
            this.startedTick = startedTick;
            this.wardenUuid = wardenUuid;
            this.anchorPos = anchorPos.immutable();
            this.coveredAtStart = coveredAtStart;
            this.trackedPlayers = new HashSet<>(trackedPlayers);
            this.latchedPlayers = new HashSet<>();
            this.lastKnownPositions = new HashMap<>();
            this.movementAccumulators = new HashMap<>();
            this.movementLastSampleTicks = new HashMap<>();
            this.lastProcessedTick = Long.MIN_VALUE;
            this.messageIndex = 0;
            this.nextMessageTick = startedTick;
            this.nextHeavySoundTick = startedTick;
            this.sniffedPlayersThisCycle = new HashSet<>();
            this.sniffPendingPlayerId = null;
            this.sniffPendingUntilTick = Long.MIN_VALUE;
            this.sniffPoseUntilTick = Long.MIN_VALUE;
            this.nextSniffSoundTick = Long.MIN_VALUE;
            this.emerging = false;
            this.emergeEndTick = Long.MIN_VALUE;
            this.attackTarget = null;
            this.lastAdmittedTrigger = "none";
            this.attackStartTick = Long.MIN_VALUE;
            this.lastCloseEncounterTick = Long.MIN_VALUE;
            this.closeContactActive = false;
            this.lastHardContactGuardTick = Long.MIN_VALUE;
            this.exiting = false;
            this.sinking = false;
            this.sinkEndTick = Long.MIN_VALUE;
            this.sinkDigSoundPlayed = false;
            this.unseenTicks = 0;
            this.searchFocusId = null;
            this.searchEndTick = Long.MIN_VALUE;
            this.nextCrossTick = Long.MIN_VALUE;
            this.crossEndTick = Long.MIN_VALUE;
            this.searchAngleDegrees = 0.0F;
            this.lastIssuedNode = null;
            this.lastIssuedTick = Long.MIN_VALUE;
            this.lastIssuedReason = "none";
            this.lastIssuedByFlow = false;
            this.pendingIssuedNode = null;
            this.pendingIssuedTick = Long.MIN_VALUE;
            this.pendingIssuedReason = "none";
            this.pendingIssuedOriginPos = null;
            this.pendingInitialDistanceSqr = Double.NaN;
            this.pendingBestDistanceSqr = Double.NaN;
            this.pendingCurrentDistanceSqr = Double.NaN;
            this.pendingBestProgressTick = Long.MIN_VALUE;
            this.pendingNavIdleSinceTick = Long.MIN_VALUE;
            this.pendingCantReachSinceTick = Long.MIN_VALUE;
            this.lastObservedYaw = Float.NaN;
            this.spinInPlaceTicks = 0L;
            this.nonAggroGuardLockoutUntilTick = Long.MIN_VALUE;
            this.lastIssueFailureTick = Long.MIN_VALUE;
            this.lastIntentConsumedTick = Long.MIN_VALUE;
            this.totalConsumedSearchNodes = 0;
            this.spatiallyValidConsumedSearchNodes = 0;
            this.lastConsumedSearchNode = null;
            this.lastConsumedSearchMicroZone = -1;
            this.consecutiveMicroLoopNodes = 0;
            this.lastIntentNotConsumedLogTick = Long.MIN_VALUE;
            this.lastIntentOverwrittenLogTick = Long.MIN_VALUE;
            this.nonAggroLastMobilityPos = null;
            this.nonAggro5sSamplePos = null;
            this.nonAggro5sSampleTick = Long.MIN_VALUE;
            this.nonAggro10sSamplePos = null;
            this.nonAggro10sSampleTick = Long.MIN_VALUE;
            this.nonAggroLastCompletedMoved5s = 0.0D;
            this.nonAggroLastCompletedMoved10s = 0.0D;
            this.nonAggroLastCompletedMoved5sTick = Long.MIN_VALUE;
            this.nonAggroLastCompletedMoved10sTick = Long.MIN_VALUE;
            this.nonAggroLastSignificantProgressTick = Long.MIN_VALUE;
            this.searchNodeIssuedTicks = new ArrayDeque<>();
            this.replanIssueTicks = new ArrayDeque<>();
            this.nearRepeatIssueTicks = new ArrayDeque<>();
            this.replanCooldownUntilTick = Long.MIN_VALUE;
            this.recentSearchNodes = new ArrayDeque<>();
            this.consumedSearchNodeTicks = new ArrayDeque<>();
            this.consumedSearchNodes = new ArrayDeque<>();
            this.consumedSearchMicroZones = new ArrayDeque<>();
            this.searchSectorVisitCounts = new int[GRAND_EVENT_SEARCH_SECTOR_COUNT];
            this.searchMicroZoneVisitCounts = new HashMap<>();
            this.recentSearchSectors = new ArrayDeque<>();
            this.lastSearchSector = -1;
            this.searchNoPathStreak = 0;
            this.searchRecoveryModeActive = false;
            this.relaxedRecentModeActive = false;
            this.recentOverlapStallStreak = 0;
            this.lastSearchHistoryResetTick = Long.MIN_VALUE;
            this.caveBreakNoIntentSinceTick = Long.MIN_VALUE;
            this.caveBreakStuckBoostActive = false;
            this.lastCaveBreakDirectionSource = "none";
            this.lastCaveBreakDirectionTick = Long.MIN_VALUE;
            this.caveExitRetreatNode = null;
            this.caveExitRetargetTick = Long.MIN_VALUE;
            this.caveExitStartTick = Long.MIN_VALUE;
            this.caveExitRetreatAttempts = 0;
            this.roarSniffStuckSinceTick = Long.MIN_VALUE;
            this.aggroTuningActive = false;
            this.waterPathMalusBeforeAggro = null;
            this.canFloatBeforeAggro = null;
            this.lastSonicAssistTick = Long.MIN_VALUE;
            this.recoveryCount = 0;
            this.ended = false;
        }

        private String runtimeId() {
            return runtimeId;
        }

        private String buildSignature() {
            return buildSignature;
        }

        private long startedTick() {
            return startedTick;
        }

        private UUID wardenUuid() {
            return wardenUuid;
        }

        private void setWardenUuid(UUID wardenUuid) {
            if (wardenUuid != null) {
                this.wardenUuid = wardenUuid;
            }
        }

        private BlockPos anchorPos() {
            return anchorPos;
        }

        private boolean coveredAtStart() {
            return coveredAtStart;
        }

        private Set<UUID> trackedPlayers() {
            return trackedPlayers;
        }

        private Set<UUID> latchedPlayers() {
            return latchedPlayers;
        }

        private void markLatched(UUID playerId) {
            if (playerId != null) {
                this.latchedPlayers.add(playerId);
                if (playerId.equals(this.searchFocusId)) {
                    clearSearchFocus();
                }
            }
        }

        private boolean allTrackedPlayersLatched(ServerLevel level) {
            for (UUID playerId : trackedPlayers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
                if (getGrandEventScopeRejectReason(level, this, player) != null) {
                    continue;
                }
                if (!latchedPlayers.contains(playerId)) {
                    return false;
                }
            }
            return true;
        }

        private long lastProcessedTick() {
            return lastProcessedTick;
        }

        private void setLastProcessedTick(long lastProcessedTick) {
            this.lastProcessedTick = lastProcessedTick;
        }

        private int messageIndex() {
            return messageIndex;
        }

        private void setMessageIndex(int messageIndex) {
            this.messageIndex = messageIndex;
        }

        private long nextMessageTick() {
            return nextMessageTick;
        }

        private void setNextMessageTick(long nextMessageTick) {
            this.nextMessageTick = nextMessageTick;
        }

        private long nextHeavySoundTick() {
            return nextHeavySoundTick;
        }

        private void setNextHeavySoundTick(long nextHeavySoundTick) {
            this.nextHeavySoundTick = nextHeavySoundTick;
        }

        private UUID attackTarget() {
            return attackTarget;
        }

        private void setAttackTarget(UUID attackTarget, long now) {
            this.attackTarget = attackTarget;
            this.attackStartTick = now;
        }

        private String lastAdmittedTrigger() {
            return this.lastAdmittedTrigger;
        }

        private void setLastAdmittedTrigger(String trigger) {
            this.lastAdmittedTrigger = trigger == null ? "none" : trigger;
        }

        private long attackStartTick() {
            return attackStartTick;
        }

        private void clearAttack() {
            this.attackTarget = null;
            this.lastAdmittedTrigger = "none";
            this.attackStartTick = Long.MIN_VALUE;
            this.roarSniffStuckSinceTick = Long.MIN_VALUE;
            this.closeContactActive = false;
        }

        private long lastCloseEncounterTick() {
            return this.lastCloseEncounterTick;
        }

        private void setLastCloseEncounterTick(long tick) {
            this.lastCloseEncounterTick = tick;
        }

        private boolean closeContactActive() {
            return this.closeContactActive;
        }

        private void setCloseContactActive(boolean active) {
            this.closeContactActive = active;
        }

        private long lastHardContactGuardTick() {
            return this.lastHardContactGuardTick;
        }

        private void setLastHardContactGuardTick(long tick) {
            this.lastHardContactGuardTick = tick;
        }

        private boolean exiting() {
            return exiting;
        }

        private void setExiting(boolean exiting) {
            this.exiting = exiting;
        }

        private boolean sinking() {
            return sinking;
        }

        private void setSinking(boolean sinking) {
            this.sinking = sinking;
        }

        private long sinkEndTick() {
            return sinkEndTick;
        }

        private void setSinkEndTick(long sinkEndTick) {
            this.sinkEndTick = sinkEndTick;
        }

        private boolean sinkDigSoundPlayed() {
            return this.sinkDigSoundPlayed;
        }

        private void setSinkDigSoundPlayed(boolean sinkDigSoundPlayed) {
            this.sinkDigSoundPlayed = sinkDigSoundPlayed;
        }

        private int unseenTicks() {
            return unseenTicks;
        }

        private void setUnseenTicks(int unseenTicks) {
            this.unseenTicks = unseenTicks;
        }

        private UUID searchFocusId() {
            return searchFocusId;
        }

        private void setSearchFocusId(UUID searchFocusId) {
            this.searchFocusId = searchFocusId;
        }

        private long searchEndTick() {
            return searchEndTick;
        }

        private void setSearchEndTick(long searchEndTick) {
            this.searchEndTick = searchEndTick;
        }

        private long nextCrossTick() {
            return nextCrossTick;
        }

        private void setNextCrossTick(long nextCrossTick) {
            this.nextCrossTick = nextCrossTick;
        }

        private long crossEndTick() {
            return crossEndTick;
        }

        private void setCrossEndTick(long crossEndTick) {
            this.crossEndTick = crossEndTick;
        }

        private float searchAngleDegrees() {
            return searchAngleDegrees;
        }

        private void setSearchAngleDegrees(float searchAngleDegrees) {
            this.searchAngleDegrees = searchAngleDegrees;
        }

        private int recoveryCount() {
            return recoveryCount;
        }

        private void incrementRecoveryCount() {
            this.recoveryCount++;
        }

        private void clearSearchFocus() {
            this.searchFocusId = null;
            this.searchEndTick = Long.MIN_VALUE;
            this.nextCrossTick = Long.MIN_VALUE;
            this.crossEndTick = Long.MIN_VALUE;
            this.searchNoPathStreak = 0;
            this.searchRecoveryModeActive = false;
            this.relaxedRecentModeActive = false;
            this.recentOverlapStallStreak = 0;
            this.caveBreakNoIntentSinceTick = Long.MIN_VALUE;
            this.caveBreakStuckBoostActive = false;
            this.roarSniffStuckSinceTick = Long.MIN_VALUE;
            this.closeContactActive = false;
            resetSearchSectorCoverage();
            this.sniffPendingPlayerId = null;
            this.sniffPendingUntilTick = Long.MIN_VALUE;
            this.sniffPoseUntilTick = Long.MIN_VALUE;
            this.nextSniffSoundTick = Long.MIN_VALUE;
            this.nonAggroGuardLockoutUntilTick = Long.MIN_VALUE;
            this.clearPendingIssuedIntent();
            this.resetNonAggroMobility();
            this.searchNodeIssuedTicks.clear();
            this.replanIssueTicks.clear();
            this.nearRepeatIssueTicks.clear();
            this.replanCooldownUntilTick = Long.MIN_VALUE;
        }

        private BlockPos lastIssuedNode() {
            return this.lastIssuedNode;
        }

        private long lastIssuedTick() {
            return this.lastIssuedTick;
        }

        private String lastIssuedReason() {
            return this.lastIssuedReason;
        }

        private boolean lastIssuedByFlow() {
            return this.lastIssuedByFlow;
        }

        private void markIssuedIntent(BlockPos node, long tick, String reason) {
            markIssuedIntent(node, tick, reason, null);
        }

        private void markIssuedIntent(BlockPos node, long tick, String reason, Vec3 issuedOriginPos) {
            if (node == null) {
                return;
            }
            BlockPos previousIssued = this.lastIssuedNode;
            this.lastIssuedNode = node.immutable();
            this.lastIssuedTick = tick;
            this.lastIssuedReason = reason == null ? "unknown" : reason;
            this.lastIssuedByFlow = true;
            this.pendingIssuedNode = this.lastIssuedNode;
            this.pendingIssuedTick = tick;
            this.pendingIssuedReason = this.lastIssuedReason;
            this.pendingIssuedOriginPos = issuedOriginPos == null ? null : new Vec3(issuedOriginPos.x, issuedOriginPos.y, issuedOriginPos.z);
            if (this.pendingIssuedOriginPos != null) {
                Vec3 pendingCenter = Vec3.atCenterOf(this.pendingIssuedNode);
                this.pendingInitialDistanceSqr = horizontalDistanceSqr(this.pendingIssuedOriginPos, pendingCenter);
                this.pendingBestDistanceSqr = this.pendingInitialDistanceSqr;
                this.pendingCurrentDistanceSqr = this.pendingInitialDistanceSqr;
                this.pendingBestProgressTick = tick;
            } else {
                this.pendingInitialDistanceSqr = Double.NaN;
                this.pendingBestDistanceSqr = Double.NaN;
                this.pendingCurrentDistanceSqr = Double.NaN;
                this.pendingBestProgressTick = tick;
            }
            this.pendingNavIdleSinceTick = Long.MIN_VALUE;
            this.pendingCantReachSinceTick = Long.MIN_VALUE;
            this.lastIssueFailureTick = Long.MIN_VALUE;
            this.lastIntentNotConsumedLogTick = Long.MIN_VALUE;
            this.lastIntentOverwrittenLogTick = Long.MIN_VALUE;
            recordReplanIssueTick(tick, previousIssued, this.pendingIssuedNode);
        }

        private void clearIssuedIntent() {
            this.lastIssuedNode = null;
            this.lastIssuedTick = Long.MIN_VALUE;
            this.lastIssuedReason = "none";
            this.lastIssuedByFlow = false;
            this.lastIssueFailureTick = Long.MIN_VALUE;
            this.lastIntentNotConsumedLogTick = Long.MIN_VALUE;
            this.lastIntentOverwrittenLogTick = Long.MIN_VALUE;
            this.lastIntentConsumedTick = Long.MIN_VALUE;
            this.clearPendingIssuedIntent();
        }

        private void markIssueFailure(long tick) {
            this.lastIssueFailureTick = tick;
        }

        private long lastIssueFailureTick() {
            return this.lastIssueFailureTick;
        }

        private long lastIntentConsumedTick() {
            return this.lastIntentConsumedTick;
        }

        private void setLastIntentConsumedTick(long tick) {
            this.lastIntentConsumedTick = tick;
        }

        private long lastIntentNotConsumedLogTick() {
            return this.lastIntentNotConsumedLogTick;
        }

        private void setLastIntentNotConsumedLogTick(long tick) {
            this.lastIntentNotConsumedLogTick = tick;
        }

        private long lastIntentOverwrittenLogTick() {
            return this.lastIntentOverwrittenLogTick;
        }

        private void setLastIntentOverwrittenLogTick(long tick) {
            this.lastIntentOverwrittenLogTick = tick;
        }

        private BlockPos pendingIssuedNode() {
            return this.pendingIssuedNode;
        }

        private long pendingIssuedTick() {
            return this.pendingIssuedTick;
        }

        private Vec3 pendingIssuedOriginPos() {
            return this.pendingIssuedOriginPos;
        }

        private String pendingIssuedReason() {
            return this.pendingIssuedReason;
        }

        private boolean hasPendingIssuedNode() {
            return this.pendingIssuedNode != null && this.pendingIssuedTick != Long.MIN_VALUE;
        }

        private void clearPendingIssuedIntent() {
            this.pendingIssuedNode = null;
            this.pendingIssuedTick = Long.MIN_VALUE;
            this.pendingIssuedReason = "none";
            this.pendingIssuedOriginPos = null;
            this.pendingInitialDistanceSqr = Double.NaN;
            this.pendingBestDistanceSqr = Double.NaN;
            this.pendingCurrentDistanceSqr = Double.NaN;
            this.pendingBestProgressTick = Long.MIN_VALUE;
            this.pendingNavIdleSinceTick = Long.MIN_VALUE;
            this.pendingCantReachSinceTick = Long.MIN_VALUE;
        }

        private double markIntentConsumed(long now, BlockPos consumedNode, boolean reachedNode, double movedFromIssue) {
            double stepFromLast = 0.0D;
            boolean spatiallyValid = false;
            if (consumedNode != null) {
                if (this.lastConsumedSearchNode != null) {
                    stepFromLast = Math.sqrt(this.lastConsumedSearchNode.distSqr(consumedNode));
                }
                rememberSearchNode(consumedNode);
                rememberConsumedCoverageNode(consumedNode, now);
                spatiallyValid = reachedNode
                        || stepFromLast >= GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_BLOCKS
                        || movedFromIssue >= GRAND_EVENT_INTENT_CONSUME_MIN_MOVE_BLOCKS;
            }
            clearPendingIssuedIntent();
            setLastIntentConsumedTick(now);
            this.totalConsumedSearchNodes++;
            if (spatiallyValid) {
                this.spatiallyValidConsumedSearchNodes++;
            }
            return stepFromLast;
        }

        private int totalConsumedSearchNodes() {
            return this.totalConsumedSearchNodes;
        }

        private int spatiallyValidConsumedSearchNodes() {
            return this.spatiallyValidConsumedSearchNodes;
        }

        private long nonAggroGuardLockoutUntilTick() {
            return this.nonAggroGuardLockoutUntilTick;
        }

        private void setNonAggroGuardLockoutUntilTick(long tick) {
            this.nonAggroGuardLockoutUntilTick = tick;
        }

        private boolean isNonAggroGuardLockoutActive(long now) {
            return this.nonAggroGuardLockoutUntilTick != Long.MIN_VALUE && now < this.nonAggroGuardLockoutUntilTick;
        }

        private void sampleNonAggroMobility(Vec3 currentPos, long now) {
            if (currentPos == null) {
                return;
            }
            if (this.nonAggroLastMobilityPos == null) {
                this.nonAggroLastMobilityPos = currentPos;
                this.nonAggro5sSamplePos = currentPos;
                this.nonAggro5sSampleTick = now;
                this.nonAggro10sSamplePos = currentPos;
                this.nonAggro10sSampleTick = now;
                this.nonAggroLastSignificantProgressTick = now;
                return;
            }

            double step = Math.sqrt(horizontalDistanceSqr(this.nonAggroLastMobilityPos, currentPos));
            this.nonAggroLastMobilityPos = currentPos;
            if (step >= GRAND_EVENT_NON_AGGRO_SIGNIFICANT_PROGRESS_BLOCKS) {
                this.nonAggroLastSignificantProgressTick = now;
            }

            if (this.nonAggro5sSamplePos == null) {
                this.nonAggro5sSamplePos = currentPos;
                this.nonAggro5sSampleTick = now;
            } else {
                this.nonAggroLastCompletedMoved5s = Math.sqrt(horizontalDistanceSqr(this.nonAggro5sSamplePos, currentPos));
                if (this.nonAggro5sSampleTick != Long.MIN_VALUE
                        && (now - this.nonAggro5sSampleTick) >= GRAND_EVENT_NON_AGGRO_STAGNATION_SAMPLE_5S_TICKS) {
                    this.nonAggroLastCompletedMoved5sTick = now;
                    this.nonAggro5sSamplePos = currentPos;
                    this.nonAggro5sSampleTick = now;
                }
            }

            if (this.nonAggro10sSamplePos == null) {
                this.nonAggro10sSamplePos = currentPos;
                this.nonAggro10sSampleTick = now;
            } else {
                this.nonAggroLastCompletedMoved10s = Math.sqrt(horizontalDistanceSqr(this.nonAggro10sSamplePos, currentPos));
                if (this.nonAggro10sSampleTick != Long.MIN_VALUE
                        && (now - this.nonAggro10sSampleTick) >= GRAND_EVENT_NON_AGGRO_STAGNATION_SAMPLE_10S_TICKS) {
                    this.nonAggroLastCompletedMoved10sTick = now;
                    this.nonAggro10sSamplePos = currentPos;
                    this.nonAggro10sSampleTick = now;
                }
            }
        }

        private double lastCompletedNonAggroMoved5s() {
            return this.nonAggroLastCompletedMoved5s;
        }

        private double lastCompletedNonAggroMoved10s() {
            return this.nonAggroLastCompletedMoved10s;
        }

        private long nonAggroStagnationTicks(long now) {
            if (this.nonAggroLastSignificantProgressTick == Long.MIN_VALUE) {
                return 0L;
            }
            return Math.max(0L, now - this.nonAggroLastSignificantProgressTick);
        }

        private boolean isNonAggroStagnating(long now) {
            boolean has5sWindow = this.nonAggroLastCompletedMoved5sTick != Long.MIN_VALUE
                    && (now - this.nonAggroLastCompletedMoved5sTick) <= (GRAND_EVENT_NON_AGGRO_STAGNATION_SAMPLE_5S_TICKS * 2L);
            boolean has10sWindow = this.nonAggroLastCompletedMoved10sTick != Long.MIN_VALUE
                    && (now - this.nonAggroLastCompletedMoved10sTick) <= (GRAND_EVENT_NON_AGGRO_STAGNATION_SAMPLE_10S_TICKS * 2L);
            boolean lowMobilityWindow = (has5sWindow && this.nonAggroLastCompletedMoved5s < GRAND_EVENT_NON_AGGRO_STAGNATION_5S_MIN_MOVEMENT)
                    || (has10sWindow && this.nonAggroLastCompletedMoved10s < GRAND_EVENT_NON_AGGRO_STAGNATION_10S_MIN_MOVEMENT);
            return lowMobilityWindow || nonAggroStagnationTicks(now) >= GRAND_EVENT_NON_AGGRO_STAGNATION_FORCE_REISSUE_TICKS;
        }

        private void resetNonAggroMobility() {
            this.nonAggroLastMobilityPos = null;
            this.nonAggro5sSamplePos = null;
            this.nonAggro5sSampleTick = Long.MIN_VALUE;
            this.nonAggro10sSamplePos = null;
            this.nonAggro10sSampleTick = Long.MIN_VALUE;
            this.nonAggroLastCompletedMoved5s = 0.0D;
            this.nonAggroLastCompletedMoved10s = 0.0D;
            this.nonAggroLastCompletedMoved5sTick = Long.MIN_VALUE;
            this.nonAggroLastCompletedMoved10sTick = Long.MIN_VALUE;
            this.nonAggroLastSignificantProgressTick = Long.MIN_VALUE;
            this.lastObservedYaw = Float.NaN;
            this.spinInPlaceTicks = 0L;
        }

        private void markSearchNodeIssuedAt(long tick) {
            this.searchNodeIssuedTicks.addLast(tick);
            long cutoff = tick - GRAND_EVENT_NON_AGGRO_NODE_WINDOW_TICKS;
            while (!this.searchNodeIssuedTicks.isEmpty() && this.searchNodeIssuedTicks.peekFirst() < cutoff) {
                this.searchNodeIssuedTicks.removeFirst();
            }
        }

        private int searchNodeChangesLast60s(long now) {
            long cutoff = now - GRAND_EVENT_NON_AGGRO_NODE_WINDOW_TICKS;
            while (!this.searchNodeIssuedTicks.isEmpty() && this.searchNodeIssuedTicks.peekFirst() < cutoff) {
                this.searchNodeIssuedTicks.removeFirst();
            }
            return this.searchNodeIssuedTicks.size();
        }

        private void samplePendingPathProgress(
                BlockPos pendingNode,
                Vec3 wardenPos,
                boolean hasWalkTarget,
                boolean hasPathMemory,
                boolean hasCantReachSince,
                boolean navDone,
                long now) {
            if (pendingNode == null || wardenPos == null || this.pendingIssuedTick == Long.MIN_VALUE) {
                return;
            }
            Vec3 pendingCenter = Vec3.atCenterOf(pendingNode);
            double distSqr = horizontalDistanceSqr(wardenPos, pendingCenter);
            this.pendingCurrentDistanceSqr = distSqr;
            if (!Double.isFinite(this.pendingBestDistanceSqr) || distSqr + GRAND_EVENT_PENDING_PROGRESS_EPSILON_SQR < this.pendingBestDistanceSqr) {
                this.pendingBestDistanceSqr = distSqr;
                this.pendingBestProgressTick = now;
            }
            boolean idle = navDone || (!hasWalkTarget && !hasPathMemory);
            if (idle) {
                if (this.pendingNavIdleSinceTick == Long.MIN_VALUE) {
                    this.pendingNavIdleSinceTick = now;
                }
            } else {
                this.pendingNavIdleSinceTick = Long.MIN_VALUE;
            }
            if (hasCantReachSince) {
                if (this.pendingCantReachSinceTick == Long.MIN_VALUE) {
                    this.pendingCantReachSinceTick = now;
                }
            } else {
                this.pendingCantReachSinceTick = Long.MIN_VALUE;
            }
        }

        private long pendingAgeTicks(long now) {
            if (this.pendingIssuedTick == Long.MIN_VALUE) {
                return 0L;
            }
            return Math.max(0L, now - this.pendingIssuedTick);
        }

        private long pendingNoProgressTicks(long now) {
            if (this.pendingBestProgressTick == Long.MIN_VALUE) {
                return 0L;
            }
            return Math.max(0L, now - this.pendingBestProgressTick);
        }

        private boolean isPendingProgressStalled(long now) {
            return pendingNoProgressTicks(now) >= GRAND_EVENT_PENDING_PROGRESS_STALL_TICKS;
        }

        private boolean isPendingNavIdleStalled(long now) {
            return this.pendingNavIdleSinceTick != Long.MIN_VALUE
                    && (now - this.pendingNavIdleSinceTick) >= GRAND_EVENT_PENDING_NAV_IDLE_STALL_TICKS;
        }

        private boolean isPendingCantReachStalled(long now) {
            return this.pendingCantReachSinceTick != Long.MIN_VALUE
                    && (now - this.pendingCantReachSinceTick) >= GRAND_EVENT_PENDING_CANT_REACH_STALL_TICKS;
        }

        private double pendingCurrentDistance() {
            if (!Double.isFinite(this.pendingCurrentDistanceSqr)) {
                return -1.0D;
            }
            return Math.sqrt(this.pendingCurrentDistanceSqr);
        }

        private double pendingBestDistance() {
            if (!Double.isFinite(this.pendingBestDistanceSqr)) {
                return -1.0D;
            }
            return Math.sqrt(this.pendingBestDistanceSqr);
        }

        private double pendingDistanceDelta() {
            if (!Double.isFinite(this.pendingInitialDistanceSqr) || !Double.isFinite(this.pendingBestDistanceSqr)) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.sqrt(this.pendingInitialDistanceSqr) - Math.sqrt(this.pendingBestDistanceSqr));
        }

        private void recordReplanIssueTick(long now, BlockPos previousIssued, BlockPos currentIssued) {
            this.replanIssueTicks.addLast(now);
            long cutoff = now - GRAND_EVENT_REPLAN_WINDOW_TICKS;
            while (!this.replanIssueTicks.isEmpty() && this.replanIssueTicks.peekFirst() < cutoff) {
                this.replanIssueTicks.removeFirst();
            }
            if (previousIssued != null && currentIssued != null && previousIssued.distSqr(currentIssued) <= GRAND_EVENT_NODE_CHURN_NEAR_REPEAT_SQR) {
                this.nearRepeatIssueTicks.addLast(now);
            }
            while (!this.nearRepeatIssueTicks.isEmpty() && this.nearRepeatIssueTicks.peekFirst() < cutoff) {
                this.nearRepeatIssueTicks.removeFirst();
            }
            if (this.replanIssueTicks.size() >= GRAND_EVENT_REPLAN_WINDOW_MAX) {
                this.replanCooldownUntilTick = now + GRAND_EVENT_REPLAN_COOLDOWN_TICKS;
            }
        }

        private int replansLastWindow(long now) {
            long cutoff = now - GRAND_EVENT_REPLAN_WINDOW_TICKS;
            while (!this.replanIssueTicks.isEmpty() && this.replanIssueTicks.peekFirst() < cutoff) {
                this.replanIssueTicks.removeFirst();
            }
            return this.replanIssueTicks.size();
        }

        private int nearRepeatReplansLastWindow(long now) {
            long cutoff = now - GRAND_EVENT_REPLAN_WINDOW_TICKS;
            while (!this.nearRepeatIssueTicks.isEmpty() && this.nearRepeatIssueTicks.peekFirst() < cutoff) {
                this.nearRepeatIssueTicks.removeFirst();
            }
            return this.nearRepeatIssueTicks.size();
        }

        private boolean isReplanCooldownActive(long now) {
            return this.replanCooldownUntilTick != Long.MIN_VALUE && now < this.replanCooldownUntilTick;
        }

        private long replanCooldownUntilTick() {
            return this.replanCooldownUntilTick;
        }

        private void sampleSpinInPlace(float currentYaw, double horizontalSpeed) {
            if (!Float.isFinite(this.lastObservedYaw)) {
                this.lastObservedYaw = currentYaw;
                this.spinInPlaceTicks = 0L;
                return;
            }
            double yawDelta = Math.abs(wrapAngleDegrees(currentYaw - this.lastObservedYaw));
            if (horizontalSpeed <= GRAND_EVENT_SPIN_IN_PLACE_SPEED_MAX && yawDelta >= GRAND_EVENT_SPIN_IN_PLACE_YAW_DELTA_MIN) {
                this.spinInPlaceTicks++;
            } else {
                this.spinInPlaceTicks = Math.max(0L, this.spinInPlaceTicks - 2L);
            }
            this.lastObservedYaw = currentYaw;
        }

        private boolean isSpinInPlaceDetected() {
            return this.spinInPlaceTicks >= GRAND_EVENT_SPIN_IN_PLACE_DETECT_TICKS;
        }

        private long spinInPlaceTicks() {
            return this.spinInPlaceTicks;
        }

        private void rememberConsumedCoverageNode(BlockPos node, long now) {
            if (node == null) {
                return;
            }
            long cutoff = now - GRAND_EVENT_NON_AGGRO_NODE_WINDOW_TICKS;
            while (!this.consumedSearchNodeTicks.isEmpty() && this.consumedSearchNodeTicks.peekFirst() < cutoff) {
                this.consumedSearchNodeTicks.removeFirst();
                if (!this.consumedSearchNodes.isEmpty()) {
                    this.consumedSearchNodes.removeFirst();
                }
                if (!this.consumedSearchMicroZones.isEmpty()) {
                    int removedMicro = this.consumedSearchMicroZones.removeFirst();
                    if (removedMicro >= 0) {
                        this.searchMicroZoneVisitCounts.computeIfPresent(removedMicro, (key, count) -> count <= 1 ? null : count - 1);
                    }
                }
            }

            this.consumedSearchNodeTicks.addLast(now);
            BlockPos immutable = node.immutable();
            this.consumedSearchNodes.addLast(immutable);
            int microZone = resolveGrandEventSearchMicroZone(this.anchorPos, immutable);
            this.consumedSearchMicroZones.addLast(microZone);
            if (microZone >= 0) {
                this.searchMicroZoneVisitCounts.merge(microZone, 1, Integer::sum);
            }
            if (this.lastConsumedSearchNode != null) {
                double step = Math.sqrt(this.lastConsumedSearchNode.distSqr(immutable));
                if (step < GRAND_EVENT_NON_AGGRO_MIN_CONSUMED_NODE_STEP_BLOCKS
                        || (this.lastConsumedSearchMicroZone >= 0 && this.lastConsumedSearchMicroZone == microZone)) {
                    this.consecutiveMicroLoopNodes++;
                } else {
                    this.consecutiveMicroLoopNodes = 0;
                }
            } else {
                this.consecutiveMicroLoopNodes = 0;
            }
            this.lastConsumedSearchNode = immutable;
            this.lastConsumedSearchMicroZone = microZone;
        }

        private int uniqueSearchSubzonesLast60s(long now) {
            pruneConsumedCoverage(now);
            return this.searchMicroZoneVisitCounts.size();
        }

        private double maxConsumedRadiusLast60s(long now) {
            pruneConsumedCoverage(now);
            double best = 0.0D;
            for (BlockPos node : this.consumedSearchNodes) {
                if (node == null) {
                    continue;
                }
                double distance = Math.sqrt(this.anchorPos.distSqr(node));
                if (distance > best) {
                    best = distance;
                }
            }
            return best;
        }

        private int searchMicroZoneVisitCount(int microZone) {
            if (microZone < 0) {
                return 0;
            }
            return this.searchMicroZoneVisitCounts.getOrDefault(microZone, 0);
        }

        private int countRecentSearchMicroZoneHits(int microZone) {
            if (microZone < 0 || this.consumedSearchMicroZones.isEmpty()) {
                return 0;
            }
            int hits = 0;
            for (int recent : this.consumedSearchMicroZones) {
                if (recent == microZone) {
                    hits++;
                }
            }
            return hits;
        }

        private boolean isLocalOrbiting(long now) {
            pruneConsumedCoverage(now);
            if (this.consumedSearchNodes.size() < GRAND_EVENT_LOCAL_ORBITING_MIN_WINDOW_NODES) {
                return false;
            }
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos node : this.consumedSearchNodes) {
                if (node == null) {
                    continue;
                }
                if (node.getX() < minX) {
                    minX = node.getX();
                }
                if (node.getX() > maxX) {
                    maxX = node.getX();
                }
                if (node.getZ() < minZ) {
                    minZ = node.getZ();
                }
                if (node.getZ() > maxZ) {
                    maxZ = node.getZ();
                }
            }
            if (minX == Integer.MAX_VALUE || minZ == Integer.MAX_VALUE) {
                return false;
            }
            double spanX = maxX - minX;
            double spanZ = maxZ - minZ;
            int uniqueSubzones = this.searchMicroZoneVisitCounts.size();
            return this.consecutiveMicroLoopNodes >= 3
                    && spanX <= GRAND_EVENT_LOCAL_ORBITING_MAX_SPAN_BLOCKS
                    && spanZ <= GRAND_EVENT_LOCAL_ORBITING_MAX_SPAN_BLOCKS
                    && uniqueSubzones <= GRAND_EVENT_LOCAL_ORBITING_MAX_UNIQUE_SUBZONES;
        }

        private int consecutiveMicroLoopNodes() {
            return this.consecutiveMicroLoopNodes;
        }

        private BlockPos lastConsumedSearchNode() {
            return this.lastConsumedSearchNode;
        }

        private void pruneConsumedCoverage(long now) {
            long cutoff = now - GRAND_EVENT_NON_AGGRO_NODE_WINDOW_TICKS;
            while (!this.consumedSearchNodeTicks.isEmpty() && this.consumedSearchNodeTicks.peekFirst() < cutoff) {
                this.consumedSearchNodeTicks.removeFirst();
                if (!this.consumedSearchNodes.isEmpty()) {
                    this.consumedSearchNodes.removeFirst();
                }
                if (!this.consumedSearchMicroZones.isEmpty()) {
                    int removedMicro = this.consumedSearchMicroZones.removeFirst();
                    if (removedMicro >= 0) {
                        this.searchMicroZoneVisitCounts.computeIfPresent(removedMicro, (key, count) -> count <= 1 ? null : count - 1);
                    }
                }
            }
        }

        private int visitedSearchSectorsCount() {
            int count = 0;
            for (int visits : this.searchSectorVisitCounts) {
                if (visits > 0) {
                    count++;
                }
            }
            return count;
        }

        private boolean ended() {
            return ended;
        }

        private void setEnded(boolean ended) {
            this.ended = ended;
        }

        private void startNewSniffCycle() {
            this.sniffPendingPlayerId = null;
            this.sniffPendingUntilTick = Long.MIN_VALUE;
            this.sniffPoseUntilTick = Long.MIN_VALUE;
        }

        private boolean hasSniffedThisCycle(UUID playerId) {
            return playerId != null && this.sniffedPlayersThisCycle.contains(playerId);
        }

        private void markSniffedThisCycle(UUID playerId) {
            if (playerId != null) {
                this.sniffedPlayersThisCycle.add(playerId);
            }
        }

        private void setSniffPending(UUID playerId, long pendingUntilTick) {
            this.sniffPendingPlayerId = playerId;
            this.sniffPendingUntilTick = pendingUntilTick;
        }

        private boolean isSniffPending(UUID playerId, long now) {
            if (this.sniffPendingPlayerId == null || playerId == null || !playerId.equals(this.sniffPendingPlayerId)) {
                return false;
            }
            if (this.sniffPendingUntilTick != Long.MIN_VALUE && now > this.sniffPendingUntilTick) {
                clearSniffPending();
                return false;
            }
            return true;
        }

        private void clearSniffPending() {
            this.sniffPendingPlayerId = null;
            this.sniffPendingUntilTick = Long.MIN_VALUE;
        }

        private long sniffPoseUntilTick() {
            return this.sniffPoseUntilTick;
        }

        private void setSniffPoseUntilTick(long tick) {
            this.sniffPoseUntilTick = tick;
        }

        private long nextSniffSoundTick() {
            return this.nextSniffSoundTick;
        }

        private void setNextSniffSoundTick(long tick) {
            this.nextSniffSoundTick = tick;
        }

        private long nextSearchSniffTick() {
            return this.nextSniffSoundTick;
        }

        private void setNextSearchSniffTick(long tick) {
            this.nextSniffSoundTick = tick;
        }

        private void scheduleNextSearchSniffTick(ServerLevel level, long now) {
            if (level == null) {
                this.nextSniffSoundTick = Long.MIN_VALUE;
                return;
            }
            this.nextSniffSoundTick = now + rollRangeInclusive(level, GRAND_EVENT_SEARCH_SNIFF_MIN_INTERVAL_TICKS, GRAND_EVENT_SEARCH_SNIFF_MAX_INTERVAL_TICKS);
        }

        private boolean emerging() {
            return this.emerging;
        }

        private long emergeEndTick() {
            return this.emergeEndTick;
        }

        private void startEmerging(long endTick) {
            this.emerging = true;
            this.emergeEndTick = endTick;
        }

        private void stopEmerging() {
            this.emerging = false;
            this.emergeEndTick = Long.MIN_VALUE;
        }

        private boolean isRecentSearchNodeTooClose(BlockPos node, double minDistanceSqr) {
            if (node == null) {
                return false;
            }
            for (BlockPos recent : this.recentSearchNodes) {
                if (recent != null && recent.distSqr(node) < minDistanceSqr) {
                    return true;
                }
            }
            return false;
        }

        private void rememberSearchNode(BlockPos node) {
            if (node == null) {
                return;
            }
            this.recentSearchNodes.addLast(node.immutable());
            while (this.recentSearchNodes.size() > GRAND_EVENT_RECENT_SEARCH_HISTORY_LIMIT) {
                this.recentSearchNodes.removeFirst();
            }
        }

        private void resetSearchSectorCoverage() {
            for (int i = 0; i < this.searchSectorVisitCounts.length; i++) {
                this.searchSectorVisitCounts[i] = 0;
            }
            this.recentSearchSectors.clear();
            this.lastSearchSector = -1;
        }

        private int searchSectorVisitCount(int sector) {
            int normalized = normalizeSearchSector(sector);
            if (normalized < 0) {
                return 0;
            }
            return this.searchSectorVisitCounts[normalized];
        }

        private int maxSearchSectorVisitCount() {
            int max = 0;
            for (int visits : this.searchSectorVisitCounts) {
                if (visits > max) {
                    max = visits;
                }
            }
            return max;
        }

        private boolean isSameSearchSectorAsLast(int sector) {
            int normalized = normalizeSearchSector(sector);
            return normalized >= 0 && this.lastSearchSector >= 0 && normalized == this.lastSearchSector;
        }

        private boolean isAdjacentSearchSectorToLast(int sector) {
            int normalized = normalizeSearchSector(sector);
            if (normalized < 0 || this.lastSearchSector < 0) {
                return false;
            }
            return areAdjacentSearchSectors(normalized, this.lastSearchSector);
        }

        private int countRecentSearchSectorAdjacencyHits(int sector) {
            int normalized = normalizeSearchSector(sector);
            if (normalized < 0 || this.recentSearchSectors.isEmpty()) {
                return 0;
            }
            int hits = 0;
            for (int recent : this.recentSearchSectors) {
                if (recent == normalized || areAdjacentSearchSectors(recent, normalized)) {
                    hits++;
                }
            }
            return hits;
        }

        private void rememberSearchSector(int sector) {
            int normalized = normalizeSearchSector(sector);
            if (normalized < 0) {
                return;
            }
            this.searchSectorVisitCounts[normalized]++;
            this.lastSearchSector = normalized;
            this.recentSearchSectors.addLast(normalized);
            while (this.recentSearchSectors.size() > GRAND_EVENT_RECENT_SEARCH_HISTORY_LIMIT) {
                this.recentSearchSectors.removeFirst();
            }
        }

        private String searchSectorCoverageString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < this.searchSectorVisitCounts.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(this.searchSectorVisitCounts[i]);
            }
            builder.append(']');
            return builder.toString();
        }

        private double minDistanceToRecentSearchNodesSqr(BlockPos node) {
            if (node == null || this.recentSearchNodes.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
            double best = Double.POSITIVE_INFINITY;
            for (BlockPos recent : this.recentSearchNodes) {
                if (recent == null) {
                    continue;
                }
                double dist = recent.distSqr(node);
                if (dist < best) {
                    best = dist;
                }
            }
            return best;
        }

        private int searchNoPathStreak() {
            return this.searchNoPathStreak;
        }

        private void incrementSearchNoPathStreak() {
            this.searchNoPathStreak++;
        }

        private void resetSearchNoPathStreak() {
            this.searchNoPathStreak = 0;
        }

        private boolean searchRecoveryModeActive() {
            return this.searchRecoveryModeActive;
        }

        private void setSearchRecoveryModeActive(boolean active) {
            this.searchRecoveryModeActive = active;
        }

        private boolean relaxedRecentModeActive() {
            return this.relaxedRecentModeActive;
        }

        private void setRelaxedRecentModeActive(boolean active) {
            this.relaxedRecentModeActive = active;
        }

        private int recentOverlapStallStreak() {
            return this.recentOverlapStallStreak;
        }

        private void incrementRecentOverlapStallStreak() {
            this.recentOverlapStallStreak++;
        }

        private void resetRecentOverlapStallStreak() {
            this.recentOverlapStallStreak = 0;
        }

        private long lastSearchHistoryResetTick() {
            return this.lastSearchHistoryResetTick;
        }

        private void setLastSearchHistoryResetTick(long tick) {
            this.lastSearchHistoryResetTick = tick;
        }

        private void clearRecentSearchHistory() {
            this.recentSearchNodes.clear();
        }

        private long caveBreakNoIntentSinceTick() {
            return this.caveBreakNoIntentSinceTick;
        }

        private void setCaveBreakNoIntentSinceTick(long tick) {
            this.caveBreakNoIntentSinceTick = tick;
        }

        private boolean caveBreakStuckBoostActive() {
            return this.caveBreakStuckBoostActive;
        }

        private void setCaveBreakStuckBoostActive(boolean active) {
            this.caveBreakStuckBoostActive = active;
        }

        private String lastCaveBreakDirectionSource() {
            return this.lastCaveBreakDirectionSource;
        }

        private long lastCaveBreakDirectionTick() {
            return this.lastCaveBreakDirectionTick;
        }

        private void setLastCaveBreakDirectionSource(String source, long now) {
            this.lastCaveBreakDirectionSource = source == null ? "none" : source;
            this.lastCaveBreakDirectionTick = now;
        }

        private BlockPos caveExitRetreatNode() {
            return this.caveExitRetreatNode;
        }

        private void setCaveExitRetreatNode(BlockPos node) {
            this.caveExitRetreatNode = node == null ? null : node.immutable();
        }

        private long caveExitRetargetTick() {
            return this.caveExitRetargetTick;
        }

        private void setCaveExitRetargetTick(long tick) {
            this.caveExitRetargetTick = tick;
        }

        private long caveExitStartTick() {
            return this.caveExitStartTick;
        }

        private void setCaveExitStartTick(long tick) {
            this.caveExitStartTick = tick;
        }

        private int caveExitRetreatAttempts() {
            return this.caveExitRetreatAttempts;
        }

        private void incrementCaveExitRetreatAttempts() {
            this.caveExitRetreatAttempts++;
        }

        private void resetCaveExitRetreatAttempts() {
            this.caveExitRetreatAttempts = 0;
        }

        private void resetCaveExitRetreat() {
            this.caveExitRetreatNode = null;
            this.caveExitRetargetTick = Long.MIN_VALUE;
            this.caveExitStartTick = Long.MIN_VALUE;
            this.caveExitRetreatAttempts = 0;
        }

        private long roarSniffStuckSinceTick() {
            return this.roarSniffStuckSinceTick;
        }

        private void setRoarSniffStuckSinceTick(long tick) {
            this.roarSniffStuckSinceTick = tick;
        }

        private Vec3 lastKnownPosition(UUID playerId) {
            return this.lastKnownPositions.get(playerId);
        }

        private void setLastKnownPosition(UUID playerId, Vec3 position) {
            if (playerId == null || position == null) {
                return;
            }
            this.lastKnownPositions.put(playerId, position);
        }

        private void removeLastKnownPosition(UUID playerId) {
            if (playerId != null) {
                this.lastKnownPositions.remove(playerId);
                clearMovementTracking(playerId);
            }
        }

        private double movementAccumulator(UUID playerId) {
            if (playerId == null) {
                return 0.0D;
            }
            return this.movementAccumulators.getOrDefault(playerId, 0.0D);
        }

        private void setMovementAccumulator(UUID playerId, double value) {
            if (playerId == null) {
                return;
            }
            if (value <= 0.0D) {
                this.movementAccumulators.remove(playerId);
            } else {
                this.movementAccumulators.put(playerId, value);
            }
        }

        private long lastMovementSampleTick(UUID playerId) {
            if (playerId == null) {
                return Long.MIN_VALUE;
            }
            return this.movementLastSampleTicks.getOrDefault(playerId, Long.MIN_VALUE);
        }

        private void setLastMovementSampleTick(UUID playerId, long tick) {
            if (playerId == null) {
                return;
            }
            this.movementLastSampleTicks.put(playerId, tick);
        }

        private void clearMovementTracking(UUID playerId) {
            if (playerId == null) {
                return;
            }
            this.movementAccumulators.remove(playerId);
            this.movementLastSampleTicks.remove(playerId);
        }

        private boolean isAggroTuningActive() {
            return this.aggroTuningActive;
        }

        private void setAggroTuningActive(boolean active) {
            this.aggroTuningActive = active;
        }

        private void captureGrandWardenAggroDefaults(Warden warden) {
            if (warden == null) {
                return;
            }
            if (this.waterPathMalusBeforeAggro == null) {
                this.waterPathMalusBeforeAggro = warden.getPathfindingMalus(PathType.WATER);
            }
            if (this.canFloatBeforeAggro == null && warden.getNavigation() instanceof GroundPathNavigation groundNavigation) {
                this.canFloatBeforeAggro = groundNavigation.canFloat();
            }
        }

        private void restoreGrandWardenAggroDefaults(Warden warden) {
            if (warden == null) {
                return;
            }
            if (this.waterPathMalusBeforeAggro != null) {
                warden.setPathfindingMalus(PathType.WATER, this.waterPathMalusBeforeAggro);
            }
            if (this.canFloatBeforeAggro != null && warden.getNavigation() instanceof GroundPathNavigation groundNavigation) {
                groundNavigation.setCanFloat(this.canFloatBeforeAggro);
            }
            this.waterPathMalusBeforeAggro = null;
            this.canFloatBeforeAggro = null;
            this.lastSonicAssistTick = Long.MIN_VALUE;
        }

        private long lastSonicAssistTick() {
            return this.lastSonicAssistTick;
        }

        private void setLastSonicAssistTick(long tick) {
            this.lastSonicAssistTick = tick;
        }
    }

    private static final class AsphyxiaState {
        private final long startTick;
        private final long endTick;
        private final AsphyxiaVariant variant;
        private boolean damageApplied;

        private AsphyxiaState(long startTick, long endTick, AsphyxiaVariant variant, boolean damageApplied) {
            this.startTick = startTick;
            this.endTick = endTick;
            this.variant = variant;
            this.damageApplied = damageApplied;
        }

        private long startTick() {
            return this.startTick;
        }

        private long endTick() {
            return this.endTick;
        }

        private AsphyxiaVariant variant() {
            return this.variant;
        }

        private boolean damageApplied() {
            return this.damageApplied;
        }

        private void setDamageApplied(boolean damageApplied) {
            this.damageApplied = damageApplied;
        }
    }

    private static final class HunterFogState {
        private final long endTick;
        private int stillTicks;

        private HunterFogState(long endTick, int stillTicks) {
            this.endTick = endTick;
            this.stillTicks = stillTicks;
        }

        private long endTick() {
            return this.endTick;
        }

        private int stillTicks() {
            return this.stillTicks;
        }

        private void setStillTicks(int stillTicks) {
            this.stillTicks = stillTicks;
        }
    }

    private static final class LivingOreState {
        private final long endTick;
        private final LivingOreVariant variant;

        private LivingOreState(long endTick, LivingOreVariant variant) {
            this.endTick = endTick;
            this.variant = variant;
        }

        private long endTick() {
            return this.endTick;
        }

        private LivingOreVariant variant() {
            return this.variant;
        }
    }

    private static final class AquaticBiteState {
        private final long startTick;
        private final long deadlineTick;
        private final BlockPos source;
        private long nextCueTick;

        private AquaticBiteState(long startTick, long deadlineTick, BlockPos source, long nextCueTick) {
            this.startTick = startTick;
            this.deadlineTick = deadlineTick;
            this.source = source.immutable();
            this.nextCueTick = nextCueTick;
        }

        private long startTick() {
            return this.startTick;
        }

        private long deadlineTick() {
            return this.deadlineTick;
        }

        private BlockPos source() {
            return this.source;
        }

        private long nextCueTick() {
            return this.nextCueTick;
        }

        private void setNextCueTick(long nextCueTick) {
            this.nextCueTick = nextCueTick;
        }
    }

    private static final class GiantSunState {
        private final long endTick;
        private long nextPulseTick;
        private int lookTicks;
        private boolean burnApplied;
        private int levitationBursts;

        private GiantSunState(long endTick, long nextPulseTick, int lookTicks, boolean burnApplied, int levitationBursts) {
            this.endTick = endTick;
            this.nextPulseTick = nextPulseTick;
            this.lookTicks = lookTicks;
            this.burnApplied = burnApplied;
            this.levitationBursts = levitationBursts;
        }

        private long endTick() {
            return this.endTick;
        }

        private long nextPulseTick() {
            return this.nextPulseTick;
        }

        private void setNextPulseTick(long nextPulseTick) {
            this.nextPulseTick = nextPulseTick;
        }

        private int lookTicks() {
            return this.lookTicks;
        }

        private void setLookTicks(int lookTicks) {
            this.lookTicks = lookTicks;
        }

        private boolean burnApplied() {
            return this.burnApplied;
        }

        private void setBurnApplied(boolean burnApplied) {
            this.burnApplied = burnApplied;
        }

        private int levitationBursts() {
            return this.levitationBursts;
        }

        private void setLevitationBursts(int levitationBursts) {
            this.levitationBursts = levitationBursts;
        }
    }

    private static final class TurnAroundTrapState {
        private final long checkUntilTick;
        private final Vec3 initialLook;

        private TurnAroundTrapState(long checkUntilTick, Vec3 initialLook) {
            this.checkUntilTick = checkUntilTick;
            this.initialLook = initialLook;
        }

        private long checkUntilTick() {
            return this.checkUntilTick;
        }

        private Vec3 initialLook() {
            return this.initialLook;
        }
    }

    private record WaterRestoreTask(ResourceKey<Level> dimension, BlockPos pos, BlockState originalState, long restoreTick) {
    }

}


