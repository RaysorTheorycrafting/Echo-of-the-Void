package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.entity.UncannyEntityMarker;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Bounded, session-only memory of harmless physical sounds available to Echoer?. */
public final class HuntingSpecialSoundMemory {
    private static final Map<ResourceKey<Level>, ArrayDeque<Memory>> MEMORIES = new HashMap<>();
    private static final Map<ResourceKey<Level>, ArrayDeque<ReplayMarker>> REPLAY_MARKERS = new HashMap<>();
    private static final double PLAYER_SOURCE_RADIUS = 3.0D;

    private HuntingSpecialSoundMemory() {
    }

    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // Doors, chests and placed blocks are played at a position; the player who caused
            // them stands right there.
            boolean playerMade = level.getNearestPlayer(
                    event.getPosition().x, event.getPosition().y, event.getPosition().z,
                    PLAYER_SOURCE_RADIUS, false) != null;
            record(level, event.getPosition(), event.getSound(), playerMade, event.getNewVolume(), event.getNewPitch());
        }
    }

    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getLevel() instanceof ServerLevel level
                && !(event.getEntity() instanceof UncannyEntityMarker)) {
            record(level, event.getEntity().position(), event.getSound(),
                    event.getEntity() instanceof Player, event.getNewVolume(), event.getNewPitch());
        }
    }

    /**
     * Newest remembered sound near {@code origin}. A sound the player made is preferred over any
     * other: hearing one's own door or block behind oneself is the whole point of the decoy.
     * Footsteps are always the newest player sound, so a distinctive one (door, chest, placed
     * block) wins whenever the player made one recently; steps come next, any sound last.
     */
    public static Memory newestBefore(
            ServerLevel level,
            Vec3 origin,
            long beforeTickExclusive,
            double maximumDistance) {
        ArrayDeque<Memory> memories = MEMORIES.get(level.dimension());
        if (memories == null) {
            return null;
        }
        prune(level, memories);
        double maximumDistanceSqr = maximumDistance * maximumDistance;
        Memory playerStep = null;
        Memory fallback = null;
        Iterator<Memory> iterator = memories.descendingIterator();
        while (iterator.hasNext()) {
            Memory memory = iterator.next();
            if (memory.tick() < beforeTickExclusive
                    && memory.position().distanceToSqr(origin) <= maximumDistanceSqr) {
                if (memory.playerMade() && !isStepSound(memory.sound())) {
                    return memory;
                }
                if (memory.playerMade() && playerStep == null) {
                    playerStep = memory;
                }
                if (fallback == null) {
                    fallback = memory;
                }
            }
        }
        return playerStep != null ? playerStep : fallback;
    }

    public static void replay(
            ServerLevel level,
            Vec3 position,
            Holder<SoundEvent> sound,
            float volume,
            float pitch) {
        replay(level, position, sound, SoundSource.AMBIENT, volume, pitch);
    }

    public static void replay(
            ServerLevel level,
            Vec3 position,
            Holder<SoundEvent> sound,
            SoundSource source,
            float volume,
            float pitch) {
        if (level == null || position == null || sound == null) {
            return;
        }
        String path = path(sound);
        ArrayDeque<ReplayMarker> markers = REPLAY_MARKERS.computeIfAbsent(
                level.dimension(), ignored -> new ArrayDeque<>());
        markers.addLast(new ReplayMarker(position, path, level.getGameTime()));
        while (markers.size() > 16) {
            markers.removeFirst();
        }
        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                sound,
                source,
                volume,
                pitch);
    }

    public static boolean isStepSound(Holder<SoundEvent> sound) {
        return path(sound).contains("step");
    }

    /** Used by every physical-memory collector so a decoy can never become its own source. */
    public static boolean isReplayEmission(
            ServerLevel level,
            Vec3 position,
            Holder<SoundEvent> sound) {
        if (level == null || position == null || sound == null) {
            return false;
        }
        ArrayDeque<ReplayMarker> markers = REPLAY_MARKERS.get(level.dimension());
        if (markers == null) {
            return false;
        }
        long now = level.getGameTime();
        while (!markers.isEmpty() && now - markers.peekFirst().tick() > 2L) {
            markers.removeFirst();
        }
        String path = path(sound);
        for (ReplayMarker marker : markers) {
            if (marker.path().equals(path) && marker.position().distanceToSqr(position) < 0.04D) {
                return true;
            }
        }
        return false;
    }

    public static int memoryCount(ServerLevel level) {
        ArrayDeque<Memory> memories = MEMORIES.get(level.dimension());
        if (memories == null) {
            return 0;
        }
        prune(level, memories);
        return memories.size();
    }

    public static void clear() {
        MEMORIES.clear();
        REPLAY_MARKERS.clear();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    private static void record(
            ServerLevel level,
            Vec3 position,
            Holder<SoundEvent> sound,
            boolean playerMade,
            float volume,
            float pitch) {
        if (sound == null
                || isReplayEmission(level, position, sound)
                || !HuntingSpecialRules.isSafeEchoSoundPath(path(sound))) {
            return;
        }
        ArrayDeque<Memory> memories = MEMORIES.computeIfAbsent(
                level.dimension(), ignored -> new ArrayDeque<>());
        memories.addLast(new Memory(position, sound, level.getGameTime(), playerMade, volume, pitch));
        while (memories.size() > HuntingSpecialRules.ECHOER_MEMORY_CAPACITY) {
            memories.removeFirst();
        }
    }

    private static void prune(ServerLevel level, ArrayDeque<Memory> memories) {
        long now = level.getGameTime();
        while (!memories.isEmpty()
                && now - memories.peekFirst().tick() > HuntingSpecialRules.ECHOER_MEMORY_TTL_TICKS) {
            memories.removeFirst();
        }
    }

    private static String path(Holder<SoundEvent> sound) {
        return sound.unwrapKey()
                .map(key -> key.location().getPath().toLowerCase(Locale.ROOT))
                .orElse("");
    }

    public record Memory(
            Vec3 position,
            Holder<SoundEvent> sound,
            long tick,
            boolean playerMade,
            float volume,
            float pitch) {
    }

    private record ReplayMarker(Vec3 position, String path, long tick) {
    }
}
