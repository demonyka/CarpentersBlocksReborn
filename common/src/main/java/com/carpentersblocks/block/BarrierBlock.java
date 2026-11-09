package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.registry.CBBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Carpenter's Barrier (1.7.10): a fence in four styles that joins barriers, gates, vanilla
 * fences and solid faces. Collides 1.5 high like a fence. In the {@code minecraft:fences} tag,
 * so mobs path around it and vanilla fences join it.
 */
public class BarrierBlock extends BoxedBlock {
    public static final EnumProperty<FenceType> TYPE = EnumProperty.create("type", FenceType.class);
    public static final BooleanProperty POST = BooleanProperty.create("post");
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** A barrier above / below (pickets and walls grow into it, rails are skipped). */
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final Direction[] HORIZONTAL = { Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST };

    public BarrierBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(TYPE, FenceType.VANILLA).setValue(POST, false)
            .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE, POST, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    static BooleanProperty side(Direction dir) {
        switch (dir) {
            case NORTH: return NORTH;
            case EAST:  return EAST;
            case SOUTH: return SOUTH;
            default:    return WEST;
        }
    }

    /** Quarter turns that bring an arm designed on the south side to {@code dir}. */
    private static int turns(Direction dir) {
        switch (dir) {
            case WEST:  return 1;
            case NORTH: return 2;
            case EAST:  return 3;
            default:    return 0;
        }
    }

    private static boolean connected(BlockState state, Direction dir) {
        return state.getValue(side(dir));
    }

    private boolean hasPost(BlockState state) {
        boolean x = state.getValue(EAST) || state.getValue(WEST), z = state.getValue(NORTH) || state.getValue(SOUTH);
        return state.getValue(POST) || x == z;
    }

    // ---- geometry -------------------------------------------------------------------

    @Override
    protected List<Part> renderParts(BlockState state) {
        FenceType type = state.getValue(TYPE);
        boolean up = state.getValue(UP), down = state.getValue(DOWN);
        List<Part> parts = new ArrayList<>();
        double post = type == FenceType.WALL ? 4 : type.isVanilla() ? 2 : 1;   // half width, pixels
        if (hasPost(state)) {
            parts.add(Part.of(Box.px(8 - post, 0, 8 - post, 8 + post, 16, 8 + post)));
        }
        for (Direction dir : HORIZONTAL) {
            List<Box> arm = new ArrayList<>();
            if (connected(state, dir)) {
                arm(type, up, down, arm);
            } else if (type == FenceType.PICKET) {
                arm.add(Box.px(7, down ? 0 : 1, 9, 9, 16, 10));   // stub
            }
            for (Box box : arm) {
                parts.add(Part.of(box.rotateY(turns(dir))));
            }
        }
        return parts;
    }

    /** One connected arm on the south half (z 8..16 pixels). */
    private static void arm(FenceType type, boolean up, boolean down, List<Box> out) {
        int drop = type.railDrop();
        switch (type) {
            case VANILLA:
            case VANILLA_X1:
            case VANILLA_X2:
                out.add(Box.px(7, 12 - drop, 8, 9, 15, 16));
                out.add(Box.px(7, 6 - drop, 8, 9, 9, 16));
                break;
            case VANILLA_X3:
                out.add(Box.px(7, 3, 8, 9, 15, 16));
                break;
            case PICKET: {
                double low = down ? 0 : 1;
                for (double x : new double[] { 6, 9 }) {
                    out.add(Box.px(x, low, 11, x + 1, up ? 16 : 14, 13));
                    out.add(Box.px(x, low, 15, x + 1, up ? 16 : 13, 16));
                }
                if (!up) {
                    out.add(Box.px(7, 10, 8, 9, 11, 16));
                }
                if (!down) {
                    out.add(Box.px(7, 3, 8, 9, 4, 16));
                }
                break;
            }
            case SHADOWBOX:
                if (!up) {
                    out.add(Box.px(7, 12, 8, 9, 14, 16));
                }
                if (!down) {
                    out.add(Box.px(7, 2, 8, 9, 4, 16));
                }
                out.add(Box.px(9, 0, 8, 10, 16, 12));
                out.add(Box.px(6, 0, 12, 7, 16, 16));
                break;
            default: // WALL
                out.add(Box.px(5, 0, 8, 11, up ? 16 : 13, 16));
                break;
        }
    }

    /**
     * Fence-like collision, 1.5 high, one box per arm (1.7.10 used one width for every style
     * and one box for corners).
     */
    @Override
    protected List<Box> collisionBoxes(BlockState state) {
        boolean wall = state.getValue(TYPE) == FenceType.WALL;
        double half = wall ? 3 : 2;
        List<Box> boxes = new ArrayList<>();
        boxes.add(Box.px(8 - half, 0, 8 - half, 8 + half, 24, 8 + half));
        for (Direction dir : HORIZONTAL) {
            if (connected(state, dir)) {
                boxes.add(Box.px(8 - half, 0, 8, 8 + half, 24, 16).rotateY(turns(dir)));
            }
        }
        return boxes;
    }

    // ---- connections ------------------------------------------------------------------

    private boolean connectsTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos at = pos.relative(dir);
        BlockState other = level.getBlockState(at);
        return other.getBlock() == this || other.getBlock() == CBBlocks.GATE.get()
            || other.is(BlockTags.FENCES) || other.isFaceSturdy(level, at, dir.getOpposite());
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction dir : HORIZONTAL) {
            state = state.setValue(side(dir), connectsTo(level, pos, dir));
        }
        return state.setValue(UP, level.getBlockState(pos.above()).getBlock() == this)
            .setValue(DOWN, level.getBlockState(pos.below()).getBlock() == this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        // Take the style of a neighbouring barrier or gate.
        for (Direction dir : Direction.values()) {
            BlockState other = context.getLevel().getBlockState(context.getClickedPos().relative(dir));
            if (other.getBlock() == this || other.getBlock() == CBBlocks.GATE.get()) {
                state = state.setValue(TYPE, other.getValue(TYPE));
            }
        }
        return withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        return withConnections(state, level, pos);
    }

    /** Hammer: left-click toggles the post; right-click changes the style (sneak: vanilla sub-styles). */
    @Override
    public boolean onHammer(HammerClick click) {
        if (click.leftClick) {
            click.set(click.state.cycle(POST));
        } else {
            FenceType type = click.state.getValue(TYPE);
            click.set(click.state.setValue(TYPE, click.sneaking ? type.nextVanilla() : type.next()));
        }
        return true;
    }
}
