package com.eotv.echoofthevoid.event.special;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.config.UncannyConfig;
import com.eotv.echoofthevoid.entity.UncannyEntityRegistry;
import com.eotv.echoofthevoid.entity.custom.UncannyFriendEntity;
import com.eotv.echoofthevoid.event.paranoia.UncannyDimensionPolicy;
import com.eotv.echoofthevoid.mixin.common.PlayerInfoUpdatePacketAccessor;
import com.eotv.echoofthevoid.state.OldFriendRecord;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import com.eotv.echoofthevoid.world.ObserverSight;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * "Joined the Game", and its very rare variant: an old friend of the host. The friend is a real
 * player found in another save; it "joins", stays in the tab list like any player for a long while,
 * and the first time it is seen is when it comes for its target with copies of the target's gear.
 * If it kills the target it leaves the game and a sign saying "Sorry" where the target fell; if it
 * dies, it leaves a few seconds later. Once over, it never happens again in this world.
 */
public final class OldFriendSystem {
    private static final int LATENCY_MS = 47;
    private static final long RESPAWN_IF_MISSING_TICKS = 20L * 30L;
    private static final List<PendingLine> PENDING_LINES = new ArrayList<>();
    private static long missingFriendTicks;
    private static boolean skinRequested;
    private static volatile boolean resolving;
    private static boolean attackWhenJoined;

    private OldFriendSystem() {
    }

    private record PendingLine(long tick, UUID recipient, Component line, UUID removeFromTab) {
    }

    // ------------------------------------------------------------------ "joined the game" (self)

