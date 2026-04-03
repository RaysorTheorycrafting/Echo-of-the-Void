package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.network.UncannyDevMenuActionPayload;
import com.eotv.echoofthevoid.network.UncannyDevMenuQaStatusPayload;
import com.eotv.echoofthevoid.network.UncannyDevMenuSyncPayload;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UncannyDevMenuClientState {
    private static final Set<String> GREEN_IDS = new HashSet<>();
    private static final Set<String> ORANGE_IDS = new HashSet<>();

    private UncannyDevMenuClientState() {
    }

    public static synchronized void applySync(UncannyDevMenuSyncPayload payload) {
        GREEN_IDS.clear();
        ORANGE_IDS.clear();
        parseInto(payload.greenIds(), GREEN_IDS);
        parseInto(payload.orangeIds(), ORANGE_IDS);

        if (payload.openMenu()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(new UncannyDevMenuScreen());
            }
        }
    }

    public static synchronized UncannyDevCatalog.QaStatus statusOf(String entryId) {
        String normalized = normalize(entryId);
        if (GREEN_IDS.contains(normalized)) {
            return UncannyDevCatalog.QaStatus.GREEN;
        }
        if (ORANGE_IDS.contains(normalized)) {
            return UncannyDevCatalog.QaStatus.ORANGE;
        }
        return UncannyDevCatalog.QaStatus.GRAY;
    }

    public static void requestTrigger(String entryId) {
        PacketDistributor.sendToServer(new UncannyDevMenuActionPayload(entryId));
    }

    public static void requestSetGreen(String entryId, boolean green) {
        PacketDistributor.sendToServer(new UncannyDevMenuQaStatusPayload(entryId, green));
    }

    private static void parseInto(String raw, Set<String> destination) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String[] split = raw.split(";");
        for (String part : split) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                destination.add(normalized);
            }
        }
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
