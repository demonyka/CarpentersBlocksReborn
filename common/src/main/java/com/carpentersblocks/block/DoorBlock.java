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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Carpenter's Door (1.7.10): two blocks high, six styles, double doors open together. Wooden
 * doors open by hand; rigid ("iron") ones only with redstone. Each half takes its own cover.
 */
public class DoorBlock extends BoxedBlock {
    public enum Style implements StringRepresentable {
        GLASS_TOP, GLASS_TALL, PANELS, SCREEN_TALL, FRENCH_GLASS, HIDDEN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
    /** The direction the placer looked; the closed door sits on the edge nearest the placer. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty RIGID = BooleanProperty.create("rigid");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PANE = 1, HANDLE = 2;

    public DoorBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(STYLE, Style.GLASS_TOP).setValue(FACING, Direction.NORTH)
            .setValue(HALF, DoubleBlockHalf.LOWER).setValue(HINGE, DoorHingeSide.LEFT).setValue(OPEN, false)
            .setValue(RIGID, false).setValue(POWERED, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STYLE, FACING, HALF, HINGE, OPEN, RIGID, POWERED);
    }

    // ---- geometry -------------------------------------------------------------------------

    /**
     * Model frame: facing north, closed slab on the south edge (z 13..16 px). An open door is
     * the same slab turned a quarter around its hinge.
     */
    private static int turns(BlockState state) {
        int facing = com.carpentersblocks.core.geom.Orientation.turnsFromNorth(Sides.toCore(state.getValue(FACING)));
        if (!state.getValue(OPEN)) {
            return facing;
        }
        return facing + (state.getValue(HINGE) == DoorHingeSide.RIGHT ? 3 : 1);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        boolean top = state.getValue(HALF) == DoubleBlockHalf.UPPER;
        Style style = state.getValue(STYLE);
        List<Part> parts = new ArrayList<>();
        if (style == Style.HIDDEN) {
            parts.add(Part.of(Box.px(0, 0, 13, 16, 16, 16)));
        } else {
            parts.add(Part.of(Box.px(0, 0, 13, 3, 16, 16)));      // stiles
            parts.add(Part.of(Box.px(13, 0, 13, 16, 16, 16)));
            panel(style, top, parts);
        }
        // Pull handle (iron) on the edge away from the hinge, at mid height of the door.
        boolean handleLeft = state.getValue(HINGE) == DoorHingeSide.RIGHT;
        double hx = handleLeft ? 1 : 14;
        parts.add(new Part(top ? Box.px(hx, 0, 11, hx + 1, 2, 12) : Box.px(hx, 14, 11, hx + 1, 16, 12), HANDLE));
        parts.add(new Part(top ? Box.px(hx, 1, 12, hx + 1, 2, 13) : Box.px(hx, 14, 12, hx + 1, 15, 13), HANDLE));
        if (style != Style.HIDDEN) {
            // Outer handle, sticking out of the block like in 1.7.10.
            parts.add(new Part(top ? Box.px(hx, 0, 17, hx + 1, 2, 18) : Box.px(hx, 14, 17, hx + 1, 16, 18), HANDLE));
            parts.add(new Part(top ? Box.px(hx, 1, 16, hx + 1, 2, 17) : Box.px(hx, 14, 16, hx + 1, 15, 17), HANDLE));
        }
        List<Part> placed = new ArrayList<>(parts.size());
        for (Part part : parts) {
            placed.add(new Part(part.box.rotateY(turns(state)), part.part));
        }
        return placed;
    }

