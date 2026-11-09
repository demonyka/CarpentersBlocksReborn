package com.carpentersblocks.block;

import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.shape.CollapsibleShape;
import com.carpentersblocks.interaction.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collapsible Block: four corner heights, adjusted with the hammer (left lowers, right raises
 * the nearest corner, matching corners of neighbouring collapsibles follow). Heights live in
 * the block entity, so the block never claims to hide its neighbours' faces.
 */
public class CollapsibleBlock extends CoverableBlock {
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
    /** Marks block entity data as set (0 alone would be "all corners at zero"). */
    private static final int SET = 1 << 20;

    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public CollapsibleBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(HANGING, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HANGING);
    }

    private static int packed(int shapeData) {
        return (shapeData & SET) != 0 ? shapeData & (SET - 1) : CollapsibleShape.FULL;
    }

    private static int packed(BlockGetter level, BlockPos pos) {
        BlockEntity be = level == null ? null : level.getBlockEntity(pos);
        return be instanceof CoverableBlockEntity ? packed(((CoverableBlockEntity) be).getShapeData()) : CollapsibleShape.FULL;
    }

    @Override
    public List<Polygon> getRenderPolygons(BlockState state) {
        return CollapsibleShape.polygons(CollapsibleShape.FULL, state.getValue(HANGING));
    }

    @Override
    public List<Polygon> getRenderPolygons(BlockState state, RenderData data) {
        return CollapsibleShape.polygons(packed(data.shape), state.getValue(HANGING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean hanging = state.getValue(HANGING);
        int packed = packed(level, pos);
        return SHAPES.computeIfAbsent(packed << 1 | (hanging ? 1 : 0),
            k -> BoxedBlock.union(CollapsibleShape.collisionBoxes(packed, hanging)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        double hitY = context.getClickLocation().y - context.getClickedPos().getY();
        boolean hanging = face == Direction.DOWN || face.getAxis().isHorizontal() && hitY > 0.5;
        return defaultBlockState().setValue(HANGING, hanging);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (level.isClientSide || !(be instanceof CoverableBlockEntity)) {
            return;
        }
        boolean hanging = state.getValue(HANGING);
        // Sneaking (or hanging, where 1.7.10 used floor rules anyway) gives a full block.
        int packed = placer != null && placer.isShiftKeyDown() || hanging ? CollapsibleShape.FULL : autoSlope(level, pos);
        ((CoverableBlockEntity) be).setShapeData(packed | SET);
        if (placer instanceof Player && packed != CollapsibleShape.FULL) {
            for (int corner = 0; corner < 4; corner++) {
                smooth(level, pos, hanging, corner, CollapsibleShape.corner(packed, corner), (Player) placer);
            }
        }
    }

    @Override
    public boolean onHammer(HammerClick click) {
        BlockEntity be = click.level.getBlockEntity(click.pos);
        if (!(be instanceof CoverableBlockEntity)) {
            return false;
        }
        if (click.simulate) {
            return true;
        }
        boolean hanging = click.state.getValue(HANGING);
        int packed = packed(((CoverableBlockEntity) be).getShapeData());
        int corner = CollapsibleShape.nearestCorner(click.hit.x, click.hit.z);
        // Left lowers the surface, right raises it, whichever way the block hangs.
        int step = click.leftClick != hanging ? -1 : 1;
        int height = Math.max(0, Math.min(CollapsibleShape.MAX, CollapsibleShape.corner(packed, corner) + step));
        ((CoverableBlockEntity) be).setShapeData(CollapsibleShape.withCorner(packed, corner, height) | SET);
        smooth(click.level, click.pos, hanging, corner, height, click.player);
        return true;
    }

    /** Copies a corner height into the matching corners of up to three neighbouring collapsibles. */
    private static void smooth(Level level, BlockPos pos, boolean hanging, int corner, int height, Player player) {
        int cx = corner & 1, cz = corner >> 1;
        for (int ox = cx - 1; ox <= cx; ox++) {
            for (int oz = cz - 1; oz <= cz; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                BlockPos at = pos.offset(ox, 0, oz);
                if (!level.hasChunkAt(at)) {
                    continue;
                }
                BlockState state = level.getBlockState(at);
                BlockEntity be = level.getBlockEntity(at);
                if (state.getBlock() instanceof CollapsibleBlock && state.getValue(HANGING) == hanging
                    && be instanceof CoverableBlockEntity && Permissions.canModify(player, level, at)) {
                    CoverableBlockEntity other = (CoverableBlockEntity) be;
                    int index = CollapsibleShape.cornerIndex(cx - ox, cz - oz);
                    other.setShapeData(CollapsibleShape.withCorner(packed(other.getShapeData()), index, height) | SET);
                }
            }
        }
    }

    // ---- 1.7.10 auto-slope ---------------------------------------------------------------

    private static int autoSlope(Level level, BlockPos pos) {
        int[] xn = scan(level, pos, Direction.WEST), xp = scan(level, pos, Direction.EAST);
        int[] zn = scan(level, pos, Direction.NORTH), zp = scan(level, pos, Direction.SOUTH);
        return CollapsibleShape.autoSlope(xn[0], new int[] { xn[1], xn[2] }, xp[0], new int[] { xp[1], xp[2] },
            zn[0], new int[] { zn[1], zn[2] }, zp[0], new int[] { zp[1], zp[2] });
    }

    /**
     * Walks up to 15 cells in one direction to the end of the slope: a collapsible, a solid
     * block, or a cliff edge. Returns {distance, height of the end's near corner 1, corner 2}.
     */
    private static int[] scan(Level level, BlockPos pos, Direction dir) {
        for (int i = 1; i <= 15; i++) {
            BlockPos cell = pos.relative(dir, i);
            if (!level.hasChunkAt(cell)) {
                break;
            }
            BlockState state = level.getBlockState(cell);
            if (state.getBlock() instanceof CollapsibleBlock || state.getMaterial().blocksMotion()
                || !level.getBlockState(cell.below()).getMaterial().blocksMotion()) {
                return endHeights(level, cell, dir, i);
            }
        }
        return endHeights(level, pos.relative(dir), dir, 1);
    }

    private static int[] endHeights(Level level, BlockPos cell, Direction dir, int distance) {
        BlockState state = level.getBlockState(cell);
        if (state.getBlock() instanceof CollapsibleBlock && !state.getValue(HANGING)) {
            int packed = packed(level, cell);
            // The end's corners on the side facing us, ordered north-to-south or west-to-east.
            switch (dir) {
                case WEST:  return new int[] { distance, corner(packed, 1, 0), corner(packed, 1, 1) };
                case EAST:  return new int[] { distance, corner(packed, 0, 0), corner(packed, 0, 1) };
                case NORTH: return new int[] { distance, corner(packed, 0, 1), corner(packed, 1, 1) };
                default:    return new int[] { distance, corner(packed, 0, 0), corner(packed, 1, 0) };
            }
        }
        int height = 0;
        if (state.getMaterial().blocksMotion()) {
            VoxelShape shape = state.getCollisionShape(level, cell);
            height = shape.isEmpty() ? 0 : (int) (shape.max(Direction.Axis.Y) * 16);
        }
        return new int[] { distance, height, height };
    }

    private static int corner(int packed, int x, int z) {
        return CollapsibleShape.corner(packed, CollapsibleShape.cornerIndex(x, z));
    }

    /** Only the base cover, as in 1.7.10. */
    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost();
    }
}
