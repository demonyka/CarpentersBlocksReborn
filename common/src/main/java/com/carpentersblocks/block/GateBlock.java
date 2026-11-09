package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.registry.CBBlocks;
import com.carpentersblocks.util.BlockFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Carpenter's Gate (1.7.10): a fence gate in the barrier styles. Opens away from the player,
 * opens and closes with redstone changes like a vanilla gate, and a gate stacked directly
 * above or below follows it.
 */
public class GateBlock extends BoxedBlock {
    public static final EnumProperty<FenceType> TYPE = BarrierBlock.TYPE;
    /** The closed panel runs along x (true) or z. */
    public static final BooleanProperty ALONG_X = BooleanProperty.create("along_x");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Opens towards +x / +z (true) or the other way. */
    public static final BooleanProperty OPENS_POSITIVE = BooleanProperty.create("opens_positive");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty GATE_ABOVE = BooleanProperty.create("gate_above");
    public static final BooleanProperty GATE_BELOW = BooleanProperty.create("gate_below");

    public GateBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(TYPE, FenceType.VANILLA).setValue(ALONG_X, true)
            .setValue(OPEN, false).setValue(OPENS_POSITIVE, true).setValue(POWERED, false)
            .setValue(GATE_ABOVE, false).setValue(GATE_BELOW, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE, ALONG_X, OPEN, OPENS_POSITIVE, POWERED, GATE_ABOVE, GATE_BELOW);
    }

    /** Model frame: panel along z at x 7..9 px, leaves swing towards +x. */
    private static int turns(BlockState state) {
        boolean positive = state.getValue(OPENS_POSITIVE);
        return state.getValue(ALONG_X) ? (positive ? 1 : 3) : (positive ? 0 : 2);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        List<Box> boxes = new ArrayList<>();
        model(state.getValue(TYPE), state.getValue(OPEN), state.getValue(GATE_ABOVE), state.getValue(GATE_BELOW), boxes);
        List<Part> parts = new ArrayList<>(boxes.size());
        for (Box box : boxes) {
            parts.add(Part.of(box.rotateY(turns(state))));
        }
        return parts;
    }

    private static void model(FenceType type, boolean open, boolean above, boolean below, List<Box> out) {
        double o = type.railDrop();
        switch (type) {
            case VANILLA:
            case VANILLA_X1:
            case VANILLA_X2: {
                double postLow = below ? 0 : 5 - o;
                out.add(Box.px(7, postLow, 0, 9, 16, 2));
                out.add(Box.px(7, postLow, 14, 9, 16, 16));
                double low = below ? 0 : 6 - o, high = above ? 16 : 15;
                if (!open) {
                    out.add(Box.px(7, low, 6, 9, high, 10));
                    for (double z : new double[] { 2, 10 }) {
                        out.add(Box.px(7, 6 - o, z, 9, 9, z + 4));
                        out.add(Box.px(7, 12 - o, z, 9, 15, z + 4));
                    }
                } else {
                    for (double z : new double[] { 0, 14 }) {
                        out.add(Box.px(13, low, z, 15, high, z + 2));
                        out.add(Box.px(9, 6 - o, z, 13, 9, z + 2));
                        out.add(Box.px(9, 12 - o, z, 13, 15, z + 2));
                    }
                }
                break;
            }
            case VANILLA_X3: {
                out.add(Box.px(7, 2, 0, 9, 16, 2));
                out.add(Box.px(7, 2, 14, 9, 16, 16));
                double low = below ? 0 : 3, high = above ? 16 : 15;
                if (!open) {
                    out.add(Box.px(7, low, 2, 9, high, 14));
                } else {
                    out.add(Box.px(9, low, 0, 15, high, 2));
                    out.add(Box.px(9, low, 14, 15, high, 16));
                }
                break;
            }
            case PICKET: {
                double low = below ? 0 : 1;
                if (!open) {
                    if (!above) {
                        out.add(Box.px(7, 10, 0, 9, 11, 16));
                    }
                    if (!below) {
                        out.add(Box.px(7, 3, 0, 9, 4, 16));
                    }
                    for (double x : new double[] { 6, 9 }) {
                        for (double z : new double[] { 0, 15 }) {
                            out.add(Box.px(x, low, z, x + 1, above ? 16 : 13, z + 1));
                        }
                        for (double z : new double[] { 3, 7, 11 }) {
                            out.add(Box.px(x, low, z, x + 1, above ? 16 : 14, z + 2));
                        }
                    }
                } else {
                    for (double z : new double[] { 1, 13 }) {
                        if (!above) {
                            out.add(Box.px(8, 10, z, 16, 11, z + 2));
                        }
                        if (!below) {
                            out.add(Box.px(8, 3, z, 16, 4, z + 2));
                        }
                    }
                    for (double z : new double[] { 0, 3, 12, 15 }) {
                        out.add(Box.px(8, low, z, 9, above ? 16 : 13, z + 1));
                        out.add(Box.px(11, low, z, 13, above ? 16 : 14, z + 1));
                        out.add(Box.px(15, low, z, 16, above ? 16 : 14, z + 1));
                    }
                }
                break;
            }
            case SHADOWBOX: {
                if (!open) {
                    if (!above) {
                        out.add(Box.px(7, 12, 0, 9, 14, 16));
                    }
                    if (!below) {
                        out.add(Box.px(7, 2, 0, 9, 4, 16));
                    }
                    out.add(Box.px(9, 0, 0, 10, 16, 4));
                    out.add(Box.px(9, 0, 8, 10, 16, 12));
                    out.add(Box.px(6, 0, 4, 7, 16, 8));
                    out.add(Box.px(6, 0, 12, 7, 16, 16));
                } else {
                    for (double z : new double[] { 1, 13 }) {
                        if (!above) {
                            out.add(Box.px(8, 12, z, 16, 14, z + 2));
                        }
                        if (!below) {
                            out.add(Box.px(8, 2, z, 16, 4, z + 2));
                        }
                    }
                    out.add(Box.px(8, 0, 0, 12, 16, 1));
                    out.add(Box.px(8, 0, 12, 12, 16, 13));
                    out.add(Box.px(12, 0, 3, 16, 16, 4));
                    out.add(Box.px(12, 0, 15, 16, 16, 16));
                }
                break;
            }
            default: { // WALL
                double high = above ? 16 : 13;
                if (!open) {
                    out.add(Box.px(7, 0, 0, 9, high, 16));
                } else {
                    out.add(Box.px(8, 0, 0, 16, high, 2));
                    out.add(Box.px(8, 0, 14, 16, high, 16));
                }
                break;
            }
        }
    }

    /** Closed: the whole span, 1.5 high like a fence gate. Open: walk through. */
    @Override
    protected List<Box> collisionBoxes(BlockState state) {
        if (state.getValue(OPEN)) {
            return Collections.emptyList();
        }
        FenceType type = state.getValue(TYPE);
        double half = type.isVanilla() || type == FenceType.WALL ? 1 : 2;
        return Collections.singletonList(Box.px(8 - half, 0, 0, 8 + half, 24, 16).rotateY(turns(state)));
    }

    // ---- placement and use ----------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The panel runs across the player's view.
        BlockState state = defaultBlockState().setValue(ALONG_X, context.getHorizontalDirection().getAxis() == Direction.Axis.Z);
        for (Direction dir : Direction.values()) {
            BlockState other = context.getLevel().getBlockState(context.getClickedPos().relative(dir));
            if (other.getBlock() == this || other.getBlock() == CBBlocks.BARRIER.get()) {
                state = state.setValue(TYPE, other.getValue(TYPE));
            }
        }
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return stacked(state, context.getLevel(), context.getClickedPos()).setValue(POWERED, powered).setValue(OPEN, powered);
    }

    private BlockState stacked(BlockState state, LevelAccessor level, BlockPos pos) {
        return state.setValue(GATE_ABOVE, level.getBlockState(pos.above()).getBlock() == this)
            .setValue(GATE_BELOW, level.getBlockState(pos.below()).getBlock() == this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        return direction.getAxis() == Direction.Axis.Y ? stacked(state, level, pos) : state;
    }

    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next = state.cycle(OPEN);
            if (next.getValue(OPEN)) {
                // Swing away from the player.
                Direction look = player.getDirection();
                next = next.setValue(OPENS_POSITIVE, next.getValue(ALONG_X)
                    ? look.getStepZ() > 0 : look.getStepX() > 0);
            }
            setOpen(level, pos, next);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Applies the open state to this gate and to one stacked gate below (or else above). */
    private void setOpen(Level level, BlockPos pos, BlockState next) {
        level.setBlock(pos, next, BlockFlags.ALL);
        level.playSound(null, pos, next.getValue(OPEN) ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
            SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        for (BlockPos other : new BlockPos[] { pos.below(), pos.above() }) {
            BlockState gate = level.getBlockState(other);
            if (gate.getBlock() == this && gate.getValue(ALONG_X) == next.getValue(ALONG_X)) {
                level.setBlock(other, gate.setValue(OPEN, next.getValue(OPEN)).setValue(OPENS_POSITIVE, next.getValue(OPENS_POSITIVE)),
                    BlockFlags.ALL);
                break;
            }
        }
    }

    /** Opens on a rising redstone signal and closes on a falling one, like vanilla gates. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState next = state.setValue(POWERED, powered);
            if (powered != state.getValue(OPEN)) {
                setOpen(level, pos, next.setValue(OPEN, powered));
            } else {
                level.setBlock(pos, next, BlockFlags.CLIENTS);
            }
        }
    }

    /** Hammer right-click changes the style (sneak: vanilla sub-styles); left-click does nothing. */
    @Override
    public boolean onHammer(HammerClick click) {
        if (click.leftClick) {
            return false;
        }
        FenceType type = click.state.getValue(TYPE);
        click.set(click.state.setValue(TYPE, click.sneaking ? type.nextVanilla() : type.next()));
        return true;
    }
}
