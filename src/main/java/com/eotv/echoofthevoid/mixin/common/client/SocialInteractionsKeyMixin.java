package com.eotv.echoofthevoid.mixin.common.client;

import com.eotv.echoofthevoid.client.OldFriendSocialScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The social interactions key works in a solo world while someone else is listed in the tab list:
 * that only happens while the old friend is "connected", so the screen closes again with the event.
 */
@Mixin(Minecraft.class)
public abstract class SocialInteractionsKeyMixin {
    @Redirect(method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isMultiplayerServer()Z"))
    private boolean eotv$socialScreenWhileSomeoneElseIsListed(Minecraft minecraft) {
        return ((MinecraftMultiplayerInvoker) minecraft).eotv$isMultiplayerServer()
                || OldFriendSocialScreen.someoneElseListedInSolo(minecraft);
    }
}
