package com.carpentersblocks.client.render;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Which sides of a see-through Carpenter's block (bare frame, glass or leaves cover, doors...)
 * are hidden by its neighbours.
 * <p>
 * Such a block reports an empty occlusion shape so that neighbours keep drawing the faces
 * visible through it. Vanilla uses the same shape to decide whether the block's own faces
 * are hidden, and with an empty shape it hides them next to any solid neighbour, even a slab
 * that covers half the side. So those faces are drawn unculled and hidden here instead.
 */
public final class FaceCulling {
    private FaceCulling() {
    }

    /** Whether vanilla culling applies to the block (it has an opaque cover and a real occlusion shape). */
    public static boolean isOpaque(BlockState state) {
        return state.hasProperty(CoverableBlock.OPAQUE) && state.getValue(CoverableBlock.OPAQUE);
    }

    /**
     * The neighbour a block's face is tested against (vanilla {@code Block.shouldRenderFace}
     * and OptiFine's equivalent, through mixins): a Carpenter's block that is a full cube
     * counts as its cover. Real glass then hides its face against a glass-covered block, as
     * it does against glass, and stone hides its face against a stone-covered one.
     */
    public static BlockState neighbourForCulling(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CoverableBlock) {
            try {
                BlockState cover = FacadeLevel.hostCover(level, pos);
                if (cover != null && Block.isShapeFullBlock(state.getShape(level, pos))) {
                    BlockEntity be = level.getBlockEntity(pos);
                    // Side covers may differ from the host cover: leave those blocks as they are.
                    if (be instanceof CoverableBlockEntity && ((CoverableBlockEntity) be).getRenderData().hasOnlyHostCover()) {
                        return cover;
                    }
                }
            } catch (RuntimeException e) {
                return state;
            }
        }
        return state;
    }

    /** @return a bit per {@link Direction} ordinal, set when the neighbour hides that whole side */
    public static int hiddenSides(BlockAndTintGetter level, BlockPos pos, BlockState state, RenderData data) {
        if (isOpaque(state)) {
            return 0;
        }
        int hidden = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            at.setWithOffset(pos, dir);
            BlockState neighbour = level.getBlockState(at);
            if (neighbour.isAir()) {
                continue;
            }
            Direction back = dir.getOpposite();
            if (neighbour.canOcclude()
                && !Shapes.joinIsNotEmpty(Shapes.block(), neighbour.getFaceOcclusionShape(level, at, back), BooleanOp.ONLY_FIRST)) {
                hidden |= 1 << dir.ordinal(); // a solid full side, e.g. stone
                continue;
            }
            // Like glass next to glass: the same see-through cover on both sides of the join.
            BlockState cover = data.cover(data.faceLocation(AttributeLocation.of(Sides.toCore(dir))));
            if (cover == null) {
                continue;
            }
            BlockState shown = neighbour;
            if (neighbour.getBlock() instanceof CoverableBlock) {
                BlockEntity be = level.getBlockEntity(at);
                if (!(be instanceof CoverableBlockEntity) || !Block.isFaceFull(neighbour.getShape(level, at), back)) {
                    continue;
                }
                RenderData other = ((CoverableBlockEntity) be).getRenderData();
                shown = other.cover(other.faceLocation(AttributeLocation.of(Sides.toCore(back))));
            } else if (!Block.isFaceFull(neighbour.getShape(level, at), back)) {
                continue;
            }
            if (shown != null && cover.skipRendering(shown, dir)) {
                hidden |= 1 << dir.ordinal();
            }
        }
        return hidden;
    }
}
