package com.carpentersblocks.forge.mixin;

import com.carpentersblocks.client.render.FaceCulling;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * OptiFine replaces vanilla's "should this face be drawn" test with its own; this is
 * {@link BlockMixin} for that copy. Names are OptiFine's and the game's runtime (SRG) names:
 * OptiFine only exists in the real game, never in the development environment. If a future
 * OptiFine changes this method the mixin silently does nothing.
 */
@Pseudo
@Mixin(targets = "net.optifine.util.BlockUtils", remap = false)
public abstract class OptiFineBlockUtilsMixin {
    @Redirect(method = "shouldSideBeRendered", remap = false, require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockReader;func_180495_p(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", remap = false))
    private static BlockState carpentersblocks$neighbourForCulling(BlockGetter level, BlockPos pos) {
        return FaceCulling.neighbourForCulling(level, pos);
    }
}
