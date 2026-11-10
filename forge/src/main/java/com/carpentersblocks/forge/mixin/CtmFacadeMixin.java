package com.carpentersblocks.forge.mixin;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import team.chisel.ctm.api.IFacade;

/**
 * Makes Carpenter's blocks facades for the CTM mod (Chisel and others): a real block with a
 * connected texture joins up with a Carpenter's block covered with the same block. Only
 * applied when CTM is installed (see {@link ForgeMixinPlugin}); the other direction, covers
 * connecting to their neighbours, is done by the block model.
 */
@Mixin(value = CoverableBlock.class, remap = false)
public abstract class CtmFacadeMixin implements IFacade {
    @Override
    public BlockState getFacade(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        BlockState cover = be instanceof CoverableBlockEntity
            ? ((CoverableBlockEntity) be).getRenderData().cover(AttributeLocation.HOST) : null;
        return cover != null ? cover : level.getBlockState(pos);
    }
}
