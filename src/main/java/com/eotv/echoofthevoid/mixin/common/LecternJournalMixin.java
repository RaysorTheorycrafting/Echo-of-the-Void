package com.eotv.echoofthevoid.mixin.common;

import com.eotv.echoofthevoid.item.UncannyItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A lectern only counts the vanilla books as a book: with a journal on it, its menu closed the moment it opened
 * and its comparator signal stayed off. The mod's journals are written books in all but item, so they count too.
 */
@Mixin(LecternBlockEntity.class)
public abstract class LecternJournalMixin {
    @Shadow
    public abstract ItemStack getBook();

    @Inject(method = "hasBook", at = @At("RETURN"), cancellable = true)
    private void eotv$journalIsABook(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && getBook().is(UncannyItemRegistry.UNCANNY_LORE_PIECE.get())) {
            cir.setReturnValue(true);
        }
    }
}
