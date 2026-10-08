package com.eotv.echoofthevoid.event.paranoia.message;

import com.eotv.echoofthevoid.config.UncannyConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** Runtime pacing and non-repeating decks for contextual player-facing messages. */
public final class ParanoiaMessageService {
    public static final long GLOBAL_MINIMUM_COOLDOWN_TICKS = 20L * 60L * 20L;
    public static final long GLOBAL_MAXIMUM_COOLDOWN_TICKS = 45L * 60L * 20L;
    public static final long CONTEXT_COOLDOWN_TICKS = 90L * 60L * 20L;

    private static final Map<UUID, Long> NEXT_GLOBAL_MESSAGE_TICKS = new HashMap<>();
    private static final Map<UUID, Map<ParanoiaMessageContext, Long>> NEXT_CONTEXT_MESSAGE_TICKS = new HashMap<>();
    private static final Map<UUID, Map<String, Deque<String>>> MESSAGE_DECKS = new HashMap<>();
    private static final Map<UUID, String> LAST_MESSAGES = new HashMap<>();
    // At most one pending line per player: it waits for the event's effect to be perceived.
    private static final Map<UUID, PendingMessage> PENDING_MESSAGES = new HashMap<>();
    /** A queued line is dropped rather than delivered long after its event. */
    public static final long MAXIMUM_PENDING_LATENESS_TICKS = 20L * 10L;

    private ParanoiaMessageService() {
    }

    /**
     * Rolls a natural message for an event that has just started. The line itself is only drawn
     * and shown once the rule's delay has elapsed, so it follows the effect it describes.
     */
    public static boolean maybeQueueForEvent(ServerPlayer player, String eventId, long now) {
        Optional<ParanoiaMessageCatalog.MessageRule> optionalRule = ParanoiaMessageCatalog.ruleForEvent(eventId);
        if (optionalRule.isEmpty() || !canReceiveNaturalMessage(player)) {
            return false;
        }

        ParanoiaMessageCatalog.MessageRule rule = optionalRule.get();
        UUID playerId = player.getUUID();
        if (now < NEXT_GLOBAL_MESSAGE_TICKS.getOrDefault(playerId, Long.MIN_VALUE)) {
            return false;
        }
        long contextUntil = NEXT_CONTEXT_MESSAGE_TICKS
                .getOrDefault(playerId, Map.of())
                .getOrDefault(rule.context(), Long.MIN_VALUE);
        if (now < contextUntil || player.getRandom().nextDouble() >= rule.naturalChance()
                || PENDING_MESSAGES.containsKey(playerId)) {
            return false;
        }

        PENDING_MESSAGES.put(playerId, new PendingMessage(eventId, now + rule.delayTicks(), player.level().dimension()));
        long cooldownRange = GLOBAL_MAXIMUM_COOLDOWN_TICKS - GLOBAL_MINIMUM_COOLDOWN_TICKS;
        long globalCooldown = GLOBAL_MINIMUM_COOLDOWN_TICKS
                + (cooldownRange <= 0L ? 0L : player.getRandom().nextInt((int) cooldownRange + 1));
        NEXT_GLOBAL_MESSAGE_TICKS.put(playerId, now + globalCooldown);
        NEXT_CONTEXT_MESSAGE_TICKS.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(rule.context(), now + CONTEXT_COOLDOWN_TICKS);
        return true;
    }

    /** Delivers a queued event line once due; returns it so callers can arm follow-ups. */
    public static Optional<String> tickPending(ServerPlayer player, long now) {
        PendingMessage pending = player == null ? null : PENDING_MESSAGES.get(player.getUUID());
        if (pending == null || now < pending.deliverTick()) {
            return Optional.empty();
        }
        if (now - pending.deliverTick() > MAXIMUM_PENDING_LATENESS_TICKS
                || !player.level().dimension().equals(pending.dimension())
                || !player.isAlive()) {
            PENDING_MESSAGES.remove(player.getUUID());
            return Optional.empty();
        }
        if (!canReceiveNaturalMessage(player)) {
            // Wait out a menu or a fight instead of interrupting it, within the lateness bound.
            return Optional.empty();
        }
        PENDING_MESSAGES.remove(player.getUUID());
        ParanoiaMessageCatalog.MessageRule rule = ParanoiaMessageCatalog.ruleForEvent(pending.eventId()).orElse(null);
        if (rule == null) {
            return Optional.empty();
        }
        String text = draw(player, "event:" + pending.eventId(), rule.lines());
        if (text == null) {
            return Optional.empty();
        }
        sendStyled(player, text);
        return Optional.of(text);
    }

