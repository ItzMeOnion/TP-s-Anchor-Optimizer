package com.tps.anchoroptimiser.mixin;

import com.tps.anchoroptimiser.Rules;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Uses the look-alike's render layer. This is what makes a disguised spawner properly
 * see-through in the gaps: the game picks the "cutout" layer for real spawners only.
 */
@Mixin(ItemBlockRenderTypes.class)
public abstract class ItemBlockRenderTypesMixin {
    @ModifyVariable(method = "getChunkRenderType", at = @At("HEAD"), argsOnly = true)
    private static BlockState tpao$swapChunkLayer(BlockState state) {
        return Rules.swapState(state);
    }

    @ModifyVariable(method = "getMovingBlockRenderType", at = @At("HEAD"), argsOnly = true, require = 0)
    private static BlockState tpao$swapMovingLayer(BlockState state) {
        return Rules.swapState(state);
    }
}
