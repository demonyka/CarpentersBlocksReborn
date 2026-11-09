package com.carpentersblocks.block;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Orientation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import java.util.List;

/**
 * A block attached to a face of its support (1.7.10 {@code BlockSided}): buttons, levers,
 * pressure plates, daylight sensors, torches. {@link #FACING} points away from the support.
 * Models are designed lying on the floor and rotated onto the support.
 */
public abstract class AttachedBlock extends BoxedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected AttachedBlock(Properties properties) {
        super(properties);
    }

    protected AttachedBlock(Properties properties, java.util.function.ToIntFunction<BlockState> ownLight) {
        super(properties, ownLight);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** Which faces of a support this block may hang on (direction away from the support). */
    protected boolean canFace(Direction facing) {
        return true;
    }

    public static Side support(BlockState state) {
        return Sides.toCore(state.getValue(FACING).getOpposite());
    }

    /** A floor-frame box rotated onto the support of {@code state}. */
    protected static Box attach(BlockState state, Box floorBox) {
        return floorBox.attachedTo(support(state));
    }

    protected static List<Polygon> attach(BlockState state, List<Polygon> floorPolygons) {
        Side support = support(state);
        return Orientation.polygons(floorPolygons, p -> Orientation.attach(p, support));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return canFace(facing) && level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState(); // drops through the loot table; covers drop in onRemove
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** Only the base cover, as in 1.7.10. */
    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost();
    }

    /** Uncovered attached blocks show the plain solid texture (1.7.10 icon_uncovered_solid). */
    @Override
    public net.minecraft.resources.ResourceLocation uncoveredTexture(BlockState state, Polygon polygon) {
        return com.carpentersblocks.util.UncoveredTextures.SOLID;
    }
}
