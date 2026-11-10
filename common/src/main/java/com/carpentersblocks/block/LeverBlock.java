package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.List;

/**
 * Carpenter's Lever: covered base plus a fixed-texture handle tilted 40 degrees. On a wall the
 * handle points up when off and down when on (1.7.10).
 */
public class LeverBlock extends PolarityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** On floor and ceiling: handle swings along x (true) or z. */
    public static final BooleanProperty ALONG_X = BooleanProperty.create("along_x");
    private static final int HANDLE = 1;

    public LeverBlock(Properties properties) {
        super(properties.noCollission());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false)
            .setValue(INVERTED, false).setValue(ALONG_X, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED, ALONG_X);
    }

    @Override
    protected boolean isActive(BlockState state) {
        return state.getValue(POWERED);
    }

    private static boolean alongX(BlockState state) {
        // Walls: the handle always swings up/down, which is canonical z.
        return state.getValue(FACING).getAxis() == Direction.Axis.Y && state.getValue(ALONG_X);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(ALONG_X, context.getHorizontalDirection().getAxis() == Direction.Axis.X);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        Box base = alongX(state) ? Box.px(3.2, 0, 5, 12.8, 3, 11) : Box.px(5, 0, 3.2, 11, 3, 12.8);
        return Collections.singletonList(Part.of(attach(state, base)));
    }

    @Override
    protected List<Polygon> extraPolygons(BlockState state) {
        // A 2x2x10 pixel stick standing on the base, tilted 40 degrees: off towards +z/+x
        // (up on a wall), on towards -z/-x.
        Box stick = Box.px(7, 1, 7, 9, 11, 9);
        Vec3d pivot = new Vec3d(0.5, 1 / 16.0, 0.5);
        boolean on = state.getValue(POWERED);
        List<Polygon> handle = alongX(state)
            ? Polygons.ofRotatedBox(stick, 'z', on ? 40 : -40, pivot, HANDLE)
            : Polygons.ofRotatedBox(stick, 'x', on ? -40 : 40, pivot, HANDLE);
        if (state.getValue(FACING).getAxis() != Direction.Axis.Y) {
            // On a wall canonical +z points down; mirror the swing so "off" points up.
            handle = Polygons.ofRotatedBox(stick, 'x', on ? 40 : -40, pivot, HANDLE);
        }
        // The stick is u 7..9, v 6..16 of the lever texture; its ends are the 2x2 at the top (1.7.10).
        List<Polygon> textured = new java.util.ArrayList<>(handle.size());
        for (Polygon face : handle) {
            boolean end = face.direction == com.carpentersblocks.core.Side.UP || face.direction == com.carpentersblocks.core.Side.DOWN;
            textured.add(Polygons.fitUvs(face, 7, 6, 9, end ? 8 : 16));
        }
        return attach(state, textured);
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        return polygon.part == HANDLE ? UncoveredTextures.LEVER : null;
    }

    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState toggled = state.cycle(POWERED);
            level.setBlock(pos, toggled, BlockFlags.ALL);
            notifyOutput(toggled, level, pos);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, toggled.getValue(POWERED) ? 0.6F : 0.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
