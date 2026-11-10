package com.carpentersblocks.fabric.mixin;

import com.carpentersblocks.client.render.FaceCulling;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A block deciding whether to draw a face sees a full, covered Carpenter's block as its cover
 * (see {@link FaceCulling#neighbourForCulling}): glass hides its face against a glass-covered
 * block instead of showing a pane inside the wall.
 */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Redirect(method = "shouldRenderFace", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState carpentersblocks$neighbourForCulling(BlockGetter level, BlockPos pos) {
        return FaceCulling.neighbourForCulling(level, pos);
    }
}
