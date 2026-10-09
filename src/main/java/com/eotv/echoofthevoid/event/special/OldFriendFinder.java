package com.eotv.echoofthevoid.event.special;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Finds "old friends": real players who have a player file in another save of this host (another
 * singleplayer world, or another world folder of the server). Read-only, bounded, and never touches the
 * network; players without a local name are named later by {@link OldFriendSystem} via the session service.
 */
public final class OldFriendFinder {
    private OldFriendFinder() {
    }

    public static List<OldFriendFiles.Friend> find(MinecraftServer server, Set<UUID> excluded) {
        return OldFriendFiles.candidates(
                server.getServerDirectory().toAbsolutePath().normalize(),
                server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(),
                excluded);
    }
}
