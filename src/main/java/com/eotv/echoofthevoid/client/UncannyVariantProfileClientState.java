package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.event.passive.VanillaVariantBehaviorRuntime;
import com.eotv.echoofthevoid.network.UncannyVariantProfilePayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Applies server-authoritative variant presentation tags, with a bounded packet-order buffer. */
public final class UncannyVariantProfileClientState {
    private static final String LEGACY_PREFIX = "eotv_passive_";
    private static final long PENDING_LIFETIME_TICKS = 200L;
    private static final Map<Integer, PendingProfile> PENDING = new HashMap<>();
    private static ClientLevel trackedLevel;

    private UncannyVariantProfileClientState() {
    }

    public static void apply(UncannyVariantProfilePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !isAllowedTag(payload.presentationTag())) {
            return;
        }
        if (applyToEntity(level.getEntity(payload.entityId()), payload.presentationTag())) {
            PENDING.remove(payload.entityId());
            return;
        }
        PENDING.put(payload.entityId(), new PendingProfile(
                payload.presentationTag(), level.getGameTime() + PENDING_LIFETIME_TICKS));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            PENDING.clear();
            trackedLevel = level;
        }
        if (level == null || PENDING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, PendingProfile>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PendingProfile> entry = iterator.next();
            PendingProfile pending = entry.getValue();
            if (now >= pending.expireTick()
                    || applyToEntity(level.getEntity(entry.getKey()), pending.presentationTag())) {
                iterator.remove();
            }
        }
    }

    static boolean isAllowedTag(String tag) {
        return tag != null && (tag.isEmpty()
                || tag.startsWith(VanillaVariantBehaviorRuntime.VISUAL_TAG_PREFIX)
                || tag.startsWith(LEGACY_PREFIX));
    }

    private static boolean applyToEntity(Entity entity, String presentationTag) {
        if (entity == null) {
            return false;
        }
        entity.getTags().removeIf(tag -> tag.startsWith(VanillaVariantBehaviorRuntime.VISUAL_TAG_PREFIX)
                || tag.startsWith(LEGACY_PREFIX));
        if (!presentationTag.isEmpty()) {
            entity.addTag(presentationTag);
        }
        return true;
    }

    private record PendingProfile(String presentationTag, long expireTick) {
    }
}
