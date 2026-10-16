package com.tps.anchoroptimiser.mixin;

import com.tps.anchoroptimiser.Rules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Renames disguised items (hover tooltip, hotbar pop-up, inventory) and hides the real id line. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void tpao$name(CallbackInfoReturnable<Component> cir) {
        Component name = Rules.nameFor((ItemStack) (Object) this);
        if (name != null) {
            cir.setReturnValue(name);
        }
    }

    @ModifyVariable(method = "getTooltipLines", at = @At("HEAD"), argsOnly = true, require = 0)
    private TooltipFlag tpao$flag(TooltipFlag flag) {
        return Rules.tooltipFlag((ItemStack) (Object) this, flag);
    }
}