    /** Everything between the stiles, per style and half (1.7.10 door renderer). */
    private static void panel(Style style, boolean top, List<Part> parts) {
        switch (style) {
            case GLASS_TOP:
                if (top) {
                    parts.add(Part.of(Box.px(3, 13, 13, 13, 16, 16)));
                    parts.add(Part.of(Box.px(3, 0, 13, 13, 3, 16)));
                    parts.add(new Part(Box.px(3, 3, 14.5, 13, 13, 14.5), PANE));
                } else {
                    raisedPanel(parts, 15);
                }
                break;
            case PANELS:
                if (top) {
                    parts.add(Part.of(Box.px(3, 0, 14, 13, 13, 15)));
                    parts.add(Part.of(Box.px(3, 13, 13, 13, 16, 16)));
                    parts.add(Part.of(Box.px(3, 1, 13, 13, 4, 16)));
                    parts.add(Part.of(Box.px(5, 6, 13, 11, 11, 16)));
                } else {
                    raisedPanel(parts, 15);
                }
                break;
            case GLASS_TALL:
            case SCREEN_TALL:
                parts.add(Part.of(top ? Box.px(3, 13, 13, 13, 16, 16) : Box.px(3, 0, 13, 13, 3, 16)));
                parts.add(new Part(top ? Box.px(3, 0, 14.5, 13, 13, 14.5) : Box.px(3, 3, 14.5, 13, 16, 14.5), PANE));
                break;
            default: // FRENCH_GLASS
                if (top) {
                    parts.add(Part.of(Box.px(7, 1, 14, 9, 6, 15)));
                    parts.add(Part.of(Box.px(7, 8, 14, 9, 13, 15)));
                    parts.add(Part.of(Box.px(3, 13, 13, 13, 16, 16)));
                    parts.add(Part.of(Box.px(3, 0, 14, 13, 1, 15)));
                    parts.add(Part.of(Box.px(3, 6, 14, 13, 8, 15)));
                    parts.add(new Part(Box.px(3, 0, 14.5, 13, 13, 14.5), PANE));
                } else {
                    parts.add(Part.of(Box.px(7, 3, 14, 9, 8, 15)));
                    parts.add(Part.of(Box.px(7, 10, 14, 9, 15, 15)));
                    parts.add(Part.of(Box.px(3, 0, 13, 13, 3, 16)));
                    parts.add(Part.of(Box.px(3, 8, 14, 13, 10, 15)));
                    parts.add(Part.of(Box.px(3, 15, 14, 13, 16, 15)));
                    parts.add(new Part(Box.px(3, 3, 14.5, 13, 16, 14.5), PANE));
                }
                break;
        }
    }

    private static void raisedPanel(List<Part> parts, double top) {
        parts.add(Part.of(Box.px(3, 3, 14, 13, 16, 15)));
        parts.add(Part.of(Box.px(3, 0, 13, 13, 3, 16)));
        parts.add(Part.of(Box.px(5, 5, 13, 11, top, 16)));
    }

    @Override
    protected List<Box> outlineBoxes(BlockState state) {
        return Collections.singletonList(Box.px(0, 0, 13, 16, 16, 16).rotateY(turns(state)));
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        if (polygon.part == HANDLE) {
            return UncoveredTextures.IRON;
        }
        if (polygon.part != PANE) {
            return null;
        }
        boolean top = state.getValue(HALF) == DoubleBlockHalf.UPPER;
        switch (state.getValue(STYLE)) {
            case GLASS_TOP:    return UncoveredTextures.DOOR_GLASS_TOP;
            case GLASS_TALL:   return top ? UncoveredTextures.DOOR_GLASS_TALL_TOP : UncoveredTextures.DOOR_GLASS_TALL_BOTTOM;
            case SCREEN_TALL:  return UncoveredTextures.DOOR_SCREEN_TALL;
            default:           return top ? UncoveredTextures.DOOR_FRENCH_TOP : UncoveredTextures.DOOR_FRENCH_BOTTOM;
        }
    }

    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost();
    }

