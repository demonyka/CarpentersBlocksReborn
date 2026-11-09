package com.carpentersblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Attached redstone source with 1.7.10 polarity: hammer left-click inverts the output.
 * Weak power on every side, strong power into the support block, like vanilla buttons.
 */
public abstract class PolarityBlock extends AttachedBlock {
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");

    protected PolarityBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INVERTED);
    }

    /** Whether the block is "on" (pressed, switched, triggered), before polarity. */
    protected abstract boolean isActive(BlockState state);

    /** Output strength 0..15 before polarity is applied (only used when active). */
    protected int activeStrength(BlockState state) {
        return 15;
    }

    public int output(BlockState state) {
        boolean inverted = state.getValue(INVERTED);
        if (isActive(state)) {
            return inverted ? 0 : activeStrength(state);
        }
        return inverted ? 15 : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return output(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING) ? output(state) : 0;
    }

    /** Tells the neighbours and the support's neighbours that the output changed. */
    protected void notifyOutput(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean wasOutputting = output(state) > 0;
        super.onRemove(state, level, pos, newState, isMoving);
        if (!isMoving && !state.is(newState.getBlock()) && wasOutputting) {
            notifyOutput(state, level, pos);
        }
    }

    /** Hammer left-click toggles polarity; subclasses handle right-click via {@link #onHammerRight}. */
    @Override
    public boolean onHammer(HammerClick click) {
        if (click.leftClick) {
            BlockState next = click.state.cycle(INVERTED);
            click.set(next);
            if (!click.simulate) {
                notifyOutput(next, click.level, click.pos);
            }
            click.message(next.getValue(INVERTED) ? "message.carpentersblocks.polarity_neg" : "message.carpentersblocks.polarity_pos");
            return true;
        }
        return onHammerRight(click);
    }

    protected boolean onHammerRight(HammerClick click) {
        return false;
    }
}
