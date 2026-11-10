package com.carpentersblocks.forge.mixin;

import com.carpentersblocks.client.render.FacadeLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When OptiFine checks whether a real block's texture connects to its neighbour, a covered
 * Carpenter's block answers as its cover: real glass joins a glass-covered block. (Covers
 * connecting to their own neighbours is done by the block model, see
 * {@code OptiFineConnectedTextures}.) Runtime (SRG) names, as in {@link OptiFineBlockUtilsMixin}.
 */
@Pseudo
@Mixin(targets = "net.optifine.ConnectedTextures", remap = false)
public abstract class OptiFineConnectedTexturesMixin {
    @Redirect(method = { "isNeighbour", "isNeighbourMatching", "isNeighbourOverlay" }, remap = false, require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockReader;func_180495_p(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", remap = false))
    private static BlockState carpentersblocks$coverOrState(BlockGetter level, BlockPos pos) {
        return FacadeLevel.coverOrState(level, pos);
    }
}
