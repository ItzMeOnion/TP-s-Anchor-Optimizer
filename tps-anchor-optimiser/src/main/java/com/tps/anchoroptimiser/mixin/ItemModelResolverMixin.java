package com.tps.anchoroptimiser.mixin;

import com.tps.anchoroptimiser.Rules;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps the model used to draw a disguised item (inventory, hotbar, hand, dropped, item frames). */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true)
    private ItemStack tpao$swapStack(ItemStack stack) {
        return Rules.swapStack(stack);
    }
}