    /** Solo only: the player's own name joins, and leaves a minute or two later. */
    public static boolean triggerSelfEcho(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || server.getPlayerList().getPlayerCount() != 1) {
            return false;
        }
        Component name = Component.literal(player.getGameProfile().getName());
        player.sendSystemMessage(joined(name));
        long delay = OldFriendRules.MIN_SELF_ECHO_TICKS + player.getRandom().nextInt(
                OldFriendRules.MAX_SELF_ECHO_TICKS - OldFriendRules.MIN_SELF_ECHO_TICKS + 1);
        PENDING_LINES.add(new PendingLine(server.getTickCount() + delay, player.getUUID(), left(name), null));
        return true;
    }

    // ------------------------------------------------------------------ the old friend

    public static boolean triggerJoin(ServerPlayer target, boolean debugImmediate) {
        MinecraftServer server = target.getServer();
        if (server == null || !UncannyConfig.OLD_FRIEND_ENABLED.get()) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        if (state.getOldFriend() != null) {
            return false;
        }
        Set<UUID> excluded = new HashSet<>();
        server.getPlayerList().getPlayers().forEach(player -> excluded.add(player.getUUID()));
        if (server.getSingleplayerProfile() != null) {
            excluded.add(server.getSingleplayerProfile().getId());
        }
        if (resolving) {
            return false;
        }
        List<OldFriendFiles.Friend> friends = OldFriendFinder.find(server, excluded);
        if (friends.isEmpty()) {
            debug("no old friend found in the host's other saves");
            return false;
        }
        long quiet = debugImmediate ? 20L * 30L : OldFriendRules.quietTicks(target.getRandom().nextDouble());
        List<OldFriendFiles.Friend> named = friends.stream().filter(OldFriendFiles.Friend::named).toList();
        if (!named.isEmpty()) {
            OldFriendFiles.Friend friend = named.get(target.getRandom().nextInt(named.size()));
            join(server, target, friend.id(), friend.name(), quiet, "", true);
            return true;
        }
        // Only ids are known locally: Mojang's session service names them (and gives the skin), off-thread.
        // A few at most, those who played the most first: the service throttles bursts of requests.
        List<UUID> ids = friends.stream().map(OldFriendFiles.Friend::id).limit(OldFriendRules.MAX_NAME_LOOKUPS).toList();
        resolveAndJoin(server, target.getUUID(), ids, quiet, debugImmediate);
        return true;
    }

    /** Names the first id the session service knows, then joins on the server thread. Offline: nothing happens. */
    private static void resolveAndJoin(MinecraftServer server, UUID targetId, List<UUID> ids, long quiet, boolean debugImmediate) {
        resolving = true;
        debug("resolving {} nameless friend(s) through the session service", ids.size());
        CompletableFuture.supplyAsync(() -> {
            for (UUID id : ids) {
                try {
                    ProfileResult result = server.getSessionService().fetchProfile(id, true);
                    if (result != null && result.profile().getName() != null && !result.profile().getName().isBlank()) {
                        return result.profile();
                    }
                } catch (RuntimeException ignored) {
                    // Unreachable or unknown: try the next one.
                }
            }
            return null;
        }, Util.backgroundExecutor()).thenAcceptAsync(profile -> {
            resolving = false;
            ServerPlayer target = server.getPlayerList().getPlayer(targetId);
            UncannyWorldState state = UncannyWorldState.get(server);
            if (profile == null) {
                debug("no nameless friend could be named (offline, or unknown to Mojang)");
                if (debugImmediate && target != null) {
                    target.displayClientMessage(Component.literal("Old Friend: no name found for the players of your other saves (offline?)"), false);
                }
                return;
            }
            if (target == null || state.getOldFriend() != null) {
                return;
            }
            join(server, target, profile.getId(), profile.getName(), quiet, texturesOf(profile), true);
            if (attackWhenJoined) {
                attackWhenJoined = false;
                forceAttack(target);
            }
        }, server);
    }

    private static String texturesOf(GameProfile profile) {
        Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
        return textures == null ? "" : textures.value() + "|" + (textures.signature() == null ? "" : textures.signature());
    }

    /** GameTests: the same arrival with a given friend, since a test server has no other saves. */
    public static void joinForTest(ServerPlayer target, UUID friendId, String friendName, long quietTicks) {
        join(target.getServer(), target, friendId, friendName, quietTicks, "", false);
    }

    private static void join(MinecraftServer server, ServerPlayer target, UUID friendId, String friendName,
            long quiet, String textures, boolean fetchSkin) {
        OldFriendRecord record = new OldFriendRecord(friendId, friendName, target.getUUID(),
                OldFriendRecord.Stage.PRESENT, quiet, textures);
        UncannyWorldState.get(server).setOldFriend(record);
        server.getPlayerList().broadcastSystemMessage(joined(Component.literal(friendName)), false);
        broadcast(server, addToTab(record));
        if (fetchSkin && textures.isEmpty()) {
            requestSkin(server, record);
        } else {
            skinRequested = true;
        }
        debug("{} joined for {} (attack in {}s)", friendName, target.getGameProfile().getName(), quiet / 20L);
    }

    /** Dev entry: the attack now, joining first if needed. */
    public static boolean forceAttack(ServerPlayer target) {
        MinecraftServer server = target.getServer();
        if (server == null) {
            return false;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        OldFriendRecord record = state.getOldFriend();
        if (record == null) {
            if (!triggerJoin(target, true)) {
                return false;
            }
            record = state.getOldFriend();
            if (record == null) {
                // Its name is being asked to the session service: it comes the moment it is known.
                attackWhenJoined = true;
                return true;
            }
        }
        if (record.stage() == OldFriendRecord.Stage.DONE) {
            return false;
        }
        record = new OldFriendRecord(record.friendId(), record.friendName(), target.getUUID(), record.stage(), 0L,
                record.textures());
        state.setOldFriend(record);
        return spawnAttack(target.serverLevel(), target, record, state);
    }

    /** Dev entry: forget this world's old friend so the event can be tested again. */
    public static boolean reset(MinecraftServer server) {
        UncannyWorldState state = UncannyWorldState.get(server);
        OldFriendRecord record = state.getOldFriend();
        if (record == null) {
            return false;
        }
        removeFriendEntities(server, record, true);
        broadcast(server, new ClientboundPlayerInfoRemovePacket(List.of(record.friendId())));
        state.setOldFriend(null);
        return true;
    }

    public static void tick(MinecraftServer server, long now) {
        deliverLines(server, now);
        if (now % 20L != 0L) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(server);
        OldFriendRecord record = state.getOldFriend();
        if (record == null || record.stage() == OldFriendRecord.Stage.DONE) {
            return;
        }
        ServerPlayer target = server.getPlayerList().getPlayer(record.targetId());
        if (target == null || !target.isAlive() || !UncannyDimensionPolicy.runsOrdinaryScheduler(target.serverLevel())) {
            return;
        }
        if (!skinRequested && record.textures().isEmpty()) {
            requestSkin(server, record);
        }
        if (record.stage() == OldFriendRecord.Stage.PRESENT) {
            long remaining = record.remainingTicks() - 20L;
            if (remaining > 0L) {
                state.setOldFriend(record.withRemaining(remaining));
            } else if (!spawnAttack(target.serverLevel(), target, record.withRemaining(0L), state)) {
                state.setOldFriend(record.withRemaining(20L * 30L));
            }
            return;
        }
        if (findFriend(target.serverLevel(), record, target.position(), 160.0D) != null) {
            missingFriendTicks = 0L;
            return;
        }
        missingFriendTicks += 20L;
        if (missingFriendTicks >= RESPAWN_IF_MISSING_TICKS) {
            missingFriendTicks = 0L;
            spawnAttack(target.serverLevel(), target, record, state);
        }
    }

    /** GameTests: the attack from a given spot (the unseen-spot search depends on terrain outside the room). */
    public static boolean attackFromForTest(ServerPlayer target, BlockPos spot) {
        UncannyWorldState state = UncannyWorldState.get(target.getServer());
        OldFriendRecord record = state.getOldFriend();
        return record != null && spawnAttackAt(target.serverLevel(), target, record, state, spot);
    }

    private static boolean spawnAttack(ServerLevel level, ServerPlayer target, OldFriendRecord record, UncannyWorldState state) {
        BlockPos spot = findUnseenSpot(level, target);
        if (spot == null) {
            debug("no unseen spot to attack {}", target.getGameProfile().getName());
            return false;
        }
        return spawnAttackAt(level, target, record, state, spot);
    }

    private static boolean spawnAttackAt(ServerLevel level, ServerPlayer target, OldFriendRecord record,
            UncannyWorldState state, BlockPos spot) {
        UncannyFriendEntity friend = UncannyEntityRegistry.UNCANNY_FRIEND.get().create(level);
        if (friend == null) {
            return false;
        }
        friend.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, target.getRandom().nextFloat() * 360.0F, 0.0F);
        friend.setup(record.friendId(), record.friendName(), target);
        if (!level.addFreshEntity(friend)) {
            return false;
        }
        state.setOldFriend(record.withStage(OldFriendRecord.Stage.ATTACKING));
        debug("{} comes for {} from {}", record.friendName(), target.getGameProfile().getName(), spot);
        return true;
    }

    private static BlockPos findUnseenSpot(ServerLevel level, ServerPlayer target) {
        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = target.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = OldFriendRules.MIN_SPAWN_DISTANCE
                    + target.getRandom().nextDouble() * (OldFriendRules.MAX_SPAWN_DISTANCE - OldFriendRules.MIN_SPAWN_DISTANCE);
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            for (int dy = 4; dy >= -4; dy--) {
                BlockPos feet = new BlockPos(x, target.getBlockY() + dy, z);
                if (!level.isLoaded(feet) || !isStandable(level, feet)) {
                    continue;
                }
                Vec3 body = Vec3.atBottomCenterOf(feet);
                if (!ObserverSight.isSeenByAny(level, List.of(body.add(0.0D, 0.9D, 0.0D), body.add(0.0D, 1.7D, 0.0D)),
                        level.players(), 10.0D, pos -> false)) {
                    return feet;
                }
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty();
    }

    private static UncannyFriendEntity findFriend(ServerLevel level, OldFriendRecord record, Vec3 around, double radius) {
        for (UncannyFriendEntity friend : level.getEntitiesOfClass(UncannyFriendEntity.class,
                new AABB(around, around).inflate(radius))) {
            if (friend.isAlive() && friend.profileId().map(record.friendId()::equals).orElse(false)) {
                return friend;
            }
        }
        return null;
    }

    private static void removeFriendEntities(MinecraftServer server, OldFriendRecord record, boolean restore) {
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> found = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof UncannyFriendEntity friend
                        && friend.profileId().map(record.friendId()::equals).orElse(false)) {
                    found.add(entity);
                }
            }
            found.forEach(Entity::discard);
        }
    }

    // ------------------------------------------------------------------ outcomes

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        UncannyWorldState state = UncannyWorldState.get(server);
        OldFriendRecord record = state.getOldFriend();
        if (record == null || record.stage() != OldFriendRecord.Stage.ATTACKING) {
            return;
        }
        long now = server.getTickCount();
        Component name = Component.literal(record.friendName());
        if (event.getEntity() instanceof UncannyFriendEntity friend
                && friend.profileId().map(record.friendId()::equals).orElse(false)) {
            state.setOldFriend(record.withStage(OldFriendRecord.Stage.DONE));
            long delay = OldFriendRules.MIN_LEAVE_DELAY_TICKS + level.random.nextInt(
                    OldFriendRules.MAX_LEAVE_DELAY_TICKS - OldFriendRules.MIN_LEAVE_DELAY_TICKS + 1);
            PENDING_LINES.add(new PendingLine(now + delay, null, left(name), record.friendId()));
            debug("{} was killed; leaves in {} ticks", record.friendName(), delay);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && player.getUUID().equals(record.targetId())) {
            placeSorrySign(level, player.blockPosition(), player.getYRot());
            removeFriendEntities(server, record, true);
            state.setOldFriend(record.withStage(OldFriendRecord.Stage.DONE));
            PENDING_LINES.add(new PendingLine(now + 30L, null, left(name), record.friendId()));
            debug("{} killed {}; sign left at {}", record.friendName(), player.getGameProfile().getName(), player.blockPosition());
        }
    }

    /** A standing oak sign reading "Sorry" where the target fell, on the nearest spot that can hold it. */
    public static BlockPos placeSorrySign(ServerLevel level, BlockPos death, float yaw) {
        List<BlockPos> spots = new ArrayList<>(List.of(death, death.above()));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            spots.add(death.relative(direction));
        }
        int rotation = Math.floorMod(Math.round((yaw + 180.0F) * 16.0F / 360.0F), 16);
        BlockState sign = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation);
        for (BlockPos spot : spots) {
            BlockState current = level.getBlockState(spot);
            if (!current.canBeReplaced() || !level.getFluidState(spot).isEmpty() || !sign.canSurvive(level, spot)) {
                continue;
            }
            level.setBlock(spot, sign, 3);
            if (level.getBlockEntity(spot) instanceof SignBlockEntity entity) {
                entity.setText(new SignText().setMessage(1, Component.literal("Sorry")), true);
                entity.setWaxed(true);
                entity.setChanged();
                level.sendBlockUpdated(spot, sign, sign, 3);
            }
            return spot;
        }
        return null;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        UncannyWorldState state = UncannyWorldState.get(player.getServer());
        OldFriendRecord record = state.getOldFriend();
        if (record == null || record.stage() == OldFriendRecord.Stage.DONE) {
            return;
        }
        if (player.getUUID().equals(record.friendId())) {
            // The real friend is here: the imitation must never coexist with them.
            removeFriendEntities(player.getServer(), record, true);
            state.setOldFriend(record.withStage(OldFriendRecord.Stage.DONE));
            return;
        }
        player.connection.send(addToTab(record));
    }

    // ------------------------------------------------------------------ tab list and chat

    private static Packet<?> addToTab(OldFriendRecord record) {
        GameProfile profile = new GameProfile(record.friendId(), record.friendName());
        String[] textures = record.textures().split("\\|", 2);
        if (textures.length == 2 && !textures[0].isEmpty()) {
            profile.getProperties().put("textures", new Property("textures", textures[0], textures[1].isEmpty() ? null : textures[1]));
        }
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), List.of());
        ((PlayerInfoUpdatePacketAccessor) packet).eotv$setEntries(List.of(new ClientboundPlayerInfoUpdatePacket.Entry(
                record.friendId(), profile, true, LATENCY_MS, GameType.SURVIVAL, null, null)));
        return packet;
    }

    /** The real skin needs Mojang's session service; fetched off-thread, offline worlds keep a default skin. */
    private static void requestSkin(MinecraftServer server, OldFriendRecord record) {
        skinRequested = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                ProfileResult result = server.getSessionService().fetchProfile(record.friendId(), true);
                return result == null ? "" : texturesOf(result.profile());
            } catch (RuntimeException exception) {
                return "";
            }
        }, Util.backgroundExecutor()).thenAcceptAsync(textures -> {
            if (textures.isEmpty()) {
                return;
            }
            UncannyWorldState state = UncannyWorldState.get(server);
            OldFriendRecord current = state.getOldFriend();
            if (current == null || !current.friendId().equals(record.friendId())
                    || current.stage() == OldFriendRecord.Stage.DONE) {
                return;
            }
            OldFriendRecord updated = current.withTextures(textures);
            state.setOldFriend(updated);
            // A tab entry cannot change skin in place: remove and add it again, silently.
            broadcast(server, new ClientboundPlayerInfoRemovePacket(List.of(updated.friendId())));
            broadcast(server, addToTab(updated));
        }, server);
    }

    private static void deliverLines(MinecraftServer server, long now) {
        Iterator<PendingLine> iterator = PENDING_LINES.iterator();
        while (iterator.hasNext()) {
            PendingLine line = iterator.next();
            if (now < line.tick()) {
                continue;
            }
            iterator.remove();
            if (line.recipient() == null) {
                server.getPlayerList().broadcastSystemMessage(line.line(), false);
            } else {
                ServerPlayer player = server.getPlayerList().getPlayer(line.recipient());
                if (player != null) {
                    player.sendSystemMessage(line.line());
                }
            }
            if (line.removeFromTab() != null) {
                broadcast(server, new ClientboundPlayerInfoRemovePacket(List.of(line.removeFromTab())));
            }
        }
    }

    private static void broadcast(MinecraftServer server, Packet<?> packet) {
        server.getPlayerList().broadcastAll(packet);
    }

    private static Component joined(Component name) {
        return Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW);
    }

    private static Component left(Component name) {
        return Component.translatable("multiplayer.player.left", name).withStyle(ChatFormatting.YELLOW);
    }

    public static void clear() {
        PENDING_LINES.clear();
        missingFriendTicks = 0L;
        skinRequested = false;
        resolving = false;
        attackWhenJoined = false;
    }

    public static String describe(MinecraftServer server) {
        OldFriendRecord record = UncannyWorldState.get(server).getOldFriend();
        if (record == null) {
            return "Old friend: none yet.";
        }
        return "Old friend: " + record.friendName() + " stage=" + record.stage() + " attackIn="
                + record.remainingTicks() / 20L + "s skin=" + (record.textures().isEmpty() ? "default" : "fetched");
    }

    private static void debug(String message, Object... args) {
        if (UncannyConfig.DEBUG_LOGS.get()) {
            EchoOfTheVoid.LOGGER.info("[UncannyDebug/OldFriend] " + message, args);
        }
    }
}
