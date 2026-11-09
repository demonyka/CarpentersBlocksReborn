package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Carpenter's Button: any face, 20-tick press, invertible output (1.7.10). */
public class ButtonBlock extends PolarityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PRESS_TICKS = 20;

    public ButtonBlock(Properties properties) {
        super(properties.noCollission());
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.UP)
            .setValue(POWERED, false).setValue(INVERTED, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected boolean isActive(BlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        double depth = state.getValue(POWERED) ? 1 : 2;
        return Collections.singletonList(Part.of(attach(state, Box.px(5, 0, 6, 11, depth, 10))));
    }

    @Override
    protected List<Box> collisionBoxes(BlockState state) {
        return Collections.emptyList();
    }

    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide) {
            BlockState pressed = state.setValue(POWERED, true);
            level.setBlock(pos, pressed, com.carpentersblocks.util.BlockFlags.ALL);
            notifyOutput(pressed, level, pos);
            level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
            level.getBlockTicks().scheduleTick(pos, this, PRESS_TICKS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (state.getValue(POWERED)) {
            BlockState released = state.setValue(POWERED, false);
            level.setBlock(pos, released, com.carpentersblocks.util.BlockFlags.ALL);
            notifyOutput(released, level, pos);
            level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
        }
    }
}
