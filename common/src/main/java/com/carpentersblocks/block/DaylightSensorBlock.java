package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Carpenter's Daylight Sensor (1.7.10): attaches to the floor or walls, re-reads the sky every
 * 20 ticks. Modes: sleep threshold, monster-spawn threshold, or a smooth output that follows
 * the sun (direction-aware on walls).
 */
public class DaylightSensorBlock extends PolarityBlock {
    public enum Mode implements StringRepresentable {
        SLEEP, MONSTERS, DYNAMIC;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    /** Light level the sensor saw last time (0..15). */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 15);
    private static final int CHECK_TICKS = 20;
    private static final int LAPIS = 1, REDSTONE = 2, GLASS = 3;

    public DaylightSensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(INVERTED, false)
            .setValue(MODE, Mode.SLEEP).setValue(LEVEL, 0).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODE, LEVEL);
    }

    /** Not on ceilings, as in 1.7.10. */
    @Override
    protected boolean canFace(Direction facing) {
        return facing != Direction.DOWN;
    }

    @Override
    protected boolean isActive(BlockState state) {
        int level = state.getValue(LEVEL);
        switch (state.getValue(MODE)) {
            case SLEEP:    return level > 11;
            case MONSTERS: return level > 7;
            default:       return level > 0;
        }
    }

    @Override
    public int output(BlockState state) {
        if (state.getValue(MODE) == Mode.DYNAMIC) {
            int level = state.getValue(LEVEL);
            return state.getValue(INVERTED) ? 15 - level : level;
        }
        return super.output(state);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        List<Part> parts = new ArrayList<>();
        parts.add(part(state, Box.px(1, 0, 1, 15, 1, 15), 0));            // bottom plate
        parts.add(part(state, Box.px(0, 0, 0, 16, 4, 1), 0));             // frame walls
        parts.add(part(state, Box.px(0, 0, 15, 16, 4, 16), 0));
        parts.add(part(state, Box.px(0, 0, 1, 1, 4, 15), 0));
        parts.add(part(state, Box.px(15, 0, 1, 16, 4, 15), 0));
        parts.add(part(state, Box.px(2, 1, 2, 14, 3, 14), LAPIS));        // lapis inlay
        parts.add(part(state, Box.px(1, 1, 1, 15, 3, 2), REDSTONE));      // redstone ring
        parts.add(part(state, Box.px(1, 1, 14, 15, 3, 15), REDSTONE));
        parts.add(part(state, Box.px(1, 1, 2, 2, 3, 14), REDSTONE));
        parts.add(part(state, Box.px(14, 1, 2, 15, 3, 14), REDSTONE));
        parts.add(part(state, Box.px(1, 3, 1, 15, 4, 15), GLASS));        // glass top
        return parts;
    }

    private static Part part(BlockState state, Box floorBox, int part) {
        return new Part(attach(state, floorBox), part);
    }

    @Override
    protected List<Box> outlineBoxes(BlockState state) {
        return java.util.Collections.singletonList(attach(state, Box.px(0, 0, 0, 16, 4, 16)));
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        switch (polygon.part) {
            case LAPIS:    return UncoveredTextures.LAPIS;
            case REDSTONE: return UncoveredTextures.REDSTONE;
            case GLASS:    return UncoveredTextures.DAYLIGHT_GLASS;
            default:       return null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !oldState.is(this)) {
            level.getBlockTicks().scheduleTick(pos, this, 1);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        level.getBlockTicks().scheduleTick(pos, this, CHECK_TICKS);
        if (!level.dimensionType().hasSkyLight()) {
            return;
        }
        int light = Mth.clamp(measure(state, level, pos), 0, 15);  // 1.7.10 could go negative and corrupt its data
        if (light != state.getValue(LEVEL)) {
            BlockState next = state.setValue(LEVEL, light);
            level.setBlock(pos, next, BlockFlags.ALL);
            notifyOutput(next, level, pos);
        }
    }

    private static int measure(BlockState state, ServerLevel level, BlockPos pos) {
        int light = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        Mode mode = state.getValue(MODE);
        if (mode == Mode.MONSTERS && level.isThundering()) {
            return 7;
        }
        if (mode != Mode.DYNAMIC) {
            return light;
        }
        float angle = level.getSunAngle(1.0F);
        if (angle > 1.67F && angle < 4.62F) {
            return 0; // night
        }
        switch (state.getValue(FACING)) {
            case NORTH:
            case SOUTH: return (int) (light * 0.6F);
            case EAST:  return sunFormula(light, angle + (float) Math.PI / 2);
            case WEST:  return sunFormula(light, angle - (float) Math.PI / 2);
            default:    return sunFormula(light, angle);
        }
    }

    /** Vanilla daylight detector formula: brightness by the cosine of the sun angle. */
    private static int sunFormula(int light, float angle) {
        float target = angle < (float) Math.PI ? 0.0F : (float) Math.PI * 2;
        angle += (target - angle) * 0.2F;
        return Math.round(light * Mth.cos(angle));
    }

    @Override
    protected boolean onHammerRight(HammerClick click) {
        Mode next = Mode.values()[(click.state.getValue(MODE).ordinal() + 1) % Mode.values().length];
        click.set(click.state.setValue(MODE, next));
        if (!click.simulate) {
            notifyOutput(click.state.setValue(MODE, next), click.level, click.pos);
        }
        click.message("message.carpentersblocks.sensitivity_" + next.getSerializedName());
        return true;
    }
}
