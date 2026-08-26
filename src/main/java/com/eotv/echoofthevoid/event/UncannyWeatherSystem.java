package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherTimingRules;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherPacingRules;
import com.eotv.echoofthevoid.event.weather.UncannyWeatherPacingRules.Event;
import com.eotv.echoofthevoid.network.UncannyLocalizedWeatherPayload;
import com.eotv.echoofthevoid.sound.UncannySoundDelivery;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UncannyWeatherSystem {
    private static final long WEATHER_TICK_INTERVAL = 1L;
    private static final long WEATHER_MAX_IDLE_COOLDOWN_TICKS = 20L * 60L * 12L;
    private static final long WEATHER_MAX_IDLE_NEXT_CHECK_TICKS = 20L * 60L * 8L;

    private UncannyWeatherSystem() {
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Weather] " + message, args);
        }
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || !(event.getEntity() instanceof ServerPlayer)
                || !isTrackedPlayerLight(event.getPlacedBlock())) {
            return;
        }
        UncannyWorldState.get(level.getServer()).rememberPlayerPlacedLight(event.getPos());
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        UncannyWorldState.get(level.getServer()).forgetPlayerPlacedLight(event.getPos());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null || server.getTickCount() % WEATHER_TICK_INTERVAL != 0L) {
            return;
        }

        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        if (allPlayers.isEmpty()) {
            return;
        }

        List<ServerPlayer> activePlayers = allPlayers.stream()
                .filter(player -> !player.isSpectator())
                .toList();

        UncannyWorldState state = UncannyWorldState.get(server);
        int phaseIndex = state.getCurrentPhaseIndex();
        long now = server.getTickCount();
        sanitizeWeatherTimers(server, state, now);

        if (!state.isSystemEnabled() || phaseIndex < 1) {
            stopActiveEvent(server, state, now, true);
            clearWeatherTags(allPlayers);
            return;
        }

        if (UncannyParanoiaEventSystem.isGrandEventAutoPauseActive(server.overworld())) {
            stopActiveEvent(server, state, now, true);
            clearWeatherTags(allPlayers);
            debugLog("WEATHER pause_auto dim={} reason=grand_event_active", server.overworld().dimension().location());
            return;
        }
        if (UncannyParanoiaEventSystem.isTensionBuilderAutoPauseActive(server.overworld())) {
            stopActiveEvent(server, state, now, true);
            clearWeatherTags(allPlayers);
            debugLog("WEATHER pause_auto dim={} reason=tension_builder_active", server.overworld().dimension().location());
            return;
        }

        Event activeEvent = Event.byId(state.getActiveWeatherEventId());
        if (activeEvent != null) {
            syncWeatherTags(allPlayers, activeEvent.id);
            if (isLocalizedWeather(activeEvent) && now % 40L == 0L) {
                syncLocalizedWeather(server, state, activeEvent, now, allPlayers);
            }
            tickActiveEvent(server, state, activeEvent, now, allPlayers);
            if (now >= state.getWeatherEventEndTick()) {
                stopActiveEvent(server, state, now, false);
            }
            return;
        }

        clearWeatherTags(allPlayers);
        if (activePlayers.isEmpty()) {
            return;
        }
        if (now < state.getWeatherCooldownUntilTick() || now < state.getWeatherNextCheckTick()) {
            return;
        }

        int profile = getProfile();
        int danger = getDangerLevel();
        state.setWeatherNextCheckTick(now + rollNextCheckDelayTicks(server.overworld(), phaseIndex, profile));

        double chance = rollTriggerChance(phaseIndex, profile);
        double roll = server.overworld().random.nextDouble();
        if (roll > chance) {
            debugLog("WEATHER no-trigger phase={} profile={} danger={} roll={} chance={}", phaseIndex, profile, danger, roll, chance);
            return;
        }

        debugLog("WEATHER trigger-roll-hit phase={} profile={} danger={} roll={} chance={}", phaseIndex, profile, danger, roll, chance);
        Event selected = rollEvent(server.overworld(), phaseIndex, profile, danger, state.getLastWeatherEventId());
        if (selected == null) {
            debugLog("WEATHER no-candidate-selected phase={} profile={} danger={}", phaseIndex, profile, danger);
            return;
        }

        debugLog("WEATHER selected event={} phase={} profile={} danger={}", selected.id, phaseIndex, profile, danger);
        if (!startEvent(server, state, selected, now, allPlayers, phaseIndex, profile)) {
            state.setWeatherNextCheckTick(now + 100L + server.overworld().random.nextInt(121));
            debugLog("WEATHER selected-context-lost event={} phase={}", selected.id, phaseIndex);
        }
    }

    public static boolean forceTrigger(MinecraftServer server, String eventId) {
        Event event = Event.byId(eventId);
        if (server == null || event == null) {
            debugLog("WEATHER force-trigger failed id={} serverNull={} eventNull={}", eventId, server == null, event == null);
            return false;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        long now = server.getTickCount();
        stopActiveEvent(server, state, now, true);
        boolean started = startEvent(
                server,
                state,
                event,
                now,
                server.getPlayerList().getPlayers(),
                state.getCurrentPhaseIndex(),
                getProfile());
        debugLog("WEATHER force-trigger result id={} started={}", eventId, started);
        return started;
    }

    public static void forceStop(MinecraftServer server) {
        if (server == null) {
            return;
        }
        stopActiveEvent(server, UncannyWorldState.get(server), server.getTickCount(), true);
        clearWeatherTags(server.getPlayerList().getPlayers());
        debugLog("WEATHER force-stop");
    }

    private static boolean startEvent(
            MinecraftServer server,
            UncannyWorldState state,
            Event event,
            long now,
            List<ServerPlayer> players,
            int phaseIndex,
            int profile) {
        ServerLevel overworld = server.overworld();
        int duration = event.minDurationTicks + overworld.random.nextInt(event.maxDurationTicks - event.minDurationTicks + 1);
        duration = applyVisualDurationRules(overworld, state, event, duration);

        if (isLocalizedWeather(event)
                && !configureLocalizedWeather(server, state, event, now, players, duration)) {
            return false;
        }

        state.setActiveWeatherEventId(event.id);
        state.setLastWeatherEventId(event.id);
        state.setWeatherEventEndTick(now + duration);
        state.setWeatherAuxTick(now);
        state.setWeatherAuxValue(0);
        state.setWeatherTargetPlayerUuid("");
        state.setWeatherSavedDayTime(Long.MIN_VALUE);
        syncWeatherTags(players, event.id);
        if (isLocalizedWeather(event)) {
            syncLocalizedWeather(server, state, event, now, players);
        }

        switch (event) {
            case RAIN_SILENT -> setWeather(overworld, true, false, duration + 120);
            case RAIN_DRY_STORM -> {
                setWeather(overworld, false, false, 0);
                state.setWeatherAuxValue(UncannyWeatherTimingRules.dryRainPulseCount(overworld.random.nextInt()));
                state.setWeatherAuxTick(now + 4L + overworld.random.nextInt(17));
            }
            case RAIN_ASH -> setWeather(overworld, false, false, 0);
            case RAIN_SOBBING -> {
                setWeather(overworld, true, false, duration + 120);
                List<ServerPlayer> eligible = players.stream()
                        .filter(player -> !player.isSpectator() && player.serverLevel() == overworld)
                        .sorted(Comparator.comparing(ServerPlayer::getStringUUID))
                        .toList();
                UncannyWeatherTimingRules.SobbingRainAudience audience =
                        UncannyWeatherTimingRules.sobbingRainAudience(eligible.size(), overworld.random.nextInt());
                if (!audience.shared()) {
                    state.setWeatherTargetPlayerUuid(eligible.get(audience.targetIndex()).getStringUUID());
                }
            }
            case THUNDER_SILENT -> setWeather(overworld, true, true, duration + 120);
            case THUNDER_ARTIFICIAL -> setWeather(overworld, true, true, duration + 120);
            case THUNDER_TARGET_STRIKE -> {
                setWeather(overworld, true, true, duration + 120);
                for (ServerPlayer player : overworld.players()) {
                    spawnTargetStrike(player);
                }
            }
            case THUNDER_STROBOSCOPIC -> {
                setWeather(overworld, true, true, duration + 120);
                state.setWeatherAuxValue(5 + overworld.random.nextInt(6));
                state.setWeatherAuxTick(now + 4L);
            }
            case FOG_BREATHING, FOG_BLACK, FOG_STATIC_WALL -> {
                // Visual side handled by client tags; no base weather override.
            }
            case SKY_FAKE_MORNING -> {
                state.setWeatherSavedDayTime(overworld.getDayTime());
                overworld.setDayTime((overworld.getDayTime() / 24000L) * 24000L + 6000L);
            }
            case SKY_EMPTY -> setWeather(overworld, true, true, duration + 120);
            case SKY_PRESSURE -> {
                for (ServerPlayer player : players) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 60, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 60, 0, false, false, true));
                }
            }
            case RAIN_FRONT, SUSPENDED_RAIN, DRY_EYE, CLEAR_DOWNPOUR,
                    WRONG_SNOWLINE, LIGHT_AVOIDING_RAIN, CONVERGING_RAIN, LEAKING_SKY -> {
                // Local precipitation is presentation-only and synchronized below.
            }
        }

        long cooldownTicks = rollCooldownTicks(overworld, phaseIndex, profile, event.severityMultiplier);
        state.setWeatherCooldownUntilTick(now + duration + cooldownTicks);
        debugLog("WEATHER start event={} duration={}t cooldown={}t phase={} profile={}", event.id, duration, cooldownTicks, phaseIndex, profile);
        return true;
    }

    private static int applyVisualDurationRules(ServerLevel level, UncannyWorldState state, Event event, int rawDurationTicks) {
        if (!isHeavyVisualWeather(event)) {
            return rawDurationTicks;
        }

        int previousHeavyDuration = state.getLastHeavyVisualWeatherDurationTicks();
        UncannyWeatherPacingRules.IntRange durationRange =
                UncannyWeatherPacingRules.visualDurationRange(previousHeavyDuration);
        int adjustedDuration = durationRange.minInclusive() + level.random.nextInt(durationRange.size());

        state.setLastHeavyVisualWeatherDurationTicks(adjustedDuration);
        debugLog(
                "WEATHER visual-duration-rules event={} raw={}t adjusted={}t previousHeavy={}t",
                event.id,
                rawDurationTicks,
                adjustedDuration,
                previousHeavyDuration);
        return adjustedDuration;
    }

    private static boolean isHeavyVisualWeather(Event event) {
        return event.heavyVisual();
    }

    private static void tickActiveEvent(
            MinecraftServer server,
            UncannyWorldState state,
            Event event,
            long now,
            List<ServerPlayer> players) {
        ServerLevel overworld = server.overworld();

        switch (event) {
            case RAIN_SILENT -> {
                for (ServerPlayer player : players) {
                    if (player.level().dimension() == Level.OVERWORLD) {
                        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.WEATHER));
                    }
                }
            }
            case RAIN_DRY_STORM -> {
                if (state.getWeatherAuxValue() > 0 && now >= state.getWeatherAuxTick()) {
                    for (ServerPlayer player : players) {
                        if (canPlayRainLikeWeatherFor(player, Event.RAIN_DRY_STORM, now)) {
                            UncannySoundDelivery.playMental(
                                    player,
                                    SoundEvents.WEATHER_RAIN_ABOVE,
                                    SoundSource.WEATHER,
                                    UncannyWeatherTimingRules.dryRainVolume(overworld.random.nextInt()),
                                    UncannyWeatherTimingRules.dryRainPitch(overworld.random.nextInt()),
                                    UncannyWeatherTimingRules.DRY_RAIN_MAX_PULSE_DURATION_TICKS);
                        }
                    }
                    state.setWeatherAuxValue(state.getWeatherAuxValue() - 1);
                    state.setWeatherAuxTick(now + UncannyWeatherTimingRules.dryRainPulseGapTicks(overworld.random.nextInt()));
                }
            }
            case RAIN_ASH -> {
                if (now % 6L == 0L) {
                    for (ServerPlayer player : players) {
                        if (player.level().dimension() != Level.OVERWORLD) {
                            continue;
                        }
                        player.serverLevel().sendParticles(
                                net.minecraft.core.particles.ParticleTypes.ASH,
                                player.getX(),
                                player.getY() + 2.0D,
                                player.getZ(),
                                90,
                                10.0D,
                                3.2D,
                                10.0D,
                                0.016D);
                    }
                }
                if (now % 1L == 0L) {
                    for (ServerPlayer player : players) {
                        if (player.level().dimension() != Level.OVERWORLD) {
                            continue;
                        }
                        player.serverLevel().sendParticles(
                                net.minecraft.core.particles.ParticleTypes.ASH,
                                player.getX(),
                                player.getY() + 6.5D,
                                player.getZ(),
                                420,
                                18.0D,
                                6.0D,
                                18.0D,
                                0.045D);
                    }
                }
            }
            case RAIN_SOBBING -> {
                if (now % 90L == 0L) {
                    String targetUuid = state.getWeatherTargetPlayerUuid();
                    for (ServerPlayer player : players) {
                        if (!targetUuid.isBlank() && !targetUuid.equals(player.getStringUUID())) {
                            continue;
                        }
                        if (!canPlayRainLikeWeatherFor(player, Event.RAIN_SOBBING, now)) {
                            continue;
                        }
                        UncannySoundDelivery.playMental(
                                player,
                                UncannySoundRegistry.UNCANNY_WHISPER.get(),
                                SoundSource.AMBIENT,
                                0.22F,
                                0.92F + player.serverLevel().random.nextFloat() * 0.14F,
                                50);
                    }
                }
            }
            case THUNDER_SILENT -> {
                for (ServerPlayer player : players) {
                    if (player.level().dimension() == Level.OVERWORLD) {
                        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.WEATHER));
                    }
                }
                if (now % 55L == 0L) {
                    for (ServerPlayer player : overworld.players()) {
                        spawnVisualLightning(overworld, randomOffsetPos(player.blockPosition(), overworld, 6, 14));
                    }
                }
            }
            case THUNDER_ARTIFICIAL -> {
                if (now % 120L == 0L) {
                    for (ServerPlayer player : overworld.players()) {
                        if (overworld.random.nextFloat() < 0.40F) {
                            sendLocalSound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.55F, 0.80F + overworld.random.nextFloat() * 0.20F);
                        }
                        if (overworld.random.nextFloat() < 0.22F) {
                            spawnVisualLightning(overworld, randomOffsetPos(player.blockPosition(), overworld, 10, 26));
                        }
                    }
                }
            }
            case THUNDER_TARGET_STRIKE -> {
                if (now % 80L == 0L) {
                    for (ServerPlayer player : overworld.players()) {
                        spawnTargetStrike(player);
                    }
                }
            }
            case THUNDER_STROBOSCOPIC -> {
                if (now % 20L == 0L) {
                    for (ServerPlayer player : overworld.players()) {
                        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.WEATHER));
                    }
                }

                if (state.getWeatherAuxValue() > 0 && now >= state.getWeatherAuxTick()) {
                    for (ServerPlayer player : overworld.players()) {
                        spawnVisualLightning(overworld, randomOffsetPos(player.blockPosition(), overworld, 2, 8));
                    }
                    state.setWeatherAuxValue(state.getWeatherAuxValue() - 1);
                    state.setWeatherAuxTick(now + 8L);
                    if (state.getWeatherAuxValue() <= 0) {
                        if (state.getCurrentPhaseIndex() >= 3) {
                            for (ServerPlayer player : players) {
                                UncannyParanoiaEventSystem.triggerTotalBlackout(player);
                            }
                        }
                        state.setWeatherEventEndTick(now + 10L);
                    }
                }
            }
            case FOG_BREATHING -> {
                if (now % 95L == 0L) {
                    for (ServerPlayer player : players) {
                        sendLocalSound(player, UncannySoundRegistry.UNCANNY_MONSTER_BREATH.get(), SoundSource.AMBIENT, 0.24F, 0.92F + player.serverLevel().random.nextFloat() * 0.16F);
                    }
                }
            }
            case FOG_BLACK -> {
                // Render-only fog handled client-side.
            }
            case FOG_STATIC_WALL -> {
                for (ServerPlayer player : players) {
                    if (player.getDeltaMovement().horizontalDistanceSqr() > 0.004D && now % 36L == 0L) {
                        sendLocalSound(player, UncannySoundRegistry.UNCANNY_TINNITUS.get(), SoundSource.AMBIENT, 0.05F, 1.0F);
                    }
                }
            }
            case SKY_FAKE_MORNING -> {
                // Daytime set on start and restored on stop.
            }
            case SKY_EMPTY -> {
                if (now % 5L == 0L) {
                    for (ServerPlayer player : players) {
                        if (player.level().dimension() == Level.OVERWORLD) {
                            player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.WEATHER));
                        }
                    }
                }
            }
            case SKY_PRESSURE -> {
                if (now % 130L == 0L) {
                    for (ServerPlayer player : players) {
                        sendLocalSound(player, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.20F, 0.62F);
                    }
                }
            }
            case RAIN_FRONT, SUSPENDED_RAIN, DRY_EYE, CLEAR_DOWNPOUR,
                    WRONG_SNOWLINE, LIGHT_AVOIDING_RAIN, CONVERGING_RAIN, LEAKING_SKY -> {
                // Parameters are persisted and periodically resynchronized to observers.
            }
        }
    }

    private static void stopActiveEvent(MinecraftServer server, UncannyWorldState state, long now, boolean immediateReset) {
        Event activeEvent = Event.byId(state.getActiveWeatherEventId());
        if (activeEvent == Event.SKY_FAKE_MORNING && state.getWeatherSavedDayTime() != Long.MIN_VALUE) {
            server.overworld().setDayTime(state.getWeatherSavedDayTime());
        }

        if (activeEvent != null) {
            clearWeatherTags(server.getPlayerList().getPlayers());
            if (isLocalizedWeather(activeEvent)) {
                clearLocalizedWeather(server.getPlayerList().getPlayers(), activeEvent.id);
            }
        }

        if ((immediateReset || activeEvent != null)
                && (activeEvent == null || !isLocalizedWeather(activeEvent))) {
            setWeather(server.overworld(), false, false, 0);
        }

        state.setActiveWeatherEventId("");
        state.setWeatherEventEndTick(Long.MIN_VALUE);
        state.setWeatherAuxTick(Long.MIN_VALUE);
        state.setWeatherAuxValue(0);
        state.setWeatherTargetPlayerUuid("");
        state.setWeatherSavedDayTime(Long.MIN_VALUE);
        state.clearLocalizedWeather();
        state.setWeatherNextCheckTick(now + 60L + server.overworld().random.nextInt(81));
        if (activeEvent != null) {
            debugLog("WEATHER stop event={} immediateReset={}", activeEvent.id, immediateReset);
        }
    }

    private static void sanitizeWeatherTimers(MinecraftServer server, UncannyWorldState state, long now) {
        String activeId = state.getActiveWeatherEventId();
        boolean hasActive = activeId != null && !activeId.isBlank();
        if (hasActive) {
            return;
        }

        long cooldownUntil = state.getWeatherCooldownUntilTick();
        if (cooldownUntil != Long.MIN_VALUE) {
            long delta = cooldownUntil - now;
            if (delta > WEATHER_MAX_IDLE_COOLDOWN_TICKS) {
                state.setWeatherCooldownUntilTick(now + 20L * (20 + server.overworld().random.nextInt(35)));
                debugLog("WEATHER sanitize cooldown old={} now={} new={}", cooldownUntil, now, state.getWeatherCooldownUntilTick());
            } else if (delta < 0L) {
                // Expired cooldown is valid and should allow immediate scheduling checks.
                state.setWeatherCooldownUntilTick(now);
                debugLog("WEATHER sanitize cooldown-expired old={} now={} new={}", cooldownUntil, now, state.getWeatherCooldownUntilTick());
            }
        }

        long nextCheck = state.getWeatherNextCheckTick();
        if (nextCheck != Long.MIN_VALUE) {
            long delta = nextCheck - now;
            if (delta > WEATHER_MAX_IDLE_NEXT_CHECK_TICKS) {
                state.setWeatherNextCheckTick(now + 20L * (8 + server.overworld().random.nextInt(20)));
                debugLog("WEATHER sanitize next-check old={} now={} new={}", nextCheck, now, state.getWeatherNextCheckTick());
            } else if (delta < 0L) {
                // Expired next-check must not be pushed away indefinitely.
                state.setWeatherNextCheckTick(now);
                debugLog("WEATHER sanitize next-check-expired old={} now={} new={}", nextCheck, now, state.getWeatherNextCheckTick());
            }
        }

        long endTick = state.getWeatherEventEndTick();
        if (endTick != Long.MIN_VALUE && (endTick <= now || endTick - now > WEATHER_MAX_IDLE_COOLDOWN_TICKS)) {
            state.setWeatherEventEndTick(Long.MIN_VALUE);
            state.setWeatherAuxTick(Long.MIN_VALUE);
            state.setWeatherAuxValue(0);
            state.setWeatherSavedDayTime(Long.MIN_VALUE);
            debugLog("WEATHER sanitize event-end old={} now={}", endTick, now);
        }
    }

    private static Event rollEvent(ServerLevel level, int phaseIndex, int profile, int danger, String lastWeatherEventId) {
        List<WeightedWeatherEvent> candidates = new ArrayList<>();
        for (Event event : Event.values()) {
            if (phaseIndex < event.minPhase) {
                continue;
            }
            if (!isWeatherContextAvailable(level, event)) {
                continue;
            }
            if (lastWeatherEventId != null
                    && !lastWeatherEventId.isBlank()
                    && lastWeatherEventId.equals(event.id)
                    && Event.values().length > 1) {
                continue;
            }
            int weight = UncannyWeatherPacingRules.effectiveWeight(event, phaseIndex, profile, danger, true);
            if (weight > 0) {
                candidates.add(new WeightedWeatherEvent(event, weight));
                debugLog("WEATHER candidate event={} weight={} phase={} profile={} danger={}", event.id, weight, phaseIndex, profile, danger);
            }
        }

        if (candidates.isEmpty()) {
            if (lastWeatherEventId == null || lastWeatherEventId.isBlank()) {
                return null;
            }
            // Fallback when the anti-repeat filter eliminated everything.
            for (Event event : Event.values()) {
                if (phaseIndex < event.minPhase) {
                    continue;
                }
                if (!isWeatherContextAvailable(level, event)) {
                    continue;
                }
                int weight = UncannyWeatherPacingRules.effectiveWeight(event, phaseIndex, profile, danger, false);
                if (weight > 0) {
                    candidates.add(new WeightedWeatherEvent(event, weight));
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
        }

        int total = 0;
        for (WeightedWeatherEvent candidate : candidates) {
            total += candidate.weight;
        }
        int roll = level.random.nextInt(total);
        int acc = 0;
        for (WeightedWeatherEvent candidate : candidates) {
            acc += candidate.weight;
            if (roll < acc) {
                return candidate.event;
            }
        }
        return candidates.get(candidates.size() - 1).event;
    }

    private static long rollNextCheckDelayTicks(ServerLevel level, int phaseIndex, int profile) {
        UncannyWeatherPacingRules.IntRange range =
                UncannyWeatherPacingRules.nextCheckSecondsRange(phaseIndex, profile);
        int baseSeconds = range.minInclusive() + level.random.nextInt(range.size());
        boolean addLongDelay = level.random.nextFloat() < 0.15F;
        int longDelaySeconds = addLongDelay ? 8 + level.random.nextInt(28) : 0;
        boolean shortenDelay = level.random.nextFloat() < 0.20F;
        int shortDelaySeconds = shortenDelay ? 1 + level.random.nextInt(6) : 0;
        return UncannyWeatherPacingRules.nextCheckDelayTicks(
                baseSeconds, addLongDelay, longDelaySeconds, shortenDelay, shortDelaySeconds);
    }

    private static double rollTriggerChance(int phaseIndex, int profile) {
        return UncannyWeatherPacingRules.triggerChance(phaseIndex, profile);
    }

    private static long rollCooldownTicks(ServerLevel level, int phaseIndex, int profile, float severityScale) {
        UncannyWeatherPacingRules.IntRange range =
                UncannyWeatherPacingRules.cooldownSecondsRange(phaseIndex, profile, severityScale);
        int baseSeconds = range.minInclusive() + level.random.nextInt(range.size());
        boolean addLongDelay = level.random.nextFloat() < 0.20F;
        int longDelaySeconds = addLongDelay ? 12 + level.random.nextInt(34) : 0;
        return UncannyWeatherPacingRules.cooldownTicks(
                baseSeconds, addLongDelay, longDelaySeconds);
    }

    private static int getProfile() {
        return Math.max(1, Math.min(5, UncannyConfig.EVENT_INTENSITY_PROFILE.get()));
    }

    private static int getDangerLevel() {
        return Math.max(0, Math.min(5, UncannyConfig.EVENT_DANGER_LEVEL.get()));
    }

    private static void setWeather(ServerLevel level, boolean rain, boolean thunder, int durationTicks) {
        level.setWeatherParameters(rain ? 0 : durationTicks, rain ? durationTicks : 0, rain, thunder);
    }

    private static void syncWeatherTags(List<ServerPlayer> players, String weatherEventId) {
        for (ServerPlayer player : players) {
            if (player.level().dimension() == Level.OVERWORLD) {
                UncannyClientStateSync.syncWeather(player, weatherEventId);
            } else {
                UncannyClientStateSync.clearWeather(player);
            }
        }
    }

    private static void clearWeatherTags(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            UncannyClientStateSync.clearWeather(player);
        }
    }

    private static void spawnTargetStrike(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }

        BlockPos target = BlockPos.containing(player.getX() + forward.x * 2.0D, player.getY(), player.getZ() + forward.z * 2.0D);
        spawnVisualLightning(player.serverLevel(), target);
    }

    private static void spawnVisualLightning(ServerLevel level, BlockPos target) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        bolt.setVisualOnly(true);
        bolt.setSilent(true);
        level.addFreshEntity(bolt);
    }

    private static BlockPos randomOffsetPos(BlockPos origin, ServerLevel level, int minDistance, int maxDistance) {
        int radius = minDistance + level.random.nextInt(Math.max(1, maxDistance - minDistance + 1));
        int dx = level.random.nextBoolean() ? radius : -radius;
        int dz = level.random.nextBoolean() ? radius : -radius;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, origin.getX() + dx, origin.getZ() + dz);
        return new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
    }

    private static void sendLocalSound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                source,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                volume,
                pitch,
                player.level().random.nextLong()));
    }

    private static boolean canPlayRainLikeWeatherFor(ServerPlayer player, Event event, long now) {
        if (player == null || player.level().dimension() != Level.OVERWORLD) {
            return false;
        }
        var biome = player.serverLevel().getBiome(player.blockPosition()).value();
        boolean hasPrecipitation = biome.hasPrecipitation();
        float temperature = biome.getBaseTemperature();
        boolean allowsRain = hasPrecipitation && temperature >= 0.15F;
        if (UncannyConfig.DEBUG_LOGS.get() && now % 40L == 0L) {
            String biomePrecip = !hasPrecipitation ? "NONE" : (temperature < 0.15F ? "SNOW" : "RAIN");
            debugLog(
                    "WEATHER rain_audio_gate biomePrecip={} allow={} event={} player={} temp={}",
                    biomePrecip,
                    allowsRain,
                    event.id,
                    player.getScoreboardName(),
                    String.format(java.util.Locale.ROOT, "%.2f", temperature));
        }
        return allowsRain;
    }

    private static boolean isLocalizedWeather(Event event) {
        return switch (event) {
            case RAIN_FRONT, SUSPENDED_RAIN, DRY_EYE, CLEAR_DOWNPOUR,
                    WRONG_SNOWLINE, LIGHT_AVOIDING_RAIN, CONVERGING_RAIN, LEAKING_SKY -> true;
            default -> false;
        };
    }

    private static boolean isWeatherContextAvailable(ServerLevel level, Event event) {
        if (!isLocalizedWeather(event)) {
            return true;
        }
        return switch (event) {
            case CLEAR_DOWNPOUR -> !level.isRaining();
            case LIGHT_AVOIDING_RAIN -> level.isRaining()
                    && UncannyWorldState.get(level.getServer()).getPlayerPlacedLights().stream()
                    .anyMatch(pos -> level.hasChunkAt(pos) && isTrackedPlayerLight(level.getBlockState(pos)));
            default -> level.isRaining();
        };
    }

    private static boolean configureLocalizedWeather(
            MinecraftServer server,
            UncannyWorldState state,
            Event event,
            long now,
            List<ServerPlayer> players,
            int duration) {
        ServerLevel level = server.overworld();
        List<ServerPlayer> eligible = players.stream()
                .filter(player -> !player.isSpectator() && player.serverLevel() == level)
                .sorted(Comparator.comparing(ServerPlayer::getStringUUID))
                .toList();
        if (eligible.isEmpty() || !isWeatherContextAvailable(level, event)) {
            return false;
        }
        ServerPlayer target = eligible.get(level.random.nextInt(eligible.size()));
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        BlockPos center;
        int radius;
        String data = "";

        switch (event) {
            case RAIN_FRONT -> {
                center = findOutdoorWeatherAnchor(level, target, direction, 7, 11);
                radius = 18;
            }
            case SUSPENDED_RAIN -> {
                center = findOutdoorWeatherAnchor(level, target, direction, 5, 10);
                radius = 5;
            }
            case DRY_EYE -> {
                center = findOutdoorWeatherAnchor(level, target, direction, 10, 16);
                radius = 8 + level.random.nextInt(7);
            }
            case CLEAR_DOWNPOUR -> {
                center = findOutdoorWeatherAnchor(level, target, direction, 3, 8);
                radius = 10 + level.random.nextInt(5);
            }
            case WRONG_SNOWLINE -> {
                center = findOutdoorWeatherAnchor(level, target, direction, 2, 6);
                radius = 14;
            }
            case LIGHT_AVOIDING_RAIN -> {
                List<BlockPos> validLights = state.getPlayerPlacedLights().stream()
                        .filter(level::hasChunkAt)
                        .filter(pos -> isTrackedPlayerLight(level.getBlockState(pos)))
                        .filter(pos -> eligible.stream().anyMatch(player ->
                                player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 40.0D * 40.0D))
                        .toList();
                if (validLights.isEmpty()) {
                    return false;
                }
                int firstIndex = level.random.nextInt(validLights.size());
                int count = Math.min(validLights.size(), 1 + level.random.nextInt(3));
                List<BlockPos> selected = new ArrayList<>();
                for (int offset = 0; offset < validLights.size() && selected.size() < count; offset++) {
                    BlockPos candidate = validLights.get((firstIndex + offset) % validLights.size());
                    if (selected.stream().noneMatch(pos -> pos.closerThan(candidate, 4.0D))) {
                        selected.add(candidate);
                    }
                }
                if (selected.isEmpty()) {
                    selected.add(validLights.get(firstIndex));
                }
                center = selected.get(0);
                radius = 2;
                data = String.join(",", selected.stream().map(pos -> Long.toString(pos.asLong())).toList());
            }
            case CONVERGING_RAIN -> {
                center = findOutdoorWeatherAnchor(level, target, direction, 6, 12);
                radius = 6;
            }
            case LEAKING_SKY -> {
                LeakAnchor leak = findLeakAnchor(level, target);
                if (leak == null) {
                    return false;
                }
                center = leak.floorAir();
                radius = leak.height();
            }
            default -> {
                return false;
            }
        }
        if (center == null) {
            return false;
        }
        state.configureLocalizedWeather(
                center, direction.x, direction.z, radius, level.random.nextLong(), now, data);
        return true;
    }

    private static BlockPos findOutdoorWeatherAnchor(
            ServerLevel level,
            ServerPlayer target,
            Vec3 preferredDirection,
            int minimumDistance,
            int maximumDistance) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double variation = (level.random.nextDouble() - 0.5D) * Math.PI * 0.8D;
            double baseAngle = Math.atan2(preferredDirection.z, preferredDirection.x) + variation;
            int distance = minimumDistance + level.random.nextInt(maximumDistance - minimumDistance + 1);
            int x = target.blockPosition().getX() + Mth.floor(Math.cos(baseAngle) * distance);
            int z = target.blockPosition().getZ() + Mth.floor(Math.sin(baseAngle) * distance);
            BlockPos probe = new BlockPos(x, level.getSeaLevel(), z);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() && level.canSeeSky(pos)) {
                return pos;
            }
        }
        return null;
    }

    private static LeakAnchor findLeakAnchor(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 96; attempt++) {
            int x = origin.getX() + level.random.nextInt(25) - 12;
            int z = origin.getZ() + level.random.nextInt(25) - 12;
            for (int yOffset = -4; yOffset <= 3; yOffset++) {
                BlockPos floor = new BlockPos(x, origin.getY() + yOffset, z);
                if (!level.hasChunkAt(floor)
                        || !level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)) {
                    continue;
                }
                for (int height = 2; height <= 4; height++) {
                    boolean open = true;
                    for (int dy = 1; dy <= height; dy++) {
                        if (!level.getBlockState(floor.above(dy)).isAir()) {
                            open = false;
                            break;
                        }
                    }
                    BlockPos roof = floor.above(height + 1);
                    if (open && !level.getBlockState(roof).isAir()
                            && level.getBlockState(roof).isFaceSturdy(level, roof, net.minecraft.core.Direction.DOWN)
                            && level.canSeeSky(roof.above())) {
                        return new LeakAnchor(floor.above(), height);
                    }
                }
            }
        }
        return null;
    }

    private static void syncLocalizedWeather(
            MinecraftServer server,
            UncannyWorldState state,
            Event event,
            long now,
            List<ServerPlayer> players) {
        BlockPos center = state.getLocalizedWeatherCenter();
        long start = state.getLocalizedWeatherStartTick();
        int elapsed = start == Long.MIN_VALUE ? 0 : Mth.clamp((int) Math.max(0L, now - start), 0, Integer.MAX_VALUE);
        int remaining = Mth.clamp((int) Math.max(1L, state.getWeatherEventEndTick() - now), 1, Integer.MAX_VALUE);
        UncannyLocalizedWeatherPayload payload = new UncannyLocalizedWeatherPayload(
                event.id, true,
                center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D,
                state.getLocalizedWeatherDirectionX(), state.getLocalizedWeatherDirectionZ(),
                state.getLocalizedWeatherRadius(), state.getLocalizedWeatherSeed(),
                elapsed, remaining, state.getLocalizedWeatherData());
        for (ServerPlayer player : players) {
            if (player.serverLevel() == server.overworld()) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private static void clearLocalizedWeather(List<ServerPlayer> players, String eventId) {
        UncannyLocalizedWeatherPayload payload = new UncannyLocalizedWeatherPayload(
                eventId, false, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 1, 0L, 0, 1, "");
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static boolean isTrackedPlayerLight(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN);
    }

    private record LeakAnchor(BlockPos floorAir, int height) {
    }

    private record WeightedWeatherEvent(Event event, int weight) {
    }
}
