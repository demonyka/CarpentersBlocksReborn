package com.carpentersblocks.client.render;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * The world as connected-texture code should see it: every covered Carpenter's block reports
 * its host cover instead of itself. A glass-covered block then counts as glass for its
 * neighbours' connections, which is how OptiFine and the CTM mod decide what joins up.
 * <p>
 * Used on chunk-building threads, like the level it wraps.
 */
public final class FacadeLevel implements BlockAndTintGetter {
    private final BlockAndTintGetter level;
    private final BlockPos selfPos;
    private final BlockState selfState;

    /**
     * @param selfPos   position of the block being drawn
     * @param selfState the cover being drawn there (a side cover may differ from the host cover)
     */
    public FacadeLevel(BlockAndTintGetter level, BlockPos selfPos, BlockState selfState) {
        this.level = level;
        this.selfPos = selfPos;
        this.selfState = selfState;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.equals(selfPos)) {
            return selfState;
        }
        return coverOrState(level, pos);
    }

    /** What stands at {@code pos} for connected textures: a Carpenter's block's host cover, else the block itself. */
    public static BlockState coverOrState(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CoverableBlock) {
            BlockState cover = hostCover(level, pos);
            if (cover != null) {
                return cover;
            }
        }
        return state;
    }

    /** The host cover of the Carpenter's block at {@code pos}, or null. */
    public static BlockState hostCover(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CoverableBlockEntity
            ? ((CoverableBlockEntity) be).getRenderData().cover(AttributeLocation.HOST) : null;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null; // the facade is a plain block
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return level.getFluidState(pos);
    }

    @Override
    public float getShade(net.minecraft.core.Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return level.getBlockTint(pos, resolver);
    }
}