    // ---- two halves -----------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        Direction facing = context.getHorizontalDirection();
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(HALF, DoubleBlockHalf.LOWER);
        // Second leaf of a double door: a left-hinged door of the same facing on our left.
        BlockState left = level.getBlockState(pos.relative(facing.getCounterClockWise()));
        if (left.getBlock() == this && left.getValue(FACING) == facing && left.getValue(HALF) == DoubleBlockHalf.LOWER
            && left.getValue(HINGE) == DoorHingeSide.LEFT) {
            state = state.setValue(HINGE, DoorHingeSide.RIGHT);
        }
        // Take style and rigidity from a neighbouring door of the same facing.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState other = level.getBlockState(pos.relative(dir));
            if (other.getBlock() == this && other.getValue(FACING) == facing) {
                state = state.setValue(STYLE, other.getValue(STYLE)).setValue(RIGID, other.getValue(RIGID));
                break;
            }
        }
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        return state.setValue(POWERED, powered).setValue(OPEN, powered);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), BlockFlags.ALL);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState under = level.getBlockState(below);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? under.isFaceSturdy(level, below, Direction.UP) : under.is(this);
    }

    /** Like vanilla doors: each half mirrors the other, and dies without it or without a floor. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            if (!neighbour.is(this) || neighbour.getValue(HALF) == half) {
                return Blocks.AIR.defaultBlockState();
            }
            return state.setValue(FACING, neighbour.getValue(FACING)).setValue(HINGE, neighbour.getValue(HINGE))
                .setValue(OPEN, neighbour.getValue(OPEN)).setValue(STYLE, neighbour.getValue(STYLE))
                .setValue(RIGID, neighbour.getValue(RIGID)).setValue(POWERED, neighbour.getValue(POWERED));
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** Creative players breaking the top half must not get a drop from the bottom half. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), BlockFlags.ALL | 32);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    /** The other leaf of a double door, as its lower-half position, or null. */
    private BlockPos partner(Level level, BlockPos lowerPos, BlockState state) {
        if (!state.is(this)) {
            return null;
        }
        Direction facing = state.getValue(FACING);
        Direction toPartner = state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos at = lowerPos.relative(toPartner);
        BlockState other = level.getBlockState(at);
        return other.is(this) && other.getValue(HALF) == DoubleBlockHalf.LOWER && other.getValue(FACING) == facing
            && other.getValue(HINGE) != state.getValue(HINGE) ? at : null;
    }

    /** Changes the lower half (the upper copies it) of this door and of its partner. */
    private void setBoth(Level level, BlockPos lowerPos, java.util.function.UnaryOperator<BlockState> change, boolean withPartner) {
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(this)) {
            return;
        }
        BlockPos partner = withPartner ? partner(level, lowerPos, lower) : null;
        level.setBlock(lowerPos, change.apply(lower), BlockFlags.ALL);
        if (partner != null) {
            level.setBlock(partner, change.apply(level.getBlockState(partner)), BlockFlags.ALL);
        }
    }

    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(RIGID)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean open = !state.getValue(OPEN);
            setBoth(level, lower(state, pos), s -> s.setValue(OPEN, open), true);
            playSound(level, pos, open, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void playSound(Level level, BlockPos pos, boolean open, boolean iron) {
        level.playSound(null, pos, open ? (iron ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.WOODEN_DOOR_OPEN)
            : (iron ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_CLOSE), SoundSource.BLOCKS, 1.0F,
            level.random.nextFloat() * 0.1F + 0.9F);
    }

    /** Redstone opens both leaves while any half of either is powered (wooden and rigid). */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        BlockPos lowerPos = lower(state, pos);
        if (!level.getBlockState(lowerPos).is(this)) {
            return; // the lower half was just removed; updateShape removes this half next
        }
        BlockPos partner = partner(level, lowerPos, level.getBlockState(lowerPos));
        boolean powered = level.hasNeighborSignal(lowerPos) || level.hasNeighborSignal(lowerPos.above())
            || partner != null && (level.hasNeighborSignal(partner) || level.hasNeighborSignal(partner.above()));
        if (powered != state.getValue(POWERED)) {
            boolean changesOpen = powered != state.getValue(OPEN);
            setBoth(level, lowerPos, s -> s.setValue(POWERED, powered).setValue(OPEN, powered), true);
            if (changesOpen) {
                playSound(level, pos, powered, state.getValue(RIGID));
            }
        }
    }

    /**
     * Hammer: left-click flips the hinge; right-click changes the style; sneak + right-click
     * toggles rigid (redstone only) on both leaves.
     */
    @Override
    public boolean onHammer(HammerClick click) {
        if (click.simulate) {
            return true;
        }
        BlockPos lowerPos = lower(click.state, click.pos);
        if (click.leftClick) {
            setBoth(click.level, lowerPos, s -> s.cycle(HINGE), false);
        } else if (click.sneaking) {
            boolean rigid = !click.state.getValue(RIGID);
            setBoth(click.level, lowerPos, s -> s.setValue(RIGID, rigid), true);
            click.message(rigid ? "message.carpentersblocks.activation_iron" : "message.carpentersblocks.activation_wood");
        } else {
            Style[] styles = Style.values();
            setBoth(click.level, lowerPos, s -> s.setValue(STYLE, styles[(s.getValue(STYLE).ordinal() + 1) % styles.length]), false);
        }
        return true;
    }

    /** Mobs only path through it while it is open, like vanilla doors and gates. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.level.pathfinder.PathComputationType type) {
        return type != net.minecraft.world.level.pathfinder.PathComputationType.WATER && state.getValue(OPEN);
    }
}
