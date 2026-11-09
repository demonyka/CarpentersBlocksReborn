package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.shape.SlabShape;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

/** Carpenter's Block: a coverable full block that the hammer turns into a slab and back. */
public class CarpentersBlock extends CoverableBlock {
    public static final EnumProperty<SlabShapeProperty> SHAPE = EnumProperty.create("shape", SlabShapeProperty.class);

    private static final VoxelShape[] SHAPES = new VoxelShape[SlabShape.values().length];
    @SuppressWarnings("unchecked")
    private static final List<Polygon>[] POLYGONS = new List[SlabShape.values().length];

    static {
        for (SlabShape shape : SlabShape.values()) {
            SHAPES[shape.ordinal()] = Sides.toShape(shape.bounds);
            POLYGONS[shape.ordinal()] = Collections.unmodifiableList(Polygons.ofBox(shape.bounds));
        }
    }

    public CarpentersBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, SlabShapeProperty.FULL).setValue(LIGHT, 0).setValue(OPAQUE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHAPE, OPAQUE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(SHAPE).ordinal()];
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(SHAPE) != SlabShapeProperty.FULL;
    }

    @Override
    public List<Polygon> getRenderPolygons(BlockState state) {
        return POLYGONS[state.getValue(SHAPE).ordinal()];
    }

    @Override
    public boolean onHammer(HammerClick click) {
        SlabShape next = click.state.getValue(SHAPE).core().hammered(click.side());
        click.set(click.state.setValue(SHAPE, SlabShapeProperty.of(next)));
        return true;
    }
}
