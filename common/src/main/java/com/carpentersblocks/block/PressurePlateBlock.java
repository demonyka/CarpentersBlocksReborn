package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.util.BlockFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Carpenter's Pressure Plate: any face, choosable trigger (players, monsters, animals,
 * everything), invertible output, re-checked every 20 ticks (1.7.10).
 */
public class PressurePlateBlock extends PolarityBlock {
    public enum Trigger implements StringRepresentable {
        PLAYER(e -> e instanceof Player && !e.isIgnoringBlockTriggers()),
        MONSTER(e -> e instanceof Enemy),
        ANIMAL(e -> e instanceof Animal),
        ALL(e -> !e.isIgnoringBlockTriggers());

        final Predicate<Entity> matches;

        Trigger(Predicate<Entity> matches) {
            this.matches = matches;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Trigger> TRIGGER = EnumProperty.create("trigger", Trigger.class);
    private static final int CHECK_TICKS = 20;

    public PressurePlateBlock(Properties properties) {
        super(properties.noCollission());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false)
            .setValue(INVERTED, false).setValue(TRIGGER, Trigger.PLAYER).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED, TRIGGER);
    }

    @Override
    protected boolean isActive(BlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        double depth = state.getValue(POWERED) ? 0.5 : 1;
        return Collections.singletonList(Part.of(attach(state, Box.px(1, 0, 1, 15, depth, 15))));
    }

    @Override
    protected List<Box> collisionBoxes(BlockState state) {
        return Collections.emptyList();
    }

    /** The plate plus a quarter block in front of it, like vanilla plates. */
    private boolean triggered(BlockState state, Level level, BlockPos pos) {
        Box zone = attach(state, Box.px(1, 0, 1, 15, 4, 15));
        AABB box = new AABB(zone.minX, zone.minY, zone.minZ, zone.maxX, zone.maxY, zone.maxZ).move(pos);
        return !level.getEntitiesOfClass(Entity.class, box, e -> e.isAlive() && state.getValue(TRIGGER).matches.test(e)).isEmpty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && !state.getValue(POWERED) && triggered(state, level, pos)) {
            setPowered(state, level, pos, true);
            level.getBlockTicks().scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        if (triggered(state, level, pos)) {
            level.getBlockTicks().scheduleTick(pos, this, CHECK_TICKS);
        } else {
            setPowered(state, level, pos, false);
        }
    }

    private void setPowered(BlockState state, Level level, BlockPos pos, boolean powered) {
        BlockState next = state.setValue(POWERED, powered);
        level.setBlock(pos, next, BlockFlags.ALL);
        notifyOutput(next, level, pos);
        level.playSound(null, pos, powered ? SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON : SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF,
            SoundSource.BLOCKS, 0.3F, powered ? 0.6F : 0.5F);
    }

    @Override
    protected boolean onHammerRight(HammerClick click) {
        Trigger next = Trigger.values()[(click.state.getValue(TRIGGER).ordinal() + 1) % Trigger.values().length];
        click.set(click.state.setValue(TRIGGER, next));
        click.message("message.carpentersblocks.trigger_" + next.getSerializedName());
        return true;
    }
}
