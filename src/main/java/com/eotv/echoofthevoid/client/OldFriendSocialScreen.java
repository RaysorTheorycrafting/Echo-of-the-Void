package com.eotv.echoofthevoid.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Vanilla closes the social interactions screen in a world that is not open to others. In a solo
 * world nobody but the player can be listed, except the old friend while its event runs: then the
 * screen opens like on a server, and closes again as soon as the friend leaves the list.
 */
public final class OldFriendSocialScreen {
    private OldFriendSocialScreen() {
    }

    public static boolean someoneElseListedInSolo(Minecraft minecraft) {
        ClientPacketListener connection = minecraft.getConnection();
        if (!minecraft.isLocalServer() || connection == null || minecraft.player == null) {
            return false;
        }
        UUID self = minecraft.player.getUUID();
        return connection.getListedOnlinePlayers().stream().anyMatch(info -> !info.getProfile().getId().equals(self));
    }
}
