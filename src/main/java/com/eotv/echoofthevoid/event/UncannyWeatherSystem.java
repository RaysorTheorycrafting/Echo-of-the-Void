package com.eotv.echoofthevoid.event;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import java.util.ArrayList;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class UncannyWeatherSystem {
    private static final long WEATHER_TICK_INTERVAL = 1L;
    private static final int WEATHER_VISUAL_MIN_DURATION_TICKS = 20 * 30;
    private static final int WEATHER_VISUAL_MAX_DURATION_TICKS = 20 * 60 * 2;
    private static final int WEATHER_VISUAL_LONG_THRESHOLD_TICKS = 20 * 90;
    private static final int WEATHER_VISUAL_SHORT_MIN_TICKS = 20 * 12;
    private static final int WEATHER_VISUAL_SHORT_CAP_TICKS = 20 * 22;
    private static final long WEATHER_MAX_IDLE_COOLDOWN_TICKS = 20L * 60L * 12L;
    private static final long WEATHER_MAX_IDLE_NEXT_CHECK_TICKS = 20L * 60L * 8L;

    private UncannyWeatherSystem() {
    }

    private static void debugLog(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/Weather] " + message, args);
        }
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

        if (!state.isSystemEnabled() || phaseIndex < 2) {
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

        WeatherEvent activeEvent = WeatherEvent.byId(state.getActiveWeatherEventId());
        if (activeEvent != null) {
            syncWeatherTags(allPlayers, activeEvent.id);
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
        WeatherEvent selected = rollEvent(server.overworld(), phaseIndex, profile, danger, state.getLastWeatherEventId());
        if (selected == null) {
            debugLog("WEATHER no-candidate-selected phase={} profile={} danger={}", phaseIndex, profile, danger);
            return;
        }

        debugLog("WEATHER selected event={} phase={} profile={} danger={}", selected.id, phaseIndex, profile, danger);
        startEvent(server, state, selected, now, allPlayers, phaseIndex, profile);
    }

    public static boolean forceTrigger(MinecraftServer server, String eventId) {
        WeatherEvent event = WeatherEvent.byId(eventId);
        if (server == null || event == null) {
            debugLog("WEATHER force-trigger failed id={} serverNull={} eventNull={}", eventId, server == null, event == null);
            return false;
        }

        UncannyWorldState state = UncannyWorldState.get(server);
        long now = server.getTickCount();
        stopActiveEvent(server, state, now, true);
        startEvent(
                server,
                state,
                event,
                now,
                server.getPlayerList().getPlayers(),
                state.getCurrentPhaseIndex(),
                getProfile());
        debugLog("WEATHER force-trigger success id={}", eventId);
        return true;
    }

    public static void forceStop(MinecraftServer server) {
        if (server == null) {
            return;
        }
        stopActiveEvent(server, UncannyWorldState.get(server), server.getTickCount(), true);
        clearWeatherTags(server.getPlayerList().getPlayers());
        debugLog("WEATHER force-stop");
    }

    private static void startEvent(
            MinecraftServer server,
            UncannyWorldState state,
            WeatherEvent event,
            long now,
            List<ServerPlayer> players,
            int phaseIndex,
            int profile) {
        ServerLevel overworld = server.overworld();
        int duration = event.minDurationTicks + overworld.random.nextInt(event.maxDurationTicks - event.minDurationTicks + 1);
        duration = applyVisualDurationRules(overworld, state, event, duration);

        state.setActiveWeatherEventId(event.id);
        state.setLastWeatherEventId(event.id);
        state.setWeatherEventEndTick(now + duration);
        state.setWeatherAuxTick(now);
        state.setWeatherAuxValue(0);
        state.setWeatherSavedDayTime(Long.MIN_VALUE);
        syncWeatherTags(players, event.id);

        switch (event) {
            case RAIN_SILENT -> setWeather(overworld, true, false, duration + 120);
            case RAIN_DRY_STORM -> setWeather(overworld, false, false, 0);
            case RAIN_ASH -> setWeather(overworld, false, false, 0);
            case RAIN_SOBBING -> setWeather(overworld, true, false, duration + 120);
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
        }

        long cooldownTicks = rollCooldownTicks(overworld, phaseIndex, profile, event.severityMultiplier);
        state.setWeatherCooldownUntilTick(now + duration + cooldownTicks);
        debugLog("WEATHER start event={} duration={}t cooldown={}t phase={} profile={}", event.id, duration, cooldownTicks, phaseIndex, profile);
    }

    private static int applyVisualDurationRules(ServerLevel level, UncannyWorldState state, WeatherEvent event, int rawDurationTicks) {
        if (!isHeavyVisualWeather(event)) {
            return rawDurationTicks;
        }

        int previousHeavyDuration = state.getLastHeavyVisualWeatherDurationTicks();
        int adjustedDuration;
        if (previousHeavyDuration >= WEATHER_VISUAL_LONG_THRESHOLD_TICKS) {
            // After one long heavy-visual weather, force the next heavy one to stay short.
            adjustedDuration = WEATHER_VISUAL_SHORT_MIN_TICKS
                    + level.random.nextInt(WEATHER_VISUAL_SHORT_CAP_TICKS - WEATHER_VISUAL_SHORT_MIN_TICKS + 1);
        } else {
            // Heavy-visual weathers use a wide random window to avoid predictable long spam.
            adjustedDuration = WEATHER_VISUAL_MIN_DURATION_TICKS
                    + level.random.nextInt(WEATHER_VISUAL_MAX_DURATION_TICKS - WEATHER_VISUAL_MIN_DURATION_TICKS + 1);
        }

        state.setLastHeavyVisualWeatherDurationTicks(adjustedDuration);
        debugLog(
                "WEATHER visual-duration-rules event={} raw={}t adjusted={}t previousHeavy={}t",
                event.id,
                rawDurationTicks,
                adjustedDuration,
                previousHeavyDuration);
        return adjustedDuration;
    }

    private static boolean isHeavyVisualWeather(WeatherEvent event) {
        return switch (event) {
            case FOG_BREATHING, FOG_BLACK, FOG_STATIC_WALL, THUNDER_STROBOSCOPIC -> true;
            default -> false;
        };
    }

    private static void tickActiveEvent(
            MinecraftServer server,
            UncannyWorldState state,
            WeatherEvent event,
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
                if (now % 24L == 0L) {
                    for (ServerPlayer player : players) {
                        if (canPlayRainLikeWeatherFor(player, WeatherEvent.RAIN_DRY_STORM, now)) {
                            sendHeadLockedWeatherSound(player, SoundEvents.WEATHER_RAIN_ABOVE, 0.70F, 0.94F);
                        }
                    }
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
                    for (ServerPlayer player : players) {
                        if (!canPlayRainLikeWeatherFor(player, WeatherEvent.RAIN_SOBBING, now)) {
                            continue;
                        }
                        sendLocalSound(player, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.42F, 0.86F);
                        sendLocalSound(player, UncannySoundRegistry.UNCANNY_WHISPER.get(), SoundSource.AMBIENT, 0.22F, 0.92F + player.serverLevel().random.nextFloat() * 0.14F);
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
        }
    }

    private static void stopActiveEvent(MinecraftServer server, UncannyWorldState state, long now, boolean immediateReset) {
        WeatherEvent activeEvent = WeatherEvent.byId(state.getActiveWeatherEventId());
        if (activeEvent == WeatherEvent.SKY_FAKE_MORNING && state.getWeatherSavedDayTime() != Long.MIN_VALUE) {
            server.overworld().setDayTime(state.getWeatherSavedDayTime());
        }

        if (activeEvent != null) {
            clearWeatherTags(server.getPlayerList().getPlayers());
        }

        if (immediateReset || activeEvent != null) {
            setWeather(server.overworld(), false, false, 0);
        }

        state.setActiveWeatherEventId("");
        state.setWeatherEventEndTick(Long.MIN_VALUE);
        state.setWeatherAuxTick(Long.MIN_VALUE);
        state.setWeatherAuxValue(0);
        state.setWeatherSavedDayTime(Long.MIN_VALUE);
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

    private static WeatherEvent rollEvent(ServerLevel level, int phaseIndex, int profile, int danger, String lastWeatherEventId) {
        List<WeightedWeatherEvent> candidates = new ArrayList<>();
        for (WeatherEvent event : WeatherEvent.values()) {
            if (phaseIndex < event.minPhase) {
                continue;
            }
            if (lastWeatherEventId != null
                    && !lastWeatherEventId.isBlank()
                    && lastWeatherEventId.equals(event.id)
                    && WeatherEvent.values().length > 1) {
                continue;
            }
            int weight = Math.max(0, Math.round(event.baseWeight * profileWeightMultiplier(profile) * dangerWeightMultiplier(event, danger)));
            if (isHeavyVisualWeather(event)) {
                float limiter = switch (phaseIndex) {
                    case 1, 2 -> 0.35F;
                    case 3 -> 0.42F;
                    default -> 0.50F;
                };
                weight = Math.max(1, Math.round(weight * limiter));
            }
            if (phaseIndex >= 4 && event.minPhase >= 3) {
                weight += Math.max(1, weight / 4);
            }
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
            for (WeatherEvent event : WeatherEvent.values()) {
                if (phaseIndex < event.minPhase) {
                    continue;
                }
                int weight = Math.max(0, Math.round(event.baseWeight * profileWeightMultiplier(profile) * dangerWeightMultiplier(event, danger)));
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
        int minSeconds = switch (phaseIndex) {
            case 1 -> 12;
            case 2 -> 10;
            case 3 -> 8;
            case 4 -> 6;
            default -> 10;
        };
        int maxSeconds = switch (phaseIndex) {
            case 1 -> 28;
            case 2 -> 24;
            case 3 -> 20;
            case 4 -> 16;
            default -> 26;
        };
        float profileScale = switch (profile) {
            case 1 -> 1.20F;
            case 2 -> 1.08F;
            case 4 -> 0.88F;
            case 5 -> 0.72F;
            default -> 1.0F;
        };
        int min = Math.max(2, Mth.floor(minSeconds * profileScale));
        int max = Math.max(min + 1, Mth.floor(maxSeconds * profileScale));
        long delay = (min + level.random.nextInt(max - min + 1)) * 20L;
        if (level.random.nextFloat() < 0.15F) {
            delay += (8L + level.random.nextInt(28)) * 20L;
        }
        if (level.random.nextFloat() < 0.20F) {
            delay = Math.max(4L * 20L, delay - (1L + level.random.nextInt(6)) * 20L);
        }
        return Math.max(4L * 20L, Math.round(delay * 1.25D));
    }

    private static double rollTriggerChance(int phaseIndex, int profile) {
        double base = switch (phaseIndex) {
            case 1 -> 0.12D;
            case 2 -> 0.24D;
            case 3 -> 0.32D;
            case 4 -> 0.40D;
            default -> 0.0D;
        };
        return Mth.clamp(base * profileChanceMultiplier(profile), 0.02D, 0.62D);
    }

    private static long rollCooldownTicks(ServerLevel level, int phaseIndex, int profile, float severityScale) {
        int minSeconds = Mth.floor(70 * severityScale);
        int maxSeconds = Mth.floor(180 * severityScale);

        float phaseScale = switch (phaseIndex) {
            case 1 -> 1.15F;
            case 2 -> 1.00F;
            case 3 -> 0.86F;
            case 4 -> 0.74F;
            default -> 1.0F;
        };
        float profileScale = switch (profile) {
            case 1 -> 1.30F;
            case 2 -> 1.10F;
            case 4 -> 0.82F;
            case 5 -> 0.70F;
            default -> 1.0F;
        };

        int min = Math.max(24, Mth.floor(minSeconds * phaseScale * profileScale));
        int max = Math.max(min + 12, Mth.floor(maxSeconds * phaseScale * profileScale));
        long cooldown = (min + level.random.nextInt(max - min + 1)) * 20L;
        if (level.random.nextFloat() < 0.20F) {
            cooldown += (12L + level.random.nextInt(34)) * 20L;
        }
        return Math.max(24L * 20L, Math.round(cooldown * 1.30D));
    }

    private static int getProfile() {
        return Math.max(1, Math.min(5, UncannyConfig.EVENT_INTENSITY_PROFILE.get()));
    }

    private static int getDangerLevel() {
        return Math.max(0, Math.min(5, UncannyConfig.EVENT_DANGER_LEVEL.get()));
    }

    private static float profileWeightMultiplier(int profile) {
        return switch (profile) {
            case 1 -> 0.82F;
            case 2 -> 0.92F;
            case 4 -> 1.20F;
            case 5 -> 1.42F;
            default -> 1.0F;
        };
    }

    private static float profileChanceMultiplier(int profile) {
        return switch (profile) {
            case 1 -> 0.92F;
            case 2 -> 1.06F;
            case 4 -> 1.34F;
            case 5 -> 1.62F;
            default -> 1.0F;
        };
    }

    private static float dangerWeightMultiplier(WeatherEvent event, int danger) {
        if (danger <= 0) {
            return switch (event) {
                case THUNDER_TARGET_STRIKE, THUNDER_STROBOSCOPIC, SKY_PRESSURE -> 0.0F;
                case THUNDER_SILENT, THUNDER_ARTIFICIAL, FOG_BLACK, FOG_STATIC_WALL, SKY_EMPTY -> 0.45F;
                default -> 1.45F;
            };
        }

        float dangerBoost = switch (danger) {
            case 1 -> 0.72F;
            case 2 -> 0.88F;
            case 4 -> 1.20F;
            case 5 -> 1.42F;
            default -> 1.0F;
        };

        return switch (event) {
            case THUNDER_TARGET_STRIKE, THUNDER_STROBOSCOPIC, SKY_PRESSURE -> dangerBoost;
            case THUNDER_SILENT, THUNDER_ARTIFICIAL, FOG_BLACK, FOG_STATIC_WALL, SKY_EMPTY -> 0.82F + (dangerBoost - 1.0F) * 0.85F;
            default -> 1.16F - (dangerBoost - 1.0F) * 0.55F;
        };
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

    private static void sendHeadLockedWeatherSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                SoundSource.WEATHER,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                volume,
                pitch,
                player.level().random.nextLong()));
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

    private static boolean canPlayRainLikeWeatherFor(ServerPlayer player, WeatherEvent event, long now) {
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

    private enum WeatherEvent {
        RAIN_SILENT("rain_silent", 1, 20 * 90, 20 * 180, 12, 1.0F),
        RAIN_DRY_STORM("rain_dry_storm", 1, 20 * 90, 20 * 180, 10, 1.0F),
        RAIN_ASH("rain_ash", 1, 20 * 90, 20 * 180, 11, 1.05F),
        RAIN_SOBBING("rain_sobbing", 1, 20 * 90, 20 * 180, 10, 1.0F),
        THUNDER_SILENT("thunder_silent", 3, 20 * 70, 20 * 150, 8, 1.2F),
        THUNDER_ARTIFICIAL("thunder_artificial", 3, 20 * 70, 20 * 150, 7, 1.2F),
        THUNDER_TARGET_STRIKE("thunder_target_strike", 3, 20 * 20, 20 * 45, 6, 1.25F),
        THUNDER_STROBOSCOPIC("thunder_stroboscopic", 3, 20 * 25, 20 * 55, 5, 1.45F),
        FOG_BREATHING("fog_breathing", 2, 20 * 45, 20 * 120, 9, 1.0F),
        FOG_BLACK("fog_black", 3, 20 * 45, 20 * 120, 6, 1.1F),
        FOG_STATIC_WALL("fog_static_wall", 3, 20 * 45, 20 * 120, 7, 1.1F),
        SKY_FAKE_MORNING("sky_fake_morning", 3, 20 * 6, 20 * 15, 4, 1.3F),
        SKY_EMPTY("sky_empty", 3, 20 * 70, 20 * 150, 5, 1.2F),
        SKY_PRESSURE("sky_pressure", 3, 20 * 35, 20 * 60, 5, 1.35F);

        private final String id;
        private final int minPhase;
        private final int minDurationTicks;
        private final int maxDurationTicks;
        private final int baseWeight;
        private final float severityMultiplier;

        WeatherEvent(
                String id,
                int minPhase,
                int minDurationTicks,
                int maxDurationTicks,
                int baseWeight,
                float severityMultiplier) {
            this.id = id;
            this.minPhase = minPhase;
            this.minDurationTicks = minDurationTicks;
            this.maxDurationTicks = maxDurationTicks;
            this.baseWeight = baseWeight;
            this.severityMultiplier = severityMultiplier;
        }

        private static WeatherEvent byId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            for (WeatherEvent value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return null;
        }
    }

    private record WeightedWeatherEvent(WeatherEvent event, int weight) {
    }
}
