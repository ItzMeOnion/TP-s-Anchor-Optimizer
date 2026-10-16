package com.tps.anchoroptimiser.mixin;

import com.tps.anchoroptimiser.Rules;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Makes a placed disguised block use the look-alike's model. */
@Mixin(BlockModelShaper.class)
public abstract class BlockModelShaperMixin {
    @ModifyVariable(method = "getBlockModel", at = @At("HEAD"), argsOnly = true)
    private BlockState tpao$swapState(BlockState state) {
        return Rules.swapState(state);
    }
}
