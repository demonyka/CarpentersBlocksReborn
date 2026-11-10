package com.carpentersblocks.client.render;

import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.attribute.AttributeLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Colours our quads: a dye wins; otherwise the cover block colours itself (grass, leaves, ...). */
public final class CoverBlockColor implements BlockColor {
    @Override
    public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        if (level == null || pos == null || tintIndex < 0) {
            return -1;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CoverableBlockEntity)) {
            return -1;
        }
        RenderData data = ((CoverableBlockEntity) be).getRenderData();
        AttributeLocation location = TintIndex.location(tintIndex);
        int dye = data.dye(location);
        if (dye != -1) {
            return dye;
        }
        BlockState cover = data.cover(location);
        int coverTint = TintIndex.coverTint(tintIndex);
        if (cover != null && coverTint >= 0) {
            try {
                return Minecraft.getInstance().getBlockColors().getColor(cover, level, pos, coverTint);
            } catch (RuntimeException e) {
                return -1; // a modded colour provider that expects its own block at this position
            }
        }
        return -1;
    }
}
