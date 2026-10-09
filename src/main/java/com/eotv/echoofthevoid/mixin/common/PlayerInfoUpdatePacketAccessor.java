package com.eotv.echoofthevoid.mixin.common;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The tab-list packet only offers constructors for real {@code ServerPlayer}s. The old friend is a
 * profile without a server player, so its entry is written into an otherwise empty packet.
 */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoUpdatePacketAccessor {
    @Mutable
    @Accessor("entries")
    void eotv$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
