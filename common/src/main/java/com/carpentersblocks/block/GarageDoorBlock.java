package com.carpentersblocks.block;

import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.Sides;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Carpenter's Garage Door (1.7.10): placed under a ceiling, it hangs a curtain of pieces down
 * to the floor. Side-by-side columns facing the same way act as one door. Open, the curtain
 * rolls up into the top piece and the lower pieces become passable; the top piece drops the
 * item, the column breaks as a whole.
 */
public class GarageDoorBlock extends BoxedBlock {
    public enum Style implements StringRepresentable {
        DEFAULT, GLASS_TOP, GLASS, SIDING, HIDDEN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
    /** The direction the placer looked. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty RIGID = BooleanProperty.create("rigid");
    /** The top piece of a column: drops the item and shows the rolled-up door when open. */
    public static final BooleanProperty HOST = BooleanProperty.create("host");
    /** Bottom piece of its column (glass styles look different there). */
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** Longest column created at once, and largest door toggled at once. */
    private static final int MAX_LENGTH = 64, MAX_PIECES = 500;
    private static final int PANE = 1, IRON = 2;

    public GarageDoorBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(STYLE, Style.DEFAULT).setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false).setValue(RIGID, false).setValue(HOST, true).setValue(BOTTOM, true)
            .setValue(POWERED, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STYLE, FACING, OPEN, RIGID, HOST, BOTTOM, POWERED);
    }

    // ---- geometry -------------------------------------------------------------------------

    /** Model frame (1.7.10): facing south, the plate near z = 0; turned to face north first. */
    private static int turns(BlockState state) {
        return com.carpentersblocks.core.geom.Orientation.turnsFromNorth(Sides.toCore(state.getValue(FACING))) + 2;
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        List<Part> parts = new ArrayList<>();
        Style style = state.getValue(STYLE);
        boolean open = state.getValue(OPEN);
        if (open && !state.getValue(HOST)) {
            return parts; // rolled up into the top piece
        }
        double y0 = open ? 8 : 0;
        switch (style) {
            case DEFAULT:
                if (open) {
                    parts.add(Part.of(Box.px(0, 8, 2, 16, 16, 4)));
                } else {
                    parts.add(Part.of(Box.px(0, 0, 2, 5, 16, 4)));
                    parts.add(Part.of(Box.px(11, 0, 2, 16, 16, 4)));
                    parts.add(Part.of(Box.px(5, 12, 2, 11, 16, 4)));
                    parts.add(Part.of(Box.px(5, 0, 2, 11, 10, 4)));
                }
                break;
            case SIDING:
                parts.add(new Part(Box.px(0, y0, 3, 16, 16, 4), IRON));
                parts.add(Part.of(Box.px(0, open ? 8 : 0, 2, 16, open ? 15 : 7, 3)));
                if (!open) {
                    parts.add(Part.of(Box.px(0, 8, 2, 16, 15, 3)));
                }
                break;
            case HIDDEN:
                parts.add(Part.of(Box.px(0, y0, 0, 16, 16, 2)));
                break;
            default: { // GLASS_TOP, GLASS
                parts.add(Part.of(Box.px(0, y0, 2, 2, 16, 3)));          // stiles
                parts.add(Part.of(Box.px(14, y0, 2, 16, 16, 3)));
                parts.add(Part.of(Box.px(2, 15, 2, 14, 16, 3)));         // top rail
                parts.add(Part.of(Box.px(2, y0, 2, 14, y0 + (open ? 2 : 2), 3)));
                if (open) {
                    parts.add(Part.of(Box.px(0, 8, 3, 16, 16, 4)));       // back panel
                    break;
                }
                boolean glassHere = style == Style.GLASS ? !state.getValue(BOTTOM) : state.getValue(HOST);
                if (glassHere) {
                    parts.add(new Part(Box.px(2, 2, 3, 14, 15, 3), PANE));
                    parts.add(Part.of(Box.px(0, 0, 3, 16, 2, 4)));
                } else {
                    parts.add(Part.of(Box.px(2, 7, 2, 14, 9, 3)));       // middle rail
                    parts.add(Part.of(Box.px(0, 0, 3, 16, 16, 4)));      // back panel
                }
                break;
            }
        }
        List<Part> placed = new ArrayList<>(parts.size());
        for (Part part : parts) {
            placed.add(new Part(part.box.rotateY(turns(state)), part.part));
        }
        return placed;
    }

    @Override
    protected List<Box> outlineBoxes(BlockState state) {
        if (state.getValue(OPEN) && !state.getValue(HOST)) {
            return Collections.emptyList(); // invisible and passable while open
        }
        boolean hidden = state.getValue(STYLE) == Style.HIDDEN;
        Box plate = Box.px(0, state.getValue(OPEN) ? 8 : 0, hidden ? 0 : 2, 16, 16, hidden ? 2 : 4);
        return Collections.singletonList(plate.rotateY(turns(state)));
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        if (polygon.part == IRON) {
            return UncoveredTextures.IRON;
        }
        if (polygon.part == PANE) {
            return state.getValue(STYLE) == Style.GLASS ? UncoveredTextures.GARAGE_GLASS : UncoveredTextures.GARAGE_GLASS_TOP;
        }
        return null;
    }

    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost();
    }

    // ---- placement ------------------------------------------------------------------------

    private boolean isPiece(BlockState state, Direction facing) {
        return state.getBlock() == this && state.getValue(FACING) == facing;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() == this) {
            // Lengthening an existing column from below.
            return above.setValue(HOST, false).setValue(BOTTOM, true);
        }
        if (!above.isFaceSturdy(level, pos.above(), Direction.DOWN)) {
            return null; // hangs from a solid ceiling
        }
        Direction facing = context.getHorizontalDirection();
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(HOST, true);
        // Join a door beside us: same facing, same style, rigidity and open state.
        for (Direction side : new Direction[] { facing.getClockWise(), facing.getCounterClockWise() }) {
            BlockState other = level.getBlockState(pos.relative(side));
            if (isPiece(other, facing)) {
                return state.setValue(STYLE, other.getValue(STYLE)).setValue(RIGID, other.getValue(RIGID))
                    .setValue(OPEN, other.getValue(OPEN));
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            extendDown(level, pos, state);
        }
    }

    /** Hangs pieces below until the floor (or the length cap), as 1.7.10 did. */
    private void extendDown(Level level, BlockPos pos, BlockState state) {
        BlockPos top = pos;
        while (level.getBlockState(top.above()).getBlock() == this) {
            top = top.above();
        }
        BlockPos at = pos.below();
        int length = pos.getY() - top.getY() + 1;
        BlockState piece = state.setValue(HOST, false);
        while (length < MAX_LENGTH && level.hasChunkAt(at) && level.getBlockState(at).getMaterial().isReplaceable()
            && !level.getBlockState(at).getMaterial().isLiquid()) {
            level.setBlock(at, piece.setValue(BOTTOM, true), BlockFlags.ALL);
            at = at.below();
            length++;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.UP && state.getValue(HOST) && !neighbour.isFaceSturdy(level, neighbourPos, Direction.DOWN)
            && neighbour.getBlock() != this) {
            return Blocks.AIR.defaultBlockState(); // lost its ceiling; the column follows in onRemove
        }
        if (direction == Direction.DOWN) {
            return state.setValue(BOTTOM, !isPiece(neighbour, state.getValue(FACING)));
        }
        return state;
    }

    /** Breaking any piece removes its whole column; only the top piece drops the item. */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (level.isClientSide || state.is(newState.getBlock())) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (Direction dir : new Direction[] { Direction.UP, Direction.DOWN }) {
            BlockPos at = pos.relative(dir);
            if (isPiece(level.getBlockState(at), facing)) {
                level.destroyBlock(at, true); // non-top pieces have no loot, only their covers
            }
        }
    }

    // ---- the whole door -------------------------------------------------------------------

    /** All pieces joined up, down and sideways (same facing exactly; 1.7.10 mixed opposites). */
    private Set<BlockPos> door(BlockGetter level, BlockPos start, Direction facing) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        Direction[] steps = { Direction.UP, Direction.DOWN, facing.getClockWise(), facing.getCounterClockWise() };
        while (!todo.isEmpty() && seen.size() < MAX_PIECES) {
            BlockPos at = todo.poll();
            if (!seen.add(at)) {
                continue;
            }
            for (Direction step : steps) {
                BlockPos next = at.relative(step);
                if (!seen.contains(next) && isPiece(level.getBlockState(next), facing)) {
                    todo.add(next);
                }
            }
        }
        return seen;
    }

    private void setAll(Level level, Set<BlockPos> pieces, java.util.function.UnaryOperator<BlockState> change) {
        for (BlockPos at : pieces) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() == this) {
                level.setBlock(at, change.apply(state), BlockFlags.ALL);
            }
        }
    }

    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(RIGID)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean open = !state.getValue(OPEN);
            setAll(level, door(level, pos, state.getValue(FACING)), s -> s.setValue(OPEN, open));
            level.playSound(null, pos, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.9F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Redstone opens the door while any piece is powered and closes it when none is. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        Set<BlockPos> pieces = door(level, pos, state.getValue(FACING));
        boolean powered = pieces.stream().anyMatch(level::hasNeighborSignal);
        if (powered != state.getValue(POWERED)) {
            boolean changesOpen = powered != state.getValue(OPEN);
            setAll(level, pieces, s -> s.setValue(POWERED, powered).setValue(OPEN, powered));
            if (changesOpen) {
                level.playSound(null, pos, powered ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
        }
    }

    /** Hammer: left next style, right previous, sneak + right toggles rigid, for the whole door. */
    @Override
    public boolean onHammer(HammerClick click) {
        if (click.simulate) {
            return true;
        }
        Set<BlockPos> pieces = door(click.level, click.pos, click.state.getValue(FACING));
        if (click.sneaking && !click.leftClick) {
            boolean rigid = !click.state.getValue(RIGID);
            setAll(click.level, pieces, s -> s.setValue(RIGID, rigid));
            click.message(rigid ? "message.carpentersblocks.activation_iron" : "message.carpentersblocks.activation_wood");
        } else {
            Style[] styles = Style.values();
            int step = click.leftClick ? 1 : styles.length - 1;
            Style next = styles[(click.state.getValue(STYLE).ordinal() + step) % styles.length];
            setAll(click.level, pieces, s -> s.setValue(STYLE, next));
        }
        return true;
    }
}