    public static boolean hasPending(UUID playerId) {
        return playerId != null && PENDING_MESSAGES.containsKey(playerId);
    }

    /** Developer/test delivery: bypass pacing while retaining context and deck rules. */
    public static Optional<String> sendForced(ServerPlayer player, ParanoiaMessageContext context) {
        if (player == null || context == null || !player.isAlive()) {
            return Optional.empty();
        }
        String text = draw(player, "context:" + context.name(), ParanoiaMessageCatalog.messages(context));
        if (text == null) {
            return Optional.empty();
        }
        sendStyled(player, text);
        return Optional.of(text);
    }

    /** Developer/test delivery of one event's own lines, bypassing pacing and delay. */
    public static Optional<String> sendForcedForEvent(ServerPlayer player, String eventId) {
        ParanoiaMessageCatalog.MessageRule rule = ParanoiaMessageCatalog.ruleForEvent(eventId).orElse(null);
        if (player == null || rule == null || !player.isAlive()) {
            return Optional.empty();
        }
        String text = draw(player, "event:" + eventId, rule.lines());
        if (text == null) {
            return Optional.empty();
        }
        sendStyled(player, text);
        return Optional.of(text);
    }

    public static String pickFalseRecipeBody(ServerPlayer player) {
        List<String> candidates = ParanoiaMessageCatalog.falseRecipeBodies();
        if (player == null || candidates.isEmpty()) {
            return "Recipe remembered.";
        }
        return candidates.get(player.getRandom().nextInt(candidates.size()));
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        NEXT_GLOBAL_MESSAGE_TICKS.remove(playerId);
        NEXT_CONTEXT_MESSAGE_TICKS.remove(playerId);
        MESSAGE_DECKS.remove(playerId);
        LAST_MESSAGES.remove(playerId);
        PENDING_MESSAGES.remove(playerId);
    }

    private static boolean canReceiveNaturalMessage(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isDeadOrDying()) {
            return false;
        }
        if (player.containerMenu != player.inventoryMenu) {
            return false;
        }
        boolean recentlyHurt = player.getLastHurtByMob() != null
                && player.tickCount - player.getLastHurtByMobTimestamp() <= 200;
        boolean recentlyAttacked = player.getLastHurtMob() != null
                && player.tickCount - player.getLastHurtMobTimestamp() <= 200;
        return !recentlyHurt && !recentlyAttacked;
    }

    private static String draw(ServerPlayer player, String deckKey, List<String> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        Map<String, Deque<String>> perDeck = MESSAGE_DECKS
                .computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        Deque<String> deck = perDeck.computeIfAbsent(deckKey, ignored -> new ArrayDeque<>());
        if (deck.isEmpty()) {
            refillDeck(player, source, deck);
        }

        String selected = deck.removeFirst();
        LAST_MESSAGES.put(player.getUUID(), selected);
        return selected;
    }

    private static void refillDeck(ServerPlayer player, List<String> source, Deque<String> target) {
        List<String> shuffled = new ArrayList<>(source);
        for (int index = shuffled.size() - 1; index > 0; index--) {
            Collections.swap(shuffled, index, player.getRandom().nextInt(index + 1));
        }

        String previous = LAST_MESSAGES.get(player.getUUID());
        if (previous != null && shuffled.size() > 1 && previous.equals(shuffled.get(0))) {
            Collections.swap(shuffled, 0, 1);
        }
        target.addAll(shuffled);
    }

    private static void sendStyled(ServerPlayer player, String text) {
        List<? extends String> configuredColors = UncannyConfig.CORRUPT_MESSAGE_COLORS.get();
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

    private record PendingMessage(
            String eventId,
            long deliverTick,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
    }
}
